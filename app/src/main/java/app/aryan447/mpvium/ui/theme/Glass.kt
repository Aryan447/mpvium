package app.aryan447.mpvium.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle

/**
 * Clear Glass core kit (chrome-first).
 *
 * No backdrop blur: glass here is frost fill + hairline rim, deliberately NOT
 * a live-blur lens. Live blur (Haze) smeared scrolling content behind bars
 * and was invisible on dark surfaces, so it was removed — translucency
 * without blur also let list text bleed through dialogs. The Haze dependency
 * is retained (types only) in case a true-lens library lands on Maven Central
 * later; see the progress tracker. Preferred lens candidates
 * (Abdullajon1881 LiquidGlass 1.0.0, Haze 2.x `haze-glass` beta) are NOT
 * yet published to Maven Central, so neither can be a Gradle dependency
 * without breaking CI resolution.
 */
enum class GlassKind {
  Bar,
  Card,
  Sheet,
  Chip,
}

object GlassTokens {
  // CLEAR look: low alpha so content stays readable behind chrome, with a
  // hairline rim for definition. Dark-theme frost is white-based so glass
  // stays visible over near-black surfaces (black-on-black was invisible).
  // Sheets/dialogs are near-opaque veils: body text must never collide with
  // content showing through.
  val blurRadius: Dp = 12.dp
  val chipBlurRadius: Dp = 8.dp
  val cardBlurRadius: Dp = 10.dp
  // Translucent fills — neutral black/white bases, no blue tint.
  // Dark Chip/Bar carry a little extra milk so the gloss gradient reads.
  const val lightSurfaceAlpha: Float = 0.32f
  const val darkSurfaceAlpha: Float = 0.26f
  const val lightCardAlpha: Float = 0.28f
  const val darkCardAlpha: Float = 0.20f
  const val lightSheetAlpha: Float = 0.68f
  const val darkSheetAlpha: Float = 0.60f
  const val glossBoost: Float = 0.12f
  const val glossOverlayAlpha: Float = 0.14f
  const val tintAlpha: Float = 0.04f
  const val rimAlpha: Float = 0.35f
  const val highlightAlpha: Float = 0.18f
  val pillShape = RoundedCornerShape(32.dp)
  val cardShape = RoundedCornerShape(20.dp)
  val sheetShape = RoundedCornerShape(16.dp)
}

val LocalGlass = compositionLocalOf { false }

val GlassHazeBlurRadius: Dp = GlassTokens.blurRadius

/** Retained no-op state holder (blur removed): keeps call sites unchanged. */
@Composable
fun rememberGlassHazeState(): HazeState = remember { HazeState() }

/**
 * Former backdrop-blur source marker. Now a pass-through (no blur anywhere):
 * kept so call sites don't churn if a lens ever returns.
 */
fun Modifier.glassBackdrop(
  state: HazeState,
  enabled: Boolean,
): Modifier = this

/** Frost fill for clear glass: style background only (no blur is applied). */
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
  // White-based frost in dark theme so glass reads over black surfaces;
  // sheets stay dark veils so dialog text never collides with show-through.
  // (blurRadius is retained on the style but no blur modifier reads it.)
  val base = glassFrostColor(isDark = isDark, kind = kind)
  return HazeStyle(
    backgroundColor = base,
    tint = null,
    blurRadius = blur,
  )
}

/**
 * Clear-glass chrome modifier: gloss-gradient frost fill + optional hairline
 * rim. The fill is brightest at the top edge and settles to the base frost
 * below, which is what reads as curved glass. No backdrop blur is applied
 * anywhere. Falls back to the call site's own container when [enabled] is
 * false (non-glass themes must keep their existing opaque behavior).
 */
fun Modifier.glassChrome(
  state: HazeState,
  style: HazeStyle,
  shape: RoundedCornerShape = GlassTokens.pillShape,
  enabled: Boolean,
  rim: Boolean = false,
): Modifier = composed {
  if (!enabled) return@composed this
  val base = style.backgroundColor
  val gloss = Brush.verticalGradient(
    0.0f to base.copy(alpha = (base.alpha + GlassTokens.glossBoost).coerceAtMost(0.6f)),
    0.4f to base,
    1.0f to base.copy(alpha = base.alpha * 0.6f),
  )
  clip(shape)
    .background(gloss, shape)
    .then(
      if (rim) {
        Modifier.border(
          1.dp,
          glassRimColor(androidx.compose.foundation.isSystemInDarkTheme()),
          shape,
        )
      } else {
        Modifier
      },
    )
}

/**
 * Specular gloss overlay for M3 components whose fill comes from a
 * `*Colors` object (buttons, icon buttons) rather than [glassChrome]:
 * draws content first, then a top-weighted white sheen clipped to [shape].
 * Kept faint so labels stay readable; skip on body-text surfaces.
 */
fun Modifier.glassSheen(
  shape: Shape,
  enabled: Boolean,
): Modifier = composed {
  if (!enabled) return@composed this
  clip(shape)
    .drawWithContent {
      drawContent()
      drawRect(
        brush = Brush.verticalGradient(
          0.0f to Color.White.copy(alpha = GlassTokens.glossOverlayAlpha),
          0.55f to Color.Transparent,
        ),
        size = size,
      )
    }
}

/**
 * Frost fill used by every glass surface ([glassChrome] reads it off the
 * style; funnels below call it directly). White-based in dark theme so glass
 * stays visible over black; sheets are dark veils so dialog text stays
 * readable with zero background bleed.
 */
@Composable
fun glassFrostColor(isDark: Boolean, kind: GlassKind = GlassKind.Card): Color {
  return when (kind) {
    GlassKind.Chip -> if (isDark) {
      Color.White.copy(alpha = 0.20f)
    } else {
      Color.White.copy(alpha = 0.26f)
    }
    GlassKind.Bar -> if (isDark) {
      Color.White.copy(alpha = GlassTokens.darkSurfaceAlpha)
    } else {
      Color.White.copy(alpha = GlassTokens.lightSurfaceAlpha)
    }
    GlassKind.Card -> if (isDark) {
      Color.White.copy(alpha = GlassTokens.darkCardAlpha)
    } else {
      Color.White.copy(alpha = GlassTokens.lightCardAlpha)
    }
    GlassKind.Sheet -> if (isDark) {
      Color.Black.copy(alpha = GlassTokens.darkSheetAlpha)
    } else {
      Color.White.copy(alpha = GlassTokens.lightSheetAlpha)
    }
  }
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
 * Sheet container: dark veil for Glass, standard M3 surface
 * otherwise. Single funnel for ModalBottomSheet / PlayerSheet / dialog
 * surfaces so sheets stay readable with no background bleed.
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
  // Menus float over busy lists: sheet-grade veil so items stay readable.
  return glassFrostColor(isDark = dark, kind = GlassKind.Sheet)
}

/**
 * Button container: clear frost for Glass, [fallback] otherwise. Single funnel
 * for hero/detail Play + Details actions sitting over backdrop art so buttons
 * read as glass (frost fill + rim, no blur).
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
