package app.aryan447.mpvium.ui.player.controls.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.SeekbarStyle
import app.aryan447.mpvium.ui.theme.spacing
import kotlin.math.roundToInt

fun percentage(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
): Float = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

fun percentage(
  value: Int,
  range: ClosedRange<Int>,
): Float = ((value - range.start - 0f) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

@Composable
fun VerticalSlider(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
  overflowValue: Float? = null,
  overflowRange: ClosedFloatingPointRange<Float>? = null,
  seekbarStyle: SeekbarStyle = SeekbarStyle.Thick,
) {
  VerticalBar(
    fraction = percentage(value.coerceIn(range), range),
    overflowFraction =
      if (overflowRange != null && overflowValue != null) {
        percentage(overflowValue, overflowRange)
      } else {
        null
      },
    seekbarStyle = seekbarStyle,
    modifier = modifier,
  )
}

@Composable
fun VerticalSlider(
  value: Int,
  range: ClosedRange<Int>,
  modifier: Modifier = Modifier,
  overflowValue: Int? = null,
  overflowRange: ClosedRange<Int>? = null,
  seekbarStyle: SeekbarStyle = SeekbarStyle.Thick,
) {
  VerticalBar(
    fraction = percentage(value.coerceIn(range), range),
    overflowFraction =
      if (overflowRange != null && overflowValue != null) {
        percentage(overflowValue, overflowRange)
      } else {
        null
      },
    seekbarStyle = seekbarStyle,
    modifier = modifier,
  )
}

/**
 * Vertical counterpart of the seekbar tracks: same [SeekbarStyle] options,
 * primary fill like the seekbar, level rising from the bottom.
 */
@Composable
private fun VerticalBar(
  fraction: Float,
  overflowFraction: Float?,
  seekbarStyle: SeekbarStyle,
  modifier: Modifier = Modifier,
) {
  val animatedFraction by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "vsliderlevel")
  val animatedOverflow by animateFloatAsState(
    (overflowFraction ?: 0f).coerceIn(0f, 1f),
    label = "vslideroverflow",
  )
  when (seekbarStyle) {
    SeekbarStyle.Standard -> StandardVerticalBar(animatedFraction, animatedOverflow, modifier)
    SeekbarStyle.Wavy -> WavyVerticalBar(animatedFraction, animatedOverflow, modifier)
    SeekbarStyle.Thick -> ThickVerticalBar(animatedFraction, animatedOverflow, modifier)
  }
}

@Composable
private fun StandardVerticalBar(
  fraction: Float,
  overflowFraction: Float,
  modifier: Modifier = Modifier,
) {
  val primary = MaterialTheme.colorScheme.primary
  val overflowColor = MaterialTheme.colorScheme.errorContainer
  Canvas(modifier = modifier.height(120.dp).width(24.dp)) {
    val trackWidth = 4.dp.toPx()
    val centerX = size.width / 2f
    val levelY = size.height * (1f - fraction)
    // Unplayed track
    drawRoundRect(
      color = primary.copy(alpha = 0.3f),
      topLeft = Offset(centerX - trackWidth / 2f, 0f),
      size = Size(trackWidth, size.height),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackWidth / 2f),
    )
    // Played portion
    val playedHeight = size.height - levelY
    if (playedHeight > 0.5f) {
      drawRoundRect(
        color = primary,
        topLeft = Offset(centerX - trackWidth / 2f, levelY),
        size = Size(trackWidth, playedHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackWidth / 2f),
      )
    }
    // Volume-boost overflow, stacked from the bottom
    val overflowHeight = size.height * overflowFraction
    if (overflowFraction > 0f && overflowHeight > 0.5f) {
      drawRoundRect(
        color = overflowColor,
        topLeft = Offset(centerX - trackWidth / 2f, size.height - overflowHeight),
        size = Size(trackWidth, overflowHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackWidth / 2f),
      )
    }
    // Thumb
    drawCircle(
      color = primary,
      radius = 7.dp.toPx(),
      center = Offset(centerX, levelY.coerceIn(0f, size.height)),
    )
  }
}

@Composable
private fun ThickVerticalBar(
  fraction: Float,
  overflowFraction: Float,
  modifier: Modifier = Modifier,
) {
  val primary = MaterialTheme.colorScheme.primary
  val overflowColor = MaterialTheme.colorScheme.errorContainer
  Canvas(modifier = modifier.height(120.dp).width(24.dp)) {
    val trackWidth = 16.dp.toPx()
    val thumbHeight = 6.dp.toPx()
    val gapHalf = 7.dp.toPx()
    val outerRadius = trackWidth / 2f
    val innerRadius = 3.dp.toPx()
    val centerX = size.width / 2f
    val levelY = (size.height * (1f - fraction)).coerceIn(0f, size.height)

    fun drawSegment(topY: Float, bottomY: Float, color: Color, topRadius: Float, bottomRadius: Float) {
      if (bottomY - topY < 0.5f) return
      val path = Path()
      path.addRoundRect(
        androidx.compose.ui.geometry.RoundRect(
          left = centerX - trackWidth / 2f,
          top = topY,
          right = centerX + trackWidth / 2f,
          bottom = bottomY,
          topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(topRadius),
          bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(bottomRadius),
          topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(topRadius),
          bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(bottomRadius),
        ),
      )
      drawPath(path, color)
    }

    // Unplayed (top) and played (bottom) with a thumb gap like the horizontal thick bar
    drawSegment(0f, levelY - gapHalf, primary.copy(alpha = 0.3f), outerRadius, innerRadius)
    drawSegment(levelY + gapHalf, size.height, primary, innerRadius, outerRadius)
    // Boost overflow from the bottom
    if (overflowFraction > 0f) {
      drawSegment(size.height * (1f - overflowFraction), size.height, overflowColor, innerRadius, outerRadius)
    }
    // Thumb
    drawSegment(levelY - thumbHeight / 2f, levelY + thumbHeight / 2f, primary, innerRadius, innerRadius)
  }
}

@Composable
private fun WavyVerticalBar(
  fraction: Float,
  overflowFraction: Float,
  modifier: Modifier = Modifier,
) {
  val primary = MaterialTheme.colorScheme.primary
  val overflowColor = MaterialTheme.colorScheme.errorContainer
  Canvas(modifier = modifier.height(120.dp).width(24.dp)) {
    val strokeWidth = 5.dp.toPx()
    val waveLength = 80f
    val amplitude = 6f
    val centerX = size.width / 2f
    val levelY = size.height * (1f - fraction)

    val path = Path()
    val yStart = size.height + waveLength / 2f
    val yEnd = -waveLength / 2f
    path.moveTo(centerX, yStart)
    var currentY = yStart
    var waveSign = 1f
    var currentAmp = waveSign * amplitude
    val dist = waveLength / 2f
    while (currentY > yEnd) {
      waveSign = -waveSign
      val nextY = currentY - dist
      val midY = currentY - dist / 2f
      val nextAmp = waveSign * amplitude
      path.cubicTo(centerX + currentAmp, midY, centerX + nextAmp, midY, centerX + nextAmp, nextY)
      currentAmp = nextAmp
      currentY = nextY
    }

    val waveStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    // Unplayed wave above the level
    clipRect(left = 0f, top = -strokeWidth, right = size.width, bottom = levelY) {
      drawPath(path, primary.copy(alpha = 0.3f), style = waveStyle)
    }
    // Played wave below the level
    clipRect(left = 0f, top = levelY, right = size.width, bottom = size.height + strokeWidth) {
      drawPath(path, primary, style = waveStyle)
    }
    // Boost overflow from the bottom
    if (overflowFraction > 0f) {
      val overflowTop = size.height * (1f - overflowFraction)
      clipRect(left = 0f, top = overflowTop, right = size.width, bottom = size.height + strokeWidth) {
        drawPath(path, overflowColor, style = waveStyle)
      }
    }
    // Thumb tick
    val barHalf = amplitude + strokeWidth
    val thumbY = levelY.coerceIn(0f, size.height)
    drawLine(
      color = primary,
      start = Offset(centerX - barHalf, thumbY),
      end = Offset(centerX + barHalf, thumbY),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round,
    )
  }
}

@Composable
fun BrightnessSlider(
  brightness: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
  seekbarStyle: SeekbarStyle = SeekbarStyle.Thick,
) {
  val coercedBrightness = brightness.coerceIn(range)
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      Text(
        (coercedBrightness * 100).toInt().toString(),
        style = MaterialTheme.typography.bodySmall,
      )
      VerticalSlider(
        coercedBrightness,
        range,
        seekbarStyle = seekbarStyle,
      )
      Icon(
        when (percentage(coercedBrightness, range)) {
          in 0f..0.3f -> Icons.Default.BrightnessLow
          in 0.3f..0.6f -> Icons.Default.BrightnessMedium
          in 0.6f..1f -> Icons.Default.BrightnessHigh
          else -> Icons.Default.BrightnessMedium
        },
        contentDescription = null,
      )
    }
  }
}

@Composable
fun VolumeSlider(
  volume: Int,
  mpvVolume: Int,
  range: ClosedRange<Int>,
  boostRange: ClosedRange<Int>?,
  modifier: Modifier = Modifier,
  displayAsPercentage: Boolean = false,
  seekbarStyle: SeekbarStyle = SeekbarStyle.Thick,
) {
  val percentage = (percentage(volume, range) * 100).roundToInt()
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      val boostVolume = mpvVolume - 100
      Text(
        getVolumeSliderText(volume, mpvVolume, boostVolume, percentage, displayAsPercentage),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
      )
      VerticalSlider(
        if (displayAsPercentage) percentage else volume,
        if (displayAsPercentage) 0..100 else range,
        overflowValue = boostVolume,
        overflowRange = boostRange,
        seekbarStyle = seekbarStyle,
      )
      Icon(
        when (percentage) {
          0 -> Icons.AutoMirrored.Default.VolumeOff
          in 0..30 -> Icons.AutoMirrored.Default.VolumeMute
          in 30..60 -> Icons.AutoMirrored.Default.VolumeDown
          in 60..100 -> Icons.AutoMirrored.Default.VolumeUp
          else -> Icons.AutoMirrored.Default.VolumeOff
        },
        contentDescription = null,
      )
    }
  }
}

val getVolumeSliderText: @Composable (Int, Int, Int, Int, Boolean) -> String =
  { volume, mpvVolume, boostVolume, percentage, displayAsPercentage ->
    when {
      mpvVolume == 100 ->
        if (displayAsPercentage) {
          "$percentage"
        } else {
          "$volume"
        }

      mpvVolume > 100 -> {
        if (displayAsPercentage) {
          "${percentage + boostVolume}"
        } else {
          stringResource(R.string.volume_slider_absolute_value, volume + boostVolume)
        }
      }

      mpvVolume < 100 -> {
        if (displayAsPercentage) {
          "${percentage + boostVolume}"
        } else {
          stringResource(R.string.volume_slider_absolute_value, volume + boostVolume)
        }
      }

      else -> {
        if (displayAsPercentage) {
          "$percentage"
        } else {
          "$volume"
        }
      }
    }
  }
