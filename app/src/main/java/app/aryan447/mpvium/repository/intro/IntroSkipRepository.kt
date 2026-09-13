package app.aryan447.mpvium.repository.intro

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.repository.wyzie.WyzieSearchRepository
import app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult
import app.aryan447.mpvium.utils.media.MediaInfoParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Describes a single skip window (intro / recap) for the currently playing media.
 */
@Serializable
data class IntroWindow(
  val startSeconds: Int,
  val endSeconds: Int,
  val type: IntroSegmentType,
) {
  fun contains(positionSeconds: Int): Boolean = positionSeconds in startSeconds until endSeconds
}

@Serializable
enum class IntroSegmentType { INTRO, RECAP }

private sealed interface CachedWindows {
  data class Present(val windows: List<IntroWindow>) : CachedWindows
  data object None : CachedWindows
}

@Serializable
private data class TheIntroDbSegment(
  @SerialName("start_ms") val startMs: Long? = null,
  @SerialName("end_ms") val endMs: Long? = null,
)

@Serializable
private data class TheIntroDbResponse(
  @SerialName("tmdb_id") val tmdbId: Int? = null,
  val type: String? = null,
  val intro: List<TheIntroDbSegment>? = null,
  val recap: List<TheIntroDbSegment>? = null,
)

/**
 * Fetches intro/recap skip timestamps from TheIntroDB (keyless API) and caches
 * them in memory plus a disk-backed JSON file per media title. Media identity
 * is derived from the filename. Disk entries live until the underlying
 * episode/movie is deleted (evicted explicitly on delete or pruned on rescan),
 * so a previously fetched skip window works offline. Only successful lookups
 * are persisted; misses are kept in memory so a later DB entry can be picked
 * up when online.
 */
class IntroSkipRepository(
  private val context: Context,
  private val client: OkHttpClient,
  private val json: Json,
  private val wyzieRepository: WyzieSearchRepository,
  private val metadataRepository: StreamingMetadataRepository,
) {
  companion object {
    private const val TAG = "IntroSkipRepository"
    private const val THE_INTRO_DB_BASE = "https://api.theintrodb.org/v3/media"
    private const val DISK_CACHE_FILE_NAME = "intro_skip_cache_v1.json"

    fun normalizeKey(title: String): String =
      title.lowercase().replace(Regex("[^a-z0-9]"), "")
  }

  private val cache = ConcurrentHashMap<String, CachedWindows>()
  private val inflight = ConcurrentHashMap<String, Mutex>()
  private val diskFile = File(context.filesDir, DISK_CACHE_FILE_NAME)
  private val diskMutex = Mutex()
  private var diskLoaded = false

  /**
   * Resolves all skip windows (intro, recap) for [mediaTitle]. Returns empty list when
   * the media cannot be identified or no intro/recap data exists. Previously
   * fetched windows are served from memory or disk without network access.
   */
  suspend fun getSkipWindows(mediaTitle: String): List<IntroWindow> {
    if (mediaTitle.isBlank()) return emptyList()
    val key = normalizeKey(mediaTitle)

    when (val cached = cache[key]) {
      is CachedWindows.Present -> return cached.windows
      is CachedWindows.None -> return emptyList()
      null -> Unit
    }

    ensureDiskLoaded()
    when (val cached = cache[key]) {
      is CachedWindows.Present -> return cached.windows
      is CachedWindows.None -> return emptyList()
      null -> Unit
    }

    val mutex = inflight.getOrPut(key) { Mutex() }

    return try {
      mutex.withLock {
        when (val cached = cache[key]) {
          is CachedWindows.Present -> return@withLock cached.windows
          is CachedWindows.None -> return@withLock emptyList()
          null -> Unit
        }

        val result = withContext(Dispatchers.IO) {
          fetchFromApi(mediaTitle)
        }
        if (result.isNotEmpty()) {
          cache[key] = CachedWindows.Present(result)
          persistDisk()
        } else {
          // Memory-only negative: a future IntroDB entry can still be picked
          // up when online instead of being frozen on disk forever.
          cache[key] = CachedWindows.None
        }
        result
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to get skip windows for '$mediaTitle'", e)
      cache[key] = CachedWindows.None
      emptyList()
    } finally {
      inflight.remove(key)
    }
  }

  /** Clears the in-memory cache and the persisted disk cache. */
  fun clearCache() {
    cache.clear()
    runCatching { diskFile.takeIf { it.exists() }?.delete() }
  }

  /**
   * Forgets the cached skip windows for a single episode/movie, e.g. right
   * after its file is deleted. Other entries are untouched.
   */
  suspend fun evict(mediaTitle: String) {
    if (mediaTitle.isBlank()) return
    ensureDiskLoaded()
    if (cache.remove(normalizeKey(mediaTitle)) != null) {
      persistDisk()
    }
  }

  /** Forgets cached skip windows for several deleted episodes/movies at once. */
  suspend fun evictAll(mediaTitles: Collection<String>) {
    if (mediaTitles.isEmpty()) return
    ensureDiskLoaded()
    var changed = false
    mediaTitles.forEach { title ->
      if (title.isNotBlank() && cache.remove(normalizeKey(title)) != null) {
        changed = true
      }
    }
    if (changed) persistDisk()
  }

  /**
   * Drops cached entries whose media no longer exists on device. Pass the
   * currently present episode/movie display names (the same values handed to
   * [getSkipWindows]); every cached key absent from that set is evicted.
   * Partial deletes keep surviving episodes: only fully-gone titles drop.
   */
  suspend fun pruneStale(presentMediaTitles: Set<String>) {
    ensureDiskLoaded()
    val keep = presentMediaTitles.mapTo(HashSet()) { normalizeKey(it) }
    var changed = false
    val iterator = cache.keys.iterator()
    while (iterator.hasNext()) {
      if (!keep.contains(iterator.next())) {
        iterator.remove()
        changed = true
      }
    }
    if (changed) {
      Log.d(TAG, "Pruned stale intro-skip entries")
      persistDisk()
    }
  }

  /**
   * Returns true when the current network is suitable for background
   * prefetching. With [requireUnmetered] (default) only Wi-Fi / unmetered
   * connections qualify, so mobile data is never burned by prefetch.
   * Playback-time lookups in [getSkipWindows] are unaffected and work on any
   * connection.
   */
  fun canPrefetch(requireUnmetered: Boolean = true): Boolean {
    return try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
      val network = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(network) ?: return false
      if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
      if (requireUnmetered && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
        return false
      }
      true
    } catch (e: Exception) {
      Log.w(TAG, "Failed to check network state for prefetch", e)
      false
    }
  }

  /**
   * Warms the disk cache for every title in [mediaTitles] that isn't cached
   * yet, so later playback works offline. Already-cached entries (memory or
   * disk) are skipped with no network call. Fetches run with bounded
   * parallelism ([concurrency]) and are cooperative with coroutine
   * cancellation. Misses are kept memory-only, so a later IntroDB entry can
   * still be picked up.
   */
  suspend fun prefetchAll(
    mediaTitles: Collection<String>,
    concurrency: Int = 3,
  ) {
    if (mediaTitles.isEmpty()) return
    ensureDiskLoaded()
    val pending = mediaTitles
      .filter { it.isNotBlank() && !cache.containsKey(normalizeKey(it)) }
      .distinctBy { normalizeKey(it) }
    if (pending.isEmpty()) return
    Log.d(TAG, "Prefetching intro-skip windows for ${pending.size} titles")
    val permits = Semaphore(concurrency.coerceIn(1, 6))
    coroutineScope {
      pending.map { title ->
        async(Dispatchers.IO) {
          permits.withPermit {
            ensureActive()
            runCatching { getSkipWindows(title) }
          }
        }
      }.awaitAll()
    }
    Log.d(TAG, "Intro-skip prefetch finished")
  }

  private suspend fun ensureDiskLoaded() {
    if (diskLoaded) return
    diskMutex.withLock {
      if (diskLoaded) return@withLock
      withContext(Dispatchers.IO) {
        try {
          if (diskFile.exists()) {
            val text = diskFile.readText()
            if (text.isNotBlank()) {
              val stored = json.decodeFromString<Map<String, List<IntroWindow>>>(text)
              stored.forEach { (key, windows) ->
                if (windows.isNotEmpty()) {
                  cache.putIfAbsent(key, CachedWindows.Present(windows))
                }
              }
              Log.d(TAG, "Loaded ${stored.size} intro-skip entries from disk cache")
            }
          }
        } catch (e: Exception) {
          Log.w(TAG, "Failed to read intro-skip disk cache", e)
        }
      }
      diskLoaded = true
    }
  }

  private suspend fun persistDisk() {
    val snapshot: Map<String, List<IntroWindow>> = diskMutex.withLock {
      cache.entries.mapNotNull { (key, value) ->
        (value as? CachedWindows.Present)?.let { key to it.windows }
      }.toMap()
    }
    withContext(Dispatchers.IO) {
      try {
        diskFile.writeText(json.encodeToString(snapshot))
      } catch (e: Exception) {
        Log.w(TAG, "Failed to persist intro-skip disk cache", e)
      }
    }
  }

  private suspend fun fetchFromApi(mediaTitle: String): List<IntroWindow> {
    val parsed = MediaInfoParser.parse(mediaTitle)
    if (parsed.title.isBlank()) return emptyList()

    val searchTitle = WyzieSearchRepository.TITLE_ALIASES[parsed.title.lowercase().trim()] ?: parsed.title

    val isTv = parsed.type == "tv" || parsed.season != null || parsed.episode != null
    val searchType = if (isTv) "tv" else "movie"

    // 1. Resolve TMDb/IMDb IDs
    val cachedTmdbId = metadataRepository.getCachedTmdbId(searchTitle) ?: metadataRepository.getCachedTmdbId(parsed.title)

    val queryParams = mutableListOf<String>()
    if (isTv) {
      parsed.season?.let { queryParams.add("season=$it") }
      parsed.episode?.let { queryParams.add("episode=$it") }
    }
    val suffix = if (queryParams.isNotEmpty()) "&" + queryParams.joinToString("&") else ""

    // Try query with cached tmdb_id if available
    if (cachedTmdbId != null) {
      val url = "$THE_INTRO_DB_BASE?tmdb_id=$cachedTmdbId$suffix"
      val windows = queryTheIntroDb(url, mediaTitle)
      if (windows.isNotEmpty()) return windows
    }

    // Search fresh candidates from Wyzie / Cinemeta if cached ID failed or was missing
    val searchMatch = runCatching {
      var results = wyzieRepository.searchMedia(searchTitle).getOrNull() ?: emptyList()
      if (results.isEmpty() && searchTitle != parsed.title) {
        results = wyzieRepository.searchMedia(parsed.title).getOrNull() ?: emptyList()
      }
      pickBestMatch(results, searchType, parsed.year, searchTitle)
    }.getOrNull()

    val freshTmdbId = searchMatch?.id
    val imdbId = searchMatch?.imdbId

    // Try query with freshly resolved tmdb_id (if different from cached)
    if (freshTmdbId != null && freshTmdbId != cachedTmdbId) {
      val url = "$THE_INTRO_DB_BASE?tmdb_id=$freshTmdbId$suffix"
      val windows = queryTheIntroDb(url, mediaTitle)
      if (windows.isNotEmpty()) return windows
    }

    // Try query with imdb_id if available
    if (imdbId != null) {
      val url = "$THE_INTRO_DB_BASE?imdb_id=$imdbId$suffix"
      val windows = queryTheIntroDb(url, mediaTitle)
      if (windows.isNotEmpty()) return windows
    }

    return emptyList()
  }

  private fun queryTheIntroDb(url: String, mediaTitle: String): List<IntroWindow> {
    return try {
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "mpvium/1.0")
        .build()
      client.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) {
          Log.d(TAG, "TheIntroDB returned ${resp.code} for '$mediaTitle' via $url")
          return emptyList()
        }
        val body = resp.body.string()
        val parsedResp = json.decodeFromString<TheIntroDbResponse>(body)
        collectWindows(parsedResp)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error fetching intro data for '$mediaTitle' from $url", e)
      emptyList()
    }
  }

  private fun collectWindows(response: TheIntroDbResponse): List<IntroWindow> {
    val list = mutableListOf<IntroWindow>()
    response.recap?.forEach { segment ->
      toWindow(segment, IntroSegmentType.RECAP)?.let { list.add(it) }
    }
    response.intro?.forEach { segment ->
      toWindow(segment, IntroSegmentType.INTRO)?.let { list.add(it) }
    }
    return list.sortedBy { it.startSeconds }
  }

  private fun toWindow(segment: TheIntroDbSegment, type: IntroSegmentType): IntroWindow? {
    val end = segment.endMs ?: return null
    val start = segment.startMs ?: 0L
    if (end <= start) return null
    return IntroWindow(
      startSeconds = (start / 1000).toInt(),
      endSeconds = (end / 1000).toInt(),
      type = type,
    )
  }

  private fun pickBestMatch(results: List<WyzieTmdbResult>, searchType: String, year: String?, targetTitle: String): WyzieTmdbResult? {
    val typed = results.filter { it.mediaType.equals(searchType, ignoreCase = true) }
    val pool = if (typed.isNotEmpty()) typed else results
    if (pool.isEmpty()) return null

    // Prefer exact title match ignoring case
    pool.firstOrNull { it.title.equals(targetTitle, ignoreCase = true) }?.let { return it }

    year?.let { y ->
      pool.firstOrNull { it.releaseYear == y }?.let { return it }
      pool.firstOrNull { it.releaseYear?.startsWith(y.take(3)) == true }?.let { return it }
    }
    return pool.firstOrNull()
  }
}
