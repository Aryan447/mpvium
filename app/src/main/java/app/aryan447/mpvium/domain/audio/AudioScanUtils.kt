package app.aryan447.mpvium.domain.audio

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import app.aryan447.mpvium.domain.media.model.LocalAlbum
import app.aryan447.mpvium.domain.media.model.LocalAudioArtist
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import app.aryan447.mpvium.utils.media.MediaInfoOps
import app.aryan447.mpvium.utils.storage.FileFilterUtils
import app.aryan447.mpvium.utils.storage.FileTypeUtils
import app.aryan447.mpvium.utils.storage.StorageVolumeUtils
import app.aryan447.mpvium.utils.storage.VideoScanUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class AudioLibraryData(
  val tracks: List<LocalAudioTrack> = emptyList(),
  val albums: List<LocalAlbum> = emptyList(),
  val artists: List<LocalAudioArtist> = emptyList(),
)

object AudioScanUtils {
  private const val TAG = "AudioScanUtils"

  private val ALBUM_ART_BASE_URI: Uri = Uri.parse("content://media/external/audio/albumart")

  /**
   * The `files` table indexes everything the media scanner saw, including audio
   * the scanner did not classify under [MediaStore.Audio] (notably `.opus` on
   * several OEM skins). Querying it covers those files without a disk walk.
   */
  private val FILES_URI: Uri = MediaStore.Files.getContentUri("external")

  /** Matches audio MIME types and audio file extensions, case-insensitively. */
  private val AUDIO_FILES_SELECTION: String = buildString {
    append("(")
    append("${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'audio/%'")
    for (extension in FileTypeUtils.AUDIO_EXTENSIONS) {
      append(" OR lower(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE '%.$extension'")
    }
    append(")")
  }

  private val AUDIO_PROJECTION = arrayOf(
    MediaStore.Audio.Media._ID,
    MediaStore.Audio.Media.TITLE,
    MediaStore.Audio.Media.DISPLAY_NAME,
    MediaStore.Audio.Media.DATA,
    MediaStore.Audio.Media.SIZE,
    MediaStore.Audio.Media.DURATION,
    MediaStore.Audio.Media.DATE_MODIFIED,
    MediaStore.Audio.Media.DATE_ADDED,
    MediaStore.Audio.Media.MIME_TYPE,
    MediaStore.Audio.Media.ARTIST_ID,
    MediaStore.Audio.Media.ARTIST,
    MediaStore.Audio.Media.ALBUM_ID,
    MediaStore.Audio.Media.ALBUM,
    MediaStore.Audio.Media.TRACK,
    MediaStore.Audio.Media.YEAR,
  )

  /**
   * The files table does not guarantee the audio columns on every device, so
   * the scan falls back to [FILES_BASIC_PROJECTION] if this one is rejected.
   */
  private val FILES_PROJECTION = arrayOf(
    MediaStore.Files.FileColumns._ID,
    MediaStore.Files.FileColumns.DISPLAY_NAME,
    MediaStore.Files.FileColumns.DATA,
    MediaStore.Files.FileColumns.SIZE,
    MediaStore.Files.FileColumns.DATE_MODIFIED,
    MediaStore.Files.FileColumns.DATE_ADDED,
    MediaStore.Files.FileColumns.MIME_TYPE,
    MediaStore.Files.FileColumns.TITLE,
    MediaStore.Audio.AudioColumns.DURATION,
    MediaStore.Audio.AudioColumns.ARTIST_ID,
    MediaStore.Audio.AudioColumns.ARTIST,
    MediaStore.Audio.AudioColumns.ALBUM_ID,
    MediaStore.Audio.AudioColumns.ALBUM,
    MediaStore.Audio.AudioColumns.TRACK,
    MediaStore.Audio.AudioColumns.YEAR,
  )

  private val FILES_BASIC_PROJECTION = arrayOf(
    MediaStore.Files.FileColumns._ID,
    MediaStore.Files.FileColumns.DISPLAY_NAME,
    MediaStore.Files.FileColumns.DATA,
    MediaStore.Files.FileColumns.SIZE,
    MediaStore.Files.FileColumns.DATE_MODIFIED,
    MediaStore.Files.FileColumns.DATE_ADDED,
    MediaStore.Files.FileColumns.MIME_TYPE,
  )

  private const val MAX_FILE_SYSTEM_DEPTH = 10
  private const val FILE_SYSTEM_PROBE_CACHE_MS = 60_000L
  private const val METADATA_PARALLELISM = 4

  @Volatile private var fileSystemProbeTime = 0L
  @Volatile private var fileSystemProbeResult = false

  /**
   * Quick check whether any audio files exist on the device.
   *
   * MediaStore is checked first and is enough on most devices. Devices that do
   * not index `.opus` under [MediaStore.Audio] are covered by the files table,
   * and a short filesystem probe catches files the scanner skipped entirely.
   */
  fun hasAudio(context: Context): Boolean {
    if (hasMediaStoreRows(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)) {
      return true
    }
    if (hasMediaStoreRows(context, FILES_URI, AUDIO_FILES_SELECTION)) {
      return true
    }
    return hasAudioOnFileSystem(context)
  }

  private fun hasMediaStoreRows(
    context: Context,
    uri: Uri,
    selection: String?,
  ): Boolean = try {
    context.contentResolver.query(
      uri,
      arrayOf(MediaStore.MediaColumns._ID),
      selection,
      null,
      null,
    )?.use { cursor ->
      cursor.moveToFirst()
    } ?: false
  } catch (e: Exception) {
    Log.w(TAG, "Failed to check for audio presence", e)
    false
  }

  /**
   * Scans all audio files, grouping them into tracks, albums (composite id
   * from album + artist), and artists.
   *
   * Sources are merged by path: MediaStore.Audio first, then the files table
   * for audio the scanner did not classify, then a filesystem walk when the
   * media database knows nothing at all.
   */
  suspend fun scanAudio(context: Context): AudioLibraryData = withContext(Dispatchers.IO) {
    val tracksByPath = LinkedHashMap<String, LocalAudioTrack>()

    scanAudioFromMediaStore(context, tracksByPath)
    scanAudioFromMediaStoreFiles(context, tracksByPath)
    if (tracksByPath.isEmpty()) {
      scanAudioFromFileSystem(context, tracksByPath)
    }

    val tracks = enrichMissingDurations(context, tracksByPath.values.toList())

    // Group into albums (ID from album + artist, never from title alone)
    val albumsMap = mutableMapOf<String, MutableList<LocalAudioTrack>>()
    for (track in tracks) {
      val albumKey = track.albumKey
      albumsMap.getOrPut(albumKey) { mutableListOf() }.add(track)
    }

    val albums = albumsMap.map { (key, albumTracks) ->
      val first = albumTracks.first()
      val sortedTracks = albumTracks.sortedWith(
        compareBy<LocalAudioTrack> { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
          .thenBy { it.title.lowercase(Locale.getDefault()) }
      )
      val totalDuration = albumTracks.sumOf { it.duration }
      val artUri = albumTracks.firstOrNull { it.albumArtUri != null }?.albumArtUri
      val maxYear = albumTracks.maxOfOrNull { it.year } ?: 0

      LocalAlbum(
        id = key,
        title = first.album,
        artist = first.artist,
        trackCount = albumTracks.size,
        duration = totalDuration,
        durationFormatted = VideoScanUtils.formatDuration(totalDuration),
        year = maxYear,
        artworkUri = artUri,
        tracks = sortedTracks,
      )
    }.sortedBy { it.title.lowercase(Locale.getDefault()) }

    // Group into artists
    val artistsMap = mutableMapOf<String, MutableList<LocalAlbum>>()
    for (album in albums) {
      val artistKey = LocalAudioArtist.createId(album.artist)
      artistsMap.getOrPut(artistKey) { mutableListOf() }.add(album)
    }

    val artists = artistsMap.map { (key, artistAlbums) ->
      val artistName = artistAlbums.first().artist
      val artistTracks = artistAlbums.flatMap { it.tracks }
      LocalAudioArtist(
        id = key,
        name = artistName,
        trackCount = artistTracks.size,
        albumCount = artistAlbums.size,
        albums = artistAlbums,
        tracks = artistTracks,
      )
    }.sortedBy { it.name.lowercase(Locale.getDefault()) }

    AudioLibraryData(
      tracks = tracks.sortedBy { it.title.lowercase(Locale.getDefault()) },
      albums = albums,
      artists = artists,
    )
  }

  // ==========================================================================
  // MediaStore
  // ==========================================================================

  private fun scanAudioFromMediaStore(
    context: Context,
    tracksByPath: MutableMap<String, LocalAudioTrack>,
  ) {
    try {
      context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        AUDIO_PROJECTION,
        null,
        null,
        "${MediaStore.Audio.Media.TITLE} ASC",
      )?.use { cursor ->
        val columns = TrackColumns(cursor)
        while (cursor.moveToNext()) {
          val track = buildTrack(
            cursor = cursor,
            columns = columns,
            contentBaseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            albumFallbackFromFolder = false,
          ) ?: continue
          tracksByPath.putIfAbsent(trackKey(track), track)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error scanning MediaStore for audio", e)
    }
  }

  private fun scanAudioFromMediaStoreFiles(
    context: Context,
    tracksByPath: MutableMap<String, LocalAudioTrack>,
  ) {
    val cursor = queryFilesTable(context, FILES_PROJECTION)
      ?: queryFilesTable(context, FILES_BASIC_PROJECTION)
      ?: return

    cursor.use {
      val columns = TrackColumns(it)
      while (it.moveToNext()) {
        val track = buildTrack(
          cursor = it,
          columns = columns,
          contentBaseUri = FILES_URI,
          albumFallbackFromFolder = true,
        ) ?: continue
        tracksByPath.putIfAbsent(trackKey(track), track)
      }
    }
  }

  private fun queryFilesTable(
    context: Context,
    projection: Array<String>,
  ): Cursor? = try {
    context.contentResolver.query(
      FILES_URI,
      projection,
      AUDIO_FILES_SELECTION,
      null,
      "${MediaStore.Files.FileColumns.DISPLAY_NAME} ASC",
    )
  } catch (e: Exception) {
    Log.w(TAG, "Files table audio query failed for ${projection.size} columns", e)
    null
  }

  private class TrackColumns(cursor: Cursor) {
    val id = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
    val title = cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)
    val displayName = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
    val data = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
    val size = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
    val duration = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)
    val dateModified = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
    val dateAdded = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
    val mimeType = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
    val artistId = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST_ID)
    val artist = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
    val albumId = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID)
    val album = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)
    val track = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TRACK)
    val year = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.YEAR)
  }

  private fun buildTrack(
    cursor: Cursor,
    columns: TrackColumns,
    contentBaseUri: Uri,
    albumFallbackFromFolder: Boolean,
  ): LocalAudioTrack? {
    val path = if (columns.data >= 0) cursor.getString(columns.data).orEmpty() else ""
    val file = path.takeIf { it.isNotBlank() }?.let(::File)
    val displayName = (if (columns.displayName >= 0) cursor.getString(columns.displayName) else null)
      ?.takeIf { it.isNotBlank() }
      ?: file?.name
      ?: "Unknown Track"
    val extension = (file?.extension ?: displayName.substringAfterLast('.', ""))
      .lowercase(Locale.getDefault())
    val mimeType = (if (columns.mimeType >= 0) cursor.getString(columns.mimeType) else null)
      ?.takeIf { it.isNotBlank() }
    val isAudio = FileTypeUtils.AUDIO_EXTENSIONS.contains(extension) ||
      mimeType?.startsWith("audio/") == true
    if (!isAudio) return null

    val id = if (columns.id >= 0) cursor.getLong(columns.id) else 0L
    val title = (if (columns.title >= 0) cursor.getString(columns.title) else null)
      ?.takeIf { it.isNotBlank() }
      ?: file?.nameWithoutExtension
      ?: displayName.substringBeforeLast('.')
    val artist = (if (columns.artist >= 0) cursor.getString(columns.artist) else null)
      ?.takeIf { it != "<unknown>" && it.isNotBlank() }
      ?: "Unknown Artist"
    val artistId = if (columns.artistId >= 0) cursor.getLong(columns.artistId) else 0L
    val albumTag = (if (columns.album >= 0) cursor.getString(columns.album) else null)
      ?.takeIf { it != "<unknown>" && it.isNotBlank() }
    val album = albumTag
      ?: if (albumFallbackFromFolder) {
        file?.parentFile?.name?.takeIf { it.isNotBlank() } ?: "Unknown Album"
      } else {
        "Unknown Album"
      }
    val albumId = if (columns.albumId >= 0) cursor.getLong(columns.albumId) else 0L
    val size = if (columns.size >= 0) cursor.getLong(columns.size) else file?.length() ?: 0L
    val duration = if (columns.duration >= 0) cursor.getLong(columns.duration) else 0L
    val dateModified = if (columns.dateModified >= 0) cursor.getLong(columns.dateModified) else 0L
    val dateAdded = if (columns.dateAdded >= 0) cursor.getLong(columns.dateAdded) else 0L
    val mime = mimeType ?: FileTypeUtils.getAudioMimeTypeFromExtension(extension)
    val trackNumber = if (columns.track >= 0) cursor.getInt(columns.track) else 0
    val year = if (columns.year >= 0) cursor.getInt(columns.year) else 0

    val uri = if (id > 0) {
      ContentUris.withAppendedId(contentBaseUri, id)
    } else if (file != null) {
      Uri.fromFile(file)
    } else {
      return null
    }
    val albumArtUri = if (albumId > 0) ContentUris.withAppendedId(ALBUM_ART_BASE_URI, albumId) else null

    return LocalAudioTrack(
      id = id,
      title = title,
      displayName = displayName,
      artist = artist,
      artistId = artistId,
      album = album,
      albumId = albumId,
      albumKey = LocalAlbum.createId(album, artist),
      trackNumber = trackNumber,
      year = year,
      duration = duration,
      durationFormatted = VideoScanUtils.formatDuration(duration),
      size = size,
      sizeFormatted = VideoScanUtils.formatFileSize(size),
      path = path,
      uri = uri,
      albumArtUri = albumArtUri,
      mimeType = mime,
      dateAdded = dateAdded,
      dateModified = dateModified,
    )
  }

  private fun trackKey(track: LocalAudioTrack): String =
    track.path.ifBlank { "${track.displayName}|${track.size}" }

  // ==========================================================================
  // Filesystem fallback
  // ==========================================================================

  private fun hasAudioOnFileSystem(context: Context): Boolean {
    val now = SystemClock.elapsedRealtime()
    if (fileSystemProbeTime != 0L && now - fileSystemProbeTime < FILE_SYSTEM_PROBE_CACHE_MS) {
      return fileSystemProbeResult
    }

    val found = audioSearchRoots(context).any { containsAudioFile(it, 0) }
    fileSystemProbeTime = now
    fileSystemProbeResult = found
    return found
  }

  private fun containsAudioFile(directory: File, depth: Int): Boolean {
    if (depth > MAX_FILE_SYSTEM_DEPTH) return false
    val children = try {
      directory.listFiles()
    } catch (e: SecurityException) {
      Log.w(TAG, "Cannot read ${directory.absolutePath}", e)
      null
    } ?: return false

    for (child in children) {
      if (child.isDirectory) {
        if (!FileFilterUtils.shouldSkipFolder(child) && containsAudioFile(child, depth + 1)) {
          return true
        }
      } else if (!FileFilterUtils.shouldSkipFile(child) && FileTypeUtils.isAudioFile(child)) {
        return true
      }
    }
    return false
  }

  private fun scanAudioFromFileSystem(
    context: Context,
    tracksByPath: MutableMap<String, LocalAudioTrack>,
  ) {
    val files = linkedSetOf<File>()
    for (root in audioSearchRoots(context)) {
      collectAudioFiles(root, 0, files)
    }

    for (file in files) {
      val track = buildTrackFromFile(file)
      tracksByPath.putIfAbsent(track.path, track)
    }
  }

  private fun collectAudioFiles(directory: File, depth: Int, files: MutableSet<File>) {
    if (depth > MAX_FILE_SYSTEM_DEPTH) return
    val children = try {
      directory.listFiles()
    } catch (e: SecurityException) {
      Log.w(TAG, "Cannot read ${directory.absolutePath}", e)
      null
    } ?: return

    for (child in children) {
      if (child.isDirectory) {
        if (!FileFilterUtils.shouldSkipFolder(child)) {
          collectAudioFiles(child, depth + 1, files)
        }
      } else if (!FileFilterUtils.shouldSkipFile(child) && FileTypeUtils.isAudioFile(child)) {
        files.add(child)
      }
    }
  }

  private fun audioSearchRoots(context: Context): List<File> {
    val paths = linkedSetOf<String>()
    val primaryStorage = Environment.getExternalStorageDirectory()

    // Common media locations first so the presence probe finds a track fast.
    val commonDirectories = listOf(
      Environment.DIRECTORY_MUSIC,
      Environment.DIRECTORY_DOWNLOADS,
      Environment.DIRECTORY_PODCASTS,
      Environment.DIRECTORY_RINGTONES,
      Environment.DIRECTORY_ALARMS,
      Environment.DIRECTORY_NOTIFICATIONS,
    )
    for (directory in commonDirectories) {
      paths.add(File(primaryStorage, directory).absolutePath)
    }

    // Then the whole volume, so audio in custom folders is not missed.
    paths.add(primaryStorage.absolutePath)
    for (volume in StorageVolumeUtils.getExternalStorageVolumes(context)) {
      StorageVolumeUtils.getVolumePath(volume)?.let { paths.add(it) }
    }
    return paths.map(::File).filter { it.isDirectory && it.canRead() }
  }

  private fun buildTrackFromFile(file: File): LocalAudioTrack {
    val extension = file.extension.lowercase(Locale.getDefault())
    val size = file.length()
    val dateModified = file.lastModified() / 1000
    val album = file.parentFile?.name?.takeIf { it.isNotBlank() } ?: "Unknown Album"

    return LocalAudioTrack(
      id = file.absolutePath.hashCode().toLong(),
      title = file.nameWithoutExtension,
      displayName = file.name,
      artist = "Unknown Artist",
      artistId = 0L,
      album = album,
      albumId = 0L,
      albumKey = LocalAlbum.createId(album, "Unknown Artist"),
      trackNumber = 0,
      year = 0,
      duration = 0L,
      durationFormatted = VideoScanUtils.formatDuration(0L),
      size = size,
      sizeFormatted = VideoScanUtils.formatFileSize(size),
      path = file.absolutePath,
      uri = Uri.fromFile(file),
      albumArtUri = null,
      mimeType = FileTypeUtils.getAudioMimeTypeFromExtension(extension),
      dateAdded = dateModified,
      dateModified = dateModified,
    )
  }

  // ==========================================================================
  // Duration enrichment
  // ==========================================================================

  /**
   * Files the media scanner could not classify often have no duration in the
   * database. Parse those with MediaInfo, bounded so a large fallback library
   * cannot exhaust file descriptors.
   */
  private suspend fun enrichMissingDurations(
    context: Context,
    tracks: List<LocalAudioTrack>,
  ): List<LocalAudioTrack> {
    val missing = tracks.filter { it.duration <= 0L }
    if (missing.isEmpty()) return tracks

    val permits = Semaphore(METADATA_PARALLELISM)
    val durations = coroutineScope {
      missing.map { track ->
        async {
          permits.withPermit { readDurationMs(context, track) }
        }
      }.awaitAll()
    }

    val durationsByUri = missing.mapIndexed { index, track ->
      track.uri.toString() to durations[index]
    }.toMap()

    return tracks.map { track ->
      if (track.duration > 0L) return@map track
      val duration = durationsByUri[track.uri.toString()] ?: 0L
      if (duration <= 0L) {
        track
      } else {
        track.copy(
          duration = duration,
          durationFormatted = VideoScanUtils.formatDuration(duration),
        )
      }
    }
  }

  private suspend fun readDurationMs(context: Context, track: LocalAudioTrack): Long = try {
    MediaInfoOps.extractBasicMetadata(context, track.uri, track.displayName)
      .getOrNull()
      ?.durationMs
      ?: 0L
  } catch (e: Exception) {
    Log.w(TAG, "Could not read duration for ${track.path.ifBlank { track.displayName }}", e)
    0L
  }
}
