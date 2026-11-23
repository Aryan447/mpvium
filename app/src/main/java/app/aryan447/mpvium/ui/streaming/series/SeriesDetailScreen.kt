package app.aryan447.mpvium.ui.streaming.series

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassButtonContainerColor
import app.aryan447.mpvium.ui.theme.glassButtonContentColor
import app.aryan447.mpvium.ui.theme.glassFilterChipColors
import app.aryan447.mpvium.ui.theme.glassMenuContainerColor
import app.aryan447.mpvium.ui.theme.glassRimColor
import app.aryan447.mpvium.ui.theme.glassSheen
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import app.aryan447.mpvium.preferences.preference.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.aryan447.mpvium.R
import app.aryan447.mpvium.domain.streaming.LibraryIndexRepository
import app.aryan447.mpvium.domain.streaming.StreamingArtworkStore
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.repository.intro.IntroSkipRepository
import app.aryan447.mpvium.domain.streaming.model.LocalEpisode
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult
import app.aryan447.mpvium.ui.browser.dialogs.DeleteConfirmationDialog
import app.aryan447.mpvium.ui.browser.dialogs.MarkAsDialog
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.streaming.components.EditMetadataSheet
import app.aryan447.mpvium.ui.streaming.components.StreamingImage
import app.aryan447.mpvium.ui.theme.AppTheme
import app.aryan447.mpvium.ui.theme.AuraThemeManager
import app.aryan447.mpvium.ui.theme.LocalAppTheme
import app.aryan447.mpvium.ui.utils.LocalBackStack
import app.aryan447.mpvium.ui.utils.LocalDetailPaneBack
import app.aryan447.mpvium.utils.media.MediaLibraryEvents
import app.aryan447.mpvium.utils.media.MediaUtils
import app.aryan447.mpvium.utils.media.VideoWatchStatusOps
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
    val paneBack = LocalDetailPaneBack.current
    val libraryIndexRepository = koinInject<LibraryIndexRepository>()
    val metadataRepository = koinInject<StreamingMetadataRepository>()
    val customSeriesRepository = koinInject<app.aryan447.mpvium.database.repository.CustomSeriesRepository>()
    val artworkStore = koinInject<StreamingArtworkStore>()
    val introSkipRepository = koinInject<IntroSkipRepository>()
    val appearancePreferences = koinInject<AppearancePreferences>()
    val showDetailPoster by appearancePreferences.showDetailPoster.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val isCustomSeries = remember(seriesId) { seriesId.startsWith("custom:") }
    val customSeriesId = remember(seriesId, isCustomSeries) {
      if (isCustomSeries) seriesId.removePrefix("custom:").toIntOrNull() else null
    }

    var series by remember { mutableStateOf<LocalSeries?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedSeason by remember { mutableIntStateOf(1) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var pendingDeletion by remember { mutableStateOf<Pair<String, List<Video>>?>(null) }
    var showDeleteCustomSeriesDialog by remember { mutableStateOf(false) }
    var showMatchPicker by remember { mutableStateOf(false) }
    var showEditMetadataSheet by remember { mutableStateOf(false) }
    var matchOptions by remember { mutableStateOf<List<WyzieTmdbResult>>(emptyList()) }
    var isSearchingMatches by remember { mutableStateOf(false) }
    var matchQuery by remember { mutableStateOf("") }
    // Manual watch status (issue #94): long-press an episode -> Mark as…
    var markAsEpisode by remember { mutableStateOf<LocalEpisode?>(null) }
    // Show-level Mark as: applies to all episodes (e.g. mark whole show finished)
    var markShowAsOpen by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val currentTheme = LocalAppTheme.current
    val auraArtwork = if (showDetailPoster) (series?.posterUrl ?: series?.backdropUrl) else null
    DisposableEffect(auraArtwork, currentTheme) {
      if (currentTheme == AppTheme.Aura && !auraArtwork.isNullOrBlank()) {
        AuraThemeManager.setPreviewArtwork(auraArtwork)
      }
      onDispose {
        if (currentTheme == AppTheme.Aura) {
          AuraThemeManager.clearPreviewArtwork(auraArtwork)
        }
      }
    }

    LaunchedEffect(seriesId, reloadKey) {
      withContext(Dispatchers.IO) {
        val detected = libraryIndexRepository.getLibrary(force = reloadKey > 0)
        val found = detected.series.find { it.id == seriesId }
        if (found != null) {
          series = metadataRepository.applyCachedSeries(found)
          selectedSeason = found.seasons.keys.firstOrNull() ?: 1
          isLoading = false

          // Enrich in background; manual reload (reloadKey > 0) forces a re-fetch
          val enriched = if (isCustomSeries) {
            metadataRepository.applyCachedSeries(found)
          } else {
            metadataRepository.enrichSeries(found, forceRefresh = reloadKey > 0)
          }
          series = enriched
        } else {
          isLoading = false
        }
      }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-read episode progress through the existing detector using cached
    // metadata only, so the remaining time updates immediately after exiting
    // the player without extra network calls, loading flicker, or resetting
    // the selected season.
    fun refreshPlaybackProgress(preserveSeason: Int) {
      coroutineScope.launch(Dispatchers.IO) {
        val detected = libraryIndexRepository.getLibrary()
        val found = detected.series.find { it.id == seriesId } ?: return@launch
        val enriched = if (isCustomSeries) {
          metadataRepository.applyCachedSeries(found)
        } else {
          metadataRepository.enrichSeries(found, forceRefresh = false)
        }
        withContext(Dispatchers.Main) {
          series = enriched
          selectedSeason =
            if (enriched.seasons.containsKey(preserveSeason)) {
              preserveSeason
            } else {
              enriched.seasons.keys.firstOrNull() ?: preserveSeason
            }
        }
      }
    }

    // Returning from PlayerActivity resumes this screen. The player persists
    // its final position in onStop/onDestroy, which can land after ON_RESUME,
    // so also refresh on the post-save notification to correct a stale read.
    DisposableEffect(lifecycleOwner) {
      var resumedOnce = false
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          if (resumedOnce) {
            refreshPlaybackProgress(selectedSeason)
          } else {
            resumedOnce = true
          }
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
      MediaLibraryEvents.changes.collect { refreshPlaybackProgress(selectedSeason) }
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
          Text("TV show not found", style = MaterialTheme.typography.bodyLarge)
        }
      } else {
        val currentSeries = series!!
        val episodesForSeason = currentSeries.seasons[selectedSeason] ?: emptyList()
        val backdropUrl = currentSeries.backdropUrl ?: currentSeries.posterUrl
        val fallbackVideo = currentSeries.seasons.values.firstOrNull()?.firstOrNull()?.video

        val detailTopBar: @Composable (Modifier) -> Unit = { modifier ->
          Row(
            modifier = modifier
              .statusBarsPadding()
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            FilledTonalIconButton(
              onClick = {
                if (paneBack != null) {
                  paneBack()
                } else if (backstack.size > 1) {
                  backstack.removeLastOrNull()
                } else {
                  (context as? android.app.Activity)?.finish()
                }
              },
              colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f),
                contentColor = MaterialTheme.colorScheme.onSurface,
              ),
              modifier = Modifier.size(42.dp),
            ) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
              )
            }

            Box {
              FilledTonalIconButton(
                onClick = { menuExpanded = true },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f),
                  contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.size(42.dp),
              ) {
                Icon(
                  Icons.Filled.MoreVert,
                  contentDescription = stringResource(R.string.more_options),
                )
              }

              DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = glassMenuContainerColor(MenuDefaults.containerColor),
              ) {
                DropdownMenuItem(
                  text = { Text(stringResource(if (showDetailPoster) R.string.hide_poster else R.string.show_poster)) },
                  trailingIcon = {
                    Icon(
                      if (showDetailPoster) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                      contentDescription = null,
                    )
                  },
                  onClick = {
                    menuExpanded = false
                    appearancePreferences.showDetailPoster.set(!showDetailPoster)
                  },
                )
                if (!isCustomSeries) {
                  DropdownMenuItem(
                    text = { Text(stringResource(R.string.fix_match)) },
                    trailingIcon = {
                      Icon(Icons.Filled.Refresh, contentDescription = null)
                    },
                    onClick = {
                      menuExpanded = false
                      showMatchPicker = true
                    },
                  )
                }
                DropdownMenuItem(
                  text = {
                    Text(
                      stringResource(if (isCustomSeries) R.string.edit_series else R.string.metadata_edit_title)
                    )
                  },
                  trailingIcon = {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                  },
                  onClick = {
                    menuExpanded = false
                    if (isCustomSeries && customSeriesId != null) {
                      backstack.add(CustomSeriesBuilderScreen(seriesId = customSeriesId))
                    } else {
                      showEditMetadataSheet = true
                    }
                  },
                )
                DropdownMenuItem(
                  text = { Text(stringResource(R.string.mark_as_action)) },
                  trailingIcon = {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                  },
                  onClick = {
                    menuExpanded = false
                    markShowAsOpen = true
                  },
                )
                DropdownMenuItem(
                  text = {
                    Text(
                      stringResource(if (isCustomSeries) R.string.delete_series_action else R.string.delete),
                      color = MaterialTheme.colorScheme.error,
                    )
                  },
                  trailingIcon = {
                    Icon(
                      Icons.Filled.Delete,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.error,
                    )
                  },
                  onClick = {
                    menuExpanded = false
                    if (isCustomSeries) {
                      showDeleteCustomSeriesDialog = true
                    } else {
                      pendingDeletion = currentSeries.title to currentSeries.seasons.values.flatten().map { it.video }
                    }
                  },
                )
              }
            }
          }
        }

        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        ) {
          if (showDetailPoster) {
            // Backdrop Header
            item {
              val configuration = LocalConfiguration.current
              val isTablet = configuration.smallestScreenWidthDp >= 600
              val headerHeight = if (isTablet) 420.dp else 340.dp

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(headerHeight),
              ) {
                StreamingImage(
                  url = backdropUrl,
                  fallbackVideo = fallbackVideo,
                  isSeries = true,
                  contentScale = ContentScale.Crop,
                  maxDimensionPx = maxOf(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels),
                  modifier = Modifier.fillMaxSize(),
                )

                // Natural edge blend at the very bottom edge only: 0-75% is 100% transparent so the poster stays vibrant
                val isGlass = LocalGlass.current
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      if (isGlass) {
                        Brush.verticalGradient(
                          0.0f to Color.Transparent,
                          0.80f to Color.Transparent,
                          1.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.40f),
                        )
                      } else {
                        Brush.verticalGradient(
                          0.0f to Color.Transparent,
                          0.75f to Color.Transparent,
                          0.92f to MaterialTheme.colorScheme.background.copy(alpha = 0.70f),
                          1.0f to MaterialTheme.colorScheme.background,
                        )
                      }
                    )
                )

                detailTopBar(Modifier.align(Alignment.TopCenter))
              }
            }
          } else {
            item {
              detailTopBar(Modifier)
            }
          }

          // Series Information Header
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
              if (!showDetailPoster) {
                // Title shown right below the back button instead of the poster
                Text(
                  text = currentSeries.title,
                  style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                  ),
                  color = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier.padding(bottom = 12.dp),
                )
              }

              // Badges Row
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                if (currentSeries.rating != null && currentSeries.rating > 0f) {
                  Surface(
                    color = Color(0xFFF5A623).copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFF5A623).copy(alpha = 0.40f)),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFF5A623),
                        modifier = Modifier.size(13.dp),
                      )
                      Text(
                        text = String.format(java.util.Locale.US, "%.1f", currentSeries.rating),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF5A623),
                      )
                    }
                  }
                }

                Surface(
                  color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  shape = RoundedCornerShape(8.dp),
                ) {
                  Text(
                    text = "${currentSeries.seasonCount} Seasons • ${currentSeries.totalEpisodes} Episodes",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }

                if (!currentSeries.year.isNullOrBlank()) {
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp),
                  ) {
                    Text(
                      text = currentSeries.year,
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }

              if (showDetailPoster) {
                // Title
                Text(
                  text = currentSeries.title,
                  style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                  ),
                  color = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier.padding(top = 12.dp),
                )
              }

              // Genres Row
              if (currentSeries.genres.isNotEmpty()) {
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.padding(top = 8.dp),
                ) {
                  items(currentSeries.genres) { genre ->
                    SuggestionChip(
                      onClick = {},
                      label = {
                        Text(
                          text = genre,
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        )
                      },
                      shape = RoundedCornerShape(8.dp),
                      colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      ),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    )
                  }
                }
              }

              // Synopsis / Overview with Expandable Text
              currentSeries.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                var isExpanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(top = 10.dp)) {
                  Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.animateContentSize(),
                  )
                  if (overview.length > 140) {
                    Text(
                      text = if (isExpanded) "Show less" else "Read more",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(top = 4.dp),
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(20.dp))

              // Big Play Button
              val playNext = currentSeries.nextEpisodeToWatch ?: currentSeries.seasons.values.firstOrNull()?.firstOrNull()
              if (playNext != null) {
                Button(
                  onClick = { MediaUtils.playFile(playNext.video, context, "series_detail_play") },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .glassSheen(RoundedCornerShape(16.dp), LocalGlass.current),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = glassButtonContainerColor(MaterialTheme.colorScheme.primary),
                    contentColor = glassButtonContentColor(MaterialTheme.colorScheme.onPrimary),
                  ),
                  shape = RoundedCornerShape(16.dp),
                  elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
                  border = if (LocalGlass.current) {
                    BorderStroke(1.dp, glassRimColor(isSystemInDarkTheme()))
                  } else {
                    null
                  },
                ) {
                  Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Play ${playNext.formattedEpisodeTag}: ${playNext.displayTitle}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              Spacer(modifier = Modifier.height(24.dp))

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
                      colors = glassFilterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                      ),
                      shape = RoundedCornerShape(12.dp),
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
              onLongClick = { markAsEpisode = episode },
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
          }
        }
      }
    }

    MarkAsDialog(
      isOpen = markAsEpisode != null,
      selectedCount = 1,
      onDismiss = { markAsEpisode = null },
      onSelect = { status ->
        val target = markAsEpisode
        markAsEpisode = null
        if (target != null) {
          coroutineScope.launch(Dispatchers.IO) {
            val applied = VideoWatchStatusOps.markVideo(target.video, status)
            withContext(Dispatchers.Main) {
              android.widget.Toast.makeText(
                context,
                context.getString(
                  if (applied) app.aryan447.mpvium.R.string.mark_as_updated
                  else app.aryan447.mpvium.R.string.mark_as_failed,
                ),
                android.widget.Toast.LENGTH_SHORT,
              ).show()
            }
          }
        }
      },
    )

    MarkAsDialog(
      isOpen = markShowAsOpen,
      selectedCount = series?.totalEpisodes ?: 0,
      onDismiss = { markShowAsOpen = false },
      onSelect = { status ->
        markShowAsOpen = false
        val videos = series?.seasons?.values?.flatten()?.map { it.video } ?: emptyList()
        if (videos.isNotEmpty()) {
          coroutineScope.launch(Dispatchers.IO) {
            val applied = VideoWatchStatusOps.markVideos(videos, status)
            withContext(Dispatchers.Main) {
              android.widget.Toast.makeText(
                context,
                context.getString(
                  if (applied > 0) app.aryan447.mpvium.R.string.mark_as_updated
                  else app.aryan447.mpvium.R.string.mark_as_failed,
                ),
                android.widget.Toast.LENGTH_SHORT,
              ).show()
            }
          }
        }
      },
    )

    pendingDeletion?.let { (name, videos) ->
      DeleteConfirmationDialog(
        isOpen = true,
        onDismiss = { pendingDeletion = null },
        onConfirm = {
            coroutineScope.launch {
              val targetId = series?.id
              val totalEpisodes = series?.totalEpisodes ?: videos.size
              val (deleted, _) = PermissionUtils.StorageOps.deleteVideos(context, videos)
              // Drop cached intro/recap windows for the deleted episodes so the
              // disk cache shrinks per-episode. Survivors keep working offline.
              runCatching { introSkipRepository.evictAll(videos.map { it.displayName }) }
              if (deleted == videos.size && videos.size == totalEpisodes) {
                // Every episode of the show is gone: drop its cached TMDB entry.
                // Partial episode deletes keep the cache.
                if (targetId != null) {
                  runCatching { metadataRepository.clearSeriesMetadata(targetId) }
                }
                if (paneBack != null) {
                  paneBack()
                } else if (backstack.size > 1) {
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

    if (showDeleteCustomSeriesDialog && customSeriesId != null) {
      val seriesTitle = series?.title.orEmpty()
      AlertDialog(
        onDismissRequest = { showDeleteCustomSeriesDialog = false },
        containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
        title = {
          Text(
            text = stringResource(R.string.delete_custom_series_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
          )
        },
        text = {
          Text(
            text = stringResource(R.string.delete_custom_series_message, seriesTitle),
            style = MaterialTheme.typography.bodyMedium,
          )
        },
        confirmButton = {
          Button(
            onClick = {
              showDeleteCustomSeriesDialog = false
              coroutineScope.launch {
                withContext(Dispatchers.IO) {
                  customSeriesRepository.deleteSeries(customSeriesId)
                  MediaLibraryEvents.notifyChanged()
                }
                withContext(Dispatchers.Main) {
                  if (paneBack != null) {
                    paneBack()
                  } else if (backstack.size > 1) {
                    backstack.removeLastOrNull()
                  } else {
                    (context as? android.app.Activity)?.finish()
                  }
                }
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          ) {
            Text(stringResource(R.string.delete_series_action))
          }
        },
        dismissButton = {
          TextButton(onClick = { showDeleteCustomSeriesDialog = false }) {
            Text(stringResource(R.string.generic_cancel))
          }
        },
        shape = MaterialTheme.shapes.extraLarge,
      )
    }

    if (showMatchPicker) {
      AlertDialog(
        onDismissRequest = { showMatchPicker = false },
        containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
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
              placeholder = { Text("Type TV show name…") },
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
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.extraLarge,
      )
    }

    val currentSeriesForSheet = series
    if (currentSeriesForSheet != null) {
      EditMetadataSheet(
        isOpen = showEditMetadataSheet,
        onDismiss = { showEditMetadataSheet = false },
        currentTitle = currentSeriesForSheet.title,
        currentYear = currentSeriesForSheet.year,
        currentPosterUrl = currentSeriesForSheet.posterUrl,
        fallbackVideo = currentSeriesForSheet.seasons.values.firstOrNull()?.firstOrNull()?.video,
        isSeries = true,
        onSave = { newTitle, newYear, posterUri, removePoster ->
          coroutineScope.launch(Dispatchers.IO) {
            val target = series ?: return@launch
            val cached = metadataRepository.getCachedSeriesMetadata(target.id)
            val finalPosterPath = when {
              posterUri != null -> artworkStore.storeCustomArtwork(posterUri, keyHint = target.id)
              removePoster -> null
              else -> cached?.customPosterPath
            }
            val updated = metadataRepository.updateSeriesOverrides(
              series = target,
              customTitle = newTitle,
              customYear = newYear,
              customPosterPath = finalPosterPath,
            )
            withContext(Dispatchers.Main) {
              series = updated
              showEditMetadataSheet = false
              MediaLibraryEvents.notifyChanged()
            }
          }
        },
        onResetToAutomatic = {
          coroutineScope.launch(Dispatchers.IO) {
            val target = series ?: return@launch
            val updated = metadataRepository.resetSeriesOverrides(target)
            withContext(Dispatchers.Main) {
              series = updated
              showEditMetadataSheet = false
              MediaLibraryEvents.notifyChanged()
            }
          }
        },
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
            append(if (isTv) "Show" else "Movie")
            option.releaseYear?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeItemCard(
  episode: LocalEpisode,
  onClick: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    border = if (LocalGlass.current) BorderStroke(1.dp, glassRimColor(isSystemInDarkTheme())) else null,
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
          .width(120.dp)
          .aspectRatio(16f / 9f)
          .clip(RoundedCornerShape(10.dp))
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
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.50f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
          )
        }

        // Watched badge on thumbnail
        if (episode.isWatched) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(4.dp)
              .size(18.dp),
          ) {
            Icon(
              Icons.Filled.CheckCircle,
              contentDescription = "Watched",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(2.dp),
            )
          }
        }

        // Progress bar at bottom of thumbnail
        if (episode.progressPercentage > 0f && !episode.isWatched) {
          LinearProgressIndicator(
            progress = { episode.progressPercentage },
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Black.copy(alpha = 0.45f),
          )
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

        // Duration & Size
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

      IconButton(onClick = onDelete) {
        Icon(
          Icons.Filled.Delete,
          contentDescription = "Delete episode",
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
      }
    }
  }
}
