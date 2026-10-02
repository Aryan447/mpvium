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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.lerp

/**
 * Ambient theme glow: a soft accent spotlight falling from the top edge,
 * cinema-marquee style. Re-tints itself from [MaterialTheme.colorScheme.primary],
 * so every theme (and Material You dynamic color) gets its own hue for free.
 *
 * Applied once at the MainActivity root, above a transparent Surface: the
 * player and media-info screens deliberately stay flat.
 */
private const val GLOW_ALPHA_DARK = 0.34f
private const val GLOW_ALPHA_LIGHT = 0.16f
private const val GLOW_RADIUS_FRACTION = 1.2f
private const val GLOW_ENTER_MS = 900
// Splash gate holds an opaque cover for ~1200ms; without this the bloom
// would play out unseen underneath it.
private const val GLOW_ENTER_DELAY_MS = 900

/** Top-center spotlight. Non-interactive decorative layer: no ripple, no semantics. */
// ponytail: animated entrance only, MainActivity root once. Replay-on-theme-change
// or a drifting loop only if asked for.
@Composable
fun Modifier.themeGlow(): Modifier = composed {
    // Luminance of the resolved scheme honours the app's DarkMode override and
    // AMOLED, unlike isSystemInDarkTheme() which only reports the OS setting.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tint = MaterialTheme.colorScheme.primary
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
        glow(
            tint = tint,
            alpha = enter.value * if (isDark) GLOW_ALPHA_DARK else GLOW_ALPHA_LIGHT,
            radius = size.width * GLOW_RADIUS_FRACTION * lerp(0.55f, 1f, enter.value),
        )
    }
}

private fun DrawScope.glow(tint: Color, alpha: Float, radius: Float) {
    drawRect(
        Brush.radialGradient(
            colors = listOf(tint.copy(alpha = alpha), Color.Transparent),
            center = Offset(size.width / 2f, 0f),
            radius = radius,
        )
    )
}