package app.aryan447.mpvium.domain.media.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Video(
  val id: Long,
  val title: String,
  val displayName: String = title,
  val path: String,
  val uri: Uri = Uri.EMPTY,
  val duration: Long = 0L,
  val durationFormatted: String = "",
  val size: Long = 0L,
  val sizeFormatted: String = "",
  val dateModified: Long = 0L,
  val dateAdded: Long = 0L,
  val mimeType: String = "",
  val bucketId: String = "",
  val bucketDisplayName: String = "",
  val width: Int = 0,
  val height: Int = 0,
  val fps: Float = 0f,
  val resolution: String = "",
  val hasEmbeddedSubtitles: Boolean = false,
  val subtitleCodec:  String = "",
)
