package app.aryan447.mpvium.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.preferences.AppIcon
import kotlin.math.hypot
import kotlin.math.max
import kotlinx.coroutines.launch

/**
 * Flagship X (Twitter)-style "zoom-through" mask reveal splash screen for mpvium.
 *
 * Choreographed launch sequence:
 * 1. System Splash Handoff: Theme.SplashScreen matches #0E0B14 canvas and center icon,
 *    handing off immediately at scale 1.0f with zero flash or jitter.
 * 2. Hold Phase (~280ms): Static at scale 1.0f while the underlying Compose UI initializes.
 *    - Portal: Inner transparent window cut out with BlendMode.Clear reveals the live UI inside
 *      the hollow open stroke from frame one.
 *    - Classic: Solid glossy gradient triangle masks the cutout at 1.0f alpha.
 * 3. Zoom-Through Phase (~380ms): CubicBezierEasing(0.75f, 0.0f, 0.88f, 0.0f) zooms both the
 *    BlendMode.Clear mask and foreground emblem outwards pivoted at the optical centroid.
 *    - Portal: Stroke stays at 1.0f alpha until the inner transparent window clears all viewport edges.
 *    - Classic: Fades solid gradient layer alpha 1.0f -> 0.0f over first 40% of zoom, morphing into
 *      a transparent play-shaped portal revealing the underlying UI.
 * 4. Cleanup: Unmounts completely on completion so there is zero GPU overdraw or touch interception.
 */
private const val HOLD_DURATION_MS = 280
private const val ZOOM_DURATION_MS = 380
private const val TOTAL_SPLASH_MS = HOLD_DURATION_MS + ZOOM_DURATION_MS

// High-speed expansion easing curve for zoom-through
private val ZoomEasing = CubicBezierEasing(0.75f, 0.0f, 0.88f, 0.0f)

// Canvas background fallback
private val DefaultSplashCanvasBackground = Color(0xFF0E0B14)

@Composable
fun MpviumSplashGate(
  appIcon: AppIcon = AppIcon.PORTAL,
  backgroundColor: Color = MaterialTheme.colorScheme.surface,
  content: @Composable () -> Unit,
) {
  var splashDone by rememberSaveable { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    // Underlying live UI initializes beneath the splash overlay
    content()

    if (!splashDone) {
      MpviumSplashScreen(
        appIcon = appIcon,
        backgroundColor = backgroundColor,
        onFinished = { splashDone = true },
      )
    }
  }
}

@Composable
private fun MpviumSplashScreen(
  appIcon: AppIcon,
  backgroundColor: Color,
  onFinished: () -> Unit,
) {
  val clock = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  var isSkipping by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    clock.animateTo(1f, tween(TOTAL_SPLASH_MS, easing = LinearEasing))
    onFinished()
  }

  val skipToFinish: () -> Unit = {
    if (!isSkipping && clock.value < 0.95f) {
      isSkipping = true
      scope.launch {
        clock.animateTo(1f, tween(100, easing = LinearEasing))
        onFinished()
      }
    }
  }

  MpviumSplashCanvas(
    appIcon = appIcon,
    backgroundColor = backgroundColor,
    progress = clock.value,
    onTap = skipToFinish,
  )
}

@Composable
private fun MpviumSplashCanvas(
  appIcon: AppIcon,
  backgroundColor: Color,
  progress: Float,
  onTap: () -> Unit,
) {
  val timeMs = progress * TOTAL_SPLASH_MS

  val (zoomScaleRatio, classicAlpha, portalStrokeAlpha) = if (timeMs <= HOLD_DURATION_MS) {
    Triple(0f, 1f, 1f)
  } else {
    val rawZoom = ((timeMs - HOLD_DURATION_MS) / ZOOM_DURATION_MS.toFloat()).coerceIn(0f, 1f)
    val easedZoom = ZoomEasing.transform(rawZoom)

    // Classic fades solid color 1.0f -> 0.0f during the first 40% of zoom-through
    val cAlpha = (1f - (rawZoom / 0.40f)).coerceIn(0f, 1f)

    // Portal keeps 1.0f alpha until inner transparent window clears all viewport edges
    val pAlpha = if (rawZoom >= 0.96f) {
      (1f - (rawZoom - 0.96f) / 0.04f).coerceIn(0f, 1f)
    } else {
      1f
    }

    Triple(easedZoom, cAlpha, pAlpha)
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
    val canvasW = size.width
    val canvasH = size.height
    val cx = canvasW / 2f
    val cy = canvasH / 2f

    // 1. Fill entire offscreen canvas with adaptive background
    drawRect(color = backgroundColor)

    // Base emblem dimensions at 1.0f scale (centered at optical centroid)
    val baseEmblemSize = 88.dp.toPx()
    val unit = baseEmblemSize / 108f

    // Calculate dynamic max scale so the inner transparent cutout clears all 4 screen corners
    // Inradius of inner cutout is ~8.5 units in 108dp coordinate space
    val innerInradiusPx = 8.5f * unit
    val halfDiagonalPx = hypot(canvasW / 2f, canvasH / 2f)
    val requiredClearScale = (halfDiagonalPx / innerInradiusPx) * 1.15f
    val maxScale = max(45f, requiredClearScale)

    val currentScale = 1.0f + (maxScale - 1.0f) * zoomScaleRatio

    // Scale outward pivoted at the emblem's optical centroid (cx, cy)
    withTransform({
      scale(
        scaleX = currentScale,
        scaleY = currentScale,
        pivot = Offset(cx, cy),
      )
    }) {
      when (appIcon) {
        AppIcon.PORTAL -> {
          drawPortalSplash(
            cx = cx,
            cy = cy,
            unit = unit,
            strokeAlpha = portalStrokeAlpha,
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
 * Draws the Portal (Default) open-stroke logo and inner cutout:
 * - Inner triangular cutout punched with BlendMode.Clear (bridges top-left gap along outer contour).
 * - Full-color pink-purple-blue glossy open-stroke logo drawn directly over the cutout border.
 * - Live UI is visible inside the hollow triangle from frame one.
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

  // 2. Open-Stroke Foreground Logo Path matching user design
  // Coordinates relative to optical centroid: (54.0, 54.0) in 108 coordinate space
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
