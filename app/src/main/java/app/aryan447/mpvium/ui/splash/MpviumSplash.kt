package app.aryan447.mpvium.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Branded launch gate.
 *
 * The cold-start system splash (Theme.mpvium.Starting) is intentionally static;
 * all motion lives here so cold start stays fast and the choreography is owned
 * by a single deterministic clock. Content composes underneath the opaque
 * overlay so first-frame work starts while the brand moment plays.
 *
 * Design — "focus pull" (original to mpvium, built around the play-mark):
 *  1. Emblem resolves into focus (settle, no bounce, no spin).
 *  2. A single orbit of light sweeps the mark once, like a lens finding focus.
 *     It never loops, so it never reads as a spinner / fake loader.
 *  3. The wordmark assembles letter by letter with a tightening tracking.
 *  4. A hairline rule draws, then the philosophy line fades in.
 *  5. The whole frame lifts slightly and dissolves into the app.
 *
 * Deliberately avoided cheap patterns: no looping spinners, no fake progress
 * bars, no elastic/bouncy overshoots, no rainbow gradients in motion, no
 * typewriter effects.
 */
private const val SPLASH_TOTAL_MS = 1900
private const val WORDMARK = "mpvium"

private val EnterEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val ExitEasing = CubicBezierEasing(0.36f, 0f, 0.2f, 1f)

private val SplashTop = Color(0xFF171130)
private val SplashMid = Color(0xFF0E0B1A)
private val SplashBottom = Color(0xFF05030A)
private val FocusViolet = Color(0xFF8A2BE2)
private val OrbitTip = Color(0xFFC9B0FF)

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
  val emblemP = EnterEasing.transform(phase(progress, 0f, 0.34f))
  val orbitP = FastOutSlowInEasing.transform(phase(progress, 0.18f, 0.60f))
  val ruleP = EnterEasing.transform(phase(progress, 0.45f, 0.60f))
  val taglineP = EnterEasing.transform(phase(progress, 0.55f, 0.71f))
  val exitP = ExitEasing.transform(phase(progress, 0.84f, 1f))

  val emblemAlpha = emblemP
  val emblemScale = lerp(0.94f, 1f, emblemP)
  val emblemRiseDp = lerp(10f, 0f, emblemP)

  val orbitRotation = lerp(-90f, 270f, orbitP)
  val orbitAlpha =
    phase(progress, 0.18f, 0.26f) * (1f - phase(progress, 0.60f, 0.72f))

  val frameAlpha = 1f - exitP
  val frameScale = 1f + 0.03f * exitP
  val trackingEm = lerp(0.22f, 0.06f, EnterEasing.transform(phase(progress, 0.29f, 0.62f)))

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
    // Ambient violet backlight that breathes with the orbit sweep.
    Canvas(modifier = Modifier.fillMaxSize()) {
      val glowRadius = size.minDimension * 0.42f
      drawCircle(
        brush = Brush.radialGradient(
          0f to FocusViolet.copy(alpha = 0.20f + 0.06f * orbitP),
          0.7f to FocusViolet.copy(alpha = 0.05f),
          1f to Color.Transparent,
          center = center,
          radius = glowRadius,
        ),
        radius = glowRadius,
        center = center,
      )
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxSize(),
    ) {
      // Emblem + single orbit sweep.
      Box(contentAlignment = Alignment.Center, modifier = Modifier.size(188.dp)) {
        Canvas(
          modifier = Modifier
            .size(188.dp)
            .alpha(orbitAlpha),
        ) {
          val ringRadius = size.minDimension / 2f - 8f
          // Faint full track so the sweep has a path to travel.
          drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = ringRadius,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
          )
          // The travelling light: one 300° sweep, exactly once.
          drawArc(
            brush = Brush.sweepGradient(
              0f to Color.Transparent,
              0.55f to Color.White.copy(alpha = 0.25f),
              0.82f to Color.White.copy(alpha = 0.9f),
              0.92f to OrbitTip,
              1f to Color.Transparent,
            ),
            startAngle = orbitRotation - 300f,
            sweepAngle = 300f,
            useCenter = false,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
          )
          // Hot tip of the light.
          val tipAngleRad = Math.toRadians(orbitRotation.toDouble())
          val tip = center + androidx.compose.ui.geometry.Offset(
            (cos(tipAngleRad) * ringRadius).toFloat(),
            (sin(tipAngleRad) * ringRadius).toFloat(),
          )
          drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = 7.dp.toPx(),
            center = tip,
          )
          drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = tip)
        }
        Image(
          painter = painterResource(R.drawable.ic_launcher_foreground),
          contentDescription = null,
          modifier = Modifier
            .size(124.dp)
            .offset(y = emblemRiseDp.dp)
            .scale(emblemScale)
            .alpha(emblemAlpha),
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Wordmark assembles letter by letter; tracking tightens as it lands.
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        WORDMARK.forEachIndexed { index, char ->
          val letterStart = 0.29f + index * 0.035f
          val letterP = EnterEasing.transform(phase(progress, letterStart, letterStart + 0.20f))
          Text(
            text = char.toString(),
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 40.sp,
              letterSpacing = (trackingEm * 40f).sp,
            ),
            color = Color.White.copy(alpha = 0.92f),
            modifier = Modifier
              .offset(y = lerp(14f, 0f, letterP).dp)
              .alpha(letterP),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Hairline rule draws outward.
      Box(
        modifier = Modifier
          .width(52.dp)
          .height(1.dp)
          .scale(scaleX = ruleP, scaleY = 1f)
          .alpha(ruleP)
          .background(Color.White.copy(alpha = 0.22f)),
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "SANE DEFAULTS · PRO CONTROL",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp,
          letterSpacing = 2.sp,
        ),
        color = Color.White.copy(alpha = 0.55f),
        modifier = Modifier.alpha(taglineP),
      )
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
    MpviumSplashFrame(progress = 0.62f)
  }
}
