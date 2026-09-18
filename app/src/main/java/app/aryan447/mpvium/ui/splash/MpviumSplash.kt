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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.R

/**
 * Branded launch gate.
 *
 * The cold-start system splash (Theme.mpvium.Starting) is intentionally static;
 * all motion lives here so cold start stays fast and the choreography is owned
 * by a single deterministic clock. Content composes underneath the opaque
 * overlay so first-frame work starts while the brand moment plays.
 *
 * Design — flagship restraint (the trillion-dollar rule: stillness is luxury):
 *  1. Near-black frame. A studio top-light fades on, like a softbox warming up.
 *  2. The mark emerges from darkness — slow fade, 0.965 settle, expo-out.
 *     Nothing pops, nothing bounces, nothing rotates.
 *  3. The wordmark sets itself in editorial type: wide tracking that tightens
 *     as each letter rises into place, unhurried.
 *  4. A beat of absolute stillness. Confidence, not decoration.
 *  5. A slow pure cross-dissolve into the app. No scale, no slide — dissolves
 *     are how flagship launches hand off.
 *
 * Deliberately absent: spinners, orbit rings, progress bars, beams, sheens,
 * taglines, version strings, bottom branding. Luxury is what you leave out.
 */
private const val SPLASH_TOTAL_MS = 2200
private const val WORDMARK = "mpvium"

// Signature expo-out: fast resolve, endless settle. Slow = expensive.
private val EnterEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val ExitEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private val SplashBlack = Color(0xFF060609)
private val SplashLift = Color(0xFF0C0C12)
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
  val lightP = EnterEasing.transform(phase(progress, 0f, 0.5f))
  val emblemP = EnterEasing.transform(phase(progress, 0.03f, 0.42f))
  val exitP = ExitEasing.transform(phase(progress, 0.86f, 1f))

  val emblemAlpha = emblemP
  val emblemScale = lerp(0.965f, 1f, emblemP)
  val emblemRiseDp = lerp(10f, 0f, emblemP)

  // Backlight swells once, then holds perfectly still.
  val glowAlpha = 0.08f + 0.07f * EnterEasing.transform(phase(progress, 0f, 0.6f))

  val trackingEm = lerp(0.22f, 0.07f, EnterEasing.transform(phase(progress, 0.30f, 0.70f)))

  Box(
    modifier = Modifier
      .fillMaxSize()
      .graphicsLayer { alpha = 1f - exitP }
      .background(Brush.verticalGradient(listOf(SplashLift, SplashBlack))),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Studio top-light warming on.
      drawRect(
        brush = Brush.verticalGradient(
          0f to Color.White.copy(alpha = 0.05f * lightP),
          0.45f to Color.Transparent,
        ),
      )
      // Faint violet aura behind the mark.
      val glowRadius = size.minDimension * 0.36f
      drawCircle(
        brush = Brush.radialGradient(
          0f to FocusViolet.copy(alpha = glowAlpha),
          0.65f to FocusViolet.copy(alpha = glowAlpha * 0.3f),
          1f to Color.Transparent,
          center = center,
          radius = glowRadius,
        ),
        radius = glowRadius,
        center = center,
      )
      // Gentle vignette: transparent heart, darkened corners. Cinema depth.
      drawRect(
        brush = Brush.radialGradient(
          0f to Color.Transparent,
          0.62f to Color.Transparent,
          1f to Color.Black.copy(alpha = 0.38f),
          center = center,
          radius = size.maxDimension * 0.62f,
        ),
      )
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
          .size(120.dp)
          .offset(y = emblemRiseDp.dp)
          .scale(emblemScale)
          .alpha(emblemAlpha),
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Editorial wordmark: wide-set type tightening as letters land.
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.alpha(emblemP),
      ) {
        WORDMARK.forEachIndexed { index, char ->
          val letterStart = 0.30f + index * 0.04f
          val letterP = EnterEasing.transform(phase(progress, letterStart, letterStart + 0.22f))
          Text(
            text = char.toString(),
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 36.sp,
              letterSpacing = (trackingEm * 36f).sp,
            ),
            color = Color.White.copy(alpha = 0.92f),
            modifier = Modifier
              .offset(y = lerp(10f, 0f, letterP).dp)
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

@Preview(showBackground = true, backgroundColor = 0xFF060609)
@Composable
private fun MpviumSplashPreview() {
  MaterialTheme {
    MpviumSplashFrame(progress = 0.7f)
  }
}
