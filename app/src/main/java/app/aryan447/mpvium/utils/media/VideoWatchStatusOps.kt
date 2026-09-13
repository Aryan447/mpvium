package app.aryan447.mpvium.utils.media

import android.util.Log
import app.aryan447.mpvium.database.entities.PlaybackStateEntity
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.playbackstate.repository.PlaybackStateRepository
import app.aryan447.mpvium.domain.recentlyplayed.repository.RecentlyPlayedRepository
import org.koin.java.KoinJavaComponent.inject

/**
 * Manual watch-status options, mirroring MX Player's "Mark as" feature.
 *
 * - [LAST_PLAYED]: highlight the video as recently played without changing its
 *   watched flag or resume position.
 * - [NEW]: drop all playback history so the video shows as unplayed
 *   (eligible for the NEW badge when recently added).
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
 * Only uses the existing [PlaybackStateEntity] / recently-played tables, so no
 * database migration is needed. Callers should refresh their lists afterwards;
 * [markVideos] already emits [MediaLibraryEvents.notifyChanged] once per batch.
 */
object VideoWatchStatusOps {
  private const val TAG = "VideoWatchStatusOps"
  private const val HISTORY_SOURCE = "video_list"

  private val playbackRepository: PlaybackStateRepository by inject(PlaybackStateRepository::class.java)
  private val recentlyPlayedRepository: RecentlyPlayedRepository by inject(RecentlyPlayedRepository::class.java)

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
    // Fully unplayed: eligible for the NEW badge when recently added.
    playbackRepository.deleteByTitle(video.displayName)
    recentlyPlayedRepository.deleteByFilePath(video.path)
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
    Log.d(TAG, "✓ Marked as finished: ${video.displayName}")
  }

  private suspend fun clearStatus(video: Video) {
    // Plain unplayed record (no highlight, no resume, no NEW badge).
    playbackRepository.upsert(neutralState(video, playbackRepository.getVideoDataByTitle(video.displayName)))
    recentlyPlayedRepository.deleteByFilePath(video.path)
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
