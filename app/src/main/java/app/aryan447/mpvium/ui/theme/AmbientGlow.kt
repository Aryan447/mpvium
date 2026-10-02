package app.aryan447.mpvium.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset

/**
 * Ambient theme glow: a soft accent bloom rising from the bottom edge,
 * Gemini-style. Replaces the flat background wash with a tinted gradient that
 * re-tints itself from [MaterialTheme.colorScheme.primary], so every theme
 * (and Material You dynamic color) gets its own hue for free.
 *
 * Applied once at the MainActivity root, above a transparent Surface: the
 * player and media-info screens deliberately stay flat.
 */
private const val GLOW_ALPHA_DARK = 0.34f
private const val GLOW_ALPHA_LIGHT = 0.16f
private const val GLOW_RADIUS_FRACTION = 1.2f

/** Bottom-center bloom. Non-interactive decorative layer: no ripple, no semantics. */
// ponytail: static single bloom, MainActivity root only. Add a drifting
// multi-blob layer (or per-screen placement) only if a screen wants its own.
@Composable
fun Modifier.themeGlow(): Modifier = composed {
    // Luminance of the resolved scheme honours the app's DarkMode override and
    // AMOLED, unlike isSystemInDarkTheme() which only reports the OS setting.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tint = MaterialTheme.colorScheme.primary
    drawBehind { glow(tint, if (isDark) GLOW_ALPHA_DARK else GLOW_ALPHA_LIGHT) }
}

private fun DrawScope.glow(tint: Color, alpha: Float) {
    // Radius tracks width, not height, so the bloom stays a bottom-edge band on
    // tall phones instead of washing halfway up the screen.
    drawRect(
        Brush.radialGradient(
            colors = listOf(tint.copy(alpha = alpha), Color.Transparent),
            center = Offset(size.width / 2f, size.height),
            radius = size.width * GLOW_RADIUS_FRACTION,
        )
    )
}