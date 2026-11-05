package app.aryan447.mpvium.ui.streaming.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieDurationFilterTest {

  private companion object {
    const val MINUTE_MS = 60_000L
    const val HOUR_MS = 60 * MINUTE_MS
  }

  @Test
  fun enumEntriesAndLabels_matchSpecification() {
    assertEquals(4, MovieDurationFilter.entries.size)
    assertEquals("Any length", MovieDurationFilter.ALL.label)
    assertEquals("30 min+", MovieDurationFilter.AT_LEAST_30M.label)
    assertEquals("1 h+", MovieDurationFilter.AT_LEAST_1H.label)
    assertEquals("1.5 h+", MovieDurationFilter.AT_LEAST_1H30M.label)
  }

  @Test
  fun filterThresholds_inMilliseconds() {
    assertEquals(0L, MovieDurationFilter.ALL.minDurationMs)
    assertEquals(30 * MINUTE_MS, MovieDurationFilter.AT_LEAST_30M.minDurationMs)
    assertEquals(HOUR_MS, MovieDurationFilter.AT_LEAST_1H.minDurationMs)
    assertEquals(90 * MINUTE_MS, MovieDurationFilter.AT_LEAST_1H30M.minDurationMs)
  }

  @Test
  fun filterMatching_shortVideoUnder30Minutes() {
    val duration = 25 * MINUTE_MS
    assertTrue(MovieDurationFilter.ALL.matches(duration))
    assertFalse(MovieDurationFilter.AT_LEAST_30M.matches(duration))
    assertFalse(MovieDurationFilter.AT_LEAST_1H.matches(duration))
    assertFalse(MovieDurationFilter.AT_LEAST_1H30M.matches(duration))
  }

  @Test
  fun filterMatching_atLeast30Minutes() {
    val duration = 30 * MINUTE_MS
    assertTrue(MovieDurationFilter.ALL.matches(duration))
    assertTrue(MovieDurationFilter.AT_LEAST_30M.matches(duration))
    assertFalse(MovieDurationFilter.AT_LEAST_1H.matches(duration))
    assertFalse(MovieDurationFilter.AT_LEAST_1H30M.matches(duration))

    val duration45m = 45 * MINUTE_MS
    assertTrue(MovieDurationFilter.ALL.matches(duration45m))
    assertTrue(MovieDurationFilter.AT_LEAST_30M.matches(duration45m))
    assertFalse(MovieDurationFilter.AT_LEAST_1H.matches(duration45m))
    assertFalse(MovieDurationFilter.AT_LEAST_1H30M.matches(duration45m))
  }

  @Test
  fun filterMatching_atLeast1Hour() {
    val duration = HOUR_MS
    assertTrue(MovieDurationFilter.ALL.matches(duration))
    assertTrue(MovieDurationFilter.AT_LEAST_30M.matches(duration))
    assertTrue(MovieDurationFilter.AT_LEAST_1H.matches(duration))
    assertFalse(MovieDurationFilter.AT_LEAST_1H30M.matches(duration))

    val duration75m = 75 * MINUTE_MS
    assertTrue(MovieDurationFilter.ALL.matches(duration75m))
    assertTrue(MovieDurationFilter.AT_LEAST_30M.matches(duration75m))
    assertTrue(MovieDurationFilter.AT_LEAST_1H.matches(duration75m))
    assertFalse(MovieDurationFilter.AT_LEAST_1H30M.matches(duration75m))
  }

  @Test
  fun filterMatching_atLeast1Hour30Minutes() {
    val duration90m = 90 * MINUTE_MS
    assertTrue(MovieDurationFilter.ALL.matches(duration90m))
    assertTrue(MovieDurationFilter.AT_LEAST_30M.matches(duration90m))
    assertTrue(MovieDurationFilter.AT_LEAST_1H.matches(duration90m))
    assertTrue(MovieDurationFilter.AT_LEAST_1H30M.matches(duration90m))

    val duration120m = 120 * MINUTE_MS
    assertTrue(MovieDurationFilter.ALL.matches(duration120m))
    assertTrue(MovieDurationFilter.AT_LEAST_30M.matches(duration120m))
    assertTrue(MovieDurationFilter.AT_LEAST_1H.matches(duration120m))
    assertTrue(MovieDurationFilter.AT_LEAST_1H30M.matches(duration120m))
  }

  @Test
  fun filterMatching_zeroOrNegativeDuration() {
    assertTrue(MovieDurationFilter.ALL.matches(0L))
    assertFalse(MovieDurationFilter.AT_LEAST_30M.matches(0L))
    assertFalse(MovieDurationFilter.AT_LEAST_1H.matches(0L))
    assertFalse(MovieDurationFilter.AT_LEAST_1H30M.matches(0L))

    assertFalse(MovieDurationFilter.AT_LEAST_30M.matches(-1L))
  }
}
