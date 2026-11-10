package app.aryan447.mpvium.domain.audio.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import java.util.Locale

@Immutable
data class LocalDiscourse(
  val id: String,
  val collection: String,
  val speaker: String,
  val folderPath: String = "",
  val artworkUri: Uri? = null,
  val parts: List<LocalAudioTrack> = emptyList(),
  val totalDuration: Long = 0L,
  val totalDurationFormatted: String = "",
  val currentPartIndex: Int = 0,
  val currentPartPositionMs: Long = 0L,
  val progressPercentage: Float = 0f,
  val isCompleted: Boolean = false,
) {
  val partCount: Int
    get() = parts.size

  val currentPart: LocalAudioTrack?
    get() = parts.getOrNull(currentPartIndex)

  companion object {
    fun createId(speaker: String, collection: String): String {
      val normSpeaker = speaker.trim().lowercase(Locale.ROOT)
      val normCollection = collection.trim().lowercase(Locale.ROOT)
      return "discourse::$normSpeaker::$normCollection"
    }
  }
}
