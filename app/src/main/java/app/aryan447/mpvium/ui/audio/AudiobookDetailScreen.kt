package app.aryan447.mpvium.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import app.aryan447.mpvium.domain.audio.AudioCollectionsRepository
import app.aryan447.mpvium.domain.audio.model.LocalAudiobook
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.audio.components.AlbumArtImage
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassRimBrush
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data class AudiobookDetailScreen(
  val audiobookId: String,
  val bookTitle: String,
) : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val collectionsRepo = koinInject<AudioCollectionsRepository>()
    val audiobooks by collectionsRepo.audiobooks.collectAsState()
    val isGlass = LocalGlass.current
    val dark = isSystemInDarkTheme()

    val book = audiobooks.firstOrNull { it.id == audiobookId }
    var showSpeedSheet by remember { mutableStateOf(false) }

    Scaffold(
      containerColor = Color.Transparent,
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = book?.title ?: bookTitle,
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
      if (book == null) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = stringResource(R.string.audiobook_not_found),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
          contentPadding = PaddingValues(bottom = 32.dp),
        ) {
          // Header Card
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Box(
                modifier = Modifier
                  .size(180.dp)
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(20.dp))
                  .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                  .then(
                    if (isGlass) {
                      Modifier.border(1.dp, glassRimBrush(dark), RoundedCornerShape(20.dp))
                    } else {
                      Modifier
                    }
                  ),
              ) {
                AlbumArtImage(
                  artworkUri = book.artworkUri,
                  contentDescription = book.title,
                  modifier = Modifier.fillMaxSize(),
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                text = book.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = book.author,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = "${stringResource(R.string.audiobook_chapters_count, book.chapterCount)} • ${book.totalDurationFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              // Progress Bar
              if (book.progressPercentage > 0f) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxHeight()
                      .fillMaxWidth(book.progressPercentage.coerceIn(0.02f, 1f))
                      .background(MaterialTheme.colorScheme.primary),
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Action Buttons: Resume & Playback Speed
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Button(
                  onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    AudioPlayerLauncher.playAudiobook(context, book, book.currentPartIndex)
                  },
                  modifier = Modifier.weight(1f),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                  ),
                ) {
                  Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  val buttonLabel = if (book.progressPercentage > 0f && !book.isCompleted) {
                    stringResource(R.string.audiobook_resume_part, book.currentPartIndex + 1)
                  } else {
                    stringResource(R.string.audio_play_all)
                  }
                  Text(
                    text = buttonLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                FilledTonalButton(
                  onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showSpeedSheet = true
                  },
                  modifier = Modifier.weight(0.7f),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = String.format("%.2fx", book.playbackSpeed),
                  )
                }
              }
            }
          }

          item {
            Text(
              text = stringResource(R.string.audiobook_chapters_header),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            )
          }

          itemsIndexed(book.parts, key = { _, track -> track.id }) { index, track ->
            val isCurrentPart = index == book.currentPartIndex && book.progressPercentage > 0f
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  AudioPlayerLauncher.playAudiobook(context, book, index)
                }
                .padding(horizontal = 20.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = if (isCurrentPart) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (isCurrentPart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
              )

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = track.title,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrentPart) FontWeight.Bold else FontWeight.Medium,
                  ),
                  color = if (isCurrentPart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                  text = track.durationFormatted,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              if (index < book.currentPartIndex) {
                Icon(
                  imageVector = Icons.Filled.Check,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }

        // Playback Speed Bottom Sheet
        if (showSpeedSheet) {
          val sheetState = rememberModalBottomSheetState()
          ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
              Text(
                text = stringResource(R.string.audiobook_speed_dialog_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = stringResource(R.string.audiobook_speed_dialog_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              Spacer(modifier = Modifier.height(16.dp))

              val speeds = listOf(0.75, 1.0, 1.25, 1.5, 1.75, 2.0)
              speeds.forEach { speed ->
                val isSelected = kotlin.math.abs(book.playbackSpeed - speed) < 0.01
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      scope.launch {
                        collectionsRepo.setBookSpeed(book.id, speed)
                        showSpeedSheet = false
                      }
                    }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = String.format("%.2fx", speed),
                    style = MaterialTheme.typography.bodyLarge.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                  )
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Filled.Check,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(24.dp))
            }
          }
        }
      }
    }
  }
}
