package app.aryan447.mpvium.ui.streaming.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.aryan447.mpvium.domain.media.model.VideoFolder
import app.aryan447.mpvium.domain.streaming.SeriesDetector
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.domain.streaming.model.ContinueWatchingItem
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.domain.streaming.model.StreamingCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

data class StreamingHomeUiState(
  val isLoading: Boolean = true,
  val heroSeries: LocalSeries? = null,
  val continueWatching: List<ContinueWatchingItem> = emptyList(),
  val series: List<LocalSeries> = emptyList(),
  val movies: List<LocalMovie> = emptyList(),
  val folders: List<VideoFolder> = emptyList(),
  val selectedCategory: StreamingCategory = StreamingCategory.ALL,
  val searchQuery: String = "",
  val isSearching: Boolean = false,
  val totalVideos: Int = 0,
)

class StreamingHomeViewModel(
  application: Application,
) : AndroidViewModel(application) {

  private val seriesDetector: SeriesDetector by inject(SeriesDetector::class.java)
  private val metadataRepository: StreamingMetadataRepository by inject(StreamingMetadataRepository::class.java)

  private val _uiState = MutableStateFlow(StreamingHomeUiState())
  val uiState: StateFlow<StreamingHomeUiState> = _uiState.asStateFlow()

  init {
    loadLibrary()
  }

  fun refresh() {
    loadLibrary()
  }

  fun setCategory(category: StreamingCategory) {
    _uiState.update { it.copy(selectedCategory = category) }
  }

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun setSearching(isSearching: Boolean) {
    _uiState.update { it.copy(isSearching = isSearching, searchQuery = if (!isSearching) "" else it.searchQuery) }
  }

  private fun loadLibrary() {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isLoading = true) }

      try {
        val detected = seriesDetector.detectLibrary()

        // Pick Hero banner: first in-progress series or highest episode count series
        val hero = detected.series.firstOrNull { it.lastWatchedEpisode != null && !it.lastWatchedEpisode.isWatched }
          ?: detected.series.firstOrNull()

        _uiState.update {
          it.copy(
            isLoading = false,
            heroSeries = hero,
            continueWatching = detected.continueWatching,
            series = detected.series,
            movies = detected.movies,
            folders = detected.folders,
            totalVideos = detected.totalVideoCount,
          )
        }

        // Enrich series with TMDb posters & ratings in background
        enrichSeriesMetadata(detected.series, detected.movies)
      } catch (e: Exception) {
        Log.e("StreamingHomeViewModel", "Error loading library", e)
        _uiState.update { it.copy(isLoading = false) }
      }
    }
  }

  private fun enrichSeriesMetadata(seriesList: List<LocalSeries>, moviesList: List<LocalMovie>, forceRefresh: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
      // Enrich series
      val enrichedSeries = seriesList.map { series ->
        val enriched = metadataRepository.enrichSeries(series, forceRefresh = forceRefresh)
        // Update hero if matching
        _uiState.update { state ->
          val updatedHero = if (state.heroSeries?.id == enriched.id) enriched else state.heroSeries
          val updatedSeries = state.series.map { if (it.id == enriched.id) enriched else it }
          state.copy(heroSeries = updatedHero, series = updatedSeries)
        }
        enriched
      }

      // Enrich movies
      moviesList.map { movie ->
        val enriched = metadataRepository.enrichMovie(movie, forceRefresh = forceRefresh)
        _uiState.update { state ->
          val updatedMovies = state.movies.map { if (it.video.id == enriched.video.id) enriched else it }
          state.copy(movies = updatedMovies)
        }
        enriched
      }
    }
  }

  fun refreshMetadata() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val detected = seriesDetector.detectLibrary()
        _uiState.update {
          it.copy(
            heroSeries = detected.series.firstOrNull { s -> s.lastWatchedEpisode != null && !s.lastWatchedEpisode.isWatched }
              ?: detected.series.firstOrNull(),
            continueWatching = detected.continueWatching,
            series = detected.series,
            movies = detected.movies,
            folders = detected.folders,
            totalVideos = detected.totalVideoCount,
          )
        }
        enrichSeriesMetadata(detected.series, detected.movies, forceRefresh = true)
      } catch (e: Exception) {
        Log.e("StreamingHomeViewModel", "Error refreshing library", e)
      }
    }
  }

  companion object {
    fun factory(application: Application) = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StreamingHomeViewModel(application) as T
      }
    }
  }
}
