package app.aryan447.mpvium.ui.library.insights

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.aryan447.mpvium.domain.recentlyplayed.repository.RecentlyPlayedRepository
import app.aryan447.mpvium.domain.streaming.SeriesDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

data class TopTitle(
  val title: String,
  val plays: Int,
)

data class InsightsUiState(
  val isLoading: Boolean = true,
  val totalVideos: Int = 0,
  val movieCount: Int = 0,
  val seriesCount: Int = 0,
  val episodeCount: Int = 0,
  val watchedCount: Int = 0,
  val inProgressCount: Int = 0,
  val unwatchedCount: Int = 0,
  val completionFraction: Float = 0f,
  val storageBytes: Long = 0L,
  val runtimeMs: Long = 0L,
  val embeddedSubsCount: Int = 0,
  val weekPlays: Int = 0,
  val weekMinutes: Long = 0L,
  val topTitles: List<TopTitle> = emptyList(),
)

class InsightsViewModel(
  application: Application,
) : AndroidViewModel(application) {

  private val seriesDetector: SeriesDetector by inject(SeriesDetector::class.java)
  private val recentRepo: RecentlyPlayedRepository by inject(RecentlyPlayedRepository::class.java)

  private val _uiState = MutableStateFlow(InsightsUiState())
  val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

  init {
    load()
  }

  fun refresh() {
    load()
  }

  private fun load() {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val detected = seriesDetector.detectLibrary()
        val episodes = detected.series.flatMap { series ->
          series.seasons.values.flatten()
        }
        val watchedMovies = detected.movies.count { it.isWatched }
        val watchedEpisodes = detected.series.sumOf { it.watchedEpisodesCount }
        val watched = watchedMovies + watchedEpisodes
        val totalUnits = detected.movies.size + episodes.size
        val inProgress = detected.continueWatching.size

        val recent = runCatching { recentRepo.getRecentlyPlayed(500) }.getOrDefault(emptyList())
        val weekCutoff = System.currentTimeMillis() - WEEK_MILLIS
        val weekPlays = recent.filter { it.timestamp >= weekCutoff }
        val topTitles = recent
          .groupingBy { it.videoTitle?.takeIf { t -> t.isNotBlank() } ?: it.fileName }
          .eachCount()
          .toList()
          .sortedByDescending { (_, plays) -> plays }
          .take(3)
          .map { (title, plays) -> TopTitle(title, plays) }

        _uiState.update {
          it.copy(
            isLoading = false,
            totalVideos = detected.totalVideoCount,
            movieCount = detected.movies.size,
            seriesCount = detected.series.size,
            episodeCount = episodes.size,
            watchedCount = watched,
            inProgressCount = inProgress,
            unwatchedCount = (totalUnits - watched - inProgress).coerceAtLeast(0),
            completionFraction = if (totalUnits > 0) (watched.toFloat() / totalUnits).coerceIn(0f, 1f) else 0f,
            storageBytes = detected.folders.sumOf { folder -> folder.totalSize },
            runtimeMs = detected.folders.sumOf { folder -> folder.totalDuration },
            embeddedSubsCount = detected.movies.count { movie -> movie.video.hasEmbeddedSubtitles } +
              episodes.count { episode -> episode.video.hasEmbeddedSubtitles },
            weekPlays = weekPlays.size,
            weekMinutes = weekPlays.sumOf { play -> play.duration } / 60_000L,
            topTitles = topTitles,
          )
        }
      } catch (e: Exception) {
        Log.e("InsightsViewModel", "Error loading insights", e)
        _uiState.update { it.copy(isLoading = false) }
      }
    }
  }

  companion object {
    private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000L

    fun factory(application: Application) = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InsightsViewModel(application) as T
      }
    }
  }
}
