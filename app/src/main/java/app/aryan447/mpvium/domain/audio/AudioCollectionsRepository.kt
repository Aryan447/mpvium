package app.aryan447.mpvium.domain.audio

import android.content.Context
import android.util.Log
import app.aryan447.mpvium.database.entities.PlaybackStateEntity
import app.aryan447.mpvium.domain.audio.model.AudioChapter
import app.aryan447.mpvium.domain.audio.model.ContinueListeningItem
import app.aryan447.mpvium.domain.audio.model.LocalAudiobook
import app.aryan447.mpvium.domain.audio.model.LocalDiscourse
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import app.aryan447.mpvium.domain.playbackstate.repository.PlaybackStateRepository
import app.aryan447.mpvium.preferences.preference.PreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioCollectionsRepository(
  private val context: Context,
  private val audioRepository: AudioRepository,
  private val playbackStateRepository: PlaybackStateRepository,
  private val preferenceStore: PreferenceStore,
) {
  private val TAG = "AudioCollectionsRepo"
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  private val _audiobooks = MutableStateFlow<List<LocalAudiobook>>(emptyList())
  val audiobooks: StateFlow<List<LocalAudiobook>> = _audiobooks.asStateFlow()

  private val _discourses = MutableStateFlow<List<LocalDiscourse>>(emptyList())
  val discourses: StateFlow<List<LocalDiscourse>> = _discourses.asStateFlow()

  private val _continueListening = MutableStateFlow<List<ContinueListeningItem>>(emptyList())
  val continueListening: StateFlow<List<ContinueListeningItem>> = _continueListening.asStateFlow()

  private val dismissedPref = preferenceStore.getStringSet("dismissed_continue_listening", emptySet())

  init {
    scope.launch {
      audioRepository.libraryData.collectLatest {
        recomputeCollections()
      }
    }
  }

  suspend fun refresh() = withContext(Dispatchers.IO) {
    audioRepository.refresh()
    recomputeCollections()
  }

  fun getBookSpeed(bookId: String): Double {
    val key = "book_speed_${bookId.hashCode()}"
    val speed = preferenceStore.getFloat(key, 1.0f).get()
    return speed.toDouble().coerceIn(0.5, 3.0)
  }

  suspend fun setBookSpeed(bookId: String, speed: Double) = withContext(Dispatchers.IO) {
    val key = "book_speed_${bookId.hashCode()}"
    preferenceStore.getFloat(key, 1.0f).set(speed.toFloat())

    // Update in-memory state
    _audiobooks.value = _audiobooks.value.map { book ->
      if (book.id == bookId) {
        book.copy(playbackSpeed = speed)
      } else {
        book
      }
    }
    // Also recompute continue listening with updated speed
    recomputeCollections()
  }

  suspend fun getChaptersForPart(track: LocalAudioTrack): List<AudioChapter> =
    AudioChapterExtractor.extractChapters(context, track)

  fun dismissContinueListening(item: ContinueListeningItem) {
    val current = dismissedPref.get().toMutableSet()
    current.add(item.id)
    dismissedPref.set(current)

    _continueListening.value = _continueListening.value.filter { it.id != item.id }
  }

  suspend fun prepareTrackForPlayback(track: LocalAudioTrack, playbackSpeed: Double = 1.0) =
    withContext(Dispatchers.IO) {
      val identifier = track.displayName
      val existing = playbackStateRepository.getVideoDataByTitle(identifier)
      if (existing != null) {
        if (existing.playbackSpeed != playbackSpeed) {
          playbackStateRepository.upsert(existing.copy(playbackSpeed = playbackSpeed))
        }
      } else {
        playbackStateRepository.upsert(
          PlaybackStateEntity(
            mediaTitle = identifier,
            lastPosition = 0,
            playbackSpeed = playbackSpeed,
            sid = -1,
            subDelay = 0,
            subSpeed = 1.0,
            aid = -1,
            audioDelay = 0,
            timeRemaining = (track.duration / 1000).toInt(),
          )
        )
      }
    }

  private suspend fun recomputeCollections() = withContext(Dispatchers.IO) {
    try {
      val tracks = audioRepository.getLibrarySnapshot().tracks
      if (tracks.isEmpty()) {
        _audiobooks.value = emptyList()
        _discourses.value = emptyList()
        _continueListening.value = emptyList()
        return@withContext
      }

      val getState: (String) -> PlaybackStateEntity? = { name ->
        kotlinx.coroutines.runBlocking {
          playbackStateRepository.getVideoDataByTitle(name)
        }
      }

      val books = AudioCollectionsDetector.detectAudiobooks(
        tracks = tracks,
        getSpeedForBook = { id -> getBookSpeed(id) },
        getStateForTrack = getState,
      )

      val discoursesList = AudioCollectionsDetector.detectDiscourses(
        tracks = tracks,
        getStateForTrack = getState,
      )

      val dismissed = dismissedPref.get()
      val clItems = AudioCollectionsDetector.buildContinueListening(
        audiobooks = books,
        discourses = discoursesList,
        getStateForTrack = getState,
      ).filterNot { dismissed.contains(it.id) }

      _audiobooks.value = books
      _discourses.value = discoursesList
      _continueListening.value = clItems
    } catch (e: Exception) {
      Log.e(TAG, "Error recomputing audio collections", e)
    }
  }
}
