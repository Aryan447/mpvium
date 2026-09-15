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
 * Clear Liquid Glass core kit (chrome-first).
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

object LiquidGlassTokens {
  val blurRadius: Dp = 24.dp
  val chipBlurRadius: Dp = 20.dp
  const val lightSurfaceAlpha: Float = 0.55f
  const val darkSurfaceAlpha: Float = 0.48f
  const val tintAlpha: Float = 0.08f
  const val rimAlpha: Float = 0.4f
  const val highlightAlpha: Float = 0.25f
  val pillShape = RoundedCornerShape(32.dp)
  val cardShape = RoundedCornerShape(20.dp)
  val sheetShape = RoundedCornerShape(16.dp)
}

val LocalLiquidGlass = compositionLocalOf { false }

val GlassHazeBlurRadius: Dp = LiquidGlassTokens.blurRadius

@Composable
fun rememberGlassHazeState(): HazeState = remember { HazeState() }

/** Mark scrolling/content backdrop so glass above can sample it. No-op if glass off. */
fun Modifier.glassBackdrop(
  state: HazeState,
  enabled: Boolean,
): Modifier = composed {
  if (enabled) hazeSource(state = state) else this
}

/** Haze style for clear glass: translucent base + blur, subtle white lift. */
@Composable
fun glassHazeStyle(
  isDark: Boolean,
  kind: GlassKind = GlassKind.Bar,
): HazeStyle {
  val blur = if (kind == GlassKind.Chip) LiquidGlassTokens.chipBlurRadius else LiquidGlassTokens.blurRadius
  val base = if (isDark) {
    Color(0xFF0B1220).copy(alpha = LiquidGlassTokens.darkSurfaceAlpha)
  } else {
    Color.White.copy(alpha = LiquidGlassTokens.lightSurfaceAlpha)
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
  shape: RoundedCornerShape = LiquidGlassTokens.pillShape,
  enabled: Boolean,
): Modifier = composed {
  if (!enabled) return@composed this
  clip(shape)
    .hazeEffect(state = state, style = style)
}
