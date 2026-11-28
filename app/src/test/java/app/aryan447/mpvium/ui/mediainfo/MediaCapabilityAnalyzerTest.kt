package app.aryan447.mpvium.ui.mediainfo

import app.aryan447.mpvium.utils.media.MediaInfoOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaCapabilityAnalyzerTest {

  private fun baseDevice(
    tier: MediaCapabilityAnalyzer.ResolutionTier = MediaCapabilityAnalyzer.ResolutionTier.FHD_1080P,
    hdr: Set<MediaCapabilityAnalyzer.HdrType> = emptySet(),
    hwHevc10Bit: Boolean = true,
    hwAv1: Boolean = false,
    surround: Boolean = false,
  ) = MediaCapabilityAnalyzer.DeviceCapabilities(
    displayWidthPx = when (tier) {
      MediaCapabilityAnalyzer.ResolutionTier.UHD_4K -> 3840
      MediaCapabilityAnalyzer.ResolutionTier.QHD_1440P -> 3120
      else -> 2400
    },
    displayHeightPx = when (tier) {
      MediaCapabilityAnalyzer.ResolutionTier.UHD_4K -> 2160
      MediaCapabilityAnalyzer.ResolutionTier.QHD_1440P -> 1440
      else -> 1080
    },
    displayTier = tier,
    maxRefreshRateHz = 120f,
    supportedHdrTypes = hdr,
    hwHevc = true,
    hwHevc10Bit = hwHevc10Bit,
    hwAv1 = hwAv1,
    hwVp9 = true,
    hwAvc = true,
    hasExternalAudio = surround,
    supportsSurroundPassThrough = surround,
    audioOutputLabel = if (surround) "HDMI Audio" else "Phone Speakers",
  )

  @Test
  fun fourKVideoOn1080pDeviceIsOverkill() {
    val data = MediaInfoOps.MediaInfoData(
      general = MediaInfoOps.GeneralInfo(format = "Matroska", overallBitRateBps = 25_000_000L),
      videoStreams = listOf(
        MediaInfoOps.VideoStreamInfo(
          streamIndex = 0,
          format = "HEVC",
          widthPx = 3840,
          heightPx = 2160,
          bitRateBps = 24_000_000L,
          bitDepthBits = 10,
          hdrFormat = "Dolby Vision",
        ),
      ),
      audioStreams = listOf(
        MediaInfoOps.AudioStreamInfo(
          streamIndex = 0,
          format = "E-AC-3",
          formatCommercial = "Dolby Digital Plus with Dolby Atmos",
          channelCount = 6,
          bitRateBps = 768_000L,
        ),
      ),
      textStreams = emptyList(),
    )

    val report = MediaCapabilityAnalyzer.evaluate(data, baseDevice())

    val resMetric = report.metrics.first { it.kind == MediaCapabilityAnalyzer.MetricKind.RESOLUTION }
    assertEquals(MediaCapabilityAnalyzer.VerdictKey.OVERKILL, resMetric.verdict)
    assertEquals(MediaCapabilityAnalyzer.VerdictTone.OVERKILL, resMetric.tone)

    val hdrMetric = report.metrics.first { it.kind == MediaCapabilityAnalyzer.MetricKind.COLOR_HDR }
    assertEquals(MediaCapabilityAnalyzer.VerdictKey.TONE_MAPPED, hdrMetric.verdict)

    val audioMetric = report.metrics.first { it.kind == MediaCapabilityAnalyzer.MetricKind.AUDIO }
    assertEquals(MediaCapabilityAnalyzer.VerdictKey.OVERKILL, audioMetric.verdict)

    assertTrue(report.badges.isAtmos)
    assertTrue(report.badges.isDolbyAudio)
    assertEquals(MediaCapabilityAnalyzer.HdrType.DOLBY_VISION, report.badges.hdrBadge)
    assertEquals("HEVC", report.badges.videoCodecBadge)
    assertEquals("10-bit", report.badges.bitDepthBadge)
    assertEquals("4K UHD", report.badges.resolutionBadge)
  }

  @Test
  fun native1080pWithHdr10MatchScoresOptimal() {
    val data = MediaInfoOps.MediaInfoData(
      general = MediaInfoOps.GeneralInfo(format = "MPEG-4"),
      videoStreams = listOf(
        MediaInfoOps.VideoStreamInfo(
          streamIndex = 0,
          format = "HEVC",
          widthPx = 1920,
          heightPx = 1080,
          bitRateBps = 6_000_000L,
          bitDepthBits = 10,
          hdrFormat = "SMPTE ST 2086, HDR10 compatible",
        ),
      ),
      audioStreams = listOf(
        MediaInfoOps.AudioStreamInfo(
          streamIndex = 0,
          format = "AAC",
          channelCount = 2,
          bitRateBps = 256_000L,
        ),
      ),
      textStreams = emptyList(),
    )

    val device = baseDevice(
      tier = MediaCapabilityAnalyzer.ResolutionTier.FHD_1080P,
      hdr = setOf(MediaCapabilityAnalyzer.HdrType.HDR10),
    )
    val report = MediaCapabilityAnalyzer.evaluate(data, device)

    val resMetric = report.metrics.first { it.kind == MediaCapabilityAnalyzer.MetricKind.RESOLUTION }
    assertEquals(MediaCapabilityAnalyzer.VerdictKey.SPOT_ON, resMetric.verdict)
    assertEquals(100, resMetric.score)

    val hdrMetric = report.metrics.first { it.kind == MediaCapabilityAnalyzer.MetricKind.COLOR_HDR }
    assertEquals(MediaCapabilityAnalyzer.VerdictKey.NATIVE_HDR, hdrMetric.verdict)
    assertEquals(100, hdrMetric.score)

    val compatMetric = report.metrics.first { it.kind == MediaCapabilityAnalyzer.MetricKind.PLAYBACK_EFFICIENCY }
    assertEquals(MediaCapabilityAnalyzer.VerdictKey.HARDWARE_READY, compatMetric.verdict)
  }

  @Test
  fun av1WithoutHardwareDecoderIsCpuHeavy() {
    val data = MediaInfoOps.MediaInfoData(
      general = MediaInfoOps.GeneralInfo(format = "WebM"),
      videoStreams = listOf(
        MediaInfoOps.VideoStreamInfo(
          streamIndex = 0,
          format = "AV1",
          widthPx = 1920,
          heightPx = 1080,
          bitRateBps = 4_000_000L,
          bitDepthBits = 10,
        ),
      ),
      audioStreams = emptyList(),
      textStreams = emptyList(),
    )

    val report = MediaCapabilityAnalyzer.evaluate(data, baseDevice(hwAv1 = false))
    val compat = report.metrics.first { it.kind == MediaCapabilityAnalyzer.MetricKind.PLAYBACK_EFFICIENCY }
    assertEquals(MediaCapabilityAnalyzer.VerdictKey.CPU_HEAVY, compat.verdict)
  }
}
