package app.aryan447.mpvium.domain.streaming

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Durable on-disk storage for streaming artwork (posters, backdrops, stills).
 *
 * Unlike the old bounded Chrome-style cache, artwork written here is never
 * evicted by a size cap or an LRU clock: a cover stays available offline until
 * the movie/show it belongs to is deleted. [StreamingMetadataRepository] calls
 * [delete] when it prunes or clears a metadata entry.
 *
 * The memory cache remains bounded; only the disk copy is permanent.
 */
class StreamingArtworkStore(
  context: Context,
  private val client: OkHttpClient,
) {
  companion object {
    private const val TAG = "StreamingArtworkStore"
    private const val DISK_DIR_NAME = "streaming_art"
    private const val DISK_FILE_EXTENSION = "img"
    private const val LEGACY_DISK_FILE_EXTENSION = "jpg"

    /** Default decode cap (long edge, px) for cards, rows and episode stills. */
    const val DEFAULT_MAX_DIMENSION = 1080
  }

  private val appContext = context.applicationContext ?: context
  private val diskDir: File = File(appContext.filesDir, DISK_DIR_NAME).apply { mkdirs() }
  private val memoryCache: LruCache<String, Bitmap>
  private val downloadLocks = ConcurrentHashMap<String, Mutex>()

  init {
    val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
    val cacheSizeKb = maxMemoryKb / 8
    memoryCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
      override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
  }

  private fun memoryKey(url: String, maxDimension: Int) = "$url|$maxDimension"

  private fun diskFileName(url: String): String = hash(url) + ".$DISK_FILE_EXTENSION"

  /**
   * Old builds keyed disk files by URL *and* decode size. Keep reading those
   * once so upgrading does not throw away covers that are already on device.
   */
  private fun legacyDiskFileName(url: String, maxDimension: Int): String =
    hash("$url|$maxDimension") + ".$LEGACY_DISK_FILE_EXTENSION"

  private fun hash(value: String): String {
    val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
  }

  private fun diskFile(url: String) = File(diskDir, diskFileName(url))

  fun getFromMemory(url: String, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap? =
    memoryCache.get(memoryKey(url, maxDimension))

  suspend fun loadImage(
    url: String,
    maxDimension: Int = DEFAULT_MAX_DIMENSION,
  ): Bitmap? = withContext(Dispatchers.IO) {
    if (url.isBlank()) return@withContext null

    val dimension = maxDimension.coerceAtLeast(1)
    val key = memoryKey(url, dimension)
    memoryCache.get(key)?.let { return@withContext it }

    // Disk comes first: this is the offline path and it survives process death.
    readFromDisk(url, dimension)?.let { bitmap ->
      memoryCache.put(key, bitmap)
      return@withContext bitmap
    }

    // Card, detail hero and row can ask for the same URL at once. Only one
    // request should hit the network; everyone else reuses its result.
    val lock = downloadLocks.computeIfAbsent(url) { Mutex() }
    lock.withLock {
      memoryCache.get(key)?.let { return@withLock it }
      readFromDisk(url, dimension)?.let { bitmap ->
        memoryCache.put(key, bitmap)
        return@withLock bitmap
      }

      try {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) return@withLock null
          val bytes = response.body.bytes()
          if (bytes.isEmpty()) return@withLock null

          val bitmap = decodeSampledBytes(bytes, dimension) ?: return@withLock null
          memoryCache.put(key, bitmap)
          writeAtomically(url, bytes)
          return@withLock bitmap
        }
      } catch (e: Exception) {
        Log.w(TAG, "Error fetching artwork from $url: ${e.message}")
      }

      null
    }
  }

  /**
   * Deletes the stored artwork for [urls]. Safe to call with duplicates,
   * blanks or URLs that were never cached.
   */
  fun delete(urls: Collection<String>) {
    val unique = urls.filter { it.isNotBlank() }.toSet()
    if (unique.isEmpty()) return

    unique.forEach { url ->
      runCatching { diskFile(url).delete() }
      removeFromMemory(url)
    }
  }

  private fun removeFromMemory(url: String) {
    val keys = memoryCache.snapshot().keys.filter { it.startsWith("$url|") }
    keys.forEach { memoryCache.remove(it) }
  }

  private fun readFromDisk(url: String, maxDimension: Int): Bitmap? {
    val file = diskFile(url)
    if (file.isFile && file.length() > 0L) {
      val bitmap = decodeSampledFile(file.absolutePath, maxDimension)
      if (bitmap != null) return bitmap
      // A partial/corrupt write should never wedge this URL forever.
      runCatching { file.delete() }
    }

    return migrateLegacyFile(url, maxDimension)
  }

  private fun migrateLegacyFile(url: String, maxDimension: Int): Bitmap? {
    val legacy = File(diskDir, legacyDiskFileName(url, maxDimension))
    if (!legacy.isFile || legacy.length() == 0L) return null

    val bitmap = decodeSampledFile(legacy.absolutePath, maxDimension)
    if (bitmap == null) {
      runCatching { legacy.delete() }
      return null
    }

    runCatching {
      val target = diskFile(url)
      if (target.exists()) {
        legacy.delete()
      } else {
        Files.move(
          legacy.toPath(),
          target.toPath(),
          StandardCopyOption.REPLACE_EXISTING,
        )
      }
    }.onFailure {
      Log.w(TAG, "Could not migrate legacy artwork for $url: ${it.message}")
    }

    return bitmap
  }

  private fun writeAtomically(url: String, bytes: ByteArray) {
    val target = diskFile(url)
    val temp = File(diskDir, "${target.name}.tmp")
    runCatching {
      FileOutputStream(temp).use { out ->
        out.write(bytes)
        out.flush()
        out.fd.sync()
      }
      try {
        Files.move(
          temp.toPath(),
          target.toPath(),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE,
        )
      } catch (_: Exception) {
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
      }
    }.onFailure {
      Log.w(TAG, "Failed to store artwork for $url: ${it.message}")
      runCatching { temp.delete() }
    }
  }

  private fun calculateInSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
    val longEdge = maxOf(options.outWidth, options.outHeight)
    if (longEdge <= 0) return 1
    var sampleSize = 1
    while (longEdge / sampleSize > maxDimension) sampleSize *= 2
    return sampleSize
  }

  private fun decodeSampledBytes(bytes: ByteArray, maxDimension: Int): Bitmap? =
    runCatching {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
      val opts = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds, maxDimension)
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }.getOrNull()

  private fun decodeSampledFile(path: String, maxDimension: Int): Bitmap? =
    runCatching {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(path, bounds)
      val opts = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds, maxDimension)
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }
      BitmapFactory.decodeFile(path, opts)
    }.getOrNull()
}
