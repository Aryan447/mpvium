package app.aryan447.mpvium.domain.playbackstate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCheckpointDeciderTest {

  @Test
  fun shouldPeriodicCheckpoint_whenPlayingAndMovedAtLeast5s_returnsTrue() {
    val shouldSave = PlaybackCheckpointDecider.shouldPeriodicCheckpoint(
      isPlaying = true,
      currentPosition = 35,
      duration = 100,
      lastSavedPosition = 30,
    )
    assertTrue(shouldSave)
  }

  @Test
  fun shouldPeriodicCheckpoint_whenMovedLessThan5s_returnsFalse() {
    val shouldSave = PlaybackCheckpointDecider.shouldPeriodicCheckpoint(
      isPlaying = true,
      currentPosition = 34,
      duration = 100,
      lastSavedPosition = 30,
    )
    assertFalse(shouldSave)
  }

  @Test
  fun shouldPeriodicCheckpoint_whenPaused_returnsFalse() {
    val shouldSave = PlaybackCheckpointDecider.shouldPeriodicCheckpoint(
      isPlaying = false,
      currentPosition = 50,
      duration = 100,
      lastSavedPosition = 30,
    )
    assertFalse(shouldSave)
  }

  @Test
  fun shouldPeriodicCheckpoint_whenDurationIsZeroOrNegative_returnsFalse() {
    assertFalse(
      PlaybackCheckpointDecider.shouldPeriodicCheckpoint(
        isPlaying = true,
        currentPosition = 30,
        duration = 0,
        lastSavedPosition = 0,
      ),
    )
    assertFalse(
      PlaybackCheckpointDecider.shouldPeriodicCheckpoint(
        isPlaying = true,
        currentPosition = 30,
        duration = -1,
        lastSavedPosition = 0,
      ),
    )
    assertFalse(
      PlaybackCheckpointDecider.shouldPeriodicCheckpoint(
        isPlaying = true,
        currentPosition = 30,
        duration = null,
        lastSavedPosition = 0,
      ),
    )
  }

  @Test
  fun shouldPeriodicCheckpoint_whenPositionExceedsDuration_returnsFalse() {
    val shouldSave = PlaybackCheckpointDecider.shouldPeriodicCheckpoint(
      isPlaying = true,
      currentPosition = 120,
      duration = 100,
      lastSavedPosition = 30,
    )
    assertFalse(shouldSave)
  }

  @Test
  fun shouldPeriodicCheckpoint_whenPositionIsNegative_returnsFalse() {
    val shouldSave = PlaybackCheckpointDecider.shouldPeriodicCheckpoint(
      isPlaying = true,
      currentPosition = -1,
      duration = 100,
      lastSavedPosition = null,
    )
    assertFalse(shouldSave)
  }

  @Test
  fun shouldPeriodicCheckpoint_whenSavePositionDisabled_returnsFalse() {
    val shouldSave = PlaybackCheckpointDecider.shouldPeriodicCheckpoint(
      isPlaying = true,
      currentPosition = 50,
      duration = 100,
      lastSavedPosition = 30,
      savePositionEnabled = false,
    )
    assertFalse(shouldSave)
  }

  @Test
  fun shouldPeriodicCheckpoint_whenLastSavedPositionNullAndPositionAtLeast5s_returnsTrue() {
    val shouldSave = PlaybackCheckpointDecider.shouldPeriodicCheckpoint(
      isPlaying = true,
      currentPosition = 5,
      duration = 100,
      lastSavedPosition = null,
    )
    assertTrue(shouldSave)
  }

  @Test
  fun shouldPeriodicCheckpoint_whenLastSavedPositionNullAndPositionLessThan5s_returnsFalse() {
    val shouldSave = PlaybackCheckpointDecider.shouldPeriodicCheckpoint(
      isPlaying = true,
      currentPosition = 4,
      duration = 100,
      lastSavedPosition = null,
    )
    assertFalse(shouldSave)
  }

  @Test
  fun shouldPeriodicCheckpoint_whenBackwardSeekExceeds5s_returnsTrue() {
    val shouldSave = PlaybackCheckpointDecider.shouldPeriodicCheckpoint(
      isPlaying = true,
      currentPosition = 20,
      duration = 100,
      lastSavedPosition = 80,
    )
    assertTrue(shouldSave)
  }

  @Test
  fun calculateSavePosition_whenSavePositionDisabled_returnsPreviousSaved() {
    val pos = PlaybackCheckpointDecider.calculateSavePosition(
      currentPosition = 50,
      duration = 100,
      lastSavedPosition = 25,
      savePositionEnabled = false,
    )
    assertEquals(25, pos)
  }

  @Test
  fun calculateSavePosition_whenDurationIsZero_preservesGoodPosition() {
    val pos = PlaybackCheckpointDecider.calculateSavePosition(
      currentPosition = 0,
      duration = 0,
      lastSavedPosition = 300,
    )
    assertEquals(300, pos)
  }

  @Test
  fun calculateSavePosition_whenPositionIsZero_preservesGoodPosition() {
    val pos = PlaybackCheckpointDecider.calculateSavePosition(
      currentPosition = 0,
      duration = 500,
      lastSavedPosition = 200,
    )
    assertEquals(200, pos)
  }

  @Test
  fun calculateSavePosition_whenFinished_resetsToZero() {
    // Within 1s of duration or past it
    val posAtEndMinusOne = PlaybackCheckpointDecider.calculateSavePosition(
      currentPosition = 99,
      duration = 100,
      lastSavedPosition = 50,
    )
    assertEquals(0, posAtEndMinusOne)

    val posAtEnd = PlaybackCheckpointDecider.calculateSavePosition(
      currentPosition = 100,
      duration = 100,
      lastSavedPosition = 50,
    )
    assertEquals(0, posAtEnd)
  }

  @Test
  fun calculateSavePosition_whenBeyondWatchedThresholdButNotFinished_savesCurrentPosition() {
    // 95% threshold of 100s is 95s; position 96s is beyond threshold but not finished (< 99s)
    val pos = PlaybackCheckpointDecider.calculateSavePosition(
      currentPosition = 96,
      duration = 100,
      lastSavedPosition = 90,
    )
    assertEquals(96, pos)
  }

  @Test
  fun calculateSavePosition_whenNormalPlayback_savesCurrentPosition() {
    val pos = PlaybackCheckpointDecider.calculateSavePosition(
      currentPosition = 40,
      duration = 100,
      lastSavedPosition = 30,
    )
    assertEquals(40, pos)
  }

  @Test
  fun calculateSavePosition_whenPositionExceedsDuration_guardsGoodPosition() {
    val pos = PlaybackCheckpointDecider.calculateSavePosition(
      currentPosition = 150,
      duration = 100,
      lastSavedPosition = 60,
    )
    assertEquals(60, pos)
  }
}
