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

  private val cache = ConcurrentHashMap<String, IntroWindow?>()
  private val inflight = ConcurrentHashMap<String, Mutex>()

  /**
   * Resolves the skip window for [mediaTitle]. Returns null when the media cannot
   * be identified or no intro/recap data exists.
   */
  suspend fun getSkipWindow(mediaTitle: String): IntroWindow? {
    if (mediaTitle.isBlank()) return null
    val key = normalizeKey(mediaTitle)

    cache[key]?.let { return it }
    val existing = inflight[key]
    if (existing != null) {
      return existing.withLock { cache[key] }
    }
    val mutex = Mutex()
    inflight[key] = mutex

    return try {
      mutex.withLock {
        val cached = cache[key]
        if (cached != null) {
          return@withLock cached
        }
        withContext(Dispatchers.IO) {
          val result = fetchFromApi(mediaTitle)
          cache[key] = result
          result
        }
      }
    } catch (e: Exception) {
      cache[key] = null
      null
    } finally {
      inflight.remove(key)
    }
  }

  /** Clears the in-memory cache so the next lookup re-queries the API. */
  fun clearCache() {
    cache.clear()
  }

  private suspend fun fetchFromApi(mediaTitle: String): IntroWindow? {
    val parsed = MediaInfoParser.parse(mediaTitle)
    if (parsed.title.isBlank()) return null

    val isTv = parsed.type == "tv" || parsed.season != null || parsed.episode != null
    val searchType = if (isTv) "tv" else "movie"
    val tmdbId =
      // Prefer the already-resolved match (honors a manual fix on the series screen)
      metadataRepository.getCachedTmdbId(parsed.title)
        ?: runCatching {
            val results = wyzieRepository.searchMedia(parsed.title).getOrNull() ?: emptyList()
            pickBestMatch(results, searchType, parsed.year)?.id
          }.getOrNull()
    if (tmdbId == null) {
      Log.d(TAG, "Could not resolve TMDB id for '$mediaTitle'")
      return null
    }

    val url =
      buildString {
        append("$THE_INTRO_DB_BASE?tmdb_id=$tmdbId")
        if (isTv) {
          parsed.season?.let { append("&season=$it") }
          parsed.episode?.let { append("&episode=$it") }
        }
      }

    return try {
      val request = Request.Builder().url(url).build()
      client.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) {
          Log.d(TAG, "TheIntroDB returned ${resp.code} for '$mediaTitle'")
          return null
        }
        val body = resp.body?.string() ?: return null
        val parsedResp = json.decodeFromString<TheIntroDbResponse>(body)
        pickWindow(parsedResp)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error fetching intro data for '$mediaTitle'", e)
      null
    }
  }

  private fun pickWindow(response: TheIntroDbResponse): IntroWindow? {
    // Prefer intro, fall back to recap.
    response.intro
      ?.asSequence()
      ?.map { toWindow(it, IntroSegmentType.INTRO) }
      ?.firstOrNull { it != null }
      ?.let { return it }
    response.recap
      ?.asSequence()
      ?.map { toWindow(it, IntroSegmentType.RECAP) }
      ?.firstOrNull { it != null }
      ?.let { return it }
    return null
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

  private fun pickBestMatch(results: List<WyzieTmdbResult>, searchType: String, year: String?): WyzieTmdbResult? {
    val typed = results.filter { it.mediaType.equals(searchType, ignoreCase = true) }
    val pool = if (typed.isNotEmpty()) typed else results
    if (pool.isEmpty()) return null
    year?.let { y ->
      pool.firstOrNull { it.releaseYear == y }?.let { return it }
      pool.firstOrNull { it.releaseYear?.startsWith(y.take(3)) == true }?.let { return it }
    }
    return pool.firstOrNull()
  }
}
