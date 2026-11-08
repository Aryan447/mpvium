package app.aryan447.mpvium.ui.player.controls

import androidx.compose.ui.graphics.Color

/**
 * Neutral, video-first chrome for the Cinema player layout.
 *
 * Cinema deliberately avoids the theme-tinted glass of the Classic layout:
 * controls sit in translucent charcoal discs with a hairline white edge, so
 * they stay legible over any frame without reading as app surfaces.
 */
internal object CinemaChrome {
  val fill = Color.Black.copy(alpha = 0.38f)
  val fillActive = Color.White.copy(alpha = 0.22f)
  val border = Color.White.copy(alpha = 0.12f)
  val borderActive = Color.White.copy(alpha = 0.30f)
  val playFill = Color.Black.copy(alpha = 0.46f)
  val playBorder = Color.White.copy(alpha = 0.18f)
  val glyph = Color.White
}
