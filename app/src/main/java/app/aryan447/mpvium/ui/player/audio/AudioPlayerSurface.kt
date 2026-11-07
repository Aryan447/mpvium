package app.aryan447.mpvium.ui.player.audio

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.ui.audio.components.AlbumArtImage
import app.aryan447.mpvium.ui.player.PlayerActivity
import app.aryan447.mpvium.ui.player.PlayerViewModel
import app.aryan447.mpvium.ui.player.RepeatMode
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassRimBrush
import is.xyz.mpv.MPVLib
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerSurface(
  viewModel: PlayerViewModel,
  onBackPress: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val isGlass = LocalGlass.current
  val dark = isSystemInDarkTheme()

  val title by viewModel.audioTitle.collectAsState()
  val artist by viewModel.audioArtist.collectAsState()
  val album by viewModel.audioAlbum.collectAsState()
  val artworkUri by viewModel.audioArtUri.collectAsState()

  val paused by MPVLib.propBoolean["pause"].collectAsState()
  val isPlaying = paused == false
  val pos by MPVLib.propInt["time-pos"].collectAsState()
  val duration by MPVLib.propInt["duration"].collectAsState()
  val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
  val repeatMode by viewModel.repeatMode.collectAsState()

  val currentPosSec = pos ?: 0
  val durationSec = duration ?: 0

  var isUserScrubbing by remember { mutableStateOf(false) }
  var scrubPositionSec by remember { mutableFloatStateOf(0f) }

  var showQueueSheet by remember { mutableStateOf(false) }
  val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
  val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.surface,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
              MaterialTheme.colorScheme.surface,
            )
          )
        )
        .padding(
          top = statusBarPadding.calculateTopPadding(),
          bottom = navBarPadding.calculateBottomPadding(),
        ),
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          IconButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onBackPress()
          }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.back),
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }

          Text(
            text = stringResource(R.string.audio_now_playing),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
          )

          IconButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showQueueSheet = true
          }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.QueueMusic,
              contentDescription = stringResource(R.string.audio_queue),
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center Album Artwork
        Box(
          modifier = Modifier
            .weight(1f, fill = false)
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
              if (isGlass) {
                Modifier.border(1.dp, glassRimBrush(dark), RoundedCornerShape(24.dp))
              } else {
                Modifier
              }
            ),
        ) {
          AlbumArtImage(
            artworkUri = artworkUri,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            targetSizePx = 600,
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Track Details (Title, Artist, Album)
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = title.ifBlank { stringResource(R.string.audio_unknown_track) },
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee(),
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = artist.ifBlank { stringResource(R.string.audio_unknown_artist) },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          if (album.isNotBlank() && album != title) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = album,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              textAlign = TextAlign.Center,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seek Bar & Timers
        Column(modifier = Modifier.fillMaxWidth()) {
          val sliderValue = if (isUserScrubbing) {
            scrubPositionSec
          } else {
            currentPosSec.toFloat()
          }
          val sliderMax = durationSec.coerceAtLeast(1).toFloat()

          Slider(
            value = sliderValue.coerceIn(0f, sliderMax),
            onValueChange = {
              isUserScrubbing = true
              scrubPositionSec = it
            },
            onValueChangeFinished = {
              viewModel.seekTo(scrubPositionSec.toInt())
              isUserScrubbing = false
            },
            valueRange = 0f..sliderMax,
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            modifier = Modifier.fillMaxWidth(),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            val displayPos = if (isUserScrubbing) scrubPositionSec.toInt() else currentPosSec
            Text(
              text = formatTimecode(displayPos),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = formatTimecode(durationSec),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls Row: Shuffle | Prev | Play/Pause | Next | Repeat
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              viewModel.toggleShuffle()
            },
          ) {
            Icon(
              imageVector = Icons.Filled.Shuffle,
              contentDescription = stringResource(R.string.audio_shuffle),
              tint = if (shuffleEnabled) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
              },
            )
          }

          IconButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              viewModel.playPrevious()
            },
            enabled = viewModel.hasPrevious(),
          ) {
            Icon(
              imageVector = Icons.Default.SkipPrevious,
              contentDescription = stringResource(R.string.player_previous),
              modifier = Modifier.size(36.dp),
              tint = if (viewModel.hasPrevious()) {
                MaterialTheme.colorScheme.onSurface
              } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
              },
            )
          }

          FilledIconButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              viewModel.pauseUnpause()
            },
            shape = CircleShape,
            modifier = Modifier.size(68.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (isPlaying) {
                stringResource(R.string.player_pause)
              } else {
                stringResource(R.string.player_play)
              },
              modifier = Modifier.size(36.dp),
            )
          }

          IconButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              viewModel.playNext()
            },
            enabled = viewModel.hasNext(),
          ) {
            Icon(
              imageVector = Icons.Default.SkipNext,
              contentDescription = stringResource(R.string.player_next),
              modifier = Modifier.size(36.dp),
              tint = if (viewModel.hasNext()) {
                MaterialTheme.colorScheme.onSurface
              } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
              },
            )
          }

          IconButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              viewModel.cycleRepeatMode()
            },
          ) {
            val repeatIcon = when (repeatMode) {
              RepeatMode.ONE -> Icons.Filled.RepeatOne
              else -> Icons.Filled.Repeat
            }
            val repeatTint = when (repeatMode) {
              RepeatMode.OFF -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
              else -> MaterialTheme.colorScheme.primary
            }
            Icon(
              imageVector = repeatIcon,
              contentDescription = stringResource(R.string.player_repeat),
              tint = repeatTint,
            )
          }
        }
      }
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
      val playerActivity = context as? PlayerActivity
      val playlist = playerActivity?.playlist ?: emptyList()
      val currentIndex = playerActivity?.playlistIndex ?: 0

      ModalBottomSheet(
        onDismissRequest = { showQueueSheet = false },
        sheetState = queueSheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
          Text(
            text = stringResource(R.string.audio_queue),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
          )

          if (playlist.isEmpty()) {
            Text(
              text = stringResource(R.string.audio_empty_no_audio_title),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 16.dp),
            )
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
              contentPadding = PaddingValues(bottom = 16.dp),
            ) {
              itemsIndexed(playlist) { index, uri ->
                val isSelected = index == currentIndex
                val itemTitle = playerActivity?.getPlaylistItemTitle(uri) ?: (uri.lastPathSegment ?: "")

                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                      if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                      } else {
                        Color.Transparent
                      }
                    )
                    .clickable {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      viewModel.playPlaylistItem(index)
                      showQueueSheet = false
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.width(32.dp),
                  )

                  Text(
                    text = itemTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (isSelected) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun formatTimecode(seconds: Int): String {
  val s = seconds.coerceAtLeast(0)
  val m = s / 60
  val remS = s % 60
  val h = m / 60
  val remM = m % 60
  return if (h > 0) {
    String.format(Locale.US, "%d:%02d:%02d", h, remM, remS)
  } else {
    String.format(Locale.US, "%d:%02d", remM, remS)
  }
}
