package app.aryan447.mpvium.domain.streaming

import app.aryan447.mpvium.domain.streaming.model.ContinueWatchingItem
import app.aryan447.mpvium.preferences.preference.PreferenceStore
import org.koin.java.KoinJavaComponent.inject

/**
 * Persists "Remove from Continue Watching" dismissals.
 *
 * An entry stays hidden until it sees playback newer than the dismissal, so
 * re-watching a dismissed show or movie naturally brings it back.
 */
object ContinueWatchingDismissals {
  private const val KEY_PREFIX = "cw_dismissed_"

  private val preferenceStore: PreferenceStore by inject(PreferenceStore::class.java)

  fun keyFor(item: ContinueWatchingItem): String =
    if (item.isSeries && item.seriesId != null) {
      "series:${item.seriesId}"
    } else {
      "movie:${item.video.path.ifBlank { item.video.uri.toString() }}"
    }

  /** Hidden while the item hasn't seen playback newer than the dismissal. */
  fun isDismissed(item: ContinueWatchingItem): Boolean {
    val dismissedAt = preferenceStore.getLong(KEY_PREFIX + keyFor(item), 0L).get()
    return dismissedAt > 0L && item.lastPlayedTimestamp <= dismissedAt
  }

  fun dismiss(item: ContinueWatchingItem) {
    preferenceStore.getLong(KEY_PREFIX + keyFor(item), 0L)
      .set(maxOf(System.currentTimeMillis(), item.lastPlayedTimestamp))
  }
}
