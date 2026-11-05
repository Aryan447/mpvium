package app.aryan447.mpvium.ui.streaming.titles

import androidx.compose.runtime.Immutable
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.ui.streaming.components.MovieDurationFilter

/**
 * Filter categories for the segment control on the Titles tab.
 */
enum class TitlesTypeFilter(val label: String) {
  ALL("All"),
  SHOWS("TV Shows"),
  MOVIES("Movies"),
}

/**
 * Normalized watch-state filter applicable across both shows and movies.
 */
enum class TitleWatchFilter(val label: String) {
  ALL("All"),
  IN_PROGRESS("In progress"),
  UNWATCHED("Unwatched"),
  WATCHED("Watched");

  fun matches(item: TitleItem): Boolean = when (this) {
    ALL -> true
    IN_PROGRESS -> when (item) {
      is TitleItem.Show -> item.series.watchedEpisodesCount > 0 && !item.series.isCompleted
      is TitleItem.Movie -> !item.movie.isWatched && item.movie.progressPercentage > 0f
    }
    UNWATCHED -> when (item) {
      is TitleItem.Show -> item.series.watchedEpisodesCount == 0
      is TitleItem.Movie -> !item.movie.isWatched && item.movie.progressPercentage <= 0f
    }
    WATCHED -> when (item) {
      is TitleItem.Show -> item.series.isCompleted
      is TitleItem.Movie -> item.movie.isWatched
    }
  }
}

/**
 * Sort options for the Titles grid and list.
 */
enum class TitlesSort(val label: String) {
  NAME("Name (A–Z)"),
  RECENTLY_ADDED("Recently added"),
  RATING("Highest rated"),
  LONGEST("Longest"),
  SHORTEST("Shortest"),
}

/**
 * Unified item wrapper representing either a TV show or a movie in the Titles tab.
 */
@Immutable
sealed interface TitleItem {
  val id: String
  val title: String
  val dateAdded: Long
  val rating: Float?

  @Immutable
  data class Show(val series: LocalSeries) : TitleItem {
    override val id: String get() = "show_${series.id}"
    override val title: String get() = series.title
    override val dateAdded: Long get() = series.seasons.values.flatten().maxOfOrNull { it.video.dateAdded } ?: 0L
    override val rating: Float? get() = series.rating
  }

  @Immutable
  data class Movie(val movie: LocalMovie) : TitleItem {
    override val id: String get() = "movie_${movie.video.id}"
    override val title: String get() = movie.title
    override val dateAdded: Long get() = movie.video.dateAdded
    override val rating: Float? get() = movie.rating
  }
}

/**
 * Comparator evaluating media length across shows (by episode count) and movies (by duration).
 */
object TitleLengthComparator : Comparator<TitleItem> {
  const val ESTIMATED_EPISODE_DURATION_MS: Long = 45 * 60 * 1000L

  override fun compare(a: TitleItem, b: TitleItem): Int {
    return when {
      a is TitleItem.Show && b is TitleItem.Show -> {
        a.series.totalEpisodes.compareTo(b.series.totalEpisodes)
      }
      a is TitleItem.Movie && b is TitleItem.Movie -> {
        a.movie.video.duration.compareTo(b.movie.video.duration)
      }
      a is TitleItem.Show && b is TitleItem.Movie -> {
        val showDuration = a.series.totalDurationMs.takeIf { it > 0 }
          ?: (a.series.totalEpisodes * ESTIMATED_EPISODE_DURATION_MS)
        val cmp = showDuration.compareTo(b.movie.video.duration)
        if (cmp != 0) cmp else a.series.totalEpisodes.compareTo(1)
      }
      a is TitleItem.Movie && b is TitleItem.Show -> {
        val showDuration = b.series.totalDurationMs.takeIf { it > 0 }
          ?: (b.series.totalEpisodes * ESTIMATED_EPISODE_DURATION_MS)
        val cmp = a.movie.video.duration.compareTo(showDuration)
        if (cmp != 0) cmp else 1.compareTo(b.series.totalEpisodes)
      }
      else -> 0
    }
  }
}

/**
 * Sorts a list of [TitleItem]s according to [TitlesSort].
 */
fun List<TitleItem>.sortedByTitlesSort(sort: TitlesSort): List<TitleItem> = when (sort) {
  TitlesSort.NAME -> sortedWith(
    compareBy<TitleItem> { it.title.lowercase() }
      .thenByDescending { it.dateAdded }
  )
  TitlesSort.RECENTLY_ADDED -> sortedWith(
    compareByDescending<TitleItem> { it.dateAdded }
      .thenBy { it.title.lowercase() }
  )
  TitlesSort.RATING -> sortedWith(
    compareByDescending<TitleItem> { it.rating ?: -1f }
      .thenBy { it.title.lowercase() }
  )
  TitlesSort.LONGEST -> sortedWith(TitleLengthComparator.reversed().thenBy { it.title.lowercase() })
  TitlesSort.SHORTEST -> sortedWith(TitleLengthComparator.thenBy { it.title.lowercase() })
}

/**
 * Checks whether this duration filter matches a [TitleItem].
 */
fun MovieDurationFilter.matches(item: TitleItem): Boolean = when (this) {
  MovieDurationFilter.ALL -> true
  else -> when (item) {
    is TitleItem.Movie -> matches(item.movie.video.duration)
    is TitleItem.Show -> matches(item.series.totalDurationMs)
  }
}
