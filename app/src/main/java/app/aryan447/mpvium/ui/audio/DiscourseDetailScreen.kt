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
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import app.aryan447.mpvium.domain.audio.model.AudioChapter
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.audio.components.AlbumArtImage
import app.aryan447.mpvium.ui.audio.components.PartChaptersSheet
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassRimBrush
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data class DiscourseDetailScreen(
  val discourseId: String,
  val collectionTitle: String,
) : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val collectionsRepo = koinInject<AudioCollectionsRepository>()
    val discourses by collectionsRepo.discourses.collectAsState()
    val isGlass = LocalGlass.current
    val dark = isSystemInDarkTheme()

    val discourse = discourses.firstOrNull { it.id == discourseId }

    var selectedPartForChapters by remember { mutableStateOf<LocalAudioTrack?>(null) }
    var partChapters by remember { mutableStateOf<List<AudioChapter>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
      containerColor = Color.Transparent,
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = discourse?.collection ?: collectionTitle,
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
      if (discourse == null) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = stringResource(R.string.discourse_not_found),
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
          // Header
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
                  artworkUri = discourse.artworkUri,
                  albumTitle = discourse.collection,
                  artistName = discourse.speaker,
                  contentDescription = discourse.collection,
                  modifier = Modifier.fillMaxSize(),
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                text = discourse.collection,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = discourse.speaker,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = "${stringResource(R.string.discourse_parts_count, discourse.partCount)} • ${discourse.totalDurationFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              if (discourse.progressPercentage > 0f) {
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
                      .fillMaxWidth(discourse.progressPercentage.coerceIn(0.02f, 1f))
                      .background(MaterialTheme.colorScheme.primary),
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Resume Button (resumes across parts)
              Button(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  AudioPlayerLauncher.playDiscourse(context, discourse, discourse.currentPartIndex)
                },
                modifier = Modifier.fillMaxWidth(),
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
                val buttonLabel = if (discourse.progressPercentage > 0f && !discourse.isCompleted) {
                  stringResource(R.string.discourse_resume_part, discourse.currentPartIndex + 1)
                } else {
                  stringResource(R.string.audio_play_all)
                }
                Text(text = buttonLabel)
              }
            }
          }

          item {
            Text(
              text = stringResource(R.string.discourse_lectures_header),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            )
          }

          itemsIndexed(discourse.parts, key = { _, track -> track.id }) { index, track ->
            val isCurrentPart = index == discourse.currentPartIndex && discourse.progressPercentage > 0f
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  AudioPlayerLauncher.playDiscourse(context, discourse, index)
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

              // Chapter list per part button
              IconButton(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  scope.launch {
                    val ch = collectionsRepo.getChaptersForPart(track)
                    partChapters = ch
                    selectedPartForChapters = track
                  }
                },
              ) {
                Icon(
                  imageVector = Icons.Filled.FormatListNumbered,
                  contentDescription = stringResource(R.string.chapters_per_part_action),
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
              }

              if (index < discourse.currentPartIndex) {
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

        // Chapter List Per Part Bottom Sheet
        selectedPartForChapters?.let { partTrack ->
          PartChaptersSheet(
            track = partTrack,
            chapters = partChapters,
            onChapterClick = { chapter ->
              selectedPartForChapters = null
              val partIndex = discourse.parts.indexOf(partTrack).coerceAtLeast(0)
              AudioPlayerLauncher.playDiscourse(context, discourse, partIndex)
            },
            onDismissRequest = { selectedPartForChapters = null },
            sheetState = sheetState,
          )
        }
      }
    }
  }
}
