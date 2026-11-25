package app.aryan447.mpvium.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.preferences.AppIcon
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Indicates whether the launch splash overlay has finished and unmounted.
 * Home screen image loaders defer heavy disk/network decoding until true
 * so the BlendMode.Clear zoom runs without main-thread contention.
 */
val LocalSplashFinished = compositionLocalOf { true }

/**
 * Flagship X (Twitter)-style "zoom-through" mask reveal splash screen for mpvium.
 *
 * Choreographed launch sequence:
 * 1. System Splash Handoff: Matches the exact pixel bounds of Theme.SplashScreen's
 *    iconView and removes the system splash view on the first rendered Compose frame.
 * 2. Hold Phase (250ms min, up to 400ms max waiting for initial media state):
 *    Static at scale 1.0f with no spinner visible inside the cutout.
 * 3. Zoom-Through Phase (320ms): Single uninterrupted Animatable from 1.0f to targetScale
 *    using CubicBezierEasing(0.78f, 0.0f, 0.90f, 0.0f), pivoted at the inner cutout's
 *    centroid until the stroke completely clears all four viewport corners.
 * 4. Cleanup: Unmounts completely once scale >= targetScale.
 */
private const val HOLD_MIN_MS = 250L
private const val HOLD_MAX_MS = 400L
private const val ZOOM_DURATION_MS = 320

// Steep exponential ease-in curve for zoom-through
private val ZoomEasing = CubicBezierEasing(0.78f, 0.0f, 0.90f, 0.0f)

@Composable
fun MpviumSplashGate(
  appIcon: AppIcon = AppIcon.PORTAL,
  backgroundColor: Color = MaterialTheme.colorScheme.surface,
  systemIconSizePx: Int? = null,
  hasPendingSystemSplash: Boolean = false,
  onSystemSplashFirstFrame: () -> Unit = {},
  isContentReady: Boolean = true,
  content: @Composable () -> Unit,
) {
  var splashDone by rememberSaveable { mutableStateOf(false) }

  // Safety guard: if splashDone was restored (e.g. config change) while a system
  // splash view is pending, dismiss it immediately.
  SideEffect {
    if (splashDone && hasPendingSystemSplash) {
      onSystemSplashFirstFrame()
    }
  }

  CompositionLocalProvider(LocalSplashFinished provides splashDone) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Underlying live UI initializes beneath the splash overlay
      content()

      if (!splashDone) {
        MpviumSplashScreen(
          appIcon = appIcon,
          backgroundColor = backgroundColor,
          systemIconSizePx = systemIconSizePx,
          hasPendingSystemSplash = hasPendingSystemSplash,
          onSystemSplashFirstFrame = onSystemSplashFirstFrame,
          isContentReady = isContentReady,
          onFinished = { splashDone = true },
        )
      }
    }
  }
}

@Composable
private fun MpviumSplashScreen(
  appIcon: AppIcon,
  backgroundColor: Color,
  systemIconSizePx: Int?,
  hasPendingSystemSplash: Boolean,
  onSystemSplashFirstFrame: () -> Unit,
  isContentReady: Boolean,
  onFinished: () -> Unit,
) {
  // Remove the system splash view after the first Compose frame with the
  // matched icon size has composed and rendered.
  LaunchedEffect(hasPendingSystemSplash, systemIconSizePx) {
    if (hasPendingSystemSplash) {
      withFrameNanos { }
      onSystemSplashFirstFrame()
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val canvasW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
    val canvasH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
    val density = LocalDensity.current

    // Match Android 12+ SplashScreen iconView bounds (typically 288dp unmasked or 160-192dp masked)
    val emblemSizeDp = remember(systemIconSizePx, density) {
      if (systemIconSizePx != null && systemIconSizePx > 0) {
        with(density) { systemIconSizePx.toDp() }
      } else {
        288.dp
      }
    }
    val baseEmblemSizePx = with(density) { emblemSizeDp.toPx() }
    val unit = baseEmblemSizePx / 108f

    val cx = canvasW / 2f
    val cy = canvasH / 2f

    val (innerCentroid, innerInradiusPx) = remember(cx, cy, unit, appIcon) {
      computeInnerCutoutMetrics(cx = cx, cy = cy, unit = unit, appIcon = appIcon)
    }

    // Dynamic target scale: screen diagonal / inner cutout inradius * 1.35f
    val targetScale = remember(canvasW, canvasH, innerInradiusPx) {
      (hypot(canvasW, canvasH) / innerInradiusPx) * 1.35f
    }

    val scaleAnim = remember { Animatable(1.0f) }
    val scope = rememberCoroutineScope()
    var isSkipping by remember { mutableStateOf(false) }
    val currentIsContentReady by rememberUpdatedState(isContentReady)

    LaunchedEffect(targetScale) {
      // Hold phase: static at 1.0f for 250ms, or until media state is ready (capped at 400ms)
      delay(HOLD_MIN_MS)
      if (!currentIsContentReady) {
        withTimeoutOrNull(HOLD_MAX_MS - HOLD_MIN_MS) {
          snapshotFlow { currentIsContentReady }.first { it }
        }
      }

      if (!isSkipping) {
        scaleAnim.animateTo(
          targetValue = targetScale,
          animationSpec = tween(
            durationMillis = ZOOM_DURATION_MS,
            easing = ZoomEasing,
          ),
        )
        if (scaleAnim.value >= targetScale) {
          onFinished()
        }
      }
    }

    val skipToFinish: () -> Unit = {
      if (!isSkipping && scaleAnim.value < targetScale) {
        isSkipping = true
        scope.launch {
          scaleAnim.animateTo(
            targetValue = targetScale,
            animationSpec = tween(durationMillis = 120, easing = ZoomEasing),
          )
          if (scaleAnim.value >= targetScale) {
            onFinished()
          }
        }
      }
    }

    MpviumSplashCanvas(
      appIcon = appIcon,
      backgroundColor = backgroundColor,
      cx = cx,
      cy = cy,
      unit = unit,
      innerCentroid = innerCentroid,
      currentScale = scaleAnim.value,
      targetScale = targetScale,
      onTap = skipToFinish,
    )
  }
}

@Composable
private fun MpviumSplashCanvas(
  appIcon: AppIcon,
  backgroundColor: Color,
  cx: Float,
  cy: Float,
  unit: Float,
  innerCentroid: Offset,
  currentScale: Float,
  targetScale: Float,
  onTap: () -> Unit,
) {
  // Classic fades solid fill 1.0f -> 0.0f during the first 40% of the zoom curve
  val classicAlpha = remember(currentScale, targetScale) {
    val normalizedZoom = ((currentScale - 1.0f) / (targetScale - 1.0f).coerceAtLeast(1f)).coerceIn(0f, 1f)
    (1f - (normalizedZoom / 0.08f)).coerceIn(0f, 1f)
  }

  Canvas(
    modifier = Modifier
      .fillMaxSize()
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onTap,
      )
      .graphicsLayer {
        // Essential: offscreen compositing buffer allows BlendMode.Clear to punch
        // transparent holes revealing the underlying live UI
        compositingStrategy = CompositingStrategy.Offscreen
      },
  ) {
    // 1. Fill entire offscreen canvas with adaptive background
    drawRect(color = backgroundColor)

    // 2. Scale outward pivoted at the inner triangular cutout's centroid
    withTransform({
      scale(
        scaleX = currentScale,
        scaleY = currentScale,
        pivot = innerCentroid,
      )
    }) {
      when (appIcon) {
        AppIcon.PORTAL -> {
          drawPortalSplash(
            cx = cx,
            cy = cy,
            unit = unit,
            strokeAlpha = 1.0f,
          )
        }
        AppIcon.CLASSIC -> {
          drawClassicSplash(
            cx = cx,
            cy = cy,
            unit = unit,
            classicAlpha = classicAlpha,
          )
        }
      }
    }
  }
}

/**
 * Computes the optical centroid (average of the 3 inner vertices) and minimum
 * inradius (Area / Semiperimeter) of the inner triangular cutout.
 */
private fun computeInnerCutoutMetrics(
  cx: Float,
  cy: Float,
  unit: Float,
  appIcon: AppIcon,
): Pair<Offset, Float> {
  val (v1, v2, v3) = when (appIcon) {
    AppIcon.PORTAL -> Triple(
      Offset(cx - 11.4f * unit, cy - 19.3f * unit),
      Offset(cx - 11.4f * unit, cy + 18.5f * unit),
      Offset(cx + 20.5f * unit, cy),
    )
    AppIcon.CLASSIC -> Triple(
      Offset(cx - 13.67f * unit, cy - 21.0f * unit),
      Offset(cx - 13.67f * unit, cy + 21.0f * unit),
      Offset(cx + 24.5f * unit, cy),
    )
  }

  val centroid = Offset(
    x = (v1.x + v2.x + v3.x) / 3f,
    y = (v1.y + v2.y + v3.y) / 3f,
  )

  val a = hypot(v2.x - v3.x, v2.y - v3.y)
  val b = hypot(v1.x - v3.x, v1.y - v3.y)
  val c = hypot(v1.x - v2.x, v1.y - v2.y)
  val s = (a + b + c) / 2f
  val area = 0.5f * abs((v2.x - v1.x) * (v3.y - v1.y) - (v3.x - v1.x) * (v2.y - v1.y))
  val inradius = (area / s).coerceAtLeast(1f)

  return centroid to inradius
}

/**
 * Draws the Portal (Default) open-stroke logo and inner cutout:
 * - Inner triangular cutout punched with BlendMode.Clear (bridges top-left gap along outer contour).
 * - Full-color pink-purple-blue glossy open-stroke logo drawn directly over the cutout border.
 * - Keeps 1.0f alpha until the cutout clears all viewport corners.
 */
private fun DrawScope.drawPortalSplash(
  cx: Float,
  cy: Float,
  unit: Float,
  strokeAlpha: Float,
) {
  // 1. Inner Triangular Cutout Path (bridges top-left gap cleanly along outer contour)
  val cutoutPath = Path().apply {
    // Inner vertical leg starts just inside left stroke
    moveTo(cx - 11.5f * unit, cy - 9.5f * unit)
    // Bridge top-left gap across to top diagonal leg cap
    lineTo(cx - 15.5f * unit, cy - 26.5f * unit)
    // Down-right along top diagonal to apex
    lineTo(cx + 20.5f * unit, cy)
    // Down-left along bottom diagonal to bottom-left corner
    lineTo(cx - 11.5f * unit, cy + 18.5f * unit)
    close()
  }

  // Punch transparent window in the offscreen layer
  drawPath(
    path = cutoutPath,
    color = Color.Black,
    blendMode = BlendMode.Clear,
  )

  // 2. Open-Stroke Foreground Logo Path matching ic_launcher_portal_foreground.xml
  val strokePath = Path().apply {
    // Top diagonal leg starts at top-left, flush with vertical bar line
    moveTo(cx - 15.5f * unit, cy - 26.5f * unit)
    lineTo(cx + 20.5f * unit, cy - 5f * unit)
    // Smoothly rounded right apex
    quadraticTo(
      cx + 29.5f * unit,
      cy,
      cx + 20.5f * unit,
      cy + 5f * unit,
    )
    lineTo(cx - 7.5f * unit, cy + 22.5f * unit)
    // Smoothly rounded bottom-left vertex
    quadraticTo(
      cx - 15.5f * unit,
      cy + 27f * unit,
      cx - 15.5f * unit,
      cy + 18.5f * unit,
    )
    // Vertical left leg up to its rounded top cap (leaving open gap below top diagonal)
    lineTo(cx - 15.5f * unit, cy - 9.5f * unit)
  }

  // Multi-stop diagonal linear gradient: #FF6B8B -> #9D3BE8 -> #448AFF
  val portalGradient = Brush.linearGradient(
    colorStops = arrayOf(
      0.0f to Color(0xFFFF6B8B).copy(alpha = strokeAlpha),
      0.45f to Color(0xFF9D3BE8).copy(alpha = strokeAlpha),
      1.0f to Color(0xFF448AFF).copy(alpha = strokeAlpha),
    ),
    start = Offset(cx - 17.0f * unit, cy - 27.0f * unit),
    end = Offset(cx + 31.0f * unit, cy + 29.0f * unit),
  )

  drawPath(
    path = strokePath,
    brush = portalGradient,
    style = Stroke(
      width = 8.2f * unit,
      cap = StrokeCap.Round,
      join = StrokeJoin.Round,
    ),
  )
}

/**
 * Draws the Classic solid rounded-triangle logo and zoom portal:
 * - Solid rounded triangle punched with BlendMode.Clear.
 * - Solid vibrant glossy gradient triangle drawn on top at 1.0f alpha initially.
 * - Alpha fades 1.0f -> 0.0f during first 40% of zoom-through, revealing the underlying UI.
 */
private fun DrawScope.drawClassicSplash(
  cx: Float,
  cy: Float,
  unit: Float,
  classicAlpha: Float,
) {
  // Solid rounded play triangle path relative to optical centroid
  val classicTrianglePath = Path().apply {
    moveTo(cx - 13.67f * unit, cy - 18.0f * unit)
    lineTo(cx - 13.67f * unit, cy + 18.0f * unit)
    quadraticTo(
      cx - 13.67f * unit,
      cy + 24.0f * unit,
      cx - 7.67f * unit,
      cy + 20.3f * unit,
    )
    lineTo(cx + 21.83f * unit, cy + 3.2f * unit)
    quadraticTo(
      cx + 27.33f * unit,
      cy,
      cx + 21.83f * unit,
      cy - 3.2f * unit,
    )
    lineTo(cx - 7.67f * unit, cy - 20.3f * unit)
    quadraticTo(
      cx - 13.67f * unit,
      cy - 24.0f * unit,
      cx - 13.67f * unit,
      cy - 18.0f * unit,
    )
    close()
  }

  // Punch the transparent play-shaped cutout in the offscreen layer
  drawPath(
    path = classicTrianglePath,
    color = Color.Black,
    blendMode = BlendMode.Clear,
  )

  // Draw solid glossy gradient triangle over the cutout when classicAlpha > 0f
  if (classicAlpha > 0f) {
    val classicGradient = Brush.linearGradient(
      colorStops = arrayOf(
        0.0f to Color(0xFFFF416C).copy(alpha = classicAlpha),
        0.5f to Color(0xFF8A2BE2).copy(alpha = classicAlpha),
        1.0f to Color(0xFF00D2FF).copy(alpha = classicAlpha),
      ),
      start = Offset(cx - 14.0f * unit, cy - 24.0f * unit),
      end = Offset(cx + 28.0f * unit, cy + 24.0f * unit),
    )

    drawPath(
      path = classicTrianglePath,
      brush = classicGradient,
    )

    // Classic polished specular highlight along upper curve
    val specularPath = Path().apply {
      moveTo(cx - 13.67f * unit, cy - 18.0f * unit)
      lineTo(cx - 7.67f * unit, cy - 20.3f * unit)
      lineTo(cx + 21.83f * unit, cy - 3.2f * unit)
      quadraticTo(
        cx + 10.0f * unit,
        cy - 2.0f * unit,
        cx - 13.67f * unit,
        cy + 4.0f * unit,
      )
      close()
    }

    val specularGradient = Brush.linearGradient(
      colorStops = arrayOf(
        0.0f to Color(0x66FFFFFF).copy(alpha = 0.4f * classicAlpha),
        1.0f to Color(0x00FFFFFF),
      ),
      start = Offset(cx - 13.67f * unit, cy - 20.0f * unit),
      end = Offset(cx + 21.83f * unit, cy - 3.2f * unit),
    )

    drawPath(
      path = specularPath,
      brush = specularGradient,
    )
  }
}
