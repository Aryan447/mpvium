package app.aryan447.mpvium.preferences

import app.aryan447.mpvium.preferences.preference.PreferenceStore
import app.aryan447.mpvium.preferences.preference.getEnum
import app.aryan447.mpvium.ui.streaming.components.MovieDurationFilter
import app.aryan447.mpvium.ui.streaming.titles.TitlesSort
import app.aryan447.mpvium.ui.streaming.titles.TitlesTypeFilter

/**
 * Preferences for the Shows and Movies library tabs.
 */
class StreamingPreferences(
  preferenceStore: PreferenceStore,
) {
  // Type filter for the unified Titles tab (All, Shows, Movies)
  val titlesTypeFilter = preferenceStore.getEnum("streaming_titles_type_filter", TitlesTypeFilter.ALL)

  // Sort order for the unified Titles tab
  val titlesSort = preferenceStore.getEnum("streaming_titles_sort", TitlesSort.NAME)

  // Sort orders for the Shows grid. Recently-added uses the newest
  // episode file date; no extra metadata or DB access needed.
  val seriesSort = preferenceStore.getEnum("streaming_series_sort", SeriesSort.NAME)

  // Sort orders for the Movies grid, all derived from local file data.
  val movieSort = preferenceStore.getEnum("streaming_movie_sort", MovieSort.NAME)

  // Duration filter for the Movies grid.
  val movieDurationFilter = preferenceStore.getEnum(
    "streaming_movie_duration_filter",
    MovieDurationFilter.ALL,
  )
}

/**
 * Sort orders for the Shows grid.
 */
enum class SeriesSort(val label: String) {
  NAME("Name (A–Z)"),
  RECENTLY_ADDED("Recently added"),
  PROGRESS("Most watched"),
}

/**
 * Sort orders for the Movies grid.
 */
enum class MovieSort(val label: String) {
  NAME("Name (A–Z)"),
  RECENTLY_ADDED("Recently added"),
  RATING("Highest rated"),
  LONGEST("Longest"),
  SHORTEST("Shortest"),
}
