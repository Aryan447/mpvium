package app.aryan447.mpvium.repository.intro

import android.util.Log
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.repository.wyzie.WyzieSearchRepository
import app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult
import app.aryan447.mpvium.utils.media.MediaInfoParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

/**
 * Describes a single skip window (intro / recap) for the currently playing media.
 */
data class IntroWindow(
  val startSeconds: Int,
  val endSeconds: Int,
  val type: IntroSegmentType,
) {
  fun contains(positionSeconds: Int): Boolean = positionSeconds in startSeconds until endSeconds
}

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
 * them in memory per media title. Media identity is derived from the filename.
 */
class IntroSkipRepository(
  private val client: OkHttpClient,
  private val json: Json,
  private val wyzieRepository: WyzieSearchRepository,
  private val metadataRepository: StreamingMetadataRepository,
) {
  companion object {
    private const val TAG = "IntroSkipRepository"
    private const val THE_INTRO_DB_BASE = "https://api.theintrodb.org/v3/media"
  }

  private val cache = ConcurrentHashMap<String, CachedWindows>()
  private val inflight = ConcurrentHashMap<String, Mutex>()

  /**
   * Resolves all skip windows (intro, recap) for [mediaTitle]. Returns empty list when
   * the media cannot be identified or no intro/recap data exists.
   */
  suspend fun getSkipWindows(mediaTitle: String): List<IntroWindow> {
    if (mediaTitle.isBlank()) return emptyList()
    val key = normalizeKey(mediaTitle)

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
        cache[key] = if (result.isNotEmpty()) CachedWindows.Present(result) else CachedWindows.None
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

  /** Clears the in-memory cache so the next lookup re-queries the API. */
  fun clearCache() {
    cache.clear()
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

  private fun normalizeKey(title: String): String =
    title.lowercase().replace(Regex("[^a-z0-9]"), "")

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
