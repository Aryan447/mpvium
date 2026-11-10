package app.aryan447.mpvium.domain.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.LruCache
import app.aryan447.mpvium.domain.media.model.LocalAlbum
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limiter ensuring no more than 1 request per second to MusicBrainz API,
 * as required by MusicBrainz API usage guidelines.
 */
object MusicBrainzRateLimiter {
  private val mutex = Mutex()
  private var lastRequestTimeMs = 0L
  private const val MIN_INTERVAL_MS = 1050L

  suspend fun acquire() = mutex.withLock {
    val now = System.currentTimeMillis()
    val elapsed = now - lastRequestTimeMs
    if (elapsed < MIN_INTERVAL_MS) {
      delay(MIN_INTERVAL_MS - elapsed)
    }
    lastRequestTimeMs = System.currentTimeMillis()
  }
}

/**
 * Durable on-disk and in-memory storage for audio album artwork.
 *
 * Implements Spec 15 requirements:
 * 1. Embedded art first via [MediaMetadataRetriever], network fallback next.
 * 2. Network source: MusicBrainz + Cover Art Archive (keyless) with rate limiting,
 *    custom User-Agent, and non-blocking I/O.
 * 3. Offline cache following [StreamingArtworkStore]'s durable pattern, keyed by
 *    album+artist ([LocalAlbum.createId]). Artwork is never evicted while an album
 *    remains in the library, and is pruned together with deleted albums during scans.
 * 4. Extends the existing image-loading and caching stack without introducing a second library.
 */
class AudioArtworkStore(
  context: Context,
  private val client: OkHttpClient,
) {
  companion object {
    private const val TAG = "AudioArtworkStore"
    private const val DISK_DIR_NAME = "audio_art"
    private const val DISK_FILE_EXTENSION = "img"
    private const val USER_AGENT = "Mpvium/1.4.0 ( https://github.com/Aryan447/mpvium )"

    const val DEFAULT_MAX_DIMENSION = 1080
  }

  private val appContext = context.applicationContext ?: context
  private val diskDir: File = File(appContext.filesDir, DISK_DIR_NAME).apply { mkdirs() }
  private val memoryCache: LruCache<String, Bitmap>
  private val downloadLocks = ConcurrentHashMap<String, Mutex>()

  init {
    val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
    val cacheSizeKb = (maxMemoryKb / 8).coerceAtLeast(1024)
    memoryCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
      override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
  }

  private fun memoryKey(albumKey: String, maxDimension: Int): String = "$albumKey|$maxDimension"

  private fun diskFileName(albumKey: String): String = "${hash(albumKey)}.$DISK_FILE_EXTENSION"

  private fun hash(value: String): String {
    val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
  }

  private fun diskFile(albumKey: String): File = File(diskDir, diskFileName(albumKey))

  fun getFromMemory(albumKey: String, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap? {
    if (albumKey.isBlank()) return null
    return memoryCache.get(memoryKey(albumKey, maxDimension.coerceAtLeast(1)))
  }

  /**
   * Resolves artwork for an album following the spec hierarchy:
   * 1. Memory cache
   * 2. Offline disk cache (durable)
   * 3. Embedded art via MediaMetadataRetriever from local track files / content URIs
   * 4. Network fallback via MusicBrainz + Cover Art Archive
   */
  suspend fun loadArtwork(
    album: String,
    artist: String,
    tracks: List<LocalAudioTrack> = emptyList(),
    albumArtUri: Uri? = null,
    maxDimension: Int = DEFAULT_MAX_DIMENSION,
  ): Bitmap? = withContext(Dispatchers.IO) {
    val albumKey = LocalAlbum.createId(album, artist)
    if (albumKey.isBlank()) return@withContext null

    val dimension = maxDimension.coerceAtLeast(1)
    val mKey = memoryKey(albumKey, dimension)

    // 1. Memory cache check
    memoryCache.get(mKey)?.let { return@withContext it }

    // 2. Durable disk cache check
    readFromDisk(albumKey, dimension)?.let { bitmap ->
      memoryCache.put(mKey, bitmap)
      return@withContext bitmap
    }

    // Deduplicate concurrent requests for the same album
    val lock = downloadLocks.computeIfAbsent(albumKey) { Mutex() }
    lock.withLock {
      memoryCache.get(mKey)?.let { return@withLock it }
      readFromDisk(albumKey, dimension)?.let { bitmap ->
        memoryCache.put(mKey, bitmap)
        return@withLock bitmap
      }

      // 3. Embedded art first via MediaMetadataRetriever
      val embeddedBytes = extractEmbeddedArt(tracks, albumArtUri)
      if (embeddedBytes != null && embeddedBytes.isNotEmpty()) {
        val bitmap = decodeSampledBytes(embeddedBytes, dimension)
        if (bitmap != null) {
          writeAtomically(albumKey, embeddedBytes)
          memoryCache.put(mKey, bitmap)
          return@withLock bitmap
        }
      }

      // 4. Network fallback next via MusicBrainz + Cover Art Archive
      val networkBytes = fetchNetworkArtwork(album, artist)
      if (networkBytes != null && networkBytes.isNotEmpty()) {
        val bitmap = decodeSampledBytes(networkBytes, dimension)
        if (bitmap != null) {
          writeAtomically(albumKey, networkBytes)
          memoryCache.put(mKey, bitmap)
          return@withLock bitmap
        }
      }

      null
    }
  }

  /**
   * Extracts embedded album artwork using [MediaMetadataRetriever] on available tracks.
   * Falls back to MediaStore's content resolver if track file extraction yields nothing.
   */
  private fun extractEmbeddedArt(
    tracks: List<LocalAudioTrack>,
    albumArtUri: Uri?,
  ): ByteArray? {
    for (track in tracks.take(3)) {
      val bytes = extractFromTrack(track)
      if (bytes != null && bytes.isNotEmpty()) return bytes
    }

    if (albumArtUri != null) {
      runCatching {
        appContext.contentResolver.openInputStream(albumArtUri)?.use { input ->
          val bytes = input.readBytes()
          if (bytes.size > 100) return bytes
        }
      }
    }

    return null
  }

  private fun extractFromTrack(track: LocalAudioTrack): ByteArray? {
    val mmr = MediaMetadataRetriever()
    try {
      if (track.path.isNotBlank() && File(track.path).exists()) {
        mmr.setDataSource(track.path)
        val pic = mmr.embeddedPicture
        if (pic != null && pic.isNotEmpty()) return pic
      }

      if (track.uri.scheme == "content") {
        appContext.contentResolver.openFileDescriptor(track.uri, "r")?.use { pfd ->
          mmr.setDataSource(pfd.fileDescriptor)
          val pic = mmr.embeddedPicture
          if (pic != null && pic.isNotEmpty()) return pic
        }
      } else {
        mmr.setDataSource(appContext, track.uri)
        val pic = mmr.embeddedPicture
        if (pic != null && pic.isNotEmpty()) return pic
      }
    } catch (_: Exception) {
    } finally {
      runCatching { mmr.release() }
    }
    return null
  }

  /**
   * Fetches album cover art from MusicBrainz + Cover Art Archive.
   * Respects rate limiting, provides an explicit User-Agent, and avoids blocking UI thread.
   */
  private suspend fun fetchNetworkArtwork(album: String, artist: String): ByteArray? {
    val cleanAlbum = cleanAlbumTitle(album)
    val cleanArtist = artist.trim()
    if (cleanAlbum.isBlank() || cleanAlbum.equals("Unknown Album", ignoreCase = true)) {
      return null
    }

    val releases = queryMusicBrainzReleases(cleanAlbum, cleanArtist)
    if (releases.isEmpty()) return null

    for (release in releases.take(3)) {
      val imageBytes = fetchFromCoverArtArchive(release)
      if (imageBytes != null && imageBytes.isNotEmpty()) {
        return imageBytes
      }
    }

    return null
  }

  private suspend fun queryMusicBrainzReleases(album: String, artist: String): List<MusicBrainzRelease> {
    val hasArtist = artist.isNotBlank() && !artist.equals("Unknown Artist", ignoreCase = true)
    val luceneQuery = if (hasArtist) {
      """release:"${escapeLucene(album)}" AND artist:"${escapeLucene(artist)}""""
    } else {
      """release:"${escapeLucene(album)}""""
    }

    val encoded = URLEncoder.encode(luceneQuery, "UTF-8")
    val url = "https://musicbrainz.org/ws/2/release/?query=$encoded&fmt=json&limit=3"

    MusicBrainzRateLimiter.acquire()

    return try {
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .header("Accept", "application/json")
        .build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return emptyList()
        val body = response.body.string()
        val json = JSONObject(body)
        val releasesArray = json.optJSONArray("releases") ?: return emptyList()
        val results = mutableListOf<MusicBrainzRelease>()
        for (i in 0 until releasesArray.length()) {
          val rel = releasesArray.getJSONObject(i)
          val id = rel.optString("id")
          val releaseGroup = rel.optJSONObject("release-group")
          val releaseGroupId = releaseGroup?.optString("id")
          if (id.isNotBlank()) {
            results.add(MusicBrainzRelease(id, releaseGroupId))
          }
        }
        results
      }
    } catch (e: Exception) {
      Log.w(TAG, "MusicBrainz lookup failed for $album / $artist: ${e.message}")
      emptyList()
    }
  }

  private fun fetchFromCoverArtArchive(release: MusicBrainzRelease): ByteArray? {
    val candidates = buildList {
      add("https://coverartarchive.org/release/${release.id}/front-500")
      add("https://coverartarchive.org/release/${release.id}/front")
      if (!release.releaseGroupId.isNullOrBlank()) {
        add("https://coverartarchive.org/release-group/${release.releaseGroupId}/front-500")
        add("https://coverartarchive.org/release-group/${release.releaseGroupId}/front")
      }
    }

    for (url in candidates) {
      try {
        val request = Request.Builder()
          .url(url)
          .header("User-Agent", USER_AGENT)
          .build()

        val bytes = client.newCall(request).execute().use { response ->
          if (response.isSuccessful) {
            val data = response.body.bytes()
            if (data.size > 256) data else null
          } else {
            null
          }
        }
        if (bytes != null) return bytes
      } catch (e: Exception) {
        Log.d(TAG, "CAA query $url: ${e.message}")
      }
    }
    return null
  }

  /**
   * Prunes cached artwork on disk for albums that are no longer present in the library.
   * Artwork for albums still in [activeAlbumKeys] is NEVER evicted.
   */
  fun prune(activeAlbumKeys: Set<String>) {
    if (activeAlbumKeys.isEmpty()) return
    val activeHashes = activeAlbumKeys.map { hash(it) }.toSet()
    val files = diskDir.listFiles() ?: return

    for (file in files) {
      if (!file.isFile || file.name.endsWith(".tmp")) continue
      val baseName = file.nameWithoutExtension
      if (baseName !in activeHashes) {
        file.delete()
      }
    }

    // Clean memory cache of stale entries
    val keys = memoryCache.snapshot().keys
    for (k in keys) {
      val albumKey = k.substringBefore('|')
      if (albumKey !in activeAlbumKeys) {
        memoryCache.remove(k)
      }
    }
  }

  /**
   * Deletes cached artwork for specified album keys.
   */
  fun delete(albumKeys: Collection<String>) {
    val unique = albumKeys.filter { it.isNotBlank() }.toSet()
    if (unique.isEmpty()) return

    unique.forEach { albumKey ->
      runCatching { diskFile(albumKey).delete() }
      removeFromMemory(albumKey)
    }
  }

  private fun removeFromMemory(albumKey: String) {
    val keys = memoryCache.snapshot().keys.filter { it.startsWith("$albumKey|") }
    keys.forEach { memoryCache.remove(it) }
  }

  private fun readFromDisk(albumKey: String, maxDimension: Int): Bitmap? {
    val file = diskFile(albumKey)
    if (file.isFile && file.length() > 0L) {
      val bitmap = decodeSampledFile(file.absolutePath, maxDimension)
      if (bitmap != null) return bitmap
      runCatching { file.delete() }
    }
    return null
  }

  private fun writeAtomically(albumKey: String, bytes: ByteArray) {
    val target = diskFile(albumKey)
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
      Log.w(TAG, "Failed to store artwork for $albumKey: ${it.message}")
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

  private fun cleanAlbumTitle(title: String): String {
    return title
      .replace(Regex("""\s*[\(\[][^\)\]]*(?:remaster|edition|deluxe|bonus|version|anniversary|expanded|reissue)[^\)\]]*[\)\]]""", RegexOption.IGNORE_CASE), "")
      .trim()
  }

  private fun escapeLucene(text: String): String {
    val sb = StringBuilder()
    for (char in text) {
      if (char in "\\+-!(){}[]^\"~*?:/") {
        sb.append('\\').append(char)
      } else {
        sb.append(char)
      }
    }
    return sb.toString()
  }

  private data class MusicBrainzRelease(
    val id: String,
    val releaseGroupId: String?,
  )
}
