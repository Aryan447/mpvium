package app.aryan447.mpvium.domain.audio.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import java.util.Locale

@Immutable
data class LocalAudiobook(
  val id: String,
  val title: String,
  val author: String,
  val folderPath: String = "",
  val artworkUri: Uri? = null,
  val parts: List<LocalAudioTrack> = emptyList(),
  val totalDuration: Long = 0L,
  val totalDurationFormatted: String = "",
  val playbackSpeed: Double = 1.0,
  val currentPartIndex: Int = 0,
  val currentPartPositionMs: Long = 0L,
  val progressPercentage: Float = 0f,
  val isCompleted: Boolean = false,
) {
  val chapterCount: Int
    get() = parts.size

  val currentPart: LocalAudioTrack?
    get() = parts.getOrNull(currentPartIndex)

  companion object {
    fun createId(folderPath: String, title: String, author: String): String {
      val normPath = folderPath.trim().lowercase(Locale.ROOT)
      val normTitle = title.trim().lowercase(Locale.ROOT)
      val normAuthor = author.trim().lowercase(Locale.ROOT)
      return if (normPath.isNotBlank()) {
        "audiobook::$normPath"
      } else {
        "audiobook::$normAuthor::$normTitle"
      }
    }
  }
}
