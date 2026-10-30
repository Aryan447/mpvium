package app.aryan447.mpvium.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentColorExtractorTest {

  private val darkSurfaceArgb = 0xFF161217.toInt()
  private val amoledSurfaceArgb = 0xFF000000.toInt()
  private val lightSurfaceArgb = 0xFFFFF7FB.toInt()

  @Test
  fun darkPoster_vibrantDarkColor_isClampedToSatisfyDarkSurfaceContrast() {
    // A dark, saturated crimson poster accent (e.g. The Batman poster, RGB: #730B14)
    val darkCrimson = 0xFF730B14.toInt()

    val hslBefore = FloatArray(3)
    ColorUtils.colorToHSL(darkCrimson, hslBefore)
    assertTrue("Poster should be saturated", hslBefore[1] >= ContentColorExtractor.MIN_SATURATION)

    val clamped = ContentColorExtractor.clampLuminanceAndEnforceContrast(
      candidateColor = darkCrimson,
      surfaceColor = darkSurfaceArgb,
      isDark = true,
      targetContrast = 4.5,
      minAcceptableContrast = 3.0,
    )

    assertNotNull("Dark poster accent must not be rejected", clamped)
    val contrastAgainstDark = ColorUtils.calculateContrast(clamped!!, darkSurfaceArgb)
    assertTrue(
      "Dark poster accent must have at least 4.5:1 contrast against dark surface, was: $contrastAgainstDark",
      contrastAgainstDark >= 4.5,
    )

    val contrastAgainstAmoled = ColorUtils.calculateContrast(clamped, amoledSurfaceArgb)
    assertTrue(
      "Dark poster accent must have >= 4.5:1 contrast against pure black AMOLED surface, was: $contrastAgainstAmoled",
      contrastAgainstAmoled >= 4.5,
    )
  }

  @Test
  fun brightPoster_vibrantLightColor_isClampedToSatisfyLightSurfaceContrast() {
    // A high-key, luminous pink/yellow poster accent (e.g. Barbie / animated poster, RGB: #FF85A2)
    val brightPink = 0xFFFF85A2.toInt()

    val clamped = ContentColorExtractor.clampLuminanceAndEnforceContrast(
      candidateColor = brightPink,
      surfaceColor = lightSurfaceArgb,
      isDark = false,
      targetContrast = 4.5,
      minAcceptableContrast = 3.0,
    )

    assertNotNull("Bright poster accent must not be rejected in light theme", clamped)
    val contrastAgainstLight = ColorUtils.calculateContrast(clamped!!, lightSurfaceArgb)
    assertTrue(
      "Bright poster accent must have at least 4.5:1 contrast against light surface, was: $contrastAgainstLight",
      contrastAgainstLight >= 4.5,
    )
  }

  @Test
  fun monochromePoster_lowSaturation_isRejectedToFallbackToAppTheme() {
    // Pure monochrome / sepia / grey poster (e.g. Schindler's List / Roma, RGB: #505050 or #808080)
    val neutralGrey = 0xFF808080.toInt()
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(neutralGrey, hsl)
    assertTrue("Grey poster saturation must be below threshold", hsl[1] < ContentColorExtractor.MIN_SATURATION)

    val resultDark = ContentColorExtractor.clampLuminanceAndEnforceContrast(
      candidateColor = neutralGrey,
      surfaceColor = darkSurfaceArgb,
      isDark = true,
    )
    assertNull("Monochrome muddy color must return null in dark mode to fall back to app theme", resultDark)

    val resultLight = ContentColorExtractor.clampLuminanceAndEnforceContrast(
      candidateColor = neutralGrey,
      surfaceColor = lightSurfaceArgb,
      isDark = false,
    )
    assertNull("Monochrome muddy color must return null in light mode to fall back to app theme", resultLight)
  }

  @Test
  fun realPostersComparison_contrastVerification() {
    // 1. Dark poster (The Batman / Noir: deep navy/slate blue #142850)
    val darkNavy = 0xFF142850.toInt()
    val clampedDarkNavy = ContentColorExtractor.clampLuminanceAndEnforceContrast(
      candidateColor = darkNavy,
      surfaceColor = darkSurfaceArgb,
      isDark = true,
    )
    assertNotNull(clampedDarkNavy)
    val darkRatio = ColorUtils.calculateContrast(clampedDarkNavy!!, darkSurfaceArgb)
    assertTrue("Dark poster contrast must be >= 4.5:1", darkRatio >= 4.5)

    // 2. Bright poster (La La Land / Sunshine: vibrant sunny yellow #FFE135)
    val sunnyYellow = 0xFFFFE135.toInt()
    val clampedYellowLight = ContentColorExtractor.clampLuminanceAndEnforceContrast(
      candidateColor = sunnyYellow,
      surfaceColor = lightSurfaceArgb,
      isDark = false,
    )
    assertNotNull(clampedYellowLight)
    val brightRatio = ColorUtils.calculateContrast(clampedYellowLight!!, lightSurfaceArgb)
    assertTrue("Bright poster contrast in light theme must be >= 4.5:1", brightRatio >= 4.5)

    // 3. Monochrome poster (Raging Bull / Black-and-white: muted washed tint #5A5E60)
    val washedMonochrome = 0xFF5A5E60.toInt()
    val clampedMonochrome = ContentColorExtractor.clampLuminanceAndEnforceContrast(
      candidateColor = washedMonochrome,
      surfaceColor = darkSurfaceArgb,
      isDark = true,
    )
    assertNull("Monochrome poster must be rejected to prevent muddy/ugly screens", clampedMonochrome)
  }

  @Test
  fun cacheKeying_returnsIdenticalPaletteForSameArtworkPath() {
    ContentColorExtractor.clearCache()
    // Test cache key format and behavior
    val path = "/data/user/0/app.aryan447.mpvium/files/streaming_art/test_poster.img"
    val surfaceColor = Color(darkSurfaceArgb)

    // Non-existent file returns null
    val result1 = ContentColorExtractor.extractFromArtworkPath(
      filePath = path,
      isDark = true,
      isAmoled = false,
      surfaceColor = surfaceColor,
    )
    assertNull(result1)

    // Second call hits cache
    val result2 = ContentColorExtractor.extractFromArtworkPath(
      filePath = path,
      isDark = true,
      isAmoled = false,
      surfaceColor = surfaceColor,
    )
    assertEquals(result1, result2)
  }
}
