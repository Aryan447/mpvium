package app.aryan447.mpvium.domain.audio

import android.net.Uri
import app.aryan447.mpvium.domain.audio.model.ChapterOrdering
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChapterOrderingTest {

  private fun createTrack(title: String, trackNumber: Int = 0): LocalAudioTrack =
    LocalAudioTrack(
      id = title.hashCode().toLong(),
      title = title,
      displayName = "$title.mp3",
      artist = "Test Author",
      album = "Test Book",
      trackNumber = trackNumber,
      uri = Uri.parse("content://audio/${title.hashCode()}"),
    )

  @Test
  fun naturalCompare_ordersNumericChaptersNaturally() {
    assertTrue("Chapter 2 must come before Chapter 10", ChapterOrdering.naturalCompare("Chapter 2", "Chapter 10") < 0)
    assertTrue("Chapter 1 must come before Chapter 2", ChapterOrdering.naturalCompare("Chapter 1", "Chapter 2") < 0)
    assertTrue("Chapter 9 must come before Chapter 10", ChapterOrdering.naturalCompare("Chapter 9", "Chapter 10") < 0)
    assertTrue("Part 01 must come before Part 02", ChapterOrdering.naturalCompare("Part 01", "Part 02") < 0)
    assertTrue("Part 2 must come before Part 10", ChapterOrdering.naturalCompare("Part 2", "Part 10") < 0)
  }

  @Test
  fun naturalCompare_caseInsensitive() {
    assertEquals(0, ChapterOrdering.naturalCompare("chapter 1", "Chapter 1"))
    assertTrue(ChapterOrdering.naturalCompare("chapter 1", "Chapter 2") < 0)
  }

  @Test
  fun compare_trackNumberTakesPrecedenceWhenAvailable() {
    val trackA = createTrack(title = "Epilogue", trackNumber = 10)
    val trackB = createTrack(title = "Chapter 1", trackNumber = 1)

    // Even though "Chapter 1" < "Epilogue" alphabetically, trackNumber 1 comes before trackNumber 10
    assertTrue("Track with trackNumber 1 should precede track with trackNumber 10", ChapterOrdering.compare(trackB, trackA) < 0)
  }

  @Test
  fun compare_fallsBackToNaturalTitleComparisonWhenTrackNumbersZero() {
    val track2 = createTrack(title = "Chapter 2", trackNumber = 0)
    val track10 = createTrack(title = "Chapter 10", trackNumber = 0)
    val track1 = createTrack(title = "Chapter 1", trackNumber = 0)

    val list = listOf(track10, track2, track1)
    val sorted = list.sortedWith(ChapterOrdering::compare)

    assertEquals("Chapter 1", sorted[0].title)
    assertEquals("Chapter 2", sorted[1].title)
    assertEquals("Chapter 10", sorted[2].title)
  }
}
