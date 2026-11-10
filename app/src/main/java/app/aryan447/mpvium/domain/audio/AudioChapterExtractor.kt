package app.aryan447.mpvium.domain.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import app.aryan447.mpvium.domain.audio.model.AudioChapter
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import app.aryan447.mpvium.utils.media.MediaInfoOps
import app.aryan447.mpvium.utils.storage.VideoScanUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object AudioChapterExtractor {
  private const val TAG = "AudioChapterExtractor"
  private val cache = ConcurrentHashMap<String, List<AudioChapter>>()

  // Matches MediaInfo menu chapter lines: "00:12:34.567 : Title" or "01:23:45 : Title"
  private val MEDIA_INFO_MENU_LINE = Pattern.compile(
    """^(\d{2}):(\d{2}):(\d{2})(?:\.(\d+))?\s*:\s*(?:[a-z]{2,3}:)?(.+)$""",
    Pattern.CASE_INSENSITIVE
  )

  /**
   * Extracts chapters for the given audio track.
   * Checks embedded chapters via MediaInfo, then adjacent .cue sheet.
   */
  suspend fun extractChapters(context: Context, track: LocalAudioTrack): List<AudioChapter> =
    withContext(Dispatchers.IO) {
      val key = track.path.ifBlank { track.uri.toString() }
      cache[key]?.let { return@withContext it }

      val chapters = mutableListOf<AudioChapter>()

      // 1. Try adjacent CUE file if local path exists
      if (track.path.isNotBlank()) {
        try {
          val audioFile = File(track.path)
          val cueFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.cue")
          if (cueFile.exists() && cueFile.canRead()) {
            val cueChapters = parseCueSheet(cueFile.readText())
            if (cueChapters.isNotEmpty()) {
              cache[key] = cueChapters
              return@withContext cueChapters
            }
          }
        } catch (e: Exception) {
          Log.w(TAG, "Failed to read CUE file for ${track.title}", e)
        }
      }

      // 2. Try MediaInfo Inform(Text)
      try {
        val infoResult = MediaInfoOps.generateTextOutput(context, track.uri, track.displayName)
        if (infoResult.isSuccess) {
          val textOutput = infoResult.getOrNull().orEmpty()
          val parsed = parseMediaInfoText(textOutput)
          if (parsed.isNotEmpty()) {
            cache[key] = parsed
            return@withContext parsed
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed extracting chapters via MediaInfo for ${track.title}", e)
      }

      cache[key] = chapters
      chapters
    }

  /**
   * Parses MediaInfo text output for chapter markers under the Menu section.
   */
  fun parseMediaInfoText(text: String): List<AudioChapter> {
    val lines = text.lines()
    var inMenuSection = false
    val rawChapters = mutableListOf<Pair<Long, String>>()

    for (line in lines) {
      val trimmed = line.trim()
      if (trimmed.equals("Menu", ignoreCase = true)) {
        inMenuSection = true
        continue
      }

      // Another top-level section ends the Menu block
      if (inMenuSection && trimmed.isNotEmpty() && !trimmed.contains(":") && !trimmed.startsWith("=")) {
        inMenuSection = false
      }

      if (inMenuSection) {
        val matcher = MEDIA_INFO_MENU_LINE.matcher(trimmed)
        if (matcher.find()) {
          val hours = matcher.group(1)?.toLongOrNull() ?: 0L
          val mins = matcher.group(2)?.toLongOrNull() ?: 0L
          val secs = matcher.group(3)?.toLongOrNull() ?: 0L
          val millisStr = matcher.group(4) ?: "0"
          val millis = millisStr.padEnd(3, '0').take(3).toLongOrNull() ?: 0L

          val totalMs = (hours * 3600 + mins * 60 + secs) * 1000 + millis
          val title = matcher.group(5)?.trim().orEmpty()
          if (title.isNotBlank()) {
            rawChapters.add(totalMs to title)
          }
        }
      }
    }

    if (rawChapters.isEmpty()) return emptyList()

    return rawChapters.mapIndexed { index, (startMs, title) ->
      val endMs = rawChapters.getOrNull(index + 1)?.first
      AudioChapter(
        index = index + 1,
        title = title,
        startMs = startMs,
        endMs = endMs,
        formattedTime = VideoScanUtils.formatDuration(startMs),
      )
    }
  }

  /**
   * Parses standard CUE sheet text for TRACK ... TITLE ... INDEX 01 mm:ss:ff.
   */
  fun parseCueSheet(cueText: String): List<AudioChapter> {
    val lines = cueText.lines()
    val chapters = mutableListOf<AudioChapter>()
    var currentTrackNum = 0
    var currentTitle: String? = null

    val trackPattern = Pattern.compile("""^\s*TRACK\s+(\d+)\s+AUDIO""", Pattern.CASE_INSENSITIVE)
    val titlePattern = Pattern.compile("""^\s*TITLE\s+"?([^"]+)"?""", Pattern.CASE_INSENSITIVE)
    val indexPattern = Pattern.compile("""^\s*INDEX\s+01\s+(\d+):(\d+):(\d+)""", Pattern.CASE_INSENSITIVE)

    for (line in lines) {
      val trackMatcher = trackPattern.matcher(line)
      if (trackMatcher.find()) {
        currentTrackNum = trackMatcher.group(1)?.toIntOrNull() ?: (chapters.size + 1)
        currentTitle = null
        continue
      }

      val titleMatcher = titlePattern.matcher(line)
      if (titleMatcher.find()) {
        if (currentTrackNum > 0 && currentTitle == null) {
          currentTitle = titleMatcher.group(1)?.trim()
        }
        continue
      }

      val indexMatcher = indexPattern.matcher(line)
      if (indexMatcher.find() && currentTrackNum > 0) {
        val mins = indexMatcher.group(1)?.toLongOrNull() ?: 0L
        val secs = indexMatcher.group(2)?.toLongOrNull() ?: 0L
        val frames = indexMatcher.group(3)?.toLongOrNull() ?: 0L
        val totalMs = (mins * 60 + secs) * 1000 + (frames * 1000 / 75)

        val title = currentTitle ?: "Track $currentTrackNum"
        chapters.add(
          AudioChapter(
            index = currentTrackNum,
            title = title,
            startMs = totalMs,
            endMs = null,
            formattedTime = VideoScanUtils.formatDuration(totalMs),
          )
        )
      }
    }

    return chapters.mapIndexed { i, ch ->
      val nextStart = chapters.getOrNull(i + 1)?.startMs
      ch.copy(endMs = nextStart)
    }
  }
}
