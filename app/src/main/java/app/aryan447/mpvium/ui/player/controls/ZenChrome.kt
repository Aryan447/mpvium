package app.aryan447.mpvium.ui.player.controls

import androidx.compose.ui.graphics.Color

/**
 * Zen chrome color tokens.
 */
internal object ZenChrome {
  /** Primary glyph colour over video. */
  val glyph = Color.White

  /** Timers and secondary metadata. */
  val glyphMuted = Color.White.copy(alpha = 0.72f)

  /** Solid hero play/pause disc. */
  val heroFill = Color.White
  val heroContent = Color.Black

  /** Faint fill for toggled-on surfaces that need a container. */
  val activeFill = Color.White.copy(alpha = 0.14f)
}

internal typealias CinemaChrome = ZenChrome
