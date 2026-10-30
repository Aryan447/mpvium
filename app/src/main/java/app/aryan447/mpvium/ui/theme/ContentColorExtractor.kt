package app.aryan447.mpvium.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

@Immutable
data class ContentPalette(
  val primary: Color,
  val secondary: Color,
  val tertiary: Color,
  val primaryContainer: Color,
  val secondaryContainer: Color,
  val tertiaryContainer: Color,
)

/**
 * Extracts a harmonized accent and ambient palette from a poster or artwork file.
 *
 * Requirements:
 * - Downscaled bitmap decode only (never full-size decode).
 * - Keyed and cached by artwork file path.
 * - Accents & tonal containers only: never derives background, body text, or on-colors.
 * - Enforces minimum contrast ratio against the surface (aims for >= 4.5:1 text, >= 3.0:1 icons).
 * - Muddy or low-contrast colors return null to fall back to the app theme.
 */
object ContentColorExtractor {
  const val MAX_EXTRACT_DIMENSION = 128
  const val MIN_SATURATION = 0.15f
  const val MIN_CONTRAST_RATIO_TEXT = 4.5
  const val MIN_CONTRAST_RATIO_ICON = 3.0

  private val cache = ConcurrentHashMap<String, ContentPalette?>()

  fun extractFromArtworkPath(
    filePath: String,
    isDark: Boolean,
    isAmoled: Boolean,
    surfaceColor: Color,
  ): ContentPalette? {
    if (filePath.isBlank()) return null
    val file = File(filePath)
    if (!file.isFile || file.length() == 0L) return null

    val cacheKey = "$filePath|dark=$isDark|amoled=$isAmoled|surface=${surfaceColor.toArgb()}"
    if (cache.containsKey(cacheKey)) {
      return cache[cacheKey]
    }

    val bitmap = decodeDownscaledBitmap(filePath, MAX_EXTRACT_DIMENSION) ?: run {
      cache[cacheKey] = null
      return null
    }

    return try {
      val palette = extractFromBitmap(
        bitmap = bitmap,
        isDark = isDark,
        isAmoled = isAmoled,
        surfaceColor = surfaceColor,
      )
      cache[cacheKey] = palette
      palette
    } finally {
      bitmap.recycle()
    }
  }

  fun extractFromBitmap(
    bitmap: Bitmap,
    isDark: Boolean,
    isAmoled: Boolean,
    surfaceColor: Color,
  ): ContentPalette? {
    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
    return derivePalette(palette, isDark, isAmoled, surfaceColor)
  }

  fun clearCache() {
    cache.clear()
  }

  @VisibleForTesting
  internal fun derivePalette(
    palette: Palette,
    isDark: Boolean,
    isAmoled: Boolean,
    surfaceColor: Color,
  ): ContentPalette? {
    val surfaceArgb = surfaceColor.toArgb()

    val candidateSwatches = if (isDark) {
      listOfNotNull(
        palette.vibrantSwatch,
        palette.lightVibrantSwatch,
        palette.dominantSwatch,
        palette.mutedSwatch,
        palette.lightMutedSwatch,
        palette.darkVibrantSwatch,
      )
    } else {
      listOfNotNull(
        palette.darkVibrantSwatch,
        palette.vibrantSwatch,
        palette.dominantSwatch,
        palette.darkMutedSwatch,
        palette.mutedSwatch,
        palette.lightVibrantSwatch,
      )
    }

    var primaryArgb: Int? = null
    var primarySwatch: Palette.Swatch? = null

    for (swatch in candidateSwatches) {
      val clamped = clampLuminanceAndEnforceContrast(
        candidateColor = swatch.rgb,
        surfaceColor = surfaceArgb,
        isDark = isDark,
      )
      if (clamped != null) {
        primaryArgb = clamped
        primarySwatch = swatch
        break
      }
    }

    if (primaryArgb == null) {
      // Extraction yielded muddy or low-contrast color; fall back to app theme
      return null
    }

    val primaryHsl = FloatArray(3)
    ColorUtils.colorToHSL(primaryArgb, primaryHsl)

    // Tertiary: find another distinct swatch from palette, or rotate hue by +35°
    val tertiaryArgb = run {
      val secondaryCandidate = candidateSwatches.firstOrNull { swatch ->
        if (swatch == primarySwatch) return@firstOrNull false
        val swatchHsl = FloatArray(3)
        ColorUtils.colorToHSL(swatch.rgb, swatchHsl)
        abs(swatchHsl[0] - primaryHsl[0]) > 25f && swatchHsl[1] >= MIN_SATURATION
      }

      val candidateRgb = secondaryCandidate?.rgb ?: run {
        val shiftedHsl = primaryHsl.clone().apply {
          this[0] = (this[0] + 35f) % 360f
        }
        ColorUtils.HSLToColor(shiftedHsl)
      }

      clampLuminanceAndEnforceContrast(
        candidateColor = candidateRgb,
        surfaceColor = surfaceArgb,
        isDark = isDark,
      ) ?: primaryArgb
    }

    // Secondary: slightly lower saturation of primary with preserved contrast
    val secondaryArgb = run {
      val secondaryHsl = primaryHsl.clone().apply {
        this[1] = (this[1] * 0.75f).coerceAtLeast(0.20f)
      }
      val rgb = ColorUtils.HSLToColor(secondaryHsl)
      clampLuminanceAndEnforceContrast(
        candidateColor = rgb,
        surfaceColor = surfaceArgb,
        isDark = isDark,
      ) ?: primaryArgb
    }

    // Tonal containers
    val primaryContainerArgb = deriveContainerColor(primaryHsl, isDark, isAmoled)
    val tertiaryHsl = FloatArray(3).apply { ColorUtils.colorToHSL(tertiaryArgb, this) }
    val secondaryHsl = FloatArray(3).apply { ColorUtils.colorToHSL(secondaryArgb, this) }

    val secondaryContainerArgb = deriveContainerColor(secondaryHsl, isDark, isAmoled)
    val tertiaryContainerArgb = deriveContainerColor(tertiaryHsl, isDark, isAmoled)

    return ContentPalette(
      primary = Color(primaryArgb),
      secondary = Color(secondaryArgb),
      tertiary = Color(tertiaryArgb),
      primaryContainer = Color(primaryContainerArgb),
      secondaryContainer = Color(secondaryContainerArgb),
      tertiaryContainer = Color(tertiaryContainerArgb),
    )
  }

  private fun deriveContainerColor(hsl: FloatArray, isDark: Boolean, isAmoled: Boolean): Int {
    val containerHsl = hsl.clone()
    if (isDark) {
      containerHsl[1] = (containerHsl[1] * 0.50f).coerceIn(0.15f, 0.45f)
      containerHsl[2] = if (isAmoled) 0.16f else 0.22f
    } else {
      containerHsl[1] = (containerHsl[1] * 0.35f).coerceIn(0.10f, 0.35f)
      containerHsl[2] = 0.90f
    }
    return ColorUtils.HSLToColor(containerHsl)
  }

  /**
   * Clamps luminance and enforces minimum contrast against the given surface color.
   * Returns null if the color is muddy (saturation < [MIN_SATURATION]) or cannot achieve
   * at least [minAcceptableContrast] ratio against the surface.
   */
  @VisibleForTesting
  internal fun clampLuminanceAndEnforceContrast(
    candidateColor: Int,
    surfaceColor: Int,
    isDark: Boolean,
    targetContrast: Double = MIN_CONTRAST_RATIO_TEXT,
    minAcceptableContrast: Double = MIN_CONTRAST_RATIO_ICON,
  ): Int? {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(candidateColor, hsl)

    if (hsl[1] < MIN_SATURATION) {
      return null
    }

    if (isDark) {
      hsl[2] = hsl[2].coerceIn(0.55f, 0.90f)
      var current = ColorUtils.HSLToColor(hsl)
      var contrast = ColorUtils.calculateContrast(current, surfaceColor)
      while (contrast < targetContrast && hsl[2] < 0.88f) {
        hsl[2] = (hsl[2] + 0.05f).coerceAtMost(0.88f)
        current = ColorUtils.HSLToColor(hsl)
        contrast = ColorUtils.calculateContrast(current, surfaceColor)
      }
      return if (contrast >= minAcceptableContrast) current else null
    } else {
      hsl[2] = hsl[2].coerceIn(0.15f, 0.42f)
      var current = ColorUtils.HSLToColor(hsl)
      var contrast = ColorUtils.calculateContrast(current, surfaceColor)
      while (contrast < targetContrast && hsl[2] > 0.18f) {
        hsl[2] = (hsl[2] - 0.05f).coerceAtLeast(0.18f)
        current = ColorUtils.HSLToColor(hsl)
        contrast = ColorUtils.calculateContrast(current, surfaceColor)
      }
      return if (contrast >= minAcceptableContrast) current else null
    }
  }

  private fun decodeDownscaledBitmap(filePath: String, maxDimension: Int): Bitmap? =
    runCatching {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(filePath, bounds)
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

      val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
      var sampleSize = 1
      while (longEdge / sampleSize > maxDimension) sampleSize *= 2

      val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }
      BitmapFactory.decodeFile(filePath, opts)
    }.getOrNull()
}
