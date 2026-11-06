package app.aryan447.mpvium.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.pow

/**
 * Extracted or fallback accent triple for the Aura theme.
 * Aura uses poster-extracted accents while keeping surfaces, text, and
 * backgrounds stable, accessible, and contrasted.
 */
data class AuraPalette(
  val primaryLight: Color,
  val primaryDark: Color,
  val secondaryLight: Color,
  val secondaryDark: Color,
  val tertiaryLight: Color,
  val tertiaryDark: Color,
)

/**
 * Built-in fallback palette used before artwork is extracted or whenever
 * extraction is rejected (e.g. monochrome, muddy, low-chroma, or poor contrast).
 */
val AuraFallbackPalette = AuraPalette(
  primaryLight = Color(0xFF595E6B),
  primaryDark = Color(0xFFC2C7D6),
  secondaryLight = Color(0xFF5B6065),
  secondaryDark = Color(0xFFC3C7CC),
  tertiaryLight = Color(0xFF6E5967),
  tertiaryDark = Color(0xFFD7C1CF),
)

data class RawPaletteCandidate(
  val rgb: Int,
  val population: Int,
)

object AuraPaletteExtractor {
  val surfaceLight = Color(0xFFF8F9FA)
  val surfaceDark = Color(0xFF121316)

  const val MIN_TEXT_CONTRAST = 4.5f
  const val MIN_ICON_CONTRAST = 3.0f
  const val MIN_CHROMA_SATURATION = 0.20f

  fun derivePaletteFromCandidates(candidates: List<RawPaletteCandidate>): AuraPalette {
    val validCandidates = candidates
      .sortedByDescending { it.population }
      .map { Color(it.rgb) }
      .filterNot { isMuddyOrLowChroma(it) }

    if (validCandidates.isEmpty()) {
      return AuraFallbackPalette
    }

    val primarySeed = validCandidates.first()

    val secondarySeed = validCandidates.drop(1).firstOrNull { candidate ->
      val hslP = FloatArray(3).also { rgbToHsl(primarySeed.red, primarySeed.green, primarySeed.blue, it) }
      val hslC = FloatArray(3).also { rgbToHsl(candidate.red, candidate.green, candidate.blue, it) }
      abs(hslP[0] - hslC[0]) > 20f
    } ?: run {
      val hsl = FloatArray(3).also { rgbToHsl(primarySeed.red, primarySeed.green, primarySeed.blue, it) }
      hslToRgb((hsl[0] + 30f) % 360f, (hsl[1] * 0.8f).coerceIn(0.25f, 1f), hsl[2])
    }

    val tertiarySeed = validCandidates.drop(1).firstOrNull { candidate ->
      val hslP = FloatArray(3).also { rgbToHsl(primarySeed.red, primarySeed.green, primarySeed.blue, it) }
      val hslS = FloatArray(3).also { rgbToHsl(secondarySeed.red, secondarySeed.green, secondarySeed.blue, it) }
      val hslC = FloatArray(3).also { rgbToHsl(candidate.red, candidate.green, candidate.blue, it) }
      abs(hslP[0] - hslC[0]) > 30f && abs(hslS[0] - hslC[0]) > 20f
    } ?: run {
      val hsl = FloatArray(3).also { rgbToHsl(primarySeed.red, primarySeed.green, primarySeed.blue, it) }
      hslToRgb((hsl[0] + 60f) % 360f, (hsl[1] * 0.85f).coerceIn(0.25f, 1f), hsl[2])
    }

    val primaryLight = clampLuminance(primarySeed, surfaceLight, MIN_TEXT_CONTRAST, preferDarker = true)
    val primaryDark = clampLuminance(primarySeed, surfaceDark, MIN_TEXT_CONTRAST, preferDarker = false)
    val secondaryLight = clampLuminance(secondarySeed, surfaceLight, MIN_ICON_CONTRAST, preferDarker = true)
    val secondaryDark = clampLuminance(secondarySeed, surfaceDark, MIN_ICON_CONTRAST, preferDarker = false)
    val tertiaryLight = clampLuminance(tertiarySeed, surfaceLight, MIN_ICON_CONTRAST, preferDarker = true)
    val tertiaryDark = clampLuminance(tertiarySeed, surfaceDark, MIN_ICON_CONTRAST, preferDarker = false)

    if (primaryLight == null || primaryDark == null ||
        secondaryLight == null || secondaryDark == null ||
        tertiaryLight == null || tertiaryDark == null) {
      return AuraFallbackPalette
    }

    return AuraPalette(
      primaryLight = primaryLight,
      primaryDark = primaryDark,
      secondaryLight = secondaryLight,
      secondaryDark = secondaryDark,
      tertiaryLight = tertiaryLight,
      tertiaryDark = tertiaryDark,
    )
  }

  fun isMuddyOrLowChroma(seed: Color): Boolean {
    val hsl = FloatArray(3)
    rgbToHsl(seed.red, seed.green, seed.blue, hsl)
    val hue = hsl[0]
    val saturation = hsl[1]
    val lightness = hsl[2]

    // Reject low chroma / grayscale / washed out slates
    if (saturation < MIN_CHROMA_SATURATION) return true

    // Extreme lightness
    if (lightness < 0.08f || lightness > 0.92f) return true

    // Muddy brown/olive tones: hue in 30..85 with low saturation or dark murky tone
    if (hue in 30f..85f && (saturation < 0.35f || lightness < 0.30f)) return true

    return false
  }

  fun clampLuminance(
    seed: Color,
    surface: Color,
    minContrast: Float,
    preferDarker: Boolean,
  ): Color? {
    val hsl = FloatArray(3)
    rgbToHsl(seed.red, seed.green, seed.blue, hsl)
    val hue = hsl[0]
    val saturation = hsl[1]
    var lightness = hsl[2]

    var currentColor = hslToRgb(hue, saturation, lightness)
    if (contrastRatio(currentColor, surface) >= minContrast) {
      if (preferDarker && lightness > 0.45f) {
        lightness = 0.40f
        currentColor = hslToRgb(hue, saturation, lightness)
      } else if (!preferDarker && lightness < 0.65f) {
        lightness = 0.70f
        currentColor = hslToRgb(hue, saturation, lightness)
      }
      if (contrastRatio(currentColor, surface) >= minContrast && !isMuddyOrLowChroma(currentColor)) {
        return currentColor
      }
    }

    val step = 0.01f
    if (preferDarker) {
      while (lightness >= 0.15f) {
        lightness -= step
        currentColor = hslToRgb(hue, saturation, lightness)
        if (contrastRatio(currentColor, surface) >= minContrast) {
          if (!isMuddyOrLowChroma(currentColor)) {
            return currentColor
          }
        }
      }
    } else {
      while (lightness <= 0.85f) {
        lightness += step
        currentColor = hslToRgb(hue, saturation, lightness)
        if (contrastRatio(currentColor, surface) >= minContrast) {
          if (!isMuddyOrLowChroma(currentColor)) {
            return currentColor
          }
        }
      }
    }

    return if (contrastRatio(currentColor, surface) >= minContrast && !isMuddyOrLowChroma(currentColor)) {
      currentColor
    } else {
      null
    }
  }

  fun contrastRatio(c1: Color, c2: Color): Float {
    val l1 = calculateLuminance(c1)
    val l2 = calculateLuminance(c2)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
  }

  fun calculateLuminance(color: Color): Float {
    fun channel(c: Float): Float {
      return if (c <= 0.04045f) {
        c / 12.92f
      } else {
        ((c + 0.055) / 1.055).pow(2.4).toFloat()
      }
    }
    return (0.2126f * channel(color.red) +
            0.7152f * channel(color.green) +
            0.0722f * channel(color.blue)).coerceIn(0f, 1f)
  }

  fun rgbToHsl(red: Float, green: Float, blue: Float, outHsl: FloatArray) {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val l = (max + min) / 2f
    var h: Float
    var s: Float

    if (delta == 0f) {
      h = 0f
      s = 0f
    } else {
      s = if (l <= 0.5f) delta / (max + min) else delta / (2f - max - min)
      h = when (max) {
        red -> ((green - blue) / delta) + (if (green < blue) 6f else 0f)
        green -> ((blue - red) / delta) + 2f
        else -> ((red - green) / delta) + 4f
      }
      h *= 60f
    }
    outHsl[0] = (h % 360f + 360f) % 360f
    outHsl[1] = s.coerceIn(0f, 1f)
    outHsl[2] = l.coerceIn(0f, 1f)
  }

  fun hslToRgb(hue: Float, saturation: Float, lightness: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f / 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)

    if (s == 0f) {
      return Color(l, l, l, 1f)
    }

    val q = if (l < 0.5f) l * (1f + s) else l + s - (l * s)
    val p = 2f * l - q

    fun hueToRgb(t: Float): Float {
      var tc = t
      if (tc < 0f) tc += 1f
      if (tc > 1f) tc -= 1f
      return when {
        tc < 1f / 6f -> p + (q - p) * 6f * tc
        tc < 1f / 2f -> q
        tc < 2f / 3f -> p + (q - p) * (2f / 3f - tc) * 6f
        else -> p
      }
    }

    return Color(
      red = hueToRgb(h + 1f / 3f).coerceIn(0f, 1f),
      green = hueToRgb(h).coerceIn(0f, 1f),
      blue = hueToRgb(h - 1f / 3f).coerceIn(0f, 1f),
      alpha = 1f,
    )
  }
}
