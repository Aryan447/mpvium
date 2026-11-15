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
  val primarySeed: Int = primaryDark,
  val secondarySeed: Int = secondaryDark,
  val tertiarySeed: Int = tertiaryDark,
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

  val primaryColor: Color
    get() = Color(primarySeed)

  val secondaryColor: Color
    get() = Color(secondarySeed)

  val tertiaryColor: Color
    get() = Color(tertiarySeed)

  /** The primary color this theme was painted from, used for backwards compatibility. */
  val seedColor: Color
    get() = primaryColor

  companion object {
    /**
     * Builds a custom theme from distinct primary, secondary, and tertiary colors.
     * Generates tonal variants for light, dark, and AMOLED modes with accessible contrast.
     */
    fun fromColors(
      id: String,
      name: String,
      primary: Color,
      secondary: Color,
      tertiary: Color,
    ): CustomTheme {
      val primaryLight = toneForLight(primary)
      val primaryDark = toneForDark(primary)
      val secondaryLight = toneForLight(secondary)
      val secondaryDark = toneForDark(secondary)
      val tertiaryLight = toneForLight(tertiary)
      val tertiaryDark = toneForDark(tertiary)

      val bgLight = toneForBackgroundLight(primary)
      val bgDark = toneForBackgroundDark(primary)

      return CustomTheme(
        id = id,
        name = name,
        primaryLight = primaryLight,
        primaryDark = primaryDark,
        secondaryLight = secondaryLight,
        secondaryDark = secondaryDark,
        tertiaryLight = tertiaryLight,
        tertiaryDark = tertiaryDark,
        backgroundLight = bgLight,
        backgroundDark = bgDark,
        primarySeed = primary.toArgb(),
        secondarySeed = secondary.toArgb(),
        tertiarySeed = tertiary.toArgb(),
      )
    }

    /**
     * Builds a complete light/dark palette from one accent color seed.
     */
    fun fromSeed(
      id: String,
      name: String,
      seed: Color,
    ): CustomTheme {
      val hsv = FloatArray(3)
      android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
      val sec = Color(
        android.graphics.Color.HSVToColor(
          floatArrayOf((hsv[0] + 32f) % 360f, (hsv[1] * 0.7f).coerceIn(0f, 1f), hsv[2])
        )
      )
      val tert = Color(
        android.graphics.Color.HSVToColor(
          floatArrayOf(((hsv[0] - 38f) % 360f + 360f) % 360f, (hsv[1] * 0.8f).coerceIn(0f, 1f), hsv[2])
        )
      )
      return fromColors(id = id, name = name, primary = seed, secondary = sec, tertiary = tert)
    }
  }
}

private fun toneForLight(color: Color): Int {
  val hsv = FloatArray(3)
  android.graphics.Color.colorToHSV(color.toArgb(), hsv)
  val sat = hsv[1].coerceAtLeast(0.30f)
  // Light mode needs high contrast against light surfaces: value ~0.44
  return android.graphics.Color.HSVToColor(
    floatArrayOf(
      hsv[0],
      (sat * 0.95f).coerceIn(0.40f, 1f),
      0.44f,
    )
  )
}

private fun toneForDark(color: Color): Int {
  val hsv = FloatArray(3)
  android.graphics.Color.colorToHSV(color.toArgb(), hsv)
  // Dark mode needs luminous, readable accent: value ~0.88
  val sat = if (hsv[1] < 0.12f) hsv[1] else (hsv[1] * 0.80f).coerceIn(0.20f, 0.85f)
  val darkVal = hsv[2].coerceAtLeast(0.85f)
  return android.graphics.Color.HSVToColor(
    floatArrayOf(
      hsv[0],
      sat,
      darkVal,
    )
  )
}

private fun toneForBackgroundLight(primary: Color): Int {
  val hsv = FloatArray(3)
  android.graphics.Color.colorToHSV(primary.toArgb(), hsv)
  val sat = (hsv[1] * 0.12f).coerceIn(0f, 0.08f)
  return android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], sat, 0.98f))
}

private fun toneForBackgroundDark(primary: Color): Int {
  val hsv = FloatArray(3)
  android.graphics.Color.colorToHSV(primary.toArgb(), hsv)
  val sat = (hsv[1] * 0.20f).coerceIn(0f, 0.12f)
  return android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], sat, 0.06f))
}

private val customThemeJson = Json { ignoreUnknownKeys = true }

fun encodeCustomThemes(themes: List<CustomTheme>): String = customThemeJson.encodeToString(themes)

fun decodeCustomThemes(raw: String): List<CustomTheme> =
  runCatching { customThemeJson.decodeFromString<List<CustomTheme>>(raw) }
    .getOrDefault(emptyList())
