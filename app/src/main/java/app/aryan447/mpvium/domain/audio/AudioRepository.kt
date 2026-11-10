package app.aryan447.mpvium.domain.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AudioRepository(
  private val context: Context,
  private val artworkStore: AudioArtworkStore? = null,
) {
  private val _libraryData = MutableStateFlow(AudioLibraryData())
  val libraryData: StateFlow<AudioLibraryData> = _libraryData.asStateFlow()

  private val _hasAudio = MutableStateFlow(false)
  val hasAudio: StateFlow<Boolean> = _hasAudio.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  suspend fun checkHasAudio(): Boolean = withContext(Dispatchers.IO) {
    val present = AudioScanUtils.hasAudio(context)
    _hasAudio.value = present
    present
  }

  suspend fun refresh(): AudioLibraryData = withContext(Dispatchers.IO) {
    _isLoading.value = true
    try {
      val data = AudioScanUtils.scanAudio(context)
      _libraryData.value = data
      _hasAudio.value = data.tracks.isNotEmpty()
      val activeAlbumKeys = data.albums.map { it.id }.toSet()
      artworkStore?.prune(activeAlbumKeys)
      data
    } finally {
      _isLoading.value = false
    }
  }

  fun getLibrarySnapshot(): AudioLibraryData = _libraryData.value
}
