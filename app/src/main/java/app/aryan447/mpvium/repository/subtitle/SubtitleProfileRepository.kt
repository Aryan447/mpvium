package app.aryan447.mpvium.repository.subtitle

import android.content.Context
import android.util.Log
import app.aryan447.mpvium.domain.streaming.SeriesDetector
import app.aryan447.mpvium.utils.media.MediaInfoParser
import app.aryan447.mpvium.utils.media.MediaLibraryEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class SubtitleProfile(
  val subPos: Int,
  val subScale: Float,
  val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Stores and manages per-title subtitle positioning and scale profiles.
 *
 * Episodes of a series share a profile keyed by seriesId.
 * Standalone movies use a separate movieKey (normalized title + year).
 * Profiles are persisted atomically to a JSON file in [Context.getFilesDir].
 */
class SubtitleProfileRepository(
  private val context: Context?,
  private val json: Json,
  private val seriesDetector: SeriesDetector?,
  private val cacheFile: File,
  private val scope: CoroutineScope,
) {
  companion object {
    private const val TAG = "SubtitleProfileRepo"
    const val CACHE_FILE_NAME = "subtitle_profiles.json"
    const val SERIES_KEY_PREFIX = "series_"
    const val MOVIE_KEY_PREFIX = "movie_"

    private val SEASON_FOLDER_REGEX =
      Regex("""\b(?:Season|Series|Specials|S)[\s._-]*(\d{1,2})\b""", RegexOption.IGNORE_CASE)
    private val SPECIALS_FOLDER_REGEX =
      Regex("""^(?:Specials|Extras)$""", RegexOption.IGNORE_CASE)

    private val GENERIC_FOLDER_NAMES = setOf(
      "movies", "movie", "films", "film", "videos", "video", "tv", "tvshows",
      "shows", "series", "media", "download", "downloads", "storage", "emulated",
      "0", "dcim", "camera", "whatsapp", "telegram", "documents", "desktop",
      "sdcard", "all", "other", "misc", "new", "unsorted", "season", "specials",
      "extras", "featurettes", "trailers", "bonus"
    )

    private val GENERIC_VIDEO_TITLES = setOf(
      "movie", "video", "film", "feature", "untitled", "unknown", "index",
      "main", "play", "watch", "sample", "trailer"
    )

    private val PART_OR_EPISODE_TITLE_REGEX =
      Regex("""(?:cd|disc|disk|part|pt|episode|ep|e)?[\s._-]*\d{1,3}""", RegexOption.IGNORE_CASE)

    fun normalizeKey(title: String): String =
      title.lowercase(Locale.ROOT)
        .replace(Regex("""[^\p{L}\p{N}]"""), "")
        .trim()

    fun seriesKeyFor(seriesId: String): String {
      val stripped = if (seriesId.startsWith(SERIES_KEY_PREFIX)) {
        seriesId.removePrefix(SERIES_KEY_PREFIX)
      } else {
        seriesId
      }
      return "$SERIES_KEY_PREFIX${normalizeKey(stripped)}"
    }

    fun movieKeyFor(movieTitle: String, year: String? = null): String {
      val base = "$MOVIE_KEY_PREFIX${normalizeKey(movieTitle)}"
      val normalizedYear = year?.filter { it.isDigit() }?.take(4)?.takeIf { it.length == 4 }
      return if (normalizedYear != null) "$base@$normalizedYear" else base
    }

    fun canonicalKey(key: String): String = when {
      key.startsWith(SERIES_KEY_PREFIX) || key.startsWith(MOVIE_KEY_PREFIX) -> key
      else -> seriesKeyFor(key)
    }

    fun resolveKey(mediaTitle: String, filePath: String? = null): String {
      val cleanTitle = mediaTitle.trim()
      val parsed = MediaInfoParser.parse(cleanTitle)

      val parentFile = filePath?.let { runCatching { File(it).parentFile }.getOrNull() }
      val parentName = parentFile?.name ?: ""
      val grandParentName = parentFile?.parentFile?.name ?: ""
      val seasonMatch = SEASON_FOLDER_REGEX.find(parentName)
      val isSpecialsFolder = SPECIALS_FOLDER_REGEX.matches(parentName.trim())
      val parentSeasonNum = when {
        seasonMatch != null -> seasonMatch.groupValues[1].toIntOrNull() ?: 1
        isSpecialsFolder -> 0
        else -> null
      }

      var isTvSeries = parsed.type == "tv" || parsed.season != null || parsed.episode != null
      var seriesTitle = parsed.title
      var seriesYear = parsed.year

      if (!isTvSeries && parentSeasonNum != null) {
        isTvSeries = true
        val folderMedia = resolveFolderMedia(grandParentName)
        if (folderMedia != null) {
          seriesTitle = folderMedia.first
          seriesYear = seriesYear ?: folderMedia.second
        }
      }

      if (isTvSeries) {
        if (seriesTitle.isBlank()) {
          val primaryFolder = if (parentSeasonNum != null) grandParentName else parentName
          val secondaryFolder = if (parentSeasonNum != null) "" else grandParentName
          val folderMedia = resolveFolderMedia(primaryFolder) ?: resolveFolderMedia(secondaryFolder)
          if (folderMedia != null) {
            seriesTitle = folderMedia.first
            seriesYear = seriesYear ?: folderMedia.second
          }
        }
        val seriesId = if (seriesTitle.isNotBlank()) {
          normalizeKey(seriesTitle)
        } else {
          normalizeKey(cleanTitle.substringBeforeLast('.'))
        }
        return seriesKeyFor(seriesId)
      } else {
        var movieTitle = parsed.title.ifBlank { cleanTitle.substringBeforeLast('.') }
        var movieYear = parsed.year
        if (isGenericVideoTitle(movieTitle)) {
          val movieFolderName = if (parentSeasonNum != null) grandParentName else parentName
          resolveFolderMedia(movieFolderName)?.let { (fTitle, fYear) ->
            movieTitle = fTitle
            movieYear = movieYear ?: fYear
          }
        }
        return movieKeyFor(movieTitle, movieYear)
      }
    }

    private fun resolveFolderMedia(folderName: String): Pair<String, String?>? {
      if (folderName.isBlank()) return null
      val trimmed = folderName.trim()
      val normalized = trimmed.lowercase(Locale.ROOT)
      if (normalized in GENERIC_FOLDER_NAMES) return null
      val parsed = MediaInfoParser.parse(trimmed)
      val title = parsed.title.ifBlank { trimmed }
      if (title.lowercase(Locale.ROOT) in GENERIC_FOLDER_NAMES) return null
      return Pair(title, parsed.year)
    }

    private fun isGenericVideoTitle(title: String): Boolean {
      val normalized = title.lowercase(Locale.ROOT).trim()
      return normalized.isBlank() || normalized in GENERIC_VIDEO_TITLES ||
        PART_OR_EPISODE_TITLE_REGEX.matches(normalized)
    }
  }

  constructor(
    context: Context,
    json: Json,
    seriesDetector: SeriesDetector,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
  ) : this(
    context = context,
    json = json,
    seriesDetector = seriesDetector,
    cacheFile = File(context.filesDir, CACHE_FILE_NAME),
    scope = scope,
  )

  constructor(
    json: Json,
    cacheFile: File,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    seriesDetector: SeriesDetector? = null,
  ) : this(
    context = null,
    json = json,
    seriesDetector = seriesDetector,
    cacheFile = cacheFile,
    scope = scope,
  )

  private val memoryCache = ConcurrentHashMap<String, SubtitleProfile>()
  private val cacheMutex = Mutex()
  private val persistMutex = Mutex()
  @Volatile
  private var isCacheLoaded = false

  init {
    scope.launch(Dispatchers.IO) {
      ensureCacheLoaded()
      MediaLibraryEvents.changes.collectLatest {
        runCatching { prune() }
      }
    }
  }

  suspend fun ensureCacheLoaded() {
    if (isCacheLoaded) return
    cacheMutex.withLock {
      if (isCacheLoaded) return@withLock
      withContext(Dispatchers.IO) {
        loadFromDisk()
      }
      isCacheLoaded = true
    }
  }

  private fun ensureCacheLoadedSync() {
    if (isCacheLoaded) return
    synchronized(this) {
      if (isCacheLoaded) return
      loadFromDisk()
      isCacheLoaded = true
    }
  }

  private fun loadFromDisk() {
    try {
      if (cacheFile.exists()) {
        val text = cacheFile.readText()
        if (text.isNotBlank()) {
          val map = try {
            json.decodeFromString<Map<String, SubtitleProfile>>(text)
          } catch (e: Exception) {
            Log.w(TAG, "Subtitle profile cache is corrupt; discarding it", e)
            runCatching { cacheFile.delete() }
            emptyMap()
          }
          memoryCache.putAll(map)
          Log.d(TAG, "Loaded ${memoryCache.size} subtitle profiles from disk")
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to read subtitle profile cache file", e)
    }
  }

  fun getProfile(key: String): SubtitleProfile? {
    ensureCacheLoadedSync()
    val canonical = canonicalKey(key)
    return memoryCache[canonical] ?: memoryCache[key]
  }

  fun getAllProfiles(): Map<String, SubtitleProfile> {
    ensureCacheLoadedSync()
    return memoryCache.toMap()
  }

  suspend fun saveProfile(key: String, subPos: Int, subScale: Float) {
    ensureCacheLoaded()
    val canonical = canonicalKey(key)
    val profile = SubtitleProfile(
      subPos = subPos,
      subScale = subScale,
      updatedAt = System.currentTimeMillis(),
    )
    memoryCache[canonical] = profile
    persistCache()
  }

  suspend fun deleteProfile(key: String) {
    ensureCacheLoaded()
    val canonical = canonicalKey(key)
    val removed1 = memoryCache.remove(canonical)
    val removed2 = memoryCache.remove(key)
    if (removed1 != null || removed2 != null) {
      persistCache()
    }
  }

  suspend fun clearAllProfiles() {
    ensureCacheLoaded()
    memoryCache.clear()
    persistCache()
  }

  private fun writeCacheAtomically(text: String) {
    val parentDir = cacheFile.parentFile ?: context?.filesDir ?: File(".")
    val tempFile = File(parentDir, "${cacheFile.name}.tmp")
    try {
      tempFile.writeText(text)
      try {
        Files.move(
          tempFile.toPath(),
          cacheFile.toPath(),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE,
        )
      } catch (_: Exception) {
        Files.move(
          tempFile.toPath(),
          cacheFile.toPath(),
          StandardCopyOption.REPLACE_EXISTING,
        )
      }
    } catch (e: Exception) {
      runCatching { tempFile.delete() }
      throw e
    }
  }

  private suspend fun persistCache() = withContext(Dispatchers.IO) {
    persistMutex.withLock {
      val snapshot = memoryCache.toMap()
      try {
        writeCacheAtomically(json.encodeToString(snapshot))
      } catch (e: Exception) {
        Log.w(TAG, "Failed to persist subtitle profile cache file", e)
      }
    }
  }

  suspend fun prune() = withContext(Dispatchers.IO) {
    val detector = seriesDetector ?: return@withContext
    val detected = runCatching { detector.detectLibrary() }.getOrNull() ?: return@withContext
    val presentSeriesIds = detected.series.map { it.id }.toSet()
    val presentMovieKeys = detected.movies.map {
      movieKeyFor(it.title, it.year)
    }.toSet()
    pruneStale(presentSeriesIds, presentMovieKeys)
  }

  suspend fun pruneStale(
    presentSeriesIds: Set<String>? = null,
    presentMovieKeys: Set<String>? = null,
  ) = withContext(Dispatchers.IO) {
    ensureCacheLoaded()
    var changed = false
    if (presentSeriesIds != null) {
      val keepSeries = presentSeriesIds.mapTo(HashSet()) { seriesKeyFor(it) }
      val iterator = memoryCache.keys.iterator()
      while (iterator.hasNext()) {
        val key = iterator.next()
        if (key.startsWith(SERIES_KEY_PREFIX) && !keepSeries.contains(key)) {
          iterator.remove()
          changed = true
        }
      }
    }
    if (presentMovieKeys != null) {
      val keepMovies = HashSet<String>()
      presentMovieKeys.forEach {
        keepMovies.add(it)
        keepMovies.add(it.substringBefore('@'))
      }
      val iterator = memoryCache.keys.iterator()
      while (iterator.hasNext()) {
        val key = iterator.next()
        if (key.startsWith(MOVIE_KEY_PREFIX) && !keepMovies.contains(key)) {
          iterator.remove()
          changed = true
        }
      }
    }
    if (changed) {
      Log.d(TAG, "Pruned stale subtitle profiles")
      persistCache()
    }
  }
}
