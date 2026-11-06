package app.aryan447.mpvium.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekCacheResolverTest {
  private companion object {
    const val MB = 1024L * 1024L
  }

  @Test
  fun offLegacyOptions_normalApi_keepsLegacyConstants() {
    val profile = SeekCacheResolver.resolveLegacy(sdkInt = 27)
    assertEquals(64L * MB, profile.demuxerMaxBytes)
    assertEquals(128L * MB, profile.demuxerMaxBackBytes)
    assertEquals(10.0, profile.demuxerReadaheadSecs, 0.001)
    assertEquals(20.0, profile.cacheSecs, 0.001)
    assertEquals(1L * MB, profile.streamBufferSize)
  }

  @Test
  fun offLegacyOptions_lowApi_keepsLegacyConstants() {
    val profile = SeekCacheResolver.resolveLegacy(sdkInt = 26)
    assertEquals(32L * MB, profile.demuxerMaxBytes)
    assertEquals(32L * MB, profile.demuxerMaxBackBytes)
    assertEquals(10.0, profile.demuxerReadaheadSecs, 0.001)
    assertEquals(20.0, profile.cacheSecs, 0.001)
    assertEquals(1L * MB, profile.streamBufferSize)
  }

  @Test
  fun ceilings_neverExceededEvenWithHugeMemory() {
    val input = SeekCacheResolver.Input(
      memoryClassMb = 10_000,
      videoWidth = 1920,
      videoHeight = 1080,
    )
    val profile = SeekCacheResolver.resolve(input)
    assertTrue("Forward bytes must not exceed 96 MB", profile.demuxerMaxBytes <= 96L * MB)
    assertTrue("Backward bytes must not exceed 128 MB", profile.demuxerMaxBackBytes <= 128L * MB)
    assertTrue(
      "Combined bytes must not exceed 224 MB",
      profile.demuxerMaxBytes + profile.demuxerMaxBackBytes <= 224L * MB,
    )
  }

  @Test
  fun lowMemoryClass_respectsSafeBudget() {
    // 128 MB memoryClass -> safeBudget ≈ 10% = 12.8 MB
    val input = SeekCacheResolver.Input(
      memoryClassMb = 128,
      videoWidth = 1920,
      videoHeight = 1080,
    )
    val profile = SeekCacheResolver.resolve(input)
    val combined = profile.demuxerMaxBytes + profile.demuxerMaxBackBytes
    assertTrue(
      "Combined ($combined) must not exceed safeBudget (${profile.safeCombinedBudgetBytes})",
      combined <= profile.safeCombinedBudgetBytes,
    )
  }

  @Test
  fun highMemoryClass_reachesCeilingsWhenBitrateAllows() {
    // Generous budget: 4096 MB memoryClass, 1080p video targeting ~180s per direction
    val input = SeekCacheResolver.Input(
      memoryClassMb = 4096,
      videoWidth = 1920,
      videoHeight = 1080,
      sourceType = SeekCacheResolver.SourceType.Local,
    )
    val profile = SeekCacheResolver.resolve(input)
    assertEquals(96L * MB, profile.demuxerMaxBytes)
    assertEquals(128L * MB, profile.demuxerMaxBackBytes)
  }

  @Test
  fun bitrateConfidence_prefersExplicitVideoBitrate() {
    // When explicit videoBitrate is available, it takes precedence over resolution
    val input = SeekCacheResolver.Input(
      memoryClassMb = 1024,
      videoWidth = 3840,
      videoHeight = 2160,
      videoBitrate = 5_000_000L, // 5 Mbps despite 4K resolution
    )
    val expectedBitrate = (5_000_000L * SeekCacheResolver.OVERHEAD_MULTIPLIER).toLong()
    assertEquals(expectedBitrate, SeekCacheResolver.determineEffectiveBitrate(input))
  }

  @Test
  fun bitrateConfidence_usesFileSizeDurationWhenNoExplicitBitrate() {
    // 900 MB file over 900 seconds = 1 MB/s = 8 Mbps
    val input = SeekCacheResolver.Input(
      memoryClassMb = 1024,
      fileSizeBytes = 900L * MB,
      durationSeconds = 900.0,
      videoWidth = 3840,
      videoHeight = 2160,
    )
    val expectedBitrate = ((900L * MB * 8.0) / 900.0).toLong()
    assertEquals(expectedBitrate, SeekCacheResolver.determineEffectiveBitrate(input))
  }

  @Test
  fun bitrateConfidence_usesResolutionFallback_4K() {
    val input = SeekCacheResolver.Input(
      memoryClassMb = 1024,
      videoWidth = 3840,
      videoHeight = 2160,
    )
    val expected = (SeekCacheResolver.BITRATE_4K * SeekCacheResolver.OVERHEAD_MULTIPLIER).toLong()
    assertEquals(expected, SeekCacheResolver.determineEffectiveBitrate(input))
  }

  @Test
  fun bitrateConfidence_usesResolutionFallback_1080p() {
    val input = SeekCacheResolver.Input(
      memoryClassMb = 1024,
      videoWidth = 1920,
      videoHeight = 1080,
    )
    val expected = (SeekCacheResolver.BITRATE_1080P * SeekCacheResolver.OVERHEAD_MULTIPLIER).toLong()
    assertEquals(expected, SeekCacheResolver.determineEffectiveBitrate(input))
  }

  @Test
  fun bitrateConfidence_usesResolutionFallback_720p() {
    val input = SeekCacheResolver.Input(
      memoryClassMb = 1024,
      videoWidth = 1280,
      videoHeight = 720,
    )
    val expected = (SeekCacheResolver.BITRATE_720P * SeekCacheResolver.OVERHEAD_MULTIPLIER).toLong()
    assertEquals(expected, SeekCacheResolver.determineEffectiveBitrate(input))
  }

  @Test
  fun fourKLocal_doesNotOverspendMemoryBudget() {
    // 4K local on a 512 MB memoryClass device (safe budget ≈ 51.2 MB)
    val input = SeekCacheResolver.Input(
      memoryClassMb = 512,
      videoWidth = 3840,
      videoHeight = 2160,
      sourceType = SeekCacheResolver.SourceType.Local,
    )
    val profile = SeekCacheResolver.resolve(input)
    val combined = profile.demuxerMaxBytes + profile.demuxerMaxBackBytes
    assertTrue(
      "Combined ($combined) must not exceed safe budget (${profile.safeCombinedBudgetBytes})",
      combined <= profile.safeCombinedBudgetBytes,
    )
    assertTrue("Forward must not exceed 96 MB", profile.demuxerMaxBytes <= 96L * MB)
    assertTrue("Backward must not exceed 128 MB", profile.demuxerMaxBackBytes <= 128L * MB)
  }

  @Test
  fun underPressure_shrinksBackwardFirst() {
    // Constrained budget where nominal demand exceeds budget
    val input = SeekCacheResolver.Input(
      memoryClassMb = 256, // safe budget ≈ 25.6 MB
      videoWidth = 1920,
      videoHeight = 1080,
      sourceType = SeekCacheResolver.SourceType.Local,
    )
    val profile = SeekCacheResolver.resolve(input)
    val combined = profile.demuxerMaxBytes + profile.demuxerMaxBackBytes
    assertTrue("Must fit within safe budget", combined <= profile.safeCombinedBudgetBytes)
    // Backward must have shrunk to accommodate forward
    assertTrue("Forward retains operational bytes", profile.demuxerMaxBytes >= 8L * MB)
  }

  @Test
  fun networkSource_biasesDeeperForwardWindow() {
    val networkInput = SeekCacheResolver.Input(
      memoryClassMb = 512,
      videoWidth = 1920,
      videoHeight = 1080,
      sourceType = SeekCacheResolver.SourceType.Network,
    )
    val localInput = networkInput.copy(sourceType = SeekCacheResolver.SourceType.Local)

    val netProfile = SeekCacheResolver.resolve(networkInput)
    val localProfile = SeekCacheResolver.resolve(localInput)

    // Network readahead and cache-secs should be equal or greater
    assertTrue(
      "Network cache-secs should bias higher",
      netProfile.cacheSecs >= localProfile.cacheSecs,
    )
  }

  @Test
  fun audioOnly_usesConservativeFallback() {
    val input = SeekCacheResolver.Input(
      memoryClassMb = 512,
      isAudioOnly = true,
    )
    val profile = SeekCacheResolver.resolve(input)
    assertNotNull(profile)
    assertTrue(profile.demuxerMaxBytes <= profile.safeCombinedBudgetBytes)
    assertEquals(
      (SeekCacheResolver.BITRATE_AUDIO * SeekCacheResolver.OVERHEAD_MULTIPLIER).toLong(),
      profile.effectiveBitrateBps,
    )
  }

  @Test
  fun liveStream_usesConservativeFallback() {
    val input = SeekCacheResolver.Input(
      memoryClassMb = 512,
      isLive = true,
      videoWidth = 1920,
      videoHeight = 1080,
    )
    val profile = SeekCacheResolver.resolve(input)
    assertEquals(10.0, profile.demuxerReadaheadSecs, 0.001)
    assertEquals(20.0, profile.cacheSecs, 0.001)
    assertTrue(profile.demuxerMaxBytes <= 32L * MB)
    assertTrue(profile.demuxerMaxBackBytes <= 32L * MB)
  }

  @Test
  fun unknownDuration_usesConservativeFallback() {
    val input = SeekCacheResolver.Input(
      memoryClassMb = 512,
      durationSeconds = -1.0,
      videoWidth = 1920,
      videoHeight = 1080,
    )
    val profile = SeekCacheResolver.resolve(input)
    assertEquals(10.0, profile.demuxerReadaheadSecs, 0.001)
    assertEquals(20.0, profile.cacheSecs, 0.001)
  }

  @Test
  fun invalidInputs_handlesGracefully() {
    val negativeMem = SeekCacheResolver.Input(
      memoryClassMb = -10,
      videoWidth = -100,
      videoHeight = -200,
      videoBitrate = -5000L,
    )
    val profile = SeekCacheResolver.resolve(negativeMem)
    assertNotNull(profile)
    assertTrue(profile.demuxerMaxBytes > 0L)
    assertTrue(profile.demuxerMaxBackBytes > 0L)
    assertTrue(profile.demuxerReadaheadSecs > 0.0)
    assertTrue(profile.cacheSecs > 0.0)
  }

  @Test
  fun readaheadSecs_derivedFromForwardBytesAndBitrate() {
    val input = SeekCacheResolver.Input(
      memoryClassMb = 2048,
      videoWidth = 1920,
      videoHeight = 1080, // 8.8 Mbps effective
    )
    val profile = SeekCacheResolver.resolve(input)
    // Not hardcoded 10.0s when ON
    val expectedSecs = (profile.demuxerMaxBytes * 8.0) / profile.effectiveBitrateBps!!
    assertEquals(expectedSecs.coerceIn(10.0, 180.0), profile.demuxerReadaheadSecs, 0.01)
  }

  @Test
  fun formatDurationMinutesSeconds_formatsProperly() {
    assertEquals("2 min 10 s", SeekCacheResolver.formatDurationMinutesSeconds(130))
    assertEquals("2 min", SeekCacheResolver.formatDurationMinutesSeconds(120))
    assertEquals("45 s", SeekCacheResolver.formatDurationMinutesSeconds(45))
  }

  @Test
  fun parseDemuxerCacheRanges_handlesNullAndEmpty() {
    val ranges = SeekCacheResolver.parseDemuxerCacheRanges(null)
    assertTrue(ranges.isEmpty())
  }

  // ==================== Cache poll gate ====================
  // The seekbar only republishes ranges when an edge moved more than the epsilon.
  // These cover the gate used by PlayerViewModel.pollDemuxerCache().

  @Test
  fun hasCacheRangesChanged_identicalRanges_doesNotRepublish() {
    val ranges = listOf(
      SeekCacheResolver.SeekableRange(0f, 30f),
      SeekCacheResolver.SeekableRange(60f, 90f),
    )
    assertFalse(SeekCacheResolver.hasCacheRangesChanged(ranges, ranges.toList()))
  }

  @Test
  fun hasCacheRangesChanged_subEpsilonDrift_isIgnored() {
    val current = listOf(SeekCacheResolver.SeekableRange(0f, 30f))
    val polled = listOf(SeekCacheResolver.SeekableRange(0.05f, 30.15f))
    assertFalse(SeekCacheResolver.hasCacheRangesChanged(current, polled))
  }

  @Test
  fun hasCacheRangesChanged_edgeBeyondEpsilon_republishes() {
    val current = listOf(SeekCacheResolver.SeekableRange(0f, 30f))
    assertTrue(
      SeekCacheResolver.hasCacheRangesChanged(
        current,
        listOf(SeekCacheResolver.SeekableRange(0f, 31f)),
      ),
    )
    assertTrue(
      SeekCacheResolver.hasCacheRangesChanged(
        current,
        listOf(SeekCacheResolver.SeekableRange(0.5f, 30f)),
      ),
    )
  }

  @Test
  fun hasCacheRangesChanged_rangeCountDiffers_republishes() {
    val current = listOf(SeekCacheResolver.SeekableRange(0f, 30f))
    assertTrue(SeekCacheResolver.hasCacheRangesChanged(current, emptyList()))
    assertTrue(
      SeekCacheResolver.hasCacheRangesChanged(
        current,
        listOf(
          SeekCacheResolver.SeekableRange(0f, 30f),
          SeekCacheResolver.SeekableRange(40f, 50f),
        ),
      ),
    )
  }

  @Test
  fun hasCacheRangesChanged_bothEmpty_doesNotRepublish() {
    assertFalse(SeekCacheResolver.hasCacheRangesChanged(emptyList(), emptyList()))
  }

  @Test
  fun cacheRangeEpsilon_matchesDocumentedThreshold() {
    assertEquals(0.2f, SeekCacheResolver.CACHE_RANGE_EPSILON, 0.0001f)
  }
}
