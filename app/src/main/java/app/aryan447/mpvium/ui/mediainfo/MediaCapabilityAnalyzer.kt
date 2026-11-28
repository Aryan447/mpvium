package app.aryan447.mpvium.ui.mediainfo

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.view.Display
import app.aryan447.mpvium.utils.media.MediaInfoOps
import kotlin.math.roundToInt

/**
 * Evaluates a media file's technical streams against the current device's display,
 * hardware decoder, and audio output capabilities to produce layman-friendly verdicts
 * and quality scores (0-100).
 */
object MediaCapabilityAnalyzer {

  enum class ResolutionTier(val rank: Int, val shortLabel: String) {
    SD(1, "SD"),
    HD_720P(2, "720p HD"),
    FHD_1080P(3, "1080p"),
    QHD_1440P(4, "2K QHD"),
    UHD_4K(5, "4K UHD"),
    UHD_8K(6, "8K"),
  }

  enum class HdrType(val label: String) {
    DOLBY_VISION("Dolby Vision"),
    HDR10_PLUS("HDR10+"),
    HDR10("HDR10"),
    HLG("HLG"),
    HDR_GENERIC("HDR"),
    SDR("SDR"),
  }

  enum class VerdictTone {
    OPTIMAL,     // Spot on / Great / Hardware ready
    OVERKILL,    // Exceeds device capabilities (e.g., 4K on 1080p)
    ADAPTED,     // Tone-mapped / Downmixed / Software decoded
    LOW,         // Below ideal quality (low res, starved bitrate)
  }

  enum class MetricKind {
    RESOLUTION,
    BITRATE,
    COLOR_HDR,
    AUDIO,
    PLAYBACK_EFFICIENCY,
  }

  enum class VerdictKey {
    SPOT_ON,
    OVERKILL,
    GREAT,
    GOOD,
    LOW_RES,
    BLOATED,
    REFERENCE,
    BALANCED,
    COMPRESSED,
    NATIVE_HDR,
    TONE_MAPPED,
    SMOOTH_10BIT,
    STANDARD_COLOR,
    HARDWARE_READY,
    CPU_HEAVY,
    SURROUND_READY,
    STUDIO_DOWNMIX,
    RICH_AUDIO,
    THIN_AUDIO,
  }

  data class DeviceCapabilities(
    val displayWidthPx: Int,
    val displayHeightPx: Int,
    val displayTier: ResolutionTier,
    val maxRefreshRateHz: Float,
    val supportedHdrTypes: Set<HdrType>,
    val hwHevc: Boolean,
    val hwHevc10Bit: Boolean,
    val hwAv1: Boolean,
    val hwVp9: Boolean,
    val hwAvc: Boolean,
    val hasExternalAudio: Boolean,
    val supportsSurroundPassThrough: Boolean,
    val audioOutputLabel: String,
  ) {
    val supportsAnyHdr: Boolean
      get() = supportedHdrTypes.any { it != HdrType.SDR }
  }

  data class MetricScore(
    val kind: MetricKind,
    val score: Int,
    val verdict: VerdictKey,
    val tone: VerdictTone,
    val primaryValue: String,
    val deviceValue: String,
  )

  data class TechBadges(
    val resolutionBadge: String? = null,
    val videoCodecBadge: String? = null,
    val bitDepthBadge: String? = null,
    val hdrBadge: HdrType? = null,
    val isDolbyAudio: Boolean = false,
    val isAtmos: Boolean = false,
    val isDts: Boolean = false,
    val audioBadge: String? = null,
    val subtitleBadges: List<String> = emptyList(),
    val hasSdhSubtitles: Boolean = false,
    val containerBadge: String? = null,
  )

  data class QualityReport(
    val overallScore: Int,
    val overallVerdict: VerdictKey,
    val overallTone: VerdictTone,
    val metrics: List<MetricScore>,
    val badges: TechBadges,
  )

  fun detectDeviceCapabilities(context: Context): DeviceCapabilities {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)

    val mode = display?.mode
    val rawW = mode?.physicalWidth ?: context.resources.displayMetrics.widthPixels
    val rawH = mode?.physicalHeight ?: context.resources.displayMetrics.heightPixels
    val shortSide = minOf(rawW, rawH)
    val longSide = maxOf(rawW, rawH)
    val displayTier = classifyDisplayTier(shortSide)

    val maxHz = display?.supportedModes?.maxOfOrNull { it.refreshRate } ?: display?.refreshRate ?: 60f

    val hdrSet = mutableSetOf<HdrType>()
    @Suppress("DEPRECATION")
    val hdrCaps = display?.hdrCapabilities
    if (hdrCaps != null) {
      for (type in hdrCaps.supportedHdrTypes) {
        when (type) {
          Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> hdrSet.add(HdrType.DOLBY_VISION)
          Display.HdrCapabilities.HDR_TYPE_HDR10 -> hdrSet.add(HdrType.HDR10)
          Display.HdrCapabilities.HDR_TYPE_HLG -> hdrSet.add(HdrType.HLG)
          else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
              type == Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS
            ) {
              hdrSet.add(HdrType.HDR10_PLUS)
            }
          }
        }
      }
    }

    val codecCaps = detectHardwareCodecs()

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val outputs = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
    var hasExternal = false
    var supportsSurround = false
    var outputName = "Phone Speakers"

    for (device in outputs) {
      when (device.type) {
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        -> {
          hasExternal = true
          outputName = "Wired Headphones"
        }

        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        -> {
          hasExternal = true
          outputName = "Bluetooth Audio"
        }

        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_HDMI_ARC,
        AudioDeviceInfo.TYPE_HDMI_EARC,
        -> {
          hasExternal = true
          outputName = "HDMI Audio"
        }
      }

      if (device.channelCounts.any { it > 2 }) {
        supportsSurround = true
      }
      val encodings = device.encodings
      if (encodings.any {
          it == AudioFormat.ENCODING_AC3 ||
            it == AudioFormat.ENCODING_E_AC3 ||
            it == AudioFormat.ENCODING_E_AC3_JOC ||
            it == AudioFormat.ENCODING_DTS ||
            it == AudioFormat.ENCODING_DTS_HD ||
            it == AudioFormat.ENCODING_DOLBY_TRUEHD
        }
      ) {
        supportsSurround = true
      }
    }

    return DeviceCapabilities(
      displayWidthPx = longSide,
      displayHeightPx = shortSide,
      displayTier = displayTier,
      maxRefreshRateHz = maxHz,
      supportedHdrTypes = hdrSet,
      hwHevc = codecCaps.hevc,
      hwHevc10Bit = codecCaps.hevc10Bit,
      hwAv1 = codecCaps.av1,
      hwVp9 = codecCaps.vp9,
      hwAvc = codecCaps.avc,
      hasExternalAudio = hasExternal,
      supportsSurroundPassThrough = supportsSurround,
      audioOutputLabel = outputName,
    )
  }

  private data class CodecFlags(
    val hevc: Boolean,
    val hevc10Bit: Boolean,
    val av1: Boolean,
    val vp9: Boolean,
    val avc: Boolean,
  )

  private fun detectHardwareCodecs(): CodecFlags {
    var hevc = false
    var hevc10Bit = false
    var av1 = false
    var vp9 = false
    var avc = false

    val infos = runCatching {
      MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
    }.getOrDefault(emptyArray())

    for (info in infos) {
      if (info.isEncoder) continue
      val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        info.isHardwareAccelerated
      } else {
        val name = info.name.lowercase()
        !name.startsWith("omx.google.") && !name.startsWith("c2.android.")
      }
      if (!isHw) continue

      for (type in info.supportedTypes) {
        when (type.lowercase()) {
          "video/hevc" -> {
            hevc = true
            val caps = runCatching { info.getCapabilitiesForType(type) }.getOrNull()
            val has10 = caps?.profileLevels?.any {
              it.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 ||
                it.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                  it.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus)
            } == true
            if (has10) hevc10Bit = true
          }
          "video/av01" -> av1 = true
          "video/x-vnd.on2.vp9" -> vp9 = true
          "video/avc" -> avc = true
        }
      }
    }

    return CodecFlags(hevc = hevc, hevc10Bit = hevc10Bit, av1 = av1, vp9 = vp9, avc = avc)
  }

  fun classifyDisplayTier(shortSidePx: Int): ResolutionTier = when {
    shortSidePx >= 4000 -> ResolutionTier.UHD_8K
    shortSidePx >= 2000 -> ResolutionTier.UHD_4K
    shortSidePx >= 1350 -> ResolutionTier.QHD_1440P
    shortSidePx >= 1000 -> ResolutionTier.FHD_1080P
    shortSidePx >= 680 -> ResolutionTier.HD_720P
    else -> ResolutionTier.SD
  }

  fun classifyVideoTier(widthPx: Int, heightPx: Int): ResolutionTier {
    val longSide = maxOf(widthPx, heightPx)
    val shortSide = minOf(widthPx, heightPx)
    return when {
      longSide >= 7000 || shortSide >= 4000 -> ResolutionTier.UHD_8K
      longSide >= 3500 || shortSide >= 2000 -> ResolutionTier.UHD_4K
      longSide >= 2400 || shortSide >= 1350 -> ResolutionTier.QHD_1440P
      longSide >= 1800 || shortSide >= 1000 -> ResolutionTier.FHD_1080P
      longSide >= 1200 || shortSide >= 680 -> ResolutionTier.HD_720P
      else -> ResolutionTier.SD
    }
  }

  fun detectHdrType(video: MediaInfoOps.VideoStreamInfo): HdrType {
    val combined = "${video.hdrFormat} ${video.hdrFormatCommercial}".lowercase()
    return when {
      combined.contains("dolby vision") || combined.contains("dovi") -> HdrType.DOLBY_VISION
      combined.contains("hdr10+") || combined.contains("smpte st 2094") -> HdrType.HDR10_PLUS
      combined.contains("hdr10") || combined.contains("smpte st 2086") || combined.contains("smpte st 2084") -> HdrType.HDR10
      combined.contains("hlg") || video.transferCharacteristics.contains("HLG", ignoreCase = true) -> HdrType.HLG
      combined.isNotBlank() -> HdrType.HDR_GENERIC
      video.transferCharacteristics.contains("PQ", ignoreCase = true) -> HdrType.HDR10
      else -> HdrType.SDR
    }
  }

  fun normalizeVideoCodec(format: String): String {
    val f = format.trim().uppercase()
    return when {
      f.contains("HEVC") || f.contains("H.265") || f.contains("H265") -> "HEVC"
      f.contains("AVC") || f.contains("H.264") || f.contains("H264") -> "H.264"
      f.contains("AV1") -> "AV1"
      f.contains("VP9") -> "VP9"
      f.contains("VP8") -> "VP8"
      f.contains("MPEG-4") -> "MPEG-4"
      f.contains("MPEG VIDEO") -> "MPEG-2"
      else -> format.trim().ifEmpty { "Video" }
    }
  }

  fun evaluate(
    data: MediaInfoOps.MediaInfoData,
    device: DeviceCapabilities,
  ): QualityReport {
    val primaryVideo = data.videoStreams.firstOrNull()
    val primaryAudio = data.audioStreams.firstOrNull()
    val metrics = mutableListOf<MetricScore>()

    if (primaryVideo != null && primaryVideo.widthPx > 0 && primaryVideo.heightPx > 0) {
      metrics += evaluateResolution(primaryVideo, device)
    }

    val effectiveBitrate = primaryVideo?.bitRateBps?.takeIf { it > 0L }
      ?: data.general.overallBitRateBps.takeIf { it > 0L }
      ?: 0L
    if (primaryVideo != null && effectiveBitrate > 0L) {
      metrics += evaluateBitrate(primaryVideo, effectiveBitrate)
    }

    if (primaryVideo != null) {
      metrics += evaluateColorHdr(primaryVideo, device)
      metrics += evaluateCompatibility(primaryVideo, device)
    }

    if (primaryAudio != null) {
      metrics += evaluateAudio(primaryAudio, device)
    }

    val overallScore = if (metrics.isNotEmpty()) {
      metrics.map { it.score }.average().roundToInt().coerceIn(0, 100)
    } else {
      75
    }

    val hasOverkill = metrics.any { it.tone == VerdictTone.OVERKILL }
    val (overallVerdict, overallTone) = when {
      hasOverkill && overallScore >= 75 -> VerdictKey.OVERKILL to VerdictTone.OVERKILL
      overallScore >= 85 -> VerdictKey.SPOT_ON to VerdictTone.OPTIMAL
      overallScore >= 70 -> VerdictKey.GREAT to VerdictTone.OPTIMAL
      overallScore >= 50 -> VerdictKey.GOOD to VerdictTone.ADAPTED
      else -> VerdictKey.LOW_RES to VerdictTone.LOW
    }

    val badges = buildTechBadges(data, primaryVideo, primaryAudio)

    return QualityReport(
      overallScore = overallScore,
      overallVerdict = overallVerdict,
      overallTone = overallTone,
      metrics = metrics,
      badges = badges,
    )
  }

  private fun evaluateResolution(
    video: MediaInfoOps.VideoStreamInfo,
    device: DeviceCapabilities,
  ): MetricScore {
    val videoTier = classifyVideoTier(video.widthPx, video.heightPx)
    val diff = videoTier.rank - device.displayTier.rank
    val primary = "${video.widthPx}×${video.heightPx} (${videoTier.shortLabel})"
    val deviceText = "${device.displayTier.shortLabel} (${device.displayWidthPx}×${device.displayHeightPx})"

    val (score, verdict, tone) = when {
      diff >= 2 -> Triple(82, VerdictKey.OVERKILL, VerdictTone.OVERKILL)
      diff == 1 -> Triple(88, VerdictKey.OVERKILL, VerdictTone.OVERKILL)
      diff == 0 -> Triple(100, VerdictKey.SPOT_ON, VerdictTone.OPTIMAL)
      diff == -1 -> Triple(84, VerdictKey.GOOD, VerdictTone.OPTIMAL)
      else -> Triple(52, VerdictKey.LOW_RES, VerdictTone.LOW)
    }

    return MetricScore(
      kind = MetricKind.RESOLUTION,
      score = score,
      verdict = verdict,
      tone = tone,
      primaryValue = primary,
      deviceValue = deviceText,
    )
  }

  private fun evaluateBitrate(
    video: MediaInfoOps.VideoStreamInfo,
    bitrateBps: Long,
  ): MetricScore {
    val mbps = bitrateBps / 1_000_000.0
    val codec = normalizeVideoCodec(video.format)
    // Modern codecs (HEVC, AV1, VP9) achieve equivalent visual quality at ~55% of H.264 bitrate
    val efficiencyFactor = when (codec) {
      "AV1" -> 2.0
      "HEVC", "VP9" -> 1.75
      else -> 1.0
    }
    val effectiveMbps = mbps * efficiencyFactor
    val tier = classifyVideoTier(video.widthPx, video.heightPx)

    // Reference target Mbps (H.264 equivalent) per resolution tier
    val targetMbps = when (tier) {
      ResolutionTier.UHD_8K -> 75.0
      ResolutionTier.UHD_4K -> 32.0
      ResolutionTier.QHD_1440P -> 16.0
      ResolutionTier.FHD_1080P -> 8.0
      ResolutionTier.HD_720P -> 4.0
      ResolutionTier.SD -> 1.8
    }

    val ratio = effectiveMbps / targetMbps
    val formatted = if (mbps >= 1.0) {
      String.format(java.util.Locale.US, "%.1f Mbps", mbps)
    } else {
      "${(bitrateBps / 1_000L)} kbps"
    }

    val (score, verdict, tone) = when {
      ratio >= 2.8 -> Triple(80, VerdictKey.BLOATED, VerdictTone.OVERKILL)
      ratio >= 1.15 -> Triple(96, VerdictKey.REFERENCE, VerdictTone.OPTIMAL)
      ratio >= 0.65 -> Triple(86, VerdictKey.BALANCED, VerdictTone.OPTIMAL)
      ratio >= 0.35 -> Triple(64, VerdictKey.COMPRESSED, VerdictTone.ADAPTED)
      else -> Triple(42, VerdictKey.COMPRESSED, VerdictTone.LOW)
    }

    return MetricScore(
      kind = MetricKind.BITRATE,
      score = score,
      verdict = verdict,
      tone = tone,
      primaryValue = "$formatted ($codec)",
      deviceValue = tier.shortLabel,
    )
  }

  private fun evaluateColorHdr(
    video: MediaInfoOps.VideoStreamInfo,
    device: DeviceCapabilities,
  ): MetricScore {
    val hdr = detectHdrType(video)
    val bitDepth = video.bitDepthBits.takeIf { it > 0 } ?: 8
    val primary = if (hdr != HdrType.SDR) {
      "${bitDepth}-bit ${hdr.label}"
    } else {
      "${bitDepth}-bit SDR"
    }

    val deviceHdrLabel = if (device.supportsAnyHdr) {
      device.supportedHdrTypes.joinToString(", ") { it.label }
    } else {
      "SDR only"
    }

    val (score, verdict, tone) = when {
      hdr != HdrType.SDR && (hdr in device.supportedHdrTypes ||
        (hdr == HdrType.HDR10_PLUS && HdrType.HDR10 in device.supportedHdrTypes)) -> {
        Triple(100, VerdictKey.NATIVE_HDR, VerdictTone.OPTIMAL)
      }
      hdr == HdrType.DOLBY_VISION && HdrType.HDR10 in device.supportedHdrTypes -> {
        Triple(82, VerdictKey.TONE_MAPPED, VerdictTone.ADAPTED)
      }
      hdr != HdrType.SDR -> {
        Triple(72, VerdictKey.TONE_MAPPED, VerdictTone.ADAPTED)
      }
      bitDepth >= 10 -> {
        Triple(90, VerdictKey.SMOOTH_10BIT, VerdictTone.OPTIMAL)
      }
      else -> {
        Triple(78, VerdictKey.STANDARD_COLOR, VerdictTone.OPTIMAL)
      }
    }

    return MetricScore(
      kind = MetricKind.COLOR_HDR,
      score = score,
      verdict = verdict,
      tone = tone,
      primaryValue = primary,
      deviceValue = deviceHdrLabel,
    )
  }

  private fun evaluateCompatibility(
    video: MediaInfoOps.VideoStreamInfo,
    device: DeviceCapabilities,
  ): MetricScore {
    val codec = normalizeVideoCodec(video.format)
    val is10Bit = video.bitDepthBits >= 10
    val isHwSupported = when (codec) {
      "HEVC" -> if (is10Bit) device.hwHevc10Bit || device.hwHevc else device.hwHevc
      "AV1" -> device.hwAv1
      "VP9" -> device.hwVp9
      "H.264" -> !is10Bit && device.hwAvc // Hi10P H.264 is almost always software decoded on mobile
      else -> false
    }

    val fpsText = if (video.frameRateFps > 0f) {
      " • ${video.frameRateFps.roundToInt()} fps"
    } else {
      ""
    }

    return if (isHwSupported) {
      MetricScore(
        kind = MetricKind.PLAYBACK_EFFICIENCY,
        score = 96,
        verdict = VerdictKey.HARDWARE_READY,
        tone = VerdictTone.OPTIMAL,
        primaryValue = "$codec${if (is10Bit) " 10-bit" else ""}$fpsText",
        deviceValue = "Hardware decoder",
      )
    } else {
      MetricScore(
        kind = MetricKind.PLAYBACK_EFFICIENCY,
        score = 62,
        verdict = VerdictKey.CPU_HEAVY,
        tone = VerdictTone.ADAPTED,
        primaryValue = "$codec${if (is10Bit) " 10-bit" else ""}$fpsText",
        deviceValue = "Software (CPU) decode",
      )
    }
  }

  private fun evaluateAudio(
    audio: MediaInfoOps.AudioStreamInfo,
    device: DeviceCapabilities,
  ): MetricScore {
    val ch = audio.channelCount.takeIf { it > 0 } ?: 2
    val chLabel = when {
      ch >= 8 -> "7.1 Surround"
      ch == 6 -> "5.1 Surround"
      ch == 2 -> "Stereo (2.0)"
      ch == 1 -> "Mono (1.0)"
      else -> "${ch}ch"
    }
    val kbps = audio.bitRateBps / 1_000L
    val isLossless = audio.compressionMode.equals("Lossless", ignoreCase = true) ||
      audio.format.contains("TrueHD", ignoreCase = true) ||
      audio.format.contains("FLAC", ignoreCase = true) ||
      audio.format.contains("DTS-HD", ignoreCase = true) ||
      audio.format.equals("PCM", ignoreCase = true)

    val codecLabel = normalizeAudioBadge(audio)
    val ratePart = if (kbps > 0) " • $kbps kbps" else ""
    val primary = "$codecLabel • $chLabel$ratePart"

    val (score, verdict, tone) = when {
      (ch > 2 || isLossless) && device.supportsSurroundPassThrough -> {
        Triple(98, VerdictKey.SURROUND_READY, VerdictTone.OPTIMAL)
      }
      ch > 2 && !device.supportsSurroundPassThrough -> {
        Triple(80, VerdictKey.OVERKILL, VerdictTone.OVERKILL)
      }
      isLossless && !device.hasExternalAudio -> {
        Triple(82, VerdictKey.STUDIO_DOWNMIX, VerdictTone.OVERKILL)
      }
      isLossless || kbps >= 192 -> {
        Triple(92, VerdictKey.RICH_AUDIO, VerdictTone.OPTIMAL)
      }
      kbps in 1..95 -> {
        Triple(55, VerdictKey.THIN_AUDIO, VerdictTone.LOW)
      }
      else -> {
        Triple(84, VerdictKey.RICH_AUDIO, VerdictTone.OPTIMAL)
      }
    }

    return MetricScore(
      kind = MetricKind.AUDIO,
      score = score,
      verdict = verdict,
      tone = tone,
      primaryValue = primary,
      deviceValue = device.audioOutputLabel,
    )
  }

  private fun normalizeAudioBadge(audio: MediaInfoOps.AudioStreamInfo): String {
    val commercial = audio.formatCommercial.lowercase()
    val format = audio.format.lowercase()
    return when {
      commercial.contains("atmos") -> "Dolby Atmos"
      commercial.contains("truehd") || format.contains("truehd") -> "TrueHD"
      commercial.contains("dolby digital plus") || format.contains("e-ac-3") -> "Dolby Digital+"
      commercial.contains("dolby digital") || format.contains("ac-3") -> "Dolby Digital"
      commercial.contains("dts:x") -> "DTS:X"
      commercial.contains("dts-hd") -> "DTS-HD"
      format.contains("dts") -> "DTS"
      format.contains("aac") -> "AAC"
      format.contains("opus") -> "Opus"
      format.contains("flac") -> "FLAC"
      format.contains("mp3") || format.contains("mpeg audio") -> "MP3"
      format.contains("vorbis") -> "Vorbis"
      format.contains("pcm") -> "PCM"
      else -> audio.format.ifEmpty { "Audio" }
    }
  }

  private fun buildTechBadges(
    data: MediaInfoOps.MediaInfoData,
    video: MediaInfoOps.VideoStreamInfo?,
    audio: MediaInfoOps.AudioStreamInfo?,
  ): TechBadges {
    val resBadge = if (video != null && video.widthPx > 0 && video.heightPx > 0) {
      classifyVideoTier(video.widthPx, video.heightPx).shortLabel
    } else {
      null
    }

    val videoCodec = video?.format?.takeIf { it.isNotBlank() }?.let(::normalizeVideoCodec)
    val bitDepth = video?.bitDepthBits?.takeIf { it > 0 }?.let { "${it}-bit" }
    val hdr = video?.let(::detectHdrType)?.takeIf { it != HdrType.SDR }

    val audioCommercial = "${audio?.formatCommercial.orEmpty()} ${audio?.format.orEmpty()}".lowercase()
    val isAtmos = audioCommercial.contains("atmos")
    val isDolbyAudio = isAtmos || audioCommercial.contains("dolby") ||
      audioCommercial.contains("ac-3") || audioCommercial.contains("truehd")
    val isDts = audioCommercial.contains("dts")
    val audioBadge = audio?.let(::normalizeAudioBadge)

    val container = when (data.general.format.uppercase()) {
      "MATROSKA" -> "MKV"
      "MPEG-4" -> "MP4"
      "WEBM" -> "WebM"
      "MPEG-TS" -> "TS"
      else -> data.general.format.uppercase().takeIf { it.isNotBlank() }
    }

    val subtitleBadges = data.textStreams
      .mapNotNull { normalizeSubtitleBadge(it).takeIf(String::isNotBlank) }
      .distinct()
    val hasSdhSubtitles = data.textStreams.any(::isSdhSubtitle)

    return TechBadges(
      resolutionBadge = resBadge,
      videoCodecBadge = videoCodec,
      bitDepthBadge = bitDepth,
      hdrBadge = hdr,
      isDolbyAudio = isDolbyAudio,
      isAtmos = isAtmos,
      isDts = isDts,
      audioBadge = audioBadge,
      subtitleBadges = subtitleBadges,
      hasSdhSubtitles = hasSdhSubtitles,
      containerBadge = container,
    )
  }

  fun normalizeSubtitleBadge(sub: MediaInfoOps.TextStreamInfo): String {
    val combined = "${sub.format} ${sub.codecId}".uppercase()
    return when {
      combined.contains("PGS") || combined.contains("HDMV") -> "PGS"
      combined.contains("ASS") -> "ASS"
      combined.contains("SSA") -> "SSA"
      combined.contains("SRT") || combined.contains("SUBRIP") || combined.contains("UTF-8") || combined.contains("UTF8") -> "SRT"
      combined.contains("VOBSUB") -> "DVD"
      combined.contains("WEBVTT") || combined.contains("VTT") -> "VTT"
      combined.contains("TX3G") || combined.contains("MOV_TEXT") -> "TX3G"
      else -> sub.format.trim().uppercase()
    }
  }

  fun isSdhSubtitle(sub: MediaInfoOps.TextStreamInfo): Boolean {
    val title = sub.title.lowercase()
    return title.contains("sdh") ||
      title.contains("hearing") ||
      title.contains("deaf") ||
      Regex("""\b(cc|hi)\b""").containsMatchIn(title)
  }
}
