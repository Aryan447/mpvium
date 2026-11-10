package app.aryan447.mpvium.ui.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.PlayerButton
import app.aryan447.mpvium.preferences.SeekbarStyle
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.ui.player.Panels
import app.aryan447.mpvium.ui.player.PlayerActivity
import app.aryan447.mpvium.ui.player.PlayerViewModel
import app.aryan447.mpvium.ui.player.SeekCacheResolver
import app.aryan447.mpvium.ui.player.Sheets
import app.aryan447.mpvium.ui.player.VideoAspect
import app.aryan447.mpvium.utils.media.MediaInfoParser
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/**
 * Modern player layout modeled after the Apple TV+ native interface.
 *
 * Characteristics:
 * - Top-left: Floating dark glass pill with Close (✕) and PiP buttons.
 * - Top-right: Floating dark glass volume capsule with horizontal level slider and speaker icon.
 * - Center screen: Floating circular glass transport controls:
 *     [ Replay 10s (↺10) | Hero Play/Pause | Forward 10s (↻10) ]
 * - Bottom area:
 *     - Bottom-left: Show header. When TV show: Episode name on top line, Series title with chevron (›) on bottom line.
 *                    When Movie: Only the movie title on a single clean line.
 *     - Bottom-right: Floating dark glass actions pill [ Subtitles | Audio waveform | Speed/More ].
 *     - Seekbar: Full-width progress track with chapter dividers and buffer band.
 *     - Timers: Elapsed time on left, remaining duration on right directly underneath the track.
 * - Safely elevated above navigation / gesture insets with .systemGestureExclusion() so horizontal
 *   scrubbing never triggers Android system gestures.
 */
@Composable
fun ModernPlayerControls(
  viewModel: PlayerViewModel,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  onOpenPanel: (Panels) -> Unit,
  activity: PlayerActivity,
  controlsShown: Boolean,
  areControlsLocked: Boolean,
  isSeeking: Boolean,
  onSeekingChanged: (Boolean) -> Unit,
  resetControlsTimestamp: () -> Unit,
  paused: Boolean,
  position: Float,
  duration: Float,
  isSwipeSeeking: Boolean,
  chapters: ImmutableList<Segment>,
  currentChapter: Int?,
  cacheRanges: ImmutableList<SeekCacheResolver.SeekableRange>,
  forwardCacheDuration: Float,
  isLeanSeekCacheEnabled: Boolean,
  invertDuration: Boolean,
  onToggleInvertDuration: () -> Unit,
  mediaTitle: String = "",
  tmdbEpisodeTitle: String? = null,
  topRightButtons: List<PlayerButton> = emptyList(),
  bottomRightButtons: List<PlayerButton> = emptyList(),
  actionButtons: List<PlayerButton> = emptyList(),
  amoledMode: Boolean = false,
  showSystemStatusBar: Boolean = false,
  showSystemNavigationBar: Boolean = false,
  tabletBottomOffset: Dp = 0.dp,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val configuration = LocalConfiguration.current
  val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

  val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
  val navBarBottom = navBarPadding.calculateBottomPadding()
  val systemGesturesPadding = WindowInsets.systemGestures.asPaddingValues()
  val systemGestureBottom = systemGesturesPadding.calculateBottomPadding()

  // Ensure bottom clearance exceeds both system gesture bars and nav bars
  val gestureSafeBottom = maxOf(
    systemGestureBottom,
    navBarBottom,
    if (isPortrait) 20.dp else 24.dp,
  )

  val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
  val topSafeInset = if (showSystemStatusBar) statusBarPadding.calculateTopPadding() + 8.dp else 16.dp

  val navBarLeft = navBarPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
  val navBarRight = navBarPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
  val startSafeInset = maxOf(navBarLeft, if (isPortrait) 16.dp else 24.dp)
  val endSafeInset = maxOf(navBarRight, if (isPortrait) 16.dp else 24.dp)

  val surfaceColor = if (amoledMode) Color(0xCC0A0B0E) else Color(0x9916181D)
  val borderStroke = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))

  // Parse title to identify TV show vs Movie
  val parsedInfo = remember(mediaTitle) {
    if (mediaTitle.isNotBlank() && !mediaTitle.contains("://")) {
      MediaInfoParser.parse(mediaTitle)
    } else {
      null
    }
  }

  val isTvShow = parsedInfo?.type == "tv"
  val episodeTitle = if (isTvShow) {
    parsedInfo?.episodeTitle?.takeIf { it.isNotBlank() }
      ?: tmdbEpisodeTitle?.takeIf { it.isNotBlank() }
      ?: (if (parsedInfo?.season != null && parsedInfo.episode != null) "S${parsedInfo.season}:E${parsedInfo.episode}" else null)
  } else {
    null
  }
  val seriesOrMovieTitle = parsedInfo?.title?.takeIf { it.isNotBlank() } ?: mediaTitle

  Box(
    modifier = modifier.fillMaxSize(),
  ) {
    // -------------------------------------------------------------
    // TOP BAR: Top-Left Pill [ ✕ | PiP ] & Top-Right Volume Pill
    // -------------------------------------------------------------
    AnimatedVisibility(
      visible = controlsShown && !areControlsLocked,
      enter = slideInVertically(playerControlsEnterAnimationSpec()) { -it } + fadeIn(playerControlsEnterAnimationSpec()),
      exit = slideOutVertically(playerControlsExitAnimationSpec()) { -it } + fadeOut(playerControlsExitAnimationSpec()),
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = topSafeInset, start = startSafeInset, end = endSafeInset),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Top-left Glass Pill: [ Close (✕) | PiP ]
        Surface(
          modifier = Modifier.height(42.dp),
          shape = RoundedCornerShape(21.dp),
          color = surfaceColor,
          border = borderStroke,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            // Close Button (✕)
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = Color.White,
              modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = ripple(bounded = true, radius = 18.dp),
                ) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  resetControlsTimestamp()
                  onBackPress()
                },
            )

            // PiP Button
            Icon(
              imageVector = Icons.Default.PictureInPictureAlt,
              contentDescription = "Picture in Picture",
              tint = Color.White,
              modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = ripple(bounded = true, radius = 18.dp),
                ) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  resetControlsTimestamp()
                  activity.enterPipModeHidingOverlay()
                },
            )
          }
        }

        // Top-right Expandable Volume & Brightness Controls
        ModernTopRightControls(
          viewModel = viewModel,
          resetControlsTimestamp = resetControlsTimestamp,
          surfaceColor = surfaceColor,
          borderStroke = borderStroke,
        )
      }
    }

    // -------------------------------------------------------------
    // CENTER TRANSPORT CONTROLS: [ ↺10 ]  [ ▶ / ⏸ ]  [ ↻10 ]
    // -------------------------------------------------------------
    AnimatedVisibility(
      visible = controlsShown && !areControlsLocked,
      enter = fadeIn(playerControlsEnterAnimationSpec()),
      exit = fadeOut(playerControlsExitAnimationSpec()),
      modifier = Modifier.align(Alignment.Center),
    ) {
      ModernCenterTransportControls(
        paused = paused,
        onPlayPause = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          resetControlsTimestamp()
          viewModel.pauseUnpause()
        },
        onRewind = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          resetControlsTimestamp()
          viewModel.seekBy(-10)
        },
        onForward = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          resetControlsTimestamp()
          viewModel.seekBy(10)
        },
        surfaceColor = surfaceColor,
        borderStroke = borderStroke,
      )
    }

    // -------------------------------------------------------------
    // BOTTOM SECTION: Title + Actions + Full-Width Seekbar & Timers
    // -------------------------------------------------------------
    AnimatedVisibility(
      visible = controlsShown && !areControlsLocked,
      enter = slideInVertically(playerControlsEnterAnimationSpec()) { it } + fadeIn(playerControlsEnterAnimationSpec()),
      exit = slideOutVertically(playerControlsExitAnimationSpec()) { it } + fadeOut(playerControlsExitAnimationSpec()),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(
          start = startSafeInset,
          end = endSafeInset,
          bottom = gestureSafeBottom + tabletBottomOffset,
        ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Row above seekbar: Title on left, Actions pill on right
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom,
        ) {
          // Title Block (Episode name on top, Series name below with chevron; or Movie name only)
          if (isTvShow && episodeTitle != null) {
            Column(
              verticalArrangement = Arrangement.spacedBy(2.dp),
              modifier = Modifier.weight(1f, fill = false),
            ) {
              Text(
                text = episodeTitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = Color.White.copy(alpha = 0.80f),
                  fontWeight = FontWeight.Normal,
                  fontSize = 14.sp,
                  shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 4f),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    resetControlsTimestamp()
                    if (viewModel.hasPlaylistSupport()) {
                      onOpenSheet(Sheets.Playlist)
                    } else {
                      onOpenSheet(Sheets.Chapters)
                    }
                  },
              ) {
                Text(
                  text = seriesOrMovieTitle,
                  style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 6f),
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Icon(
                  imageVector = Icons.Filled.ChevronRight,
                  contentDescription = "Episodes / Chapters",
                  tint = Color.White.copy(alpha = 0.80f),
                  modifier = Modifier.size(22.dp),
                )
              }
            }
          } else {
            // Movie: Single line with movie name only
            Column(
              modifier = Modifier.weight(1f, fill = false),
            ) {
              Text(
                text = seriesOrMovieTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp,
                  shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 6f),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Spacer(modifier = Modifier.width(16.dp))

          // Bottom-Right Actions Pill: [ Subtitles | Audio | Explain | Menu ]
          ModernBottomActionsPill(
            buttons = if (actionButtons.isNotEmpty()) actionButtons else bottomRightButtons,
            onOpenSubtitles = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              resetControlsTimestamp()
              onOpenSheet(Sheets.SubtitleTracks)
            },
            onOpenAudio = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              resetControlsTimestamp()
              onOpenSheet(Sheets.AudioTracks)
            },
            onOpenExplain = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              resetControlsTimestamp()
              onOpenSheet(Sheets.Explain)
            },
            onOpenMore = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              resetControlsTimestamp()
              onOpenSheet(Sheets.More)
            },
            onOpenSheet = onOpenSheet,
            onOpenPanel = onOpenPanel,
            viewModel = viewModel,
            activity = activity,
            chapters = chapters,
            currentChapter = currentChapter,
            surfaceColor = surfaceColor,
            borderStroke = borderStroke,
            resetControlsTimestamp = resetControlsTimestamp,
          )
        }

        // Seekbar track and timers
        var wasPlayerAlreadyPaused by remember { mutableStateOf(false) }
        ModernSeekbarSection(
          position = position,
          duration = duration,
          chapters = chapters,
          cacheRanges = cacheRanges,
          isPaused = paused,
          onSeekTo = { target, isScrubbing ->
            resetControlsTimestamp()
            if (isScrubbing) {
              if (!isSeeking) {
                wasPlayerAlreadyPaused = paused
              }
              viewModel.seekTo(target.roundToInt(), isScrubbing = true)
            } else {
              if (wasPlayerAlreadyPaused || paused) {
                viewModel.seekTo(target.roundToInt(), isScrubbing = false)
              } else {
                viewModel.seekToAndResume(target.roundToInt())
              }
            }
          },
          onSeekingChanged = onSeekingChanged,
          invertDuration = invertDuration,
          onToggleInvertDuration = onToggleInvertDuration,
        )
      }
    }
  }
}

enum class ModernTopRightTarget {
  VOLUME,
  BRIGHTNESS,
}

/**
 * Top-right expandable controls with Volume (left) and Brightness (right):
 * - When Volume is expanded:
 *     volume expanded (brightness minimized round icon)
 * - When Brightness is expanded:
 *     (volume minimized round icon) brightness expanded
 *
 * Smooth animated transition between the two states:
 * - Volume width animates between 164.dp (expanded) and 42.dp (minimized)
 * - Brightness width animates between 42.dp (minimized) and 164.dp (expanded)
 * - Tapping either minimized button smoothly switches the expanded focus
 * - Scrubbing/dragging either slider dynamically swaps the icon with the live percentage readout
 */
@Composable
fun ModernTopRightControls(
  viewModel: PlayerViewModel,
  resetControlsTimestamp: () -> Unit,
  surfaceColor: Color,
  borderStroke: BorderStroke,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val currentVolume by viewModel.currentVolume.collectAsState()
  val maxVolume = viewModel.maxVolume.coerceAtLeast(1)
  var previousVolume by remember { mutableIntStateOf(currentVolume.takeIf { it > 0 } ?: (maxVolume / 2)) }

  val currentBrightness by viewModel.currentBrightness.collectAsState()

  var activeTarget by remember { mutableStateOf(ModernTopRightTarget.VOLUME) }
  var isScrubbingVolume by remember { mutableStateOf(false) }
  var isScrubbingBrightness by remember { mutableStateOf(false) }

  val isVolumeSliderShown by viewModel.isVolumeSliderShown.collectAsState()
  val isBrightnessSliderShown by viewModel.isBrightnessSliderShown.collectAsState()

  var showVolumeNumberTemporarily by remember { mutableStateOf(false) }
  var showBrightnessNumberTemporarily by remember { mutableStateOf(false) }
  var lastObservedVolume by remember { mutableIntStateOf(currentVolume) }
  LaunchedEffect(currentVolume) {
    if (lastObservedVolume != currentVolume) {
      lastObservedVolume = currentVolume
      showBrightnessNumberTemporarily = false
      activeTarget = ModernTopRightTarget.VOLUME
      showVolumeNumberTemporarily = true
      delay(1000)
      showVolumeNumberTemporarily = false
    }
  }

  var lastObservedBrightness by remember { mutableFloatStateOf(currentBrightness) }
  LaunchedEffect(currentBrightness) {
    if (kotlin.math.abs(lastObservedBrightness - currentBrightness) > 0.005f) {
      lastObservedBrightness = currentBrightness
      showVolumeNumberTemporarily = false
      activeTarget = ModernTopRightTarget.BRIGHTNESS
      showBrightnessNumberTemporarily = true
      delay(1000)
      showBrightnessNumberTemporarily = false
      if (!isBrightnessSliderShown) {
        activeTarget = ModernTopRightTarget.VOLUME
      }
    }
  }

  LaunchedEffect(isBrightnessSliderShown) {
    if (isBrightnessSliderShown) {
      activeTarget = ModernTopRightTarget.BRIGHTNESS
      showVolumeNumberTemporarily = false
    } else {
      showBrightnessNumberTemporarily = false
      activeTarget = ModernTopRightTarget.VOLUME
    }
  }

  LaunchedEffect(isVolumeSliderShown) {
    if (isVolumeSliderShown) {
      activeTarget = ModernTopRightTarget.VOLUME
      showBrightnessNumberTemporarily = false
    } else {
      showVolumeNumberTemporarily = false
    }
  }

  val isVolumeAdjusting = isScrubbingVolume || showVolumeNumberTemporarily || isVolumeSliderShown
  val isBrightnessAdjusting = isScrubbingBrightness || showBrightnessNumberTemporarily || isBrightnessSliderShown

  val showBrightnessIconOnVolume = isBrightnessAdjusting && !isVolumeAdjusting
  val showVolumeIconOnBrightness = isVolumeAdjusting && !isBrightnessAdjusting

  var volTrackWidthPx by remember { mutableIntStateOf(0) }
  var briTrackWidthPx by remember { mutableIntStateOf(0) }

  val volumeFraction = (currentVolume.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f)
  val brightnessFraction = currentBrightness.coerceIn(0f, 1f)

  val themePrimary = MaterialTheme.colorScheme.primary

  val speakerIcon = when {
    currentVolume == 0 -> Icons.AutoMirrored.Filled.VolumeMute
    currentVolume < maxVolume / 2 -> Icons.AutoMirrored.Filled.VolumeDown
    else -> Icons.AutoMirrored.Filled.VolumeUp
  }

  val isVolumeExpanded = activeTarget == ModernTopRightTarget.VOLUME
  val isBrightnessExpanded = !isVolumeExpanded

  val transitionSpec = tween<Dp>(durationMillis = 300, easing = FastOutSlowInEasing)
  val volumeWidth by animateDpAsState(
    targetValue = if (isVolumeExpanded) 164.dp else 42.dp,
    animationSpec = transitionSpec,
    label = "ModernVolumeWidth",
  )
  val brightnessWidth by animateDpAsState(
    targetValue = if (isBrightnessExpanded) 164.dp else 42.dp,
    animationSpec = transitionSpec,
    label = "ModernBrightnessWidth",
  )

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // 1. Volume (Always Left)
    Surface(
      modifier = Modifier
        .width(volumeWidth)
        .height(42.dp)
        .clip(RoundedCornerShape(21.dp))
        .then(
          if (!isVolumeExpanded) {
            Modifier.clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = true, radius = 21.dp),
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                resetControlsTimestamp()
                showBrightnessNumberTemporarily = false
                viewModel.isBrightnessSliderShown.value = false
                activeTarget = ModernTopRightTarget.VOLUME
              },
            )
          } else {
            Modifier
          }
        ),
      shape = RoundedCornerShape(21.dp),
      color = surfaceColor,
      border = borderStroke,
    ) {
      Crossfade(
        targetState = isVolumeExpanded,
        animationSpec = tween(180),
        label = "ModernVolumeContent",
      ) { expanded ->
        if (!expanded) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = if (showBrightnessIconOnVolume) Icons.Default.BrightnessMedium else speakerIcon,
              contentDescription = if (showBrightnessIconOnVolume) "Brightness" else "Switch to Volume slider",
              tint = Color.White,
              modifier = Modifier.size(20.dp),
            )
          }
        } else {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // Horizontal volume slider track
            Box(
              modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .onSizeChanged { volTrackWidthPx = it.width }
                .pointerInput(maxVolume) {
                  detectTapGestures { offset ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    resetControlsTimestamp()
                    if (volTrackWidthPx > 0) {
                      val fraction = (offset.x / volTrackWidthPx).coerceIn(0f, 1f)
                      val newVol = (fraction * maxVolume).roundToInt()
                      viewModel.changeVolumeTo(newVol)
                      if (newVol > 0) previousVolume = newVol
                      viewModel.displayVolumeSlider()
                    }
                  }
                }
                .pointerInput(maxVolume) {
                  detectDragGestures(
                    onDragStart = { offset ->
                      isScrubbingVolume = true
                      showBrightnessNumberTemporarily = false
                      viewModel.isBrightnessSliderShown.value = false
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      resetControlsTimestamp()
                      if (volTrackWidthPx > 0) {
                        val fraction = (offset.x / volTrackWidthPx).coerceIn(0f, 1f)
                        val newVol = (fraction * maxVolume).roundToInt()
                        viewModel.changeVolumeTo(newVol)
                        if (newVol > 0) previousVolume = newVol
                      }
                    },
                    onDragEnd = {
                      isScrubbingVolume = false
                      viewModel.displayVolumeSlider()
                    },
                    onDragCancel = {
                      isScrubbingVolume = false
                      viewModel.displayVolumeSlider()
                    },
                    onDrag = { change, _ ->
                      change.consume()
                      resetControlsTimestamp()
                      if (volTrackWidthPx > 0) {
                        val fraction = (change.position.x / volTrackWidthPx).coerceIn(0f, 1f)
                        val newVol = (fraction * maxVolume).roundToInt()
                        viewModel.changeVolumeTo(newVol)
                        if (newVol > 0) previousVolume = newVol
                      }
                    },
                  )
                },
              contentAlignment = Alignment.CenterStart,
            ) {
              Canvas(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(5.dp),
              ) {
                val w = size.width
                val h = size.height
                val radius = CornerRadius(h / 2f, h / 2f)

                // Unfilled / remaining track
                drawRoundRect(
                  color = Color.White.copy(alpha = 0.28f),
                  size = size,
                  cornerRadius = radius,
                )

                // Filled active volume with theme primary color
                val filledWidth = volumeFraction * w
                if (filledWidth > 0.5f) {
                  drawRoundRect(
                    color = themePrimary,
                    size = Size(filledWidth, h),
                    cornerRadius = radius,
                  )
                }
              }
            }

            // Trailing slot: if scrubbing or volume changed, display number; otherwise speaker icon
            Box(
              modifier = Modifier.widthIn(min = 28.dp),
              contentAlignment = Alignment.Center,
            ) {
              if (isVolumeAdjusting) {
                Text(
                  text = "${(volumeFraction * 100).roundToInt()}%",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                  ),
                )
              } else {
                Icon(
                  imageVector = speakerIcon,
                  contentDescription = "Mute / Unmute",
                  tint = Color.White,
                  modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      resetControlsTimestamp()
                      if (currentVolume > 0) {
                        previousVolume = currentVolume
                        viewModel.changeVolumeTo(0)
                      } else {
                        viewModel.changeVolumeTo(previousVolume.takeIf { it > 0 } ?: (maxVolume / 2))
                      }
                    },
                )
              }
            }
          }
        }
      }
    }

    // 2. Brightness (Always Right)
    Surface(
      modifier = Modifier
        .width(brightnessWidth)
        .height(42.dp)
        .clip(RoundedCornerShape(21.dp))
        .then(
          if (!isBrightnessExpanded) {
            Modifier.clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = true, radius = 21.dp),
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                resetControlsTimestamp()
                showVolumeNumberTemporarily = false
                viewModel.isVolumeSliderShown.value = false
                activeTarget = ModernTopRightTarget.BRIGHTNESS
              },
            )
          } else {
            Modifier
          }
        ),
      shape = RoundedCornerShape(21.dp),
      color = surfaceColor,
      border = borderStroke,
    ) {
      Crossfade(
        targetState = isBrightnessExpanded,
        animationSpec = tween(180),
        label = "ModernBrightnessContent",
      ) { expanded ->
        if (!expanded) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = if (showVolumeIconOnBrightness) speakerIcon else Icons.Default.BrightnessMedium,
              contentDescription = if (showVolumeIconOnBrightness) "Volume" else "Switch to Brightness slider",
              tint = Color.White,
              modifier = Modifier.size(20.dp),
            )
          }
        } else {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // Horizontal brightness slider track
            Box(
              modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .onSizeChanged { briTrackWidthPx = it.width }
                .pointerInput(Unit) {
                  detectTapGestures { offset ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    resetControlsTimestamp()
                    if (briTrackWidthPx > 0) {
                      val fraction = (offset.x / briTrackWidthPx).coerceIn(0f, 1f)
                      viewModel.changeBrightnessTo(fraction)
                      viewModel.displayBrightnessSlider()
                    }
                  }
                }
                .pointerInput(Unit) {
                  detectDragGestures(
                    onDragStart = { offset ->
                      isScrubbingBrightness = true
                      showVolumeNumberTemporarily = false
                      viewModel.isVolumeSliderShown.value = false
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      resetControlsTimestamp()
                      if (briTrackWidthPx > 0) {
                        val fraction = (offset.x / briTrackWidthPx).coerceIn(0f, 1f)
                        viewModel.changeBrightnessTo(fraction)
                      }
                    },
                    onDragEnd = {
                      isScrubbingBrightness = false
                      viewModel.displayBrightnessSlider()
                    },
                    onDragCancel = {
                      isScrubbingBrightness = false
                      viewModel.displayBrightnessSlider()
                    },
                    onDrag = { change, _ ->
                      change.consume()
                      resetControlsTimestamp()
                      if (briTrackWidthPx > 0) {
                        val fraction = (change.position.x / briTrackWidthPx).coerceIn(0f, 1f)
                        viewModel.changeBrightnessTo(fraction)
                      }
                    },
                  )
                },
              contentAlignment = Alignment.CenterStart,
            ) {
              Canvas(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(5.dp),
              ) {
                val w = size.width
                val h = size.height
                val radius = CornerRadius(h / 2f, h / 2f)

                // Unfilled track
                drawRoundRect(
                  color = Color.White.copy(alpha = 0.28f),
                  size = size,
                  cornerRadius = radius,
                )

                // Filled brightness track with theme primary color
                val filledWidth = brightnessFraction * w
                if (filledWidth > 0.5f) {
                  drawRoundRect(
                    color = themePrimary,
                    size = Size(filledWidth, h),
                    cornerRadius = radius,
                  )
                }
              }
            }

            // Trailing slot: if scrubbing or brightness changed, display number; otherwise brightness icon
            Box(
              modifier = Modifier.widthIn(min = 28.dp),
              contentAlignment = Alignment.Center,
            ) {
              if (isBrightnessAdjusting) {
                Text(
                  text = "${(brightnessFraction * 100).roundToInt()}%",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                  ),
                )
              } else {
                Icon(
                  imageVector = Icons.Default.BrightnessMedium,
                  contentDescription = "Brightness",
                  tint = Color.White,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Backward compatibility alias for ModernTopRightControls.
 */
@Composable
fun ModernVolumePill(
  viewModel: PlayerViewModel,
  resetControlsTimestamp: () -> Unit,
  surfaceColor: Color,
  borderStroke: BorderStroke,
  modifier: Modifier = Modifier,
) = ModernTopRightControls(
  viewModel = viewModel,
  resetControlsTimestamp = resetControlsTimestamp,
  surfaceColor = surfaceColor,
  borderStroke = borderStroke,
  modifier = modifier,
)

/**
 * Center floating circular glass transport controls:
 * [ Rewind 10s | Hero Play/Pause | Forward 10s ]
 */
@Composable
fun ModernCenterTransportControls(
  paused: Boolean,
  onPlayPause: () -> Unit,
  onRewind: () -> Unit,
  onForward: () -> Unit,
  surfaceColor: Color,
  borderStroke: BorderStroke,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(22.dp),
  ) {
    // Replay 10s
    Surface(
      modifier = Modifier
        .size(52.dp)
        .clip(CircleShape)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple(bounded = true, radius = 26.dp),
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onRewind()
          },
        ),
      shape = CircleShape,
      color = surfaceColor,
      border = borderStroke,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Outlined.Replay10,
          contentDescription = "Rewind 10 seconds",
          tint = Color.White,
          modifier = Modifier.size(25.dp),
        )
      }
    }

    // Play / Pause (Hero button)
    Surface(
      modifier = Modifier
        .size(68.dp)
        .clip(CircleShape)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple(bounded = true, radius = 34.dp),
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onPlayPause()
          },
        ),
      shape = CircleShape,
      color = surfaceColor,
      border = borderStroke,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
          contentDescription = if (paused) "Play" else "Pause",
          tint = Color.White,
          modifier = Modifier.size(36.dp),
        )
      }
    }

    // Forward 10s
    Surface(
      modifier = Modifier
        .size(52.dp)
        .clip(CircleShape)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple(bounded = true, radius = 26.dp),
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onForward()
          },
        ),
      shape = CircleShape,
      color = surfaceColor,
      border = borderStroke,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Outlined.Forward10,
          contentDescription = "Forward 10 seconds",
          tint = Color.White,
          modifier = Modifier.size(25.dp),
        )
      }
    }
  }
}

/**
 * Bottom-right floating glass actions pill:
 * Features Subtitles, Audio tracks, Explain (core feature), and Menu by default,
 * and dynamically adapts to buttons configured in Landscape Controls.
 */
@Composable
fun ModernBottomActionsPill(
  buttons: List<PlayerButton> = emptyList(),
  onOpenSubtitles: () -> Unit,
  onOpenAudio: () -> Unit,
  onOpenExplain: () -> Unit = {},
  onOpenMore: () -> Unit,
  onOpenSheet: (Sheets) -> Unit = {},
  onOpenPanel: (Panels) -> Unit = {},
  viewModel: PlayerViewModel? = null,
  activity: PlayerActivity? = null,
  chapters: List<Segment> = emptyList(),
  currentChapter: Int? = null,
  surfaceColor: Color,
  borderStroke: BorderStroke,
  resetControlsTimestamp: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current

  val displayButtons = if (buttons.isNotEmpty()) {
    buttons
  } else {
    listOf(
      PlayerButton.SUBTITLES,
      PlayerButton.AUDIO_TRACK,
      PlayerButton.EXPLAIN,
      PlayerButton.MORE_OPTIONS,
    )
  }

  Surface(
    modifier = modifier.height(42.dp),
    shape = RoundedCornerShape(21.dp),
    color = surfaceColor,
    border = borderStroke,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      displayButtons.forEach { button ->
        when (button) {
          PlayerButton.SUBTITLES -> {
            Icon(
              imageVector = Icons.Default.Subtitles,
              contentDescription = "Subtitles",
              tint = Color.White,
              modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = ripple(bounded = true, radius = 18.dp),
                  onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenSubtitles()
                  },
                ),
            )
          }

          PlayerButton.AUDIO_TRACK -> {
            Icon(
              imageVector = Icons.Outlined.GraphicEq,
              contentDescription = "Audio Tracks",
              tint = Color.White,
              modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = ripple(bounded = true, radius = 18.dp),
                  onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenAudio()
                  },
                ),
            )
          }

          PlayerButton.EXPLAIN -> {
            Icon(
              imageVector = Icons.Outlined.Translate,
              contentDescription = "Explain",
              tint = Color.White,
              modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = ripple(bounded = true, radius = 18.dp),
                  onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenExplain()
                  },
                ),
            )
          }

          PlayerButton.MORE_OPTIONS -> {
            Icon(
              imageVector = Icons.Outlined.MoreVert,
              contentDescription = "Menu and More Options",
              tint = Color.White,
              modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = ripple(bounded = true, radius = 18.dp),
                  onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenMore()
                  },
                ),
            )
          }

          else -> {
            if (viewModel != null && activity != null) {
              RenderPlayerButton(
                button = button,
                chapters = chapters,
                currentChapter = currentChapter,
                isPortrait = false,
                isSpeedNonOne = false,
                currentZoom = 1f,
                aspect = VideoAspect.Fit,
                mediaTitle = null,
                hideBackground = true,
                decoder = app.aryan447.mpvium.ui.player.Decoder.SW,
                playbackSpeed = 1f,
                onBackPress = {},
                onOpenSheet = { sheet ->
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  resetControlsTimestamp()
                  onOpenSheet(sheet)
                },
                onOpenPanel = { panel ->
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  resetControlsTimestamp()
                  onOpenPanel(panel)
                },
                viewModel = viewModel,
                activity = activity,
                buttonSize = 28.dp,
              )
            } else {
              Icon(
                imageVector = button.icon,
                contentDescription = button.name,
                tint = Color.White,
                modifier = Modifier
                  .size(20.dp)
                  .clip(CircleShape)
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 18.dp),
                    onClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onOpenMore()
                    },
                  ),
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Full-width seekbar track with timers row underneath.
 */
@Composable
fun ModernSeekbarSection(
  position: Float,
  duration: Float,
  chapters: ImmutableList<Segment>,
  cacheRanges: ImmutableList<SeekCacheResolver.SeekableRange>,
  isPaused: Boolean = false,
  onSeekTo: (Float, Boolean) -> Unit,
  onSeekingChanged: (Boolean) -> Unit,
  invertDuration: Boolean,
  onToggleInvertDuration: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current

  Column(
    modifier = modifier
      .fillMaxWidth()
      .systemGestureExclusion(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // Progress bar
    ModernSeekbarTrack(
      position = position,
      duration = duration,
      chapters = chapters,
      cacheRanges = cacheRanges,
      isPaused = isPaused,
      onSeekTo = onSeekTo,
      onSeekingChanged = onSeekingChanged,
    )

    // Timers row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Current position timer
      Text(
        text = Utils.prettyTime(position.toInt(), false),
        style = MaterialTheme.typography.labelSmall.copy(
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
          color = Color.White.copy(alpha = 0.85f),
          shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f),
        ),
      )

      // Remaining / total duration timer
      val rightTimerValue = if (invertDuration) position - duration else duration
      val durationFormatted = Utils.prettyTime(rightTimerValue.toInt(), invertDuration)

      Text(
        text = durationFormatted,
        style = MaterialTheme.typography.labelSmall.copy(
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
          color = Color.White.copy(alpha = 0.85f),
          shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f),
        ),
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .clickable {
            onToggleInvertDuration()
          },
      )
    }
  }
}

/**
 * Interactive progress track inside the Modern player layout.
 * Supports Standard, Wavy, and Thick styles, defaulting to Standard.
 * Adapts to theme colors (MaterialTheme.colorScheme.primary) and respects
 * chaptersInSeekbar preference. Supports vertical swipe up for 1x, 2x, 4x, 8x scrubbing speed.
 */
@Composable
fun ModernSeekbarTrack(
  position: Float,
  duration: Float,
  chapters: ImmutableList<Segment>,
  cacheRanges: ImmutableList<SeekCacheResolver.SeekableRange>,
  isPaused: Boolean = false,
  onSeekTo: (Float, Boolean) -> Unit,
  onSeekingChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val chaptersInSeekbar by appearancePreferences.chaptersInSeekbar.collectAsState()
  val seekbarStyle by appearancePreferences.modernSeekbarStyle.collectAsState()

  var isDragging by remember { mutableStateOf(false) }
  var dragPosition by remember { mutableFloatStateOf(position) }
  var trackWidthPx by remember { mutableIntStateOf(0) }

  val density = LocalDensity.current
  val scrubTierStepPx = with(density) { 48.dp.toPx() }
  var scrubTier by remember { mutableIntStateOf(0) }
  var dragAnchor by remember { mutableFloatStateOf(0f) }
  var scaledDragDxPx by remember { mutableFloatStateOf(0f) }
  var verticalDragPx by remember { mutableFloatStateOf(0f) }
  val scrubRate = 1 shl scrubTier.coerceIn(0, 3)

  val latestDuration by rememberUpdatedState(duration)
  val latestPosition by rememberUpdatedState(position)
  val latestOnSeekTo by rememberUpdatedState(onSeekTo)
  val latestOnSeekingChanged by rememberUpdatedState(onSeekingChanged)

  val displayPosition = if (isDragging) dragPosition else position
  val playedFraction = if (duration > 0f) (displayPosition / duration).coerceIn(0f, 1f) else 0f

  val isWavy = seekbarStyle == SeekbarStyle.Wavy
  val isThick = seekbarStyle == SeekbarStyle.Thick

  val baseHeight = if (isThick) 7.dp else 5.dp
  val expandedHeight = if (isThick) 10.dp else 7.dp

  val animatedTrackHeight by animateDpAsState(
    targetValue = if (isDragging) expandedHeight else baseHeight,
    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
    label = "trackHeight",
  )

  // Wave parameters and animation for Wavy style
  var phaseOffset by remember { mutableFloatStateOf(0f) }
  var heightFraction by remember { mutableFloatStateOf(if (isPaused) 0f else 1f) }

  val waveLength = 80f
  val lineAmplitude = 6f
  val phaseSpeed = 10f // px per second
  val transitionPeriods = 1.5f

  LaunchedEffect(isPaused, isDragging, seekbarStyle) {
    if (seekbarStyle != SeekbarStyle.Wavy) {
      heightFraction = 0f
      return@LaunchedEffect
    }

    val shouldFlatten = isPaused || isDragging
    val targetHeight = if (shouldFlatten) 0f else 1f
    val animDuration = if (shouldFlatten) 550 else 800
    val startDelay = if (shouldFlatten) 0L else 60L

    delay(startDelay)

    val animator = Animatable(heightFraction)
    animator.animateTo(
      targetValue = targetHeight,
      animationSpec = tween(
        durationMillis = animDuration,
        easing = LinearEasing,
      ),
    ) {
      heightFraction = value
    }
  }

  LaunchedEffect(isPaused, seekbarStyle) {
    if (isPaused || seekbarStyle != SeekbarStyle.Wavy) return@LaunchedEffect

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

  val themePrimary = MaterialTheme.colorScheme.primary

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(if (isWavy) 30.dp else 24.dp)
      .onSizeChanged { trackWidthPx = it.width }
      .pointerInput(Unit) {
        detectTapGestures(
          onTap = { offset ->
            val currDuration = latestDuration
            if (currDuration <= 0f || trackWidthPx <= 0) return@detectTapGestures
            val fraction = (offset.x / trackWidthPx.toFloat()).coerceIn(0f, 1f)
            val target = fraction * currDuration
            latestOnSeekTo(target, false)
          }
        )
      }
      .pointerInput(scrubTierStepPx) {
        detectDragGestures(
          onDragStart = { offset ->
            val currDuration = latestDuration
            if (currDuration <= 0f || trackWidthPx <= 0) return@detectDragGestures
            isDragging = true
            latestOnSeekingChanged(true)
            dragAnchor = (offset.x / trackWidthPx.toFloat()).coerceIn(0f, 1f) * currDuration
            scaledDragDxPx = 0f
            verticalDragPx = 0f
            scrubTier = 0
            dragPosition = dragAnchor
            latestOnSeekTo(dragPosition, true)
          },
          onDragEnd = {
            isDragging = false
            latestOnSeekingChanged(false)
            scrubTier = 0
            latestOnSeekTo(dragPosition, false)
          },
          onDragCancel = {
            isDragging = false
            latestOnSeekingChanged(false)
            scrubTier = 0
            dragPosition = latestPosition
          },
          onDrag = { change, dragAmount ->
            change.consume()
            val currDuration = latestDuration
            if (currDuration <= 0f || trackWidthPx <= 0) return@detectDragGestures
            verticalDragPx += dragAmount.y
            val newTier = ((-verticalDragPx) / scrubTierStepPx).toInt().coerceIn(0, 3)
            if (newTier != scrubTier) scrubTier = newTier
            scaledDragDxPx += dragAmount.x * (1 shl scrubTier)
            val anchorFraction = dragAnchor.toDouble() / currDuration.toDouble()
            val deltaFraction = scaledDragDxPx.toDouble() / trackWidthPx.toDouble()
            dragPosition = ((anchorFraction + deltaFraction) * currDuration.toDouble())
              .toFloat().coerceIn(0f, currDuration)
            latestOnSeekTo(dragPosition, true)
          }
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(if (isWavy) 30.dp else animatedTrackHeight),
    ) {
      val w = size.width
      val h = size.height
      if (w <= 0f || h <= 0f) return@Canvas

      val centerY = h / 2f
      val playedWidth = playedFraction * w
      val unplayedColor = Color.White.copy(alpha = 0.24f)
      val cacheColor = themePrimary.copy(alpha = 0.40f)

      if (seekbarStyle == SeekbarStyle.Wavy) {
        val strokeWidth = animatedTrackHeight.toPx()

        fun computeAmplitude(x: Float, sign: Float): Float {
          val length = transitionPeriods * waveLength
          val coeff = ((playedWidth + length / 2f - x) / length).coerceIn(0f, 1f)
          return sign * heightFraction * lineAmplitude * coeff
        }

        val path = Path()
        val waveStart = -phaseOffset - waveLength / 2f
        val waveEnd = w

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

        val gapWidth = 2.dp.toPx()
        val chapterGaps = if (chaptersInSeekbar && duration > 0f && chapters.isNotEmpty()) {
          chapters.mapNotNull { ch ->
            val chFraction = (ch.start / duration).coerceIn(0f, 1f)
            if (chFraction > 0.01f && chFraction < 0.99f) {
              val chX = chFraction * w
              (chX - gapWidth / 2f) to (chX + gapWidth / 2f)
            } else null
          }
        } else emptyList()

        fun drawWavySegment(startX: Float, endX: Float, color: Color) {
          if (endX <= startX) return
          val relevantGaps = chapterGaps
            .filter { (gStart, gEnd) -> gEnd > startX && gStart < endX }
            .sortedBy { it.first }

          val clipTop = lineAmplitude + strokeWidth + 4f
          var curPos = startX
          for ((gStart, gEnd) in relevantGaps) {
            val segEnd = gStart.coerceAtMost(endX)
            if (segEnd > curPos) {
              clipRect(
                left = curPos,
                top = centerY - clipTop,
                right = segEnd,
                bottom = centerY + clipTop,
              ) {
                drawPath(
                  path = path,
                  color = color,
                  style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
              }
            }
            curPos = gEnd.coerceAtLeast(curPos)
          }
          if (curPos < endX) {
            clipRect(
              left = curPos,
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

        // 1. Unplayed track rail
        drawWavySegment(playedWidth, w, unplayedColor)

        // 2. Cache ranges (buffer band)
        if (duration > 0f) {
          for (range in cacheRanges) {
            val startX = (range.start / duration).coerceIn(0f, 1f) * w
            val endX = (range.end / duration).coerceIn(0f, 1f) * w
            if (endX > startX) {
              drawWavySegment(startX, endX, cacheColor)
            }
          }
        }

        // 3. Played progress
        if (playedWidth > 0.5f) {
          drawWavySegment(0f, playedWidth, themePrimary)
        }

        // 4. Round start cap
        val startAmp = kotlin.math.cos(kotlin.math.abs(waveStart) / waveLength * (2f * kotlin.math.PI.toFloat()))
        drawCircle(
          color = themePrimary,
          radius = strokeWidth / 2f,
          center = Offset(0f, centerY + startAmp * lineAmplitude * heightFraction),
        )

        // 5. Thumb indicator
        if (isDragging && playedWidth > 0.5f) {
          drawCircle(
            color = Color.White,
            radius = (strokeWidth / 2f) + 3.dp.toPx(),
            center = Offset(playedWidth.coerceIn(0f, w), centerY),
          )
          drawCircle(
            color = themePrimary,
            radius = strokeWidth / 2f + 1.dp.toPx(),
            center = Offset(playedWidth.coerceIn(0f, w), centerY),
          )
        } else if (playedWidth > 0.5f) {
          val barHalfHeight = (lineAmplitude * heightFraction + strokeWidth) / 2f + 2.dp.toPx()
          val barWidth = 3.dp.toPx()
          drawLine(
            color = Color.White,
            start = Offset(playedWidth, centerY - barHalfHeight),
            end = Offset(playedWidth, centerY + barHalfHeight),
            strokeWidth = barWidth,
            cap = StrokeCap.Round,
          )
        }
      } else {
        // Standard & Thick styles (exact previous modern player seekbar implementation)
        val cornerRadius = CornerRadius(h / 2f, h / 2f)

        // 1. Unplayed track background
        val bgPath = Path().apply {
          addRoundRect(RoundRect(0f, 0f, w, h, cornerRadius))
        }
        drawPath(bgPath, unplayedColor)

        // 2. Cache ranges (buffer band)
        if (duration > 0f) {
          for (range in cacheRanges) {
            val startFraction = (range.start / duration).coerceIn(0f, 1f)
            val endFraction = (range.end / duration).coerceIn(0f, 1f)
            val startX = startFraction * w
            val endX = endFraction * w
            if (endX > startX) {
              val cachePath = Path().apply {
                addRoundRect(RoundRect(startX, 0f, endX, h, cornerRadius))
              }
              drawPath(cachePath, cacheColor)
            }
          }
        }

        // 3. Played progress track (themed with MaterialTheme.colorScheme.primary)
        val playedWidth = playedFraction * w
        if (playedWidth > 0.5f) {
          val playedPath = Path().apply {
            addRoundRect(RoundRect(0f, 0f, playedWidth, h, cornerRadius))
          }
          drawPath(playedPath, themePrimary)
        }

        // 4. Chapter ticks (gaps) - strictly gated by chaptersInSeekbar preference
        if (chaptersInSeekbar && duration > 0f && chapters.isNotEmpty()) {
          val gapWidth = 2.dp.toPx()
          for (chapter in chapters) {
            val chFraction = (chapter.start / duration).coerceIn(0f, 1f)
            if (chFraction > 0.01f && chFraction < 0.99f) {
              val chX = chFraction * w
              drawRect(
                color = Color.Black.copy(alpha = 0.65f),
                topLeft = Offset(chX - gapWidth / 2f, 0f),
                size = Size(gapWidth, h),
              )
            }
          }
        }

        // 5. Thumb indicator when dragging
        if (isDragging && playedWidth > 0.5f) {
          drawCircle(
            color = Color.White,
            radius = (h / 2f) + 2.5.dp.toPx(),
            center = Offset(playedWidth.coerceIn(0f, w), h / 2f),
          )
          drawCircle(
            color = themePrimary,
            radius = (h / 2f),
            center = Offset(playedWidth.coerceIn(0f, w), h / 2f),
          )
        }
      }
    }

    // Floating 1x/2x/4x/8x scrub rate popup
    AnimatedVisibility(
      visible = isDragging && scrubTier > 0,
      enter = fadeIn() + scaleIn(),
      exit = fadeOut() + scaleOut(),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .offset {
          val travelPx = (trackWidthPx - 50.dp.roundToPx()).coerceAtLeast(0)
          IntOffset(x = ((playedFraction - 0.5f) * travelPx).roundToInt(), y = (-30).dp.roundToPx())
        },
    ) {
      Box(
        modifier = Modifier
          .background(
            MaterialTheme.colorScheme.inverseSurface,
            RoundedCornerShape(8.dp),
          )
          .padding(horizontal = 8.dp, vertical = 4.dp),
      ) {
        Text(
          text = "${scrubRate}×",
          color = MaterialTheme.colorScheme.inverseOnSurface,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
        )
      }
    }
  }
}
