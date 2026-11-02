package app.aryan447.mpvium.ui.player

import android.content.Context
import app.aryan447.mpvium.R
import `is`.xyz.mpv.MPVNode
import kotlin.math.roundToInt

/**
 * Deterministic, pure-function resolver for MPV demuxer cache options and seekable time windows.
 *
 * Implements Spec 17 "Lean seek cache":
 * - Safe combined budget ≈ 10% of ActivityManager.memoryClass, capped at 96 MB forward / 128 MB back.
 * - Under pressure, shrinks backward cache first, then forward.
 * - Sizing follows bitrate confidence: valid video-bitrate -> file-size/duration -> resolution fallback.
 * - Preserves exact legacy constants when OFF.
 */
object SeekCacheResolver {
  // Legacy constants (when leanSeekCache is OFF)
  const val LEGACY_DEMUXER_MAX_BYTES_NORMAL = 64L * 1024 * 1024
  const val LEGACY_DEMUXER_MAX_BYTES_LOW_API = 32L * 1024 * 1024
  const val LEGACY_DEMUXER_MAX_BACK_BYTES_NORMAL = 128L * 1024 * 1024
  const val LEGACY_DEMUXER_MAX_BACK_BYTES_LOW_API = 32L * 1024 * 1024
  const val LEGACY_DEMUXER_READAHEAD_SECS = 10.0
  const val LEGACY_CACHE_SECS = 20.0
  const val LEGACY_STREAM_BUFFER_SIZE = 1024L * 1024

  // Ceilings when leanSeekCache is ON
  const val MAX_FORWARD_BYTES = 96L * 1024 * 1024       // 96 MB
  const val MAX_BACKWARD_BYTES = 128L * 1024 * 1024     // 128 MB
  const val MAX_COMBINED_BYTES = MAX_FORWARD_BYTES + MAX_BACKWARD_BYTES // 224 MB

  // Operational floors
  const val MIN_SAFE_BUDGET_BYTES = 8L * 1024 * 1024
  const val LOW_RAM_MAX_BUDGET_BYTES = 48L * 1024 * 1024

  // Target window
  const val NOMINAL_TARGET_SECS = 180.0 // ~3 min target

  // Resolution bitrate fallbacks in bits per second
  const val BITRATE_4K = 40_000_000L   // 40 Mbps
  const val BITRATE_1080P = 8_000_000L // 8 Mbps
  const val BITRATE_720P = 3_000_000L  // 3 Mbps
  const val BITRATE_SD = 1_500_000L    // 1.5 Mbps
  const val BITRATE_AUDIO = 256_000L   // 256 kbps

  // Modest audio & container overhead multiplier on video bitrate
  const val OVERHEAD_MULTIPLIER = 1.10

  enum class SourceType {
    Local,
    Network,
  }

  data class Input(
    val memoryClassMb: Int,
    val isLowRamDevice: Boolean = false,
    val sourceType: SourceType = SourceType.Local,
    val videoWidth: Int? = null,
    val videoHeight: Int? = null,
    val videoBitrate: Long? = null, // in bps
    val fileSizeBytes: Long? = null,
    val durationSeconds: Double? = null,
    val isAudioOnly: Boolean = false,
    val isLive: Boolean = false,
  )

  data class Profile(
    val demuxerMaxBytes: Long,
    val demuxerMaxBackBytes: Long,
    val demuxerReadaheadSecs: Double,
    val cacheSecs: Double,
    val streamBufferSize: Long = 1024L * 1024,
    val estimatedForwardSecs: Double? = null,
    val estimatedBackSecs: Double? = null,
    val effectiveBitrateBps: Long? = null,
    val safeCombinedBudgetBytes: Long = 0L,
    val sourceType: SourceType = SourceType.Local,
  )

  data class SeekableRange(
    val start: Float,
    val end: Float,
  )

  /**
   * Resolves the legacy configuration used when Lean seek cache is OFF.
   */
  fun resolveLegacy(sdkInt: Int): Profile {
    val forward = if (sdkInt >= android.os.Build.VERSION_CODES.O_MR1) {
      LEGACY_DEMUXER_MAX_BYTES_NORMAL
    } else {
      LEGACY_DEMUXER_MAX_BYTES_LOW_API
    }
    val back = if (sdkInt >= android.os.Build.VERSION_CODES.O_MR1) {
      LEGACY_DEMUXER_MAX_BACK_BYTES_NORMAL
    } else {
      LEGACY_DEMUXER_MAX_BACK_BYTES_LOW_API
    }
    return Profile(
      demuxerMaxBytes = forward,
      demuxerMaxBackBytes = back,
      demuxerReadaheadSecs = LEGACY_DEMUXER_READAHEAD_SECS,
      cacheSecs = LEGACY_CACHE_SECS,
      streamBufferSize = LEGACY_STREAM_BUFFER_SIZE,
      safeCombinedBudgetBytes = forward + back,
      sourceType = SourceType.Local,
    )
  }

  /**
   * Resolves dynamic demuxer options and estimated seek windows when Lean seek cache is ON.
   */
  fun resolve(input: Input): Profile {
    val safeBudget = computeSafeBudget(input.memoryClassMb, input.isLowRamDevice)

    // Fallback for live, invalid duration, or audio-only without known video
    if (input.isLive || (input.durationSeconds != null && input.durationSeconds <= 0.0)) {
      return conservativeFallback(safeBudget, input.sourceType)
    }

    // Determine effective bitrate via confidence order
    val effectiveBitrate = determineEffectiveBitrate(input)
    if (effectiveBitrate == null || effectiveBitrate <= 0L) {
      return conservativeFallback(safeBudget, input.sourceType)
    }

    // Sizing targets for ~45-180 s of media per direction
    val bytesPerSec = effectiveBitrate / 8.0
    val targetBytesPerDirection = (bytesPerSec * NOMINAL_TARGET_SECS).toLong()

    // Clamp by absolute ceilings
    var forwardBytes = minOf(targetBytesPerDirection, MAX_FORWARD_BYTES)
    var backBytes = minOf(targetBytesPerDirection, MAX_BACKWARD_BYTES)

    // Adjust for network vs local source bias
    if (input.sourceType == SourceType.Network) {
      // Network stalls on re-reads; bias toward deeper forward window
      forwardBytes = minOf(MAX_FORWARD_BYTES, (targetBytesPerDirection * 1.15).toLong())
    } else {
      // Local scrubbing benefits from solid backward cache
      backBytes = minOf(MAX_BACKWARD_BYTES, (targetBytesPerDirection * 1.10).toLong())
    }

    // Clamp by safe combined memory budget: under pressure shrink backward first, then forward
    if (forwardBytes + backBytes > safeBudget) {
      val excess = (forwardBytes + backBytes) - safeBudget
      val minBack = minOf(16L * 1024 * 1024, safeBudget / 4)
      val backShrinkable = maxOf(0L, backBytes - minBack)
      val shrinkBack = minOf(excess, backShrinkable)
      backBytes -= shrinkBack
      val remainingExcess = excess - shrinkBack
      if (remainingExcess > 0) {
        forwardBytes = maxOf(8L * 1024 * 1024, forwardBytes - remainingExcess)
      }

      // If budget is extraordinarily tight, enforce strict combined clamp
      if (forwardBytes + backBytes > safeBudget) {
        val overflow = (forwardBytes + backBytes) - safeBudget
        if (backBytes >= overflow) {
          backBytes -= overflow
        } else {
          val rem = overflow - backBytes
          backBytes = 0L
          forwardBytes = maxOf(4L * 1024 * 1024, forwardBytes - rem)
        }
      }
    }

    // Never exceed absolute ceilings
    forwardBytes = minOf(forwardBytes, MAX_FORWARD_BYTES)
    backBytes = minOf(backBytes, MAX_BACKWARD_BYTES)

    // Derive demuxer-readahead-secs from forward bytes + bitrate (do not hardcode 10 s when ON)
    val forwardSecs = (forwardBytes * 8.0) / effectiveBitrate
    val backSecs = (backBytes * 8.0) / effectiveBitrate
    val readaheadSecs = forwardSecs.coerceIn(10.0, 180.0)

    // Network: bias toward higher cache-secs / readahead when budget supports it
    val cacheSecs = if (input.sourceType == SourceType.Network) {
      maxOf(20.0, readaheadSecs * 1.5).coerceAtMost(300.0)
    } else {
      maxOf(20.0, readaheadSecs).coerceAtMost(300.0)
    }

    return Profile(
      demuxerMaxBytes = forwardBytes,
      demuxerMaxBackBytes = backBytes,
      demuxerReadaheadSecs = readaheadSecs,
      cacheSecs = cacheSecs,
      streamBufferSize = LEGACY_STREAM_BUFFER_SIZE,
      estimatedForwardSecs = forwardSecs,
      estimatedBackSecs = backSecs,
      effectiveBitrateBps = effectiveBitrate,
      safeCombinedBudgetBytes = safeBudget,
      sourceType = input.sourceType,
    )
  }

  /**
   * Computes safe combined budget ≈ 10% of ActivityManager.memoryClass,
   * under conservative ceilings and memory-pressure constraints.
   */
  fun computeSafeBudget(memoryClassMb: Int, isLowRamDevice: Boolean): Long {
    if (memoryClassMb <= 0) return 32L * 1024 * 1024
    val rawBytes = (memoryClassMb * 1024L * 1024L * 0.10).toLong()
    val constrained = if (isLowRamDevice) minOf(rawBytes, LOW_RAM_MAX_BUDGET_BYTES) else rawBytes
    return minOf(constrained, MAX_COMBINED_BYTES).coerceAtLeast(MIN_SAFE_BUDGET_BYTES)
  }

  /**
   * Bitrate confidence order:
   * 1. Valid video-bitrate (+ audio/container overhead)
   * 2. File-size/duration estimate (when both reliable)
   * 3. Resolution fallback (720p ≈ 3 Mbps, 1080p ≈ 8 Mbps, 4K ≈ 40 Mbps)
   */
  fun determineEffectiveBitrate(input: Input): Long? {
    if (input.isAudioOnly) {
      return (BITRATE_AUDIO * OVERHEAD_MULTIPLIER).toLong()
    }
    if (input.videoBitrate != null && input.videoBitrate > 0) {
      return (input.videoBitrate * OVERHEAD_MULTIPLIER).toLong()
    }
    if (input.fileSizeBytes != null && input.fileSizeBytes > 100_000L &&
      input.durationSeconds != null && input.durationSeconds > 1.0) {
      return ((input.fileSizeBytes * 8.0) / input.durationSeconds).toLong()
    }
    val w = input.videoWidth ?: 0
    val h = input.videoHeight ?: 0
    if (w > 0 || h > 0) {
      val baseBitrate = when {
        h >= 1440 || w >= 2560 -> BITRATE_4K
        h >= 900 || w >= 1600 -> BITRATE_1080P
        h >= 600 || w >= 1000 -> BITRATE_720P
        else -> BITRATE_SD
      }
      return (baseBitrate * OVERHEAD_MULTIPLIER).toLong()
    }
    return null
  }

  private fun conservativeFallback(safeBudget: Long, sourceType: SourceType): Profile {
    val fBytes = minOf(32L * 1024 * 1024, safeBudget / 2)
    val bBytes = minOf(32L * 1024 * 1024, safeBudget / 2)
    return Profile(
      demuxerMaxBytes = fBytes,
      demuxerMaxBackBytes = bBytes,
      demuxerReadaheadSecs = LEGACY_DEMUXER_READAHEAD_SECS,
      cacheSecs = LEGACY_CACHE_SECS,
      streamBufferSize = LEGACY_STREAM_BUFFER_SIZE,
      safeCombinedBudgetBytes = safeBudget,
      sourceType = sourceType,
    )
  }

  /**
   * Formats settings subtitle according to Requirement 5:
   * OFF: Static legacy explanation.
   * ON: Dynamic values resolved from this device's memoryClass.
   */
  fun formatSettingsSubtitle(
    isOn: Boolean,
    memoryClassMb: Int,
    isLowRamDevice: Boolean,
    context: Context,
  ): String {
    if (!isOn) {
      return context.getString(R.string.pref_advanced_lean_seek_cache_summary_off)
    }

    // Resolve device baseline using standard 1080p reference
    val profile = resolve(
      Input(
        memoryClassMb = memoryClassMb,
        isLowRamDevice = isLowRamDevice,
        sourceType = SourceType.Local,
        videoWidth = 1920,
        videoHeight = 1080,
      )
    )

    val forwardMb = (profile.demuxerMaxBytes / (1024 * 1024)).toInt()
    val backMb = (profile.demuxerMaxBackBytes / (1024 * 1024)).toInt()

    val fSecs = profile.estimatedForwardSecs
    val bSecs = profile.estimatedBackSecs

    return if (fSecs != null && bSecs != null && fSecs > 0 && bSecs > 0) {
      val avgSecs = ((fSecs + bSecs) / 2.0).roundToInt()
      val timeFormatted = formatDurationMinutesSeconds(avgSecs)
      context.getString(
        R.string.pref_advanced_lean_seek_cache_summary_on,
        forwardMb,
        backMb,
        timeFormatted,
      )
    } else {
      context.getString(
        R.string.pref_advanced_lean_seek_cache_summary_on_fallback,
        forwardMb,
        backMb,
      )
    }
  }

  fun formatDurationMinutesSeconds(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return when {
      mins > 0 && secs > 0 -> "$mins min $secs s"
      mins > 0 -> "$mins min"
      else -> "$secs s"
    }
  }

  /**
   * Parses the MPVNode for demuxer-cache-state into a list of seekable ranges.
   */
  fun parseDemuxerCacheRanges(node: MPVNode?): List<SeekableRange> {
    if (node == null || node is MPVNode.None) return emptyList()
    val rangesNode = node.get("seekable-ranges") ?: return emptyList()
    val array = runCatching { rangesNode.asArray() }.getOrNull() ?: return emptyList()
    val result = ArrayList<SeekableRange>(array.size)
    for (item in array) {
      val start = runCatching { item.get("start")?.asDouble() }.getOrNull()
      val end = runCatching { item.get("end")?.asDouble() }.getOrNull()
      if (start != null && end != null && end > start) {
        result.add(SeekableRange(start.toFloat(), end.toFloat()))
      }
    }
    return result
  }
}
