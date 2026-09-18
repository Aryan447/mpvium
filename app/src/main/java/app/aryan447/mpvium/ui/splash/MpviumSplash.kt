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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.R
import kotlinx.coroutines.launch

/**
 * World-class branded launch gate for mpvium.
 *
 * Drawing inspiration from Apple, Google, Netflix, YouTube, and X:
 * - **Apple**: Atmospheric studio top-lighting, ultra-smooth cubic-bezier spring
 *   optics, a subtle diagonal specular sheen gliding across the play emblem, and
 *   razor-sharp micro-typography.
 * - **Netflix**: Deepest OLED darkroom canvas with an expanding chromatic aura
 *   (crimson, electric violet, cyan) breathing behind the hero mark like a theater
 *   projector warming to life.
 * - **YouTube**: Confident, unmistakable play emblem presence with crisp contrast
 *   and responsive execution (~1900ms duration).
 * - **X**: Scale-through portal dissolve on exit — smoothly expanding into the
 *   frame to seamlessly unveil the app underneath.
 * - **Google (Material 3)**: Fluid physics, organic settle, edge-to-edge immersion,
 *   and instant tap-to-skip responsiveness.
 */
private const val SPLASH_TOTAL_MS = 1900

// Signature easing physics
private val EnterEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val ExitEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val SheenEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

private val SplashBlack = Color(0xFF060609)
private val SplashLift = Color(0xFF0D0B14)

// Brand chromatic streaming aura
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
    if (!isSkipping && clock.value < 0.86f) {
      isSkipping = true
      scope.launch {
        clock.animateTo(1f, tween(240, easing = ExitEasing))
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
  // ---- Phase choreography ----
  val lightP = EnterEasing.transform(phase(progress, 0f, 0.48f))
  val emblemP = EnterEasing.transform(phase(progress, 0.04f, 0.42f))
  val auraP = EnterEasing.transform(phase(progress, 0.06f, 0.65f))
  val wordmarkP = EnterEasing.transform(phase(progress, 0.26f, 0.58f))
  val taglineP = EnterEasing.transform(phase(progress, 0.44f, 0.72f))
  val exitP = ExitEasing.transform(phase(progress, 0.86f, 1f))

  // Sheen light glint across the emblem
  val sheenP = SheenEasing.transform(phase(progress, 0.32f, 0.66f))
  val sheenAlpha = (phase(progress, 0.32f, 0.40f) * (1f - phase(progress, 0.58f, 0.66f))).coerceIn(0f, 1f)

  // Emblem transform
  val emblemAlpha = emblemP
  val emblemScale = lerp(0.88f, 1f, emblemP)
  val emblemRiseDp = lerp(12f, 0f, emblemP)

  // Ambient chromatic aura breathing
  val glowAlpha = 0.10f + 0.12f * auraP

  // Wordmark dynamic tracking & rise
  val trackingEm = lerp(0.18f, 0.06f, wordmarkP)
  val wordmarkRiseDp = lerp(10f, 0f, wordmarkP)
  val taglineRiseDp = lerp(8f, 0f, taglineP)

  // Portal exit: scale-through dissolve (X / Netflix style)
  val frameAlpha = 1f - exitP
  val frameScale = 1f + 0.08f * exitP

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
        scaleX = frameScale
        scaleY = frameScale
      }
      .background(Brush.verticalGradient(listOf(SplashLift, SplashBlack))),
    contentAlignment = Alignment.Center,
  ) {
    // Canvas: Multi-layered cinematic backlighting
    Canvas(modifier = Modifier.fillMaxSize()) {
      // 1. Studio softbox top-light warming on (Apple aesthetic)
      drawRect(
        brush = Brush.verticalGradient(
          0f to Color.White.copy(alpha = 0.06f * lightP),
          0.45f to Color.Transparent,
        ),
      )

      // 2. Cinematic chromatic aura bloom behind the emblem (Netflix theatrical aesthetic)
      val glowRadius = size.minDimension * lerp(0.30f, 0.48f, auraP)
      drawCircle(
        brush = Brush.radialGradient(
          0.0f to FocusViolet.copy(alpha = glowAlpha),
          0.38f to RubyCrimson.copy(alpha = glowAlpha * 0.55f),
          0.72f to CyanElectric.copy(alpha = glowAlpha * 0.22f),
          1.0f to Color.Transparent,
          center = center,
          radius = glowRadius,
        ),
        radius = glowRadius,
        center = center,
      )

      // 3. Focal core radiance (Google Material organic glow)
      val coreRadius = size.minDimension * 0.22f
      drawCircle(
        brush = Brush.radialGradient(
          0.0f to Color.White.copy(alpha = glowAlpha * 0.35f),
          0.60f to FocusViolet.copy(alpha = glowAlpha * 0.18f),
          1.0f to Color.Transparent,
          center = center,
          radius = coreRadius,
        ),
        radius = coreRadius,
        center = center,
      )

      // 4. Cinema vignette: dark framing corners for theater contrast
      drawRect(
        brush = Brush.radialGradient(
          0f to Color.Transparent,
          0.58f to Color.Transparent,
          1f to Color.Black.copy(alpha = 0.42f),
          center = center,
          radius = size.maxDimension * 0.65f,
        ),
      )
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxSize(),
    ) {
      // Hero Emblem with Apple-style specular light sheen
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(124.dp)
          .offset(y = emblemRiseDp.dp)
          .scale(emblemScale)
          .alpha(emblemAlpha)
          .clipToBounds(),
      ) {
        Image(
          painter = painterResource(R.drawable.ic_launcher_foreground),
          contentDescription = null,
          modifier = Modifier.fillMaxSize(),
        )

        // Specular glint sweeping across the glyph
        if (sheenAlpha > 0.001f) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val sheenProgressOffset = lerp(-size.width * 0.7f, size.width * 1.7f, sheenP)
            rotate(degrees = -25f, pivot = center) {
              drawRect(
                brush = Brush.horizontalGradient(
                  0f to Color.Transparent,
                  0.35f to Color.White.copy(alpha = 0.08f * sheenAlpha),
                  0.50f to Color.White.copy(alpha = 0.38f * sheenAlpha),
                  0.65f to Color.White.copy(alpha = 0.08f * sheenAlpha),
                  1f to Color.Transparent,
                  startX = sheenProgressOffset - size.width * 0.35f,
                  endX = sheenProgressOffset + size.width * 0.35f,
                ),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Wordmark: Bold pure-white "mpv" + frosted lavender "ium"
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .offset(y = wordmarkRiseDp.dp)
          .alpha(wordmarkP),
      ) {
        Text(
          text = buildAnnotatedString {
            withStyle(
              SpanStyle(
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
            ) {
              append("mpv")
            }
            withStyle(
              SpanStyle(
                color = Color(0xFFDDD0FC),
                fontWeight = FontWeight.Light,
              )
            ) {
              append("ium")
            }
          },
          style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 38.sp,
            letterSpacing = (trackingEm * 38f).sp,
          ),
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Micro-badge: tracked pro-level subtitle
      Text(
        text = "PRO MEDIA PLAYER",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.SemiBold,
          fontSize = 10.5.sp,
          letterSpacing = 3.6.sp,
        ),
        color = Color(0xFFAAA0BF).copy(alpha = 0.75f * taglineP),
        modifier = Modifier
          .offset(y = taglineRiseDp.dp)
          .alpha(taglineP),
      )
    }
  }
}

private fun phase(progress: Float, start: Float, end: Float): Float =
  ((progress - start) / (end - start)).coerceIn(0f, 1f)

private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

@Preview(name = "Splash Entrance", showBackground = true, backgroundColor = 0xFF060609)
@Composable
private fun MpviumSplashPreviewEntrance() {
  MaterialTheme {
    MpviumSplashFrame(progress = 0.25f)
  }
}

@Preview(name = "Splash Settle & Sheen", showBackground = true, backgroundColor = 0xFF060609)
@Composable
private fun MpviumSplashPreviewSettle() {
  MaterialTheme {
    MpviumSplashFrame(progress = 0.52f)
  }
}

@Preview(name = "Splash Portal Exit", showBackground = true, backgroundColor = 0xFF060609)
@Composable
private fun MpviumSplashPreviewExit() {
  MaterialTheme {
    MpviumSplashFrame(progress = 0.94f)
  }
}
