package app.aryan447.mpvium.ui.streaming.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.aryan447.mpvium.domain.audio.AudioCollectionsRepository
import app.aryan447.mpvium.domain.audio.model.ContinueListeningItem
import app.aryan447.mpvium.domain.media.model.VideoFolder
import app.aryan447.mpvium.domain.streaming.ContinueWatchingDismissals
import app.aryan447.mpvium.domain.streaming.LibraryIndexRepository
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.repository.intro.IntroSkipRepository
import app.aryan447.mpvium.repository.subtitle.SubtitleProfileRepository
import app.aryan447.mpvium.domain.streaming.model.ContinueWatchingItem
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.domain.streaming.model.StreamingCategory
import app.aryan447.mpvium.utils.history.RecentlyPlayedOps
import app.aryan447.mpvium.utils.media.MediaLibraryEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

data class StreamingHomeUiState(
  val isLoading: Boolean = true,
  val heroSeries: LocalSeries? = null,
  val continueWatching: List<ContinueWatchingItem> = emptyList(),
  val continueListening: List<ContinueListeningItem> = emptyList(),
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

  private val libraryIndexRepository: LibraryIndexRepository by inject(LibraryIndexRepository::class.java)
  private val metadataRepository: StreamingMetadataRepository by inject(StreamingMetadataRepository::class.java)
  private val introSkipRepository: IntroSkipRepository by inject(IntroSkipRepository::class.java)
  private val subtitleProfileRepository: SubtitleProfileRepository by inject(SubtitleProfileRepository::class.java)
  private val audioCollectionsRepository: AudioCollectionsRepository by inject(AudioCollectionsRepository::class.java)

  private val _uiState = MutableStateFlow(StreamingHomeUiState())
  val uiState: StateFlow<StreamingHomeUiState> = _uiState.asStateFlow()

  init {
    loadLibrary()
    // Stack behavior: the just-played item jumps to the front instantly,
    // ahead of the slower full-library rescan that follows on resume.
    viewModelScope.launch {
      RecentlyPlayedOps.observeLastPlayedPath().collect { path ->
        if (path.isNullOrBlank()) return@collect
        _uiState.update { state ->
          val index = state.continueWatching.indexOfFirst {
            it.video.path == path || it.video.uri.toString() == path
          }
          if (index <= 0) {
            state
          } else {
            val reordered = state.continueWatching.toMutableList()
            reordered.add(0, reordered.removeAt(index))
            state.copy(continueWatching = reordered)
          }
        }
      }
    }
    // Observe Continue Listening items from audio collections
    viewModelScope.launch {
      audioCollectionsRepository.continueListening.collect { items ->
        _uiState.update { it.copy(continueListening = items) }
      }
    }
    // The player notifies after its final position is persisted, which can
    // land after ON_RESUME. Reload then so remaining times reflect the
    // newly saved value instead of a stale pre-save read.
    viewModelScope.launch(Dispatchers.IO) {
      MediaLibraryEvents.changes.collectLatest {
        loadLibrary()
      }
    }
  }

  fun refresh() {
    loadLibrary()
    viewModelScope.launch(Dispatchers.IO) {
      audioCollectionsRepository.refresh()
    }
  }

  fun removeFromContinueListening(item: ContinueListeningItem) {
    audioCollectionsRepository.dismissContinueListening(item)
  }

  /**
   * Removes a show or movie from Continue Watching immediately and persists
   * the dismissal across rescans. It reappears on its own if played again.
   */
  fun removeFromContinueWatching(item: ContinueWatchingItem) {
    val key = ContinueWatchingDismissals.keyFor(item)
    viewModelScope.launch(Dispatchers.IO) {
      runCatching { ContinueWatchingDismissals.dismiss(item) }
      _uiState.update { state ->
        state.copy(
          continueWatching = state.continueWatching.filterNot {
            ContinueWatchingDismissals.keyFor(it) == key
          },
        )
      }
    }
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
      // Stale-while-revalidate: only show the full-screen loader on first
      // load. Refreshes (e.g. returning from the player) keep the current
      // list visible and swap in fresh data when the scan completes.
      val hasData = _uiState.value.series.isNotEmpty() || _uiState.value.movies.isNotEmpty()
      if (!hasData) {
        _uiState.update { it.copy(isLoading = true) }
      }

      try {
        val detected = libraryIndexRepository.getLibrary()

        // Evict cached TMDB entries for media that no longer exists, so the
        // disk cache only shrinks when a movie or a whole show is deleted.
        // Surviving entries are reused as-is with no re-scrape on app open.
        runCatching {
          metadataRepository.pruneStaleMetadata(
            presentSeriesIds = detected.series.map { it.id }.toSet(),
            presentMovieKeys = detected.movies.map {
              StreamingMetadataRepository.movieKeyFor(it.title, it.year)
            }.toSet(),
          )
        }
        // Drop cached intro/recap windows for deleted episodes/movies so the
        // intro disk cache only shrinks when its file is gone. Surviving
        // entries are reused offline with no re-fetch on app open.
        val presentIntroTitles = buildSet {
          detected.series.forEach { series ->
            series.seasons.values.forEach { episodes ->
              episodes.forEach { add(it.video.displayName) }
            }
          }
          detected.movies.forEach { add(it.video.displayName) }
        }
        runCatching {
          introSkipRepository.pruneStale(presentIntroTitles)
        }
        runCatching {
          subtitleProfileRepository.pruneStale(
            presentSeriesIds = detected.series.map { it.id }.toSet(),
            presentMovieKeys = detected.movies.map {
              StreamingMetadataRepository.movieKeyFor(it.title, it.year)
            }.toSet(),
          )
        }

        // Pick Hero banner: first in-progress series or highest episode count series
        val cachedSeries = detected.series.map { metadataRepository.applyCachedSeries(it) }
        val cachedMovies = detected.movies.map { metadataRepository.applyCachedMovie(it) }
        val seriesById = cachedSeries.associateBy { it.id }
        val movieByVideoId = cachedMovies.associateBy { it.video.id }
        val enrichedContinueWatching = detected.continueWatching.map { item ->
          if (item.isSeries) {
            val s = seriesById[item.seriesId]
            if (s != null) item.copy(title = s.title, posterUrl = s.posterUrl) else item
          } else {
            val m = movieByVideoId[item.video.id]
            if (m != null) item.copy(title = m.title, posterUrl = m.posterUrl) else item
          }
        }
        val hero = cachedSeries.firstOrNull { it.lastWatchedEpisode != null && !it.lastWatchedEpisode.isWatched }
          ?: cachedSeries.firstOrNull()

        _uiState.update {
          it.copy(
            isLoading = false,
            heroSeries = hero,
            continueWatching = enrichedContinueWatching,
            series = cachedSeries,
            movies = cachedMovies,
            folders = detected.folders,
            totalVideos = detected.totalVideoCount,
          )
        }

        // Enrich series with TMDb posters & ratings in background
        enrichSeriesMetadata(detected.series, detected.movies)

        // Warm the intro/recap disk cache on unmetered connections so later
        // playback works offline. Already-cached titles are skipped with no
        // network call; misses stay memory-only and retry next app open.
        prefetchIntroWindows(presentIntroTitles)
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
          val updatedContinueWatching = state.continueWatching.map { item ->
            if (item.isSeries && item.seriesId == enriched.id) {
              item.copy(title = enriched.title, posterUrl = enriched.posterUrl)
            } else {
              item
            }
          }
          state.copy(heroSeries = updatedHero, series = updatedSeries, continueWatching = updatedContinueWatching)
        }
        enriched
      }

      // Enrich movies
      moviesList.map { movie ->
        val enriched = metadataRepository.enrichMovie(movie, forceRefresh = forceRefresh)
        _uiState.update { state ->
          val updatedMovies = state.movies.map { if (it.video.id == enriched.video.id) enriched else it }
          val updatedContinueWatching = state.continueWatching.map { item ->
            if (!item.isSeries && item.video.id == enriched.video.id) {
              item.copy(title = enriched.title, posterUrl = enriched.posterUrl)
            } else {
              item
            }
          }
          state.copy(movies = updatedMovies, continueWatching = updatedContinueWatching)
        }
        enriched
      }
    }
  }

  private fun prefetchIntroWindows(titles: Set<String>) {
    viewModelScope.launch(Dispatchers.IO) {
      runCatching {
        if (!introSkipRepository.canPrefetch()) {
          Log.d("StreamingHomeViewModel", "Skipping intro prefetch: no unmetered connection")
          return@launch
        }
        introSkipRepository.prefetchAll(titles)
      }
    }
  }

  fun refreshMetadata() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val detected = libraryIndexRepository.refresh(force = true)
        val cachedSeries = detected.series.map { metadataRepository.applyCachedSeries(it) }
        val cachedMovies = detected.movies.map { metadataRepository.applyCachedMovie(it) }
        val seriesById = cachedSeries.associateBy { it.id }
        val movieByVideoId = cachedMovies.associateBy { it.video.id }
        val enrichedContinueWatching = detected.continueWatching.map { item ->
          if (item.isSeries) {
            val s = seriesById[item.seriesId]
            if (s != null) item.copy(title = s.title, posterUrl = s.posterUrl) else item
          } else {
            val m = movieByVideoId[item.video.id]
            if (m != null) item.copy(title = m.title, posterUrl = m.posterUrl) else item
          }
        }
        _uiState.update {
          it.copy(
            heroSeries = cachedSeries.firstOrNull { s -> s.lastWatchedEpisode != null && !s.lastWatchedEpisode.isWatched }
              ?: cachedSeries.firstOrNull(),
            continueWatching = enrichedContinueWatching,
            series = cachedSeries,
            movies = cachedMovies,
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
