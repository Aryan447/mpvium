package app.aryan447.mpvium.ui.streaming.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.streaming.SeriesDetector
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult
import app.aryan447.mpvium.ui.browser.dialogs.DeleteConfirmationDialog
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
data class MovieDetailScreen(
  val videoId: Long,
  val movieTitle: String,
) : Screen {

  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val seriesDetector = koinInject<SeriesDetector>()
    val metadataRepository = koinInject<StreamingMetadataRepository>()
    val coroutineScope = rememberCoroutineScope()

    var movie by remember { mutableStateOf<LocalMovie?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var pendingDeletion by remember { mutableStateOf<Video?>(null) }
    var showMatchPicker by remember { mutableStateOf(false) }
    var matchOptions by remember { mutableStateOf<List<WyzieTmdbResult>>(emptyList()) }
    var isSearchingMatches by remember { mutableStateOf(false) }

    LaunchedEffect(videoId, reloadKey) {
      withContext(Dispatchers.IO) {
        val detected = seriesDetector.detectLibrary()
        val found = detected.movies.find { it.video.id == videoId } ?: detected.movies.find { it.title.equals(movieTitle, ignoreCase = true) }
        if (found != null) {
          movie = found
          isLoading = false

          val enriched = metadataRepository.enrichMovie(found, forceRefresh = reloadKey > 0)
          movie = enriched
        } else {
          isLoading = false
        }
      }
    }

    LaunchedEffect(showMatchPicker) {
      val target = movie
      if (showMatchPicker && target != null) {
        isSearchingMatches = true
        matchOptions = withContext(Dispatchers.IO) {
          metadataRepository.searchMovieMatches(target.title)
        }
        isSearchingMatches = false
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
                fallbackVideo = currentMovie.video,
                isSeries = false,
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
                  onClick = { showMatchPicker = true },
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                  Icon(Icons.Filled.Refresh, contentDescription = "Fix match from search", tint = Color.White)
                }
                IconButton(
                  onClick = { pendingDeletion = currentMovie.video },
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                  Icon(Icons.Filled.Delete, contentDescription = "Delete movie", tint = Color.White)
                }
              }
            }
          }

          // Movie Information Section
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
                if (currentMovie.rating != null && currentMovie.rating > 0f) {
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
                        text = String.format(java.util.Locale.US, "%.1f", currentMovie.rating),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                      )
                    }
                  }
                }

                if (!currentMovie.year.isNullOrBlank()) {
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(6.dp),
                  ) {
                    Text(
                      text = currentMovie.year,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                  }
                }

                if (currentMovie.video.durationFormatted.isNotBlank() && currentMovie.video.durationFormatted != "0s") {
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(6.dp),
                  ) {
                    Text(
                      text = currentMovie.video.durationFormatted,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                  }
                }

                currentMovie.video.resolution.takeIf { it != "--" }?.let { resolution ->
                  val displayRes = resolution.substringBefore("@")
                  Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
                  ) {
                    Text(
                      text = displayRes,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Title
              Text(
                text = currentMovie.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
              )

              // Synopsis / Overview
              currentMovie.overview?.let { overview ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = overview,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Spacer(modifier = Modifier.height(20.dp))

              // Big Play Button
              Button(
                onClick = { MediaUtils.playFile(currentMovie.video, context, "movie_detail_play") },
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
                  text = "Play Movie",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
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
            PermissionUtils.StorageOps.deleteVideos(context, listOf(video))
            if (backstack.size > 1) {
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

    if (showMatchPicker) {
      val target = movie
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
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = if (target != null) "Search results for \"${target.title}\" — pick the one that matches your movie." else "Searching...",
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
                        val selectedTarget = movie
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
            append(if (isMovie) "Movie" else "TV Series")
            option.releaseYear?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
