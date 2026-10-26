package app.aryan447.mpvium.preferences

import app.aryan447.mpvium.preferences.preference.PreferenceStore
import app.aryan447.mpvium.preferences.preference.getEnum

/**
 * Preferences for the Shows and Movies library tabs.
 */
class StreamingPreferences(
  preferenceStore: PreferenceStore,
) {
  // Sort orders for the Shows grid. Recently-added uses the newest
  // episode file date; no extra metadata or DB access needed.
  val seriesSort = preferenceStore.getEnum("streaming_series_sort", SeriesSort.NAME)

  // Sort orders for the Movies grid, all derived from local file data.
  val movieSort = preferenceStore.getEnum("streaming_movie_sort", MovieSort.NAME)
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
