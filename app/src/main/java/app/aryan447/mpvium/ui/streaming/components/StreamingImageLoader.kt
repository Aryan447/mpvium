package app.aryan447.mpvium.ui.streaming.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.thumbnail.ThumbnailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.compose.koinInject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Image Cache and Downloader for Streaming artwork.
 */
object StreamingImageCache {
  private const val TAG = "StreamingImageCache"
  private val memoryCache: LruCache<String, Bitmap>
  private var diskCacheDir: File? = null

  init {
    val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
    val cacheSizeKb = maxMemoryKb / 8
    memoryCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
      override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
  }

  private fun getDiskDir(context: Context): File {
    if (diskCacheDir == null) {
      diskCacheDir = File(context.filesDir, "streaming_art").apply { mkdirs() }
    }
    return diskCacheDir!!
  }

  private fun hashKey(url: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(url.toByteArray())
    return digest.joinToString("") { "%02x".format(it) } + ".jpg"
  }

  fun getFromMemory(url: String): Bitmap? = memoryCache.get(url)

  suspend fun loadImage(context: Context, client: OkHttpClient, url: String): Bitmap? = withContext(Dispatchers.IO) {
    if (url.isBlank()) return@withContext null

    memoryCache.get(url)?.let { return@withContext it }

    val diskFile = File(getDiskDir(context), hashKey(url))
    if (diskFile.exists() && diskFile.length() > 0) {
      try {
        val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
        if (bitmap != null) {
          memoryCache.put(url, bitmap)
          return@withContext bitmap
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to decode cached disk image for $url", e)
      }
    }

    try {
      val request = Request.Builder().url(url).build()
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val bytes = response.body?.bytes()
          if (bytes != null && bytes.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
              memoryCache.put(url, bitmap)
              runCatching {
                FileOutputStream(diskFile).use { out ->
                  out.write(bytes)
                  out.flush()
                }
              }
              return@withContext bitmap
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error fetching image from $url: ${e.message}")
    }

    null
  }
}

/**
 * Modern Compose Image component for streaming posters, backdrops, and stills.
 */
@Composable
fun StreamingImage(
  url: String?,
  modifier: Modifier = Modifier,
  fallbackVideo: Video? = null,
  isSeries: Boolean = true,
  contentScale: ContentScale = ContentScale.Crop,
  contentDescription: String? = null,
) {
  val context = LocalContext.current
  val okHttpClient = koinInject<OkHttpClient>()
  val thumbnailRepository = koinInject<ThumbnailRepository>()
  val density = LocalDensity.current

  var bitmap by remember(url) {
    mutableStateOf(url?.let { StreamingImageCache.getFromMemory(it) })
  }
  var isLoading by remember(url) { mutableStateOf(bitmap == null) }

  LaunchedEffect(url, fallbackVideo) {
    if (bitmap != null) {
      isLoading = false
      return@LaunchedEffect
    }

    if (!url.isNullOrBlank()) {
      val loaded = StreamingImageCache.loadImage(context, okHttpClient, url)
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
