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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import kotlin.math.hypot
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * X (Twitter)-style "zoom-through" mask reveal launch gate for mpvium.
 *
 * Architecture:
 * 1. System Splash Handoff: Android Theme.SplashScreen (#0E0B14) seamlessly hands off
 *    at 1.0f scale to Compose with zero flicker.
 * 2. Hold Phase (~280ms): Static full-color play logo (pink -> purple -> blue gradient with
 *    glossy top highlight) held at scale = 1.0f while the media library UI renders silently underneath.
 * 3. Zoom-Through Phase (~380ms):
 *    - Layer 1 (Dark Mask): Offscreen canvas filled with #0E0B14 punches a transparent play-shaped
 *      window (BlendMode.Clear) centered on the optical centroid.
 *    - Layer 2 (Gradient Logo): Cross-fades alpha 1.0f -> 0.0f over first ~40% (152ms) of zoom,
 *      dissolving the solid colored play logo into a clear portal framing the expanding app.
 *    - Scale expands aggressively from 1.0f to ~35f-45f via CubicBezierEasing(0.75f, 0.0f, 0.88f, 0.0f),
 *      clearing all screen corners on both phones and tablets.
 * 4. Cleanup: Splash composable unmounts immediately upon completion (0 GPU overdraw).
 */
private const val HOLD_DURATION_MS = 280L
private const val ZOOM_DURATION_MS = 380
private const val FADE_DURATION_MS = 152 // ~40% of zoom duration

// Aggressive exponential ease-in curve for the explosive zoom-through reveal
private val ZoomEasing = CubicBezierEasing(0.75f, 0.0f, 0.88f, 0.0f)

private val SplashBackground = Color(0xFF0E0B14)
private val GradientPink = Color(0xFFFF8BA7)
private val GradientPurple = Color(0xFF9D3BE8)
private val GradientBlue = Color(0xFF4B7BF5)
private val SpecularHighlightStart = Color(0x66FFFFFF)
private val SpecularHighlightEnd = Color(0x00FFFFFF)

// Mathematically pristine rounded-triangle play glyph in 475 x 600 viewport
private const val PLAY_PATH_DATA =
  "M 0.00,105.97 L 0.00,494.03 A 60.00,60.00 0 0,0 90.87,545.48 L 414.25,351.45 A 60.00,60.00 0 0,0 414.25,248.55 L 90.87,54.52 A 60.00,60.00 0 0,0 0.00,105.97 Z"

// Specular glossy reflection highlight curving along the top edge
private const val HIGHLIGHT_PATH_DATA =
  "M 0.00,260.00 L 0.00,105.97 A 60.00,60.00 0 0,1 90.87,54.52 L 414.25,248.55 A 60.00,60.00 0 0,1 435.34,270.00 C 330.00,310.00 180.00,285.00 0.00,260.00 Z"

// Geometry in 475 x 600 coordinate space
private const val PATH_VIEWPORT_HEIGHT = 600f
private const val OPTICAL_CENTROID_X = 195.0f
private const val OPTICAL_CENTROID_Y = 300.0f
private const val OPTICAL_INRADIUS = 156.9f

@Composable
fun MpviumSplashGate(content: @Composable () -> Unit) {
  var splashDone by rememberSaveable { mutableStateOf(false) }
  Box(modifier = Modifier.fillMaxSize()) {
    content()
    if (!splashDone) {
      MpviumSplashScreen(onFinished = { splashDone = true })
    }
  }
}

@Composable
private fun MpviumSplashScreen(onFinished: () -> Unit) {
  val zoomProgress = remember { Animatable(0f) }
  val logoAlpha = remember { Animatable(1f) }
  var isFinished by remember { mutableStateOf(false) }

  val finishSplash = remember {
    {
      if (!isFinished) {
        isFinished = true
        onFinished()
      }
    }
  }

  LaunchedEffect(Unit) {
    // 1. Hold Phase (~280ms): static scale=1.0f, alpha=1.0f
    delay(HOLD_DURATION_MS)

    // 2. Zoom-Through Phase (~380ms)
    coroutineScope {
      // Cross-fade Layer 2 (logo) alpha down to 0.0f over first ~40% (152ms)
      launch {
        logoAlpha.animateTo(
          targetValue = 0f,
          animationSpec = tween(durationMillis = FADE_DURATION_MS, easing = LinearEasing),
        )
      }
      // Animate scale up to maxScale with exponential ease-in
      launch {
        zoomProgress.animateTo(
          targetValue = 1f,
          animationSpec = tween(durationMillis = ZOOM_DURATION_MS, easing = ZoomEasing),
        )
      }
    }

    // 3. Cleanup: immediately unmount splash overlay
    finishSplash()
  }

  MpviumSplashContent(
    zoomProgress = zoomProgress.value,
    logoAlpha = logoAlpha.value,
    onTap = finishSplash,
  )
}

@Composable
private fun MpviumSplashContent(
  zoomProgress: Float,
  logoAlpha: Float,
  onTap: (() -> Unit)? = null,
) {
  val density = LocalDensity.current
  // Base emblem size matches 68dp x 86dp (viewport ratio 475:600)
  val baseHeightPx = with(density) { 86.dp.toPx() }
  val pathScale = baseHeightPx / PATH_VIEWPORT_HEIGHT
  val inradiusPx = OPTICAL_INRADIUS * pathScale

  // Matrix shifts optical centroid (195.0, 300.0) to origin (0, 0)
  // and scales to base display size so zoom expands uniformly from optical center
  val matrix = remember(pathScale) {
    android.graphics.Matrix().apply {
      postTranslate(-OPTICAL_CENTROID_X, -OPTICAL_CENTROID_Y)
      postScale(pathScale, pathScale)
    }
  }

  val transformedPlayPath = remember(matrix) {
    val androidPath = PathParser.createPathFromPathData(PLAY_PATH_DATA)
    androidPath.transform(matrix)
    androidPath.asComposePath()
  }

  val transformedHighlightPath = remember(matrix) {
    val androidPath = PathParser.createPathFromPathData(HIGHLIGHT_PATH_DATA)
    androidPath.transform(matrix)
    androidPath.asComposePath()
  }

  val playGradient = remember(pathScale) {
    Brush.linearGradient(
      colorStops = arrayOf(
        0.0f to GradientPink,
        0.5f to GradientPurple,
        1.0f to GradientBlue,
      ),
      start = Offset(-OPTICAL_CENTROID_X * pathScale, -250f * pathScale),
      end = Offset(245f * pathScale, 250f * pathScale),
    )
  }

  val highlightGradient = remember(pathScale) {
    Brush.linearGradient(
      colorStops = arrayOf(
        0.0f to SpecularHighlightStart,
        1.0f to SpecularHighlightEnd,
      ),
      start = Offset((90f - OPTICAL_CENTROID_X) * pathScale, (54f - OPTICAL_CENTROID_Y) * pathScale),
      end = Offset((260f - OPTICAL_CENTROID_X) * pathScale, (290f - OPTICAL_CENTROID_Y) * pathScale),
    )
  }

  Canvas(
    modifier = Modifier
      .fillMaxSize()
      .then(
        if (onTap != null) {
          Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onTap,
          )
        } else Modifier
      )
      .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
      }
  ) {
    val screenCenter = Offset(size.width / 2f, size.height / 2f)
    val maxCornerDist = hypot(screenCenter.x, screenCenter.y)

    // Dynamically calculate max scale from viewport diagonal / triangle inradius
    // with 25% safety margin so edges completely clear all corners on phones and tablets
    val dynamicMaxScale = maxOf(38f, (maxCornerDist / inradiusPx) * 1.25f)
    val currentScale = 1f + (dynamicMaxScale - 1f) * zoomProgress

    // Layer 1 (Dark Mask): Fill offscreen canvas with deep dark #0E0B14
    drawRect(color = SplashBackground)

    // Cutout & Logo transformation sharing identical scale and optical pivot
    withTransform({
      translate(left = screenCenter.x, top = screenCenter.y)
      scale(scaleX = currentScale, scaleY = currentScale, pivot = Offset.Zero)
    }) {
      // Layer 1 cutout: BlendMode.Clear punches transparent window directly into live UI
      drawPath(
        path = transformedPlayPath,
        color = Color.Transparent,
        blendMode = BlendMode.Clear,
      )

      // Layer 2: Full-color gradient play logo + glossy specular highlight
      if (logoAlpha > 0f) {
        drawPath(
          path = transformedPlayPath,
          brush = playGradient,
          alpha = logoAlpha,
        )
        drawPath(
          path = transformedHighlightPath,
          brush = highlightGradient,
          alpha = logoAlpha,
        )
      }
    }
  }
}

@Preview(name = "Splash Zoom-Through Preview", showBackground = true, backgroundColor = 0xFF0E0B14)
@Composable
private fun MpviumSplashPreview() {
  MaterialTheme {
    MpviumSplashContent(
      zoomProgress = 0f,
      logoAlpha = 1f,
    )
  }
}
