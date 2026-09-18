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
 * Flagship branded launch gate for mpvium.
 *
 * True flagship design language (Apple · Netflix · YouTube · X):
 * - Supreme visual restraint: zero amateur scanlines, zero rotating sheen tricks,
 *   zero taglines. Stillness and precision represent true quality.
 * - Normalized hero mark: perfectly proportioned vector play glyph with natural
 *   streaming gradient, built-in soft shadow, and specular curve.
 * - OLED darkroom atmosphere: obsidian canvas with an ultra-soft, monochromatic
 *   brand violet aura diffusing into cinema black.
 * - Unified lockup: mark and editorial wordmark emerge together as one cohesive
 *   piece with fluid Apple-grade expo-out settling.
 * - Fast, respectful choreography: 1350ms total timeline with a beat of stillness
 *   and a silky pure cross-dissolve into the app.
 */
private const val SPLASH_TOTAL_MS = 1350

// Signature easing curves
private val EnterEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val ExitEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private val SplashBlack = Color(0xFF050508)
private val SplashLift = Color(0xFF0B0912)
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
    if (!isSkipping && clock.value < 0.78f) {
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
  // Master choreography
  val lightP = EnterEasing.transform(phase(progress, 0f, 0.45f))
  val lockupP = EnterEasing.transform(phase(progress, 0.04f, 0.42f))
  val auraP = EnterEasing.transform(phase(progress, 0.02f, 0.50f))
  val exitP = ExitEasing.transform(phase(progress, 0.78f, 1f))

  // Motion transforms
  val lockupScale = lerp(0.94f, 1f, lockupP)
  val lockupRiseDp = lerp(8f, 0f, lockupP)
  val auraAlpha = 0.09f * auraP

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
      .graphicsLayer { alpha = 1f - exitP }
      .background(Brush.verticalGradient(listOf(SplashLift, SplashBlack))),
    contentAlignment = Alignment.Center,
  ) {
    // Cinematic background atmosphere
    Canvas(modifier = Modifier.fillMaxSize()) {
      // 1. Studio softbox top-light (subtle depth)
      drawRect(
        brush = Brush.verticalGradient(
          0f to Color.White.copy(alpha = 0.04f * lightP),
          0.40f to Color.Transparent,
        ),
      )

      // 2. Monochromatic brand violet aura (Netflix theater stage)
      val glowRadius = size.minDimension * 0.40f
      drawCircle(
        brush = Brush.radialGradient(
          0.0f to AuraViolet.copy(alpha = auraAlpha),
          0.60f to AuraViolet.copy(alpha = auraAlpha * 0.35f),
          1.0f to Color.Transparent,
          center = center,
          radius = glowRadius,
        ),
        radius = glowRadius,
        center = center,
      )

      // 3. Cinema vignette: dark corners for contrast
      drawRect(
        brush = Brush.radialGradient(
          0f to Color.Transparent,
          0.60f to Color.Transparent,
          1f to Color.Black.copy(alpha = 0.40f),
          center = center,
          radius = size.maxDimension * 0.65f,
        ),
      )
    }

    // Unified brand lockup
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxSize()
        .offset(y = lockupRiseDp.dp)
        .scale(lockupScale)
        .alpha(lockupP),
    ) {
      Image(
        painter = painterResource(R.drawable.ic_mpvium_mark),
        contentDescription = null,
        modifier = Modifier.size(width = 76.dp, height = 94.dp),
      )

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "mpvium",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.SemiBold,
          fontSize = 28.sp,
          letterSpacing = 1.4.sp,
        ),
        color = Color(0xFFF7F7FA),
      )
    }
  }
}

private fun phase(progress: Float, start: Float, end: Float): Float =
  ((progress - start) / (end - start)).coerceIn(0f, 1f)

private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

@Preview(name = "Splash Preview", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun MpviumSplashPreview() {
  MaterialTheme {
    MpviumSplashFrame(progress = 0.60f)
  }
}
