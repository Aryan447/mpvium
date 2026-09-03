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
import app.aryan447.mpvium.repository.MediaFileRepository
import app.aryan447.mpvium.utils.media.MediaInfoParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

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
 */
class SeriesDetector(
  private val context: Context,
  private val database: MpviumDatabase,
) {
  companion object {
    private const val TAG = "SeriesDetector"
    private const val WATCHED_PERCENTAGE_THRESHOLD = 0.95f
    private const val MAX_FALLBACK_EPISODE_DURATION_MS = 90 * 60 * 1000L
    private val SEASON_FOLDER_REGEX = Regex("""(?:Season|S|Series|Specials)\s*(\d{1,2})?""", RegexOption.IGNORE_CASE)
    private val EPISODE_NUM_REGEX = Regex("""(?:^|[^\d])(?:E|EP|Episode|#)?\s*(\d{1,4})(?:[^\d]|$)""", RegexOption.IGNORE_CASE)
  }

  suspend fun detectLibrary(): DetectedMediaLibrary = withContext(Dispatchers.IO) {
    try {
      val folders = MediaFileRepository.getAllVideoFolders(context)
      val allVideos = mutableListOf<Video>()

      for (folder in folders) {
        val folderVideos = MediaFileRepository.getVideosInFolder(context, folder.bucketId)
        allVideos.addAll(folderVideos)
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
        val isWatched = (playbackState?.hasBeenWatched == true) || (progress >= WATCHED_PERCENTAGE_THRESHOLD)

        // Check if video is TV series or movie
        var isTvSeries = parsed.type == "tv" || parsed.season != null || parsed.episode != null
        var seriesTitle = parsed.title
        var seasonNum = parsed.season ?: 1
        var episodeNum = parsed.episode
        var isFolderFallback = false

        // Folder-level fallback heuristic (e.g. Breaking Bad / Season 1 / 01.mp4)
        if (!isTvSeries) {
          val parentFile = File(video.path).parentFile
          val parentName = parentFile?.name ?: ""
          val grandParentName = parentFile?.parentFile?.name ?: ""

          val seasonMatch = SEASON_FOLDER_REGEX.find(parentName)
          if (seasonMatch != null) {
            isTvSeries = true
            isFolderFallback = true
            seasonNum = seasonMatch.groupValues[1].toIntOrNull() ?: 1
            seriesTitle = if (grandParentName.isNotBlank() && grandParentName != "0" && !grandParentName.equals("emulated", true)) {
              MediaInfoParser.parse(grandParentName).title.ifBlank { grandParentName }
            } else {
              parsed.title
            }
            if (episodeNum == null) {
              val epMatch = EPISODE_NUM_REGEX.find(video.displayName)
              episodeNum = epMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
            }
          }
        }

        // A long video in a folder named "Season" is more likely a standalone movie
        // than an episode. Keep explicit episode filename matches untouched.
        if (isFolderFallback && durationMs >= MAX_FALLBACK_EPISODE_DURATION_MS) {
          isTvSeries = false
        }

        if (isTvSeries && seriesTitle.isNotBlank()) {
          val normalizedSeriesId = normalizeKey(seriesTitle)
          seriesTitleById.putIfAbsent(normalizedSeriesId, seriesTitle)
          if (parsed.year != null && seriesYearById[normalizedSeriesId] == null) {
            seriesYearById[normalizedSeriesId] = parsed.year
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
          if (positionMs > 10_000L && !isWatched && progress < WATCHED_PERCENTAGE_THRESHOLD) {
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
          val movieTitle = parsed.title.ifBlank { video.title }
          val movie = LocalMovie(
            video = video,
            title = movieTitle,
            year = parsed.year,
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
          if (positionMs > 10_000L && !isWatched && progress < WATCHED_PERCENTAGE_THRESHOLD) {
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

      // Sort Continue Watching by most recently played
      val sortedContinueWatching = continueWatchingList.sortedByDescending { it.lastPlayedTimestamp }

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

  private fun normalizeKey(title: String): String {
    return title.lowercase(Locale.getDefault())
      .replace(Regex("""[^a-z0-9]"""), "")
      .trim()
  }
}
