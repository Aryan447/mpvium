package app.aryan447.mpvium.domain.media.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.util.Locale

@Immutable
data class LocalAlbum(
  val id: String,
  val title: String,
  val artist: String,
  val trackCount: Int = 0,
  val duration: Long = 0L,
  val durationFormatted: String = "",
  val year: Int = 0,
  val artworkUri: Uri? = null,
  val tracks: List<LocalAudioTrack> = emptyList(),
) {
  companion object {
    /**
     * Generates a unique, stable album identifier from album name and artist name.
     * Never from title alone (per spec 14 requirement 2).
     */
    fun createId(album: String, artist: String): String {
      val normArtist = artist.trim().lowercase(Locale.ROOT)
      val normAlbum = album.trim().lowercase(Locale.ROOT)
      return "$normArtist::$normAlbum"
    }
  }
}
