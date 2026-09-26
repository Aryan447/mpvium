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
import android.os.SystemClock
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Vertical drag distance (dp) per scrub-rate tier while seekbar-scrubbing.
 * Dragging up through tiers selects 1x, 2x, 4x, 8x horizontal scrub speed.
 */
private const val SCRUB_RATE_TIER_STEP_DP = 48f
private const val MAX_SCRUB_TIER = 3

/**
 * How far (seconds) the reported mpv position may sit from a requested seek
 * target while still counting as "landed". Covers exact-seek rounding and
 * small poll jitter.
 */
private const val SEEK_SETTLE_TOLERANCE_SEC = 1.5f

/**
 * Safety net: never leave the thumb parked on a requested target longer than
 * this if mpv never confirms (rejected seek, EOF, track switch). Only
 * releases the hold back to the live position; performs no seek.
 */
private const val SEEK_SETTLE_TIMEOUT_MS = 2000L

/**
 * Blind window after a tap/release during which non-landed live polls are
 * ignored outright. Covers the backlog of throttled scrub seeks still
 * draining through mpv: without it, the first intermediate landing satisfies
 * "moved on" and snaps the thumb backward (e.g. 12:20 -> 10:xx -> 12:20)
 * before the final seek lands.
 */
private const val SEEK_SETTLE_HOLD_MS = 350L

/**
 * A moved live position only counts as the final landing once it sits
 * unchanged this long. Queued intermediates keep changing poll-to-poll, so
 * they can never trigger a release; the parked final (exact or
 * keyframe-inexact) can.
 */
private const val SEEK_SETTLE_STABLE_MS = 250L

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
  seekbarStyle: SeekbarStyle = SeekbarStyle.Standard,
  loopStart: Float? = null,
  loopEnd: Float? = null,
  modifier: Modifier = Modifier,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  var isUserInteracting by remember { mutableStateOf(false) }
  var userPosition by remember { mutableFloatStateOf(position) }

  // Seek-settle guard: while a seek is in flight, stale time-pos polls (still
  // sitting at the pre-seek position) must not move the thumb backward.
  // dragActive = finger down (preview owns the thumb, never auto-release).
  // settling = tap or post-release, waiting for mpv's time-pos to confirm.
  var dragActive by remember { mutableStateOf(false) }
  var settling by remember { mutableStateOf(false) }
  var settleTarget by remember { mutableFloatStateOf(Float.NaN) }
  var settleBase by remember { mutableFloatStateOf(0f) }
  // Book-keeping for the backlog-proof settle policy below: when the hold
  // started, and the last live value plus when it last changed.
  var settleHoldUntilMs by remember { mutableLongStateOf(0L) }
  var settleLastLive by remember { mutableFloatStateOf(Float.NaN) }
  var settleLastChangeMs by remember { mutableLongStateOf(0L) }

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

  // Position follower. The precise-position poll already ticks at ~24fps, so
  // follow it immediately (snap) instead of a lagging tween: overlapping
  // 200ms animations kept the thumb perpetually behind and made every tap
  // visibly stutter, worst on wide tablet tracks. While a seek is settling,
  // hold the thumb on the user's target until mpv confirms.
  val animatedPosition = remember { Animatable(position) }
  val scope = rememberCoroutineScope()
  val latestPosition by rememberUpdatedState(position)
  // Read via state (not pointerInput keys) so a duration load completing
  // mid-drag doesn't restart — and cancel — the active gesture detector.
  val latestDuration by rememberUpdatedState(duration)
  val latestOnValueChange by rememberUpdatedState(onValueChange)
  val latestOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

  // Shared entry into the settling hold for taps and releases. The thumb
  // parks on [target] until mpv proves it got there; see the policy below.
  fun enterSettling(target: Float) {
    val now = SystemClock.uptimeMillis()
    dragActive = false
    isUserInteracting = true
    settling = true
    settleBase = latestPosition
    settleTarget = target
    settleHoldUntilMs = now + SEEK_SETTLE_HOLD_MS
    settleLastLive = Float.NaN
    settleLastChangeMs = now
    scrubTier = 0
  }

  LaunchedEffect(position, dragActive, settling) {
    if (dragActive) return@LaunchedEffect // finger down: preview owns the thumb
    if (settling) {
      val target = settleTarget
      if (target.isNaN()) {
        settling = false
        isUserInteracting = false
      } else if (abs(position - target) <= SEEK_SETTLE_TOLERANCE_SEC) {
        // Fast path: mpv landed on (or plays through) the target. Stale
        // polls sit far from the target by definition, so this can never
        // false-fire on them — safe to release immediately, even mid-hold.
        settling = false
        isUserInteracting = false
        settleTarget = Float.NaN
        animatedPosition.snapTo(position)
      } else {
        // Otherwise the final seek is still in flight (or landed keyframe-
        // inexact). Throttled scrub seeks queued behind the finger keep
        // landing here, and any one of them would satisfy a naive "moved
        // on" check and snap the thumb backward mid-seek. So: track the
        // live value and only accept a moved position once it has parked
        // unchanged past the blind hold — i.e. the queue has drained and
        // mpv sits on the final landing. Anything else keeps holding the
        // thumb on the user's target.
        val now = SystemClock.uptimeMillis()
        if (settleLastLive.isNaN() || position != settleLastLive) {
          settleLastLive = position
          settleLastChangeMs = now
        }
        val moved = abs(position - settleBase) >= SEEK_SETTLE_TOLERANCE_SEC
        val parked = now - settleLastChangeMs >= SEEK_SETTLE_STABLE_MS
        if (moved && parked && now >= settleHoldUntilMs) {
          settling = false
          isUserInteracting = false
          settleTarget = Float.NaN
          animatedPosition.snapTo(position)
        } else if (animatedPosition.value != userPosition) {
          animatedPosition.snapTo(userPosition)
        }
      }
      return@LaunchedEffect
    }
    if (position != animatedPosition.value) {
      animatedPosition.snapTo(position)
    }
  }

  // Safety net for the settle guard: if mpv never confirms (rejected seek,
  // EOF, track switch), fall back to the live position instead of parking
  // the thumb on a stale target forever.
  LaunchedEffect(settling) {
    if (!settling) return@LaunchedEffect
    delay(SEEK_SETTLE_TIMEOUT_MS)
    if (settling && !dragActive) {
      settling = false
      isUserInteracting = false
      settleTarget = Float.NaN
      animatedPosition.snapTo(latestPosition)
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

    // Seekbar with expanded touch area. The outer box is the full 48dp touch
    // height; the visual track inside is smaller and centered. The overlay
    // below uses matchParentSize so it exactly covers this box — a fixed
    // larger height would overflow and steal taps meant for neighbouring
    // controls (timers, video surface).
    Box(
      modifier =
        Modifier
          .weight(1f)
          .height(48.dp)
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
            // Touch handled by parent overlay (single gesture handler).
            onSeek = { },
            onSeekFinished = { },
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
            // Touch handled by parent overlay (single gesture handler).
            onSeek = { },
            onSeekFinished = { },
            loopStart = loopStart,
            loopEnd = loopEnd,
          )
        }
        SeekbarStyle.Slim, SeekbarStyle.NeonGlow, SeekbarStyle.Segmented, SeekbarStyle.RetroBlocky -> {
          StyledSeekbar(
            position = if (isUserInteracting) userPosition else animatedPosition.value,
            duration = duration,
            chapters = chapters,
            isScrubbing = isUserInteracting,
            seekbarStyle = seekbarStyle,
            // Touch handled by parent overlay (single gesture handler).
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
          .matchParentSize()
          .pointerInput(scrubTierStepPx) {
            detectTapGestures(
              onTap = { offset ->
                val currentDuration = latestDuration
                if (currentDuration <= 0f) return@detectTapGestures
                val target = xToSeekPosition(offset.x, size.width, currentDuration)
                // Single exact seek per tap: park the thumb on the target
                // immediately and let the settle guard hold it there until
                // mpv's time-pos confirms. No preview scrub seek first — that
                // issued two mpv seeks per tap plus a pause/unpause cycle.
                enterSettling(target)
                userPosition = target
                scope.launch { animatedPosition.snapTo(target) }
                latestOnValueChangeFinished(target)
              }
            )
          }
          .pointerInput(scrubTierStepPx) {
            detectDragGestures(
              onDragStart = { offset ->
                val currentDuration = latestDuration
                if (currentDuration <= 0f) return@detectDragGestures
                dragActive = true
                isUserInteracting = true
                settling = false
                settleTarget = Float.NaN
                settleBase = latestPosition
                // Anchor to the grab point immediately: the first onDrag
                // callback only fires after touch slop, otherwise the
                // preview would jump from the stale position on wide tracks.
                dragAnchor = xToSeekPosition(offset.x, size.width, currentDuration)
                scaledDragDxPx = 0f
                verticalDragPx = 0f
                scrubTier = 0
                userPosition = dragAnchor
                scope.launch { animatedPosition.snapTo(dragAnchor) }
                latestOnValueChange(userPosition)
              },
              onDragEnd = {
                // Release uses the last settled drag value — never re-sample
                // here, so lift-off micro-jitter can't clobber the target.
                // enterSettling re-bases on the live position at release and
                // starts the blind hold, so queued scrub intermediates landing
                // now can't snap the thumb back before the final seek lands.
                enterSettling(userPosition)
                latestOnValueChangeFinished(userPosition)
              },
              onDragCancel = {
                enterSettling(userPosition)
                latestOnValueChangeFinished(userPosition)
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
              val currentDuration = latestDuration
              if (size.width > 0 && currentDuration > 0f) {
                scaledDragDxPx += dragAmount.x * scrubRateForTier(scrubTier)
                val anchorFraction = dragAnchor.toDouble() / currentDuration.toDouble()
                val deltaFraction = scaledDragDxPx.toDouble() / size.width.toDouble()
                userPosition =
                  ((anchorFraction + deltaFraction) * currentDuration.toDouble())
                    .toFloat().coerceIn(0f, currentDuration)
                latestOnValueChange(userPosition)
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

  // Wave parameters
  val waveLength = 80f
  val lineAmplitude = if (useWavySeekbar) 6f else 0f
  val phaseSpeed = 10f // px per second
  val transitionPeriods = 1.5f
  val minWaveEndpoint = 0f
  val matchedWaveEndpoint = 1f
  val transitionEnabled = true

  // Animate height fraction based on paused state and scrubbing state.
  // Runs directly in the LaunchedEffect (not a nested scope.launch) so a
  // pause/resume toggle cancels the in-flight animation instead of leaking
  // concurrent animators that fight over heightFraction.
  LaunchedEffect(isPaused, isScrubbing, useWavySeekbar) {
    if (!useWavySeekbar) {
      heightFraction = 0f
      return@LaunchedEffect
    }

    val shouldFlatten = isPaused || isScrubbing
    val targetHeight = if (shouldFlatten) 0f else 1f
    val animDuration = if (shouldFlatten) 550 else 800
    val startDelay = if (shouldFlatten) 0L else 60L

    kotlinx.coroutines.delay(startDelay)

    val animator = Animatable(heightFraction)
    animator.animateTo(
      targetValue = targetHeight,
      animationSpec =
        tween(
          durationMillis = animDuration,
          easing = LinearEasing,
        ),
    ) {
      heightFraction = value
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

    // Animation state (same as SquigglySeekbar). Runs directly in the
    // LaunchedEffect so toggle flapping cancels instead of leaking.
    var heightFraction by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(isPaused, isScrubbing) {
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

    // Non-interactive visual only: the parent touch overlay is the single
    // gesture handler for every style. Leaving this Slider enabled would add
    // a second handler underneath (with its own thumb-inset mapping), stealing
    // pointers from the overlay and clobbering the release target.
    Slider(
        value = position,
        onValueChange = onSeek,
        onValueChangeFinished = onSeekFinished,
        valueRange = 0f..duration.coerceAtLeast(0.1f),
        modifier = Modifier.fillMaxWidth(),
        enabled = false,
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

/**
 * Parameter-driven seekbar track backing the Slim, Neon glow, Segmented and
 * Retro blocky styles. Same non-interactive Slider shell as [StandardSeekbar]:
 * the parent touch overlay is the single gesture handler, so this visual
 * stays disabled and only draws the track + thumb.
 */
@Composable
private fun StyledSeekbar(
    position: Float,
    duration: Float,
    chapters: ImmutableList<Segment>,
    isScrubbing: Boolean = false,
    seekbarStyle: SeekbarStyle,
    loopStart: Float? = null,
    loopEnd: Float? = null,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    // Thicken the track while scrubbing so the finger's target stays visible.
    val scrubThickness by animateFloatAsState(
        targetValue = if (isScrubbing) 1.5f else 1f,
        animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        label = "styledScrubThickness",
    )

    val isBlocky = seekbarStyle == SeekbarStyle.RetroBlocky
    val isSegmented = seekbarStyle == SeekbarStyle.Segmented
    val isGlow = seekbarStyle == SeekbarStyle.NeonGlow
    val baseTrackHeight = when (seekbarStyle) {
        SeekbarStyle.Slim -> 2.dp
        SeekbarStyle.NeonGlow -> 6.dp
        SeekbarStyle.Segmented -> 8.dp
        SeekbarStyle.RetroBlocky -> 12.dp
        else -> 4.dp
    }
    val trackHeightDp = baseTrackHeight * scrubThickness
    // Neon halo needs vertical headroom around the core track.
    val glowPadding = if (isGlow) 8.dp else 0.dp
    val canvasHeightDp = trackHeightDp + glowPadding * 2

    val thumbSize = when (seekbarStyle) {
        SeekbarStyle.Slim -> 8.dp
        SeekbarStyle.NeonGlow -> 12.dp
        SeekbarStyle.Segmented -> 12.dp
        SeekbarStyle.RetroBlocky -> 10.dp
        else -> 12.dp
    }
    val thumbShape = if (isBlocky) RoundedCornerShape(2.dp) else CircleShape

    Slider(
        value = position,
        onValueChange = {},
        valueRange = 0f..duration.coerceAtLeast(0.1f),
        modifier = modifier.fillMaxWidth(),
        enabled = false,
        interactionSource = interactionSource,
        track = { sliderState ->
            val disabledAlpha = 0.3f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeightDp),
            ) {
                val min = sliderState.valueRange.start
                val max = sliderState.valueRange.endInclusive
                val range = (max - min).takeIf { it > 0f } ?: 1f

                val playedFraction = ((sliderState.value - min) / range).coerceIn(0f, 1f)
                val playedPx = size.width * playedFraction

                val glowPaddingPx = glowPadding.toPx()
                val coreHeight = size.height - glowPaddingPx * 2f
                val coreTop = glowPaddingPx
                val coreBottom = glowPaddingPx + coreHeight

                val outerRadius = if (isBlocky) 0f else coreHeight / 2f
                val innerRadius = if (isBlocky) 0f else 2.dp.toPx()
                val chapterGapHalf = 1.dp.toPx()

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

                // Fixed-interval ticks for the Segmented style.
                val tickGaps = if (isSegmented && size.width > 0f) {
                    val stepPx = 24.dp.toPx()
                    val tickHalf = 1.dp.toPx()
                    generateSequence(stepPx) { it + stepPx }
                        .takeWhile { it < size.width - 1f }
                        .map { (it - tickHalf) to (it + tickHalf) }
                        .toList()
                } else emptyList()
                val allGaps = chapterGaps + tickGaps

                fun drawSegment(startX: Float, endX: Float, top: Float, bottom: Float, color: Color) {
                    if (endX - startX < 0.5f || bottom - top < 0.5f) return

                    val path = Path()
                    val leftRadius = if (startX <= 0.5f) outerRadius else innerRadius
                    val rightRadius = if (endX >= size.width - 0.5f) outerRadius else innerRadius
                    path.addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = startX,
                            top = top,
                            right = endX,
                            bottom = bottom,
                            topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(leftRadius),
                            bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(leftRadius),
                            topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(rightRadius),
                            bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(rightRadius)
                        )
                    )
                    drawPath(path, color)
                }

                fun drawRangeWithGaps(
                    rangeStart: Float,
                    rangeEnd: Float,
                    top: Float,
                    bottom: Float,
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
                            drawSegment(currentPos, segmentEnd, top, bottom, color)
                        }
                        currentPos = gEnd.coerceAtLeast(currentPos)
                    }
                    if (currentPos < rangeEnd) {
                        drawSegment(currentPos, rangeEnd, top, bottom, color)
                    }
                }

                // Neon halo: widening low-alpha passes behind the played core.
                if (isGlow && playedPx > 0.5f) {
                    drawSegment(0f, playedPx, 0f, size.height, primaryColor.copy(alpha = 0.10f))
                    val midInset = glowPaddingPx / 2f
                    drawSegment(0f, playedPx, midInset, size.height - midInset, primaryColor.copy(alpha = 0.18f))
                }

                // 1. Unplayed Background
                drawRangeWithGaps(playedPx, size.width, coreTop, coreBottom, allGaps, primaryColor.copy(alpha = disabledAlpha))

                // 2. Played
                if (playedPx > 0) {
                    drawRangeWithGaps(0f, playedPx, coreTop, coreBottom, allGaps, primaryColor)
                }

                // 3. A-B Loop Indicators
                if (loopStart != null || loopEnd != null) {
                    val loopColor = Color(0xFFFFB300) // Amber/Gold color for loop
                    val markerWidth = 2.dp.toPx()

                    if (loopStart != null) {
                        val startPx = (loopStart / duration).coerceIn(0f, 1f) * size.width
                        drawLine(
                            color = loopColor,
                            start = Offset(startPx, coreTop),
                            end = Offset(startPx, coreBottom),
                            strokeWidth = markerWidth
                        )
                    }

                    if (loopEnd != null) {
                        val endPx = (loopEnd / duration).coerceIn(0f, 1f) * size.width
                        drawLine(
                            color = loopColor,
                            start = Offset(endPx, coreTop),
                            end = Offset(endPx, coreBottom),
                            strokeWidth = markerWidth
                        )
                    }

                    if (loopStart != null && loopEnd != null) {
                        val minPx = (minOf(loopStart, loopEnd) / duration).coerceIn(0f, 1f) * size.width
                        val maxPx = (maxOf(loopStart, loopEnd) / duration).coerceIn(0f, 1f) * size.width
                        drawRect(
                            color = loopColor.copy(alpha = 0.3f),
                            topLeft = Offset(minPx, coreTop),
                            size = Size(maxPx - minPx, coreBottom - coreTop)
                        )
                    }
                }
            }
        },
        thumb = {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                if (isGlow) {
                    Box(
                        modifier = Modifier
                            .width(thumbSize + 12.dp)
                            .height(thumbSize + 12.dp)
                            .background(primaryColor.copy(alpha = 0.25f), CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(thumbSize)
                        .height(thumbSize)
                        .background(primaryColor, thumbShape)
                )
            }
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
