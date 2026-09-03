package app.aryan447.mpvium.domain.streaming

import android.content.Context
import android.util.Log
import app.aryan447.mpvium.domain.streaming.model.LocalEpisode
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.repository.wyzie.WyzieSearchRepository
import app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class CachedMediaMetadata(
  val tmdbId: Int? = null,
  val title: String,
  val posterUrl: String? = null,
  val backdropUrl: String? = null,
  val rating: Float? = null,
  val overview: String? = null,
  val genres: List<String> = emptyList(),
  val year: String? = null,
  val manuallySelected: Boolean = false, // true when the user explicitly chose this TMDb match
  val episodeStills: Map<String, String> = emptyMap(), // "S1E2" -> stillUrl
  val episodeTitles: Map<String, String> = emptyMap(), // "S1E2" -> title
  val episodeOverviews: Map<String, String> = emptyMap(), // "S1E2" -> overview
  val episodeRatings: Map<String, Float> = emptyMap(), // "S1E2" -> rating
)

/**
 * Fetches and caches TMDb metadata, posters, ratings, backdrops and episode info for series and movies.
 */
class StreamingMetadataRepository(
  private val context: Context,
  private val client: OkHttpClient,
  private val json: Json,
  private val wyzieRepository: WyzieSearchRepository,
) {
  companion object {
    private const val TAG = "StreamingMetadataRepo"
    private const val TMDB_IMAGE_BASE_W500 = "https://image.tmdb.org/t/p/w500"
    private const val TMDB_IMAGE_BASE_W780 = "https://image.tmdb.org/t/p/w780"
    private const val CACHE_FILE_NAME = "streaming_metadata_cache_v1.json"
  }

  private val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
  private val memoryCache = ConcurrentHashMap<String, CachedMediaMetadata>()
  private val cacheMutex = Mutex()
  private var isCacheLoaded = false

  private suspend fun ensureCacheLoaded() {
    if (isCacheLoaded) return
    cacheMutex.withLock {
      if (isCacheLoaded) return@withLock
      withContext(Dispatchers.IO) {
        try {
          if (cacheFile.exists()) {
            val text = cacheFile.readText()
            if (text.isNotBlank()) {
              val map = json.decodeFromString<Map<String, CachedMediaMetadata>>(text)
              memoryCache.putAll(map)
              Log.d(TAG, "Loaded ${memoryCache.size} metadata entries from disk cache")
            }
          }
        } catch (e: Exception) {
          Log.w(TAG, "Failed to read metadata cache file", e)
        }
        isCacheLoaded = true
      }
    }
  }

  private suspend fun persistCache() {
    cacheMutex.withLock {
      withContext(Dispatchers.IO) {
        try {
          val map = memoryCache.toMap()
          val text = json.encodeToString(map)
          cacheFile.writeText(text)
        } catch (e: Exception) {
          Log.w(TAG, "Failed to persist metadata cache file", e)
        }
      }
    }
  }

  suspend fun enrichSeries(
    series: LocalSeries,
    forceRefresh: Boolean = false,
    preferred: WyzieTmdbResult? = null,
  ) =
    withContext(Dispatchers.IO) {
      ensureCacheLoaded()
      val cacheKey = "series_${series.id}"
      val cached = memoryCache[cacheKey]

      if (cached != null && preferred == null && !forceRefresh) {
        return@withContext applyMetadataToSeries(series, cached)
      }

      // Fetch from TMDb / Wyzie
      try {
        var match = preferred
        var manualOverride = match != null
        if (match == null && cached?.manuallySelected == true && cached.tmdbId != null) {
          // Keep a previously chosen match across forced refreshes instead of re-picking
          match = WyzieTmdbResult(id = cached.tmdbId!!, mediaType = "tv", title = cached.title)
          manualOverride = true
        }
        if (match == null) {
          val searchResults = wyzieRepository.searchMedia(series.title).getOrNull() ?: emptyList()
          match = pickBestTvMatch(searchResults, series.year)
        }
        if (match == null) return@withContext series

        var poster = match.poster?.let { formatImageUrl(it, TMDB_IMAGE_BASE_W500) }
        var backdrop = match.backdrop?.let { formatImageUrl(it, TMDB_IMAGE_BASE_W780) }
        var overview = match.overview
        var year = match.releaseYear ?: series.year
        var rating: Float? = if (manualOverride) null else extractRatingFromTmdb(match.id, isTv = true)

        // Try getting full TV show details
        val showDetails = runCatching { wyzieRepository.getTvShowDetails(match.id).getOrNull() }.getOrNull()

        val episodeStills = mutableMapOf<String, String>()
        val episodeTitles = mutableMapOf<String, String>()
        val episodeOverviews = mutableMapOf<String, String>()
        val episodeRatings = mutableMapOf<String, Float>()

        // Fetch season episodes for all present seasons
        series.seasons.keys.forEach { seasonNum ->
          runCatching {
            val episodes = wyzieRepository.getSeasonEpisodes(match.id, seasonNum, match.imdbId).getOrNull() ?: emptyList()
            episodes.forEach { ep ->
              val epKeyStandard = "S${seasonNum}E${ep.episode_number}"
              val epKeyPadded = "S${seasonNum.toString().padStart(2, '0')}E${ep.episode_number.toString().padStart(2, '0')}"
              ep.name?.takeIf { it.isNotBlank() }?.let {
                episodeTitles[epKeyStandard] = it
                episodeTitles[epKeyPadded] = it
              }
              ep.still_path?.takeIf { it.isNotBlank() }?.let {
                val formatted = formatImageUrl(it, TMDB_IMAGE_BASE_W500)
                episodeStills[epKeyStandard] = formatted
                episodeStills[epKeyPadded] = formatted
              }
              ep.overview?.takeIf { it.isNotBlank() }?.let {
                episodeOverviews[epKeyStandard] = it
                episodeOverviews[epKeyPadded] = it
              }
            }
          }
        }

        val cachedMetadata = CachedMediaMetadata(
          tmdbId = match.id,
          title = match.title.ifBlank { series.title },
          posterUrl = poster,
          backdropUrl = backdrop,
          rating = rating,
          overview = overview,
          year = year,
          manuallySelected = manualOverride,
          episodeStills = episodeStills,
          episodeTitles = episodeTitles,
          episodeOverviews = episodeOverviews,
          episodeRatings = episodeRatings,
        )

        // A manually chosen match may only carry an id, so preserve artwork already in cache
        val mergedMetadata = if (manualOverride) {
          cachedMetadata.copy(
            posterUrl = poster ?: cached?.posterUrl,
            backdropUrl = backdrop ?: cached?.backdropUrl,
            overview = overview ?: cached?.overview,
            rating = rating ?: cached?.rating,
          )
        } else {
          cachedMetadata
        }

        memoryCache[cacheKey] = mergedMetadata
        persistCache()
        return@withContext applyMetadataToSeries(series, mergedMetadata)
      } catch (e: Exception) {
        Log.w(TAG, "Error fetching metadata for series: ${series.title}", e)
      }

      series
    }

  /**
   * Returns search candidates (series first, then movies) so the user can manually fix a match.
   */
  suspend fun searchSeriesMatches(title: String): List<WyzieTmdbResult> = withContext(Dispatchers.IO) {
    val results = wyzieRepository.searchMedia(title).getOrNull() ?: emptyList()
    results.sortedBy { it.mediaType != "tv" }
  }

  /**
   * Forgets the cached metadata (including any manual match) for a series so the next
   * enrichment runs automatic detection again.
   */
  suspend fun clearSeriesMetadata(seriesId: String) {
    memoryCache.remove("series_$seriesId")
    persistCache()
  }

  /**
   * Returns the already-resolved (and possibly manually chosen) TMDb id for a title,
   * so other features like intro skip can reuse the correct match instead of re-guessing.
   */
  suspend fun getCachedTmdbId(title: String): Int? = withContext(Dispatchers.IO) {
    ensureCacheLoaded()
    val normalized = title.lowercase().replace(Regex("[^a-z0-9]"), "")
    memoryCache["series_$normalized"]?.tmdbId
      ?: memoryCache["movie_$normalized"]?.tmdbId
  }

  private fun pickBestTvMatch(results: List<WyzieTmdbResult>, year: String?): WyzieTmdbResult? {
    val tvResults = results.filter { it.mediaType.equals("tv", ignoreCase = true) }
    val pool = if (tvResults.isNotEmpty()) tvResults else results
    if (pool.isEmpty()) return null
    year?.let { y ->
      pool.firstOrNull { it.releaseYear == y }?.let { return it }
      pool.firstOrNull { it.releaseYear?.startsWith(y.take(3)) == true }?.let { return it }
    }
    return pool.firstOrNull()
  }

  private fun pickBestMovieMatch(results: List<WyzieTmdbResult>, year: String?): WyzieTmdbResult? {
    val movieResults = results.filter { it.mediaType.equals("movie", ignoreCase = true) }
    val pool = if (movieResults.isNotEmpty()) movieResults else results
    if (pool.isEmpty()) return null
    year?.let { y ->
      pool.firstOrNull { it.releaseYear == y }?.let { return it }
      pool.firstOrNull { it.releaseYear?.startsWith(y.take(3)) == true }?.let { return it }
    }
    return pool.firstOrNull()
  }

  suspend fun enrichMovie(movie: LocalMovie, forceRefresh: Boolean = false) =
    withContext(Dispatchers.IO) {
      ensureCacheLoaded()
      val cacheKey = "movie_${movie.title.lowercase().replace(Regex("[^a-z0-9]"), "")}"
      val cached = memoryCache[cacheKey]

      if (cached != null && !forceRefresh) {
        return@withContext movie.copy(
          posterUrl = cached.posterUrl ?: movie.posterUrl,
          backdropUrl = cached.backdropUrl ?: movie.backdropUrl,
          rating = cached.rating ?: movie.rating,
          overview = cached.overview ?: movie.overview,
          year = cached.year ?: movie.year,
          tmdbId = cached.tmdbId ?: movie.tmdbId,
        )
      }

      try {
        val searchResults = wyzieRepository.searchMedia(movie.title).getOrNull() ?: emptyList()
        val bestMatch = pickBestMovieMatch(searchResults, movie.year)

        if (bestMatch != null) {
          val poster = bestMatch.poster?.let { formatImageUrl(it, TMDB_IMAGE_BASE_W500) }
          val backdrop = bestMatch.backdrop?.let { formatImageUrl(it, TMDB_IMAGE_BASE_W780) }
          val rating = extractRatingFromTmdb(bestMatch.id, isTv = false)

          val cachedMetadata = CachedMediaMetadata(
            tmdbId = bestMatch.id,
            title = bestMatch.title.ifBlank { movie.title },
            posterUrl = poster,
            backdropUrl = backdrop,
            rating = rating,
            overview = bestMatch.overview,
            year = bestMatch.releaseYear ?: movie.year,
          )

          memoryCache[cacheKey] = cachedMetadata
          persistCache()

          return@withContext movie.copy(
            posterUrl = poster,
            backdropUrl = backdrop,
            rating = rating,
            overview = bestMatch.overview,
            year = bestMatch.releaseYear ?: movie.year,
            tmdbId = bestMatch.id,
          )
        }
      } catch (e: Exception) {
        Log.w(TAG, "Error fetching metadata for movie: ${movie.title}", e)
      }

      movie
    }

  private fun applyMetadataToSeries(series: LocalSeries, metadata: CachedMediaMetadata): LocalSeries {
    val enrichedSeasons = series.seasons.mapValues { (seasonNum, episodes) ->
      episodes.map { ep ->
        val epKey = "S${seasonNum}E${ep.episodeNumber}"
        val epKeyPadded = "S${seasonNum.toString().padStart(2, '0')}E${ep.episodeNumber.toString().padStart(2, '0')}"
        ep.copy(
          episodeTitle = metadata.episodeTitles[epKey] ?: metadata.episodeTitles[epKeyPadded] ?: ep.episodeTitle,
          stillUrl = metadata.episodeStills[epKey] ?: metadata.episodeStills[epKeyPadded] ?: ep.stillUrl,
          overview = metadata.episodeOverviews[epKey] ?: metadata.episodeOverviews[epKeyPadded] ?: ep.overview,
          rating = metadata.episodeRatings[epKey] ?: metadata.episodeRatings[epKeyPadded] ?: ep.rating,
        )
      }
    }

    val allEnrichedEpisodes = enrichedSeasons.values.flatten()
    val lastWatched = allEnrichedEpisodes.find { it.video.id == series.lastWatchedEpisode?.video?.id }
    val nextToWatch = allEnrichedEpisodes.find { it.video.id == series.nextEpisodeToWatch?.video?.id }

    return series.copy(
      posterUrl = metadata.posterUrl ?: series.posterUrl,
      backdropUrl = metadata.backdropUrl ?: series.backdropUrl,
      rating = metadata.rating ?: series.rating,
      overview = metadata.overview ?: series.overview,
      year = metadata.year ?: series.year,
      tmdbId = metadata.tmdbId ?: series.tmdbId,
      seasons = enrichedSeasons,
      lastWatchedEpisode = lastWatched ?: series.lastWatchedEpisode,
      nextEpisodeToWatch = nextToWatch ?: series.nextEpisodeToWatch,
    )
  }

  private fun extractRatingFromTmdb(id: Int, isTv: Boolean): Float? {
    return try {
      val type = if (isTv) "tv" else "movie"
      val url = "https://sub.wyzie.ru/api/tmdb/$type/$id"
      val request = Request.Builder().url(url).build()
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        val jsonElement = json.decodeFromString<JsonObject>(body)
        jsonElement["vote_average"]?.jsonPrimitive?.floatOrNull
          ?: jsonElement["rating"]?.jsonPrimitive?.floatOrNull
      }
    } catch (e: Exception) {
      null
    }
  }

  private fun formatImageUrl(pathOrUrl: String, baseUrl: String): String {
    if (pathOrUrl.startsWith("http://", ignoreCase = true) || pathOrUrl.startsWith("https://", ignoreCase = true)) {
      return pathOrUrl
    }
    val cleanPath = if (pathOrUrl.startsWith("/")) pathOrUrl else "/$pathOrUrl"
    return "$baseUrl$cleanPath"
  }
}
