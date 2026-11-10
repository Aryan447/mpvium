package app.aryan447.mpvium.domain.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import app.aryan447.mpvium.domain.media.model.LocalAlbum
import app.aryan447.mpvium.domain.media.model.LocalAudioArtist
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import app.aryan447.mpvium.utils.storage.FileTypeUtils
import app.aryan447.mpvium.utils.storage.VideoScanUtils
import kotlinx.coroutines.Dispatchers
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
   * Quick check whether any audio files exist on the device.
   */
  fun hasAudio(context: Context): Boolean {
    return try {
      val projection = arrayOf(MediaStore.Audio.Media._ID)
      val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
      context.contentResolver.query(
        uri,
        projection,
        null,
        null,
        "${MediaStore.Audio.Media._ID} ASC LIMIT 1",
      )?.use { cursor ->
        cursor.moveToFirst()
      } ?: false
    } catch (e: Exception) {
      Log.w(TAG, "Failed to check for audio presence", e)
      false
    }
  }

  /**
   * Scans all audio files via MediaStore.Audio, grouping them into tracks,
   * albums (composite id from album + artist), and artists.
   */
  suspend fun scanAudio(context: Context): AudioLibraryData = withContext(Dispatchers.IO) {
    val tracks = mutableListOf<LocalAudioTrack>()

    val projection = arrayOf(
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

    try {
      context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        "${MediaStore.Audio.Media.TITLE} ASC",
      )?.use { cursor ->
        val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
        val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
        val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
        val modCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
        val addCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
        val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
        val artistIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
        val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
        val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
        val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
        val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
        val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)

        while (cursor.moveToNext()) {
          val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
          val file = if (path.isNotBlank()) File(path) else null
          val ext = file?.extension?.lowercase(Locale.getDefault()) ?: ""
          val isAudio = FileTypeUtils.AUDIO_EXTENSIONS.contains(ext) ||
            (mimeCol >= 0 && cursor.getString(mimeCol)?.startsWith("audio/") == true)

          if (!isAudio) continue

          val id = if (idCol >= 0) cursor.getLong(idCol) else 0L
          val title = if (titleCol >= 0) cursor.getString(titleCol) ?: file?.nameWithoutExtension ?: "Unknown Track" else "Unknown Track"
          val displayName = if (nameCol >= 0) cursor.getString(nameCol) ?: file?.name ?: title else title
          val artist = if (artistCol >= 0) cursor.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Unknown Artist" else "Unknown Artist"
          val artistId = if (artistIdCol >= 0) cursor.getLong(artistIdCol) else 0L
          val album = if (albumCol >= 0) cursor.getString(albumCol)?.takeIf { it != "<unknown>" } ?: "Unknown Album" else "Unknown Album"
          val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else 0L
          val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else file?.length() ?: 0L
          val duration = if (durCol >= 0) cursor.getLong(durCol) else 0L
          val dateModified = if (modCol >= 0) cursor.getLong(modCol) else 0L
          val dateAdded = if (addCol >= 0) cursor.getLong(addCol) else 0L
          val mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) ?: FileTypeUtils.getAudioMimeTypeFromExtension(ext) else FileTypeUtils.getAudioMimeTypeFromExtension(ext)
          val trackNumber = if (trackCol >= 0) cursor.getInt(trackCol) else 0
          val year = if (yearCol >= 0) cursor.getInt(yearCol) else 0

          val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
          val albumArtUri = if (albumId > 0) ContentUris.withAppendedId(ALBUM_ART_BASE_URI, albumId) else null

          tracks.add(
            LocalAudioTrack(
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
              mimeType = mimeType,
              dateAdded = dateAdded,
              dateModified = dateModified,
            )
          )
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error scanning MediaStore for audio", e)
    }

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
}
