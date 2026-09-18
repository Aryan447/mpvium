package app.aryan447.mpvium.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * Branded launch gate.
 *
 * The cold-start system splash (Theme.mpvium.Starting) is intentionally static;
 * all motion lives here so cold start stays fast and the choreography is owned
 * by a single deterministic clock. Content composes underneath the opaque
 * overlay so first-frame work starts while the brand moment plays.
 *
 * Design — quiet luxury, no loader semantics anywhere:
 *  1. A violet backlight breathes once behind the mark.
 *  2. A faint diagonal beam of light drifts across the frame (ambient, slow,
 *     barely-there — it suggests cinema light, never progress).
 *  3. The emblem settles into place (fade + rise, expo-out, no bounce).
 *  4. The wordmark assembles letter by letter with tightening tracking.
 *  5. The frame lifts a touch and dissolves into the app.
 *
 * Deliberately avoided cheap patterns: no spinners or orbit rings, no fake
 * progress bars, no elastic/bouncy overshoots, no rainbow gradients in motion,
 * no typewriter effects, no tagline clutter.
 */
private const val SPLASH_TOTAL_MS = 1700
private const val WORDMARK = "mpvium"

private val EnterEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val ExitEasing = CubicBezierEasing(0.36f, 0f, 0.2f, 1f)

private val SplashTop = Color(0xFF1A1433)
private val SplashMid = Color(0xFF0E0B1A)
private val SplashBottom = Color(0xFF05030A)
private val FocusViolet = Color(0xFF8A2BE2)

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
  LaunchedEffect(Unit) {
    clock.animateTo(1f, tween(SPLASH_TOTAL_MS, easing = LinearEasing))
    onFinished()
  }
  MpviumSplashFrame(progress = clock.value)
}

@Composable
private fun MpviumSplashFrame(progress: Float) {
  // ---- phase mapping (fractions of the master clock) ----
  val emblemP = EnterEasing.transform(phase(progress, 0f, 0.41f))
  val exitP = ExitEasing.transform(phase(progress, 0.82f, 1f))

  val emblemAlpha = emblemP
  val emblemScale = lerp(0.95f, 1f, emblemP)
  val emblemRiseDp = lerp(8f, 0f, emblemP)

  // One slow breath of the backlight: swells, then settles.
  val breath = sin(PI.toFloat() * phase(progress, 0f, 0.85f))
  val glowAlpha = 0.15f + 0.09f * breath

  val frameAlpha = 1f - exitP
  val frameScale = 1f + 0.02f * exitP
  val trackingEm = lerp(0.18f, 0.05f, EnterEasing.transform(phase(progress, 0.32f, 0.68f)))

  Box(
    modifier = Modifier
      .fillMaxSize()
      .graphicsLayer {
        alpha = frameAlpha
        scaleX = frameScale
        scaleY = frameScale
      }
      .background(Brush.verticalGradient(listOf(SplashTop, SplashMid, SplashBottom))),
    contentAlignment = Alignment.Center,
  ) {
    // Ambient light: breathing backlight + one slow diagonal beam drift.
    Canvas(modifier = Modifier.fillMaxSize()) {
      val glowRadius = size.minDimension * 0.40f
      drawCircle(
        brush = Brush.radialGradient(
          0f to FocusViolet.copy(alpha = glowAlpha),
          0.7f to FocusViolet.copy(alpha = glowAlpha * 0.25f),
          1f to Color.Transparent,
          center = center,
          radius = glowRadius,
        ),
        radius = glowRadius,
        center = center,
      )
      // Barely-there cinema beam, drifting left to right over the full run.
      rotate(degrees = 18f, pivot = center) {
        val beamWidth = size.width * 0.38f
        val left = lerp(-0.55f * size.width, 1.05f * size.width, progress)
        drawRect(
          brush = Brush.horizontalGradient(
            0f to Color.White.copy(alpha = 0f),
            0.5f to Color.White.copy(alpha = 0.055f),
            1f to Color.White.copy(alpha = 0f),
            startX = left,
            endX = left + beamWidth,
          ),
          topLeft = Offset(left, -size.height * 0.25f),
          size = Size(beamWidth, size.height * 1.5f),
        )
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxSize(),
    ) {
      Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = null,
        modifier = Modifier
          .size(128.dp)
          .offset(y = emblemRiseDp.dp)
          .scale(emblemScale)
          .alpha(emblemAlpha),
      )

      Spacer(modifier = Modifier.height(30.dp))

      // Wordmark assembles letter by letter; tracking tightens as it lands.
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        WORDMARK.forEachIndexed { index, char ->
          val letterStart = 0.32f + index * 0.04f
          val letterP = EnterEasing.transform(phase(progress, letterStart, letterStart + 0.22f))
          Text(
            text = char.toString(),
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 42.sp,
              letterSpacing = (trackingEm * 42f).sp,
            ),
            color = Color.White.copy(alpha = 0.93f),
            modifier = Modifier
              .offset(y = lerp(12f, 0f, letterP).dp)
              .alpha(letterP),
          )
        }
      }
    }
  }
}

private fun phase(progress: Float, start: Float, end: Float): Float =
  ((progress - start) / (end - start)).coerceIn(0f, 1f)

private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

@Preview(showBackground = true, backgroundColor = 0xFF0E0B1A)
@Composable
private fun MpviumSplashPreview() {
  MaterialTheme {
    MpviumSplashFrame(progress = 0.55f)
  }
}
