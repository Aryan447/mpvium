package app.aryan447.mpvium.domain.subtitles

import android.content.Context
import android.util.Log
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

/**
 * One saved subtitle position/scale pair for a show or a standalone movie.
 */
@Serializable
data class SubtitleProfile(
  val subPos: Int = 100,
  val subScale: Float = 1f,
  val updatedAt: Long = 0L,
)

/**
 * Stores per-title subtitle position and scale in a JSON file under filesDir.
 *
 * Room is deliberately not used here: the profile cache is independent from
 * the playback database and can be pruned or cleared without a schema change.
 * Writes use the same temp-file + atomic-rename approach as
 * [StreamingMetadataRepository] so a killed process cannot leave a half-written
 * profile file behind.
 */
class SubtitleProfileRepository(
  private val context: Context,
  private val json: Json,
) {
  companion object {
    private const val TAG = "SubtitleProfileRepo"
    private const val FILE_NAME = "subtitle_profiles_v1.json"
    private const val SERIES_KEY_PREFIX = "series_"
    private const val MOVIE_KEY_PREFIX = "movie_"

    fun seriesKeyFor(seriesId: String): String = StreamingMetadataRepository.seriesKeyFor(seriesId)

    fun movieKeyFor(title: String, year: String? = null): String =
      StreamingMetadataRepository.movieKeyFor(title, year)
  }

  private val profileFile = File(context.filesDir, FILE_NAME)
  private val memoryCache = ConcurrentHashMap<String, SubtitleProfile>()
  private val cacheMutex = Mutex()
  private val persistMutex = Mutex()
  private var isCacheLoaded = false

  private suspend fun ensureCacheLoaded() {
    if (isCacheLoaded) return
    cacheMutex.withLock {
      if (isCacheLoaded) return@withLock
      withContext(Dispatchers.IO) {
        try {
          if (profileFile.exists()) {
            val text = profileFile.readText()
            if (text.isNotBlank()) {
              val map = try {
                json.decodeFromString<Map<String, SubtitleProfile>>(text)
              } catch (e: Exception) {
                Log.w(TAG, "Subtitle profile file is corrupt; discarding it", e)
                runCatching { profileFile.delete() }
                emptyMap()
              }
              memoryCache.putAll(map)
              Log.d(TAG, "Loaded ${memoryCache.size} subtitle profiles from disk")
            }
          }
        } catch (e: Exception) {
          Log.w(TAG, "Failed to read subtitle profile file", e)
        }
        isCacheLoaded = true
      }
    }
  }

  private fun writeCacheAtomically(text: String) {
    val tempFile = File(context.filesDir, "$FILE_NAME.tmp")
    try {
      tempFile.writeText(text)
      try {
        Files.move(
          tempFile.toPath(),
          profileFile.toPath(),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE,
        )
      } catch (_: Exception) {
        Files.move(
          tempFile.toPath(),
          profileFile.toPath(),
          StandardCopyOption.REPLACE_EXISTING,
        )
      }
    } catch (e: Exception) {
      runCatching { tempFile.delete() }
      throw e
    }
  }

  private suspend fun persistCache() = withContext(Dispatchers.IO) {
    // Serialize the snapshot and the write together so two nearby saves
    // cannot let an older snapshot overwrite a newer one.
    persistMutex.withLock {
      val snapshot: Map<String, SubtitleProfile> = cacheMutex.withLock {
        memoryCache.toMap()
      }
      try {
        writeCacheAtomically(json.encodeToString(snapshot))
      } catch (e: Exception) {
        Log.w(TAG, "Failed to persist subtitle profile file", e)
      }
    }
  }

  suspend fun getProfile(key: String): SubtitleProfile? = withContext(Dispatchers.IO) {
    ensureCacheLoaded()
    memoryCache[key]
  }

  suspend fun saveProfile(
    key: String,
    subPos: Int,
    subScale: Float,
  ) {
    ensureCacheLoaded()
    cacheMutex.withLock {
      memoryCache[key] = SubtitleProfile(
        subPos = subPos,
        subScale = subScale,
        updatedAt = System.currentTimeMillis(),
      )
    }
    persistCache()
  }

  suspend fun clearProfile(key: String): Boolean {
    ensureCacheLoaded()
    val removed = cacheMutex.withLock { memoryCache.remove(key) != null }
    if (removed) persistCache()
    return removed
  }

  suspend fun clearAll() {
    ensureCacheLoaded()
    val changed = cacheMutex.withLock {
      val hadEntries = memoryCache.isNotEmpty()
      memoryCache.clear()
      hadEntries
    }
    if (changed) persistCache()
  }

  suspend fun profileCount(): Int = withContext(Dispatchers.IO) {
    ensureCacheLoaded()
    memoryCache.size
  }

  /**
   * Drops profiles for shows/movies that no longer exist on device. Pass null
   * for a media type to leave it untouched (for example, a series-only rescan).
   */
  suspend fun pruneStaleProfiles(
    presentSeriesIds: Set<String>? = null,
    presentMovieKeys: Set<String>? = null,
  ) {
    ensureCacheLoaded()
    var changed = false
    cacheMutex.withLock {
      if (presentSeriesIds != null) {
        val keep = presentSeriesIds.mapTo(HashSet()) { seriesKeyFor(it) }
        val iterator = memoryCache.keys.iterator()
        while (iterator.hasNext()) {
          val key = iterator.next()
          if (key.startsWith(SERIES_KEY_PREFIX) && key !in keep) {
            iterator.remove()
            changed = true
          }
        }
      }
      if (presentMovieKeys != null) {
        // Keep both the year-qualified key and the title-only fallback used by
        // older entries, matching the metadata cache's prune behaviour.
        val keep = HashSet<String>()
        presentMovieKeys.forEach {
          keep.add(it)
          keep.add(it.substringBefore('@'))
        }
        val iterator = memoryCache.keys.iterator()
        while (iterator.hasNext()) {
          val key = iterator.next()
          if (key.startsWith(MOVIE_KEY_PREFIX) && key !in keep) {
            iterator.remove()
            changed = true
          }
        }
      }
    }
    if (changed) {
      Log.d(TAG, "Pruned stale subtitle profiles")
      persistCache()
    }
  }
}
