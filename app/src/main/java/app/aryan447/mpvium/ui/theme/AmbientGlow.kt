package app.aryan447.mpvium.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.util.lerp
import kotlin.math.min

/**
 * Ambient theme glow: a soft accent spotlight falling from the top edge,
 * cinema-marquee style. Re-tints itself from [MaterialTheme.colorScheme.primary],
 * so every theme (and Material You dynamic color) gets its own hue for free.
 *
 * Applied once at the MainActivity root, above a transparent Surface: the
 * player and media-info screens deliberately stay flat.
 */
private const val GLOW_ALPHA_DARK = 0.32f
private const val GLOW_ALPHA_LIGHT = 0.15f
private const val GLOW_ALPHA_AMOLED = 0.40f
private const val GLOW_WIDTH_FRACTION = 1.05f
private const val GLOW_HEIGHT_FRACTION = 0.48f
private const val GLOW_CENTER_Y_FRACTION = -0.06f
private const val GLOW_ENTER_MS = 900
// Splash gate holds an opaque cover for ~1200ms; without this the bloom
// would play out unseen underneath it.
private const val GLOW_ENTER_DELAY_MS = 900

/**
 * Top-center spotlight. Non-interactive decorative layer: no ripple, no semantics.
 *
 * The glow radius is bounded by both screen width and height so the bloom ends
 * around the middle of the screen instead of tinting the full page. AMOLED
 * backgrounds also receive a pure-black base, which makes the falloff resolve
 * to black instead of the window's default grey.
 */
// ponytail: animated entrance only, MainActivity root once. Replay-on-theme-change
// or a drifting loop only if asked for.
@Composable
fun Modifier.themeGlow(): Modifier = composed {
  // Luminance of the resolved scheme honours the app's DarkMode override and
  // AMOLED, unlike isSystemInDarkTheme() which only reports the OS setting.
  val background = MaterialTheme.colorScheme.background
  val isDark = background.luminance() < 0.5f
  val isAmoled = isDark && background.luminance() < 0.001f
  val tint = MaterialTheme.colorScheme.primary
  val accent = MaterialTheme.colorScheme.tertiary
  val enter = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    enter.animateTo(
      targetValue = 1f,
      animationSpec = tween(
        durationMillis = GLOW_ENTER_MS,
        delayMillis = GLOW_ENTER_DELAY_MS,
        easing = FastOutSlowInEasing,
      ),
    )
  }
  drawBehind {
    drawRect(background)
    glow(
      tint = tint,
      accent = accent,
      alpha = enter.value * when {
        isAmoled -> GLOW_ALPHA_AMOLED
        isDark -> GLOW_ALPHA_DARK
        else -> GLOW_ALPHA_LIGHT
      },
      radius = min(
        size.width * GLOW_WIDTH_FRACTION,
        size.height * GLOW_HEIGHT_FRACTION,
      ) * lerp(0.72f, 1f, enter.value),
      center = Offset(size.width / 2f, size.height * GLOW_CENTER_Y_FRACTION),
    )
  }
}

private fun DrawScope.glow(
  tint: Color,
  accent: Color,
  alpha: Float,
  radius: Float,
  center: Offset,
) {
  // Broad, soft wash: the main premium bloom.
  drawRect(
    Brush.radialGradient(
      colorStops = arrayOf(
        0f to tint.copy(alpha = alpha),
        0.34f to tint.copy(alpha = alpha * 0.62f),
        0.68f to tint.copy(alpha = alpha * 0.20f),
        0.88f to tint.copy(alpha = alpha * 0.05f),
        1f to Color.Transparent,
      ),
      center = center,
      radius = radius,
    ),
  )

  // Tighter core, offset to the left so the light feels directional.
  drawRect(
    Brush.radialGradient(
      colorStops = arrayOf(
        0f to tint.copy(alpha = alpha * 0.55f),
        0.55f to tint.copy(alpha = alpha * 0.16f),
        1f to Color.Transparent,
      ),
      center = Offset(center.x - radius * 0.14f, center.y),
      radius = radius * 0.48f,
    ),
  )

  // A whisper of tertiary on the opposite side adds depth without a new focal point.
  drawRect(
    Brush.radialGradient(
      colorStops = arrayOf(
        0f to accent.copy(alpha = alpha * 0.22f),
        0.62f to accent.copy(alpha = alpha * 0.05f),
        1f to Color.Transparent,
      ),
      center = Offset(center.x + radius * 0.20f, center.y),
      radius = radius * 0.58f,
    ),
  )
}
