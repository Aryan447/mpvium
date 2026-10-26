package app.aryan447.mpvium.ui.streaming.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.streaming.StreamingArtworkStore
import app.aryan447.mpvium.domain.thumbnail.ThumbnailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * Modern Compose Image component for streaming posters, backdrops, and stills.
 *
 * Artwork is served from the durable [StreamingArtworkStore]: once a cover has
 * loaded it stays available offline until its movie/show is deleted.
 */
@Composable
fun StreamingImage(
  url: String?,
  modifier: Modifier = Modifier,
  fallbackVideo: Video? = null,
  isSeries: Boolean = true,
  contentScale: ContentScale = ContentScale.Crop,
  contentDescription: String? = null,
  maxDimensionPx: Int = StreamingArtworkStore.DEFAULT_MAX_DIMENSION,
) {
  val artworkStore = koinInject<StreamingArtworkStore>()
  val thumbnailRepository = koinInject<ThumbnailRepository>()
  val cappedDimension = maxDimensionPx.coerceAtLeast(1)

  var bitmap by remember(url, cappedDimension) {
    mutableStateOf(url?.let { artworkStore.getFromMemory(it, cappedDimension) })
  }
  var isLoading by remember(url, cappedDimension) { mutableStateOf(bitmap == null) }

  LaunchedEffect(url, cappedDimension, fallbackVideo) {
    if (bitmap != null) {
      isLoading = false
      return@LaunchedEffect
    }

    if (!url.isNullOrBlank()) {
      val loaded = artworkStore.loadImage(url, cappedDimension)
      if (loaded != null) {
        bitmap = loaded
        isLoading = false
        return@LaunchedEffect
      }
    }

    // Fallback to local video thumbnail if url is missing or failed
    if (fallbackVideo != null) {
      val thumb = withContext(Dispatchers.IO) {
        thumbnailRepository.getThumbnail(fallbackVideo, 500, 750)
      }
      if (thumb != null) {
        bitmap = thumb
      }
    }
    isLoading = false
  }

  Crossfade(
    targetState = bitmap,
    animationSpec = tween(300),
    label = "StreamingImageCrossfade",
    modifier = modifier,
  ) { currentBitmap ->
    if (currentBitmap != null) {
      Image(
        bitmap = currentBitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize(),
        contentScale = contentScale,
      )
    } else {
      // Sleek streaming placeholder with subtle gradient
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.surfaceContainer,
              )
            )
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (isSeries) Icons.Filled.Tv else Icons.Filled.Movie,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        )
      }
    }
  }
}
