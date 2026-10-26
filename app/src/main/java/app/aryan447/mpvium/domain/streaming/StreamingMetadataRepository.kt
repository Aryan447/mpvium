package app.aryan447.mpvium.domain.streaming

import android.content.Context
import android.util.Log
import app.aryan447.mpvium.domain.streaming.model.LocalEpisode
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.repository.wyzie.WyzieMediaMatcher
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
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
  private val artworkStore: StreamingArtworkStore,
) {
  companion object {
    private const val TAG = "StreamingMetadataRepo"
    private const val TMDB_IMAGE_BASE_W500 = "https://image.tmdb.org/t/p/w500"
    private const val TMDB_IMAGE_BASE_W780 = "https://image.tmdb.org/t/p/w780"
    private const val TMDB_IMAGE_BASE_W1280 = "https://image.tmdb.org/t/p/w1280"
    // v3 re-resolves automatic matches with the stricter title/year matcher.
    // v2 entries could permanently pin a wrong poster because cache entries
    // have no TTL, so only user-picked matches are migrated.
    private const val CACHE_FILE_NAME = "streaming_metadata_cache_v3.json"
    private const val PREVIOUS_CACHE_FILE_NAME = "streaming_metadata_cache_v2.json"
    private const val LEGACY_CACHE_FILE_NAME = "streaming_metadata_cache_v1.json"

    // Cache keys are indefinite: entries live until the underlying media is
    // deleted (pruned on rescan or evicted explicitly on delete). No TTL.
    private const val SERIES_KEY_PREFIX = "series_"
    private const val MOVIE_KEY_PREFIX = "movie_"

    // Keep Unicode letters and digits: stripping everything outside a-z0-9
    // collapses all Japanese/Chinese/Korean/Arabic titles to the same cache
    // key, making every show share one poster (and every movie, one match).
    fun normalizeKey(title: String): String =
      title.lowercase(java.util.Locale.ROOT)
        .replace(Regex("""[^\p{L}\p{N}]"""), "")
        .trim()

    fun seriesKeyFor(seriesId: String): String = "$SERIES_KEY_PREFIX$seriesId"

    /**
     * Movie keys include the release year so same-name films (Dune 1984 and
     * Dune 2021, The Thing 1982 and The Thing 2011) do not share a poster.
     */
    fun movieKeyFor(movieTitle: String, year: String? = null): String {
      val base = "$MOVIE_KEY_PREFIX${normalizeKey(movieTitle)}"
      val normalizedYear = year?.filter { it.isDigit() }?.take(4)?.takeIf { it.length == 4 }
      return if (normalizedYear != null) "$base@$normalizedYear" else base
    }
  }

  // Phone-sized artwork (w500 posters, w780 backdrops) upscales ~2.4x on a
  // landscape tablet hero and looks blurry, so tablets fetch one size up.
  // Episode stills stay w500: they render at ~110dp everywhere.
  private val isTablet: Boolean =
    context.resources.configuration.smallestScreenWidthDp >= 600
  private val posterBase: String =
    if (isTablet) TMDB_IMAGE_BASE_W780 else TMDB_IMAGE_BASE_W500
  private val backdropBase: String =
    if (isTablet) TMDB_IMAGE_BASE_W1280 else TMDB_IMAGE_BASE_W780

  private val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
  private val memoryCache = ConcurrentHashMap<String, CachedMediaMetadata>()
  private val cacheMutex = Mutex()
  private val persistMutex = Mutex()
  private var isCacheLoaded = false

  private suspend fun ensureCacheLoaded() {
    if (isCacheLoaded) return
    cacheMutex.withLock {
      if (isCacheLoaded) return@withLock
      withContext(Dispatchers.IO) {
        try {
          // v1 cached phone-sized URLs; v2 auto-matches may be wrong. Drop
          // both, but carry forward matches the user picked by hand so their
          // manual corrections survive the matcher upgrade.
          if (!cacheFile.exists()) {
            migrateManualMatches()
          }
          runCatching { File(context.filesDir, LEGACY_CACHE_FILE_NAME).takeIf { it.exists() }?.delete() }
          runCatching { File(context.filesDir, PREVIOUS_CACHE_FILE_NAME).takeIf { it.exists() }?.delete() }
          if (cacheFile.exists()) {
            val text = cacheFile.readText()
            if (text.isNotBlank()) {
              val map = try {
                json.decodeFromString<Map<String, CachedMediaMetadata>>(text)
              } catch (e: Exception) {
                // A half-written file from an older build (or a killed
                // process) would otherwise force every launch to re-fetch
                // every cover. A transient read error is not handled here:
                // only a file that cannot be decoded is thrown away.
                Log.w(TAG, "Metadata cache is corrupt; discarding it", e)
                runCatching { cacheFile.delete() }
                emptyMap()
              }
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

  /** Carries user-picked matches from the previous cache generation. */
  private fun migrateManualMatches() {
    val previous = File(context.filesDir, PREVIOUS_CACHE_FILE_NAME)
    if (!previous.exists()) return
    try {
      val text = previous.readText()
      if (text.isBlank()) return
      val old = json.decodeFromString<Map<String, CachedMediaMetadata>>(text)
      val manual = old.filterValues { it.manuallySelected }
      if (manual.isNotEmpty()) {
        writeCacheAtomically(json.encodeToString(manual))
        Log.d(TAG, "Migrated ${manual.size} manually matched metadata entries")
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to migrate previous metadata cache", e)
    }
  }

  /**
   * Writes the whole cache file atomically.
   *
   * A partially written cache is worse than no cache at all: it loses every
   * poster URL. Writing to a sibling temp file and renaming keeps the previous
   * good file intact if the process dies mid-write.
   */
  private fun writeCacheAtomically(text: String) {
    val tempFile = File(context.filesDir, "$CACHE_FILE_NAME.tmp")
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
    // Serialize the snapshot and the write together. Taking the snapshot
    // outside the lock lets an older snapshot overwrite a newer one when two
    // enrichers finish close together.
    persistMutex.withLock {
      val snapshot: Map<String, CachedMediaMetadata> = cacheMutex.withLock {
        memoryCache.toMap()
      }
      try {
        writeCacheAtomically(json.encodeToString(snapshot))
      } catch (e: Exception) {
        Log.w(TAG, "Failed to persist metadata cache file", e)
      }
    }
  }

  /** Removes the durable artwork that belongs to a cleared/pruned entry. */
  private fun deleteArtwork(metadata: CachedMediaMetadata) {
    artworkStore.delete(artworkUrls(metadata))
  }

  /**
   * Drops files for artwork that a forced refresh/manual match replaced.
   * Keeps anything still referenced so an unchanged cover never blinks out.
   */
  private fun deleteReplacedArtwork(
    previous: CachedMediaMetadata?,
    current: CachedMediaMetadata,
  ) {
    if (previous == null) return
    val currentUrls = artworkUrls(current).toSet()
    val replaced = artworkUrls(previous).filterNot { it in currentUrls }
    artworkStore.delete(replaced)
  }

  private fun artworkUrls(metadata: CachedMediaMetadata): List<String> = buildList {
    metadata.posterUrl?.let(::add)
    metadata.backdropUrl?.let(::add)
    addAll(metadata.episodeStills.values)
  }

  suspend fun enrichSeries(
    series: LocalSeries,
    forceRefresh: Boolean = false,
    preferred: WyzieTmdbResult? = null,
  ) =
    withContext(Dispatchers.IO) {
      ensureCacheLoaded()
      val cacheKey = seriesKeyFor(series.id)
      val cached = memoryCache[cacheKey]

      // Cached entries are reused indefinitely (no TTL, no re-scrape on app
      // open). Only forceRefresh, a manual match pick, or deletion evicts.

      if (cached != null && preferred == null && !forceRefresh) {
        return@withContext applyMetadataToSeries(series, cached)
      }

      // Fetch from TMDb / Wyzie
      try {
        var match = preferred
        var manualOverride = match != null
        if (match == null && cached?.manuallySelected == true && cached.tmdbId != null) {
          // Keep a previously chosen match across forced refreshes instead of re-picking
          match = WyzieTmdbResult(id = cached.tmdbId, mediaType = "tv", title = cached.title)
          manualOverride = true
        }
        if (match == null) {
          val searchResults = wyzieRepository.searchMedia(series.title).getOrNull() ?: emptyList()
          match = WyzieMediaMatcher.pickBest(searchResults, "tv", series.title, series.year)
        }
        if (match == null) {
          return@withContext cached?.let { applyMetadataToSeries(series, it) } ?: series
        }

        var poster = match.poster?.let { formatImageUrl(it, posterBase) }
        var backdrop = match.backdrop?.let { formatImageUrl(it, backdropBase) }
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
        deleteReplacedArtwork(cached, mergedMetadata)
        persistCache()
        return@withContext applyMetadataToSeries(series, mergedMetadata)
      } catch (e: Exception) {
        Log.w(TAG, "Error fetching metadata for series: ${series.title}", e)
      }

      // A failed forced refresh must not blank a cover that is already cached.
      cached?.let { applyMetadataToSeries(series, it) } ?: series
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
   * enrichment runs automatic detection again. Called when every episode of the
   * show has been deleted.
   */
  suspend fun clearSeriesMetadata(seriesId: String) {
    ensureCacheLoaded()
    val removed = memoryCache.remove(seriesKeyFor(seriesId))
    if (removed != null) {
      withContext(Dispatchers.IO) { deleteArtwork(removed) }
      persistCache()
    }
  }

  /**
   * Drops cached entries for shows/movies that no longer exist on device.
   * Per-series entries survive partial episode deletes: only shows with zero
   * remaining episodes (absent from [presentSeriesIds]) are evicted.
   * Pass null for a media type to leave it untouched (e.g. series-only rescan).
   */
  suspend fun pruneStaleMetadata(
    presentSeriesIds: Set<String>? = null,
    presentMovieKeys: Set<String>? = null,
  ) {
    ensureCacheLoaded()
    var changed = false
    val removedEntries = mutableListOf<CachedMediaMetadata>()
    if (presentSeriesIds != null) {
      val keep = presentSeriesIds.mapTo(HashSet()) { seriesKeyFor(it) }
      val iterator = memoryCache.keys.iterator()
      while (iterator.hasNext()) {
        val key = iterator.next()
        if (key.startsWith(SERIES_KEY_PREFIX) && !keep.contains(key)) {
          memoryCache[key]?.let(removedEntries::add)
          iterator.remove()
          changed = true
        }
      }
    }
    if (presentMovieKeys != null) {
      // Keep both the year-qualified key and the title-only fallback used by
      // migrated manual matches.
      val keep = HashSet<String>()
      presentMovieKeys.forEach {
        keep.add(it)
        keep.add(it.substringBefore('@'))
      }
      val iterator = memoryCache.keys.iterator()
      while (iterator.hasNext()) {
        val key = iterator.next()
        if (key.startsWith(MOVIE_KEY_PREFIX) && !keep.contains(key)) {
          memoryCache[key]?.let(removedEntries::add)
          iterator.remove()
          changed = true
        }
      }
    }
    if (changed) {
      withContext(Dispatchers.IO) { removedEntries.forEach(::deleteArtwork) }
      Log.d(TAG, "Pruned stale metadata entries")
      persistCache()
    }
  }

  /**
   * Returns the already-resolved (and possibly manually chosen) TMDb id for a title,
   * so other features like intro skip can reuse the correct match instead of re-guessing.
   */
  suspend fun getCachedTmdbId(title: String, year: String? = null): Int? = withContext(Dispatchers.IO) {
    ensureCacheLoaded()
    val normalized = normalizeKey(title)
    memoryCache["series_$normalized"]?.tmdbId
      ?: memoryCache[movieKeyFor(title, year)]?.tmdbId
      ?: memoryCache["movie_$normalized"]?.tmdbId
  }

  /**
   * Local-cache-only episode title lookup for the player header.
   *
   * Reads the on-disk metadata cache populated by [enrichSeries] (kept until
   * all episodes of the show are deleted). Never hits the network, so a
   * missing entry simply returns null and the caller falls back to the
   * filename-parsed title.
   */
  suspend fun findCachedEpisodeTitle(showTitle: String, season: Int, episode: Int): String? =
    withContext(Dispatchers.IO) {
      ensureCacheLoaded()
      if (showTitle.isBlank()) return@withContext null
      val normalized = normalizeKey(showTitle)
      val standardKey = "S${season}E${episode}"
      val paddedKey = "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"

      // Direct hit: cache key is "series_" + normalized series id.
      memoryCache[seriesKeyFor(normalized)]?.let { cached ->
        cached.episodeTitles[standardKey]?.takeIf { it.isNotBlank() }?.let { return@withContext it }
        cached.episodeTitles[paddedKey]?.takeIf { it.isNotBlank() }?.let { return@withContext it }
      }

      // Fallback: cached TMDb display title may differ slightly from the
      // filename-parsed show name (e.g. manual match pick), so scan values.
      memoryCache.values.firstNotNullOfOrNull { cached ->
        if (normalizeKey(cached.title) != normalized) return@firstNotNullOfOrNull null
        cached.episodeTitles[standardKey]?.takeIf { it.isNotBlank() }
          ?: cached.episodeTitles[paddedKey]?.takeIf { it.isNotBlank() }
      }
    }

  /**
   * Returns search candidates (movies first, then series) so the user can manually fix a match for movies.
   */
  suspend fun searchMovieMatches(title: String): List<WyzieTmdbResult> = withContext(Dispatchers.IO) {
    val results = wyzieRepository.searchMedia(title).getOrNull() ?: emptyList()
    results.sortedBy { it.mediaType != "movie" }
  }

  /**
   * Forgets the cached metadata (including any manual match) for a movie so the next
   * enrichment runs automatic detection again. Called when the movie file is deleted.
   */
  suspend fun clearMovieMetadata(movieTitle: String) {
    ensureCacheLoaded()
    val baseKey = movieKeyFor(movieTitle)
    var changed = false
    val removedEntries = mutableListOf<CachedMediaMetadata>()
    val iterator = memoryCache.keys.iterator()
    while (iterator.hasNext()) {
      val key = iterator.next()
      if (key == baseKey || key.startsWith("$baseKey@")) {
        memoryCache[key]?.let(removedEntries::add)
        iterator.remove()
        changed = true
      }
    }
    if (changed) {
      withContext(Dispatchers.IO) { removedEntries.forEach(::deleteArtwork) }
      persistCache()
    }
  }

  suspend fun enrichMovie(
    movie: LocalMovie,
    forceRefresh: Boolean = false,
    preferred: WyzieTmdbResult? = null,
  ): LocalMovie =
    withContext(Dispatchers.IO) {
      ensureCacheLoaded()
      val cacheKey = movieKeyFor(movie.title, movie.year)
      val legacyKey = movieKeyFor(movie.title)
      var cached = memoryCache[cacheKey]

      // v2 stored one title-only entry per movie. If that entry was a manual
      // pick, move it to the year-qualified key so it keeps winning without
      // leaking to a same-name film from another year.
      if (cached == null && legacyKey != cacheKey) {
        val legacy = memoryCache.remove(legacyKey)
        if (legacy?.manuallySelected == true) {
          memoryCache[cacheKey] = legacy
          persistCache()
          cached = legacy
        }
      }

      // Cached entries are reused indefinitely (no TTL, no re-scrape on app
      // open). Only forceRefresh, a manual match pick, or deletion evicts.

      if (cached != null && preferred == null && !forceRefresh) {
        return@withContext applyMetadataToMovie(movie, cached)
      }

      try {
        var match = preferred
        var manualOverride = match != null
        if (match == null && cached?.manuallySelected == true && cached.tmdbId != null) {
          match = WyzieTmdbResult(id = cached.tmdbId, mediaType = "movie", title = cached.title)
          manualOverride = true
        }
        if (match == null) {
          val searchResults = wyzieRepository.searchMedia(movie.title).getOrNull() ?: emptyList()
          match = WyzieMediaMatcher.pickBest(searchResults, "movie", movie.title, movie.year)
        }

        if (match != null) {
          val poster = match.poster?.let { formatImageUrl(it, posterBase) }
          val backdrop = match.backdrop?.let { formatImageUrl(it, backdropBase) }
          val overview = match.overview
          val year = match.releaseYear ?: movie.year
          val rating = if (manualOverride) null else extractRatingFromTmdb(match.id, isTv = false)

          val cachedMetadata = CachedMediaMetadata(
            tmdbId = match.id,
            title = match.title.ifBlank { movie.title },
            posterUrl = poster,
            backdropUrl = backdrop,
            rating = rating,
            overview = overview,
            year = year,
            manuallySelected = manualOverride,
          )

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
          if (legacyKey != cacheKey) {
            memoryCache.remove(legacyKey)
          }
          deleteReplacedArtwork(cached, mergedMetadata)
          persistCache()

          return@withContext applyMetadataToMovie(movie, mergedMetadata)
        }
      } catch (e: Exception) {
        Log.w(TAG, "Error fetching metadata for movie: ${movie.title}", e)
      }

      // A failed forced refresh must not blank a cover that is already cached.
      cached?.let { applyMetadataToMovie(movie, it) } ?: movie
    }

  /**
   * Applies an already-resolved cached entry without touching the network.
   *
   * Grids call this before [enrichMovie] so a movie that was enriched on a
   * previous run renders its stored cover immediately instead of briefly
   * falling back to a video thumbnail (and looking like it was re-fetched).
   */
  suspend fun applyCachedMovie(movie: LocalMovie): LocalMovie = withContext(Dispatchers.IO) {
    ensureCacheLoaded()
    val cacheKey = movieKeyFor(movie.title, movie.year)
    val cached = memoryCache[cacheKey]
      ?: memoryCache[movieKeyFor(movie.title)]?.takeIf { it.manuallySelected }
      ?: return@withContext movie
    applyMetadataToMovie(movie, cached)
  }

  suspend fun applyCachedSeries(series: LocalSeries): LocalSeries = withContext(Dispatchers.IO) {
    ensureCacheLoaded()
    val cached = memoryCache[seriesKeyFor(series.id)] ?: return@withContext series
    applyMetadataToSeries(series, cached)
  }

  private fun applyMetadataToMovie(movie: LocalMovie, metadata: CachedMediaMetadata): LocalMovie =
    movie.copy(
      posterUrl = metadata.posterUrl ?: movie.posterUrl,
      backdropUrl = metadata.backdropUrl ?: movie.backdropUrl,
      rating = metadata.rating ?: movie.rating,
      overview = metadata.overview ?: movie.overview,
      year = metadata.year ?: movie.year,
      tmdbId = metadata.tmdbId ?: movie.tmdbId,
    )

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
        val body = response.body.string()
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
