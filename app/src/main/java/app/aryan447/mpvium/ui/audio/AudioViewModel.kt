package app.aryan447.mpvium.ui.audio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.aryan447.mpvium.domain.audio.AudioCollectionsRepository
import app.aryan447.mpvium.domain.audio.AudioLibraryData
import app.aryan447.mpvium.domain.audio.AudioRepository
import app.aryan447.mpvium.domain.audio.model.LocalAudiobook
import app.aryan447.mpvium.domain.audio.model.LocalDiscourse
import app.aryan447.mpvium.domain.media.model.LocalAlbum
import app.aryan447.mpvium.domain.media.model.LocalAudioArtist
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

enum class AudioTabFilter {
  ALBUMS,
  AUDIOBOOKS,
  DISCOURSES,
  TRACKS,
  ARTISTS,
  ;
}

class AudioViewModel(
  application: Application,
) : AndroidViewModel(application), KoinComponent {

  private val audioRepository: AudioRepository by inject()
  private val audioCollectionsRepository: AudioCollectionsRepository by inject()

  private val _filter = MutableStateFlow(AudioTabFilter.ALBUMS)
  val filter: StateFlow<AudioTabFilter> = _filter.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

  val libraryData: StateFlow<AudioLibraryData> = audioRepository.libraryData
  val isLoading: StateFlow<Boolean> = audioRepository.isLoading

  val filteredAlbums: StateFlow<List<LocalAlbum>> = combine(
    libraryData,
    _searchQuery,
  ) { data, query ->
    val clean = query.trim().lowercase(Locale.getDefault())
    if (clean.isBlank()) {
      data.albums
    } else {
      data.albums.filter {
        it.title.lowercase(Locale.getDefault()).contains(clean) ||
          it.artist.lowercase(Locale.getDefault()).contains(clean)
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val filteredTracks: StateFlow<List<LocalAudioTrack>> = combine(
    libraryData,
    _searchQuery,
  ) { data, query ->
    val clean = query.trim().lowercase(Locale.getDefault())
    if (clean.isBlank()) {
      data.tracks
    } else {
      data.tracks.filter {
        it.title.lowercase(Locale.getDefault()).contains(clean) ||
          it.artist.lowercase(Locale.getDefault()).contains(clean) ||
          it.album.lowercase(Locale.getDefault()).contains(clean)
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val filteredArtists: StateFlow<List<LocalAudioArtist>> = combine(
    libraryData,
    _searchQuery,
  ) { data, query ->
    val clean = query.trim().lowercase(Locale.getDefault())
    if (clean.isBlank()) {
      data.artists
    } else {
      data.artists.filter {
        it.name.lowercase(Locale.getDefault()).contains(clean)
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val filteredAudiobooks: StateFlow<List<LocalAudiobook>> = combine(
    audioCollectionsRepository.audiobooks,
    _searchQuery,
  ) { books, query ->
    val clean = query.trim().lowercase(Locale.getDefault())
    if (clean.isBlank()) {
      books
    } else {
      books.filter {
        it.title.lowercase(Locale.getDefault()).contains(clean) ||
          it.author.lowercase(Locale.getDefault()).contains(clean)
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val filteredDiscourses: StateFlow<List<LocalDiscourse>> = combine(
    audioCollectionsRepository.discourses,
    _searchQuery,
  ) { discourses, query ->
    val clean = query.trim().lowercase(Locale.getDefault())
    if (clean.isBlank()) {
      discourses
    } else {
      discourses.filter {
        it.collection.lowercase(Locale.getDefault()).contains(clean) ||
          it.speaker.lowercase(Locale.getDefault()).contains(clean)
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  init {
    refresh()
  }

  fun setFilter(tab: AudioTabFilter) {
    _filter.value = tab
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setSearching(searching: Boolean) {
    _isSearching.value = searching
    if (!searching) {
      _searchQuery.value = ""
    }
  }

  fun refresh() {
    viewModelScope.launch(Dispatchers.IO) {
      audioRepository.refresh()
      audioCollectionsRepository.refresh()
    }
  }

  companion object {
    fun factory(application: Application): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          AudioViewModel(application) as T
      }
  }
}
