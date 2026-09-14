package app.aryan447.mpvium.utils.media

import android.util.Log
import app.aryan447.mpvium.database.entities.PlaybackStateEntity
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.playbackstate.repository.PlaybackStateRepository
import app.aryan447.mpvium.domain.recentlyplayed.repository.RecentlyPlayedRepository
import app.aryan447.mpvium.preferences.AppearancePreferences
import org.koin.java.KoinJavaComponent.inject

/**
 * Manual watch-status options, mirroring MX Player's "Mark as" feature.
 *
 * - [LAST_PLAYED]: highlight the video as recently played without changing its
 *   watched flag or resume position.
 * - [NEW]: drop all playback history so the video shows as unplayed
 *   and force the NEW badge even when the file is older than the
 *   "new video" days threshold (issue #47).
 * - [FINISHED]: mark the video as watched and clear any resume position.
 * - [CLEARED]: remove highlight/resume state and leave a neutral unplayed
 *   record so no status badge is shown.
 */
enum class ManualWatchStatus {
  LAST_PLAYED,
  NEW,
  FINISHED,
  CLEARED,
}

/**
 * Applies manual watch-status overrides to videos.
 *
 * Uses the existing [PlaybackStateEntity] / recently-played tables plus a
 * SharedPreferences-backed manual NEW set, so no database migration is needed.
 * Callers should refresh their lists afterwards;
 * [markVideos] already emits [MediaLibraryEvents.notifyChanged] once per batch.
 */
object VideoWatchStatusOps {
  private const val TAG = "VideoWatchStatusOps"
  private const val HISTORY_SOURCE = "video_list"

  private val playbackRepository: PlaybackStateRepository by inject(PlaybackStateRepository::class.java)
  private val recentlyPlayedRepository: RecentlyPlayedRepository by inject(RecentlyPlayedRepository::class.java)
  private val appearancePreferences: AppearancePreferences by inject(AppearancePreferences::class.java)

  /**
   * Whether [displayName] was manually marked as NEW and the override is still active.
   * Callers should additionally ensure the video has no watch progress before showing
   * the badge, so a stale override never hides real playback state.
   */
  fun isMarkedAsNew(displayName: String): Boolean {
    if (displayName.isBlank()) return false
    return runCatching { appearancePreferences.manuallyMarkedNewVideos.get().contains(displayName) }
      .getOrDefault(false)
  }

  /** Removes a manual NEW override, e.g. after the video is actually played. */
  fun clearManualNew(displayName: String) {
    if (displayName.isBlank()) return
    runCatching {
      val current = appearancePreferences.manuallyMarkedNewVideos.get()
      if (current.contains(displayName)) {
        appearancePreferences.manuallyMarkedNewVideos.set(current - displayName)
      }
    }
  }

  /** Migrates a manual NEW override across a filename change. */
  fun renameManualNew(oldDisplayName: String, newDisplayName: String) {
    if (oldDisplayName.isBlank() || newDisplayName.isBlank() || oldDisplayName == newDisplayName) return
    runCatching {
      val current = appearancePreferences.manuallyMarkedNewVideos.get()
      if (current.contains(oldDisplayName)) {
        appearancePreferences.manuallyMarkedNewVideos.set((current - oldDisplayName) + newDisplayName)
      }
    }
  }

  /**
   * Applies [status] to every video, returning how many succeeded.
   */
  suspend fun markVideos(
    videos: List<Video>,
    status: ManualWatchStatus,
  ): Int {
    if (videos.isEmpty()) return 0
    var applied = 0
    videos.forEach { video ->
      if (markVideo(video, status, notify = false)) applied++
    }
    if (applied > 0) MediaLibraryEvents.notifyChanged()
    return applied
  }

  /**
   * Applies [status] to a single video.
   */
  suspend fun markVideo(
    video: Video,
    status: ManualWatchStatus,
    notify: Boolean = true,
  ): Boolean {
    if (video.displayName.isBlank()) return false
    return try {
      when (status) {
        ManualWatchStatus.LAST_PLAYED -> markAsLastPlayed(video)
        ManualWatchStatus.NEW -> markAsNew(video)
        ManualWatchStatus.FINISHED -> markAsFinished(video)
        ManualWatchStatus.CLEARED -> clearStatus(video)
      }
      if (notify) MediaLibraryEvents.notifyChanged()
      true
    } catch (e: Exception) {
      Log.w(TAG, "Failed to mark ${video.displayName} as $status: ${e.message}")
      false
    }
  }

  private suspend fun markAsLastPlayed(video: Video) {
    // Touch (or create) a neutral playback record so the NEW badge clears,
    // but never alter the watched flag or an existing resume position here.
    val existing = playbackRepository.getVideoDataByTitle(video.displayName)
    if (existing == null) {
      playbackRepository.upsert(neutralState(video, existing = null))
    }
    clearManualNew(video.displayName)
    recentlyPlayedRepository.addRecentlyPlayed(
      filePath = video.path,
      fileName = video.displayName,
      videoTitle = video.title,
      duration = video.duration,
      fileSize = video.size,
      width = video.width,
      height = video.height,
      launchSource = HISTORY_SOURCE,
    )
    Log.d(TAG, "✓ Marked as last played: ${video.displayName}")
  }

  private suspend fun markAsNew(video: Video) {
    // Fully unplayed + forced NEW badge (issue #47). Deleting history alone only
    // shows NEW for recently-added files, so persist a manual override that the
    // UI honors regardless of file age.
    playbackRepository.deleteByTitle(video.displayName)
    recentlyPlayedRepository.deleteByFilePath(video.path)
    runCatching {
      val current = appearancePreferences.manuallyMarkedNewVideos.get()
      if (!current.contains(video.displayName)) {
        appearancePreferences.manuallyMarkedNewVideos.set(current + video.displayName)
      }
    }
    Log.d(TAG, "✓ Marked as new: ${video.displayName}")
  }

  private suspend fun markAsFinished(video: Video) {
    // Watched with no resume position left; track/subtitle prefs are kept.
    val existing = playbackRepository.getVideoDataByTitle(video.displayName)
    val finished =
      existing?.copy(
        lastPosition = 0,
        timeRemaining = 0,
        hasBeenWatched = true,
      )
        ?: PlaybackStateEntity(
          mediaTitle = video.displayName,
          lastPosition = 0,
          playbackSpeed = 1.0,
          videoZoom = 0f,
          sid = 0,
          secondarySid = -1,
          subDelay = 0,
          subSpeed = 1.0,
          aid = 0,
          audioDelay = 0,
          timeRemaining = 0,
          externalSubtitles = "",
          hasBeenWatched = true,
        )
    playbackRepository.upsert(finished)
    clearManualNew(video.displayName)
    Log.d(TAG, "✓ Marked as finished: ${video.displayName}")
  }

  private suspend fun clearStatus(video: Video) {
    // Plain unplayed record (no highlight, no resume, no NEW badge).
    playbackRepository.upsert(neutralState(video, playbackRepository.getVideoDataByTitle(video.displayName)))
    recentlyPlayedRepository.deleteByFilePath(video.path)
    clearManualNew(video.displayName)
    Log.d(TAG, "✓ Cleared status: ${video.displayName}")
  }

  private fun neutralState(
    video: Video,
    existing: PlaybackStateEntity?,
  ): PlaybackStateEntity {
    val durationSeconds = (video.duration / 1000).toInt().coerceAtLeast(0)
    return existing?.copy(
      lastPosition = 0,
      timeRemaining = durationSeconds,
      hasBeenWatched = false,
    ) ?: PlaybackStateEntity(
      mediaTitle = video.displayName,
      lastPosition = 0,
      playbackSpeed = 1.0,
      videoZoom = 0f,
      sid = 0,
      secondarySid = -1,
      subDelay = 0,
      subSpeed = 1.0,
      aid = 0,
      audioDelay = 0,
      timeRemaining = durationSeconds,
      externalSubtitles = "",
      hasBeenWatched = false,
    )
  }
}
