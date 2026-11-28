package app.aryan447.mpvium.ui.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Title
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.CustomScriptButton
import app.aryan447.mpvium.preferences.PlayerButton
import app.aryan447.mpvium.preferences.SeekbarStyle
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.ui.player.Panels
import app.aryan447.mpvium.ui.player.PlayerActivity
import app.aryan447.mpvium.ui.player.PlayerViewModel
import app.aryan447.mpvium.ui.player.SeekCacheResolver
import app.aryan447.mpvium.ui.player.Sheets
import app.aryan447.mpvium.ui.player.VideoAspect
import app.aryan447.mpvium.ui.player.controls.components.CustomButtonsRow
import app.aryan447.mpvium.utils.media.EpisodeTitleFormatter
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import org.koin.compose.koinInject

/**
 * Zen player layout: minimalist floating dark frosted-glass pills.
 *
 * Characteristics:
 * - Top-left: Circular Close (X) button, with an on-demand single-line clean title beside it.
 * - Top-right: Title unhide toggle button beside a circular Playlist/Chapters/PiP button.
 *   The title starts hidden every time the UI is shown and resets when the UI hides.
 * - Bottom: Single horizontal row of three frosted-glass pills in landscape:
 *   [ Replay 10 | Play/Pause | Forward 10 ]  [ Seekbar + Timers ]  [ Action Shortcuts ]
 *   (Stacks into two rows on narrow portrait screens so the seekbar stays usable.)
 */
@Composable
fun ZenPlayerControls(
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
  chapters: ImmutableList<Segment>,
  currentChapter: Int?,
  cacheRanges: ImmutableList<SeekCacheResolver.SeekableRange>,
  invertDuration: Boolean,
  onToggleInvertDuration: () -> Unit,
  mediaTitle: String = "",
  tmdbEpisodeTitle: String? = null,
  showEpisodeHeader: Boolean = true,
  actionButtons: List<PlayerButton> = emptyList(),
  amoledMode: Boolean = false,
  playerFullColors: Boolean = false,
  showSystemStatusBar: Boolean = false,
  tabletBottomOffset: Dp = 0.dp,
  isSpeedNonOne: Boolean = false,
  playbackSpeed: Float = 1f,
  currentZoom: Float = 1f,
  aspect: VideoAspect = VideoAspect.Fit,
  decoder: app.aryan447.mpvium.ui.player.Decoder = app.aryan447.mpvium.ui.player.Decoder.SW,
  customButtons: List<CustomScriptButton> = emptyList(),
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val configuration = LocalConfiguration.current
  val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

  val appearancePreferences = koinInject<AppearancePreferences>()
  val zenSeekbarStyle by appearancePreferences.zenSeekbarStyle.collectAsState()

  val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
  val navBarBottom = navBarPadding.calculateBottomPadding()
  val systemGesturesPadding = WindowInsets.systemGestures.asPaddingValues()
  val systemGestureBottom = systemGesturesPadding.calculateBottomPadding()

  val gestureSafeBottom = maxOf(
    systemGestureBottom,
    navBarBottom,
    if (isPortrait) 20.dp else 20.dp,
  )

  val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
  val topSafeInset = if (showSystemStatusBar) statusBarPadding.calculateTopPadding() + 8.dp else 16.dp

  val navBarLeft = navBarPadding.calculateLeftPadding(LayoutDirection.Ltr)
  val navBarRight = navBarPadding.calculateRightPadding(LayoutDirection.Ltr)
  val startSafeInset = maxOf(navBarLeft, if (isPortrait) 16.dp else 24.dp)
  val endSafeInset = maxOf(navBarRight, if (isPortrait) 16.dp else 24.dp)

  val surfaceColor = if (amoledMode) Color(0xCC0A0B0E) else Color(0x9916181D)
  val borderStroke = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))

  // Title starts hidden and resets to hidden whenever controls disappear.
  var isTitleVisible by remember { mutableStateOf(false) }
  LaunchedEffect(controlsShown) {
    if (!controlsShown) {
      isTitleVisible = false
    }
  }

  val singleLineCleanTitle = remember(mediaTitle, tmdbEpisodeTitle, showEpisodeHeader) {
    if (showEpisodeHeader && mediaTitle.isNotBlank()) {
      EpisodeTitleFormatter.resolve(mediaTitle, tmdbEpisodeTitle)?.singleLine ?: mediaTitle
    } else {
      mediaTitle
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    // -------------------------------------------------------------
    // TOP BAR: [ X ] (single-line title when unhidden) ... [ TitleToggle ] [ TopRight ]
    // -------------------------------------------------------------
    AnimatedVisibility(
      visible = controlsShown && !areControlsLocked,
      enter = slideInVertically(playerControlsEnterAnimationSpec()) { -it } + fadeIn(playerControlsEnterAnimationSpec()),
      exit = slideOutVertically(playerControlsExitAnimationSpec()) { -it } + fadeOut(playerControlsExitAnimationSpec()),
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = topSafeInset, start = startSafeInset, end = endSafeInset),
    ) {
      val topCustomButtons = remember(customButtons) {
        customButtons.filter { it.position.equals("top", ignoreCase = true) }
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left: Close (X) circle + optional single-line clean title beside it
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false),
        ) {
          ZenCircleButton(
            surfaceColor = surfaceColor,
            borderStroke = borderStroke,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              resetControlsTimestamp()
              onBackPress()
            },
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = Color.White,
              modifier = Modifier.size(20.dp),
            )
          }

          AnimatedVisibility(
            visible = isTitleVisible && singleLineCleanTitle.isNotBlank(),
            enter = fadeIn(playerControlsEnterAnimationSpec()),
            exit = fadeOut(playerControlsExitAnimationSpec()),
          ) {
            Text(
              text = singleLineCleanTitle,
              style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                shadow = Shadow(color = Color.Black.copy(alpha = 0.65f), blurRadius = 6f),
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier
                .padding(start = 12.dp, end = 8.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  resetControlsTimestamp()
                  if (viewModel.hasPlaylistSupport()) {
                    onOpenSheet(Sheets.Playlist)
                  } else {
                    onOpenSheet(Sheets.Chapters)
                  }
                },
            )
          }
        }

        // Optional top custom script buttons matching Zen's frosted glass aesthetic
        if (topCustomButtons.isNotEmpty()) {
          Spacer(modifier = Modifier.width(12.dp))
          CustomButtonsRow(
            buttons = topCustomButtons,
            onExecute = { cmd -> viewModel.executeCustomCommand(cmd) },
            resetControlsTimestamp = resetControlsTimestamp,
            surfaceColor = surfaceColor,
            borderStroke = borderStroke,
            modifier = Modifier.weight(1f, fill = false),
          )
          Spacer(modifier = Modifier.width(12.dp))
        }

        // Right: Top-right action button
        ZenCircleButton(
          surfaceColor = surfaceColor,
          borderStroke = borderStroke,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            resetControlsTimestamp()
            when {
              viewModel.hasPlaylistSupport() -> onOpenSheet(Sheets.Playlist)
              chapters.isNotEmpty() -> onOpenSheet(Sheets.Chapters)
              else -> activity.enterPipModeHidingOverlay()
            }
          },
        ) {
          Icon(
            imageVector = Icons.Outlined.Subscriptions,
            contentDescription = "Playlist / Chapters",
            tint = Color.White,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }

    // -------------------------------------------------------------
    // BOTTOM BAR: [ Transport Pill ] [ Seekbar + Timers Pill ] [ Actions Pill ]
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
      val bottomCustomButtons = remember(customButtons) {
        customButtons.filter { !it.position.equals("top", ignoreCase = true) }
      }

      var wasPlayerAlreadyPaused by remember { mutableStateOf(false) }
      val seekbarPlayedColor = if (playerFullColors) MaterialTheme.colorScheme.primary else Color.White

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (bottomCustomButtons.isNotEmpty()) {
          Box(modifier = Modifier.fillMaxWidth()) {
            CustomButtonsRow(
              buttons = bottomCustomButtons,
              onExecute = { cmd -> viewModel.executeCustomCommand(cmd) },
              resetControlsTimestamp = resetControlsTimestamp,
              surfaceColor = surfaceColor,
              borderStroke = borderStroke,
              modifier = Modifier.align(Alignment.CenterEnd),
            )
          }
        }

        if (isPortrait) {
          ZenSeekbarPill(
            position = position,
            duration = duration,
            chapters = chapters,
            cacheRanges = cacheRanges,
            isPaused = paused,
            seekbarStyle = zenSeekbarStyle,
            playedColor = seekbarPlayedColor,
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
            surfaceColor = surfaceColor,
            borderStroke = borderStroke,
            modifier = Modifier.fillMaxWidth(),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ZenTransportPill(
              paused = paused,
              onRewind = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                resetControlsTimestamp()
                viewModel.handleMediaPrevious()
              },
              onPlayPause = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                resetControlsTimestamp()
                viewModel.handleMediaPlay()
              },
              onForward = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                resetControlsTimestamp()
                viewModel.handleMediaNext()
              },
              surfaceColor = surfaceColor,
              borderStroke = borderStroke,
            )

            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ZenCircleButton(
                size = 50.dp,
                surfaceColor = surfaceColor,
                borderStroke = borderStroke,
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  resetControlsTimestamp()
                  isTitleVisible = !isTitleVisible
                },
              ) {
                Icon(
                  imageVector = Icons.Outlined.Title,
                  contentDescription = if (isTitleVisible) "Hide Title" else "Show Title",
                  tint = if (isTitleVisible) MaterialTheme.colorScheme.primary else Color.White,
                  modifier = Modifier.size(20.dp),
                )
              }

              ModernBottomActionsPill(
                buttons = actionButtons,
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
                isSpeedNonOne = isSpeedNonOne,
                playbackSpeed = playbackSpeed,
                currentZoom = currentZoom,
                aspect = aspect,
                decoder = decoder,
                pillHeight = 50.dp,
              )
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ZenTransportPill(
              paused = paused,
              onRewind = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                resetControlsTimestamp()
                viewModel.handleMediaPrevious()
              },
              onPlayPause = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                resetControlsTimestamp()
                viewModel.handleMediaPlay()
              },
              onForward = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                resetControlsTimestamp()
                viewModel.handleMediaNext()
              },
              surfaceColor = surfaceColor,
              borderStroke = borderStroke,
            )

            ZenSeekbarPill(
              position = position,
              duration = duration,
              chapters = chapters,
              cacheRanges = cacheRanges,
              isPaused = paused,
              seekbarStyle = zenSeekbarStyle,
              playedColor = seekbarPlayedColor,
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
              surfaceColor = surfaceColor,
              borderStroke = borderStroke,
              modifier = Modifier.weight(1f),
            )

            ZenCircleButton(
              size = 50.dp,
              surfaceColor = surfaceColor,
              borderStroke = borderStroke,
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                resetControlsTimestamp()
                isTitleVisible = !isTitleVisible
              },
            ) {
              Icon(
                imageVector = Icons.Outlined.Title,
                contentDescription = if (isTitleVisible) "Hide Title" else "Show Title",
                tint = if (isTitleVisible) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(20.dp),
              )
            }

            ModernBottomActionsPill(
              buttons = actionButtons,
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
              isSpeedNonOne = isSpeedNonOne,
              playbackSpeed = playbackSpeed,
              currentZoom = currentZoom,
              aspect = aspect,
              decoder = decoder,
              pillHeight = 50.dp,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ZenCircleButton(
  surfaceColor: Color,
  borderStroke: BorderStroke,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 42.dp,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, radius = size / 2),
        onClick = onClick,
      ),
    shape = CircleShape,
    color = surfaceColor,
    border = borderStroke,
  ) {
    Box(contentAlignment = Alignment.Center) {
      content()
    }
  }
}

@Composable
private fun ZenTransportPill(
  paused: Boolean,
  onRewind: () -> Unit,
  onPlayPause: () -> Unit,
  onForward: () -> Unit,
  surfaceColor: Color,
  borderStroke: BorderStroke,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(50.dp),
    shape = RoundedCornerShape(25.dp),
    color = surfaceColor,
    border = borderStroke,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.Replay10,
        contentDescription = "Rewind 10 seconds",
        tint = Color.White,
        modifier = Modifier
          .size(22.dp)
          .clip(CircleShape)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, radius = 18.dp),
            onClick = onRewind,
          ),
      )

      Icon(
        imageVector = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
        contentDescription = if (paused) "Play" else "Pause",
        tint = Color.White,
        modifier = Modifier
          .size(26.dp)
          .clip(CircleShape)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, radius = 20.dp),
            onClick = onPlayPause,
          ),
      )

      Icon(
        imageVector = Icons.Outlined.Forward10,
        contentDescription = "Forward 10 seconds",
        tint = Color.White,
        modifier = Modifier
          .size(22.dp)
          .clip(CircleShape)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, radius = 18.dp),
            onClick = onForward,
          ),
      )
    }
  }
}

@Composable
private fun ZenSeekbarPill(
  position: Float,
  duration: Float,
  chapters: ImmutableList<Segment>,
  cacheRanges: ImmutableList<SeekCacheResolver.SeekableRange>,
  isPaused: Boolean,
  seekbarStyle: SeekbarStyle,
  playedColor: Color,
  onSeekTo: (Float, Boolean) -> Unit,
  onSeekingChanged: (Boolean) -> Unit,
  invertDuration: Boolean,
  onToggleInvertDuration: () -> Unit,
  surfaceColor: Color,
  borderStroke: BorderStroke,
  modifier: Modifier = Modifier,
) {
  var isDragging by remember { mutableStateOf(false) }
  var dragPosition by remember { mutableFloatStateOf(position) }
  val effectivePosition = if (isDragging) dragPosition else position

  Surface(
    modifier = modifier
      .height(50.dp)
      .systemGestureExclusion(),
    shape = RoundedCornerShape(25.dp),
    color = surfaceColor,
    border = borderStroke,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.Center,
    ) {
      ModernSeekbarTrack(
        position = position,
        duration = duration,
        chapters = chapters,
        cacheRanges = cacheRanges,
        isPaused = isPaused,
        onSeekTo = onSeekTo,
        onSeekingChanged = { seeking ->
          isDragging = seeking
          onSeekingChanged(seeking)
        },
        onDragPositionChange = { dragPosition = it },
        seekbarStyleOverride = seekbarStyle,
        playedColorOverride = playedColor,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = Utils.prettyTime(effectivePosition.toInt(), false),
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.85f),
          ),
        )

        val rightTimerValue = if (invertDuration) effectivePosition - duration else duration
        Text(
          text = Utils.prettyTime(rightTimerValue.toInt(), invertDuration),
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.85f),
          ),
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onToggleInvertDuration() },
        )
      }
    }
  }
}
