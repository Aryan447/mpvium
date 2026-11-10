package app.aryan447.mpvium.domain.audio.model

import androidx.compose.runtime.Immutable

@Immutable
data class AudioChapter(
  val index: Int,
  val title: String,
  val startMs: Long,
  val endMs: Long? = null,
  val formattedTime: String = "",
)
