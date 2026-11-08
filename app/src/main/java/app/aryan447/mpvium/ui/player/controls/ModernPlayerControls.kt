package app.aryan447.mpvium.ui.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.preferences.PlayerButton
import app.aryan447.mpvium.ui.player.Panels
import app.aryan447.mpvium.ui.player.PlayerActivity
import app.aryan447.mpvium.ui.player.PlayerViewModel
import app.aryan447.mpvium.ui.player.SeekCacheResolver
import app.aryan447.mpvium.ui.player.Sheets
import app.aryan447.mpvium.utils.media.MediaInfoParser
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList
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
  amoledMode: Boolean = false,
  showSystemStatusBar: Boolean = false,
  showSystemNavigationBar: Boolean = false,
  tabletBottomOffset: Dp = 0.dp,
  modifier: Modifier = Modifier,
) {
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
                  interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                  indication = ripple(bounded = true, radius = 18.dp),
                ) {
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
                  interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                  indication = ripple(bounded = true, radius = 18.dp),
                ) {
                  resetControlsTimestamp()
                  activity.enterPipModeHidingOverlay()
                },
            )
          }
        }

        // Top-right Glass Volume Pill [ ===o 🔊 ]
        ModernVolumePill(
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
          resetControlsTimestamp()
          viewModel.pauseUnpause()
        },
        onRewind = {
          resetControlsTimestamp()
          viewModel.seekBy(-10)
        },
        onForward = {
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

          // Bottom-Right Actions Pill: [ Subtitles | Audio | Speed/More ]
          ModernBottomActionsPill(
            onOpenSubtitles = {
              resetControlsTimestamp()
              onOpenSheet(Sheets.SubtitleTracks)
            },
            onOpenAudio = {
              resetControlsTimestamp()
              onOpenSheet(Sheets.AudioTracks)
            },
            onOpenMore = {
              resetControlsTimestamp()
              onOpenSheet(Sheets.More)
            },
            surfaceColor = surfaceColor,
            borderStroke = borderStroke,
          )
        }

        // Seekbar track and timers
        ModernSeekbarSection(
          position = position,
          duration = duration,
          chapters = chapters,
          cacheRanges = cacheRanges,
          onSeekTo = { target, isScrubbing ->
            resetControlsTimestamp()
            if (isScrubbing) {
              viewModel.seekTo(target.roundToInt(), isScrubbing = true)
            } else {
              viewModel.seekToAndResume(target.roundToInt())
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

/**
 * Top-right floating glass volume pill with horizontal level slider and speaker icon.
 */
@Composable
fun ModernVolumePill(
  viewModel: PlayerViewModel,
  resetControlsTimestamp: () -> Unit,
  surfaceColor: Color,
  borderStroke: BorderStroke,
  modifier: Modifier = Modifier,
) {
  val currentVolume by viewModel.currentVolume.collectAsState()
  val maxVolume = viewModel.maxVolume.coerceAtLeast(1)
  var previousVolume by remember { mutableIntStateOf(currentVolume.takeIf { it > 0 } ?: (maxVolume / 2)) }

  var trackWidthPx by remember { mutableIntStateOf(0) }
  val volumeFraction = (currentVolume.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f)

  Surface(
    modifier = modifier.height(42.dp),
    shape = RoundedCornerShape(21.dp),
    color = surfaceColor,
    border = borderStroke,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Horizontal volume slider track
      Box(
        modifier = Modifier
          .width(100.dp)
          .height(24.dp)
          .onSizeChanged { trackWidthPx = it.width }
          .pointerInput(maxVolume) {
            detectTapGestures { offset ->
              resetControlsTimestamp()
              if (trackWidthPx > 0) {
                val fraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                val newVol = (fraction * maxVolume).roundToInt()
                viewModel.changeVolumeTo(newVol)
                if (newVol > 0) previousVolume = newVol
              }
            }
          }
          .pointerInput(maxVolume) {
            detectDragGestures(
              onDragStart = { offset ->
                resetControlsTimestamp()
                if (trackWidthPx > 0) {
                  val fraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                  val newVol = (fraction * maxVolume).roundToInt()
                  viewModel.changeVolumeTo(newVol)
                  if (newVol > 0) previousVolume = newVol
                }
              },
              onDrag = { change, _ ->
                change.consume()
                resetControlsTimestamp()
                if (trackWidthPx > 0) {
                  val fraction = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                  val newVol = (fraction * maxVolume).roundToInt()
                  viewModel.changeVolumeTo(newVol)
                  if (newVol > 0) previousVolume = newVol
                }
              }
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

          // Filled active volume
          val filledWidth = volumeFraction * w
          if (filledWidth > 0.5f) {
            drawRoundRect(
              color = Color.White,
              size = Size(filledWidth, h),
              cornerRadius = radius,
            )
          }
        }
      }

      // Speaker icon (tap to mute/unmute)
      val speakerIcon = when {
        currentVolume == 0 -> Icons.AutoMirrored.Filled.VolumeMute
        currentVolume < maxVolume / 2 -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeUp
      }

      Icon(
        imageVector = speakerIcon,
        contentDescription = "Mute / Unmute",
        tint = Color.White,
        modifier = Modifier
          .size(20.dp)
          .clip(CircleShape)
          .clickable {
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
          interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
          indication = ripple(bounded = true, radius = 26.dp),
          onClick = onRewind,
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
          interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
          indication = ripple(bounded = true, radius = 34.dp),
          onClick = onPlayPause,
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
          interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
          indication = ripple(bounded = true, radius = 26.dp),
          onClick = onForward,
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
 * [ Subtitles | Audio waveform | Speed/More ]
 */
@Composable
fun ModernBottomActionsPill(
  onOpenSubtitles: () -> Unit,
  onOpenAudio: () -> Unit,
  onOpenMore: () -> Unit,
  surfaceColor: Color,
  borderStroke: BorderStroke,
  modifier: Modifier = Modifier,
) {
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
      // Subtitles
      Icon(
        imageVector = Icons.Default.Subtitles,
        contentDescription = "Subtitles",
        tint = Color.White,
        modifier = Modifier
          .size(20.dp)
          .clip(CircleShape)
          .clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = ripple(bounded = true, radius = 18.dp),
            onClick = onOpenSubtitles,
          ),
      )

      // Audio tracks (waveform / audio icon)
      Icon(
        imageVector = Icons.Outlined.GraphicEq,
        contentDescription = "Audio Tracks",
        tint = Color.White,
        modifier = Modifier
          .size(20.dp)
          .clip(CircleShape)
          .clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = ripple(bounded = true, radius = 18.dp),
            onClick = onOpenAudio,
          ),
      )

      // Playback speed / More options
      Icon(
        imageVector = Icons.Outlined.Speed,
        contentDescription = "Speed and Settings",
        tint = Color.White,
        modifier = Modifier
          .size(20.dp)
          .clip(CircleShape)
          .clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = ripple(bounded = true, radius = 18.dp),
            onClick = onOpenMore,
          ),
      )
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
  onSeekTo: (Float, Boolean) -> Unit,
  onSeekingChanged: (Boolean) -> Unit,
  invertDuration: Boolean,
  onToggleInvertDuration: () -> Unit,
  modifier: Modifier = Modifier,
) {
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
          .clickable(onClick = onToggleInvertDuration),
      )
    }
  }
}

/**
 * Interactive progress track inside the Modern player layout.
 */
@Composable
fun ModernSeekbarTrack(
  position: Float,
  duration: Float,
  chapters: ImmutableList<Segment>,
  cacheRanges: ImmutableList<SeekCacheResolver.SeekableRange>,
  onSeekTo: (Float, Boolean) -> Unit,
  onSeekingChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isDragging by remember { mutableStateOf(false) }
  var dragPosition by remember { mutableFloatStateOf(position) }
  var trackWidthPx by remember { mutableIntStateOf(0) }

  val latestDuration by rememberUpdatedState(duration)
  val latestPosition by rememberUpdatedState(position)
  val latestOnSeekTo by rememberUpdatedState(onSeekTo)
  val latestOnSeekingChanged by rememberUpdatedState(onSeekingChanged)

  val displayPosition = if (isDragging) dragPosition else position
  val playedFraction = if (duration > 0f) (displayPosition / duration).coerceIn(0f, 1f) else 0f

  val animatedTrackHeight by animateDpAsState(
    targetValue = if (isDragging) 7.dp else 5.dp,
    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
    label = "trackHeight",
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(20.dp)
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
      .pointerInput(Unit) {
        detectDragGestures(
          onDragStart = { offset ->
            val currDuration = latestDuration
            if (currDuration <= 0f || trackWidthPx <= 0) return@detectDragGestures
            isDragging = true
            latestOnSeekingChanged(true)
            val fraction = (offset.x / trackWidthPx.toFloat()).coerceIn(0f, 1f)
            dragPosition = fraction * currDuration
            latestOnSeekTo(dragPosition, true)
          },
          onDragEnd = {
            isDragging = false
            latestOnSeekingChanged(false)
            latestOnSeekTo(dragPosition, false)
          },
          onDragCancel = {
            isDragging = false
            latestOnSeekingChanged(false)
            dragPosition = latestPosition
          },
          onDrag = { change, _ ->
            change.consume()
            val currDuration = latestDuration
            if (currDuration <= 0f || trackWidthPx <= 0) return@detectDragGestures
            val fraction = (change.position.x / trackWidthPx.toFloat()).coerceIn(0f, 1f)
            dragPosition = fraction * currDuration
            latestOnSeekTo(dragPosition, true)
          }
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(animatedTrackHeight),
    ) {
      val w = size.width
      val h = size.height
      val cornerRadius = CornerRadius(h / 2f, h / 2f)

      if (w <= 0f || h <= 0f) return@Canvas

      // 1. Unplayed track background
      val bgPath = Path().apply {
        addRoundRect(RoundRect(0f, 0f, w, h, cornerRadius))
      }
      drawPath(bgPath, Color.White.copy(alpha = 0.28f))

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
            drawPath(cachePath, Color.White.copy(alpha = 0.45f))
          }
        }
      }

      // 3. Played progress track
      val playedWidth = playedFraction * w
      if (playedWidth > 0.5f) {
        val playedPath = Path().apply {
          addRoundRect(RoundRect(0f, 0f, playedWidth, h, cornerRadius))
        }
        drawPath(playedPath, Color.White)
      }

      // 4. Chapter ticks (gaps)
      if (duration > 0f && chapters.isNotEmpty()) {
        val gapWidth = 1.5.dp.toPx()
        for (chapter in chapters) {
          val chFraction = (chapter.start / duration).coerceIn(0f, 1f)
          if (chFraction > 0.01f && chFraction < 0.99f) {
            val chX = chFraction * w
            drawRect(
              color = Color.Black.copy(alpha = 0.60f),
              topLeft = Offset(chX - gapWidth / 2f, 0f),
              size = Size(gapWidth, h),
            )
          }
        }
      }
    }
  }
}
