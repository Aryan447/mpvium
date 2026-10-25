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
  // Text inside controls. Icons get their own tint so the accent can live on
  // the glyph alone without recoloring labels.
  val controlContent: Color,
  val controlIconContent: Color,
  // Text and icon glyphs floating directly over video (backgrounds hidden).
  val onVideoContent: Color,
  val onVideoIconContent: Color,
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
    controlContent = Color.White,
    controlIconContent = Color(0xFFE8B5EF),
    onVideoContent = Color.White,
    onVideoIconContent = Color(0xFFE8B5EF),
    accent = Color(0xFFE8B5EF),
    accentContent = Color(0xFF472150),
    activeFill = Color(0xCC393338),
    activeContent = Color(0xFFE8B5EF),
    activeOnVideoContent = Color(0xFFE8B5EF),
    playPauseFill = Color(0x8C49454F),
    playPauseContent = Color(0xFFE8B5EF),
    timerContent = Color.White,
    chipFill = Color(0xF2ECE6F0),
    chipContent = Color(0xFF1D1B20),
    chipIconContent = Color(0xFF6750A4),
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
  val neutralActiveFill =
    scheme.surfaceContainerHigh.copy(alpha = glassPlayerAlpha(default = 0.92f, glass = 0.38f))
  if (!fullColors) {
    // Neutral chrome: only the seekbar, sliders and toggled-on icons wear
    // the theme accent.
    return PlayerColors(
      controlFill = neutralFill,
      controlBorder = neutralBorder,
      controlContent = scheme.onSurface,
      controlIconContent = scheme.onSurface,
      onVideoContent = controlColor,
      onVideoIconContent = controlColor,
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
  // Backgrounds stay neutral glass; the accent lives on the icon glyphs and
  // on progress indicators, so the overlay never reads as a field of color.
  return PlayerColors(
    controlFill = neutralFill,
    controlBorder = neutralBorder,
    controlContent = scheme.onSurface,
    controlIconContent = scheme.primary,
    onVideoContent = controlColor,
    onVideoIconContent = onVideoAccent,
    accent = scheme.primary,
    accentContent = scheme.onPrimary,
    activeFill = neutralActiveFill,
    activeContent = scheme.primary,
    activeOnVideoContent = onVideoAccent,
    playPauseFill = neutralFill,
    playPauseContent = scheme.primary,
    timerContent = controlColor,
    chipFill = scheme.surfaceContainer.copy(alpha = 0.95f),
    chipContent = scheme.onSurface,
    chipIconContent = scheme.primary,
  )
}
