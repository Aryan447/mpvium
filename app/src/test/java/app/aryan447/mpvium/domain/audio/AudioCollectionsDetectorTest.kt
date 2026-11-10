package app.aryan447.mpvium.domain.audio

import android.net.Uri
import app.aryan447.mpvium.database.entities.PlaybackStateEntity
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCollectionsDetectorTest {

  private fun createTrack(
    title: String,
    album: String,
    artist: String,
    path: String,
    durationMs: Long = 600_000L,
    trackNumber: Int = 0,
  ): LocalAudioTrack = LocalAudioTrack(
    id = title.hashCode().toLong(),
    title = title,
    displayName = "$title.mp3",
    artist = artist,
    album = album,
    duration = durationMs,
    path = path,
    trackNumber = trackNumber,
    uri = Uri.parse("content://audio/${title.hashCode()}"),
  )

  @Test
  fun detectAudiobooks_groupsByFolderWithChapterOrdering() {
    val tracks = listOf(
      createTrack("Chapter 10", "Dune", "Frank Herbert", "/storage/Audiobooks/Dune/Chapter 10.mp3"),
      createTrack("Chapter 1", "Dune", "Frank Herbert", "/storage/Audiobooks/Dune/Chapter 1.mp3"),
      createTrack("Chapter 2", "Dune", "Frank Herbert", "/storage/Audiobooks/Dune/Chapter 2.mp3"),
    )

    val audiobooks = AudioCollectionsDetector.detectAudiobooks(tracks)
    assertEquals(1, audiobooks.size)

    val book = audiobooks.first()
    assertEquals("Dune", book.title)
    assertEquals("Frank Herbert", book.author)
    assertEquals(3, book.chapterCount)

    // Check chapter ordering: Chapter 1, Chapter 2, Chapter 10
    assertEquals("Chapter 1", book.parts[0].title)
    assertEquals("Chapter 2", book.parts[1].title)
    assertEquals("Chapter 10", book.parts[2].title)
  }

  @Test
  fun detectAudiobooks_perBookResume_advancesToNextUnwatchedPart() {
    val part1 = createTrack("Part 1", "Dune", "Frank Herbert", "/storage/Audiobooks/Dune/Part 1.mp3", durationMs = 100_000L)
    val part2 = createTrack("Part 2", "Dune", "Frank Herbert", "/storage/Audiobooks/Dune/Part 2.mp3", durationMs = 100_000L)
    val part3 = createTrack("Part 3", "Dune", "Frank Herbert", "/storage/Audiobooks/Dune/Part 3.mp3", durationMs = 100_000L)

    val states = mapOf(
      part1.displayName to PlaybackStateEntity(
        mediaTitle = part1.displayName,
        lastPosition = 98, // finished (> 100s - 15s)
        playbackSpeed = 1.25,
        sid = -1, subDelay = 0, subSpeed = 1.0, aid = -1, audioDelay = 0,
        hasBeenWatched = true,
      ),
      part2.displayName to PlaybackStateEntity(
        mediaTitle = part2.displayName,
        lastPosition = 30, // in progress at 30 seconds
        playbackSpeed = 1.25,
        sid = -1, subDelay = 0, subSpeed = 1.0, aid = -1, audioDelay = 0,
      )
    )

    val audiobooks = AudioCollectionsDetector.detectAudiobooks(
      tracks = listOf(part1, part2, part3),
      getStateForTrack = { states[it] },
    )

    val book = audiobooks.first()
    // Resume target should be Part 2 (index 1), at 30_000 ms
    assertEquals(1, book.currentPartIndex)
    assertEquals(30_000L, book.currentPartPositionMs)
    assertFalse("Book should not be marked as fully completed", book.isCompleted)
  }

  @Test
  fun detectDiscourses_groupsBySpeakerAndCollection() {
    val tracks = listOf(
      createTrack("Lecture 2", "Eastern Wisdom", "Alan Watts", "/storage/Discourses/Eastern Wisdom/Lecture 2.mp3", durationMs = 1_800_000L),
      createTrack("Lecture 1", "Eastern Wisdom", "Alan Watts", "/storage/Discourses/Eastern Wisdom/Lecture 1.mp3", durationMs = 1_800_000L),
      createTrack("Talk 1", "Physics Lectures", "Richard Feynman", "/storage/Discourses/Physics/Talk 1.mp3", durationMs = 2_400_000L),
    )

    val discourses = AudioCollectionsDetector.detectDiscourses(tracks)
    assertEquals(2, discourses.size)

    val watts = discourses.first { it.speaker == "Alan Watts" }
    assertEquals("Eastern Wisdom", watts.collection)
    assertEquals(2, watts.partCount)
    // Check ordering: Lecture 1, Lecture 2
    assertEquals("Lecture 1", watts.parts[0].title)
    assertEquals("Lecture 2", watts.parts[1].title)

    val feynman = discourses.first { it.speaker == "Richard Feynman" }
    assertEquals("Physics Lectures", feynman.collection)
    assertEquals(1, feynman.partCount)
  }

  @Test
  fun buildContinueListening_generatesFeedForActiveCollections() {
    val part1 = createTrack("Part 1", "Eastern Wisdom", "Alan Watts", "/storage/Discourses/Eastern Wisdom/Part 1.mp3", durationMs = 100_000L)
    val part2 = createTrack("Part 2", "Eastern Wisdom", "Alan Watts", "/storage/Discourses/Eastern Wisdom/Part 2.mp3", durationMs = 100_000L)

    val states = mapOf(
      part1.displayName to PlaybackStateEntity(
        mediaTitle = part1.displayName,
        lastPosition = 50,
        playbackSpeed = 1.0,
        sid = -1, subDelay = 0, subSpeed = 1.0, aid = -1, audioDelay = 0,
      )
    )

    val discourses = AudioCollectionsDetector.detectDiscourses(
      tracks = listOf(part1, part2),
      getStateForTrack = { states[it] },
    )

    val clItems = AudioCollectionsDetector.buildContinueListening(
      audiobooks = emptyList(),
      discourses = discourses,
      getStateForTrack = { states[it] },
    )

    assertEquals(1, clItems.size)
    val item = clItems.first()
    assertEquals("Eastern Wisdom", item.title)
    assertTrue("Subtitle should contain speaker Alan Watts", item.subtitle.contains("Alan Watts"))
    assertEquals(part1.title, item.targetTrack.title)
  }
}
