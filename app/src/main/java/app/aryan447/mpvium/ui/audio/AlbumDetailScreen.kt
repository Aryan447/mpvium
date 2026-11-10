package app.aryan447.mpvium.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.domain.audio.AudioRepository
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.audio.components.AlbumArtImage
import app.aryan447.mpvium.ui.audio.components.TrackRow
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassRimBrush
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data class AlbumDetailScreen(
  val albumId: String,
  val albumTitle: String,
) : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val haptic = LocalHapticFeedback.current
    val audioRepository = koinInject<AudioRepository>()
    val libraryData by audioRepository.libraryData.collectAsState()
    val isGlass = LocalGlass.current
    val dark = isSystemInDarkTheme()

    val album = libraryData.albums.firstOrNull { it.id == albumId }

    Scaffold(
      containerColor = Color.Transparent,
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = album?.title ?: albumTitle,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          navigationIcon = {
            IconButton(onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              backstack.removeLastOrNull()
            }) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
          ),
        )
      },
    ) { padding ->
      if (album == null) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = stringResource(R.string.audio_empty_no_audio_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        return@Scaffold
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      ) {
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(
                  if (isGlass) {
                    Modifier.border(1.dp, glassRimBrush(dark), RoundedCornerShape(16.dp))
                  } else {
                    Modifier
                  }
                ),
            ) {
              AlbumArtImage(
                album = album,
                albumTitle = album?.title ?: albumTitle,
                artworkUri = album?.artworkUri,
                contentDescription = album?.title ?: albumTitle,
                modifier = Modifier.fillMaxSize(),
              )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.Center,
            ) {
              Text(
                text = album.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = album.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )

              Spacer(modifier = Modifier.height(4.dp))

              val meta = buildString {
                if (album.year > 0) append("${album.year} • ")
                append(context.getString(R.string.audio_track_count, album.trackCount))
                if (album.durationFormatted.isNotBlank()) append(" • ${album.durationFormatted}")
              }
              Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
              )
            }
          }
        }

        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Button(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                AudioPlayerLauncher.playAlbum(context, album, 0)
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(32.dp),
            ) {
              Icon(Icons.Filled.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(R.string.audio_play_all))
            }

            FilledTonalButton(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val shuffledIndices = album.tracks.indices.shuffled()
                val start = shuffledIndices.firstOrNull() ?: 0
                AudioPlayerLauncher.playAlbum(context, album, start)
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(32.dp),
            ) {
              Icon(Icons.Filled.Shuffle, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(R.string.audio_shuffle))
            }
          }
        }

        itemsIndexed(album.tracks, key = { _, track -> track.id }) { index, track ->
          TrackRow(
            track = track,
            trackNumber = if (track.trackNumber > 0) track.trackNumber else index + 1,
            showArtwork = false,
            onClick = {
              AudioPlayerLauncher.playAlbum(context, album, index)
            },
          )
        }
      }
    }
  }
}
