package app.aryan447.mpvium.ui.streaming.series

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.database.repository.CustomSeriesItemDraft
import app.aryan447.mpvium.database.repository.CustomSeriesRepository
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.streaming.LibraryIndexRepository
import app.aryan447.mpvium.domain.streaming.StreamingArtworkStore
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import app.aryan447.mpvium.ui.utils.LocalBackStack
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.repository.MediaFileRepository
import app.aryan447.mpvium.utils.media.MediaInfoParser
import app.aryan447.mpvium.utils.media.MediaLibraryEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File

@Serializable
data class CustomSeriesBuilderScreen(
  val seriesId: Int? = null,
  @Transient val initialVideos: List<Video> = emptyList(),
) : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val customSeriesRepository = koinInject<CustomSeriesRepository>()
    val artworkStore = koinInject<StreamingArtworkStore>()
    val libraryIndexRepository = koinInject<LibraryIndexRepository>()
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var posterPath by remember { mutableStateOf<String?>(null) }
    var selectedPosterUri by remember { mutableStateOf<Uri?>(null) }
    var removePoster by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var episodes by remember { mutableStateOf<List<CustomSeriesItemDraft>>(emptyList()) }
    var isLoading by remember { mutableStateOf(seriesId != null) }
    var isSaving by remember { mutableStateOf(false) }

    var editingEpisodeItem by remember { mutableStateOf<Pair<Int, CustomSeriesItemDraft>?>(null) }
    var showAddEpisodesSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
      if (uri != null) {
        selectedPosterUri = uri
        removePoster = false
      }
    }

    // Load initial data
    LaunchedEffect(seriesId) {
      if (seriesId != null) {
        withContext(Dispatchers.IO) {
          val series = customSeriesRepository.getSeriesById(seriesId)
          val items = customSeriesRepository.getItemsForSeries(seriesId)
          if (series != null) {
            title = series.title
            year = series.year.orEmpty()
            posterPath = series.posterPath
            episodes = items.map {
              CustomSeriesItemDraft(
                id = it.id,
                filePath = it.filePath,
                fileName = it.fileName,
                season = it.season,
                episode = it.episode,
                position = it.position,
              )
            }

            if (!series.posterPath.isNullOrBlank()) {
              val bitmap = artworkStore.loadImage(series.posterPath)
                ?: runCatching { BitmapFactory.decodeFile(series.posterPath) }.getOrNull()
              previewBitmap = bitmap
            }
          }
          isLoading = false
        }
      } else if (initialVideos.isNotEmpty()) {
        val drafts = initialVideos.mapIndexed { index, video ->
          val parsed = MediaInfoParser.parse(video.displayName)
          CustomSeriesItemDraft(
            filePath = video.path,
            fileName = video.displayName,
            season = parsed.season ?: 1,
            episode = parsed.episode ?: (index + 1),
            position = index,
          )
        }
        episodes = drafts
        val firstParsed = MediaInfoParser.parse(initialVideos.first().displayName)
        title = firstParsed.title.ifBlank { "" }
        year = firstParsed.year.orEmpty()
        isLoading = false
      } else {
        isLoading = false
      }
    }

    // Load selected image preview
    LaunchedEffect(selectedPosterUri) {
      val uri = selectedPosterUri
      if (uri != null) {
        val bitmap = withContext(Dispatchers.IO) {
          runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
              BitmapFactory.decodeStream(stream)
            }
          }.getOrNull()
        }
        previewBitmap = bitmap
      }
    }

    val hasUnsavedChanges = remember(title, year, selectedPosterUri, removePoster, episodes) {
      title.isNotBlank() || episodes.isNotEmpty() || selectedPosterUri != null
    }

    val onBack = {
      if (hasUnsavedChanges && !isSaving) {
        showDiscardDialog = true
      } else if (backstack.size > 1) {
        backstack.removeLastOrNull()
      } else {
        (context as? android.app.Activity)?.finish()
      }
    }

    BackHandler(onBack = { onBack() })

    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
      // Exclude header items from drag indices
      val fromEpIndex = from.index - 1
      val toEpIndex = to.index - 1
      if (fromEpIndex in episodes.indices && toEpIndex in episodes.indices) {
        val updated = episodes.toMutableList()
        val item = updated.removeAt(fromEpIndex)
        updated.add(toEpIndex, item)
        episodes = updated.mapIndexed { idx, draft -> draft.copy(position = idx) }
      }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = if (seriesId != null) stringResource(R.string.edit_series) else stringResource(R.string.new_series),
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
          },
          navigationIcon = {
            IconButton(onClick = { onBack() }) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.generic_cancel))
            }
          },
          actions = {
            Button(
              onClick = {
                if (title.isBlank()) return@Button
                isSaving = true
                coroutineScope.launch {
                  val finalPoster = withContext(Dispatchers.IO) {
                    when {
                      selectedPosterUri != null -> {
                        artworkStore.storeCustomArtwork(selectedPosterUri!!, keyHint = title)
                      }
                      removePoster -> null
                      else -> posterPath
                    }
                  }

                  withContext(Dispatchers.IO) {
                    if (seriesId == null) {
                      customSeriesRepository.createSeries(
                        title = title,
                        year = year.ifBlank { null },
                        posterPath = finalPoster,
                        items = episodes,
                      )
                    } else {
                      customSeriesRepository.updateSeries(
                        id = seriesId,
                        title = title,
                        year = year.ifBlank { null },
                        posterPath = finalPoster,
                        items = episodes,
                      )
                    }
                    MediaLibraryEvents.notifyChanged()
                  }

                  Toast.makeText(context, context.getString(R.string.custom_series_saved), Toast.LENGTH_SHORT).show()
                  if (backstack.size > 1) {
                    backstack.removeLastOrNull()
                  } else {
                    (context as? android.app.Activity)?.finish()
                  }
                }
              },
              enabled = title.isNotBlank() && episodes.isNotEmpty() && !isSaving,
              shape = RoundedCornerShape(20.dp),
              modifier = Modifier.padding(end = 8.dp),
            ) {
              if (isSaving) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              } else {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.metadata_save))
              }
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
      },
      floatingActionButton = {
        FloatingActionButton(
          onClick = { showAddEpisodesSheet = true },
          shape = CircleShape,
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.custom_series_add_episodes))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.custom_series_add_episodes), fontWeight = FontWeight.SemiBold)
          }
        }
      },
    ) { innerPadding ->
      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
        return@Scaffold
      }

      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .imePadding(),
        contentPadding = PaddingValues(bottom = 96.dp),
      ) {
        // Form Header
        item(key = "header_section") {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalAlignment = Alignment.Top,
            ) {
              // Poster Picker Card
              Box(
                modifier = Modifier
                  .width(110.dp)
                  .height(160.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                  .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp),
                  )
                  .clickable {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                  },
                contentAlignment = Alignment.Center,
              ) {
                if (previewBitmap != null && !removePoster) {
                  Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.metadata_field_poster),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                  IconButton(
                    onClick = {
                      removePoster = true
                      selectedPosterUri = null
                      previewBitmap = null
                      posterPath = null
                    },
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .padding(4.dp)
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(Color.Black.copy(alpha = 0.6f)),
                  ) {
                    Icon(
                      Icons.Filled.Close,
                      contentDescription = stringResource(R.string.metadata_remove_poster),
                      tint = Color.White,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                } else {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(8.dp),
                  ) {
                    Icon(
                      Icons.Filled.AddPhotoAlternate,
                      contentDescription = stringResource(R.string.custom_series_pick_poster),
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(32.dp),
                    )
                    Text(
                      text = stringResource(R.string.custom_series_pick_poster),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontWeight = FontWeight.Medium,
                    )
                  }
                }
              }

              // Name & Year Inputs
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                OutlinedTextField(
                  value = title,
                  onValueChange = { title = it },
                  label = { Text(stringResource(R.string.metadata_field_title)) },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp),
                )

                OutlinedTextField(
                  value = year,
                  onValueChange = { input ->
                    year = input.filter { it.isDigit() }.take(4)
                  },
                  label = { Text(stringResource(R.string.metadata_field_year)) },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Episodes Section Header
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = stringResource(R.string.custom_series_builder_title),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = stringResource(R.string.custom_series_episodes_count, episodes.size),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              if (episodes.size > 1) {
                Text(
                  text = stringResource(R.string.custom_series_drag_to_reorder),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }

        // Reorderable Episode Items
        itemsIndexed(
          items = episodes,
          key = { _, item -> item.filePath },
        ) { index, item ->
          ReorderableItem(
            state = reorderableState,
            key = item.filePath,
          ) { isDragging ->
            val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .shadow(elevation, RoundedCornerShape(12.dp)),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isDragging) {
                  MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                  MaterialTheme.colorScheme.surfaceContainer
                },
              ),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                // Drag handle
                Icon(
                  imageVector = Icons.Filled.DragHandle,
                  contentDescription = stringResource(R.string.custom_series_drag_to_reorder),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier
                    .draggableHandle()
                    .padding(end = 10.dp),
                )

                // Episode info
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )

                  // Season & Episode Chips (clickable to edit numbers explicitly)
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    AssistChip(
                      onClick = { editingEpisodeItem = index to item },
                      label = {
                        Text(
                          text = "S${item.season.toString().padStart(2, '0')} · E${item.episode.toString().padStart(2, '0')}",
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                        )
                      },
                      colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                      ),
                      border = null,
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.height(28.dp),
                    )
                  }
                }

                // Remove item button
                IconButton(
                  onClick = {
                    episodes = episodes.filterIndexed { i, _ -> i != index }
                      .mapIndexed { idx, draft -> draft.copy(position = idx) }
                  },
                ) {
                  Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    // Dialog: Edit Season & Episode explicitly
    editingEpisodeItem?.let { (index, item) ->
      SeasonEpisodeEditDialog(
        currentSeason = item.season,
        currentEpisode = item.episode,
        fileName = item.fileName,
        onDismiss = { editingEpisodeItem = null },
        onConfirm = { newSeason, newEpisode ->
          val updated = episodes.toMutableList()
          updated[index] = item.copy(season = newSeason, episode = newEpisode)
          episodes = updated
          editingEpisodeItem = null
        },
      )
    }

    // Sheet: Add episodes from library videos
    if (showAddEpisodesSheet) {
      VideoPickerSheet(
        alreadyAddedPaths = episodes.map { it.filePath }.toSet(),
        onDismiss = { showAddEpisodesSheet = false },
        onVideosSelected = { selectedVideos ->
          val maxSeason = episodes.maxOfOrNull { it.season } ?: 1
          var nextEp = (episodes.filter { it.season == maxSeason }.maxOfOrNull { it.episode } ?: 0) + 1
          val startPos = episodes.size

          val newDrafts = selectedVideos.mapIndexed { idx, video ->
            val parsed = MediaInfoParser.parse(video.displayName)
            val season = parsed.season ?: maxSeason
            val ep = if (parsed.season != null && parsed.episode != null) {
              parsed.episode
            } else {
              val epNum = nextEp
              nextEp++
              epNum
            }
            CustomSeriesItemDraft(
              filePath = video.path,
              fileName = video.displayName,
              season = season,
              episode = ep,
              position = startPos + idx,
            )
          }

          episodes = episodes + newDrafts
          if (title.isBlank() && episodes.isNotEmpty()) {
            val parsedFirst = MediaInfoParser.parse(episodes.first().fileName)
            title = parsedFirst.title.ifBlank { "" }
            if (year.isBlank()) year = parsedFirst.year.orEmpty()
          }
          showAddEpisodesSheet = false
        },
      )
    }

    // Discard changes dialog
    if (showDiscardDialog) {
      AlertDialog(
        onDismissRequest = { showDiscardDialog = false },
        containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
        title = { Text("Discard unsaved changes?") },
        text = { Text("You have unsaved changes that will be lost if you leave.") },
        confirmButton = {
          TextButton(
            onClick = {
              showDiscardDialog = false
              if (backstack.size > 1) {
                backstack.removeLastOrNull()
              } else {
                (context as? android.app.Activity)?.finish()
              }
            },
          ) {
            Text("Discard", color = MaterialTheme.colorScheme.error)
          }
        },
        dismissButton = {
          TextButton(onClick = { showDiscardDialog = false }) {
            Text(stringResource(R.string.generic_cancel))
          }
        },
      )
    }
  }
}

@Composable
private fun SeasonEpisodeEditDialog(
  currentSeason: Int,
  currentEpisode: Int,
  fileName: String,
  onDismiss: () -> Unit,
  onConfirm: (season: Int, episode: Int) -> Unit,
) {
  var seasonText by remember { mutableStateOf(currentSeason.toString()) }
  var episodeText by remember { mutableStateOf(currentEpisode.toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
    title = {
      Text(
        text = stringResource(R.string.custom_series_season_episode_dialog_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = fileName,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedTextField(
            value = seasonText,
            onValueChange = { input -> seasonText = input.filter { it.isDigit() } },
            label = { Text(stringResource(R.string.custom_series_season_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
          )

          OutlinedTextField(
            value = episodeText,
            onValueChange = { input -> episodeText = input.filter { it.isDigit() } },
            label = { Text(stringResource(R.string.custom_series_episode_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val s = seasonText.toIntOrNull()?.coerceAtLeast(0) ?: currentSeason
          val e = episodeText.toIntOrNull()?.coerceAtLeast(1) ?: currentEpisode
          onConfirm(s, e)
        },
      ) {
        Text(stringResource(R.string.generic_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.generic_cancel))
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPickerSheet(
  alreadyAddedPaths: Set<String>,
  onDismiss: () -> Unit,
  onVideosSelected: (List<Video>) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current
  val libraryIndexRepository = koinInject<LibraryIndexRepository>()

  var allVideos by remember { mutableStateOf<List<Video>>(emptyList()) }
  var searchQuery by remember { mutableStateOf("") }
  val selectedPaths = remember { mutableStateOf(setOf<String>()) }
  var isLoading by remember { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      val library = libraryIndexRepository.getLibrary()
      val bucketIds = library.folders.map { it.bucketId }.toSet()
      val listFromBuckets = MediaFileRepository.getVideosForBuckets(context, bucketIds)
      val listFromLibrary = library.series.flatMap { it.seasons.values.flatten().map { ep -> ep.video } } + library.movies.map { it.video }
      val unique = (listFromBuckets + listFromLibrary).distinctBy { it.path }.sortedBy { it.displayName.lowercase() }
      allVideos = unique
      isLoading = false
    }
  }

  val filteredVideos = remember(allVideos, searchQuery, alreadyAddedPaths) {
    val unadded = allVideos.filter { it.path !in alreadyAddedPaths }
    if (searchQuery.isBlank()) {
      unadded
    } else {
      unadded.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .navigationBarsPadding(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.custom_series_add_episodes),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )

        Button(
          onClick = {
            val selected = allVideos.filter { it.path in selectedPaths.value }
            onVideosSelected(selected)
          },
          enabled = selectedPaths.value.isNotEmpty(),
          shape = RoundedCornerShape(16.dp),
        ) {
          Text("Add (${selectedPaths.value.size})")
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      } else if (filteredVideos.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "No videos available to add",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(380.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          itemsIndexed(filteredVideos, key = { _, v -> v.path }) { _, video ->
            val isChecked = video.path in selectedPaths.value
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  val current = selectedPaths.value
                  selectedPaths.value = if (isChecked) current - video.path else current + video.path
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = isChecked,
                onCheckedChange = { checked ->
                  val current = selectedPaths.value
                  selectedPaths.value = if (checked) current + video.path else current - video.path
                },
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = video.displayName,
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = File(video.path).parentFile?.name ?: "",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }
  }
}
