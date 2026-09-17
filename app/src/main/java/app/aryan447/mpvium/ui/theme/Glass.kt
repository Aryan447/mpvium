package app.aryan447.mpvium.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Clear Glass core kit (chrome-first).
 *
 * Current blur core: Haze 1.5.3 (Maven Central, latest).
 * The preferred true-lens library (Abdullajon1881 LiquidGlass 1.0.0) is NOT
 * yet published to Maven Central (README: "Maven Central publishing is
 * configured but not yet released"), so it cannot be a Gradle dependency
 * without breaking CI resolution. This file isolates all glass calls so the
 * lens implementation can be swapped later without touching call sites:
 * replace [glassChrome] internals with `Modifier.liquidGlass(...)` + provider.
 */
enum class GlassKind {
  Bar,
  Card,
  Sheet,
  Chip,
}

object GlassTokens {
  // CLEAR look: low blur + low alpha so content stays readable behind chrome.
  // Previously 24dp/20dp read as frosted; 12dp/8dp keeps the refractive
  // edge without milky blur.
  val blurRadius: Dp = 12.dp
  val chipBlurRadius: Dp = 8.dp
  val cardBlurRadius: Dp = 10.dp
  // Translucent fills — neutral black/white bases, no blue tint.
  const val lightSurfaceAlpha: Float = 0.32f
  const val darkSurfaceAlpha: Float = 0.28f
  const val lightCardAlpha: Float = 0.28f
  const val darkCardAlpha: Float = 0.24f
  const val tintAlpha: Float = 0.04f
  const val rimAlpha: Float = 0.35f
  const val highlightAlpha: Float = 0.18f
  val pillShape = RoundedCornerShape(32.dp)
  val cardShape = RoundedCornerShape(20.dp)
  val sheetShape = RoundedCornerShape(16.dp)
}

val LocalGlass = compositionLocalOf { false }

val GlassHazeBlurRadius: Dp = GlassTokens.blurRadius

@Composable
fun rememberGlassHazeState(): HazeState = remember { HazeState() }

/** Mark scrolling/content backdrop so glass above can sample it. No-op if glass off. */
fun Modifier.glassBackdrop(
  state: HazeState,
  enabled: Boolean,
): Modifier = composed {
  if (enabled) hazeSource(state = state) else this
}

/** Haze style for clear glass: translucent neutral base + light blur. */
@Composable
fun glassHazeStyle(
  isDark: Boolean,
  kind: GlassKind = GlassKind.Bar,
): HazeStyle {
  val blur = when (kind) {
    GlassKind.Chip -> GlassTokens.chipBlurRadius
    GlassKind.Card, GlassKind.Sheet -> GlassTokens.cardBlurRadius
    GlassKind.Bar -> GlassTokens.blurRadius
  }
  val alpha = when (kind) {
    GlassKind.Card, GlassKind.Sheet ->
      if (isDark) GlassTokens.darkCardAlpha else GlassTokens.lightCardAlpha
    else ->
      if (isDark) GlassTokens.darkSurfaceAlpha else GlassTokens.lightSurfaceAlpha
  }
  // Neutral bases: pure black in dark, white in light. No blue tint.
  val base = if (isDark) {
    Color.Black.copy(alpha = alpha)
  } else {
    Color.White.copy(alpha = alpha)
  }
  return HazeStyle(
    backgroundColor = base,
    tint = null,
    blurRadius = blur,
  )
}

/**
 * Clear-glass chrome modifier: backdrop blur + translucent tint.
 * Falls back to plain translucent surface color when [enabled] is false
 * (non-glass themes must keep their existing opaque behavior at call sites).
 */
fun Modifier.glassChrome(
  state: HazeState,
  style: HazeStyle,
  shape: RoundedCornerShape = GlassTokens.pillShape,
  enabled: Boolean,
): Modifier = composed {
  if (!enabled) return@composed this
  clip(shape)
    .hazeEffect(state = state, style = style)
}

/**
 * Frost fallback for surfaces that have no HazeState in scope (cards,
 * dialogs, sheets, dropdowns). Gives the same clear neutral translucency
 * as [glassChrome] without live blur, so glass reads consistently
 * throughout the UI even before per-screen HazeState refactors.
 */
@Composable
fun glassFrostColor(isDark: Boolean, kind: GlassKind = GlassKind.Card): Color {
  val alpha = when (kind) {
    GlassKind.Chip -> if (isDark) 0.20f else 0.26f
    GlassKind.Bar -> if (isDark) GlassTokens.darkSurfaceAlpha else GlassTokens.lightSurfaceAlpha
    GlassKind.Card, GlassKind.Sheet ->
      if (isDark) GlassTokens.darkCardAlpha else GlassTokens.lightCardAlpha
  }
  return if (isDark) Color.Black.copy(alpha = alpha) else Color.White.copy(alpha = alpha)
}

/** Neutral hairline rim for glass surfaces. */
@Composable
fun glassRimColor(isDark: Boolean): Color =
  if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f)

@Composable
fun glassCardColors(kind: GlassKind = GlassKind.Card): androidx.compose.material3.CardColors {
  val isGlass = LocalGlass.current
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
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
 * Sheet container: clear frost for Glass, standard M3 surface
 * otherwise. Single funnel for ModalBottomSheet / PlayerSheet / dialog
 * surfaces so sheets read as glass without per-screen Haze states.
 */
@Composable
fun glassSheetContainerColor(fallback: Color): Color {
  if (!LocalGlass.current) return fallback
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
  return glassFrostColor(isDark = dark, kind = GlassKind.Sheet)
}

/** Dropdown/exposed-menu container for Glass. */
@Composable
fun glassMenuContainerColor(fallback: Color): Color {
  if (!LocalGlass.current) return fallback
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
  // Menus float over content: slightly more opaque than cards for readability.
  return glassFrostColor(isDark = dark, kind = GlassKind.Card)
}

/**
 * Button container: clear frost for Glass, [fallback] otherwise. Single funnel
 * for hero/detail Play + Details actions sitting over backdrop art so buttons
 * read as glass without per-screen Haze states (frost-appropriate, like FABs).
 */
@Composable
fun glassButtonContainerColor(fallback: Color): Color {
  if (!LocalGlass.current) return fallback
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
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
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
  return androidx.compose.material3.NavigationBarItemDefaults.colors(
    selectedIconColor = glassButtonContentColor(
      androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
    ),
    selectedTextColor = glassButtonContentColor(
      androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    ),
    indicatorColor = glassFrostColor(isDark = dark, kind = GlassKind.Chip),
  )
}

/** Rail twin of [glassNavigationBarItemColors]. */
@Composable
fun glassNavigationRailItemColors(): androidx.compose.material3.NavigationRailItemColors {
  if (!LocalGlass.current) return androidx.compose.material3.NavigationRailItemDefaults.colors()
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
  return androidx.compose.material3.NavigationRailItemDefaults.colors(
    selectedIconColor = glassButtonContentColor(
      androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
    ),
    selectedTextColor = glassButtonContentColor(
      androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    ),
    indicatorColor = glassFrostColor(isDark = dark, kind = GlassKind.Chip),
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
