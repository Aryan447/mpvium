package app.aryan447.mpvium.ui.streaming.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.domain.streaming.SeriesDetector
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.domain.streaming.model.LocalEpisode
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult
import app.aryan447.mpvium.ui.browser.dialogs.DeleteConfirmationDialog
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.streaming.components.StreamingImage
import app.aryan447.mpvium.ui.utils.LocalBackStack
import app.aryan447.mpvium.utils.media.MediaUtils
import app.aryan447.mpvium.utils.permission.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data class SeriesDetailScreen(
  val seriesId: String,
) : Screen {

  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val seriesDetector = koinInject<SeriesDetector>()
    val metadataRepository = koinInject<StreamingMetadataRepository>()
    val coroutineScope = rememberCoroutineScope()

    var series by remember { mutableStateOf<LocalSeries?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedSeason by remember { mutableIntStateOf(1) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var pendingDeletion by remember { mutableStateOf<Pair<String, List<Video>>?>(null) }
    var showMatchPicker by remember { mutableStateOf(false) }
    var matchOptions by remember { mutableStateOf<List<WyzieTmdbResult>>(emptyList()) }
    var isSearchingMatches by remember { mutableStateOf(false) }
    var matchQuery by remember { mutableStateOf("") }

    LaunchedEffect(seriesId, reloadKey) {
      withContext(Dispatchers.IO) {
        val detected = seriesDetector.detectLibrary()
        val found = detected.series.find { it.id == seriesId }
        if (found != null) {
          series = found
          selectedSeason = found.seasons.keys.firstOrNull() ?: 1
          isLoading = false

          // Enrich in background; manual reload (reloadKey > 0) forces a re-fetch
          val enriched = metadataRepository.enrichSeries(found, forceRefresh = reloadKey > 0)
          series = enriched
        } else {
          isLoading = false
        }
      }
    }

    // Search candidates for the match picker; re-runnable with a typed query
    fun searchMatches(query: String) {
      if (query.isBlank()) return
      coroutineScope.launch {
        isSearchingMatches = true
        matchOptions = withContext(Dispatchers.IO) {
          metadataRepository.searchSeriesMatches(query)
        }
        isSearchingMatches = false
      }
    }

    // Load search candidates whenever the match picker opens
    LaunchedEffect(showMatchPicker) {
      val target = series
      if (showMatchPicker && target != null) {
        matchQuery = target.title
        searchMatches(target.title)
      }
    }

    Scaffold { innerPadding ->
      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      } else if (series == null) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          Text("Series not found", style = MaterialTheme.typography.bodyLarge)
        }
      } else {
        val currentSeries = series!!
        val episodesForSeason = currentSeries.seasons[selectedSeason] ?: emptyList()
        val backdropUrl = currentSeries.backdropUrl ?: currentSeries.posterUrl
        val fallbackVideo = currentSeries.seasons.values.firstOrNull()?.firstOrNull()?.video

        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        ) {
          // Backdrop Header
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            ) {
              StreamingImage(
                url = backdropUrl,
                fallbackVideo = fallbackVideo,
                isSeries = true,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )

              // Gradient Scrim
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      0.0f to Color.Black.copy(alpha = 0.35f),
                      0.5f to Color.Black.copy(alpha = 0.40f),
                      0.85f to MaterialTheme.colorScheme.background.copy(alpha = 0.90f),
                      1.0f to MaterialTheme.colorScheme.background,
                    )
                  )
              )

              // Back Button
              IconButton(
                onClick = {
                  if (backstack.size > 1) {
                    backstack.removeLastOrNull()
                  } else {
                    (context as? android.app.Activity)?.finish()
                  }
                },
                modifier = Modifier
                  .padding(top = 40.dp, start = 12.dp)
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.5f)),
              ) {
                Icon(
                  Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  tint = Color.White,
                )
              }

              Row(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(top = 40.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                IconButton(
                  onClick = {
                    showMatchPicker = true
                  },
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                  Icon(Icons.Filled.Refresh, contentDescription = "Fix match from search", tint = Color.White)
                }
                IconButton(
                  onClick = {
                    pendingDeletion = currentSeries.title to currentSeries.seasons.values.flatten().map { it.video }
                  },
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                  Icon(Icons.Filled.Delete, contentDescription = "Delete series", tint = Color.White)
                }
              }
            }
          }

          // Series Information Header
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            ) {
              // Badges Row
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                if (currentSeries.rating != null && currentSeries.rating > 0f) {
                  Surface(
                    color = Color(0xFFE5A00D).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(6.dp),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp),
                      )
                      Spacer(modifier = Modifier.width(3.dp))
                      Text(
                        text = String.format(java.util.Locale.US, "%.1f", currentSeries.rating),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                      )
                    }
                  }
                }

                Surface(
                  color = MaterialTheme.colorScheme.surfaceContainerHighest,
                  shape = RoundedCornerShape(6.dp),
                ) {
                  Text(
                    text = "${currentSeries.seasonCount} Seasons • ${currentSeries.totalEpisodes} Episodes",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                  )
                }

                if (!currentSeries.year.isNullOrBlank()) {
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(6.dp),
                  ) {
                    Text(
                      text = currentSeries.year,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Title
              Text(
                text = currentSeries.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
              )

              // Synopsis / Overview
              currentSeries.overview?.let { overview ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = overview,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Big Play Button
              val playNext = currentSeries.nextEpisodeToWatch ?: currentSeries.seasons.values.firstOrNull()?.firstOrNull()
              if (playNext != null) {
                Button(
                  onClick = { MediaUtils.playFile(playNext.video, context, "series_detail_play") },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                  ),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Play ${playNext.formattedEpisodeTag}: ${playNext.displayTitle}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              Spacer(modifier = Modifier.height(20.dp))

              // Season Selector Tabs
              if (currentSeries.seasons.size > 1) {
                Text(
                  text = "Seasons",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.padding(bottom = 12.dp),
                ) {
                  items(currentSeries.seasons.keys.toList()) { seasonNum ->
                    val selected = seasonNum == selectedSeason
                    FilterChip(
                      selected = selected,
                      onClick = { selectedSeason = seasonNum },
                      label = { Text("Season $seasonNum") },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                      ),
                      shape = RoundedCornerShape(20.dp),
                    )
                  }
                }
              } else {
                Text(
                  text = "Episodes",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  modifier = Modifier.padding(bottom = 8.dp),
                )
              }
            }
          }

          // Episodes List
          items(episodesForSeason, key = { "ep_${it.video.id}_${it.seasonNumber}_${it.episodeNumber}" }) { episode ->
            EpisodeItemCard(
              episode = episode,
              onClick = { MediaUtils.playFile(episode.video, context, "series_episode_click") },
              onDelete = { pendingDeletion = episode.displayTitle to listOf(episode.video) },
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
          }
        }
      }
    }

    pendingDeletion?.let { (name, videos) ->
      DeleteConfirmationDialog(
        isOpen = true,
        onDismiss = { pendingDeletion = null },
        onConfirm = {
          coroutineScope.launch {
            val (deleted, _) = PermissionUtils.StorageOps.deleteVideos(context, videos)
            if (deleted == videos.size && videos.size == series?.totalEpisodes) {
              if (backstack.size > 1) {
                backstack.removeLastOrNull()
              } else {
                (context as? android.app.Activity)?.finish()
              }
            } else {
              isLoading = true
              reloadKey++
            }
          }
        },
        itemType = if (videos.size == 1) "episode" else "show",
        itemCount = videos.size,
        itemNames = if (videos.size == 1) listOf(name) else emptyList(),
      )
    }

    if (showMatchPicker) {
      AlertDialog(
        onDismissRequest = { showMatchPicker = false },
        title = {
          Text(
            text = "Choose the correct match",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
          )
        },
        text = {
          val keyboardController = LocalSoftwareKeyboardController.current
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            OutlinedTextField(
              value = matchQuery,
              onValueChange = { matchQuery = it },
              modifier = Modifier.fillMaxWidth(),
              placeholder = { Text("Type series name…") },
              singleLine = true,
              trailingIcon = {
                Row {
                  if (matchQuery.isNotEmpty()) {
                    IconButton(onClick = { matchQuery = "" }) {
                      Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                  }
                  IconButton(
                    onClick = {
                      searchMatches(matchQuery)
                      keyboardController?.hide()
                    },
                    enabled = matchQuery.isNotBlank() && !isSearchingMatches,
                  ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                  }
                }
              },
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              keyboardActions = KeyboardActions(onSearch = {
                searchMatches(matchQuery)
                keyboardController?.hide()
              }),
            )
            Text(
              text = if (matchQuery.isNotBlank()) "Search results for \"$matchQuery\" — pick the one that matches your show." else "Type a name above and search.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
              isSearchingMatches -> {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  CircularProgressIndicator()
                }
              }

              matchOptions.isEmpty() -> {
                Text(
                  text = "No matches found. Check your connection and try again.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(vertical = 12.dp),
                )
              }

              else -> {
                LazyColumn(
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  items(matchOptions, key = { "${it.id}_${it.mediaType}" }) { option ->
                    MatchOptionRow(
                      option = option,
                      onClick = {
                        showMatchPicker = false
                        val selectedTarget = series
                        if (selectedTarget != null) {
                          coroutineScope.launch {
                            val enriched = withContext(Dispatchers.IO) {
                              metadataRepository.enrichSeries(
                                selectedTarget,
                                forceRefresh = true,
                                preferred = option,
                              )
                            }
                            series = enriched
                          }
                        }
                        matchOptions = emptyList()
                      },
                    )
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showMatchPicker = false }) {
            Text("Cancel", fontWeight = FontWeight.Medium)
          }
        },
        dismissButton = {
          TextButton(
            onClick = {
              showMatchPicker = false
              coroutineScope.launch(Dispatchers.IO) {
                val targetId = series?.id
                if (targetId != null) {
                  metadataRepository.clearSeriesMetadata(targetId)
                  reloadKey++
                }
              }
            },
          ) {
            Text("Use auto match", fontWeight = FontWeight.Medium)
          }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.extraLarge,
      )
    }
  }
}

@Composable
private fun MatchOptionRow(
  option: WyzieTmdbResult,
  onClick: () -> Unit,
) {
  val isTv = option.mediaType.equals("tv", ignoreCase = true)
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick),
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(10.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .width(52.dp)
          .height(76.dp)
          .clip(RoundedCornerShape(6.dp)),
      ) {
        StreamingImage(
          url = option.poster,
          isSeries = isTv,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = option.title,
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = buildString {
            append(if (isTv) "TV Series" else "Movie")
            option.releaseYear?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun EpisodeItemCard(
  episode: LocalEpisode,
  onClick: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick),
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Episode Thumbnail
      Box(
        modifier = Modifier
          .width(110.dp)
          .aspectRatio(16f / 9f)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) {
        StreamingImage(
          url = episode.stillUrl,
          fallbackVideo = episode.video,
          isSeries = true,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )

        // Center Play Icon
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
          )
        }

        // Progress bar at bottom
        if (episode.progressPercentage > 0f && !episode.isWatched) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .height(3.dp)
              .background(Color.Black.copy(alpha = 0.5f)),
          ) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(episode.progressPercentage)
                .background(MaterialTheme.colorScheme.primary),
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Episode Details
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = episode.formattedEpisodeTag,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = episode.displayTitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Duration & Size chips
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (episode.video.durationFormatted.isNotBlank() && episode.video.durationFormatted != "0s") {
            Text(
              text = episode.video.durationFormatted,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (episode.video.sizeFormatted.isNotBlank()) {
            Text(
              text = "• ${episode.video.sizeFormatted}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Overview snippet if present
        episode.overview?.takeIf { it.isNotBlank() }?.let { epOverview ->
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = epOverview,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // Watched Checkmark indicator
      if (episode.isWatched) {
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          Icons.Filled.CheckCircle,
          contentDescription = "Watched",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
      }

      IconButton(onClick = onDelete) {
        Icon(Icons.Filled.Delete, contentDescription = "Delete episode")
      }
    }
  }
}
