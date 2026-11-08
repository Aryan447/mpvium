package app.aryan447.mpvium.ui.player.controls

import androidx.compose.ui.graphics.Color

/**
 * Cinema drops the Classic glass entirely.
 *
 * Instead of another row of translucent mpv pills, controls are bare white
 * glyphs floating over the scrim, the hero play/pause is a single solid disc,
 * and toggled controls are marked with an accent dot. Where a container is
 * functionally required (buffering, active pills) it is a faint highlight, not
 * a bordered surface.
 */
internal object CinemaChrome {
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
