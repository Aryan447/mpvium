package app.aryan447.mpvium.domain.media.model

import androidx.compose.runtime.Immutable
import java.util.Locale

@Immutable
data class LocalAudioArtist(
  val id: String,
  val name: String,
  val trackCount: Int = 0,
  val albumCount: Int = 0,
  val albums: List<LocalAlbum> = emptyList(),
  val tracks: List<LocalAudioTrack> = emptyList(),
) {
  companion object {
    fun createId(name: String): String = name.trim().lowercase(Locale.ROOT)
  }
}
