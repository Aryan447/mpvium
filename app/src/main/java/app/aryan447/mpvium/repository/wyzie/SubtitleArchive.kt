package app.aryan447.mpvium.repository.wyzie

import android.util.Log
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Unpacks subtitle payloads that arrive as compressed archives.
 *
 * Some providers (e.g. SubSource) return a ZIP containing one or more
 * subtitle files instead of the raw subtitle, so the best entry is
 * extracted here. When a season/episode is known, the archive member
 * matching that episode is preferred over season packs.
 */
internal object SubtitleArchive {
  private const val TAG = "SubtitleArchive"

  /** Extensions the player can load, in order of preference. */
  private val PREFERRED_EXTENSIONS = listOf(
    "srt", "ass", "ssa", "vtt", "sub", "smi", "sami", "mpl", "txt", "lrc"
  )

  fun isZip(bytes: ByteArray): Boolean =
    bytes.size >= 4 &&
      bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
      (bytes[2] == 0x03.toByte() || bytes[2] == 0x05.toByte() || bytes[2] == 0x07.toByte())

  /**
   * Returns the subtitle payload and the extension to save it with. If
   * [bytes] is a ZIP archive, the best matching entry inside it is extracted;
   * otherwise [bytes] is returned untouched with [fallbackExtension].
   */
  fun unwrapIfZip(
    bytes: ByteArray,
    fallbackExtension: String? = null,
    season: Int? = null,
    episode: Int? = null,
  ): Pair<ByteArray, String> {
    if (!isZip(bytes)) return bytes to (fallbackExtension ?: "srt")
    return try {
      val entries = mutableListOf<Pair<String, ByteArray>>()
      ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        var entry = zip.nextEntry
        var guard = 0
        while (entry != null && guard < 200) {
          guard++
          try {
            if (!entry.isDirectory) {
              val data = zip.readBytes()
              if (data.isNotEmpty()) entries.add(entry.name to data)
            }
          } catch (e: Exception) {
            Log.w(TAG, "Skipping unreadable zip entry ${entry.name}: ${e.message}")
          }
          entry = zip.nextEntry
        }
      }
      val best = pickBest(entries, season, episode)
        ?: throw IllegalStateException("Archive contains no files")
      val extension = best.first.substringAfterLast(".", "").lowercase()
        .takeIf { it.isNotEmpty() } ?: fallbackExtension ?: "srt"
      best.second to extension
    } catch (e: Exception) {
      Log.w(TAG, "Failed to unpack subtitle archive, keeping raw payload: ${e.message}")
      bytes to (fallbackExtension ?: "srt")
    }
  }

  private fun pickBest(
    entries: List<Pair<String, ByteArray>>,
    season: Int?,
    episode: Int?,
  ): Pair<String, ByteArray>? {
    if (entries.isEmpty()) return null
    if (season != null && episode != null) {
      val target = Regex("""(?i)[Ss]0*${Regex.escape(season.toString())}[\s._-]*[Ee]0*${Regex.escape(episode.toString())}""")
      entries.firstOrNull { (name, _) -> target.containsMatchIn(name) }?.let { return it }
    }
    // Prefer entries whose extension the player supports, in preference order.
    for (ext in PREFERRED_EXTENSIONS) {
      entries.firstOrNull { (name, _) ->
        name.substringAfterLast(".", "").lowercase() == ext &&
          !name.lowercase().contains("sample")
      }?.let { return it }
    }
    // Fall back to the first non-sample file, then the very first entry.
    return entries.firstOrNull { (name, _) -> !name.lowercase().contains("sample") }
      ?: entries.first()
  }
}