package app.aryan447.mpvium.ui.player.controls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `is`.xyz.mpv.MPVLib
import app.aryan447.mpvium.preferences.AudioPreferences
import app.aryan447.mpvium.preferences.GesturePreferences
import app.aryan447.mpvium.preferences.PlayerPreferences
import app.aryan447.mpvium.preferences.SubtitlesPreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.components.LeftSideOvalShape
import app.aryan447.mpvium.presentation.components.RightSideOvalShape
import app.aryan447.mpvium.ui.player.Panels
import app.aryan447.mpvium.ui.player.PlayerUpdates
import app.aryan447.mpvium.ui.player.PlayerViewModel
import app.aryan447.mpvium.ui.player.SingleActionGesture
import app.aryan447.mpvium.ui.theme.playerRippleConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Touches starting within this vertical distance of the current subtitle
 * position are treated as subtitle touches: hold to drag subtitles up/down,
 * double-tap to reset them. Kept deliberately tight so holds and double-taps
 * elsewhere don't get hijacked.
 */
private const val SubtitleTouchToleranceFraction = 0.075f

/** Minimum touch slop for the subtitle band so small videos stay usable. */
private const val SubtitleTouchMinSlopPx = 48f

/**
 * Vertical video rect (top offset, height in px) inside the touch
 * container, assuming aspect-fit scaling (mpv default). mpv positions
 * subtitles relative to the *video* frame, not the full screen, so on
 * letterboxed/pillarboxed content the old full-height mapping put the
 * touch zone way below (or above) the actual subtitles. Falls back to
 * the full container while the video aspect is still unknown.
 */
private fun videoRectTopAndHeight(
  containerWidth: Float,
  containerHeight: Float,
  videoAspect: Double?,
): Pair<Float, Float> {
  if (containerWidth <= 0f || containerHeight <= 0f || videoAspect == null || videoAspect <= 0.0) {
    return 0f to containerHeight
  }
  val containerAspect = (containerWidth / containerHeight).toDouble()
  return if (containerAspect > videoAspect) {
    // Pillarboxed: video fills the container height.
    0f to containerHeight
  } else {
    // Letterboxed: video height derived from the container width.
    val videoHeight = (containerWidth / videoAspect).toFloat()
    ((containerHeight - videoHeight) / 2f) to videoHeight
  }
}

/** Current rotation-corrected video aspect (w/h), or null if unknown. */
private fun currentVideoAspect(): Double? {
  val aspect = MPVLib.getPropertyDouble("video-params/aspect") ?: return null
  if (aspect <= 0.0) return null
  val rotate = MPVLib.getPropertyInt("video-params/rotate") ?: 0
  return if (rotate % 180 == 90) 1.0 / aspect else aspect
}

private fun subtitleYInContainer(
  containerWidth: Float,
  containerHeight: Float,
  videoAspect: Double?,
  currentSubPos: Int,
): Float {
  val (top, videoHeight) = videoRectTopAndHeight(containerWidth, containerHeight, videoAspect)
  return top + (currentSubPos.coerceIn(0, 110) / 100f) * videoHeight
}

private fun isSubtitleTouch(
  y: Float,
  containerWidth: Float,
  containerHeight: Float,
  videoAspect: Double?,
  currentSubPos: Int,
): Boolean {
  if (containerHeight <= 0f) return false
  val (_, videoHeight) = videoRectTopAndHeight(containerWidth, containerHeight, videoAspect)
  val tolerance = (videoHeight * SubtitleTouchToleranceFraction).coerceAtLeast(SubtitleTouchMinSlopPx)
  return kotlin.math.abs(
    y - subtitleYInContainer(containerWidth, containerHeight, videoAspect, currentSubPos),
  ) <= tolerance
}

@Suppress("CyclomaticComplexMethod", "MultipleEmitters")
@Composable
fun GestureHandler(
  viewModel: PlayerViewModel,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
) {
  val playerPreferences = koinInject<PlayerPreferences>()
  val audioPreferences = koinInject<AudioPreferences>()
  val gesturePreferences = koinInject<GesturePreferences>()
  val subtitlesPreferences = koinInject<SubtitlesPreferences>()
  val panelShown by viewModel.panelShown.collectAsState()
  val allowGesturesInPanels by playerPreferences.allowGesturesInPanels.collectAsState()
  val paused by MPVLib.propBoolean["pause"].collectAsState()
  val duration by MPVLib.propInt["duration"].collectAsState()
  val position by MPVLib.propInt["time-pos"].collectAsState()
  val playbackSpeed by MPVLib.propFloat["speed"].collectAsState()
  val controlsShown by viewModel.controlsShown.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  val seekAmount by viewModel.doubleTapSeekAmount.collectAsState()
  val isSeekingForwards by viewModel.isSeekingForwards.collectAsState()
  val useSingleTapForCenter by gesturePreferences.useSingleTapForCenter.collectAsState()
  val doubleTapSeekAreaWidth by gesturePreferences.doubleTapSeekAreaWidth.collectAsState()
  var isDoubleTapSeeking by remember { mutableStateOf(false) }
  LaunchedEffect(seekAmount) {
    delay(800)
    isDoubleTapSeeking = false
    viewModel.updateSeekAmount(0)
    viewModel.updateSeekText(null)
    delay(100)
    viewModel.hideSeekBar()
  }
  val multipleSpeedGesture by playerPreferences.holdForMultipleSpeed.collectAsState()
  val showDynamicSpeedOverlay by playerPreferences.showDynamicSpeedOverlay.collectAsState()
  val brightnessGesture by playerPreferences.brightnessGesture.collectAsState()
  val volumeGesture by playerPreferences.volumeGesture.collectAsState()
  val swapVolumeAndBrightness by playerPreferences.swapVolumeAndBrightness.collectAsState()
  val pinchToZoomGesture by playerPreferences.pinchToZoomGesture.collectAsState()
  val panAndZoomEnabled by playerPreferences.panAndZoomEnabled.collectAsState()
  val horizontalSwipeToSeek by playerPreferences.horizontalSwipeToSeek.collectAsState()
  val horizontalSwipeSensitivity by playerPreferences.horizontalSwipeSensitivity.collectAsState()
  var isLongPressing by remember { mutableStateOf(false) }
  var isDynamicSpeedControlActive by remember { mutableStateOf(false) }
  var dynamicSpeedStartX by remember { mutableStateOf(0f) }
  var dynamicSpeedStartValue by remember { mutableStateOf(2f) }
  var lastAppliedSpeed by remember { mutableStateOf(2f) }
  var hasSwipedEnough by remember { mutableStateOf(false) }
  var longPressTriggeredDuringTouch by remember { mutableStateOf(false) }
  var isVerticalGestureActive by remember { mutableStateOf(false) }
  val currentVolume by viewModel.currentVolume.collectAsState()
  val currentMPVVolume by MPVLib.propInt["volume"].collectAsState()
  val currentBrightness by viewModel.currentBrightness.collectAsState()
  val volumeBoostingCap = audioPreferences.volumeBoostCap.get()
  val haptics = LocalHapticFeedback.current
  val coroutineScope = rememberCoroutineScope()
  val configuration = LocalConfiguration.current
  val isTablet = remember(configuration) { configuration.smallestScreenWidthDp >= 600 }

  // Isolated double-tap state tracking
  var tapCount by remember { mutableStateOf(0) }
  var lastTapTime by remember { mutableStateOf(0L) }
  var lastTapPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
  var lastTapRegion by remember { mutableStateOf<String?>(null) }
  var pendingSingleTapRegion by remember { mutableStateOf<String?>(null) }
  var pendingSingleTapPosition by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
  val doubleTapTimeout = 300L
  val multiTapContinueWindow = 650L

  // Multi-tap seeking state
  var lastSeekRegion by remember { mutableStateOf<String?>(null) }
  var lastSeekTime by remember { mutableStateOf<Long?>(null) }

  // Auto-reset tap count on timeout and execute single tap if no double tap detected
  LaunchedEffect(tapCount, longPressTriggeredDuringTouch) {
    if (tapCount == 1 && pendingSingleTapRegion != null) {
      delay(doubleTapTimeout)
      // Timeout occurred, execute single tap action only if not double-tap seeking and not triggered by long press
      if (tapCount == 1 && pendingSingleTapRegion != null && !isDoubleTapSeeking && !longPressTriggeredDuringTouch) {
        val region = pendingSingleTapRegion!!
        val isCenterTap = region == "center"
        if (useSingleTapForCenter && isCenterTap) {
          viewModel.handleCenterSingleTap()
        } else {
          if (panelShown != Panels.None && !allowGesturesInPanels) {
            viewModel.panelShown.update { Panels.None }
          }
          if (controlsShown) {
            viewModel.hideControls()
          } else {
            viewModel.showControls()
          }
        }
        pendingSingleTapRegion = null
        pendingSingleTapPosition = null
      }
      tapCount = 0
      lastTapRegion = null
      if (!isDoubleTapSeeking) {
        isDoubleTapSeeking = false
        viewModel.updateSeekAmount(0)
      }
    }
  }

  // Reset double-tap seek state when seeking stops
  LaunchedEffect(seekAmount) {
    if (seekAmount == 0) {
      delay(100)
      isDoubleTapSeeking = false
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 48.dp, vertical = 0.dp)
      .pointerInput(areControlsLocked, doubleTapSeekAreaWidth, gesturePreferences, isVerticalGestureActive) {
        // Isolated double-tap detection that doesn't interfere with other gestures
        if (isVerticalGestureActive) return@pointerInput
        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          val downPosition = down.position
          val downTime = System.currentTimeMillis()

          // Calculate regions
          val seekAreaFraction = doubleTapSeekAreaWidth / 100f
          val leftBoundary = size.width * seekAreaFraction
          val rightBoundary = size.width * (1f - seekAreaFraction)
          val region = when {
            downPosition.x > rightBoundary -> "right"
            downPosition.x < leftBoundary -> "left"
            else -> "center"
          }

          // Track for potential drag
          var isDrag = false
          var wasConsumedByTapGesture = false

          do {
            val event = awaitPointerEvent()
            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

            // Check if this is a drag (not a tap)
            val distance = sqrt(
              (pointer.position.x - downPosition.x) * (pointer.position.x - downPosition.x) +
              (pointer.position.y - downPosition.y) * (pointer.position.y - downPosition.y)
            )

            if (distance > 10f) {
              isDrag = true
              // Don't consume - let other pointer inputs handle drag gestures
            }

            if (!pointer.pressed) {
              // Pointer lifted - this is a tap if it wasn't a drag
              if (!isDrag && !wasConsumedByTapGesture) {
                val timeSinceLastTap = downTime - lastTapTime
                val positionChange = sqrt(
                  (downPosition.x - lastTapPosition.x) * (downPosition.x - lastTapPosition.x) +
                  (downPosition.y - lastTapPosition.y) * (downPosition.y - lastTapPosition.y)
                )

                // Check if this is a continuation of multi-tap sequence
                val isMultiTapContinuation =
                  lastTapRegion == region &&
                  timeSinceLastTap < multiTapContinueWindow &&
                  positionChange < 100f &&
                  tapCount >= 2 &&
                  isDoubleTapSeeking

                val currentSubPos = MPVLib.getPropertyInt("sub-pos") ?: 100
                val videoAspect = currentVideoAspect()
                val isSubTap = isSubtitleTouch(downPosition.y, size.width.toFloat(), size.height.toFloat(), videoAspect, currentSubPos)
                val isPrevSubTap = isSubtitleTouch(lastTapPosition.y, size.width.toFloat(), size.height.toFloat(), videoAspect, currentSubPos)
                val isDoubleTapOnSubtitle = isSubTap && isPrevSubTap && positionChange < 150f

                // Check if this is a valid double-tap
                val isDoubleTap =
                  timeSinceLastTap < doubleTapTimeout &&
                  (lastTapRegion == region || isDoubleTapOnSubtitle) &&
                  (positionChange < 100f || isDoubleTapOnSubtitle) &&
                  tapCount == 1

                if (isDoubleTap && !areControlsLocked) {
                  // Valid double-tap detected
                  tapCount = 2
                  lastTapTime = downTime
                  lastTapPosition = downPosition
                  pendingSingleTapRegion = null // Cancel pending single tap
                  pendingSingleTapPosition = null
                  wasConsumedByTapGesture = true
                  pointer.consume()

                  if (isSubTap) {
                    val actualSubPos = 100
                    MPVLib.setPropertyInt("sub-pos", actualSubPos)
                    MPVLib.setPropertyInt("secondary-sub-pos", (actualSubPos - 10).coerceIn(0, 110))
                    if (subtitlesPreferences.persistSubPos.get()) {
                      subtitlesPreferences.subPos.set(actualSubPos)
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.playerUpdate.update {
                      PlayerUpdates.SubtitlePosition(actualSubPos, isReset = true)
                    }
                  } else {
                    when (region) {
                      "right" -> {
                        val rightGesture = gesturePreferences.rightSingleActionGesture.get()
                        if (rightGesture == SingleActionGesture.Seek) {
                          isDoubleTapSeeking = true
                          lastSeekRegion = "right"
                          lastSeekTime = System.currentTimeMillis()
                          if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                        }
                        viewModel.handleRightDoubleTap()
                      }
                      "left" -> {
                        val leftGesture = gesturePreferences.leftSingleActionGesture.get()
                        if (leftGesture == SingleActionGesture.Seek) {
                          isDoubleTapSeeking = true
                          lastSeekRegion = "left"
                          lastSeekTime = System.currentTimeMillis()
                          if (isSeekingForwards) viewModel.updateSeekAmount(0)
                        }
                        viewModel.handleLeftDoubleTap()
                      }
                      "center" -> {
                        viewModel.handleCenterDoubleTap()
                      }
                    }
                  }
                } else if (isMultiTapContinuation && isDoubleTapSeeking) {
                  // Continue multi-tap seeking
                  tapCount++
                  wasConsumedByTapGesture = true
                  pointer.consume()
                  lastSeekTime = System.currentTimeMillis()
                  lastTapTime = downTime
                  lastTapPosition = downPosition

                  when (region) {
                    "right" -> {
                      val rightGesture = gesturePreferences.rightSingleActionGesture.get()
                      if (rightGesture == SingleActionGesture.Seek) {
                        if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                      }
                      viewModel.handleRightDoubleTap()
                    }
                    "left" -> {
                      val leftGesture = gesturePreferences.leftSingleActionGesture.get()
                      if (leftGesture == SingleActionGesture.Seek) {
                        if (isSeekingForwards) viewModel.updateSeekAmount(0)
                      }
                      viewModel.handleLeftDoubleTap()
                    }
                    "center" -> {
                      viewModel.handleCenterDoubleTap()
                    }
                  }
                } else if (tapCount == 0 || timeSinceLastTap >= doubleTapTimeout) {
                  // Single tap or timed out - start new tap sequence
                  tapCount = 1
                  lastTapTime = downTime
                  lastTapPosition = downPosition
                  lastTapRegion = region
                  pendingSingleTapRegion = region
                  pendingSingleTapPosition = downPosition
                  wasConsumedByTapGesture = true
                  pointer.consume()
                  // Don't execute single tap action yet - wait to see if second tap comes
                }
              }
              break
            }
          } while (event.changes.any { it.pressed })
        }
      }
      .pointerInput(areControlsLocked, multipleSpeedGesture, brightnessGesture, volumeGesture) {
        if (areControlsLocked) return@pointerInput

        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          val startPosition = down.position

          // Reset long press tracking at the start of each gesture
          longPressTriggeredDuringTouch = false

          // State for vertical gestures (volume/brightness)
          var startingY = 0f
          var mpvVolumeStartingY = 0f
          var originalVolume = currentVolume
          var originalMPVVolume = currentMPVVolume
          var originalBrightness = currentBrightness
          var lastVolumeValue = currentVolume
          var lastMPVVolumeValue = currentMPVVolume ?: 100
          var lastBrightnessValue = currentBrightness
          val brightnessGestureSens = 0.001f
          val volumeGestureSens = 0.017f
          val mpvVolumeGestureSens = 0.017f

          // Original speed for long press
          var originalSpeed = playbackSpeed ?: 1f

          // Track long press separately
          var longPressTriggered = false
          // Subtitle drag: press-and-hold in the subtitle band, then move up/down.
          // Tracks whether the hold started the speed boost (vs subtitle drag),
          // so gesture-end cleanup only undoes what was actually started.
          var subtitleDragActive = false
          var speedHoldActive = false
          var subtitleDragStartSubPos = subtitlesPreferences.subPos.get()
          var lastSubtitlePos = subtitleDragStartSubPos
          val gestureAreaHeight = size.height.toFloat()
          val gestureAreaWidth = size.width.toFloat()
          val gestureVideoAspect = currentVideoAspect()
          val longPressDelay = 500L
          var longPressJob = coroutineScope.launch {
            delay(longPressDelay)
            if (longPressTriggered) return@launch
            val distance = sqrt(
              (down.position.x - startPosition.x) * (down.position.x - startPosition.x) +
              (down.position.y - startPosition.y) * (down.position.y - startPosition.y)
            )
            val currentSubPos = MPVLib.getPropertyInt("sub-pos") ?: subtitlesPreferences.subPos.get()
            // Hold without moving in the subtitle band starts subtitle drag mode
            if (distance < 10f && isSubtitleTouch(startPosition.y, gestureAreaWidth, gestureAreaHeight, gestureVideoAspect, currentSubPos)) {
              longPressTriggered = true
              isLongPressing = true
              longPressTriggeredDuringTouch = true
              subtitleDragActive = true
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              subtitleDragStartSubPos = currentSubPos
              lastSubtitlePos = subtitleDragStartSubPos
              viewModel.playerUpdate.update { PlayerUpdates.SubtitlePosition(lastSubtitlePos) }
            } else if (distance < 10f && paused == false && multipleSpeedGesture > 0f) {
                longPressTriggered = true
                speedHoldActive = true
                isLongPressing = true
                longPressTriggeredDuringTouch = true
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                originalSpeed = playbackSpeed ?: 1f
                // Ramp speed up incrementally to avoid audio filter stutter
                val startSpeed = originalSpeed
                val targetSpeed = multipleSpeedGesture
                val steps = 5
                val stepDelay = 16L // ~one frame per step
                for (i in 1..steps) {
                  val t = i.toFloat() / steps
                  val intermediateSpeed = startSpeed + (targetSpeed - startSpeed) * t
                  MPVLib.setPropertyFloat("speed", intermediateSpeed)
                  if (i < steps) delay(stepDelay)
                }

                if (showDynamicSpeedOverlay) {
                  isDynamicSpeedControlActive = true
                  hasSwipedEnough = false
                  dynamicSpeedStartX = startPosition.x
                  dynamicSpeedStartValue = multipleSpeedGesture
                  lastAppliedSpeed = multipleSpeedGesture
                  viewModel.playerUpdate.update { PlayerUpdates.DynamicSpeedControl(multipleSpeedGesture, false) }
                } else {
                  viewModel.playerUpdate.update { PlayerUpdates.MultipleSpeed }
                }
            }
          }

          var gestureType: String? = null

          do {
            val event = awaitPointerEvent()
            val pointerCount = event.changes.count { it.pressed }

            if (pointerCount == 1) {
              event.changes.forEach { change ->
                if (change.pressed) {
                  // Subtitle drag: the hold started in the subtitle band, so
                  // vertical movement repositions subtitles (drag up moves
                  // them up). Horizontal drift is ignored.
                  if (subtitleDragActive) {
                    // Scale the drag against the video height (not the full
                    // container) so subtitles track the finger 1:1.
                    val (_, gestureVideoHeight) = videoRectTopAndHeight(
                      gestureAreaWidth,
                      gestureAreaHeight,
                      gestureVideoAspect,
                    )
                    val dragOffset = (change.position.y - startPosition.y) / gestureVideoHeight * 100f
                    val newPos = (subtitleDragStartSubPos + dragOffset).roundToInt().coerceIn(0, 110)
                    if (newPos != lastSubtitlePos) {
                      lastSubtitlePos = newPos
                      MPVLib.setPropertyInt("sub-pos", newPos)
                      MPVLib.setPropertyInt("secondary-sub-pos", (newPos - 10).coerceIn(0, 110))
                      viewModel.playerUpdate.update { PlayerUpdates.SubtitlePosition(newPos) }
                    }
                    change.consume()
                    return@forEach
                  }

                  val currentPosition = change.position
                  val deltaX = currentPosition.x - startPosition.x
                  val deltaY = currentPosition.y - startPosition.y

                  // Determine gesture type based on initial drag direction
                  if (gestureType == null && (abs(deltaX) > 20f || abs(deltaY) > 20f)) {
                    // Cancel long press if drag started
                    longPressJob.cancel()

                    // Check if we're in long press mode with dynamic speed control
                    if (isLongPressing && isDynamicSpeedControlActive && showDynamicSpeedOverlay && abs(deltaX) > 10f) {
                      gestureType = "speed_control"
                    } else {
                      gestureType = if (abs(deltaX) > abs(deltaY) * 1.5f) {
                        "horizontal"
                      } else if (abs(deltaY) > abs(deltaX) * 1.5f) {
                        "vertical"
                      } else {
                        null
                      }
                    }

                    // Initialize gesture-specific state
                    when (gestureType) {
                      "speed_control" -> {
                        dynamicSpeedStartX = currentPosition.x
                        dynamicSpeedStartValue = MPVLib.getPropertyFloat("speed") ?: multipleSpeedGesture
                      }
                      "vertical" -> {
                        if ((brightnessGesture || volumeGesture) && !isLongPressing) {
                          isVerticalGestureActive = true
                          startingY = 0f
                          mpvVolumeStartingY = 0f
                          originalVolume = currentVolume
                          originalMPVVolume = currentMPVVolume
                          originalBrightness = currentBrightness
                          lastVolumeValue = currentVolume
                          lastMPVVolumeValue = currentMPVVolume ?: 100
                          lastBrightnessValue = currentBrightness
                        }
                      }
                    }
                  }

                  // Handle the appropriate gesture
                  when (gestureType) {
                    "speed_control" -> {
                      if (!showDynamicSpeedOverlay) return@forEach
                      if (isLongPressing && isDynamicSpeedControlActive && paused == false) {
                        change.consume()

                        val speedPresets = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f)
                        val screenWidth = size.width.toFloat()

                        val deltaX = currentPosition.x - dynamicSpeedStartX
                        val swipeDetectionThreshold = 10.dp.toPx()

                        if (!hasSwipedEnough && abs(deltaX) >= swipeDetectionThreshold) {
                          hasSwipedEnough = true
                          viewModel.playerUpdate.update { PlayerUpdates.DynamicSpeedControl(lastAppliedSpeed, true) }
                        }

                        if (hasSwipedEnough) {
                          val presetsRange = speedPresets.size - 1
                          val indexDelta = (deltaX / screenWidth) * presetsRange * 3.5f

                          val startIndex = speedPresets.indexOfFirst {
                            abs(it - dynamicSpeedStartValue) < 0.01f
                          }.takeIf { it >= 0 } ?: 4

                          val newIndex = (startIndex + indexDelta.toInt()).coerceIn(0, speedPresets.size - 1)
                          val newSpeed = speedPresets[newIndex]

                          if (abs(lastAppliedSpeed - newSpeed) > 0.01f) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastAppliedSpeed = newSpeed
                            MPVLib.setPropertyFloat("speed", newSpeed)
                            viewModel.playerUpdate.update { PlayerUpdates.DynamicSpeedControl(newSpeed, true) }
                          }
                        }
                      }
                    }
                    "vertical" -> {
                      if ((brightnessGesture || volumeGesture) && !isLongPressing) {
                        val amount = currentPosition.y - startPosition.y

                        val changeVolume: () -> Unit = {
                          val isIncreasingVolumeBoost: (Float) -> Boolean = {
                            volumeBoostingCap > 0 && currentVolume == viewModel.maxVolume &&
                              (currentMPVVolume ?: 100) - 100 < volumeBoostingCap && amount < 0
                          }
                          val isDecreasingVolumeBoost: (Float) -> Boolean = {
                            volumeBoostingCap > 0 && currentVolume == viewModel.maxVolume &&
                              (currentMPVVolume ?: 100) - 100 in 1..volumeBoostingCap && amount > 0
                          }

                          if (isIncreasingVolumeBoost(amount) || isDecreasingVolumeBoost(amount)) {
                            if (mpvVolumeStartingY == 0f) {
                              startingY = 0f
                              originalVolume = currentVolume
                              mpvVolumeStartingY = currentPosition.y
                            }
                            val newMPVVolume = calculateNewVerticalGestureValue(
                              originalMPVVolume ?: 100,
                              mpvVolumeStartingY,
                              currentPosition.y,
                              mpvVolumeGestureSens,
                            ).coerceIn(100..volumeBoostingCap + 100)

                            if (newMPVVolume != lastMPVVolumeValue) {
                              viewModel.changeMPVVolumeTo(newMPVVolume)
                              lastMPVVolumeValue = newMPVVolume
                            }
                          } else {
                            if (startingY == 0f) {
                              mpvVolumeStartingY = 0f
                              originalMPVVolume = currentMPVVolume
                              startingY = currentPosition.y
                            }
                            val newVolume = calculateNewVerticalGestureValue(
                              originalVolume,
                              startingY,
                              currentPosition.y,
                              volumeGestureSens,
                            )

                            if (newVolume != lastVolumeValue) {
                              viewModel.changeVolumeTo(newVolume)
                              lastVolumeValue = newVolume
                            }
                          }

                          viewModel.displayVolumeSlider()
                        }
                        val changeBrightness: () -> Unit = {
                          if (startingY == 0f) startingY = currentPosition.y
                          val newBrightness = calculateNewVerticalGestureValue(
                            originalBrightness,
                            startingY,
                            currentPosition.y,
                            brightnessGestureSens,
                          )

                          if (abs(newBrightness - lastBrightnessValue) > 0.001f) {
                            viewModel.changeBrightnessTo(newBrightness)
                            lastBrightnessValue = newBrightness
                          }

                          viewModel.displayBrightnessSlider()
                        }

                        when {
                          volumeGesture && brightnessGesture -> {
                            if (swapVolumeAndBrightness) {
                              if (currentPosition.x > size.width / 2) changeBrightness() else changeVolume()
                            } else {
                              if (currentPosition.x < size.width / 2) changeBrightness() else changeVolume()
                            }
                          }
                          brightnessGesture -> changeBrightness()
                          volumeGesture -> changeVolume()
                          else -> {}
                        }

                        change.consume()
                      }
                    }
                  }
                }
              }
            } else if (pointerCount > 1) {
              // Multi-finger gesture detected
              longPressJob.cancel()
              if (subtitleDragActive && subtitlesPreferences.persistSubPos.get()) {
                subtitlesPreferences.subPos.set(lastSubtitlePos)
              }
              subtitleDragActive = false
              if (gestureType != null) {
                when (gestureType) {
                  "vertical" -> {
                    if (brightnessGesture || volumeGesture) {
                      isVerticalGestureActive = false
                      startingY = 0f
                      lastVolumeValue = currentVolume
                      lastMPVVolumeValue = currentMPVVolume ?: 100
                      lastBrightnessValue = currentBrightness
                    }
                  }
                }
                gestureType = null
              }
              break
            }
          } while (event.changes.any { it.pressed })

          // Handle gesture end
          longPressJob.cancel()

          // Subtitle drag ended: the new position sticks, the pill auto-clears
          if (subtitleDragActive && subtitlesPreferences.persistSubPos.get()) {
            subtitlesPreferences.subPos.set(lastSubtitlePos)
          }
          subtitleDragActive = false

          if (isLongPressing) {
            isLongPressing = false
            isDynamicSpeedControlActive = false
            hasSwipedEnough = false
            if (!speedHoldActive) return@awaitEachGesture
            speedHoldActive = false
            // Ramp speed back down incrementally to avoid audio filter stutter
            val currentSpeed = MPVLib.getPropertyFloat("speed") ?: multipleSpeedGesture
            val targetSpeed = originalSpeed
            val steps = 5
            val stepDelay = 16L
            coroutineScope.launch {
              for (i in 1..steps) {
                val t = i.toFloat() / steps
                val intermediateSpeed = currentSpeed + (targetSpeed - currentSpeed) * t
                MPVLib.setPropertyFloat("speed", intermediateSpeed)
                if (i < steps) delay(stepDelay)
              }
            }
            viewModel.playerUpdate.update { PlayerUpdates.None }
          }

          when (gestureType) {
            "vertical" -> {
              if (brightnessGesture || volumeGesture) {
                isVerticalGestureActive = false
                startingY = 0f
                lastVolumeValue = currentVolume
                lastMPVVolumeValue = currentMPVVolume ?: 100
                lastBrightnessValue = currentBrightness
              }
            }
          }
        }
      }
      .pointerInput(pinchToZoomGesture, panAndZoomEnabled, areControlsLocked, isVerticalGestureActive) {
        if (!pinchToZoomGesture || areControlsLocked || isVerticalGestureActive) return@pointerInput

        // Helper: get video display dimensions at 1x (how mpv fits the video to screen)
        fun videoDisplaySize(): Pair<Float, Float> {
          val sw = size.width.toFloat()
          val sh = size.height.toFloat()
          val va = MPVLib.getPropertyDouble("video-params/aspect")?.toFloat() ?: (sw / sh)
          val sa = sw / sh
          return if (va >= sa) Pair(sw, sw / va) else Pair(sh * va, sh)
        }

        // Helper: apply pan with EMA smoothing and bounds clamping
        fun applyPan(
          dx: Float, dy: Float, scale: Float,
          smoothState: FloatArray, // [smoothX, smoothY, initialized]
          smoothFactor: Float = 0.5f,
        ) {
          val sw = size.width.toFloat()
          val sh = size.height.toFloat()
          if (sw <= 0 || sh <= 0) return
          val (bw, bh) = videoDisplaySize()
          // 1 finger pixel = 1 video pixel
          val curX = MPVLib.getPropertyDouble("video-pan-x")?.toFloat() ?: 0f
          val curY = MPVLib.getPropertyDouble("video-pan-y")?.toFloat() ?: 0f
          val targetX = curX + dx / (bw * scale)
          val targetY = curY + dy / (bh * scale)
          // Initialize smoothing on first call
          if (smoothState[2] == 0f) { smoothState[0] = targetX; smoothState[1] = targetY; smoothState[2] = 1f }
          smoothState[0] += (targetX - smoothState[0]) * smoothFactor
          smoothState[1] += (targetY - smoothState[1]) * smoothFactor
          // Bounds: video edge can't go past screen edge
          val maxPan = ((scale - 1f) / (2f * scale)).coerceAtLeast(0f)
          viewModel.setVideoPan(
            smoothState[0].coerceIn(-maxPan, maxPan),
            smoothState[1].coerceIn(-maxPan, maxPan),
          )
        }

        awaitEachGesture {
          var zoom = 0f
          var gestureStarted = false
          var prevDist = 0f
          var prevMidX = 0f
          var prevMidY = 0f
          val panSmooth = floatArrayOf(0f, 0f, 0f) // smoothX, smoothY, initialized

          awaitFirstDown(requireUnconsumed = false)

          do {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }

            if (pressed.size == 2) {
              val p1 = pressed[0].position
              val p2 = pressed[1].position
              val dx = p2.x - p1.x
              val dy = p2.y - p1.y
              val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
              val midX = (p1.x + p2.x) / 2f
              val midY = (p1.y + p2.y) / 2f

              if (prevDist == 0f) {
                // First frame — capture baseline
                prevDist = dist
                zoom = MPVLib.getPropertyDouble("video-zoom")?.toFloat() ?: 0f
                prevMidX = midX
                prevMidY = midY
              } else {
                // Activate on significant pinch movement
                if (!gestureStarted && abs(dist - prevDist) > 5f) {
                  gestureStarted = true
                  viewModel.playerUpdate.update { PlayerUpdates.VideoZoom }
                }

                if (gestureStarted) {
                  // Per-frame zoom: small delta from previous distance → naturally smooth
                  val zoomDelta = ln((dist / prevDist).toDouble()).toFloat() * 1.2f
                  zoom = (zoom + zoomDelta).coerceIn(-1f, 3f)
                  viewModel.setVideoZoom(zoom)

                  // Simultaneous pan while pinching
                  if (panAndZoomEnabled) {
                    applyPan(midX - prevMidX, midY - prevMidY, 2f.pow(zoom), panSmooth)
                  }
                }

                prevDist = dist
                prevMidX = midX
                prevMidY = midY
              }

              pressed.forEach { it.consume() }
            } else if (pressed.size < 2 && prevDist != 0f) {
              break
            }
          } while (event.changes.any { it.pressed })
        }
      }
      // Single-finger pan (only when Pan & Zoom enabled and zoomed in)
      .pointerInput(panAndZoomEnabled, pinchToZoomGesture, areControlsLocked, isVerticalGestureActive) {
        if (!panAndZoomEnabled || !pinchToZoomGesture || areControlsLocked || isVerticalGestureActive) return@pointerInput

        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          var panning = false
          var prevX = down.position.x
          var prevY = down.position.y
          val startX = prevX
          val startY = prevY
          val panSmooth = floatArrayOf(0f, 0f, 0f)

          // Helper: get video display dimensions at 1x
          fun videoDisplaySize(): Pair<Float, Float> {
            val sw = size.width.toFloat()
            val sh = size.height.toFloat()
            val va = MPVLib.getPropertyDouble("video-params/aspect")?.toFloat() ?: (sw / sh)
            val sa = sw / sh
            return if (va >= sa) Pair(sw, sw / va) else Pair(sh * va, sh)
          }

          do {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }

            if (pressed.size == 1) {
              val change = pressed[0]
              val zoom = MPVLib.getPropertyDouble("video-zoom")?.toFloat() ?: 0f
              if (zoom <= 0f) { continue }

              val pos = change.position

              // Activate after 20px drag threshold
              if (!panning) {
                val d = sqrt((pos.x - startX).let { it * it } + (pos.y - startY).let { it * it })
                if (d > 20f) { panning = true; prevX = pos.x; prevY = pos.y }
              }

              if (panning) {
                val sw = size.width.toFloat()
                val sh = size.height.toFloat()
                if (sw > 0 && sh > 0) {
                  val scale = 2f.pow(zoom)
                  val (bw, bh) = videoDisplaySize()
                  val curX = MPVLib.getPropertyDouble("video-pan-x")?.toFloat() ?: 0f
                  val curY = MPVLib.getPropertyDouble("video-pan-y")?.toFloat() ?: 0f
                  val targetX = curX + (pos.x - prevX) / (bw * scale)
                  val targetY = curY + (pos.y - prevY) / (bh * scale)
                  // Initialize smoothing on first pan frame
                  if (panSmooth[2] == 0f) { panSmooth[0] = targetX; panSmooth[1] = targetY; panSmooth[2] = 1f }
                  panSmooth[0] += (targetX - panSmooth[0]) * 0.5f
                  panSmooth[1] += (targetY - panSmooth[1]) * 0.5f
                  val maxPan = ((scale - 1f) / (2f * scale)).coerceAtLeast(0f)
                  viewModel.setVideoPan(
                    panSmooth[0].coerceIn(-maxPan, maxPan),
                    panSmooth[1].coerceIn(-maxPan, maxPan),
                  )
                  prevX = pos.x
                  prevY = pos.y
                }
                change.consume()
              }
            } else if (pressed.size > 1) {
              break
            }
          } while (event.changes.any { it.pressed })
        }
      }
      .pointerInput(horizontalSwipeToSeek, areControlsLocked, gesturePreferences, isVerticalGestureActive) {
        if (!horizontalSwipeToSeek || areControlsLocked || isVerticalGestureActive) return@pointerInput

        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          val startPosition = down.position
          val startTime = System.currentTimeMillis()

          var gestureType: String? = null
          var hasStartedSeeking = false
          var initialVideoPosition = 0f
          var lastClampedPosition = 0f
          var wasPlayerAlreadyPaused = false
          // Use the sensitivity preference instead of hardcoded value
          val seekSensitivity = horizontalSwipeSensitivity

          do {
            val event = awaitPointerEvent()
            val pointerCount = event.changes.count { it.pressed }

            if (pointerCount == 1) {
              event.changes.forEach { change ->
                if (change.pressed) {
                  val currentPosition = change.position
                  val deltaX = currentPosition.x - startPosition.x
                  val deltaY = currentPosition.y - startPosition.y
                  val timeSinceStart = System.currentTimeMillis() - startTime

                  // Tablet-specific responsive swipe gesture vs standard phone gesture
                  val isGestureTriggered = if (isTablet) {
                    abs(deltaX) > 18f && abs(deltaX) > abs(deltaY) * 1.25f
                  } else {
                    abs(deltaX) > 30f && abs(deltaX) > abs(deltaY) * 2f && timeSinceStart > 100L
                  }

                  if (gestureType == null &&
                      isGestureTriggered &&
                      !isLongPressing && // Don't conflict with long press
                      !isDynamicSpeedControlActive && // Don't conflict with speed control
                      panelShown == Panels.None) { // Only when no panels are shown

                    gestureType = "horizontal_seek"
                    hasStartedSeeking = true
                    initialVideoPosition = position?.toFloat() ?: 0f

                    // Pause before seeking to prevent decoder stalls
                    wasPlayerAlreadyPaused = paused ?: false
                    if (!wasPlayerAlreadyPaused) {
                      viewModel.pause()
                    }

                    // Show seekbar and start seeking mode (same as seekbar scrubbing)
                    viewModel.showSeekBar()
                    change.consume()
                  }

                  if (gestureType == "horizontal_seek" && hasStartedSeeking) {
                    // Calculate seek amount based on horizontal movement
                    val seekAmount = deltaX * seekSensitivity
                    val targetPosition = (initialVideoPosition + seekAmount).coerceAtLeast(0f)
                    val maxDuration = duration?.toFloat() ?: 0f
                    val clampedPosition = targetPosition.coerceAtMost(maxDuration)
                    lastClampedPosition = clampedPosition

                    // Quantize to whole seconds with rounding (not truncation) so tiny
                    // finger jitter around a second boundary doesn't flip the display
                    // back and forth. This single value drives the mpv seek, the
                    // center pill and the seekbar preview so they can't disagree.
                    val previewSecond = clampedPosition.roundToInt()
                    val currentPos = previewSecond
                    val seekDelta = previewSecond - initialVideoPosition.roundToInt()

                    // Fast keyframe seek during active drag for fluid 60fps preview updates
                    viewModel.seekTo(previewSecond, isScrubbing = true)

                    // Format and display time position updates
                    val currentTimeStr = formatSeekTime(currentPos)

                    // Format seek delta with +/- prefix
                    val deltaStr = if (seekDelta >= 0) {
                      "+${formatSeekTime(seekDelta)}"
                    } else {
                      "-${formatSeekTime(-seekDelta)}"
                    }

                    // Use PlayerUpdates system like zoom updates
                    viewModel.playerUpdate.update {
                      PlayerUpdates.HorizontalSeek(currentTimeStr, deltaStr, previewSecond.toFloat())
                    }

                    change.consume()
                  }
                }
              }
            } else if (pointerCount > 1) {
              // Multi-finger detected, cancel horizontal seek
              if (hasStartedSeeking) {
                hasStartedSeeking = false
                // Clean up seeking state without showing controls
                viewModel.playerUpdate.update { PlayerUpdates.None }
                viewModel.hideSeekBar()
              }
              break
            }
          } while (event.changes.any { it.pressed })

          // Apply the final exact seek when gesture ends
          if (hasStartedSeeking) {
            viewModel.seekTo(lastClampedPosition.roundToInt(), isScrubbing = false)

            // Unpause if it wasn't paused before seeking
            if (!wasPlayerAlreadyPaused) {
              viewModel.unpause()
            }

            // Clear the horizontal seek update and hide seekbar after a short delay
            coroutineScope.launch {
              delay(300)
              viewModel.playerUpdate.update { PlayerUpdates.None }
              viewModel.hideSeekBar()
            }
          }
        }
      },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoubleTapToSeekOvals(
  amount: Int,
  text: String?,
  showOvals: Boolean,
  showSeekIcon: Boolean,
  showSeekTime: Boolean,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
) {
  val gesturePreferences = koinInject<GesturePreferences>()
  val doubleTapSeekAreaWidth by gesturePreferences.doubleTapSeekAreaWidth.collectAsState()
  val seekAreaFraction = doubleTapSeekAreaWidth / 100f

  val alpha by animateFloatAsState(if (amount == 0) 0f else 0.2f, label = "double_tap_animation_alpha")

  // Scale animation for text
  var scaleTarget by remember { mutableStateOf(1f) }
  val scale by animateFloatAsState(
      targetValue = scaleTarget,
      animationSpec = tween(durationMillis = 150),
      label = "text_scale"
  )

  LaunchedEffect(amount) {
      if (amount != 0) {
          scaleTarget = 1.2f
          delay(100)
          scaleTarget = 1f
      } else {
        scaleTarget = 1f
      }
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = if (amount > 0) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    CompositionLocalProvider(
      LocalRippleConfiguration provides playerRippleConfiguration,
    ) {
      if (amount != 0) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(seekAreaFraction),
          contentAlignment = Alignment.Center,
        ) {
          if (showOvals) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(if (amount > 0) RightSideOvalShape else LeftSideOvalShape)
                .background(Color.White.copy(alpha))
                .indication(interactionSource, ripple()),
            )
          }
          if (showSeekIcon || showSeekTime) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (amount < 0) {
                    CombiningChevronsAnimation(isRight = false, trigger = amount)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "- ${abs(amount)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        modifier = Modifier.scale(scale)
                    )
                } else {
                    Text(
                        text = "+ ${abs(amount)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        modifier = Modifier.scale(scale)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CombiningChevronsAnimation(isRight = true, trigger = amount)
                }
            }
          }
        }
      }
    }
  }
}

fun calculateNewVerticalGestureValue(originalValue: Int, startingY: Float, newY: Float, sensitivity: Float): Int {
  return originalValue + ((startingY - newY) * sensitivity).toInt()
}

fun calculateNewVerticalGestureValue(originalValue: Float, startingY: Float, newY: Float, sensitivity: Float): Float {
  return originalValue + ((startingY - newY) * sensitivity)
}

private fun formatSeekTime(seconds: Int): String {
  val absSeconds = kotlin.math.abs(seconds)
  val hours = absSeconds / 3600
  val minutes = (absSeconds % 3600) / 60
  val secs = absSeconds % 60
  return if (hours > 0) {
    String.format("%d:%02d:%02d", hours, minutes, secs)
  } else {
    String.format("%02d:%02d", minutes, secs)
  }
}

@Composable
fun CombiningChevronsAnimation(
    isRight: Boolean,
    trigger: Int,
    modifier: Modifier = Modifier
) {
    // List of active animations (unique IDs)
    val animations = remember { mutableStateListOf<Long>() }

    // Fire a new animation whenever trigger changes
    LaunchedEffect(trigger) {
        animations.add(System.nanoTime())
    }

    Row(modifier = modifier) {
        Box {
             // Static Chevron
             Icon(
                imageVector = if (isRight) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )

            // Render active moving chevrons
            animations.forEach { animId ->
                key(animId) {
                    MovingChevron(
                        isRight = isRight,
                        onFinished = { animations.remove(animId) }
                    )
                }
            }
        }
    }
}

@Composable
fun MovingChevron(
    isRight: Boolean,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(250, easing = LinearEasing)
        )
        onFinished()
    }

    val startOffset = if (isRight) -15f else 15f
    val currentOffset = startOffset * (1f - progress.value)
    val alpha = 1f - progress.value

    Icon(
        imageVector = if (isRight) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowLeft,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier
            .size(48.dp)
            .alpha(alpha)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(x = currentOffset.dp.roundToPx(), y = 0)
                }
            }
    )
}
