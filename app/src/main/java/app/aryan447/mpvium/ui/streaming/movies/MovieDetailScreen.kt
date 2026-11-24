package app.aryan447.mpvium.ui.streaming.movies

import android.content.res.Configuration

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassButtonContainerColor
import app.aryan447.mpvium.ui.theme.glassButtonContentColor
import app.aryan447.mpvium.ui.theme.glassMenuContainerColor
import app.aryan447.mpvium.ui.theme.glassRimColor
import app.aryan447.mpvium.ui.theme.glassSheen
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import androidx.compose.material3.CircularProgressIndicator
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
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.streaming.LibraryIndexRepository
import app.aryan447.mpvium.domain.streaming.StreamingArtworkStore
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.repository.intro.IntroSkipRepository
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult
import app.aryan447.mpvium.ui.browser.dialogs.DeleteConfirmationDialog
import app.aryan447.mpvium.ui.browser.dialogs.MarkAsDialog
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
data class MovieDetailScreen(
  val videoId: Long,
  val movieTitle: String,
) : Screen {

  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val paneBack = LocalDetailPaneBack.current
    val libraryIndexRepository = koinInject<LibraryIndexRepository>()
    val metadataRepository = koinInject<StreamingMetadataRepository>()
    val artworkStore = koinInject<StreamingArtworkStore>()
    val introSkipRepository = koinInject<IntroSkipRepository>()
    val appearancePreferences = koinInject<AppearancePreferences>()
    val showDetailPoster by appearancePreferences.showDetailPoster.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var movie by remember { mutableStateOf<LocalMovie?>(null) }
    var detectedMovie by remember { mutableStateOf<LocalMovie?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var pendingDeletion by remember { mutableStateOf<Video?>(null) }
    var showMatchPicker by remember { mutableStateOf(false) }
    var showEditMetadataSheet by remember { mutableStateOf(false) }
    var matchOptions by remember { mutableStateOf<List<WyzieTmdbResult>>(emptyList()) }
    var isSearchingMatches by remember { mutableStateOf(false) }
    var matchQuery by remember { mutableStateOf("") }
    // Manual watch status (issue #94)
    var markAsOpen by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val currentTheme = LocalAppTheme.current
    val auraArtwork = if (showDetailPoster) (movie?.posterUrl ?: movie?.backdropUrl) else null
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

    LaunchedEffect(videoId, reloadKey) {
      withContext(Dispatchers.IO) {
        val detected = libraryIndexRepository.getLibrary(force = reloadKey > 0)
        val found = detected.movies.find { it.video.id == videoId } ?: detected.movies.find { it.title.equals(movieTitle, ignoreCase = true) }
        if (found != null) {
          detectedMovie = found
          movie = metadataRepository.applyCachedMovie(found)
          isLoading = false

          val enriched = metadataRepository.enrichMovie(found, forceRefresh = reloadKey > 0)
          movie = enriched
        } else {
          isLoading = false
        }
      }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-read playback progress through the existing detector using cached
    // metadata only, so the remaining time updates immediately after exiting
    // the player without extra network calls or loading flicker.
    fun refreshPlaybackProgress() {
      coroutineScope.launch(Dispatchers.IO) {
        val detected = libraryIndexRepository.getLibrary()
        val found = detected.movies.find { it.video.id == videoId }
          ?: detected.movies.find { it.title.equals(movieTitle, ignoreCase = true) }
          ?: return@launch
        val enriched = metadataRepository.enrichMovie(found, forceRefresh = false)
        withContext(Dispatchers.Main) {
          movie = enriched
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
            refreshPlaybackProgress()
          } else {
            resumedOnce = true
          }
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
      MediaLibraryEvents.changes.collect { refreshPlaybackProgress() }
    }

    // Search candidates for the match picker; re-runnable with a typed query
    fun searchMatches(query: String) {
      if (query.isBlank()) return
      coroutineScope.launch {
        isSearchingMatches = true
        matchOptions = withContext(Dispatchers.IO) {
          metadataRepository.searchMovieMatches(query)
        }
        isSearchingMatches = false
      }
    }

    LaunchedEffect(showMatchPicker) {
      val target = movie
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
      } else if (movie == null) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          Text("Movie not found", style = MaterialTheme.typography.bodyLarge)
        }
      } else {
        val currentMovie = movie!!
        val backdropUrl = currentMovie.backdropUrl ?: currentMovie.posterUrl

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
                DropdownMenuItem(
                  text = { Text(stringResource(R.string.metadata_edit_title)) },
                  trailingIcon = {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                  },
                  onClick = {
                    menuExpanded = false
                    showEditMetadataSheet = true
                  },
                )
                DropdownMenuItem(
                  text = { Text(stringResource(R.string.mark_as_action)) },
                  trailingIcon = {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                  },
                  onClick = {
                    menuExpanded = false
                    markAsOpen = true
                  },
                )
                DropdownMenuItem(
                  text = {
                    Text(
                      stringResource(R.string.delete),
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
                    pendingDeletion = currentMovie.video
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
              val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
              val headerHeight = when {
                isTablet && isLandscape -> 240.dp
                isTablet -> 280.dp
                else -> 320.dp
              }

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(headerHeight),
              ) {
                StreamingImage(
                  url = backdropUrl,
                  fallbackVideo = currentMovie.video,
                  isSeries = false,
                  contentScale = ContentScale.Crop,
                  maxDimensionPx = maxOf(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels),
                  modifier = Modifier.fillMaxSize(),
                )

                // Natural edge blend at the bottom edge only: top ~65-70% is 100% transparent so the poster stays vibrant
                val isGlass = LocalGlass.current
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      if (isGlass) {
                        Brush.verticalGradient(
                          0.0f to Color.Transparent,
                          0.70f to Color.Transparent,
                          1.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.40f),
                        )
                      } else {
                        Brush.verticalGradient(
                          0.0f to Color.Transparent,
                          0.65f to Color.Transparent,
                          0.88f to MaterialTheme.colorScheme.background.copy(alpha = 0.70f),
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

          // Movie Information Section
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
              if (!showDetailPoster) {
                // Title shown right below the back button instead of the poster
                Text(
                  text = currentMovie.title,
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
                if (currentMovie.rating != null && currentMovie.rating > 0f) {
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
                        text = String.format(java.util.Locale.US, "%.1f", currentMovie.rating),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF5A623),
                      )
                    }
                  }
                }

                if (!currentMovie.year.isNullOrBlank()) {
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp),
                  ) {
                    Text(
                      text = currentMovie.year,
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }

                if (currentMovie.video.durationFormatted.isNotBlank() && currentMovie.video.durationFormatted != "0s") {
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp),
                  ) {
                    Text(
                      text = currentMovie.video.durationFormatted,
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }

                currentMovie.video.resolution.takeIf { it != "--" }?.let { resolution ->
                  val displayRes = resolution.substringBefore("@")
                  Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                  ) {
                    Text(
                      text = displayRes,
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }

              if (showDetailPoster) {
                // Title
                Text(
                  text = currentMovie.title,
                  style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                  ),
                  color = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier.padding(top = 12.dp),
                )
              }

              // Genres Row
              if (currentMovie.genres.isNotEmpty()) {
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.padding(top = 8.dp),
                ) {
                  items(currentMovie.genres) { genre ->
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
              currentMovie.overview?.takeIf { it.isNotBlank() }?.let { overview ->
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
              Button(
                onClick = { MediaUtils.playFile(currentMovie.video, context, "movie_detail_play") },
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
                  text = "Play Movie",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
              }
            }
          }
        }
      }
    }

    pendingDeletion?.let { video ->
      DeleteConfirmationDialog(
        isOpen = true,
        onDismiss = { pendingDeletion = null },
        onConfirm = {
            coroutineScope.launch {
              val deletedTitle = movie?.title
              PermissionUtils.StorageOps.deleteVideos(context, listOf(video))
              // Movie file is gone: drop its cached TMDB entry immediately.
              if (deletedTitle != null) {
                runCatching { metadataRepository.clearMovieMetadata(deletedTitle) }
              }
              // Movie file is gone: drop its cached intro/recap windows too.
              runCatching { introSkipRepository.evictAll(listOfNotNull(video.displayName, deletedTitle)) }
              if (paneBack != null) {
                paneBack()
              } else if (backstack.size > 1) {
                backstack.removeLastOrNull()
              } else {
                (context as? android.app.Activity)?.finish()
              }
            }
        },
        itemType = "movie",
        itemCount = 1,
        itemNames = listOf(movie?.title ?: video.title),
      )
    }

    MarkAsDialog(
      isOpen = markAsOpen,
      selectedCount = 1,
      onDismiss = { markAsOpen = false },
      onSelect = { status ->
        markAsOpen = false
        val target = movie?.video
        if (target != null) {
          coroutineScope.launch(Dispatchers.IO) {
            val applied = VideoWatchStatusOps.markVideo(target, status)
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
              placeholder = { Text("Type movie name…") },
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
              text = if (matchQuery.isNotBlank()) "Search results for \"$matchQuery\" — pick the one that matches your movie." else "Type a name above and search.",
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
                        // Key the manual pick from the file's detected title/year.
                        // `movie` may already carry the TMDb display year, which
                        // would store the entry under a key the rescan prunes.
                        val selectedTarget = detectedMovie ?: movie
                        if (selectedTarget != null) {
                          coroutineScope.launch {
                            val enriched = withContext(Dispatchers.IO) {
                              metadataRepository.enrichMovie(
                                selectedTarget,
                                forceRefresh = true,
                                preferred = option,
                              )
                            }
                            movie = enriched
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
                val targetTitle = movie?.title
                if (targetTitle != null) {
                  metadataRepository.clearMovieMetadata(targetTitle)
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

    val currentMovieForSheet = movie
    if (currentMovieForSheet != null) {
      EditMetadataSheet(
        isOpen = showEditMetadataSheet,
        onDismiss = { showEditMetadataSheet = false },
        currentTitle = currentMovieForSheet.title,
        currentYear = currentMovieForSheet.year,
        currentPosterUrl = currentMovieForSheet.posterUrl,
        fallbackVideo = currentMovieForSheet.video,
        isSeries = false,
        onSave = { newTitle, newYear, posterUri, removePoster ->
          coroutineScope.launch(Dispatchers.IO) {
            val target = detectedMovie ?: currentMovieForSheet
            val cached = metadataRepository.getCachedMovieMetadata(target)
            val finalPosterPath = when {
              posterUri != null -> artworkStore.storeCustomArtwork(posterUri, keyHint = target.title)
              removePoster -> null
              else -> cached?.customPosterPath
            }
            val updated = metadataRepository.updateMovieOverrides(
              movie = target,
              customTitle = newTitle,
              customYear = newYear,
              customPosterPath = finalPosterPath,
            )
            withContext(Dispatchers.Main) {
              movie = updated
              showEditMetadataSheet = false
              MediaLibraryEvents.notifyChanged()
            }
          }
        },
        onResetToAutomatic = {
          coroutineScope.launch(Dispatchers.IO) {
            val target = detectedMovie ?: currentMovieForSheet
            val updated = metadataRepository.resetMovieOverrides(target)
            withContext(Dispatchers.Main) {
              movie = updated
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
  val isMovie = option.mediaType.equals("movie", ignoreCase = true)
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
          isSeries = !isMovie,
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
            append(if (isMovie) "Movie" else "Show")
            option.releaseYear?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
