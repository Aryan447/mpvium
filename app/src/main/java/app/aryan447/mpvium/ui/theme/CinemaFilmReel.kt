package app.aryan447.mpvium.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Colors specifically tailored for the Cinema Theme & Cinema Film Reel aesthetics.
 * Marquee theater grade: glowing gold, neon marquee red, velvet near-black.
 */
val CinemaGold = Color(0xFFFFD27A)
val CinemaRed = Color(0xFFFF5147)
val CinemaDarkBackground = Color(0xFF0F0607)
val FilmStripMarginColor = Color(0xFF080304)
val SprocketHoleColor = Color(0xFF2A1214)

/**
 * Modifier that draws cinema 35mm film perforations (sprocket holes) along top & bottom edges.
 */
fun Modifier.cinemaFilmStrip(
  enabled: Boolean = true,
  borderColor: Color = CinemaGold,
  sprocketColor: Color = SprocketHoleColor,
  stripHeight: Dp = 10.dp,
): Modifier = if (!enabled) this else this.drawWithContent {
  drawContent()

  val stripHeightPx = stripHeight.toPx()
  val holeWidthPx = 6.dp.toPx()
  val holeHeightPx = 4.dp.toPx()
  val holeSpacingPx = 8.dp.toPx()

  // Top film strip background
  drawRect(
    color = FilmStripMarginColor,
    topLeft = Offset(0f, 0f),
    size = Size(size.width, stripHeightPx),
  )

  // Bottom film strip background
  drawRect(
    color = FilmStripMarginColor,
    topLeft = Offset(0f, size.height - stripHeightPx),
    size = Size(size.width, stripHeightPx),
  )

  // Golden accent dividing lines
  drawLine(
    color = borderColor.copy(alpha = 0.85f),
    start = Offset(0f, stripHeightPx),
    end = Offset(size.width, stripHeightPx),
    strokeWidth = 1.dp.toPx(),
  )
  drawLine(
    color = borderColor.copy(alpha = 0.85f),
    start = Offset(0f, size.height - stripHeightPx),
    end = Offset(size.width, size.height - stripHeightPx),
    strokeWidth = 1.dp.toPx(),
  )

  // Draw sprocket perforations along top and bottom
  var currentX = 4.dp.toPx()
  val topY = (stripHeightPx - holeHeightPx) / 2f
  val bottomY = size.height - stripHeightPx + (stripHeightPx - holeHeightPx) / 2f

  while (currentX + holeWidthPx <= size.width) {
    // Top perforation
    drawRoundRect(
      color = sprocketColor,
      topLeft = Offset(currentX, topY),
      size = Size(holeWidthPx, holeHeightPx),
      cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
    )
    // Bottom perforation
    drawRoundRect(
      color = sprocketColor,
      topLeft = Offset(currentX, bottomY),
      size = Size(holeWidthPx, holeHeightPx),
      cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
    )

    currentX += holeWidthPx + holeSpacingPx
  }
}

/**
 * Cinema Film Reel Icon composable dynamically rendered on Canvas.
 */
@Composable
fun CinemaFilmReelIcon(
  modifier: Modifier = Modifier,
  goldColor: Color = CinemaGold,
  redColor: Color = CinemaRed,
) {
  Canvas(modifier = modifier) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val outerRadius = minOf(size.width, size.height) / 2f - 2.dp.toPx()
    val innerRadius = outerRadius * 0.28f

    // Outer gold ring
    drawCircle(
      color = goldColor,
      center = center,
      radius = outerRadius,
      style = Stroke(width = 2.5.dp.toPx()),
    )

    // Inner gold ring
    drawCircle(
      color = goldColor,
      center = center,
      radius = innerRadius,
      style = Stroke(width = 1.8.dp.toPx()),
    )

    // Center hub
    drawCircle(
      color = redColor,
      center = center,
      radius = innerRadius * 0.55f,
    )

    // 4 film reel circular cutouts between inner and outer ring
    val cutoutRadius = (outerRadius - innerRadius) * 0.35f
    val cutoutDist = (outerRadius + innerRadius) / 2f

    for (i in 0 until 4) {
      val angle = (i * 90) * (Math.PI / 180.0)
      val cutoutCenter = Offset(
        (center.x + cutoutDist * Math.cos(angle)).toFloat(),
        (center.y + cutoutDist * Math.sin(angle)).toFloat(),
      )
      drawCircle(
        color = FilmStripMarginColor,
        center = cutoutCenter,
        radius = cutoutRadius,
      )
      drawCircle(
        color = goldColor,
        center = cutoutCenter,
        radius = cutoutRadius,
        style = Stroke(width = 1.dp.toPx()),
      )
    }
  }
}

/**
 * Wraps content in a card decorated with Cinema Film Reel / Film Strip styling when Cinema Theme is active.
 */
@Composable
fun CinemaReelCard(
  modifier: Modifier = Modifier,
  forceCinema: Boolean = false,
  content: @Composable () -> Unit,
) {
  val isCinema = forceCinema || LocalAppTheme.current == AppTheme.Cinema

  if (isCinema) {
    Card(
      modifier = modifier
        .border(
          width = 1.5.dp,
          color = CinemaGold.copy(alpha = 0.7f),
          shape = RoundedCornerShape(10.dp),
        )
        .cinemaFilmStrip(enabled = true, borderColor = CinemaGold),
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
    ) {
      Box(modifier = Modifier.padding(vertical = 10.dp)) {
        content()
      }
    }
  } else {
    content()
  }
}
