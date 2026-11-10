package app.aryan447.mpvium.domain.audio

import app.aryan447.mpvium.database.entities.PlaybackStateEntity
import app.aryan447.mpvium.domain.audio.model.AudioCollectionType
import app.aryan447.mpvium.domain.audio.model.ChapterOrdering
import app.aryan447.mpvium.domain.audio.model.ContinueListeningItem
import app.aryan447.mpvium.domain.audio.model.LocalAudiobook
import app.aryan447.mpvium.domain.audio.model.LocalDiscourse
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import app.aryan447.mpvium.utils.storage.VideoScanUtils
import java.io.File
import java.util.Locale

object AudioCollectionsDetector {

  private val AUDIOBOOK_PATH_KEYWORDS = listOf(
    "audiobook", "audiobooks", "audio book", "audio books", "books"
  )

  private val DISCOURSE_PATH_KEYWORDS = listOf(
    "discourse", "discourses", "talks", "talk", "lectures", "lecture",
    "speeches", "speech", "podcasts", "podcast", "sermons", "sermon"
  )

  /**
   * Groups audio tracks into Audiobooks based on folder/album grouping with chapter ordering.
   */
  fun detectAudiobooks(
    tracks: List<LocalAudioTrack>,
    getSpeedForBook: (String) -> Double = { 1.0 },
    getStateForTrack: (String) -> PlaybackStateEntity? = { null },
  ): List<LocalAudiobook> {
    if (tracks.isEmpty()) return emptyList()

    // Group tracks by folder path (or fallback to albumKey)
    val folderGroups = mutableMapOf<String, MutableList<LocalAudioTrack>>()
    for (track in tracks) {
      val folderKey = getFolderKey(track)
      folderGroups.getOrPut(folderKey) { mutableListOf() }.add(track)
    }

    val audiobooks = mutableListOf<LocalAudiobook>()

    for ((folderKey, folderTracks) in folderGroups) {
      if (!isAudiobookCollection(folderKey, folderTracks)) continue

      val first = folderTracks.first()
      val folderFile = if (first.path.isNotBlank()) File(first.path).parentFile else null
      val folderPath = folderFile?.absolutePath ?: folderKey
      val title = deriveCollectionTitle(folderFile?.name, first.album, first.title)
      val author = first.artist.takeIf { it != "Unknown Artist" && it.isNotBlank() }
        ?: folderFile?.parentFile?.name
        ?: "Unknown Author"

      val bookId = LocalAudiobook.createId(folderPath, title, author)
      val sortedTracks = folderTracks.sortedWith(ChapterOrdering::compare)
      val totalDuration = sortedTracks.sumOf { it.duration }
      val artUri = sortedTracks.firstOrNull { it.albumArtUri != null }?.albumArtUri
      val speed = getSpeedForBook(bookId)

      // Calculate per-book resume (active part and position)
      val resumeInfo = calculateResume(sortedTracks, totalDuration, getStateForTrack)

      audiobooks.add(
        LocalAudiobook(
          id = bookId,
          title = title,
          author = author,
          folderPath = folderPath,
          artworkUri = artUri,
          parts = sortedTracks,
          totalDuration = totalDuration,
          totalDurationFormatted = VideoScanUtils.formatDuration(totalDuration),
          playbackSpeed = speed,
          currentPartIndex = resumeInfo.currentPartIndex,
          currentPartPositionMs = resumeInfo.currentPositionMs,
          progressPercentage = resumeInfo.overallProgress,
          isCompleted = resumeInfo.isCompleted,
        )
      )
    }

    return audiobooks.sortedBy { it.title.lowercase(Locale.ROOT) }
  }

  /**
   * Groups audio tracks into Discourses/talks based on speaker/collection grouping.
   */
  fun detectDiscourses(
    tracks: List<LocalAudioTrack>,
    getStateForTrack: (String) -> PlaybackStateEntity? = { null },
  ): List<LocalDiscourse> {
    if (tracks.isEmpty()) return emptyList()

    // Group tracks by Speaker and Collection
    val groups = mutableMapOf<String, MutableList<LocalAudioTrack>>()
    for (track in tracks) {
      val folderKey = getFolderKey(track)
      val folderFile = if (track.path.isNotBlank()) File(track.path).parentFile else null
      val speaker = track.artist.takeIf { it != "Unknown Artist" && it.isNotBlank() }
        ?: folderFile?.parentFile?.name
        ?: "Unknown Speaker"
      val collection = deriveCollectionTitle(folderFile?.name, track.album, track.title)
      val key = LocalDiscourse.createId(speaker, collection)
      groups.getOrPut(key) { mutableListOf() }.add(track)
    }

    val discourses = mutableListOf<LocalDiscourse>()

    for ((key, collectionTracks) in groups) {
      if (!isDiscourseCollection(key, collectionTracks)) continue

      val first = collectionTracks.first()
      val folderFile = if (first.path.isNotBlank()) File(first.path).parentFile else null
      val folderPath = folderFile?.absolutePath ?: ""
      val speaker = first.artist.takeIf { it != "Unknown Artist" && it.isNotBlank() }
        ?: folderFile?.parentFile?.name
        ?: "Unknown Speaker"
      val collection = deriveCollectionTitle(folderFile?.name, first.album, first.title)

      val sortedTracks = collectionTracks.sortedWith(ChapterOrdering::compare)
      val totalDuration = sortedTracks.sumOf { it.duration }
      val artUri = sortedTracks.firstOrNull { it.albumArtUri != null }?.albumArtUri

      val resumeInfo = calculateResume(sortedTracks, totalDuration, getStateForTrack)

      discourses.add(
        LocalDiscourse(
          id = key,
          collection = collection,
          speaker = speaker,
          folderPath = folderPath,
          artworkUri = artUri,
          parts = sortedTracks,
          totalDuration = totalDuration,
          totalDurationFormatted = VideoScanUtils.formatDuration(totalDuration),
          currentPartIndex = resumeInfo.currentPartIndex,
          currentPartPositionMs = resumeInfo.currentPositionMs,
          progressPercentage = resumeInfo.overallProgress,
          isCompleted = resumeInfo.isCompleted,
        )
      )
    }

    return discourses.sortedBy { it.collection.lowercase(Locale.ROOT) }
  }

  /**
   * Builds the Continue Listening feed for the Home screen from active audiobooks and discourses.
   */
  fun buildContinueListening(
    audiobooks: List<LocalAudiobook>,
    discourses: List<LocalDiscourse>,
    getStateForTrack: (String) -> PlaybackStateEntity? = { null },
    getTimestampForTrack: (LocalAudioTrack) -> Long = { track ->
      (track.dateModified * 1000L).takeIf { it > 0L } ?: (track.dateAdded * 1000L)
    },
  ): List<ContinueListeningItem> {
    val items = mutableListOf<ContinueListeningItem>()

    // In-progress audiobooks
    for (book in audiobooks) {
      if (book.progressPercentage <= 0f && book.currentPartIndex == 0 && book.currentPartPositionMs == 0L) {
        continue
      }
      if (book.isCompleted) continue

      val activeTrack = book.currentPart ?: book.parts.firstOrNull() ?: continue
      val state = getStateForTrack(activeTrack.displayName) ?: getStateForTrack(activeTrack.path)
      if (state == null && book.currentPartPositionMs == 0L) continue
      val timestamp = getTimestampForTrack(activeTrack)

      val partProgress = if (activeTrack.duration > 0) {
        (book.currentPartPositionMs.toFloat() / activeTrack.duration).coerceIn(0.02f, 1f)
      } else {
        0.05f
      }

      val subtitle = if (book.parts.size > 1) {
        "${book.author} • Part ${book.currentPartIndex + 1} of ${book.parts.size}"
      } else {
        book.author
      }

      items.add(
        ContinueListeningItem(
          id = "cl_book_${book.id}",
          collectionId = book.id,
          title = book.title,
          subtitle = subtitle,
          artworkUri = book.artworkUri,
          playbackPositionMs = book.currentPartPositionMs,
          totalDurationMs = activeTrack.duration,
          progressPercentage = partProgress,
          lastPlayedTimestamp = timestamp,
          type = AudioCollectionType.AUDIOBOOK,
          targetTrack = activeTrack,
          queue = book.parts,
          playbackSpeed = book.playbackSpeed,
        )
      )
    }

    // In-progress discourses
    for (discourse in discourses) {
      if (discourse.progressPercentage <= 0f && discourse.currentPartIndex == 0 && discourse.currentPartPositionMs == 0L) {
        continue
      }
      if (discourse.isCompleted) continue

      val activeTrack = discourse.currentPart ?: discourse.parts.firstOrNull() ?: continue
      val state = getStateForTrack(activeTrack.displayName) ?: getStateForTrack(activeTrack.path)
      if (state == null && discourse.currentPartPositionMs == 0L) continue
      val timestamp = getTimestampForTrack(activeTrack)

      val partProgress = if (activeTrack.duration > 0) {
        (discourse.currentPartPositionMs.toFloat() / activeTrack.duration).coerceIn(0.02f, 1f)
      } else {
        0.05f
      }

      val subtitle = if (discourse.parts.size > 1) {
        "${discourse.speaker} • Part ${discourse.currentPartIndex + 1} of ${discourse.parts.size}"
      } else {
        discourse.speaker
      }

      items.add(
        ContinueListeningItem(
          id = "cl_disc_${discourse.id}",
          collectionId = discourse.id,
          title = discourse.collection,
          subtitle = subtitle,
          artworkUri = discourse.artworkUri,
          playbackPositionMs = discourse.currentPartPositionMs,
          totalDurationMs = activeTrack.duration,
          progressPercentage = partProgress,
          lastPlayedTimestamp = timestamp,
          type = AudioCollectionType.DISCOURSE,
          targetTrack = activeTrack,
          queue = discourse.parts,
          playbackSpeed = 1.0,
        )
      )
    }

    // Sort with most recently played first
    return items.sortedByDescending { it.lastPlayedTimestamp }
  }

  // --- Internal helpers ---

  private fun getFolderKey(track: LocalAudioTrack): String {
    if (track.path.isNotBlank()) {
      val file = File(track.path)
      val parent = file.parentFile
      if (parent != null) return parent.absolutePath
    }
    return track.albumKey
  }

  private fun isAudiobookCollection(folderKey: String, tracks: List<LocalAudioTrack>): Boolean {
    val lowerKey = folderKey.lowercase(Locale.ROOT)
    if (AUDIOBOOK_PATH_KEYWORDS.any { lowerKey.contains(it) }) return true

    // Check if any track has .m4b extension or audiobook genre/title indicators
    return tracks.any { track ->
      val pathLower = track.path.lowercase(Locale.ROOT)
      pathLower.endsWith(".m4b") ||
        AUDIOBOOK_PATH_KEYWORDS.any { track.album.lowercase(Locale.ROOT).contains(it) } ||
        AUDIOBOOK_PATH_KEYWORDS.any { track.title.lowercase(Locale.ROOT).contains(it) }
    }
  }

  private fun isDiscourseCollection(groupKey: String, tracks: List<LocalAudioTrack>): Boolean {
    val lowerKey = groupKey.lowercase(Locale.ROOT)
    if (DISCOURSE_PATH_KEYWORDS.any { lowerKey.contains(it) }) return true

    // Check if any track indicates lecture / discourse / talk
    val hasKeyword = tracks.any { track ->
      val pathLower = track.path.lowercase(Locale.ROOT)
      DISCOURSE_PATH_KEYWORDS.any { pathLower.contains(it) } ||
        DISCOURSE_PATH_KEYWORDS.any { track.album.lowercase(Locale.ROOT).contains(it) } ||
        DISCOURSE_PATH_KEYWORDS.any { track.title.lowercase(Locale.ROOT).contains(it) }
    }
    if (hasKeyword) return true

    // Long lectures in a collection (average > 10 min duration and 2+ parts)
    if (tracks.size >= 2) {
      val avgDuration = tracks.sumOf { it.duration } / tracks.size
      if (avgDuration >= 600_000L) { // >= 10 minutes
        return true
      }
    }

    return false
  }

  private fun deriveCollectionTitle(folderName: String?, albumTitle: String?, trackTitle: String?): String {
    val cleanFolder = folderName?.trim().orEmpty()
    val cleanAlbum = albumTitle?.trim().orEmpty()

    val genericFolders = setOf("download", "downloads", "music", "audio", "podcasts", "audiobooks", "discourses")
    if (cleanFolder.isNotEmpty() && !genericFolders.contains(cleanFolder.lowercase(Locale.ROOT))) {
      return cleanFolder
    }
    if (cleanAlbum.isNotEmpty() && cleanAlbum != "Unknown Album") {
      return cleanAlbum
    }
    return trackTitle?.trim().orEmpty().ifBlank { "Untitled Collection" }
  }

  private data class ResumeInfo(
    val currentPartIndex: Int,
    val currentPositionMs: Long,
    val overallProgress: Float,
    val isCompleted: Boolean,
  )

  private fun calculateResume(
    parts: List<LocalAudioTrack>,
    totalDuration: Long,
    getStateForTrack: (String) -> PlaybackStateEntity?,
  ): ResumeInfo {
    if (parts.isEmpty()) {
      return ResumeInfo(0, 0L, 0f, isCompleted = false)
    }

    var activeIndex = 0
    var activePosMs = 0L
    var foundActive = false
    var totalPlayedMs = 0L
    var finishedCount = 0

    for ((index, part) in parts.withIndex()) {
      val state = getStateForTrack(part.displayName) ?: getStateForTrack(part.path)
      val posSec = state?.lastPosition ?: 0
      val posMs = posSec * 1000L
      val partDuration = part.duration.coerceAtLeast(0L)

      val isPartWatched = state?.hasBeenWatched == true ||
        (partDuration > 0 && posMs >= partDuration - 15_000L)

      if (isPartWatched) {
        totalPlayedMs += partDuration
        finishedCount++
      } else {
        totalPlayedMs += posMs
        if (!foundActive) {
          activeIndex = index
          activePosMs = posMs
          foundActive = true
        }
      }
    }

    val isAllCompleted = finishedCount == parts.size
    if (isAllCompleted) {
      activeIndex = 0
      activePosMs = 0L
    }

    val overallProgress = if (totalDuration > 0) {
      (totalPlayedMs.toFloat() / totalDuration).coerceIn(0f, 1f)
    } else {
      0f
    }

    return ResumeInfo(
      currentPartIndex = activeIndex,
      currentPositionMs = activePosMs,
      overallProgress = overallProgress,
      isCompleted = isAllCompleted,
    )
  }
}
