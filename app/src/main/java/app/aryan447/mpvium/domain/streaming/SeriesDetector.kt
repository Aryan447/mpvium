package app.aryan447.mpvium.domain.streaming

import android.content.Context
import android.util.Log
import app.aryan447.mpvium.database.MpviumDatabase
import app.aryan447.mpvium.database.entities.PlaybackStateEntity
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.media.model.VideoFolder
import app.aryan447.mpvium.domain.streaming.model.ContinueWatchingItem
import app.aryan447.mpvium.domain.streaming.model.LocalEpisode
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.preferences.BrowserPreferences
import app.aryan447.mpvium.preferences.FolderVisibility
import app.aryan447.mpvium.preferences.FoldersPreferences
import app.aryan447.mpvium.repository.MediaFileRepository
import app.aryan447.mpvium.utils.media.MediaInfoParser
import app.aryan447.mpvium.utils.media.ParsedMediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Result of scanning and detecting local media structure.
 */
data class DetectedMediaLibrary(
  val series: List<LocalSeries>,
  val movies: List<LocalMovie>,
  val continueWatching: List<ContinueWatchingItem>,
  val folders: List<VideoFolder>,
  val totalVideoCount: Int,
)

/**
 * Scans all device storage to detect TV Series, Seasons, Episodes, and Movies.
 *
 * Library scope (issue #87): the whitelist-only filter applies here (Home,
 * Shows, Movies, Insights). The Folders/Files full-storage browsers ignore
 * the whitelist and apply only the blacklist.
 */
class SeriesDetector(
  private val context: Context,
  private val database: MpviumDatabase,
) : KoinComponent {
  private val foldersPreferences: FoldersPreferences by inject()
  private val browserPreferences: BrowserPreferences by inject()
  companion object {
    private const val TAG = "SeriesDetector"
    private const val DEFAULT_WATCHED_PERCENTAGE_THRESHOLD = 0.95f
    private const val MAX_FALLBACK_EPISODE_DURATION_MS = 90 * 60 * 1000L
    // Require a number after the season token so ordinary folders that merely
    // start with "S" (Spider-Man, Star Wars, Sci-Fi) are not mistaken for
    // seasons and turned into fake shows.
    private val SEASON_FOLDER_REGEX = Regex("""\b(?:Season|Series|Specials|S)[\s._-]*(\d{1,2})\b""", RegexOption.IGNORE_CASE)
    private val SPECIALS_FOLDER_REGEX = Regex("""^(?:Specials|Extras)$""", RegexOption.IGNORE_CASE)
    private val EPISODE_NUM_REGEX = Regex("""(?:^|[^\d])(?:E|EP|Episode|#)?\s*(\d{1,4})(?:[^\d]|$)""", RegexOption.IGNORE_CASE)

    // Folders that describe a library root rather than one title. They must
    // never become a show/movie name.
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

    // "01", "CD1", "Part 2", "Episode 5" are episode/part labels, not titles.
    private val PART_OR_EPISODE_TITLE_REGEX =
      Regex("""(?:cd|disc|disk|part|pt|episode|ep|e)?[\s._-]*\d{1,3}""", RegexOption.IGNORE_CASE)
  }

  suspend fun detectLibrary(): DetectedMediaLibrary = withContext(Dispatchers.IO) {
    try {
      val whitelist = foldersPreferences.whitelistedFolders.get()
      val blacklist = foldersPreferences.blacklistedFolders.get()
      val whitelistOnly = foldersPreferences.whitelistOnlyEnabled.get()
      // User-configurable watched threshold (50-100%). Continue Watching and
      // Up Next must honor the same value used when saving playback state,
      // otherwise e.g. 90% stays stuck in Continue Watching (issue #94).
      val watchedThreshold = runCatching {
        browserPreferences.watchedThreshold.get().coerceIn(1, 100) / 100f
      }.getOrDefault(DEFAULT_WATCHED_PERCENTAGE_THRESHOLD)
      val folders = MediaFileRepository.getAllVideoFolders(context).filter { folder ->
        FolderVisibility.isFolderVisible(folder.path, whitelist, blacklist, whitelistOnly)
      }
      val allVideos = mutableListOf<Video>()

      for (folder in folders) {
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

      val playbackStates = try {
        database.videoDataDao().getAllPlaybackStates().associateBy { it.mediaTitle }
      } catch (e: Exception) {
        Log.w(TAG, "Error fetching playback states", e)
        emptyMap()
      }

      val recentPlayedList = try {
        database.recentlyPlayedDao().getAllRecentlyPlayed()
      } catch (e: Exception) {
        Log.w(TAG, "Error fetching recently played", e)
        emptyList()
      }
      val recentTimestampByFileName = recentPlayedList.associate { it.fileName to it.timestamp }

      val rawSeriesMap = mutableMapOf<String, MutableList<Pair<LocalEpisode, Video>>>()
      val seriesTitleById = mutableMapOf<String, String>()
      val seriesYearById = mutableMapOf<String, String?>()
      val detectedMovies = mutableListOf<LocalMovie>()
      val continueWatchingList = mutableListOf<ContinueWatchingItem>()

      for (video in allVideos) {
        val parsed = MediaInfoParser.parse(video.displayName)
        val playbackState = playbackStates[video.displayName] ?: playbackStates[video.title]
        val positionMs = (playbackState?.lastPosition ?: 0) * 1000L
        val durationMs = video.duration
        val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        val isWatched = (playbackState?.hasBeenWatched == true) || (progress >= watchedThreshold)

        val parentFile = File(video.path).parentFile
        val parentName = parentFile?.name ?: ""
        val grandParentName = parentFile?.parentFile?.name ?: ""
        val seasonMatch = SEASON_FOLDER_REGEX.find(parentName)
        val isSpecialsFolder = SPECIALS_FOLDER_REGEX.matches(parentName.trim())
        val parentSeasonNum = when {
          seasonMatch != null -> seasonMatch.groupValues[1].toIntOrNull() ?: 1
          isSpecialsFolder -> 0
          else -> null
        }

        // Check if video is TV series or movie
        var isTvSeries = parsed.type == "tv" || parsed.season != null || parsed.episode != null
        var seriesTitle = parsed.title
        var seriesYear = parsed.year
        var seasonNum = parsed.season ?: parentSeasonNum ?: 1
        var episodeNum = parsed.episode
        var isFolderFallback = false

        // Folder-level fallback heuristic (e.g. Breaking Bad / Season 1 / 01.mp4)
        if (!isTvSeries && parentSeasonNum != null) {
          isTvSeries = true
          isFolderFallback = true
          seasonNum = parentSeasonNum
          resolveFolderMedia(grandParentName)?.let { folder ->
            seriesTitle = folder.title
            seriesYear = seriesYear ?: folder.year
          }
          if (episodeNum == null) {
            val epMatch = EPISODE_NUM_REGEX.find(video.displayName)
            episodeNum = epMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
          }
        }

        // A bare episode number directly inside a show folder ("Show/01.mkv")
        // is an episode, not a one-file movie. "CD1"/"Part 1" stay movies.
        // Three digits max: "1917.mkv"/"2012.mkv" are film titles, not episodes.
        if (!isTvSeries && parentSeasonNum == null &&
          parsed.title.matches(Regex("""\d{1,3}""")) &&
          (durationMs == 0L || durationMs < MAX_FALLBACK_EPISODE_DURATION_MS)
        ) {
          resolveFolderMedia(parentName)?.let { folder ->
            isTvSeries = true
            isFolderFallback = true
            seriesTitle = folder.title
            seriesYear = seriesYear ?: folder.year
            seasonNum = 1
            episodeNum = parsed.title.toIntOrNull() ?: 1
          }
        }

        // A long video in a folder named "Season" is more likely a standalone movie
        // than an episode. Keep explicit episode filename matches untouched.
        if (isFolderFallback && durationMs >= MAX_FALLBACK_EPISODE_DURATION_MS) {
          isTvSeries = false
        }

        // Bare filenames like "S01E01.mkv" or "Episode 1.mkv" carry no show
        // name. Recover it from the show/season folder instead of showing the
        // file as a movie with a meaningless cover.
        if (isTvSeries && seriesTitle.isBlank()) {
          val primaryFolder = if (parentSeasonNum != null) grandParentName else parentName
          val secondaryFolder = if (parentSeasonNum != null) "" else grandParentName
          val folder = resolveFolderMedia(primaryFolder) ?: resolveFolderMedia(secondaryFolder)
          if (folder != null) {
            seriesTitle = folder.title
            seriesYear = seriesYear ?: folder.year
          }
          if (episodeNum == null) {
            episodeNum = EPISODE_NUM_REGEX.find(video.displayName)
              ?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
          }
        }

        if (isTvSeries && seriesTitle.isNotBlank()) {
          val normalizedSeriesId = normalizeKey(seriesTitle)
          seriesTitleById.putIfAbsent(normalizedSeriesId, seriesTitle)
          if (seriesYear != null && seriesYearById[normalizedSeriesId] == null) {
            seriesYearById[normalizedSeriesId] = seriesYear
          }

          val finalEpisodeNum = episodeNum ?: (rawSeriesMap[normalizedSeriesId]?.size?.plus(1) ?: 1)
          val episode = LocalEpisode(
            video = video,
            seasonNumber = seasonNum,
            episodeNumber = finalEpisodeNum,
            episodeTitle = parsed.episodeTitle,
            stillUrl = null,
            overview = null,
            rating = null,
            playbackPositionMs = positionMs,
            isWatched = isWatched,
            progressPercentage = progress,
          )

          rawSeriesMap.getOrPut(normalizedSeriesId) { mutableListOf() }.add(Pair(episode, video))

          // Continue Watching for in-progress series episode
          if (positionMs > 10_000L && !isWatched && progress < watchedThreshold) {
            val remainingMins = (durationMs - positionMs) / (1000 * 60)
            val episodeTag = episode.formattedEpisodeTag
            val subtitleText = if (remainingMins > 0) "$episodeTag • ${remainingMins}m remaining" else episodeTag

            continueWatchingList.add(
              ContinueWatchingItem(
                video = video,
                title = seriesTitle,
                subtitle = subtitleText,
                playbackPositionMs = positionMs,
                totalDurationMs = durationMs,
                progressPercentage = progress,
                lastPlayedTimestamp = recentTimestampByFileName[video.displayName] ?: video.dateModified * 1000,
                isSeries = true,
                seriesId = normalizedSeriesId,
              )
            )
          }
        } else {
          // Movie / Standalone video
          var movieTitle = parsed.title.ifBlank { video.title }
          var movieYear = parsed.year
          // "movie.mkv", "01.mkv", "CD1.mkv" etc. tell us nothing; the folder
          // name ("Inception (2010)") is a much better metadata query.
          if (shouldUseFolderTitle(movieTitle)) {
            val movieFolderName = if (parentSeasonNum != null) grandParentName else parentName
            resolveFolderMedia(movieFolderName)?.let { folder ->
              movieTitle = folder.title
              movieYear = movieYear ?: folder.year
            }
          }
          val movie = LocalMovie(
            video = video,
            title = movieTitle,
            year = movieYear,
            posterUrl = null,
            backdropUrl = null,
            rating = null,
            overview = null,
            playbackPositionMs = positionMs,
            isWatched = isWatched,
            progressPercentage = progress,
          )
          detectedMovies.add(movie)

          // Continue Watching for in-progress movie
          if (positionMs > 10_000L && !isWatched && progress < watchedThreshold) {
            val remainingMins = (durationMs - positionMs) / (1000 * 60)
            continueWatchingList.add(
              ContinueWatchingItem(
                video = video,
                title = movieTitle,
                subtitle = if (remainingMins > 0) "${remainingMins}m remaining" else "In Progress",
                playbackPositionMs = positionMs,
                totalDurationMs = durationMs,
                progressPercentage = progress,
                lastPlayedTimestamp = recentTimestampByFileName[video.displayName] ?: video.dateModified * 1000,
                isSeries = false,
                seriesId = null,
              )
            )
          }
        }
      }

      // Build consolidated LocalSeries objects
      val detectedSeriesList = rawSeriesMap.map { (seriesId, episodePairs) ->
        val title = seriesTitleById[seriesId] ?: seriesId
        val year = seriesYearById[seriesId]

        // Group by season and sort episodes
        val seasonsMap = episodePairs
          .map { it.first }
          .groupBy { it.seasonNumber }
          .mapValues { (_, episodes) ->
            episodes.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
          }
          .toSortedMap()

        val allEpisodes = seasonsMap.values.flatten()
        val totalEpisodes = allEpisodes.size
        val totalDuration = allEpisodes.sumOf { it.video.duration }
        val watchedCount = allEpisodes.count { it.isWatched }

        // Determine last watched and next episode to watch
        val lastWatched = allEpisodes
          .filter { it.playbackPositionMs > 0 }
          .maxByOrNull { episode ->
            recentTimestampByFileName[episode.video.displayName] ?: episode.video.dateModified
          }

        val nextToWatch = if (lastWatched != null) {
          if (!lastWatched.isWatched) {
            // Still in progress, resume same episode
            lastWatched
          } else {
            // Find next episode sequentially
            val currentIndex = allEpisodes.indexOf(lastWatched)
            if (currentIndex in 0 until allEpisodes.size - 1) {
              allEpisodes[currentIndex + 1]
            } else {
              lastWatched
            }
          }
        } else {
          allEpisodes.firstOrNull()
        }

        LocalSeries(
          id = seriesId,
          title = title,
          year = year,
          posterUrl = null,
          backdropUrl = null,
          rating = null,
          overview = null,
          genres = emptyList(),
          tmdbId = null,
          totalEpisodes = totalEpisodes,
          totalDurationMs = totalDuration,
          watchedEpisodesCount = watchedCount,
          lastWatchedEpisode = lastWatched,
          nextEpisodeToWatch = nextToWatch,
          seasons = seasonsMap,
        )
      }.sortedByDescending { it.totalEpisodes }

      // Keep a series in Continue Watching while unwatched episodes remain.
      // Without this, deleting (or finishing) the in-progress episode drops
      // the whole series even though the next episode is still waiting.
      val videosAlreadyListed = continueWatchingList.map { it.video.path }.toSet()
      for (series in detectedSeriesList) {
        val lastWatched = series.lastWatchedEpisode ?: continue
        val next = series.nextEpisodeToWatch ?: continue
        if (next.isWatched) continue
        if (videosAlreadyListed.contains(next.video.path)) continue
        val tag = next.formattedEpisodeTag
        continueWatchingList.add(
          ContinueWatchingItem(
            video = next.video,
            title = series.title,
            subtitle = if (tag.isNotBlank()) "Up next • $tag" else "Up next",
            playbackPositionMs = 0L,
            totalDurationMs = next.video.duration,
            progressPercentage = 0f,
            lastPlayedTimestamp = recentTimestampByFileName[lastWatched.video.displayName]
              ?: lastWatched.video.dateModified * 1000,
            isSeries = true,
            seriesId = series.id,
          )
        )
      }

      // Honor "Remove from Continue Watching" dismissals. Entries reappear
      // on their own once newer playback activity bumps their timestamp.
      val visibleContinueWatching =
        continueWatchingList.filterNot { ContinueWatchingDismissals.isDismissed(it) }

      // Sort Continue Watching by most recently played
      val sortedContinueWatching = visibleContinueWatching.sortedByDescending { it.lastPlayedTimestamp }

      DetectedMediaLibrary(
        series = detectedSeriesList,
        movies = detectedMovies.sortedBy { it.title.lowercase(Locale.getDefault()) },
        continueWatching = sortedContinueWatching,
        folders = folders,
        totalVideoCount = allVideos.size,
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error detecting media library", e)
      DetectedMediaLibrary(
        series = emptyList(),
        movies = emptyList(),
        continueWatching = emptyList(),
        folders = emptyList(),
        totalVideoCount = 0,
      )
    }
  }

  /**
   * Parses a containing folder as a title, rejecting library roots such as
   * "Movies", "TV Shows" or "Downloads" that would otherwise become covers.
   */
  private fun resolveFolderMedia(name: String): ParsedMediaInfo? {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return null
    val parsed = MediaInfoParser.parse(trimmed)
    val title = parsed.title.ifBlank { trimmed }
    val key = normalizeKey(title)
    if (key.isBlank() || key in GENERIC_FOLDER_NAMES) return null
    if (title.matches(Regex("""\d+"""))) return null
    return parsed.copy(title = title)
  }

  private fun shouldUseFolderTitle(title: String): Boolean {
    val normalized = title.lowercase(Locale.ROOT).trim()
    if (normalized.isBlank()) return true
    if (normalized in GENERIC_VIDEO_TITLES) return true
    return PART_OR_EPISODE_TITLE_REGEX.matches(normalized)
  }

  private fun normalizeKey(title: String): String {
    // Keep Unicode letters/digits. An ASCII-only filter maps every non-Latin
    // show to the same empty id, merging unrelated series (and their covers).
    return title.lowercase(Locale.ROOT)
      .replace(Regex("""[^\p{L}\p{N}]"""), "")
      .trim()
  }
}
