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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.R
import kotlinx.coroutines.launch

/**
 * Flagship launch gate for mpvium.
 *
 * True flagship design language (Apple · Google · Netflix · YouTube · X):
 * - Supreme visual restraint: zero cartoon sunburst rays, zero cheap bloom,
 *   zero distortion.
 * - Perfectly proportioned hero mark (58dp × 72dp) with balanced editorial spacing.
 * - Pristine OLED canvas (#060609) with an ultra-faint, luxury studio top-falloff.
 * - Crisp, understated "mpvium" typography with refined letter tracking.
 * - Fast, respectful 1200ms timeline with a subtle 0.96 settle and a silk cross-dissolve.
 */
private const val SPLASH_TOTAL_MS = 1200

// Signature Apple / Google fluid easing curves
private val EnterEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private val ExitEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private val SplashBlack = Color(0xFF060609)
private val SplashLift = Color(0xFF0C0A12)
private val AuraViolet = Color(0xFF8A2BE2)

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
    if (!isSkipping && clock.value < 0.76f) {
      isSkipping = true
      scope.launch {
        clock.animateTo(1f, tween(180, easing = ExitEasing))
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
  val enterP = EnterEasing.transform(phase(progress, 0.04f, 0.38f))
  val exitP = ExitEasing.transform(phase(progress, 0.76f, 1f))

  val frameAlpha = 1f - exitP
  val lockupScale = lerp(0.96f, 1f, enterP)

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
      .graphicsLayer { alpha = frameAlpha }
      .background(Brush.verticalGradient(listOf(SplashLift, SplashBlack))),
    contentAlignment = Alignment.Center,
  ) {
    // Subtle, high-end studio atmosphere (zero cartoon bloom)
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f - 20.dp.toPx()

      // 1. Studio softbox top falloff (Apple style)
      drawRect(
        brush = Brush.verticalGradient(
          0f to Color.White.copy(alpha = 0.035f * enterP),
          0.40f to Color.Transparent,
        ),
      )

      // 2. Ultra-faint ambient brand violet haze (barely perceptible OLED depth)
      val glowRadius = 110.dp.toPx()
      drawCircle(
        brush = Brush.radialGradient(
          0.0f to AuraViolet.copy(alpha = 0.06f * enterP),
          1.0f to Color.Transparent,
          center = Offset(cx, cy),
          radius = glowRadius,
        ),
        radius = glowRadius,
        center = Offset(cx, cy),
      )
    }

    // Unified brand lockup: perfectly proportioned emblem + generous spacing + clean wordmark
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxSize()
        .offset(y = (-20).dp)
        .scale(lockupScale)
        .graphicsLayer { alpha = enterP },
    ) {
      Image(
        painter = painterResource(R.drawable.ic_mpvium_mark),
        contentDescription = null,
        modifier = Modifier.size(width = 58.dp, height = 72.dp),
      )

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "mpvium",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Medium,
          fontSize = 18.sp,
          letterSpacing = 1.4.sp,
        ),
        color = Color.White.copy(alpha = 0.90f),
      )
    }
  }
}

private fun phase(progress: Float, start: Float, end: Float): Float =
  ((progress - start) / (end - start)).coerceIn(0f, 1f)

private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

@Preview(name = "Splash Settle Preview", showBackground = true, backgroundColor = 0xFF060609)
@Composable
private fun MpviumSplashPreview() {
  MaterialTheme {
    MpviumSplashFrame(progress = 0.50f)
  }
}
