package app.aryan447.mpvium.ui.browser.recentlyplayed

import app.aryan447.mpvium.database.entities.PlaylistEntity
import app.aryan447.mpvium.domain.media.model.Video

sealed class RecentlyPlayedItem {
  abstract val timestamp: Long

  data class VideoItem(
    val video: Video,
    override val timestamp: Long,
  ) : RecentlyPlayedItem()

  data class PlaylistItem(
    val playlist: PlaylistEntity,
    val videoCount: Int,
    val mostRecentVideoPath: String,
    override val timestamp: Long,
  ) : RecentlyPlayedItem()
}
