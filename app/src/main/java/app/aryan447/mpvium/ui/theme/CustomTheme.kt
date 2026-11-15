package app.aryan447.mpvium.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A user-created theme from Theme Studio.
 *
 * Colors are stored as ARGB ints so the whole palette can live in one
 * preference string and survive process death without a database migration.
 */
@Serializable
data class CustomTheme(
  val id: String,
  val name: String,
  val primaryLight: Int,
  val primaryDark: Int,
  val secondaryLight: Int,
  val secondaryDark: Int,
  val tertiaryLight: Int,
  val tertiaryDark: Int,
  val backgroundLight: Int,
  val backgroundDark: Int,
) {
  fun getLightColorScheme(): ColorScheme =
    buildLightColorScheme(
      primaryLight = Color(primaryLight),
      primaryDark = Color(primaryDark),
      secondaryLight = Color(secondaryLight),
      tertiaryLight = Color(tertiaryLight),
      backgroundLight = Color(backgroundLight),
      backgroundDark = Color(backgroundDark),
    )

  fun getDarkColorScheme(): ColorScheme =
    buildDarkColorScheme(
      primaryLight = Color(primaryLight),
      primaryDark = Color(primaryDark),
      secondaryLight = Color(secondaryLight),
      secondaryDark = Color(secondaryDark),
      tertiaryLight = Color(tertiaryLight),
      tertiaryDark = Color(tertiaryDark),
      backgroundLight = Color(backgroundLight),
      backgroundDark = Color(backgroundDark),
    )

  fun getAmoledColorScheme(): ColorScheme =
    buildAmoledColorScheme(getDarkColorScheme(), Color(primaryDark))

  /** The color this theme was painted from, used to reopen it in My Canvas. */
  val seedColor: Color
    get() = Color(primaryLight)

  companion object {
    /**
     * Builds a complete light/dark palette from one accent color. Keeping the
     * seed as the only input means a canvas cannot produce an unreadable
     * combination of on-colors; the shared scheme builders own that.
     */
    fun fromSeed(
      id: String,
      name: String,
      seed: Color,
    ): CustomTheme {
      val palette = paletteFromSeed(seed)
      return CustomTheme(
        id = id,
        name = name,
        primaryLight = palette[0],
        primaryDark = palette[1],
        secondaryLight = palette[2],
        secondaryDark = palette[3],
        tertiaryLight = palette[4],
        tertiaryDark = palette[5],
        backgroundLight = palette[6],
        backgroundDark = palette[7],
      )
    }
  }
}

private fun paletteFromSeed(seed: Color): IntArray {
  val hsv = FloatArray(3)
  android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
  val saturation = hsv[1].coerceAtLeast(0.22f)

  fun tone(
    hueOffset: Float,
    toneSaturation: Float,
    value: Float,
  ): Int {
    val hue = ((hsv[0] + hueOffset) % 360f + 360f) % 360f
    return android.graphics.Color.HSVToColor(
      floatArrayOf(
        hue,
        toneSaturation.coerceIn(0f, 1f),
        value.coerceIn(0f, 1f),
      ),
    )
  }

  return intArrayOf(
    // Primary stays deep enough for white on-colors in light mode.
    tone(hueOffset = 0f, toneSaturation = saturation * 0.9f, value = 0.42f),
    tone(hueOffset = 0f, toneSaturation = saturation * 0.55f, value = 0.88f),
    tone(hueOffset = 32f, toneSaturation = saturation * 0.45f, value = 0.40f),
    tone(hueOffset = 32f, toneSaturation = saturation * 0.35f, value = 0.84f),
    tone(hueOffset = -38f, toneSaturation = saturation * 0.55f, value = 0.44f),
    tone(hueOffset = -38f, toneSaturation = saturation * 0.40f, value = 0.86f),
    // Backgrounds keep a hint of the accent without staining content.
    tone(hueOffset = 0f, toneSaturation = saturation * 0.16f, value = 0.98f),
    tone(hueOffset = 0f, toneSaturation = saturation * 0.22f, value = 0.06f),
  )
}

private val customThemeJson = Json { ignoreUnknownKeys = true }

fun encodeCustomThemes(themes: List<CustomTheme>): String = customThemeJson.encodeToString(themes)

fun decodeCustomThemes(raw: String): List<CustomTheme> =
  runCatching { customThemeJson.decodeFromString<List<CustomTheme>>(raw) }
    .getOrDefault(emptyList())
