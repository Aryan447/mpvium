package app.aryan447.mpvium.ui.player.controls

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.aryan447.mpvium.ui.theme.controlColor
import app.aryan447.mpvium.ui.theme.glassPlayerAlpha

/**
 * Colors for the on-screen player controls, resolved from the active app
 * theme and the user's "Full player colors" setting.
 */
@Immutable
data class PlayerColors(
  // Circular buttons, pills, badges and slider panels.
  val controlFill: Color,
  val controlBorder: Color,
  val controlContent: Color,
  // Icons/text floating directly over video (button backgrounds hidden).
  val onVideoContent: Color,
  // Theme accent for progress and indicators (seekbar, sliders).
  val accent: Color,
  val accentContent: Color,
  // Toggled-on controls (repeat, shuffle, mirror, ...).
  val activeFill: Color,
  val activeContent: Color,
  val activeOnVideoContent: Color,
  // Hero play/pause button.
  val playPauseFill: Color,
  val playPauseContent: Color,
  // Seek timers.
  val timerContent: Color,
  // Skip-intro / shader-peek chip.
  val chipFill: Color,
  val chipContent: Color,
  val chipIconContent: Color,
)

/**
 * Fallback palette for @Preview composables. The player always provides a
 * theme-resolved palette through [LocalPlayerColors].
 */
private val DefaultPlayerColors =
  PlayerColors(
    controlFill = Color(0x8C49454F),
    controlBorder = Color(0x66CAC4D0),
    controlContent = Color(0xFF1D1B20),
    onVideoContent = Color.White,
    accent = Color(0xFF6750A4),
    accentContent = Color.White,
    activeFill = Color(0x8C49454F),
    activeContent = Color(0xFF6750A4),
    activeOnVideoContent = Color(0xFF6750A4),
    playPauseFill = Color(0x8C49454F),
    playPauseContent = Color(0xFF1D1B20),
    timerContent = Color.White,
    chipFill = Color(0xF2ECE6F0),
    chipContent = Color(0xFF1D1B20),
    chipIconContent = Color.White,
  )

val LocalPlayerColors = staticCompositionLocalOf { DefaultPlayerColors }

@Composable
fun playerColorsFor(fullColors: Boolean): PlayerColors {
  val scheme = MaterialTheme.colorScheme
  // Player chrome sits on top of video, not on an app surface. A light
  // scheme's `primary` is dark because it targets light surfaces, so over
  // video the bright inverse primary is the readable accent; dark schemes
  // already carry the bright variant in `primary`.
  val onVideoAccent =
    if (scheme.background.luminance() < 0.5f) scheme.primary else scheme.inversePrimary
  val neutralFill = scheme.surfaceContainer.copy(alpha = glassPlayerAlpha())
  val neutralBorder = scheme.outlineVariant.copy(alpha = 0.4f)
  if (!fullColors) {
    // Existing player look: neutral chrome with the seekbar, sliders and
    // toggled-on icons wearing the theme accent.
    return PlayerColors(
      controlFill = neutralFill,
      controlBorder = neutralBorder,
      controlContent = scheme.onSurface,
      onVideoContent = controlColor,
      accent = scheme.primary,
      accentContent = scheme.onPrimary,
      activeFill = neutralFill,
      activeContent = scheme.primary,
      activeOnVideoContent = scheme.primary,
      playPauseFill = neutralFill,
      playPauseContent = scheme.onSurface,
      timerContent = controlColor,
      chipFill = scheme.surfaceContainer.copy(alpha = 0.95f),
      chipContent = scheme.onSurface,
      chipIconContent = controlColor,
    )
  }
  return PlayerColors(
    controlFill = scheme.primaryContainer.copy(alpha = glassPlayerAlpha()),
    controlBorder = scheme.primary.copy(alpha = 0.35f),
    controlContent = scheme.onPrimaryContainer,
    onVideoContent = onVideoAccent,
    accent = scheme.primary,
    accentContent = scheme.onPrimary,
    activeFill = scheme.primary.copy(alpha = glassPlayerAlpha(default = 0.85f, glass = 0.35f)),
    activeContent = scheme.onPrimary,
    activeOnVideoContent = onVideoAccent,
    playPauseFill = scheme.primary.copy(alpha = glassPlayerAlpha(default = 0.85f, glass = 0.35f)),
    playPauseContent = scheme.onPrimary,
    timerContent = onVideoAccent,
    chipFill = scheme.primaryContainer.copy(alpha = 0.95f),
    chipContent = scheme.onPrimaryContainer,
    chipIconContent = scheme.onPrimaryContainer,
  )
}
