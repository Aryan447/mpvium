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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import kotlinx.coroutines.launch

/**
 * Flagship Naked-Emblem launch gate for mpvium.
 *
 * True flagship design language (premium streaming style):
 * - Pure standalone naked emblem: zero text, zero slogans, zero clutter.
 * - Mathematically pristine, perfectly proportioned play glyph (68dp × 86dp).
 * - Pitch-black OLED canvas (#050508) with ultra-faint studio top-falloff.
 * - Fluid motion: gentle 0.92 settle, confident stillness, and a silky
 *   portal cross-dissolve into the app.
 */
private const val SPLASH_TOTAL_MS = 1200

// Signature Apple / X fluid easing curves
private val EnterEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val ExitEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private val SplashBlack = Color(0xFF050508)
private val SplashLift = Color(0xFF0C0A12)

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
  val enterP = EnterEasing.transform(phase(progress, 0.04f, 0.40f))
  val exitP = ExitEasing.transform(phase(progress, 0.76f, 1f))

  val frameAlpha = 1f - exitP
  val emblemScale = lerp(0.92f, 1f, enterP) * (1f + 0.12f * exitP)

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
    // Subtle studio softbox top-light
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(
        brush = Brush.verticalGradient(
          0f to Color.White.copy(alpha = 0.035f * enterP),
          0.40f to Color.Transparent,
        ),
      )
    }

    // Hero Naked Emblem: perfectly centered, no text
    Image(
      painter = painterResource(R.drawable.ic_mpvium_mark),
      contentDescription = null,
      modifier = Modifier
        .size(width = 68.dp, height = 86.dp)
        .scale(emblemScale)
        .graphicsLayer { alpha = enterP },
    )
  }
}

private fun phase(progress: Float, start: Float, end: Float): Float =
  ((progress - start) / (end - start)).coerceIn(0f, 1f)

private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

@Preview(name = "Splash Emblem Preview", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun MpviumSplashPreview() {
  MaterialTheme {
    MpviumSplashFrame(progress = 0.55f)
  }
}
