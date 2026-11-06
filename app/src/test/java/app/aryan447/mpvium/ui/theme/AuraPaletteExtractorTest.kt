package app.aryan447.mpvium.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuraPaletteExtractorTest {

  @Before
  fun setUp() {
    AuraThemeManager.clearCache()
  }

  @Test
  fun builtInFallbackPalette_satisfiesContrastAgainstSurfaces() {
    val lightPrimaryContrast = AuraPaletteExtractor.contrastRatio(
      AuraFallbackPalette.primaryLight,
      AuraPaletteExtractor.surfaceLight,
    )
    assertTrue("Light primary contrast must be >= 4.5:1, was $lightPrimaryContrast", lightPrimaryContrast >= 4.5f)

    val lightSecondaryContrast = AuraPaletteExtractor.contrastRatio(
      AuraFallbackPalette.secondaryLight,
      AuraPaletteExtractor.surfaceLight,
    )
    assertTrue("Light secondary contrast must be >= 3.0:1, was $lightSecondaryContrast", lightSecondaryContrast >= 3.0f)

    val lightTertiaryContrast = AuraPaletteExtractor.contrastRatio(
      AuraFallbackPalette.tertiaryLight,
      AuraPaletteExtractor.surfaceLight,
    )
    assertTrue("Light tertiary contrast must be >= 3.0:1, was $lightTertiaryContrast", lightTertiaryContrast >= 3.0f)

    val darkPrimaryContrast = AuraPaletteExtractor.contrastRatio(
      AuraFallbackPalette.primaryDark,
      AuraPaletteExtractor.surfaceDark,
    )
    assertTrue("Dark primary contrast must be >= 4.5:1, was $darkPrimaryContrast", darkPrimaryContrast >= 4.5f)

    val darkSecondaryContrast = AuraPaletteExtractor.contrastRatio(
      AuraFallbackPalette.secondaryDark,
      AuraPaletteExtractor.surfaceDark,
    )
    assertTrue("Dark secondary contrast must be >= 3.0:1, was $darkSecondaryContrast", darkSecondaryContrast >= 3.0f)

    val darkTertiaryContrast = AuraPaletteExtractor.contrastRatio(
      AuraFallbackPalette.tertiaryDark,
      AuraPaletteExtractor.surfaceDark,
    )
    assertTrue("Dark tertiary contrast must be >= 3.0:1, was $darkTertiaryContrast", darkTertiaryContrast >= 3.0f)

    // AMOLED pure black contrast check
    val amoledContrast = AuraPaletteExtractor.contrastRatio(AuraFallbackPalette.primaryDark, Color.Black)
    assertTrue("AMOLED primary contrast must be >= dark contrast", amoledContrast >= darkPrimaryContrast)
  }

  @Test
  fun darkPoster_extractsHighContrastPalette() {
    // Archetype 1: Dark poster with deep navy/indigo accent
    val darkNavy = Color(0xFF1E2D60) // H ≈ 227°, S ≈ 0.52, L ≈ 0.25
    val candidates = listOf(
      RawPaletteCandidate(rgb = darkNavy.toArgb(), population = 1500),
    )

    val palette = AuraPaletteExtractor.derivePaletteFromCandidates(candidates)
    assertNotEquals("Dark poster with saturated accent must not fall back", AuraFallbackPalette, palette)

    val lightContrast = AuraPaletteExtractor.contrastRatio(palette.primaryLight, AuraPaletteExtractor.surfaceLight)
    assertTrue("Light mode primary contrast must be >= 4.5:1, was $lightContrast", lightContrast >= 4.5f)

    val darkContrast = AuraPaletteExtractor.contrastRatio(palette.primaryDark, AuraPaletteExtractor.surfaceDark)
    assertTrue("Dark mode primary contrast must be >= 4.5:1, was $darkContrast", darkContrast >= 4.5f)
  }

  @Test
  fun brightPoster_extractsClampedAccents() {
    // Archetype 2: Bright vibrant poster with saturated coral
    val brightCoral = Color(0xFFE63946) // H ≈ 355°, S ≈ 0.78, L ≈ 0.56
    val candidates = listOf(
      RawPaletteCandidate(rgb = brightCoral.toArgb(), population = 2000),
    )

    val palette = AuraPaletteExtractor.derivePaletteFromCandidates(candidates)
    assertNotEquals("Bright vibrant poster must not fall back", AuraFallbackPalette, palette)

    val lightContrast = AuraPaletteExtractor.contrastRatio(palette.primaryLight, AuraPaletteExtractor.surfaceLight)
    assertTrue("Clamped light primary contrast must be >= 4.5:1, was $lightContrast", lightContrast >= 4.5f)

    val darkContrast = AuraPaletteExtractor.contrastRatio(palette.primaryDark, AuraPaletteExtractor.surfaceDark)
    assertTrue("Clamped dark primary contrast must be >= 4.5:1, was $darkContrast", darkContrast >= 4.5f)
  }

  @Test
  fun monochromePoster_fallsBackToAuraIdentity() {
    // Archetype 3: Monochrome / grayscale noir poster (low-chroma)
    val candidates = listOf(
      RawPaletteCandidate(rgb = Color(0xFF323232).toArgb(), population = 5000),
      RawPaletteCandidate(rgb = Color(0xFF787878).toArgb(), population = 3000),
      RawPaletteCandidate(rgb = Color(0xFFE0E0E0).toArgb(), population = 1000),
    )

    val palette = AuraPaletteExtractor.derivePaletteFromCandidates(candidates)
    assertEquals("Low-chroma monochrome poster must reject seed and return fallback", AuraFallbackPalette, palette)
  }

  @Test
  fun muddyOrMurkySeed_isRejectedAndFallsBack() {
    // Muddy/murky olive brown (H ≈ 50°, low saturation and murky lightness)
    val murkyOlive = Color(0xFF3D371E)
    assertTrue("Murky olive should be detected as muddy", AuraPaletteExtractor.isMuddyOrLowChroma(murkyOlive))

    val candidates = listOf(
      RawPaletteCandidate(rgb = murkyOlive.toArgb(), population = 4000),
    )
    val palette = AuraPaletteExtractor.derivePaletteFromCandidates(candidates)
    assertEquals("Muddy seed must fall back to Aura built-in palette", AuraFallbackPalette, palette)
  }

  @Test
  fun extractionCaching_returnsCachedInstanceOnRepeatedCall() = runBlocking {
    val fakeKey = "/fake/artwork/poster.jpg"
    // Pre-populate cache directly
    val customPalette = AuraPalette(
      primaryLight = Color(0xFF204080),
      primaryDark = Color(0xFF80A0E0),
      secondaryLight = Color(0xFF305090),
      secondaryDark = Color(0xFF90B0F0),
      tertiaryLight = Color(0xFF4060A0),
      tertiaryDark = Color(0xFFA0C0FF),
    )

    // Verify cache is initially empty
    assertEquals(null, AuraThemeManager.getCachedPalette(fakeKey))

    // Call extractPalette on a non-existent file -> returns AuraFallbackPalette and caches it
    val extracted = AuraThemeManager.extractPalette(fakeKey)
    assertEquals(AuraFallbackPalette, extracted)
    assertEquals(AuraFallbackPalette, AuraThemeManager.getCachedPalette(fakeKey))

    // Clear cache
    AuraThemeManager.clearCache()
    assertEquals(null, AuraThemeManager.getCachedPalette(fakeKey))
  }

  @Test
  fun schemeBuilder_amoledHasPureBlackSurfacesAndPreservesAccents() {
    val customPalette = AuraPalette(
      primaryLight = Color(0xFF1E3A8A),
      primaryDark = Color(0xFF93C5FD),
      secondaryLight = Color(0xFF1E293B),
      secondaryDark = Color(0xFFCBD5E1),
      tertiaryLight = Color(0xFF831843),
      tertiaryDark = Color(0xFFF472B6),
    )

    val darkScheme = buildDarkColorScheme(
      primaryLight = customPalette.primaryLight,
      primaryDark = customPalette.primaryDark,
      secondaryLight = customPalette.secondaryLight,
      secondaryDark = customPalette.secondaryDark,
      tertiaryLight = customPalette.tertiaryLight,
      tertiaryDark = customPalette.tertiaryDark,
      backgroundLight = AppTheme.Aura.backgroundLight,
      backgroundDark = AppTheme.Aura.backgroundDark,
    )

    val amoledScheme = buildAmoledColorScheme(darkScheme, customPalette.primaryDark)

    // AMOLED background and surface must be pure black
    assertEquals(Color.Black, amoledScheme.background)
    assertEquals(Color.Black, amoledScheme.surface)
    assertEquals(Color.Black, amoledScheme.surfaceContainerLowest)

    // Accents are preserved
    assertEquals(customPalette.primaryDark, amoledScheme.primary)
    assertEquals(customPalette.secondaryDark, amoledScheme.secondary)
    assertEquals(customPalette.tertiaryDark, amoledScheme.tertiary)
  }

  @Test
  fun appThemeEnumOrder_auraIsThird() {
    val themes = AppTheme.entries
    assertEquals("First theme must be Default", AppTheme.Default, themes[0])
    assertEquals("Second theme must be Dynamic", AppTheme.Dynamic, themes[1])
    assertEquals("Third theme must be Aura", AppTheme.Aura, themes[2])
    assertEquals("Fourth theme must be Cinema", AppTheme.Cinema, themes[3])
  }
}
