package app.aryan447.mpvium.domain.audio.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack

@Immutable
data class ContinueListeningItem(
  val id: String,
  val collectionId: String,
  val title: String,
  val subtitle: String,
  val artworkUri: Uri? = null,
  val playbackPositionMs: Long = 0L,
  val totalDurationMs: Long = 0L,
  val progressPercentage: Float = 0f,
  val lastPlayedTimestamp: Long = 0L,
  val type: AudioCollectionType,
  val targetTrack: LocalAudioTrack,
  val queue: List<LocalAudioTrack> = emptyList(),
  val playbackSpeed: Double = 1.0,
)
