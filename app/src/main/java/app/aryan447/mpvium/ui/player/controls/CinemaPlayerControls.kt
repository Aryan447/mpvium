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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.preferences.PlayerButton
import app.aryan447.mpvium.ui.player.Panels
import app.aryan447.mpvium.ui.player.PlayerActivity
import app.aryan447.mpvium.ui.player.PlayerViewModel
import app.aryan447.mpvium.ui.player.SeekCacheResolver
import app.aryan447.mpvium.ui.player.Sheets
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

/**
 * Cinema player layout modeled after the Apple TV / Infuse floating glass pill interface.
 *
 * Characteristics:
 * - Top-left: Circular floating glass close button (✕).
 * - Top-right: Circular floating glass PiP button.
 * - Center screen: Completely clean and unobstructed video view (no giant play/pause disc).
 * - Bottom: Three floating dark glass pills:
 *     1. Transport pill: [ Replay 10s | Play/Pause | Forward 10s ]
 *     2. Timeline pill: Wide capsule with progress track on top, position left, remaining duration right.
 *     3. Actions pill: [ Tracks (Audio/Subtitles) | Chapters/Playlist list ]
 * - Elevated safely above navigation / gesture insets so scrubbing never triggers Android system gestures.
 */
@Composable
fun CinemaPlayerControls(
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
  topRightButtons: List<PlayerButton>,
  bottomRightButtons: List<PlayerButton>,
  amoledMode: Boolean,
  showSystemStatusBar: Boolean,
  showSystemNavigationBar: Boolean,
  tabletBottomOffset: Dp,
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

  val surfaceColor = if (amoledMode) Color(0xE608090C) else Color(0xCC16181D)
  val borderStroke = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))

  Box(
    modifier = modifier.fillMaxSize(),
  ) {
    // -------------------------------------------------------------
    // TOP CONTROLS: Close (✕) on Left, PiP on Right
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
        // Top-left Close Button (✕)
        Surface(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
              interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
              indication = ripple(bounded = true, radius = 22.dp),
            ) {
              resetControlsTimestamp()
              onBackPress()
            },
          shape = CircleShape,
          color = surfaceColor,
          border = borderStroke,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = Color.White,
              modifier = Modifier.size(22.dp),
            )
          }
        }

        // Top-right PiP Button
        Surface(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
              interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
              indication = ripple(bounded = true, radius = 22.dp),
            ) {
              resetControlsTimestamp()
              activity.enterPipModeHidingOverlay()
            },
          shape = CircleShape,
          color = surfaceColor,
          border = borderStroke,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.PictureInPictureAlt,
              contentDescription = "Picture in Picture",
              tint = Color.White,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }
    }

    // -------------------------------------------------------------
    // BOTTOM CONTROLS: Floating Glass Pills
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
      if (isPortrait) {
        // Portrait Layout: Timeline pill on top, Transport & Actions pills underneath
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Center Timeline Pill
          CinemaTimelinePill(
            modifier = Modifier.fillMaxWidth(),
            position = position,
            duration = duration,
            isSeeking = isSeeking,
            onSeekingChanged = onSeekingChanged,
            onSeekTo = { target, isScrubbing ->
              resetControlsTimestamp()
              if (isScrubbing) {
                viewModel.seekTo(target.roundToInt(), isScrubbing = true)
              } else {
                viewModel.seekToAndResume(target.roundToInt())
              }
            },
            chapters = chapters,
            cacheRanges = cacheRanges,
            invertDuration = invertDuration,
            onToggleInvertDuration = onToggleInvertDuration,
            surfaceColor = surfaceColor,
            borderStroke = borderStroke,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // Transport Pill
            CinemaTransportPill(
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

            // Actions Pill
            CinemaActionsPill(
              hasPlaylist = viewModel.hasPlaylistSupport(),
              onOpenSubtitles = {
                resetControlsTimestamp()
                onOpenSheet(Sheets.SubtitleTracks)
              },
              onOpenAudio = {
                resetControlsTimestamp()
                onOpenSheet(Sheets.AudioTracks)
              },
              onOpenChapters = {
                resetControlsTimestamp()
                onOpenSheet(Sheets.Chapters)
              },
              onOpenPlaylist = {
                resetControlsTimestamp()
                onOpenSheet(Sheets.Playlist)
              },
              surfaceColor = surfaceColor,
              borderStroke = borderStroke,
            )
          }
        }
      } else {
        // Landscape Layout: 3 Pills side-by-side [ Transport ] [ Timeline ] [ Actions ]
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Left: Transport Pill
          CinemaTransportPill(
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

          // Center: Timeline Pill (Fills available space)
          CinemaTimelinePill(
            modifier = Modifier.weight(1f),
            position = position,
            duration = duration,
            isSeeking = isSeeking,
            onSeekingChanged = onSeekingChanged,
            onSeekTo = { target, isScrubbing ->
              resetControlsTimestamp()
              if (isScrubbing) {
                viewModel.seekTo(target.roundToInt(), isScrubbing = true)
              } else {
                viewModel.seekToAndResume(target.roundToInt())
              }
            },
            chapters = chapters,
            cacheRanges = cacheRanges,
            invertDuration = invertDuration,
            onToggleInvertDuration = onToggleInvertDuration,
            surfaceColor = surfaceColor,
            borderStroke = borderStroke,
          )

          // Right: Actions Pill
          CinemaActionsPill(
            hasPlaylist = viewModel.hasPlaylistSupport(),
            onOpenSubtitles = {
              resetControlsTimestamp()
              onOpenSheet(Sheets.SubtitleTracks)
            },
            onOpenAudio = {
              resetControlsTimestamp()
              onOpenSheet(Sheets.AudioTracks)
            },
            onOpenChapters = {
              resetControlsTimestamp()
              onOpenSheet(Sheets.Chapters)
            },
            onOpenPlaylist = {
              resetControlsTimestamp()
              onOpenSheet(Sheets.Playlist)
            },
            surfaceColor = surfaceColor,
            borderStroke = borderStroke,
          )
        }
      }
    }
  }
}

/**
 * Bottom-left floating glass transport pill:
 * [ Rewind 10s | Play / Pause | Forward 10s ]
 */
@Composable
fun CinemaTransportPill(
  paused: Boolean,
  onPlayPause: () -> Unit,
  onRewind: () -> Unit,
  onForward: () -> Unit,
  surfaceColor: Color,
  borderStroke: BorderStroke,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(54.dp),
    shape = RoundedCornerShape(27.dp),
    color = surfaceColor,
    border = borderStroke,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Replay 10s
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = ripple(bounded = true, radius = 19.dp),
            onClick = onRewind,
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.Replay10,
          contentDescription = "Rewind 10 seconds",
          tint = Color.White,
          modifier = Modifier.size(24.dp),
        )
      }

      // Solid Play/Pause Glyph
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = ripple(bounded = true, radius = 21.dp),
            onClick = onPlayPause,
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
          contentDescription = if (paused) "Play" else "Pause",
          tint = Color.White,
          modifier = Modifier.size(28.dp),
        )
      }

      // Forward 10s
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = ripple(bounded = true, radius = 19.dp),
            onClick = onForward,
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.Forward10,
          contentDescription = "Forward 10 seconds",
          tint = Color.White,
          modifier = Modifier.size(24.dp),
        )
      }
    }
  }
}

/**
 * Bottom-center floating glass timeline pill:
 * Progress track on top, current position left, remaining duration right.
 */
@Composable
fun CinemaTimelinePill(
  position: Float,
  duration: Float,
  isSeeking: Boolean,
  onSeekingChanged: (Boolean) -> Unit,
  onSeekTo: (Float, Boolean) -> Unit,
  chapters: ImmutableList<Segment>,
  cacheRanges: ImmutableList<SeekCacheResolver.SeekableRange>,
  invertDuration: Boolean,
  onToggleInvertDuration: () -> Unit,
  surfaceColor: Color,
  borderStroke: BorderStroke,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .height(54.dp)
      .systemGestureExclusion(),
    shape = RoundedCornerShape(27.dp),
    color = surfaceColor,
    border = borderStroke,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 7.dp),
      verticalArrangement = Arrangement.Center,
    ) {
      // Seekbar track
      CinemaSeekbarTrack(
        position = position,
        duration = duration,
        chapters = chapters,
        cacheRanges = cacheRanges,
        onSeekTo = onSeekTo,
        onSeekingChanged = onSeekingChanged,
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Timers Row
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
            fontSize = 11.5.sp,
            color = Color.White.copy(alpha = 0.90f),
          ),
        )

        // Remaining or total duration timer (tap to invert)
        val rightTimerValue = if (invertDuration) position - duration else duration
        val durationFormatted = Utils.prettyTime(rightTimerValue.toInt(), invertDuration)

        Text(
          text = durationFormatted,
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp,
            color = Color.White.copy(alpha = 0.90f),
          ),
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onToggleInvertDuration),
        )
      }
    }
  }
}

/**
 * Interactive progress track inside the Cinema timeline pill.
 */
@Composable
fun CinemaSeekbarTrack(
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
    targetValue = if (isDragging) 6.dp else 4.dp,
    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
    label = "trackHeight",
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(18.dp)
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
      drawPath(bgPath, Color.White.copy(alpha = 0.22f))

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
            drawPath(cachePath, Color.White.copy(alpha = 0.38f))
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

/**
 * Bottom-right floating glass actions pill:
 * [ Tracks (Audio / Subtitles) | Playlist / Chapters list ]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CinemaActionsPill(
  hasPlaylist: Boolean,
  onOpenSubtitles: () -> Unit,
  onOpenAudio: () -> Unit,
  onOpenChapters: () -> Unit,
  onOpenPlaylist: () -> Unit,
  surfaceColor: Color,
  borderStroke: BorderStroke,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(54.dp),
    shape = RoundedCornerShape(27.dp),
    color = surfaceColor,
    border = borderStroke,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Subtitles & Audio Tracks (tap for subtitles, long-press for audio)
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .combinedClickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = ripple(bounded = true, radius = 19.dp),
            onClick = onOpenSubtitles,
            onLongClick = onOpenAudio,
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Tune,
          contentDescription = "Tracks",
          tint = Color.White,
          modifier = Modifier.size(22.dp),
        )
      }

      // Chapters & Playlist (tap for playlist if present else chapters, long-press for alternate)
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .combinedClickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = ripple(bounded = true, radius = 19.dp),
            onClick = { if (hasPlaylist) onOpenPlaylist() else onOpenChapters() },
            onLongClick = { if (hasPlaylist) onOpenChapters() else onOpenPlaylist() },
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.List,
          contentDescription = "Chapters",
          tint = Color.White,
          modifier = Modifier.size(24.dp),
        )
      }
    }
  }
}
