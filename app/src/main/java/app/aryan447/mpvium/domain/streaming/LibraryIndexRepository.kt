package app.aryan447.mpvium.domain.streaming

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import app.aryan447.mpvium.database.dao.LibraryIndexDao
import app.aryan447.mpvium.database.entities.LibraryIndexEntity
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.media.model.VideoFolder
import app.aryan447.mpvium.preferences.FolderVisibility
import app.aryan447.mpvium.preferences.FoldersPreferences
import app.aryan447.mpvium.preferences.LibraryScanMode
import app.aryan447.mpvium.repository.MediaFileRepository
import app.aryan447.mpvium.utils.media.MediaInfoParser
import app.aryan447.mpvium.utils.media.MediaLibraryEvents
import app.aryan447.mpvium.utils.media.ParsedMediaInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.pow

class LibraryIndexRepository(
  private val context: Context,
  private val dao: LibraryIndexDao,
  private val foldersPreferences: FoldersPreferences,
  private val seriesDetector: SeriesDetector,
) {
  companion object {
    private const val TAG = "LibraryIndexRepository"
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val scanMutex = Mutex()
  @Volatile private var inFlightDeferred: Deferred<DetectedMediaLibrary>? = null
  @Volatile private var inFlightForce: Boolean = false
  @Volatile private var isDirty: Boolean = true

  private val _library = MutableStateFlow<DetectedMediaLibrary?>(null)
  val library: StateFlow<DetectedMediaLibrary?> = _library.asStateFlow()

  init {
    scope.launch {
      // Warm from snapshot immediately if present
      loadFromSnapshotIfAvailable()

      // Kick off initial scan in AUTO mode
      if (foldersPreferences.libraryScanMode.get() == LibraryScanMode.AUTO) {
        runCatching { refresh(force = false) }
      }
    }

    // Invalidation: MediaLibraryEvents.changes marks the index dirty and refreshes in background
    scope.launch {
      MediaLibraryEvents.changes.collectLatest {
        Log.d(TAG, "MediaLibraryEvents change received, marking dirty")
        isDirty = true
        val mode = foldersPreferences.libraryScanMode.get()
        if (mode == LibraryScanMode.AUTO) {
          runCatching { refresh(force = false) }
        } else {
          // In MANUAL mode, reload playback states from snapshot without scanning storage
          runCatching { refresh(force = false) }
        }
      }
    }

    // React to folder preference updates
    scope.launch {
      foldersPreferences.whitelistedFolders.changes().collectLatest {
        isDirty = true
        if (foldersPreferences.libraryScanMode.get() == LibraryScanMode.AUTO && _library.value != null) {
          runCatching { refresh(force = false) }
        }
      }
    }
    scope.launch {
      foldersPreferences.blacklistedFolders.changes().collectLatest {
        isDirty = true
        if (foldersPreferences.libraryScanMode.get() == LibraryScanMode.AUTO && _library.value != null) {
          runCatching { refresh(force = false) }
        }
      }
    }
    scope.launch {
      foldersPreferences.whitelistOnlyEnabled.changes().collectLatest {
        isDirty = true
        if (foldersPreferences.libraryScanMode.get() == LibraryScanMode.AUTO && _library.value != null) {
          runCatching { refresh(force = false) }
        }
      }
    }
  }

  suspend fun getLibrary(force: Boolean = false): DetectedMediaLibrary {
    return refresh(force)
  }

  suspend fun refresh(force: Boolean = false): DetectedMediaLibrary {
    val scanMode = foldersPreferences.libraryScanMode.get()

    // Fast-path: if not forcing, not dirty, and data already loaded
    if (!force && !isDirty && _library.value != null) {
      return _library.value!!
    }

    // Coalesce concurrent callers (mutex + in-flight job)
    val deferred = scanMutex.withLock {
      val existing = inFlightDeferred
      if (existing != null && existing.isActive) {
        // If force is requested and the running scan wasn't forced, let it finish then run forced scan
        if (force && !inFlightForce) {
          // Handled after await below
        } else {
          return@withLock existing
        }
      }

      inFlightForce = force
      val job = scope.async {
        try {
          executeScan(force = force, scanMode = scanMode)
        } finally {
          scanMutex.withLock {
            if (inFlightDeferred === this@async) {
              inFlightDeferred = null
              inFlightForce = false
            }
          }
        }
      }
      inFlightDeferred = job
      job
    }

    val result = deferred.await()
    // If a forced scan was requested while a non-forced scan was running, execute forced scan now
    if (force && !inFlightForce && isDirty) {
      return refresh(force = true)
    }
    return result
  }

  private suspend fun executeScan(force: Boolean, scanMode: LibraryScanMode): DetectedMediaLibrary = withContext(Dispatchers.IO) {
    val totalStartTime = System.currentTimeMillis()

    // In MANUAL mode without force: load snapshot only
    if (scanMode == LibraryScanMode.MANUAL && !force) {
      val cachedEntities = dao.getAll()
      if (cachedEntities.isNotEmpty()) {
        val groupingStartTime = System.currentTimeMillis()
        val items = cachedEntities.map { it.toVideo() to it.toParsedMediaInfo() }
        val folders = buildFoldersFromVideos(items.map { it.first })
        val detected = seriesDetector.groupMedia(items, folders)
        val groupingMs = System.currentTimeMillis() - groupingStartTime
        val totalMs = System.currentTimeMillis() - totalStartTime

        Log.d(TAG, "Library scan: folders=0ms, parse=0ms, grouping=${groupingMs}ms, total=${totalMs}ms")
        _library.value = detected
        isDirty = false
        return@withContext detected
      }
      // If snapshot was empty in MANUAL mode, proceed to first-run scan below
    }

    // Phase 1: Folders
    val foldersStartTime = System.currentTimeMillis()
    val allVideos = mutableListOf<Video>()
    val visibleFolders: List<VideoFolder>
    try {
      val whitelist = foldersPreferences.whitelistedFolders.get()
      val blacklist = foldersPreferences.blacklistedFolders.get()
      val whitelistOnly = foldersPreferences.whitelistOnlyEnabled.get()

      val rawFolders = MediaFileRepository.getAllVideoFolders(context).filter { folder ->
        FolderVisibility.isFolderVisible(folder.path, whitelist, blacklist, whitelistOnly)
      }
      visibleFolders = rawFolders

      for (folder in visibleFolders) {
        val folderVideos = MediaFileRepository.getVideosInFolder(context, folder.bucketId)
        allVideos.addAll(
          folderVideos.filter { video ->
            val parent = try {
              File(video.path).parent ?: folder.path
            } catch (_: Exception) {
              folder.path
            }
            FolderVisibility.isFolderVisible(parent, whitelist, blacklist, whitelistOnly)
          },
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error during folder discovery phase", e)
      // Never wipe the snapshot on error!
      val existing = _library.value ?: loadFallbackFromSnapshot()
      if (existing != null) {
        return@withContext existing
      } else {
        throw e
      }
    }
    val foldersMs = System.currentTimeMillis() - foldersStartTime

    // Phase 2: Parse (incremental)
    val parseStartTime = System.currentTimeMillis()
    val items = mutableListOf<Pair<Video, ParsedMediaInfo>>()
    try {
      val cachedEntities = dao.getAll()
      val cachedMap = cachedEntities.associateBy { it.path }
      val currentPaths = HashSet<String>(allVideos.size)
      val toInsertOrUpdate = mutableListOf<LibraryIndexEntity>()

      for (video in allVideos) {
        currentPaths.add(video.path)
        val cached = cachedMap[video.path]
        if (cached != null && cached.size == video.size && cached.mtimeMs == video.dateModified) {
          // Cache hit: re-use parsed info, no file/string re-parse needed
          items.add(video to cached.toParsedMediaInfo())
        } else {
          // Cache miss or changed size/mtime: re-parse file display name
          val parsed = MediaInfoParser.parse(video.displayName)
          items.add(video to parsed)
          toInsertOrUpdate.add(video.toEntity(parsed))
        }
      }

      // Prune deleted ones
      val deletedPaths = cachedMap.keys.filter { it !in currentPaths }
      if (deletedPaths.isNotEmpty()) {
        deletedPaths.chunked(500).forEach { chunk ->
          dao.deleteBatch(chunk)
        }
      }

      // Persist newly parsed or updated items
      if (toInsertOrUpdate.isNotEmpty()) {
        toInsertOrUpdate.chunked(500).forEach { chunk ->
          dao.insertOrUpdateBatch(chunk)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error during parse/snapshot phase", e)
      // Never wipe the snapshot on error
      if (items.isEmpty()) {
        val existing = _library.value ?: loadFallbackFromSnapshot()
        if (existing != null) return@withContext existing
      }
    }
    val parseMs = System.currentTimeMillis() - parseStartTime

    // Phase 3: Grouping
    val groupingStartTime = System.currentTimeMillis()
    val detected = seriesDetector.groupMedia(items, visibleFolders)
    val groupingMs = System.currentTimeMillis() - groupingStartTime

    val totalMs = System.currentTimeMillis() - totalStartTime
    Log.d(TAG, "Library scan: folders=${foldersMs}ms, parse=${parseMs}ms, grouping=${groupingMs}ms, total=${totalMs}ms")

    _library.value = detected
    isDirty = false
    detected
  }

  private suspend fun loadFromSnapshotIfAvailable() {
    try {
      val cachedEntities = dao.getAll()
      if (cachedEntities.isNotEmpty() && _library.value == null) {
        val totalStart = System.currentTimeMillis()
        val items = cachedEntities.map { it.toVideo() to it.toParsedMediaInfo() }
        val folders = buildFoldersFromVideos(items.map { it.first })
        val groupingStart = System.currentTimeMillis()
        val detected = seriesDetector.groupMedia(items, folders)
        val groupingMs = System.currentTimeMillis() - groupingStart
        val totalMs = System.currentTimeMillis() - totalStart
        Log.d(TAG, "Warm start from snapshot: folders=0ms, parse=0ms, grouping=${groupingMs}ms, total=${totalMs}ms")
        _library.value = detected
        if (foldersPreferences.libraryScanMode.get() == LibraryScanMode.MANUAL) {
          isDirty = false
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to load snapshot on startup", e)
    }
  }

  private suspend fun loadFallbackFromSnapshot(): DetectedMediaLibrary? {
    return try {
      val cachedEntities = dao.getAll()
      if (cachedEntities.isEmpty()) return null
      val items = cachedEntities.map { it.toVideo() to it.toParsedMediaInfo() }
      val folders = buildFoldersFromVideos(items.map { it.first })
      seriesDetector.groupMedia(items, folders)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load fallback snapshot", e)
      null
    }
  }

  private fun buildFoldersFromVideos(videos: List<Video>): List<VideoFolder> {
    return videos.groupBy { it.bucketId }.map { (bucketId, list) ->
      val first = list.first()
      val folderPath = try {
        File(first.path).parent ?: bucketId
      } catch (_: Exception) {
        bucketId
      }
      VideoFolder(
        bucketId = bucketId,
        name = first.bucketDisplayName.ifBlank { File(folderPath).name },
        path = folderPath,
        videoCount = list.size,
        totalSize = list.sumOf { it.size },
        totalDuration = list.sumOf { it.duration },
        lastModified = list.maxOfOrNull { it.dateModified } ?: 0L,
      )
    }.sortedBy { it.name.lowercase(Locale.getDefault()) }
  }
}

fun LibraryIndexEntity.toParsedMediaInfo(): ParsedMediaInfo =
  ParsedMediaInfo(
    title = parsedTitle,
    year = parsedYear,
    season = parsedSeason,
    episode = parsedEpisode,
    episodeTitle = parsedEpisodeTitle,
    type = parsedType,
  )

fun LibraryIndexEntity.toVideo(): Video {
  val uri = if (mediaStoreId > 0) {
    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
  } else {
    Uri.fromFile(File(path))
  }
  return Video(
    id = if (mediaStoreId > 0) mediaStoreId else path.hashCode().toLong(),
    title = displayName.substringBeforeLast('.', displayName),
    displayName = displayName,
    path = path,
    uri = uri,
    duration = durationMs,
    durationFormatted = formatDuration(durationMs),
    size = size,
    sizeFormatted = formatFileSize(size),
    dateModified = mtimeMs,
    dateAdded = dateAdded,
    mimeType = mimeType,
    bucketId = bucketId,
    bucketDisplayName = bucketDisplayName,
    width = width,
    height = height,
    fps = fps,
    resolution = formatResolution(width, height),
    hasEmbeddedSubtitles = false,
    subtitleCodec = "",
  )
}

fun Video.toEntity(parsed: ParsedMediaInfo): LibraryIndexEntity =
  LibraryIndexEntity(
    path = path,
    size = size,
    mtimeMs = dateModified,
    parsedTitle = parsed.title,
    parsedYear = parsed.year,
    parsedSeason = parsed.season,
    parsedEpisode = parsed.episode,
    parsedEpisodeTitle = parsed.episodeTitle,
    parsedType = parsed.type,
    mediaStoreId = id,
    displayName = displayName,
    durationMs = duration,
    dateAdded = dateAdded,
    mimeType = mimeType,
    bucketId = bucketId,
    bucketDisplayName = bucketDisplayName,
    width = width,
    height = height,
    fps = fps,
    lastScanned = System.currentTimeMillis(),
  )

private fun formatDuration(durationMs: Long): String {
  if (durationMs <= 0) return "00:00"
  val totalSeconds = durationMs / 1000
  val seconds = totalSeconds % 60
  val minutes = (totalSeconds / 60) % 60
  val hours = totalSeconds / 3600
  return if (hours > 0) {
    "%d:%02d:%02d".format(hours, minutes, seconds)
  } else {
    "%02d:%02d".format(minutes, seconds)
  }
}

private fun formatFileSize(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val units = arrayOf("B", "KB", "MB", "GB", "TB")
  val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
  val value = bytes / 1024.0.pow(digitGroups.toDouble())
  return "%.1f %s".format(value, units[digitGroups])
}

private fun formatResolution(width: Int, height: Int): String {
  return if (width > 0 && height > 0) "${width}x${height}" else ""
}
