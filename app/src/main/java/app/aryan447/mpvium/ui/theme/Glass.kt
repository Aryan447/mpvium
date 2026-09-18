package app.aryan447.mpvium.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Clear Liquid Glass core kit (iOS-inspired).
 *
 * Provides true optical clarity:
 * - Live optical backdrop blur via Haze 1.5.3 (hazeEffect + hazeSource)
 * - Directional specular rim gradient (bright overhead light catch on top edge, subtle shadow below)
 * - Refractive top-lip specular glint (internal reflection inside curved glass thickness)
 * - Crystal dark-tinted glass in dark theme (never chalky or milky white) and luminous clarity in light theme
 * - Deep smoked glass for sheets/dialogs for 100% typography contrast
 */
enum class GlassKind {
  Bar,
  Card,
  Sheet,
  Chip,
}

object GlassTokens {
  // CLEAR LIQUID GLASS (NO BLUR, NO FROST):
  // Optical crystal clarity with specular rim and liquid top-lip glint.
  val blurRadius: Dp = 0.dp
  val chipBlurRadius: Dp = 0.dp
  val cardBlurRadius: Dp = 0.dp

  // Translucent base fills: deep crystal in dark, luminous in light
  const val lightSurfaceAlpha: Float = 0.32f
  const val darkSurfaceAlpha: Float = 0.28f
  const val lightCardAlpha: Float = 0.28f
  const val darkCardAlpha: Float = 0.22f
  const val lightSheetAlpha: Float = 0.72f
  const val darkSheetAlpha: Float = 0.70f

  // Specular rim alphas (directional light catch from overhead)
  const val specularRimTopDark: Float = 0.60f
  const val specularRimMidDark: Float = 0.22f
  const val specularRimBottomDark: Float = 0.06f

  const val specularRimTopLight: Float = 0.85f
  const val specularRimMidLight: Float = 0.40f
  const val specularRimBottomLight: Float = 0.10f

  // Internal specular reflection glint (refractive lip)
  const val glintTopAlphaDark: Float = 0.26f
  const val glintMidAlphaDark: Float = 0.04f
  const val glintTopAlphaLight: Float = 0.42f
  const val glintMidAlphaLight: Float = 0.08f

  val pillShape = RoundedCornerShape(32.dp)
  val cardShape = RoundedCornerShape(20.dp)
  val sheetShape = RoundedCornerShape(16.dp)
}

val LocalGlass = compositionLocalOf { false }

val GlassHazeBlurRadius: Dp = GlassTokens.blurRadius

/** State holder for glass layout compatibility. */
@Composable
fun rememberGlassHazeState(): HazeState = remember { HazeState() }

/**
 * In Clear Liquid Glass (blur replaced with clear glass), backdrop sampling
 * is a lightweight pass-through: content behind the glass shines through 100%
 * sharp and clear with zero GPU blur smearing.
 */
fun Modifier.glassBackdrop(
  state: HazeState,
  enabled: Boolean,
): Modifier = this

/** Style for clear liquid glass: optical crystal clarity without blur. */
@Composable
fun glassHazeStyle(
  isDark: Boolean,
  kind: GlassKind = GlassKind.Bar,
): HazeStyle {
  val base = glassFrostColor(isDark = isDark, kind = kind)
  return HazeStyle(
    backgroundColor = base,
    tint = null,
    blurRadius = 0.dp,
  )
}

/**
 * Clear-glass chrome modifier: crystal optical transparency +
 * top-lip specular glint + optional directional specular rim.
 */
fun Modifier.glassChrome(
  state: HazeState,
  style: HazeStyle,
  shape: RoundedCornerShape = GlassTokens.pillShape,
  enabled: Boolean,
  rim: Boolean = false,
): Modifier = composed {
  if (!enabled) return@composed this
  val isDark = isSystemInDarkTheme()
  clip(shape)
    .background(style.backgroundColor, shape)
    .drawWithContent {
      drawContent()
      // Top-lip specular reflection (light caught in curved glass edge)
      drawRect(
        brush = Brush.verticalGradient(
          0.0f to (if (isDark) Color.White.copy(alpha = GlassTokens.glintTopAlphaDark)
          else Color.White.copy(alpha = GlassTokens.glintTopAlphaLight)),
          0.18f to (if (isDark) Color.White.copy(alpha = GlassTokens.glintMidAlphaDark)
          else Color.White.copy(alpha = GlassTokens.glintMidAlphaLight)),
          0.50f to Color.Transparent,
        ),
        size = size,
      )
    }
    .then(
      if (rim) {
        Modifier.border(
          width = 1.dp,
          brush = glassRimBrush(isDark),
          shape = shape,
        )
      } else {
        Modifier
      },
    )
}

/**
 * Specular gloss overlay for glass components (buttons, icon buttons):
 * draws content first, then a delicate top-lip specular sheen clipped to [shape].
 */
fun Modifier.glassSheen(
  shape: Shape,
  enabled: Boolean,
): Modifier = composed {
  if (!enabled) return@composed this
  val isDark = isSystemInDarkTheme()
  clip(shape)
    .drawWithContent {
      drawContent()
      drawRect(
        brush = Brush.verticalGradient(
          0.0f to (if (isDark) Color.White.copy(alpha = GlassTokens.glintTopAlphaDark)
          else Color.White.copy(alpha = GlassTokens.glintTopAlphaLight)),
          0.20f to (if (isDark) Color.White.copy(alpha = GlassTokens.glintMidAlphaDark)
          else Color.White.copy(alpha = GlassTokens.glintMidAlphaLight)),
          0.50f to Color.Transparent,
        ),
        size = size,
      )
    }
}

/**
 * Color fill used by glass surfaces:
 * In dark mode: crystal-tinted dark neutral glass (never chalky white).
 * In light mode: luminous crystal white glass.
 * Sheets stay deep smoked glass so text remains razor sharp.
 */
@Composable
fun glassFrostColor(isDark: Boolean, kind: GlassKind = GlassKind.Card): Color {
  return when (kind) {
    GlassKind.Chip -> if (isDark) {
      Color(0xFF1E1E24).copy(alpha = 0.45f)
    } else {
      Color.White.copy(alpha = 0.55f)
    }
    GlassKind.Bar -> if (isDark) {
      Color(0xFF0E0E12).copy(alpha = GlassTokens.darkSurfaceAlpha)
    } else {
      Color.White.copy(alpha = GlassTokens.lightSurfaceAlpha)
    }
    GlassKind.Card -> if (isDark) {
      Color(0xFF141418).copy(alpha = GlassTokens.darkCardAlpha)
    } else {
      Color.White.copy(alpha = GlassTokens.lightCardAlpha)
    }
    GlassKind.Sheet -> if (isDark) {
      Color(0xFF101014).copy(alpha = GlassTokens.darkSheetAlpha)
    } else {
      Color.White.copy(alpha = GlassTokens.lightSheetAlpha)
    }
  }
}

/** Directional specular gradient rim: catches ambient overhead light on top curve. */
fun glassRimBrush(isDark: Boolean): Brush {
  return Brush.verticalGradient(
    0.0f to (if (isDark) Color.White.copy(alpha = GlassTokens.specularRimTopDark)
    else Color.White.copy(alpha = GlassTokens.specularRimTopLight)),
    0.35f to (if (isDark) Color.White.copy(alpha = GlassTokens.specularRimMidDark)
    else Color.White.copy(alpha = GlassTokens.specularRimMidLight)),
    1.0f to (if (isDark) Color.White.copy(alpha = GlassTokens.specularRimBottomDark)
    else Color.Black.copy(alpha = GlassTokens.specularRimBottomLight)),
  )
}

/** Directional specular hairline rim stroke helper. */
fun glassRimStroke(isDark: Boolean, width: Dp = 1.dp): BorderStroke =
  BorderStroke(width = width, brush = glassRimBrush(isDark))

/** Neutral hairline rim color fallback. */
@Composable
fun glassRimColor(isDark: Boolean): Color =
  if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f)

@Composable
fun glassCardColors(kind: GlassKind = GlassKind.Card): androidx.compose.material3.CardColors {
  val isGlass = LocalGlass.current
  val dark = isSystemInDarkTheme()
  // Non-glass themes keep their default opaque card colors.
  if (!isGlass) return androidx.compose.material3.CardDefaults.cardColors()
  val frost = glassFrostColor(isDark = dark, kind = kind)
  return androidx.compose.material3.CardDefaults.cardColors(
    containerColor = frost,
    disabledContainerColor = frost,
  )
}

/**
 * Player chrome alpha: clear (0.22) for Glass, standard (0.55)
 * otherwise. Use for control surfaces over video so the glass theme stays
 * clear everywhere without touching each call site's non-glass behavior.
 */
@Composable
fun glassPlayerAlpha(default: Float = 0.55f, glass: Float = 0.22f): Float =
  if (LocalGlass.current) glass else default

/**
 * Sheet container: deep smoked glass for Glass, standard M3 surface
 * otherwise. Single funnel for ModalBottomSheet / PlayerSheet / dialog
 * surfaces so sheets stay readable with zero background text collisions.
 */
@Composable
fun glassSheetContainerColor(fallback: Color): Color {
  if (!LocalGlass.current) return fallback
  val dark = isSystemInDarkTheme()
  return glassFrostColor(isDark = dark, kind = GlassKind.Sheet)
}

/** Dropdown/exposed-menu container for Glass. */
@Composable
fun glassMenuContainerColor(fallback: Color): Color {
  if (!LocalGlass.current) return fallback
  val dark = isSystemInDarkTheme()
  // Menus float over busy lists: sheet-grade veil so items stay readable.
  return glassFrostColor(isDark = dark, kind = GlassKind.Sheet)
}

/**
 * Button container: clear glass for Glass, [fallback] otherwise. Single funnel
 * for hero/detail Play + Details actions sitting over backdrop art.
 */
@Composable
fun glassButtonContainerColor(fallback: Color): Color {
  if (!LocalGlass.current) return fallback
  val dark = isSystemInDarkTheme()
  return glassFrostColor(isDark = dark, kind = GlassKind.Chip)
}

/**
 * Button content: high-contrast white/black on frost for Glass, [fallback]
 * otherwise. Needed because frost flips container luminance vs. the opaque
 * primary/tonal fills (light-mode frost + onPrimary white would be unreadable).
 */
@Composable
fun glassButtonContentColor(fallback: Color): Color {
  if (!LocalGlass.current) return fallback
  return if (androidx.compose.foundation.isSystemInDarkTheme()) Color.White else Color.Black
}

/**
 * FilterChip colors: frost container + contrast label for Glass, M3 defaults
 * (with the call site's selected colors) otherwise. Custom unselected
 * [containerColor] hues (e.g. tertiary user-preset markers) are kept at glass
 * alpha instead of frost so their meaning survives; selected fills drop to
 * glass alpha so chips read as glass without losing state affordance.
 */
@Composable
fun glassFilterChipColors(
  containerColor: Color = Color.Transparent,
  labelColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
  selectedContainerColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
  selectedLabelColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
): androidx.compose.material3.SelectableChipColors {
  if (!LocalGlass.current) {
    return androidx.compose.material3.FilterChipDefaults.filterChipColors(
      containerColor = containerColor,
      labelColor = labelColor,
      selectedContainerColor = selectedContainerColor,
      selectedLabelColor = selectedLabelColor,
    )
  }
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
  val glassContainer = if (containerColor == Color.Transparent) {
    glassFrostColor(isDark = dark, kind = GlassKind.Chip)
  } else {
    containerColor.copy(alpha = 0.45f)
  }
  return androidx.compose.material3.FilterChipDefaults.filterChipColors(
    containerColor = glassContainer,
    labelColor = glassButtonContentColor(
      androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    selectedContainerColor = selectedContainerColor.copy(alpha = 0.55f),
    selectedLabelColor = glassButtonContentColor(selectedLabelColor),
  )
}

/**
 * AssistChip colors: frost container + contrast label for Glass, M3 defaults
 * otherwise. Single funnel for word-tap chips in player sheets.
 */
@Composable
fun glassAssistChipColors(): androidx.compose.material3.ChipColors {
  if (!LocalGlass.current) return androidx.compose.material3.AssistChipDefaults.assistChipColors()
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
  return androidx.compose.material3.AssistChipDefaults.assistChipColors(
    containerColor = glassFrostColor(isDark = dark, kind = GlassKind.Chip),
    labelColor = glassButtonContentColor(
      androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    ),
  )
}

/**
 * Bottom-nav item colors: frost indicator pill + contrast selected icon/label
 * for Glass, M3 defaults otherwise. The indicator sits on the already-glass
 * bar, so frost-on-frost keeps the pill readable as a glass highlight.
 */
@Composable
fun glassNavigationBarItemColors(): androidx.compose.material3.NavigationBarItemColors {
  if (!LocalGlass.current) return androidx.compose.material3.NavigationBarItemDefaults.colors()
  val dark = isSystemInDarkTheme()
  return androidx.compose.material3.NavigationBarItemDefaults.colors(
    selectedIconColor = glassButtonContentColor(
      androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
    ),
    selectedTextColor = glassButtonContentColor(
      androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    ),
    indicatorColor = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.65f),
  )
}

/** Rail twin of [glassNavigationBarItemColors]. */
@Composable
fun glassNavigationRailItemColors(): androidx.compose.material3.NavigationRailItemColors {
  if (!LocalGlass.current) return androidx.compose.material3.NavigationRailItemDefaults.colors()
  val dark = isSystemInDarkTheme()
  return androidx.compose.material3.NavigationRailItemDefaults.colors(
    selectedIconColor = glassButtonContentColor(
      androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
    ),
    selectedTextColor = glassButtonContentColor(
      androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    ),
    indicatorColor = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.65f),
  )
}

/**
 * Segmented-button colors: frost inactive container + translucent active fill
 * for Glass, M3 defaults otherwise. Single funnel for the sort-order selector
 * and the appearance multi-choice row.
 */
@Composable
fun glassSegmentedButtonColors(): androidx.compose.material3.SegmentedButtonColors {
  if (!LocalGlass.current) return androidx.compose.material3.SegmentedButtonDefaults.colors()
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
  return androidx.compose.material3.SegmentedButtonDefaults.colors(
    activeContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer.copy(
      alpha = 0.55f,
    ),
    activeContentColor = glassButtonContentColor(
      androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
    ),
    inactiveContainerColor = glassFrostColor(isDark = dark, kind = GlassKind.Chip),
    inactiveContentColor = glassButtonContentColor(
      androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
    ),
  )
}

/**
 * Slider colors: translucent thumb/track for Glass, M3 defaults otherwise.
 * For grid-column sliders inside glass dialogs.
 */
@Composable
fun glassSliderColors(): androidx.compose.material3.SliderColors {
  if (!LocalGlass.current) return androidx.compose.material3.SliderDefaults.colors()
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
  val content = glassButtonContentColor(
    androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
  )
  return androidx.compose.material3.SliderDefaults.colors(
    thumbColor = content,
    activeTrackColor = glassFrostColor(isDark = dark, kind = GlassKind.Chip),
    inactiveTrackColor = glassFrostColor(isDark = dark, kind = GlassKind.Chip).copy(alpha = 0.5f),
  )
}

/** SearchBar colors: clear frost for Glass, M3 defaults otherwise. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun glassSearchBarColors(): androidx.compose.material3.SearchBarColors {
  if (!LocalGlass.current) return androidx.compose.material3.SearchBarDefaults.colors()
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
  return androidx.compose.material3.SearchBarDefaults.colors(
    containerColor = glassFrostColor(isDark = dark, kind = GlassKind.Bar),
  )
}
