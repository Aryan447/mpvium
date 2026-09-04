package app.aryan447.mpvium.ui.player.controls.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import app.aryan447.mpvium.ui.player.controls.LocalPlayerButtonsClickEvent
import app.aryan447.mpvium.ui.theme.spacing
import app.aryan447.mpvium.preferences.SeekbarStyle
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Vertical drag distance (dp) per scrub-rate tier while seekbar-scrubbing.
 * Dragging up through tiers selects 1x, 2x, 4x, 8x horizontal scrub speed.
 */
private const val SCRUB_RATE_TIER_STEP_DP = 48f
private const val MAX_SCRUB_TIER = 3

private fun scrubRateForTier(tier: Int): Int = 1 shl tier.coerceIn(0, MAX_SCRUB_TIER)

/**
 * Pill showing the active scrub-rate multiplier, positioned above the thumb.
 * A [BoxScope] extension so the scoped [AnimatedVisibility] overload and
 * [BoxScope.align] resolve against the seekbar container unambiguously.
 */
@Composable
private fun BoxScope.ScrubRatePopup(
  visible: Boolean,
  rateText: String,
  positionFraction: Float,
  trackWidthPx: Int,
) {
  var popupWidthPx by remember { mutableIntStateOf(0) }
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + scaleIn(),
    exit = fadeOut() + scaleOut(),
    modifier = Modifier
      .align(Alignment.TopCenter)
      .offset {
        // Travel range keeps the pill inside the track bounds.
        val travelPx = (trackWidthPx - popupWidthPx).coerceAtLeast(0)
        IntOffset(x = ((positionFraction - 0.5f) * travelPx).roundToInt(), y = 0)
      },
  ) {
    Box(
      modifier = Modifier
        .background(
          MaterialTheme.colorScheme.inverseSurface,
          RoundedCornerShape(8.dp),
        )
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .onSizeChanged { popupWidthPx = it.width },
    ) {
      Text(
        text = rateText,
        color = MaterialTheme.colorScheme.inverseOnSurface,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
      )
    }
  }
}

/**
 * Maps a horizontal touch coordinate to a seek position in seconds.
 *
 * Evaluated strictly within the track's drawn bounds: [0, trackWidthPx].
 * Uses Double precision for the pixel-to-time ratio so wide tracks (thousands
 * of px) don't lose sub-second precision, and clamps the coordinate itself —
 * not just the result — so overshoot, insets and density scaling can't skew
 * the fraction. Returns 0 when the track hasn't been measured yet.
 */
private fun xToSeekPosition(x: Float, trackWidthPx: Int, duration: Float): Float {
  if (trackWidthPx <= 0 || duration <= 0f) return 0f
  val clampedX = x.coerceIn(0f, trackWidthPx.toFloat()).toDouble()
  val fraction = clampedX / trackWidthPx.toDouble()
  return (fraction * duration.toDouble()).toFloat().coerceIn(0f, duration)
}

@Composable
fun SeekbarWithTimers(
  position: Float,
  duration: Float,
  onValueChange: (Float) -> Unit,
  onValueChangeFinished: (Float) -> Unit,
  timersInverted: Pair<Boolean, Boolean>,
  positionTimerOnClick: () -> Unit,
  durationTimerOnCLick: () -> Unit,
  chapters: ImmutableList<Segment>,
  paused: Boolean,
  seekbarStyle: SeekbarStyle = SeekbarStyle.Wavy,
  loopStart: Float? = null,
  loopEnd: Float? = null,
  modifier: Modifier = Modifier,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  var isUserInteracting by remember { mutableStateOf(false) }
  var userPosition by remember { mutableFloatStateOf(position) }

  // Variable scrub rate: dragging upward while scrubbing steps through
  // 1x/2x/4x/8x tiers. Horizontal deltas are scaled by the active rate and
  // accumulated relative to the grab anchor (not re-mapped absolutely), so a
  // mid-drag rate change never teleports the preview.
  var scrubTier by remember { mutableIntStateOf(0) }
  var dragAnchor by remember { mutableFloatStateOf(0f) }
  var scaledDragDxPx by remember { mutableFloatStateOf(0f) }
  var verticalDragPx by remember { mutableFloatStateOf(0f) }
  val scrubRate = scrubRateForTier(scrubTier)

  // Measured track width for the thumb-following rate popup.
  var trackWidthPx by remember { mutableIntStateOf(0) }
  val density = LocalDensity.current
  val scrubTierStepPx = with(density) { SCRUB_RATE_TIER_STEP_DP.dp.toPx() }

  // Animated position for smooth transitions
  val animatedPosition = remember { Animatable(position) }
  val scope = rememberCoroutineScope()

  // Only animate position updates when user is not interacting
  LaunchedEffect(position, isUserInteracting) {
    if (!isUserInteracting && position != animatedPosition.value) {
      scope.launch {
        animatedPosition.animateTo(
          targetValue = position,
          animationSpec =
            tween(
              durationMillis = 200,
              easing = LinearEasing,
            ),
        )
      }
    }
  }

  Row(
    modifier = modifier.height(48.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
  ) {
    VideoTimer(
      value = if (isUserInteracting) userPosition else position,
      timersInverted.first,
      onClick = {
        clickEvent()
        positionTimerOnClick()
      },
      modifier = Modifier.width(92.dp),
    )

    // Seekbar with expanded touch area
    Box(
      modifier =
        Modifier
          .weight(1f)
          .height(48.dp)
          .padding(vertical = 8.dp) // Add vertical padding for larger touch area
          .onSizeChanged { trackWidthPx = it.width },
      contentAlignment = Alignment.Center,
    ) {
      // Visual seekbar (smaller, centered) — declared first so the touch
      // overlay below stays on top and remains the single gesture handler.
      // This keeps touch coordinates mapped against one set of track bounds
      // (no thumb-inset mismatch vs the inner Slider) and prevents the
      // release value from being clobbered by a second handler.
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(32.dp),
        contentAlignment = Alignment.Center,
      ) {
      when (seekbarStyle) {
        SeekbarStyle.Standard -> {
          StandardSeekbar(
            position = if (isUserInteracting) userPosition else animatedPosition.value,
            duration = duration,
            chapters = chapters,
            isPaused = paused,
            isScrubbing = isUserInteracting,
            seekbarStyle = SeekbarStyle.Standard,
            onSeek = { newPosition ->
              if (!isUserInteracting) isUserInteracting = true
              userPosition = newPosition
              onValueChange(newPosition)
            },
            onSeekFinished = {
              scope.launch { animatedPosition.snapTo(userPosition) }
              isUserInteracting = false
              onValueChangeFinished(userPosition)
            },
            loopStart = loopStart,
            loopEnd = loopEnd,
          )
        }
        SeekbarStyle.Wavy -> {
          SquigglySeekbar(
            position = if (isUserInteracting) userPosition else animatedPosition.value,
            duration = duration,
            chapters = chapters,
            isPaused = paused,
            isScrubbing = isUserInteracting,
            useWavySeekbar = true,
            seekbarStyle = SeekbarStyle.Wavy,
            onSeek = { }, // Touch handled by parent
            onSeekFinished = { }, // Touch handled by parent
            loopStart = loopStart,
            loopEnd = loopEnd,
          )
        }
        SeekbarStyle.Thick -> {
          StandardSeekbar(
            position = if (isUserInteracting) userPosition else animatedPosition.value,
            duration = duration,
            chapters = chapters,
            isPaused = paused,
            isScrubbing = isUserInteracting,
            seekbarStyle = SeekbarStyle.Thick,
            onSeek = { newPosition ->
              if (!isUserInteracting) isUserInteracting = true
              userPosition = newPosition
              onValueChange(newPosition)
            },
            onSeekFinished = {
              scope.launch { animatedPosition.snapTo(userPosition) }
              isUserInteracting = false
              onValueChangeFinished(userPosition)
            },
            loopStart = loopStart,
            loopEnd = loopEnd,
          )
        }
      }
      }

      // Transparent touch overlay, drawn last so it is hit-tested first and
      // remains the single gesture handler for every seekbar style.
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp) // Larger touch area
          .pointerInput(duration) {
            detectTapGestures(
              onTap = { offset ->
                if (!isUserInteracting) isUserInteracting = true
                scrubTier = 0
                userPosition = xToSeekPosition(offset.x, size.width, duration)
                onValueChange(userPosition)
                scope.launch {
                  // Snap to user position immediately to prevent jumping
                  animatedPosition.snapTo(userPosition)
                  isUserInteracting = false
                  onValueChangeFinished(userPosition)
                }
              }
            )
          }
          .pointerInput(duration, scrubTierStepPx) {
            detectDragGestures(
              onDragStart = { offset ->
                isUserInteracting = true
                // Anchor to the grab point immediately: the first onDrag
                // callback only fires after touch slop, otherwise the
                // preview would jump from the stale position on wide tracks.
                dragAnchor = xToSeekPosition(offset.x, size.width, duration)
                scaledDragDxPx = 0f
                verticalDragPx = 0f
                scrubTier = 0
                userPosition = dragAnchor
                onValueChange(userPosition)
              },
              onDragEnd = {
                // Release uses the last settled drag value — never re-sample
                // here, so lift-off micro-jitter can't clobber the target.
                scope.launch {
                  // Allow a tiny window for mpv/viewModel to sync back before releasing control
                  delay(50)
                  animatedPosition.snapTo(userPosition)
                  isUserInteracting = false
                  scrubTier = 0
                  onValueChangeFinished(userPosition)
                }
              },
              onDragCancel = {
                scope.launch {
                  delay(50)
                  animatedPosition.snapTo(userPosition)
                  isUserInteracting = false
                  scrubTier = 0
                  onValueChangeFinished(userPosition)
                }
              },
            ) { change, dragAmount ->
              change.consume()
              // Upward displacement selects the rate tier; dragging back down
              // lowers it again. Horizontal deltas are scaled by the active
              // rate and accumulated from the grab anchor.
              verticalDragPx += dragAmount.y
              val newTier =
                ((-verticalDragPx) / scrubTierStepPx).toInt()
                  .coerceIn(0, MAX_SCRUB_TIER)
              if (newTier != scrubTier) scrubTier = newTier
              if (size.width > 0 && duration > 0f) {
                scaledDragDxPx += dragAmount.x * scrubRateForTier(scrubTier)
                val anchorFraction = dragAnchor.toDouble() / duration.toDouble()
                val deltaFraction = scaledDragDxPx.toDouble() / size.width.toDouble()
                userPosition =
                  ((anchorFraction + deltaFraction) * duration.toDouble())
                    .toFloat().coerceIn(0f, duration)
                onValueChange(userPosition)
              }
            }
          }
      )

      // Scrub-rate popup: follows the thumb while a boosted rate is active.
      ScrubRatePopup(
        visible = isUserInteracting && scrubTier > 0,
        rateText = "${scrubRate}×",
        positionFraction =
          if (duration > 0f) (userPosition / duration).coerceIn(0f, 1f) else 0f,
        trackWidthPx = trackWidthPx,
      )
  }

    VideoTimer(
      value = if (timersInverted.second) position - duration else duration,
      isInverted = timersInverted.second,
      onClick = {
        clickEvent()
        durationTimerOnCLick()
      },
      modifier = Modifier.width(92.dp),
    )
  }
}

@Composable
private fun SquigglySeekbar(
  position: Float,
  duration: Float,
  chapters: ImmutableList<Segment>,
  isPaused: Boolean,
  isScrubbing: Boolean,
  useWavySeekbar: Boolean,
  seekbarStyle: SeekbarStyle,
  onSeek: (Float) -> Unit,
  onSeekFinished: () -> Unit,
  loopStart: Float? = null,
  loopEnd: Float? = null,
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

  // Manual Interaction State Tracking
  var isPressed by remember { mutableStateOf(false) }
  var isDragged by remember { mutableStateOf(false) }
  val isInteracting = isPressed || isDragged || isScrubbing

  // Animation state
  var phaseOffset by remember { mutableFloatStateOf(0f) }
  var heightFraction by remember { mutableFloatStateOf(1f) }

  val scope = rememberCoroutineScope()

  // Wave parameters
  val waveLength = 80f
  val lineAmplitude = if (useWavySeekbar) 6f else 0f
  val phaseSpeed = 10f // px per second
  val transitionPeriods = 1.5f
  val minWaveEndpoint = 0f
  val matchedWaveEndpoint = 1f
  val transitionEnabled = true

  // Animate height fraction based on paused state and scrubbing state
  LaunchedEffect(isPaused, isScrubbing, useWavySeekbar) {
    if (!useWavySeekbar) {
      heightFraction = 0f
      return@LaunchedEffect
    }

    scope.launch {
      val shouldFlatten = isPaused || isScrubbing
      val targetHeight = if (shouldFlatten) 0f else 1f
      val duration = if (shouldFlatten) 550 else 800
      val startDelay = if (shouldFlatten) 0L else 60L

      kotlinx.coroutines.delay(startDelay)

      val animator = Animatable(heightFraction)
      animator.animateTo(
        targetValue = targetHeight,
        animationSpec =
          tween(
            durationMillis = duration,
            easing = LinearEasing,
          ),
      ) {
        heightFraction = value
      }
    }
  }

  // Animate wave movement only when not paused
  LaunchedEffect(isPaused, useWavySeekbar) {
    if (isPaused || !useWavySeekbar) return@LaunchedEffect

    var lastFrameTime = withFrameMillis { it }
    while (isActive) {
      withFrameMillis { frameTimeMillis ->
        val deltaTime = (frameTimeMillis - lastFrameTime) / 1000f
        phaseOffset += deltaTime * phaseSpeed
        phaseOffset %= waveLength
        lastFrameTime = frameTimeMillis
      }
    }
  }

  // Thicken the stroke while scrubbing so the finger's target stays visible.
  val scrubThickness by animateFloatAsState(
    targetValue = if (isScrubbing) 1.8f else 1f,
    animationSpec = tween(durationMillis = 150, easing = LinearEasing),
    label = "scrubThickness",
  )

  Canvas(
    modifier =
      modifier
        .fillMaxWidth()
        .height(48.dp),
  ) {
    val strokeWidth = 5.dp.toPx() * scrubThickness
    val progress = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f
    val totalWidth = size.width
    val totalProgressPx = totalWidth * progress
    val centerY = size.height / 2f

    // Calculate wave progress
    val waveProgressPx =
      if (!transitionEnabled || progress > matchedWaveEndpoint) {
        totalWidth * progress
      } else {
        val t = (progress / matchedWaveEndpoint).coerceIn(0f, 1f)
        totalWidth * (minWaveEndpoint + (matchedWaveEndpoint - minWaveEndpoint) * t)
      }

    // Helper function to compute amplitude
    fun computeAmplitude(
      x: Float,
      sign: Float,
    ): Float =
      if (transitionEnabled) {
        val length = transitionPeriods * waveLength
        val coeff = ((waveProgressPx + length / 2f - x) / length).coerceIn(0f, 1f)
        sign * heightFraction * lineAmplitude * coeff
      } else {
        sign * heightFraction * lineAmplitude
      }

    // Build wavy path for played portion
    val path = Path()
    val waveStart = -phaseOffset - waveLength / 2f
    val waveEnd = if (transitionEnabled) totalWidth else waveProgressPx

    path.moveTo(waveStart, centerY)

    var currentX = waveStart
    var waveSign = 1f
    var currentAmp = computeAmplitude(currentX, waveSign)
    val dist = waveLength / 2f

    while (currentX < waveEnd) {
      waveSign = -waveSign
      val nextX = currentX + dist
      val midX = currentX + dist / 2f
      val nextAmp = computeAmplitude(nextX, waveSign)

      path.cubicTo(
        midX,
        centerY + currentAmp,
        midX,
        centerY + nextAmp,
        nextX,
        centerY + nextAmp,
      )

      currentAmp = nextAmp
      currentX = nextX
    }

    // Draw path up to progress position using clipping
    val clipTop = lineAmplitude + strokeWidth

    val chapterGapHalf = 1.dp.toPx()
    val chapterGaps = if (totalWidth > 0f && duration > 0f && chapters.isNotEmpty()) {
      chapters.mapNotNull { chapter ->
        val chapterStart = chapter.start
        if (chapterStart > 0f && chapterStart < duration) {
          val chapterPx = (chapterStart / duration) * totalWidth
          (chapterPx - chapterGapHalf) to (chapterPx + chapterGapHalf)
        } else null
      }
    } else emptyList()

    fun drawPathWithGaps(
      startX: Float,
      endX: Float,
      color: Color,
    ) {
      if (endX <= startX) return
      val relevantGaps = chapterGaps
        .filter { (gStart, gEnd) -> gEnd > startX && gStart < endX }
        .sortedBy { it.first }

      var currentPos = startX
      for ((gStart, gEnd) in relevantGaps) {
        val segmentEnd = gStart.coerceAtMost(endX)
        if (segmentEnd > currentPos) {
          clipRect(
            left = currentPos,
            top = centerY - clipTop,
            right = segmentEnd,
            bottom = centerY + clipTop,
          ) {
            drawPath(
              path = path,
              color = color,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
          }
        }
        currentPos = gEnd.coerceAtLeast(currentPos)
      }
      if (currentPos < endX) {
        clipRect(
          left = currentPos,
          top = centerY - clipTop,
          right = endX,
          bottom = centerY + clipTop,
        ) {
          drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )
        }
      }
    }

    // Played segment
    drawPathWithGaps(0f, totalProgressPx, primaryColor)

    if (transitionEnabled) {
      val disabledAlpha = 77f / 255f
      drawPathWithGaps(totalProgressPx, totalWidth, primaryColor.copy(alpha = disabledAlpha))
    } else {
      drawLine(
        color = surfaceVariant.copy(alpha = 0.4f),
        start = Offset(totalProgressPx, centerY),
        end = Offset(totalWidth, centerY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
      )
    }

    // Draw round cap
    val startAmp = kotlin.math.cos(kotlin.math.abs(waveStart) / waveLength * (2f * kotlin.math.PI.toFloat()))
    drawCircle(
      color = primaryColor,
      radius = strokeWidth / 2f,
      center = Offset(0f, centerY + startAmp * lineAmplitude * heightFraction),
    )

    // Vertical Bar Thumb
    val barHalfHeight = (lineAmplitude + strokeWidth)
    val barWidth = 5.dp.toPx() * scrubThickness

    if (barHalfHeight > 0.5f) {
        drawLine(
          color = primaryColor,
          start = Offset(totalProgressPx, centerY - barHalfHeight),
          end = Offset(totalProgressPx, centerY + barHalfHeight),
          strokeWidth = barWidth,
          cap = StrokeCap.Round,
        )
    }

    // A-B Loop Indicators for SquigglySeekbar
    if (loopStart != null || loopEnd != null) {
      val loopColor = Color(0xFFFFB300)
      val markerWidth = 2.dp.toPx()

      if (loopStart != null && duration > 0f) {
        val startPx = (loopStart / duration).coerceIn(0f, 1f) * totalWidth
        drawLine(
          color = loopColor,
          start = Offset(startPx, centerY - lineAmplitude - strokeWidth),
          end = Offset(startPx, centerY + lineAmplitude + strokeWidth),
          strokeWidth = markerWidth,
        )
      }

      if (loopEnd != null && duration > 0f) {
        val endPx = (loopEnd / duration).coerceIn(0f, 1f) * totalWidth
        drawLine(
          color = loopColor,
          start = Offset(endPx, centerY - lineAmplitude - strokeWidth),
          end = Offset(endPx, centerY + lineAmplitude + strokeWidth),
          strokeWidth = markerWidth,
        )
      }

      if (loopStart != null && loopEnd != null && duration > 0f) {
        val minPx = (minOf(loopStart, loopEnd) / duration).coerceIn(0f, 1f) * totalWidth
        val maxPx = (maxOf(loopStart, loopEnd) / duration).coerceIn(0f, 1f) * totalWidth
        drawRect(
          color = loopColor.copy(alpha = 0.2f),
          topLeft = Offset(minPx, centerY - lineAmplitude - strokeWidth),
          size = Size(maxPx - minPx, (lineAmplitude + strokeWidth) * 2),
        )
      }
    }
  }
}

@Composable
fun VideoTimer(
  value: Float,
  isInverted: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  val interactionSource = remember { MutableInteractionSource() }
  Text(
    modifier =
      modifier
        .fillMaxHeight()
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(),
          onClick = onClick,
        )
        .wrapContentHeight(Alignment.CenterVertically),
    text = Utils.prettyTime(value.toInt(), isInverted),
    color = Color.White,
    textAlign = TextAlign.Center,
  )
}

@Composable
fun StandardSeekbar(
    position: Float,
    duration: Float,
    chapters: ImmutableList<Segment>,
    isPaused: Boolean = false,
    isScrubbing: Boolean = false,
    useWavySeekbar: Boolean = false,
    seekbarStyle: SeekbarStyle = SeekbarStyle.Standard,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    loopStart: Float? = null,
    loopEnd: Float? = null,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    // Animation state (same as SquigglySeekbar)
    var heightFraction by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()

    // Animate height fraction based on paused state and scrubbing state (same as SquigglySeekbar)
    LaunchedEffect(isPaused, isScrubbing) {
        scope.launch {
            val shouldFlatten = isPaused || isScrubbing
            val targetHeight = if (shouldFlatten) 0.7f else 1f // Slightly less dramatic for standard seekbar
            val animationDuration = if (shouldFlatten) 550 else 800
            val startDelay = if (shouldFlatten) 0L else 60L

            kotlinx.coroutines.delay(startDelay)

            val animator = Animatable(heightFraction)
            animator.animateTo(
                targetValue = targetHeight,
                animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = LinearEasing,
                ),
            ) {
                heightFraction = value
            }
        }
    }

    // Thicken the track while scrubbing so the finger's target stays visible.
    val scrubThickness by animateFloatAsState(
        targetValue = if (isScrubbing) 1.75f else 1f,
        animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        label = "scrubThickness",
    )

    val isThick = seekbarStyle == SeekbarStyle.Thick
    val baseTrackHeight = if (isThick) 16.dp else 4.dp
    val trackHeightDp = (if (isThick) baseTrackHeight * heightFraction else baseTrackHeight) * scrubThickness
    val thumbWidth = if (isThick) 6.dp else 14.dp
    val thumbHeight = if (isThick) 16.dp * scrubThickness else 14.dp
    val thumbShape = if (isThick) RoundedCornerShape(3.dp) else CircleShape

    Slider(
        value = position,
        onValueChange = onSeek,
        onValueChangeFinished = onSeekFinished,
        valueRange = 0f..duration.coerceAtLeast(0.1f),
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        track = { sliderState ->
            val disabledAlpha = 0.3f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeightDp),
            ) {
                val min = sliderState.valueRange.start
                val max = sliderState.valueRange.endInclusive
                val range = (max - min).takeIf { it > 0f } ?: 1f

                val playedFraction = ((sliderState.value - min) / range).coerceIn(0f, 1f)

                val playedPx = size.width * playedFraction
                val trackHeight = size.height

                // Radius for the outer ends of the seekbar
                val outerRadius = trackHeight / 2f

                // For Thick style, inner corners match outer rounding; for Standard, small rounding
                val innerRadius = if (isThick) outerRadius else 2.dp.toPx()

                val thumbTrackGapSize = if (isThick) 14.dp.toPx() else 0f
                val gapHalf = thumbTrackGapSize / 2f
                val chapterGapHalf = 1.dp.toPx()

                val thumbGapStart = (playedPx - gapHalf).coerceIn(0f, size.width)
                val thumbGapEnd = (playedPx + gapHalf).coerceIn(0f, size.width)

                val chapterGaps = if (chapters.isNotEmpty()) {
                    chapters.mapNotNull { chapter ->
                        val chapterStart = chapter.start
                        if (chapterStart > min && chapterStart < max) {
                            val chapterFraction = ((chapterStart - min) / range).coerceIn(0f, 1f)
                            val chapterPx = size.width * chapterFraction
                            (chapterPx - chapterGapHalf) to (chapterPx + chapterGapHalf)
                        } else null
                    }
                } else emptyList()

                fun drawSegment(startX: Float, endX: Float, color: Color) {
                    if (endX - startX < 0.5f) return

                    val path = Path()
                    val isOuterLeft = startX <= 0.5f
                    val isInnerLeft = isThick && kotlin.math.abs(startX - thumbGapEnd) < 0.5f

                    val cornerRadiusLeft = when {
                        isOuterLeft -> androidx.compose.ui.geometry.CornerRadius(outerRadius)
                        isInnerLeft -> androidx.compose.ui.geometry.CornerRadius(innerRadius)
                        else -> androidx.compose.ui.geometry.CornerRadius.Zero
                    }

                    val isOuterRight = endX >= size.width - 0.5f
                    val isInnerRight = isThick && kotlin.math.abs(endX - thumbGapStart) < 0.5f

                    val cornerRadiusRight = when {
                        isOuterRight -> androidx.compose.ui.geometry.CornerRadius(outerRadius)
                        isInnerRight -> androidx.compose.ui.geometry.CornerRadius(innerRadius)
                        else -> androidx.compose.ui.geometry.CornerRadius.Zero
                    }

                    path.addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = startX,
                            top = 0f,
                            right = endX,
                            bottom = trackHeight,
                            topLeftCornerRadius = cornerRadiusLeft,
                            bottomLeftCornerRadius = cornerRadiusLeft,
                            topRightCornerRadius = cornerRadiusRight,
                            bottomRightCornerRadius = cornerRadiusRight
                        )
                    )
                    drawPath(path, color)
                }

                fun drawRangeWithGaps(
                    rangeStart: Float,
                    rangeEnd: Float,
                    gaps: List<Pair<Float, Float>>,
                    color: Color
                ) {
                    if (rangeEnd <= rangeStart) return
                    val relevantGaps = gaps
                        .filter { (gStart, gEnd) -> gEnd > rangeStart && gStart < rangeEnd }
                        .sortedBy { it.first }

                    var currentPos = rangeStart
                    for ((gStart, gEnd) in relevantGaps) {
                        val segmentEnd = gStart.coerceAtMost(rangeEnd)
                        if (segmentEnd > currentPos) {
                            drawSegment(currentPos, segmentEnd, color)
                        }
                        currentPos = gEnd.coerceAtLeast(currentPos)
                    }
                    if (currentPos < rangeEnd) {
                        drawSegment(currentPos, rangeEnd, color)
                    }
                }

                // 1. Unplayed Background
                val unplayedStart = if (isThick) thumbGapEnd else playedPx
                drawRangeWithGaps(unplayedStart, size.width, chapterGaps, primaryColor.copy(alpha = disabledAlpha))

                // 2. Played
                val playedEnd = if (isThick) thumbGapStart else playedPx
                if (playedEnd > 0) {
                    drawRangeWithGaps(0f, playedEnd, chapterGaps, primaryColor)
                }

                // 3. A-B Loop Indicators
                if (loopStart != null || loopEnd != null) {
                    val loopColor = Color(0xFFFFB300) // Amber/Gold color for loop
                    val markerWidth = 2.dp.toPx()

                    // Draw loop start marker
                    if (loopStart != null) {
                        val startPx = (loopStart / duration).coerceIn(0f, 1f) * size.width
                        drawLine(
                            color = loopColor,
                            start = Offset(startPx, 0f),
                            end = Offset(startPx, size.height),
                            strokeWidth = markerWidth
                        )
                    }

                    // Draw loop end marker
                    if (loopEnd != null) {
                        val endPx = (loopEnd / duration).coerceIn(0f, 1f) * size.width
                        drawLine(
                            color = loopColor,
                            start = Offset(endPx, 0f),
                            end = Offset(endPx, size.height),
                            strokeWidth = markerWidth
                        )
                    }

                    // Draw connected segment if both are set
                    if (loopStart != null && loopEnd != null) {
                        val minPx = (minOf(loopStart, loopEnd) / duration).coerceIn(0f, 1f) * size.width
                        val maxPx = (maxOf(loopStart, loopEnd) / duration).coerceIn(0f, 1f) * size.width

                        // Draw a semi-transparent overlay between A and B
                        drawRect(
                            color = loopColor.copy(alpha = 0.3f),
                            topLeft = Offset(minPx, 0f),
                            size = Size(maxPx - minPx, size.height)
                        )
                    }
                }
            }
        },
            thumb = {
                Box(
                    modifier = Modifier
                        .width(thumbWidth)
                        .height(thumbHeight)
                        .background(primaryColor, thumbShape)
                )
            }
        )
    }

@Preview
@Composable
private fun PreviewSeekBar() {
  SeekbarWithTimers(
    position = 30f,
    duration = 180f,
    onValueChange = {},
    onValueChangeFinished = {},
    timersInverted = Pair(false, true),
    positionTimerOnClick = {},
    durationTimerOnCLick = {},
    chapters = persistentListOf(),
    paused = false,
  )
}
