package app.aryan447.mpvium.ui.audio.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs

object AlbumPlaceholderUtils {

  /**
   * Generates 1 or 2 uppercase initials from album title or artist.
   * If both are unknown or blank, returns a musical note "♪".
   */
  fun extractInitials(title: String?, artist: String? = null): String {
    val cleanTitle = title?.trim().orEmpty()
    if (cleanTitle.isNotBlank() && !cleanTitle.equals("Unknown Album", ignoreCase = true)) {
      val words = cleanTitle
        .split(Regex("[\\s_\\-–—·/\\\\]+"))
        .filter { it.isNotBlank() && it.any { char -> char.isLetterOrDigit() } }
      when {
        words.size >= 2 -> {
          val c1 = words[0].first { it.isLetterOrDigit() }.uppercaseChar()
          val c2 = words[1].first { it.isLetterOrDigit() }.uppercaseChar()
          return "$c1$c2"
        }
        words.isNotEmpty() -> {
          val word = words[0].filter { it.isLetterOrDigit() }
          return if (word.length >= 2) {
            word.take(2).uppercase(Locale.ROOT)
          } else if (word.isNotEmpty()) {
            word.take(1).uppercase(Locale.ROOT)
          } else {
            "♪"
          }
        }
      }
    }

    val cleanArtist = artist?.trim().orEmpty()
    if (cleanArtist.isNotBlank() && !cleanArtist.equals("Unknown Artist", ignoreCase = true)) {
      val words = cleanArtist
        .split(Regex("[\\s_\\-–—·/\\\\]+"))
        .filter { it.isNotBlank() && it.any { char -> char.isLetterOrDigit() } }
      when {
        words.size >= 2 -> {
          val c1 = words[0].first { it.isLetterOrDigit() }.uppercaseChar()
          val c2 = words[1].first { it.isLetterOrDigit() }.uppercaseChar()
          return "$c1$c2"
        }
        words.isNotEmpty() -> {
          val word = words[0].filter { it.isLetterOrDigit() }
          return if (word.length >= 2) {
            word.take(2).uppercase(Locale.ROOT)
          } else if (word.isNotEmpty()) {
            word.take(1).uppercase(Locale.ROOT)
          } else {
            "♪"
          }
        }
      }
    }

    return "♪"
  }

  /**
   * Generates a square [Bitmap] tile with initials on a tonal surface.
   * Useful for system notifications and MediaSession lock screen art.
   */
  fun generatePlaceholderBitmap(
    title: String?,
    artist: String? = null,
    sizePx: Int = 512,
    backgroundTone: Int = 0xFF2A2D34.toInt(),
    textTone: Int = 0xFFECEFF4.toInt(),
  ): Bitmap {
    val size = sizePx.coerceAtLeast(64)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = backgroundTone
      style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    // Faint vinyl-style ring
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = textTone
      alpha = 24
      style = Paint.Style.STROKE
      strokeWidth = (size / 80f).coerceAtLeast(2f)
    }
    canvas.drawCircle(size / 2f, size / 2f, size * 0.38f, ringPaint)

    val initials = extractInitials(title, artist)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = textTone
      textSize = size * 0.34f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }

    val bounds = Rect()
    textPaint.getTextBounds(initials, 0, initials.length, bounds)
    val yPos = (size / 2f) - bounds.exactCenterY()
    canvas.drawText(initials, size / 2f, yPos, textPaint)

    return bitmap
  }
}

/**
 * Beautiful generated placeholder tile displaying album initials on a Material 3 tonal surface.
 * Shown when both embedded and network artwork are missing, preventing broken-image icons.
 */
@Composable
fun AlbumPlaceholderTile(
  title: String?,
  artist: String?,
  key: String,
  modifier: Modifier = Modifier,
) {
  val initials = AlbumPlaceholderUtils.extractInitials(title, artist)
  val colorScheme = MaterialTheme.colorScheme

  // Pick deterministic tonal palette variant based on stable key
  val hash = abs(key.hashCode())
  val (baseContainer, onContainer) = when (hash % 4) {
    0 -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
    1 -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
    2 -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
    else -> colorScheme.surfaceContainerHighest to colorScheme.onSurfaceVariant
  }

  val gradientBrush = Brush.radialGradient(
    colors = listOf(
      baseContainer.copy(alpha = 0.95f),
      baseContainer.copy(alpha = 0.70f),
    )
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(gradientBrush),
    contentAlignment = Alignment.Center,
  ) {
    // Faint decorative vinyl rings
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val maxRadius = minOf(size.width, size.height) / 2f

      drawCircle(
        color = onContainer.copy(alpha = 0.08f),
        radius = maxRadius * 0.78f,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
      )
      drawCircle(
        color = onContainer.copy(alpha = 0.05f),
        radius = maxRadius * 0.52f,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f),
      )
    }

    Text(
      text = initials,
      style = MaterialTheme.typography.titleLarge.copy(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 1.sp,
      ),
      color = onContainer,
    )
  }
}
