package app.aryan447.mpvium.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import app.aryan447.mpvium.R

/**
 * App themes inspired by Aniyomi design
 * Each theme has light and dark color schemes with unique backgrounds
 */
enum class AppTheme(
  @StringRes val titleRes: Int,
  val primaryLight: Color,
  val primaryDark: Color,
  val secondaryLight: Color,
  val secondaryDark: Color,
  val tertiaryLight: Color,
  val tertiaryDark: Color,
  // Background colors - tinted with theme color
  val backgroundLight: Color,
  val backgroundDark: Color,
  val isDynamic: Boolean = false,
) {
  Default(
    titleRes = R.string.theme_default,
    primaryLight = Color(0xFF794F81),
    primaryDark = Color(0xFFE8B5EF),
    secondaryLight = Color(0xFF6A596C),
    secondaryDark = Color(0xFFD6C0D6),
    tertiaryLight = Color(0xFF82524D),
    tertiaryDark = Color(0xFFF5B7B0),
    backgroundLight = Color(0xFFFFF7FB),
    backgroundDark = Color(0xFF161217),
  ),
  Dynamic(
    titleRes = R.string.theme_dynamic,
    primaryLight = Color(0xFF6750A4),
    primaryDark = Color(0xFFD0BCFF),
    secondaryLight = Color(0xFF625B71),
    secondaryDark = Color(0xFFCCC2DC),
    tertiaryLight = Color(0xFF7D5260),
    tertiaryDark = Color(0xFFEFB8C8),
    backgroundLight = Color(0xFFFFFBFF),
    backgroundDark = Color(0xFF1C1B1F),
    isDynamic = true,
  ),
  Cinema(
    titleRes = R.string.theme_cinema,
    primaryLight = Color(0xFF8A5F00),
    primaryDark = Color(0xFFFFD27A),
    secondaryLight = Color(0xFF7A1010),
    secondaryDark = Color(0xFFFF5147),
    tertiaryLight = Color(0xFF9A5B00),
    tertiaryDark = Color(0xFFFFB64D),
    backgroundLight = Color(0xFFFFF1CF),
    backgroundDark = Color(0xFF0F0607),
  ),
  NoirCinema(
    titleRes = R.string.theme_noir_cinema,
    primaryLight = Color(0xFF1E2025),
    primaryDark = Color(0xFFE6E1D5),
    secondaryLight = Color(0xFF4E565E),
    secondaryDark = Color(0xFF9AA3AB),
    tertiaryLight = Color(0xFF7A5C1E),
    tertiaryDark = Color(0xFFD9A940),
    backgroundLight = Color(0xFFEFE9DC),
    backgroundDark = Color(0xFF0A0B0D),
  );

  /**
   * Get the light color scheme for this theme
   */
  fun getLightColorScheme(): ColorScheme {
    val surfaceTint = primaryLight.copy(alpha = 0.05f).compositeOver(backgroundLight)
    return lightColorScheme(
      primary = primaryLight,
      onPrimary = Color.White,
      primaryContainer = primaryLight.copy(alpha = 0.15f).compositeOver(Color.White),
      onPrimaryContainer = primaryLight.darken(0.3f),
      secondary = secondaryLight,
      onSecondary = Color.White,
      secondaryContainer = secondaryLight.copy(alpha = 0.15f).compositeOver(Color.White),
      onSecondaryContainer = secondaryLight.darken(0.3f),
      tertiary = tertiaryLight,
      onTertiary = Color.White,
      tertiaryContainer = tertiaryLight.copy(alpha = 0.15f).compositeOver(Color.White),
      onTertiaryContainer = tertiaryLight.darken(0.3f),
      error = Color(0xFFBA1A1A),
      onError = Color.White,
      errorContainer = Color(0xFFFFDAD6),
      onErrorContainer = Color(0xFF93000A),
      background = backgroundLight,
      onBackground = Color(0xFF1C1B1F),
      surface = backgroundLight,
      onSurface = Color(0xFF1C1B1F),
      surfaceVariant = primaryLight.copy(alpha = 0.08f).compositeOver(Color(0xFFF0F0F0)),
      onSurfaceVariant = Color(0xFF49454F),
      outline = secondaryLight.copy(alpha = 0.5f).compositeOver(Color(0xFF79747E)),
      outlineVariant = primaryLight.copy(alpha = 0.12f).compositeOver(Color(0xFFCAC4D0)),
      inverseSurface = backgroundDark,
      inverseOnSurface = Color(0xFFF4EFF4),
      inversePrimary = primaryDark,
      surfaceContainerLowest = backgroundLight,
      surfaceContainerLow = surfaceTint,
      surfaceContainer = primaryLight.copy(alpha = 0.06f).compositeOver(backgroundLight),
      surfaceContainerHigh = primaryLight.copy(alpha = 0.08f).compositeOver(backgroundLight),
      surfaceContainerHighest = primaryLight.copy(alpha = 0.11f).compositeOver(backgroundLight),
    )
  }

  /**
   * Get the dark color scheme for this theme
   */
  fun getDarkColorScheme(): ColorScheme {
    val surfaceTint = primaryDark.copy(alpha = 0.05f).compositeOver(backgroundDark)
    return darkColorScheme(
      primary = primaryDark,
      onPrimary = primaryLight.darken(0.5f),
      primaryContainer = primaryLight.darken(0.3f),
      onPrimaryContainer = primaryDark.lighten(0.1f),
      secondary = secondaryDark,
      onSecondary = secondaryLight.darken(0.5f),
      secondaryContainer = secondaryLight.darken(0.3f),
      onSecondaryContainer = secondaryDark.lighten(0.1f),
      tertiary = tertiaryDark,
      onTertiary = tertiaryLight.darken(0.5f),
      tertiaryContainer = tertiaryLight.darken(0.3f),
      onTertiaryContainer = tertiaryDark.lighten(0.1f),
      error = Color(0xFFFFB4AB),
      onError = Color(0xFF690005),
      errorContainer = Color(0xFF93000A),
      onErrorContainer = Color(0xFFFFDAD6),
      background = backgroundDark,
      onBackground = Color(0xFFE6E1E5),
      surface = backgroundDark,
      onSurface = Color(0xFFE6E1E5),
      surfaceVariant = primaryDark.copy(alpha = 0.12f).compositeOver(Color(0xFF2A2A2A)),
      onSurfaceVariant = Color(0xFFCAC4D0),
      outline = secondaryDark.copy(alpha = 0.4f).compositeOver(Color(0xFF938F99)),
      outlineVariant = primaryDark.copy(alpha = 0.15f).compositeOver(Color(0xFF49454F)),
      inverseSurface = backgroundLight,
      inverseOnSurface = Color(0xFF313033),
      inversePrimary = primaryLight,
      surfaceContainerLowest = backgroundDark.darken(0.2f),
      surfaceContainerLow = surfaceTint,
      surfaceContainer = primaryDark.copy(alpha = 0.05f).compositeOver(backgroundDark),
      surfaceContainerHigh = primaryDark.copy(alpha = 0.08f).compositeOver(backgroundDark),
      surfaceContainerHighest = primaryDark.copy(alpha = 0.11f).compositeOver(backgroundDark),
    )
  }

  /**
   * Get the AMOLED (pure black) color scheme for this theme
   */
  fun getAmoledColorScheme(): ColorScheme = getDarkColorScheme().copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = primaryDark.copy(alpha = 0.08f).compositeOver(Color(0xFF1A1A1A)),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainerLowest = Color.Black,
    surfaceContainerHigh = primaryDark.copy(alpha = 0.05f).compositeOver(Color(0xFF151515)),
    surfaceContainerHighest = primaryDark.copy(alpha = 0.08f).compositeOver(Color(0xFF1F1F1F)),
    surfaceDim = Color.Black,
    surfaceBright = primaryDark.copy(alpha = 0.06f).compositeOver(Color(0xFF2A2A2A)),
  )
}

// Extension functions for color manipulation
private fun Color.darken(factor: Float): Color {
  return Color(
    red = (red * (1 - factor)).coerceIn(0f, 1f),
    green = (green * (1 - factor)).coerceIn(0f, 1f),
    blue = (blue * (1 - factor)).coerceIn(0f, 1f),
    alpha = alpha
  )
}

private fun Color.lighten(factor: Float): Color {
  return Color(
    red = (red + (1 - red) * factor).coerceIn(0f, 1f),
    green = (green + (1 - green) * factor).coerceIn(0f, 1f),
    blue = (blue + (1 - blue) * factor).coerceIn(0f, 1f),
    alpha = alpha
  )
}

private fun Color.compositeOver(background: Color): Color {
  val bgAlpha = background.alpha
  val fgAlpha = alpha
  val a = fgAlpha + bgAlpha * (1f - fgAlpha)
  return if (a == 0f) {
    Color.Transparent
  } else {
    Color(
      red = (red * fgAlpha + background.red * bgAlpha * (1f - fgAlpha)) / a,
      green = (green * fgAlpha + background.green * bgAlpha * (1f - fgAlpha)) / a,
      blue = (blue * fgAlpha + background.blue * bgAlpha * (1f - fgAlpha)) / a,
      alpha = a
    )
  }
}
