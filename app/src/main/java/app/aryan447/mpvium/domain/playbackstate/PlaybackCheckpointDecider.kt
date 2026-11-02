package app.aryan447.mpvium.domain.playbackstate

import kotlin.math.abs

/**
 * Pure logic helper for deciding when to periodically checkpoint playback state
 * and calculating the position to persist while guarding against edge cases.
 */
object PlaybackCheckpointDecider {
  const val DEFAULT_MIN_POSITION_DELTA_SECONDS = 5

  /**
   * Decides whether a periodic checkpoint should be written while playing.
   *
   * @param isPlaying Whether the video is actively playing (false if paused or stopped)
   * @param currentPosition Current playback position in seconds
   * @param duration Media duration in seconds
   * @param lastSavedPosition Last successfully saved playback position in seconds
   * @param savePositionEnabled Whether position saving is enabled in preferences
   * @param minPositionDeltaSeconds Minimum position movement required to trigger checkpoint (default ~5s)
   * @return true if a checkpoint should be written, false otherwise
   */
  fun shouldPeriodicCheckpoint(
    isPlaying: Boolean,
    currentPosition: Int?,
    duration: Int?,
    lastSavedPosition: Int?,
    savePositionEnabled: Boolean = true,
    minPositionDeltaSeconds: Int = DEFAULT_MIN_POSITION_DELTA_SECONDS,
  ): Boolean {
    if (!savePositionEnabled) return false
    if (!isPlaying) return false
    if (currentPosition == null || duration == null) return false
    if (duration <= 0) return false
    if (currentPosition < 0 || currentPosition > duration) return false

    if (lastSavedPosition != null) {
      if (abs(currentPosition - lastSavedPosition) < minPositionDeltaSeconds) {
        return false
      }
    } else {
      if (currentPosition < minPositionDeltaSeconds) {
        return false
      }
    }

    return true
  }

  /**
   * Calculates the position to persist, guarding against bad cases:
   * - Duration still 0 (keeps last saved position, never writes 0 over a good position)
   * - Position > duration (guards against invalid position)
   * - Finished resets to 0 (existing rule when position >= duration - 1)
   * - Transient 0 while loading (keeps last saved position if > 0)
   *
   * @param currentPosition Current playback position in seconds
   * @param duration Media duration in seconds
   * @param lastSavedPosition The existing saved position from repository or memory
   * @param savePositionEnabled Whether position saving is enabled in preferences
   * @return Position in seconds to persist
   */
  fun calculateSavePosition(
    currentPosition: Int?,
    duration: Int?,
    lastSavedPosition: Int?,
    savePositionEnabled: Boolean = true,
  ): Int {
    val fallback = lastSavedPosition ?: 0
    if (!savePositionEnabled) return fallback
    if (currentPosition == null || duration == null) return fallback
    if (duration <= 0) return fallback
    if (currentPosition < 0 || currentPosition > duration) return fallback

    // Finished resets to 0 rule: at end of video (within 1s)
    if (currentPosition >= duration - 1) {
      return 0
    }

    // Never write 0 over a good position
    if (currentPosition == 0 && fallback > 0) {
      return fallback
    }

    return currentPosition
  }
}
