package app.aryan447.mpvium.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.R
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Cinematic Motion Graphics Launch Experience for mpvium.
 *
 * Choreography:
 *  1. [0.0s – 0.6s] **Laser Outline Draw**: A luminous tracer head races around the
 *     play button silhouette in the dark, etching the iconic mark with neon brilliance.
 *  2. [0.5s – 1.0s] **Ignition & Volumetric Rays**: As the stroke closes, the play
 *     button floods with its vibrant streaming gradient. Volumetric light beams
 *     radiate outward through deep cinema haze (Netflix / Apple style).
 *  3. [0.7s – 1.2s] **Editorial Reveal**: The unified "mpvium" wordmark emerges
 *     beneath the illuminated mark.
 *  4. [1.2s – 1.6s] **Zoom-Through Portal Exit**: The glowing glyph expands forward
 *     into the screen (scale 1.16x) and cross-dissolves directly into the app.
 */
private const val SPLASH_TOTAL_MS = 1600

// Signature motion curves
private val EnterEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val BurstEasing = CubicBezierEasing(0.05f, 0.9f, 0.2f, 1f)
private val ExitEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private val SplashBlack = Color(0xFF050508)
private val SplashLift = Color(0xFF0C0916)

private val FocusViolet = Color(0xFF8A2BE2)
private val RubyCrimson = Color(0xFFFF416C)
private val CyanElectric = Color(0xFF00D2FF)

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
  val clock = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  var isSkipping by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    clock.animateTo(1f, tween(SPLASH_TOTAL_MS, easing = LinearEasing))
    onFinished()
  }

  val skipToFinish: () -> Unit = {
    if (!isSkipping && clock.value < 0.82f) {
      isSkipping = true
      scope.launch {
        clock.animateTo(1f, tween(200, easing = ExitEasing))
        onFinished()
      }
    }
  }

  MpviumSplashFrame(
    progress = clock.value,
    onTap = skipToFinish,
  )
}

@Composable
private fun MpviumSplashFrame(
  progress: Float,
  onTap: (() -> Unit)? = null,
) {
  // Phase mapping
  val traceP = EnterEasing.transform(phase(progress, 0.02f, 0.42f))
  val igniteP = BurstEasing.transform(phase(progress, 0.35f, 0.68f))
  val wordmarkP = EnterEasing.transform(phase(progress, 0.45f, 0.74f))
  val exitP = ExitEasing.transform(phase(progress, 0.82f, 1f))

  val frameAlpha = 1f - exitP
  val zoomScale = 1f + 0.16f * exitP

  Box(
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
        alpha = frameAlpha
        scaleX = zoomScale
        scaleY = zoomScale
      }
      .background(Brush.verticalGradient(listOf(SplashLift, SplashBlack))),
    contentAlignment = Alignment.Center,
  ) {
    // Dynamic Canvas: Volumetric Rays + Laser Tracer + Chromatic Bloom
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f - 24.dp.toPx()

      // 1. Studio softbox top-light warming on
      drawRect(
        brush = Brush.verticalGradient(
          0f to Color.White.copy(alpha = 0.05f * phase(progress, 0f, 0.4f)),
          0.42f to Color.Transparent,
        ),
      )

      // 2. Volumetric Light Rays (Netflix / Apple style)
      if (igniteP > 0.01f) {
        val rayRot = (progress - 0.35f) * 25f
        val numRays = 12
        val rayLength = size.minDimension * lerp(0.35f, 0.65f, igniteP)
        val rayAlpha = 0.20f * igniteP * (1f - 0.4f * exitP)

        rotate(degrees = rayRot, pivot = Offset(cx, cy)) {
          for (rIdx in 0 until numRays) {
            val angle = rIdx * (2f * Math.PI.toFloat() / numRays)
            val halfAngle = 0.11f

            val p1 = Offset(
              cx + cos(angle - halfAngle) * rayLength,
              cy + sin(angle - halfAngle) * rayLength,
            )
            val p2 = Offset(
              cx + cos(angle + halfAngle) * rayLength,
              cy + sin(angle + halfAngle) * rayLength,
            )

            val rayPath = Path().apply {
              moveTo(cx, cy)
              lineTo(p1.x, p1.y)
              lineTo(p2.x, p2.y)
              close()
            }

            val rayColor = if (rIdx % 2 == 0) FocusViolet else CyanElectric
            drawPath(
              path = rayPath,
              brush = Brush.radialGradient(
                0f to rayColor.copy(alpha = rayAlpha),
                0.55f to RubyCrimson.copy(alpha = rayAlpha * 0.4f),
                1f to Color.Transparent,
                center = Offset(cx, cy),
                radius = rayLength,
              ),
            )
          }
        }
      }

      // 3. Chromatic Aura Bloom
      val glowRadius = size.minDimension * lerp(0.30f, 0.54f, igniteP)
      val glowAlpha = 0.10f + 0.16f * igniteP
      drawCircle(
        brush = Brush.radialGradient(
          0.0f to FocusViolet.copy(alpha = glowAlpha),
          0.42f to RubyCrimson.copy(alpha = glowAlpha * 0.6f),
          0.78f to CyanElectric.copy(alpha = glowAlpha * 0.25f),
          1.0f to Color.Transparent,
          center = Offset(cx, cy),
          radius = glowRadius,
        ),
        radius = glowRadius,
        center = Offset(cx, cy),
      )

      // 4. Cinema Vignette
      drawRect(
        brush = Brush.radialGradient(
          0f to Color.Transparent,
          0.60f to Color.Transparent,
          1f to Color.Black.copy(alpha = 0.45f),
          center = Offset(cx, cy),
          radius = size.maxDimension * 0.66f,
        ),
      )

      // 5. Laser Stroke Tracing the Play Button Silhouette
      if (traceP in 0.001f..0.999f) {
        val targetW = 76.dp.toPx()
        val targetH = 94.dp.toPx()
        val left = cx - targetW * 0.42f
        val right = cx + targetW * 0.52f
        val top = cy - targetH * 0.48f
        val bottom = cy + targetH * 0.48f

        val seg1Len = hypot(right - left, cy - top)
        val seg2Len = hypot(left - right, bottom - cy)
        val seg3Len = bottom - top
        val totalLen = seg1Len + seg2Len + seg3Len

        val currDist = totalLen * traceP
        val strokePath = Path().apply { moveTo(left, top) }
        var tipX = left
        var tipY = top

        if (currDist <= seg1Len) {
          val f = currDist / seg1Len
          tipX = lerp(left, right, f)
          tipY = lerp(top, cy, f)
          strokePath.lineTo(tipX, tipY)
        } else if (currDist <= seg1Len + seg2Len) {
          strokePath.lineTo(right, cy)
          val f = (currDist - seg1Len) / seg2Len
          tipX = lerp(right, left, f)
          tipY = lerp(cy, bottom, f)
          strokePath.lineTo(tipX, tipY)
        } else {
          strokePath.lineTo(right, cy)
          strokePath.lineTo(left, bottom)
          val f = (currDist - seg1Len - seg2Len) / seg3Len
          tipX = left
          tipY = lerp(bottom, top, f)
          strokePath.lineTo(tipX, tipY)
        }

        // Glow halo under stroke
        drawPath(
          path = strokePath,
          color = FocusViolet.copy(alpha = 0.45f),
          style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Sharp core laser stroke
        drawPath(
          path = strokePath,
          color = Color.White.copy(alpha = 0.95f),
          style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Luminous Tracer Bead at tip
        drawCircle(
          brush = Brush.radialGradient(
            0f to Color.White,
            0.35f to CyanElectric.copy(alpha = 0.8f),
            1f to Color.Transparent,
            center = Offset(tipX, tipY),
            radius = 16.dp.toPx(),
          ),
          radius = 16.dp.toPx(),
          center = Offset(tipX, tipY),
        )
        drawCircle(
          color = Color.White,
          radius = 4.dp.toPx(),
          center = Offset(tipX, tipY),
        )
      }
    }

    // Hero Emblem + Wordmark
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxSize()
        .offset(y = (-24).dp),
    ) {
      // Play Mark: Floods with gradient on ignition
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(width = 76.dp, height = 94.dp),
      ) {
        if (igniteP > 0.01f) {
          Image(
            painter = painterResource(R.drawable.ic_mpvium_mark),
            contentDescription = null,
            modifier = Modifier
              .fillMaxSize()
              .scale(lerp(0.95f, 1f, igniteP))
              .alpha(igniteP),
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Wordmark: Smoothly reveals as illumination peaks
      if (wordmarkP > 0.01f) {
        Text(
          text = "mpvium",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            letterSpacing = 1.4.sp,
          ),
          color = Color(0xFFF7F7FA),
          modifier = Modifier
            .offset(y = lerp(10f, 0f, wordmarkP).dp)
            .alpha(wordmarkP),
        )
      }
    }
  }
}

private fun phase(progress: Float, start: Float, end: Float): Float =
  ((progress - start) / (end - start)).coerceIn(0f, 1f)

private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

@Preview(name = "Laser Trace Phase", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun MpviumSplashPreviewTrace() {
  MaterialTheme {
    MpviumSplashFrame(progress = 0.25f)
  }
}

@Preview(name = "Ignition & Rays Phase", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun MpviumSplashPreviewIgnition() {
  MaterialTheme {
    MpviumSplashFrame(progress = 0.60f)
  }
}

@Preview(name = "Zoom-Through Exit", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun MpviumSplashPreviewExit() {
  MaterialTheme {
    MpviumSplashFrame(progress = 0.90f)
  }
}
