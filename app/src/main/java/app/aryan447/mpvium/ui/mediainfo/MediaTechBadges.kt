package app.aryan447.mpvium.ui.mediainfo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HearingDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassRimStroke
import app.aryan447.mpvium.ui.theme.spacing

/**
 * Flow row of Material 3 vector tech badges (Dolby Vision/Atmos, HDR10+, 4K/HD, HEVC, 10-bit, DTS, etc.).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TechBadgeRow(
  badges: MediaCapabilityAnalyzer.TechBadges,
  modifier: Modifier = Modifier,
) {
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
  ) {
    badges.resolutionBadge?.let { res ->
      TechBadgeChip(
        label = res,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        logo = { color -> ResolutionLogoMark(color = color) },
      )
    }

    badges.hdrBadge?.let { hdr ->
      val isDolbyVision = hdr == MediaCapabilityAnalyzer.HdrType.DOLBY_VISION
      TechBadgeChip(
        label = hdr.label,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        logo = { color ->
          if (isDolbyVision) {
            DolbyLogoMark(color = color)
          } else {
            HdrLogoMark(color = color, hasPlus = hdr == MediaCapabilityAnalyzer.HdrType.HDR10_PLUS)
          }
        },
      )
    }

    badges.videoCodecBadge?.let { codec ->
      TechBadgeChip(
        label = codec,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        logo = { color -> CodecLogoMark(color = color) },
      )
    }

    badges.bitDepthBadge?.let { depth ->
      TechBadgeChip(
        label = depth,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        logo = { color -> BitDepthLogoMark(color = color) },
      )
    }

    badges.audioBadge?.let { audio ->
      TechBadgeChip(
        label = audio,
        containerColor = if (badges.isDolbyAudio || badges.isDts) {
          MaterialTheme.colorScheme.primaryContainer
        } else {
          MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (badges.isDolbyAudio || badges.isDts) {
          MaterialTheme.colorScheme.onPrimaryContainer
        } else {
          MaterialTheme.colorScheme.onSecondaryContainer
        },
        logo = { color ->
          when {
            badges.isAtmos -> AtmosLogoMark(color = color)
            badges.isDolbyAudio -> DolbyLogoMark(color = color)
            badges.isDts -> DtsLogoMark(color = color)
            else -> AudioWaveLogoMark(color = color)
          }
        },
      )
    }

    badges.subtitleBadges.forEach { sub ->
      TechBadgeChip(
        label = sub,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        logo = { color -> SubtitleLogoMark(color = color) },
        trailing = if (badges.hasSdhSubtitles) {
          { color ->
            Icon(
              imageVector = Icons.Filled.HearingDisabled,
              contentDescription = null,
              tint = color,
              modifier = Modifier.size(15.dp),
            )
          }
        } else {
          null
        },
      )
    }

    badges.containerBadge?.let { container ->
      TechBadgeChip(
        label = container,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        logo = { color -> ContainerLogoMark(color = color) },
      )
    }
  }
}

@Composable
private fun TechBadgeChip(
  label: String,
  containerColor: Color,
  contentColor: Color,
  logo: @Composable (Color) -> Unit,
  trailing: (@Composable (Color) -> Unit)? = null,
) {
  val isGlass = LocalGlass.current
  val dark = isSystemInDarkTheme()
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = containerColor,
    border = if (isGlass) {
      glassRimStroke(dark)
    } else {
      BorderStroke(0.75.dp, contentColor.copy(alpha = 0.16f))
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      logo(contentColor)
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = contentColor,
      )
      if (trailing != null) {
        Spacer(modifier = Modifier.width(5.dp))
        trailing(contentColor)
      }
    }
  }
}

/**
 * Iconic Dolby double-D emblem: two facing solid D shapes inside a clean frame.
 */
@Composable
fun DolbyLogoMark(
  color: Color,
  modifier: Modifier = Modifier.size(16.dp),
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val gap = w * 0.08f
    val halfW = (w - gap) / 2f
    val top = h * 0.14f
    val bottom = h * 0.86f
    val boxH = bottom - top

    // Left D (flat on left, curved on right)
    val leftD = Path().apply {
      moveTo(0f, top)
      lineTo(halfW * 0.45f, top)
      cubicTo(halfW * 1.05f, top, halfW * 1.05f, bottom, halfW * 0.45f, bottom)
      lineTo(0f, bottom)
      close()
    }
    drawPath(leftD, color = color)

    // Right mirrored D (curved on left, flat on right)
    val startX = halfW + gap
    val rightD = Path().apply {
      moveTo(w, top)
      lineTo(startX + halfW * 0.55f, top)
      cubicTo(
        startX - halfW * 0.05f,
        top,
        startX - halfW * 0.05f,
        bottom,
        startX + halfW * 0.55f,
        bottom,
      )
      lineTo(w, bottom)
      close()
    }
    drawPath(rightD, color = color)

    // Subtle inner cutouts for true double-D silhouette
    val holeH = boxH * 0.44f
    val holeTop = top + (boxH - holeH) / 2f
    drawRoundRect(
      color = color.copy(alpha = 0f),
      topLeft = Offset(halfW * 0.25f, holeTop),
      size = Size(halfW * 0.2f, holeH),
    )
  }
}

/**
 * Dolby Atmos emblem: double-D with overhead spatial sound arcs.
 */
@Composable
fun AtmosLogoMark(
  color: Color,
  modifier: Modifier = Modifier.size(16.dp),
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = w * 0.1f, cap = StrokeCap.Round)

    // Overhead Atmos spatial arcs
    drawArc(
      color = color,
      startAngle = 210f,
      sweepAngle = 120f,
      useCenter = false,
      topLeft = Offset(w * 0.12f, h * 0.06f),
      size = Size(w * 0.76f, h * 0.46f),
      style = stroke,
    )
    drawArc(
      color = color,
      startAngle = 220f,
      sweepAngle = 100f,
      useCenter = false,
      topLeft = Offset(w * 0.26f, h * 0.20f),
      size = Size(w * 0.48f, h * 0.30f),
      style = stroke,
    )

    // Compact double-D base
    val baseTop = h * 0.50f
    val baseBottom = h * 0.92f
    val mid = w * 0.5f
    val leftD = Path().apply {
      moveTo(w * 0.1f, baseTop)
      lineTo(mid * 0.55f, baseTop)
      cubicTo(mid * 0.92f, baseTop, mid * 0.92f, baseBottom, mid * 0.55f, baseBottom)
      lineTo(w * 0.1f, baseBottom)
      close()
    }
    val rightD = Path().apply {
      moveTo(w * 0.9f, baseTop)
      lineTo(mid + mid * 0.45f, baseTop)
      cubicTo(mid + mid * 0.08f, baseTop, mid + mid * 0.08f, baseBottom, mid + mid * 0.45f, baseBottom)
      lineTo(w * 0.9f, baseBottom)
      close()
    }
    drawPath(leftD, color = color)
    drawPath(rightD, color = color)
  }
}

/**
 * Framed widescreen cinema emblem for 4K / 8K / 1080p / HD.
 */
@Composable
fun ResolutionLogoMark(
  color: Color,
  modifier: Modifier = Modifier.size(16.dp),
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokeW = w * 0.11f
    drawRoundRect(
      color = color,
      topLeft = Offset(w * 0.06f, h * 0.18f),
      size = Size(w * 0.88f, h * 0.64f),
      cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
      style = Stroke(width = strokeW),
    )
    // Inner aspect-ratio corner ticks
    val tick = w * 0.18f
    drawLine(color, Offset(w * 0.24f, h * 0.38f), Offset(w * 0.24f + tick, h * 0.38f), strokeW, StrokeCap.Round)
    drawLine(color, Offset(w * 0.24f, h * 0.38f), Offset(w * 0.24f, h * 0.38f + tick), strokeW, StrokeCap.Round)
    drawLine(color, Offset(w * 0.76f, h * 0.62f), Offset(w * 0.76f - tick, h * 0.62f), strokeW, StrokeCap.Round)
    drawLine(color, Offset(w * 0.76f, h * 0.62f), Offset(w * 0.76f, h * 0.62f - tick), strokeW, StrokeCap.Round)
  }
}

/**
 * HDR sunburst / dynamic range contrast emblem.
 */
@Composable
fun HdrLogoMark(
  color: Color,
  hasPlus: Boolean = false,
  modifier: Modifier = Modifier.size(16.dp),
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.5f
    val r = w * 0.22f
    val strokeW = w * 0.1f

    drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(strokeW))
    val rays = 6
    for (i in 0 until rays) {
      val angle = Math.toRadians((i * 60).toDouble())
      val x1 = cx + (r * 1.45f * kotlin.math.cos(angle)).toFloat()
      val y1 = cy + (r * 1.45f * kotlin.math.sin(angle)).toFloat()
      val x2 = cx + (r * 2.0f * kotlin.math.cos(angle)).toFloat()
      val y2 = cy + (r * 2.0f * kotlin.math.sin(angle)).toFloat()
      drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeW, StrokeCap.Round)
    }

    if (hasPlus) {
      drawLine(color, Offset(w * 0.76f, h * 0.22f), Offset(w * 0.96f, h * 0.22f), strokeW, StrokeCap.Round)
      drawLine(color, Offset(w * 0.86f, h * 0.12f), Offset(w * 0.86f, h * 0.32f), strokeW, StrokeCap.Round)
    }
  }
}

/**
 * Codec compression block / film frame emblem (HEVC, AV1, H.264, VP9).
 */
@Composable
fun CodecLogoMark(
  color: Color,
  modifier: Modifier = Modifier.size(16.dp),
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokeW = w * 0.1f
    drawRoundRect(
      color = color,
      topLeft = Offset(w * 0.14f, h * 0.14f),
      size = Size(w * 0.72f, h * 0.72f),
      cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
      style = Stroke(strokeW),
    )
    // Play triangle inside chip
    val play = Path().apply {
      moveTo(w * 0.40f, h * 0.33f)
      lineTo(w * 0.68f, h * 0.50f)
      lineTo(w * 0.40f, h * 0.67f)
      close()
    }
    drawPath(play, color = color)
  }
}

/**
 * Stacked color-depth planes emblem for 10-bit / 12-bit.
 */
@Composable
fun BitDepthLogoMark(
  color: Color,
  modifier: Modifier = Modifier.size(16.dp),
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokeW = w * 0.1f

    val topDiamond = Path().apply {
      moveTo(w * 0.5f, h * 0.12f)
      lineTo(w * 0.88f, h * 0.36f)
      lineTo(w * 0.5f, h * 0.60f)
      lineTo(w * 0.12f, h * 0.36f)
      close()
    }
    drawPath(topDiamond, color = color, style = Stroke(strokeW, join = StrokeJoin.Round))

    val bottomPlane = Path().apply {
      moveTo(w * 0.16f, h * 0.58f)
      lineTo(w * 0.5f, h * 0.80f)
      lineTo(w * 0.84f, h * 0.58f)
    }
    drawPath(bottomPlane, color = color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
  }
}

/**
 * DTS geometric surround-channel emblem.
 */
@Composable
fun DtsLogoMark(
  color: Color,
  modifier: Modifier = Modifier.size(16.dp),
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokeW = w * 0.11f
    drawCircle(color = color, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.5f))
    drawArc(
      color = color,
      startAngle = -55f,
      sweepAngle = 110f,
      useCenter = false,
      topLeft = Offset(w * 0.14f, h * 0.14f),
      size = Size(w * 0.72f, h * 0.72f),
      style = Stroke(strokeW, cap = StrokeCap.Round),
    )
    drawArc(
      color = color,
      startAngle = 125f,
      sweepAngle = 110f,
      useCenter = false,
      topLeft = Offset(w * 0.14f, h * 0.14f),
      size = Size(w * 0.72f, h * 0.72f),
      style = Stroke(strokeW, cap = StrokeCap.Round),
    )
  }
}

/**
 * Audio equalizer waveform emblem for FLAC / AAC / Opus / MP3.
 */
@Composable
fun AudioWaveLogoMark(
  color: Color,
  modifier: Modifier = Modifier.size(16.dp),
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val barW = w * 0.13f
    val heights = listOf(0.40f, 0.76f, 0.56f, 0.86f)
    val xs = listOf(0.14f, 0.36f, 0.58f, 0.80f)
    heights.forEachIndexed { idx, frac ->
      val barH = h * frac
      val top = (h - barH) / 2f
      drawLine(
        color = color,
        start = Offset(w * xs[idx], top),
        end = Offset(w * xs[idx], top + barH),
        strokeWidth = barW,
        cap = StrokeCap.Round,
      )
    }
  }
}

/**
 * Rounded subtitle box emblem with two caption lines (SRT, ASS, PGS, VTT).
 */
@Composable
fun SubtitleLogoMark(
  color: Color,
  modifier: Modifier = Modifier.size(16.dp),
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokeW = w * 0.11f
    drawRoundRect(
      color = color,
      topLeft = Offset(w * 0.08f, h * 0.16f),
      size = Size(w * 0.84f, h * 0.68f),
      cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
      style = Stroke(width = strokeW),
    )
    drawLine(
      color = color,
      start = Offset(w * 0.28f, h * 0.42f),
      end = Offset(w * 0.72f, h * 0.42f),
      strokeWidth = strokeW,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = color,
      start = Offset(w * 0.28f, h * 0.60f),
      end = Offset(w * 0.56f, h * 0.60f),
      strokeWidth = strokeW,
      cap = StrokeCap.Round,
    )
  }
}

/**
 * Media container box emblem (MKV / MP4 / WebM).
 */
@Composable
fun ContainerLogoMark(
  color: Color,
  modifier: Modifier = Modifier.size(16.dp),
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokeW = w * 0.1f
    drawRoundRect(
      color = color,
      topLeft = Offset(w * 0.14f, h * 0.22f),
      size = Size(w * 0.72f, h * 0.58f),
      cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
      style = Stroke(strokeW),
    )
    drawLine(color, Offset(w * 0.16f, h * 0.42f), Offset(w * 0.84f, h * 0.42f), strokeW)
  }
}
