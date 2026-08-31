package app.aryan447.mpvium.domain.streaming.model

import androidx.compose.runtime.Immutable
import app.aryan447.mpvium.domain.media.model.Video

/**
 * Represents a local TV series detected on device storage.
 */
@Immutable
data class LocalSeries(
  val id: String, // Normalized unique series slug / identifier
  val title: String,
  val year: String? = null,
  val posterUrl: String? = null,
  val backdropUrl: String? = null,
  val rating: Float? = null, // TMDb rating (0.0 to 10.0)
  val overview: String? = null,
  val genres: List<String> = emptyList(),
  val tmdbId: Int? = null,
  val totalEpisodes: Int = 0,
  val totalDurationMs: Long = 0L,
  val watchedEpisodesCount: Int = 0,
  val lastWatchedEpisode: LocalEpisode? = null,
  val nextEpisodeToWatch: LocalEpisode? = null,
  val seasons: Map<Int, List<LocalEpisode>> = emptyMap(), // Season Number -> Sorted List of Episodes
) {
  val seasonCount: Int get() = seasons.size
  val isCompleted: Boolean get() = totalEpisodes > 0 && watchedEpisodesCount >= totalEpisodes
  val progressPercentage: Float get() = if (totalEpisodes > 0) (watchedEpisodesCount.toFloat() / totalEpisodes).coerceIn(0f, 1f) else 0f
}

/**
 * Represents an individual episode in a detected TV series.
 */
@Immutable
data class LocalEpisode(
  val video: Video,
  val seasonNumber: Int,
  val episodeNumber: Int,
  val episodeTitle: String? = null,
  val stillUrl: String? = null,
  val overview: String? = null,
  val rating: Float? = null,
  val playbackPositionMs: Long = 0L,
  val isWatched: Boolean = false,
  val progressPercentage: Float = 0f,
) {
  val formattedEpisodeTag: String get() = "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
  val displayTitle: String get() = episodeTitle?.takeIf { it.isNotBlank() } ?: "Episode $episodeNumber"
}

/**
 * Represents a standalone movie detected on device storage.
 */
@Immutable
data class LocalMovie(
  val video: Video,
  val title: String,
  val year: String? = null,
  val posterUrl: String? = null,
  val backdropUrl: String? = null,
  val rating: Float? = null,
  val overview: String? = null,
  val genres: List<String> = emptyList(),
  val tmdbId: Int? = null,
  val playbackPositionMs: Long = 0L,
  val isWatched: Boolean = false,
  val progressPercentage: Float = 0f,
)

/**
 * Represents an item in the Continue Watching carousel.
 */
@Immutable
data class ContinueWatchingItem(
  val video: Video,
  val title: String,
  val subtitle: String, // e.g. "S1:E3 • The Spoils of War" or "Movie • 1h 45m left"
  val posterUrl: String? = null,
  val backdropUrl: String? = null,
  val playbackPositionMs: Long = 0L,
  val totalDurationMs: Long = 0L,
  val progressPercentage: Float = 0f,
  val lastPlayedTimestamp: Long = 0L,
  val isSeries: Boolean = false,
  val seriesId: String? = null,
)

/**
 * Filter categories on the streaming home screen.
 */
enum class StreamingCategory(val displayName: String) {
  ALL("All"),
  SERIES("Series"),
  MOVIES("Movies"),
  CONTINUE_WATCHING("In Progress"),
}
