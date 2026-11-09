package app.aryan447.mpvium.ui.audio.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val albumArtCache = LruCache<String, Bitmap>(64)

@Composable
fun AlbumArtImage(
  artworkUri: Uri?,
  modifier: Modifier = Modifier,
  contentDescription: String? = null,
  contentScale: ContentScale = ContentScale.Crop,
  targetSizePx: Int = 300,
) {
  val context = LocalContext.current
  val cacheKey = artworkUri?.toString() ?: ""

  var bitmap by remember(cacheKey) {
    mutableStateOf(if (cacheKey.isNotEmpty()) albumArtCache.get(cacheKey) else null)
  }

  LaunchedEffect(cacheKey) {
    if (cacheKey.isEmpty() || artworkUri == null) {
      bitmap = null
      return@LaunchedEffect
    }

    val cached = albumArtCache.get(cacheKey)
    if (cached != null) {
      bitmap = cached
      return@LaunchedEffect
    }

    val loaded = withContext(Dispatchers.IO) {
      loadAlbumArtBitmap(context, artworkUri, targetSizePx)
    }
    if (loaded != null) {
      albumArtCache.put(cacheKey, loaded)
      bitmap = loaded
    }
  }

  Crossfade(
    targetState = bitmap,
    animationSpec = tween(250),
    label = "AlbumArtCrossfade",
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
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Audiotrack,
          contentDescription = contentDescription,
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.size(36.dp),
        )
      }
    }
  }
}

private fun loadAlbumArtBitmap(context: Context, uri: Uri, targetSizePx: Int): Bitmap? {
  return try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      try {
        context.contentResolver.loadThumbnail(uri, Size(targetSizePx, targetSizePx), null)
      } catch (_: Exception) {
        loadWithBitmapFactory(context, uri, targetSizePx)
      }
    } else {
      loadWithBitmapFactory(context, uri, targetSizePx)
    }
  } catch (_: Exception) {
    null
  }
}

private fun loadWithBitmapFactory(context: Context, uri: Uri, targetSizePx: Int): Bitmap? {
  return try {
    context.contentResolver.openInputStream(uri)?.use { stream ->
      val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
      }
      BitmapFactory.decodeStream(stream, null, options)

      val originalWidth = options.outWidth
      val originalHeight = options.outHeight
      if (originalWidth <= 0 || originalHeight <= 0) return null

      var sampleSize = 1
      while (originalWidth / (sampleSize * 2) >= targetSizePx &&
        originalHeight / (sampleSize * 2) >= targetSizePx
      ) {
        sampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
      }

      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, decodeOptions)
      }
    }
  } catch (_: Exception) {
    null
  }
}
