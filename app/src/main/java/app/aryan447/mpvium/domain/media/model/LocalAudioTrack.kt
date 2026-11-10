package app.aryan447.mpvium.domain.media.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class LocalAudioTrack(
  val id: Long,
  val title: String,
  val displayName: String,
  val artist: String,
  val artistId: Long = 0L,
  val album: String,
  val albumId: Long = 0L,
  val albumKey: String = LocalAlbum.createId(album, artist),
  val trackNumber: Int = 0,
  val year: Int = 0,
  val duration: Long = 0L,
  val durationFormatted: String = "",
  val size: Long = 0L,
  val sizeFormatted: String = "",
  val path: String = "",
  val uri: Uri,
  val albumArtUri: Uri? = null,
  val mimeType: String = "audio/*",
  val dateAdded: Long = 0L,
  val dateModified: Long = 0L,
)
