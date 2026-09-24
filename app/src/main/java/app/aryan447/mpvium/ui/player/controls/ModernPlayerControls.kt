package app.aryan447.mpvium.ui.player.controls

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.PlayerPreferences
import app.aryan447.mpvium.preferences.SeekbarStyle
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.preferences.preference.deleteAndGet
import app.aryan447.mpvium.preferences.preference.minusAssign
import app.aryan447.mpvium.preferences.preference.plusAssign
import app.aryan447.mpvium.ui.player.Decoder.Companion.getDecoderFromValue
import app.aryan447.mpvium.ui.player.Panels
import app.aryan447.mpvium.ui.player.PlayerActivity
import app.aryan447.mpvium.ui.player.PlayerTitleMode
import app.aryan447.mpvium.ui.player.PlayerUpdates
import app.aryan447.mpvium.ui.player.PlayerViewModel
import app.aryan447.mpvium.ui.player.Sheets
import app.aryan447.mpvium.ui.player.controls.components.ControlsButton
import app.aryan447.mpvium.ui.player.controls.components.PlayerPlayPauseButton
import app.aryan447.mpvium.ui.player.controls.components.PlayerTransportButton
import app.aryan447.mpvium.ui.player.controls.components.SeekbarWithTimers
import app.aryan447.mpvium.ui.player.controls.components.SlideToUnlock
import app.aryan447.mpvium.ui.player.controls.components.sheets.toFixed
import app.aryan447.mpvium.ui.theme.controlColor
import app.aryan447.mpvium.ui.theme.glassPlayerAlpha
import app.aryan447.mpvium.ui.theme.playerRippleConfiguration
import app.aryan447.mpvium.ui.theme.spacing
import app.aryan447.mpvium.utils.media.EpisodeTitleFormatter
import app.aryan447.mpvium.utils.media.MediaInfoParser
import `is`.xyz.mpv.MPVLib
import kotlin.math.roundToInt
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import org.koin.compose.koinInject

/**
 * Modern player interface: a fixed minimal layout (top bar, centered
 * transport, glass bottom bar with seekbar).
 *
 * Shares the Classic player's [PlayerViewModel], MPV backend, [GestureHandler],
 * double-tap seek, sheets, panels, PiP, orientation and lock logic — only the
 * presentation layer differs. Per-user Classic button layouts do not apply here.
 */
@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class,
  ExperimentalFoundationApi::class,
)
@Composable
@Suppress("CyclomaticComplexMethod")
fun ModernPlayerControls(
  viewModel: PlayerViewModel,
  onBackPress: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val playerPreferences = koinInject<PlayerPreferences>()
  val appearancePreferences = koinInject<AppearancePreferences>()
  val hideBackground by appearancePreferences.hidePlayerButtonsBackground.collectAsState()
  val controlsShown by viewModel.controlsShown.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  val pausedForCache by MPVLib.propBoolean["paused-for-cache"].collectAsState()
  val paused by MPVLib.propBoolean["pause"].collectAsState()
  val duration by MPVLib.propInt["duration"].collectAsState()
  val precisePosition by viewModel.precisePosition.collectAsState()
  val preciseDuration by viewModel.preciseDuration.collectAsState()
  val playbackSpeed by MPVLib.propFloat["speed"].collectAsState()
  val doubleTapSeekAmount by viewModel.doubleTapSeekAmount.collectAsState()
  val seekText by viewModel.seekText.collectAsState()
  val showDoubleTapOvals by playerPreferences.showDoubleTapOvals.collectAsState()
  val showSeekTime by playerPreferences.showSeekTimeWhileSeeking.collectAsState()
  val showLoadingCircle by playerPreferences.showLoadingCircle.collectAsState()
  val showSystemStatusBar by playerPreferences.showSystemStatusBar.collectAsState()
  val reduceMotion by playerPreferences.reduceMotion.collectAsState()
  val invertDuration by playerPreferences.invertDuration.collectAsState()
  val playlistMode by playerPreferences.playlistMode.collectAsState()
  val playerTimeToDisappear by playerPreferences.playerTimeToDisappear.collectAsState()
  val customSkipDuration by playerPreferences.customSkipDuration.collectAsState()
  val chapters by viewModel.chapters.collectAsState(persistentListOf())
  val currentChapterRaw by MPVLib.propInt["chapter"].collectAsState()
  val currentChapter = remember(currentChapterRaw, precisePosition, chapters) {
    if (currentChapterRaw != null && currentChapterRaw!! >= 0) {
      currentChapterRaw
    } else if (chapters.isNotEmpty()) {
      chapters.indexOfLast { it.start <= precisePosition }.coerceAtLeast(0)
    } else {
      null
    }
  }
  val mpvDecoder by MPVLib.propString["hwdec-current"].collectAsState()
  val decoder by remember { derivedStateOf { getDecoderFromValue(mpvDecoder ?: "auto") } }
  val abLoopA by viewModel.abLoopA.collectAsState()
  val abLoopB by viewModel.abLoopB.collectAsState()

  val interactionSource = remember { MutableInteractionSource() }
  var isSeeking by remember { mutableStateOf(false) }
  var resetControlsTimestamp by remember { mutableStateOf(0L) }
  var wasPlayerAlreadyPaused by remember { mutableStateOf(false) }
  var isUnlockSliderDragging by remember { mutableStateOf(false) }

  val onOpenSheet: (Sheets) -> Unit = {
    viewModel.sheetShown.update { _ -> it }
    if (it == Sheets.None) {
      viewModel.showControls()
    } else {
      viewModel.hideControls()
      viewModel.panelShown.update { Panels.None }
    }
  }

  val onOpenPanel: (Panels) -> Unit = {
    viewModel.panelShown.update { _ -> it }
    if (it == Panels.None) {
      viewModel.showControls()
    } else {
      viewModel.hideControls()
      viewModel.sheetShown.update { Sheets.None }
    }
  }

  // Clean title, same rules as Classic (episode header + title style).
  val activity = LocalActivity.current as PlayerActivity
  val rawMediaTitle by MPVLib.propString["media-title"].collectAsState()
  val showEpisodeHeader by playerPreferences.showEpisodeHeader.collectAsState()
  val titleMode by playerPreferences.titleMode.collectAsState()
  val metadataRepository = koinInject<StreamingMetadataRepository>()
  var tmdbEpisodeTitle by remember { mutableStateOf<String?>(null) }
  val lookupRaw = rawMediaTitle?.takeIf { it.isNotBlank() } ?: activity.getTitleForControls()
  LaunchedEffect(lookupRaw) {
    val raw = lookupRaw
    val info = MediaInfoParser.parse(raw)
    if (info.type != "tv" || !info.episodeTitle.isNullOrBlank()) {
      tmdbEpisodeTitle = null
    } else {
      val season = info.season
      val episode = info.episode
      tmdbEpisodeTitle = if (season != null && episode != null && info.title.isNotBlank()) {
        runCatching {
          metadataRepository.findCachedEpisodeTitle(info.title, season, episode)
        }.getOrNull()
      } else {
        null
      }
    }
  }
  val modernTitle: Pair<String, String?> = remember(
    rawMediaTitle,
    activity,
    showEpisodeHeader,
    titleMode,
    tmdbEpisodeTitle,
  ) {
    val raw = rawMediaTitle?.takeIf { it.isNotBlank() } ?: activity.getTitleForControls()
    if (!showEpisodeHeader) {
      Pair(raw, null)
    } else {
      val clean = EpisodeTitleFormatter.resolve(raw, tmdbEpisodeTitle)
      when (titleMode) {
        PlayerTitleMode.SingleLine -> Pair(clean?.singleLine ?: raw, null)
        PlayerTitleMode.EpisodeOnly -> Pair(clean?.episodeOnly ?: raw, null)
        PlayerTitleMode.TwoLine ->
          if (clean != null) Pair(clean.showName, clean.episodePart) else Pair(raw, null)
      }
    }
  }
  val (mediaTitle, mediaSubtitle) = modernTitle

  val speedText = remember(playbackSpeed) {
    val speed = playbackSpeed ?: 1f
    val trimmed = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
    "$trimmed×"
  }

  val currentPlayerUpdate by viewModel.playerUpdate.collectAsState()
  // While a horizontal swipe-seek is active, drive the seekbar from the
  // gesture preview instead of the lagging live mpv position.
  val seekPreviewPosition = (currentPlayerUpdate as? PlayerUpdates.HorizontalSeek)?.position

  LaunchedEffect(
    controlsShown,
    paused,
    isSeeking,
    resetControlsTimestamp,
    areControlsLocked,
    isUnlockSliderDragging,
  ) {
    if (controlsShown && paused == false && !isSeeking && !isUnlockSliderDragging) {
      val delayTime = if (areControlsLocked) 2000L else playerTimeToDisappear.toLong()
      delay(delayTime)
      viewModel.hideControls()
    }
  }

  GestureHandler(
    viewModel = viewModel,
    interactionSource = interactionSource,
  )

  DoubleTapToSeekOvals(doubleTapSeekAmount, seekText, showDoubleTapOvals, showSeekTime, showSeekTime, interactionSource)

  CompositionLocalProvider(
    LocalRippleConfiguration provides playerRippleConfiguration,
    LocalPlayerButtonsClickEvent provides { resetControlsTimestamp = System.currentTimeMillis() },
  ) {
    Box(modifier = Modifier.fillMaxSize().then(modifier)) {
      if (pausedForCache == true && showLoadingCircle) {
        LoadingIndicator(
          modifier = Modifier.size(96.dp).align(Alignment.Center),
        )
      }

      AnimatedVisibility(
        visible = controlsShown && !areControlsLocked,
        enter = if (!reduceMotion) {
          fadeIn(playerControlsEnterAnimationSpec())
        } else {
          EnterTransition.None
        },
        exit = if (!reduceMotion) {
          fadeOut(playerControlsExitAnimationSpec())
        } else {
          ExitTransition.None
        },
        modifier = Modifier.fillMaxSize(),
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          ModernTopBar(
            title = mediaTitle,
            subtitle = mediaSubtitle,
            onBackPress = onBackPress,
            onAudioClick = { onOpenSheet(Sheets.AudioTracks) },
            onSubtitlesClick = { onOpenSheet(Sheets.SubtitleTracks) },
            hideBackground = hideBackground,
            modifier = Modifier
              .align(Alignment.TopCenter)
              .then(
                if (showSystemStatusBar) {
                  Modifier.windowInsetsPadding(WindowInsets.statusBars)
                } else {
                  Modifier
                },
              ),
          )

          ModernCenterTransport(
            paused = paused == true,
            buffering = pausedForCache == true,
            skipSeconds = customSkipDuration,
            showNext = playlistMode && viewModel.hasNext(),
            onPlayPause = {
              resetControlsTimestamp = System.currentTimeMillis()
              viewModel.pauseUnpause()
            },
            onSkipBack = {
              resetControlsTimestamp = System.currentTimeMillis()
              viewModel.seekBy(-customSkipDuration)
            },
            onSkipForward = {
              resetControlsTimestamp = System.currentTimeMillis()
              viewModel.seekBy(customSkipDuration)
            },
            onNext = {
              resetControlsTimestamp = System.currentTimeMillis()
              if (viewModel.hasNext()) viewModel.playNext()
            },
            hideBackground = hideBackground,
            modifier = Modifier.align(Alignment.Center),
          )

          ModernBottomBar(
            speedText = speedText,
            onSubtitlesClick = { onOpenSheet(Sheets.SubtitleTracks) },
            onAudioClick = { onOpenSheet(Sheets.AudioTracks) },
            onSpeedClick = { onOpenSheet(Sheets.PlaybackSpeed) },
            onMoreClick = { onOpenSheet(Sheets.More) },
            modifier = Modifier.align(Alignment.BottomCenter),
          ) {
            SeekbarWithTimers(
              position = seekPreviewPosition ?: precisePosition,
              duration = if (preciseDuration > 0) preciseDuration else duration?.toFloat() ?: 0f,
              onValueChange = {
                if (!isSeeking) {
                  wasPlayerAlreadyPaused = paused ?: false
                  if (!wasPlayerAlreadyPaused) {
                    viewModel.pause(transientPause = true)
                  }
                }
                isSeeking = true
                resetControlsTimestamp = System.currentTimeMillis()
                viewModel.seekTo(it.roundToInt(), isScrubbing = true)
              },
              onValueChangeFinished = { finalPosition ->
                val wasScrubbing = isSeeking
                isSeeking = false
                resetControlsTimestamp = System.currentTimeMillis()
                val shouldHoldAndResume =
                  if (wasScrubbing) !wasPlayerAlreadyPaused else !(paused ?: false)
                if (shouldHoldAndResume) {
                  viewModel.seekToAndResume(finalPosition.roundToInt())
                } else {
                  viewModel.seekTo(finalPosition.roundToInt(), isScrubbing = false)
                }
                viewModel.showControls()
              },
              timersInverted = Pair(false, invertDuration),
              durationTimerOnCLick = {
                resetControlsTimestamp = System.currentTimeMillis()
                playerPreferences.invertDuration.set(!invertDuration)
              },
              positionTimerOnClick = {},
              chapters = chapters.toImmutableList(),
              paused = paused ?: false,
              seekbarStyle = SeekbarStyle.Standard,
              loopStart = abLoopA?.toFloat(),
              loopEnd = abLoopB?.toFloat(),
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }

      AnimatedVisibility(
        visible = controlsShown && areControlsLocked,
        enter = if (!reduceMotion) fadeIn() else EnterTransition.None,
        exit = if (!reduceMotion) fadeOut() else ExitTransition.None,
        modifier = Modifier.align(Alignment.BottomCenter),
      ) {
        SlideToUnlock(
          onUnlock = { viewModel.unlockControls() },
          onDraggingChanged = { isDragging -> isUnlockSliderDragging = isDragging },
        )
      }
    }
  }

  val sheetShown by viewModel.sheetShown.collectAsState()
  val subtitles by viewModel.subtitleTracks.collectAsState(persistentListOf())
  val audioTracks by viewModel.audioTracks.collectAsState(persistentListOf())
  val sleepTimerTimeRemaining by viewModel.remainingTime.collectAsState()
  val speedPresets by playerPreferences.speedPresets.collectAsState()

  PlayerSheets(
    viewModel = viewModel,
    sheetShown = sheetShown,
    subtitles = subtitles.toImmutableList(),
    onAddSubtitle = viewModel::addSubtitle,
    onToggleSubtitle = viewModel::toggleSubtitle,
    isSubtitleSelected = viewModel::isSubtitleSelected,
    onRemoveSubtitle = viewModel::removeSubtitle,
    audioTracks = audioTracks.toImmutableList(),
    onAddAudio = viewModel::addAudio,
    onSelectAudio = {
      if (MPVLib.getPropertyInt("aid") == it.id) {
        MPVLib.setPropertyBoolean("aid", false)
      } else {
        MPVLib.setPropertyInt("aid", it.id)
      }
    },
    chapter = chapters.getOrNull(currentChapter ?: 0),
    chapters = chapters.toImmutableList(),
    onSeekToChapter = {
      MPVLib.setPropertyInt("chapter", it)
      viewModel.unpause()
    },
    decoder = decoder,
    onUpdateDecoder = { MPVLib.setPropertyString("hwdec", it.value) },
    speed = playbackSpeed ?: playerPreferences.defaultSpeed.get(),
    onSpeedChange = { MPVLib.setPropertyFloat("speed", it.toFixed(2)) },
    onMakeDefaultSpeed = { playerPreferences.defaultSpeed.set(it.toFixed(2)) },
    onAddSpeedPreset = { playerPreferences.speedPresets += it.toFixed(2).toString() },
    onRemoveSpeedPreset = { playerPreferences.speedPresets -= it.toFixed(2).toString() },
    onResetSpeedPresets = playerPreferences.speedPresets::delete,
    speedPresets = speedPresets.map { it.toFloat() }.sorted(),
    onResetDefaultSpeed = {
      MPVLib.setPropertyFloat("speed", playerPreferences.defaultSpeed.deleteAndGet().toFixed(2))
    },
    sleepTimerTimeRemaining = sleepTimerTimeRemaining,
    onStartSleepTimer = viewModel::startTimer,
    onOpenPanel = onOpenPanel,
    onShowSheet = onOpenSheet,
    onDismissRequest = { onOpenSheet(Sheets.None) },
  )

  val panel by viewModel.panelShown.collectAsState()
  PlayerPanels(
    panelShown = panel,
    onDismissRequest = { onOpenPanel(Panels.None) },
  )
}

@Composable
private fun ModernTopBar(
  title: String,
  subtitle: String?,
  onBackPress: () -> Unit,
  onAudioClick: () -> Unit,
  onSubtitlesClick: () -> Unit,
  hideBackground: Boolean,
  modifier: Modifier = Modifier,
) {
  val spacing = MaterialTheme.spacing
  val titleStyle = MaterialTheme.typography.titleMedium.copy(
    color = Color.White,
    shadow = Shadow(
      color = Color.Black.copy(alpha = 0.6f),
      offset = Offset(0f, 2f),
      blurRadius = 4f,
    ),
  )
  val subtitleStyle = MaterialTheme.typography.bodySmall.copy(
    color = Color.White.copy(alpha = 0.75f),
    shadow = Shadow(
      color = Color.Black.copy(alpha = 0.6f),
      offset = Offset(0f, 1f),
      blurRadius = 3f,
    ),
  )
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(spacing.small),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = spacing.medium, vertical = spacing.small),
  ) {
    ControlsButton(
      icon = Icons.AutoMirrored.Filled.ArrowBack,
      onClick = onBackPress,
      title = stringResource(R.string.modern_player_desc_back),
      color = if (hideBackground) {
        controlColor
      } else {
        MaterialTheme.colorScheme.onSurface
      },
      modifier = Modifier.size(48.dp),
    )
    Column(
      modifier = Modifier.weight(1f),
    ) {
      Text(
        text = title,
        style = titleStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          style = subtitleStyle,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    ControlsButton(
      icon = Icons.Default.Audiotrack,
      onClick = onAudioClick,
      title = stringResource(R.string.modern_player_desc_audio),
      color = if (hideBackground) {
        controlColor
      } else {
        MaterialTheme.colorScheme.onSurface
      },
      modifier = Modifier.size(48.dp),
    )
    ControlsButton(
      icon = Icons.Default.Subtitles,
      onClick = onSubtitlesClick,
      title = stringResource(R.string.modern_player_desc_subtitles),
      color = if (hideBackground) {
        controlColor
      } else {
        MaterialTheme.colorScheme.onSurface
      },
      modifier = Modifier.size(48.dp),
    )
  }
}

@Composable
private fun ModernCenterTransport(
  paused: Boolean,
  buffering: Boolean,
  skipSeconds: Int,
  showNext: Boolean,
  onPlayPause: () -> Unit,
  onSkipBack: () -> Unit,
  onSkipForward: () -> Unit,
  onNext: () -> Unit,
  hideBackground: Boolean,
  modifier: Modifier = Modifier,
) {
  // While buffering, the centered spinner (drawn underneath) is the focus;
  // keep the chrome laid out so it fades back in without a jump.
  Row(
    horizontalArrangement = Arrangement.spacedBy(20.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier,
  ) {
    PlayerTransportButton(
      icon = Icons.Default.FastRewind,
      contentDescription = stringResource(R.string.modern_player_desc_skip_back, skipSeconds),
      onClick = onSkipBack,
      size = 64.dp,
      hideBackground = hideBackground,
    )
    PlayerPlayPauseButton(
      paused = paused,
      onClick = onPlayPause,
      size = 88.dp,
      hideBackground = hideBackground,
    )
    PlayerTransportButton(
      icon = Icons.Default.FastForward,
      contentDescription = stringResource(R.string.modern_player_desc_skip_forward, skipSeconds),
      onClick = onSkipForward,
      size = 64.dp,
      hideBackground = hideBackground,
    )
    if (showNext && !buffering) {
      PlayerTransportButton(
        icon = Icons.Default.SkipNext,
        contentDescription = stringResource(R.string.modern_player_desc_next),
        onClick = onNext,
        size = 64.dp,
        hideBackground = hideBackground,
      )
    }
  }
}

@Composable
private fun ModernBottomBar(
  speedText: String,
  onSubtitlesClick: () -> Unit,
  onAudioClick: () -> Unit,
  onSpeedClick: () -> Unit,
  onMoreClick: () -> Unit,
  modifier: Modifier = Modifier,
  seekbar: @Composable () -> Unit,
) {
  val spacing = MaterialTheme.spacing
  Surface(
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = glassPlayerAlpha()),
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    ),
    modifier = modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.navigationBars),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(spacing.small),
      modifier = Modifier.padding(
        start = spacing.large,
        end = spacing.large,
        top = spacing.medium,
        bottom = spacing.extraLarge,
      ),
    ) {
      seekbar()
      Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        ControlsButton(
          icon = Icons.Default.Subtitles,
          onClick = onSubtitlesClick,
          title = stringResource(R.string.modern_player_desc_subtitles),
          modifier = Modifier.size(48.dp),
        )
        ControlsButton(
          icon = Icons.Default.Audiotrack,
          onClick = onAudioClick,
          title = stringResource(R.string.modern_player_desc_audio),
          modifier = Modifier.size(48.dp),
        )
        ModernSpeedChip(
          speedText = speedText,
          onClick = onSpeedClick,
        )
        ControlsButton(
          icon = Icons.Default.MoreVert,
          onClick = onMoreClick,
          title = stringResource(R.string.modern_player_desc_more),
          modifier = Modifier.size(48.dp),
        )
      }
    }
  }
}

@Composable
private fun ModernSpeedChip(
  speedText: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
      .clip(CircleShape)
      .clickable(
        onClickLabel = stringResource(R.string.modern_player_desc_speed),
        onClick = onClick,
      )
      .padding(horizontal = 12.dp),
  ) {
    Text(
      text = speedText,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
    )
  }
}
