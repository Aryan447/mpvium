package app.aryan447.mpvium.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioChapterExtractorTest {

  @Test
  fun parseMediaInfoText_extractsChaptersFromMenuSection() {
    val sampleMediaInfo = """
      General
      Complete name : /storage/lecture.m4a
      Format : MPEG-4
      Duration : 1 h 12 min

      Menu
      00:00:00.000                             : en:Introduction
      00:15:30.000                             : en:The Law of Gravitation
      00:45:10.500                             : en:Q&A Session

      Audio
      Format : AAC
    """.trimIndent()

    val chapters = AudioChapterExtractor.parseMediaInfoText(sampleMediaInfo)
    assertEquals(3, chapters.size)

    assertEquals("Introduction", chapters[0].title)
    assertEquals(0L, chapters[0].startMs)
    assertEquals(930_000L, chapters[0].endMs)

    assertEquals("The Law of Gravitation", chapters[1].title)
    assertEquals(930_000L, chapters[1].startMs)
    assertEquals(2_710_500L, chapters[1].endMs)

    assertEquals("Q&A Session", chapters[2].title)
    assertEquals(2_710_500L, chapters[2].startMs)
  }

  @Test
  fun parseCueSheet_extractsTracksAndTimestamps() {
    val sampleCue = """
      PERFORMER "Alan Watts"
      TITLE "Out of Your Mind"
      FILE "lecture.mp3" MP3
        TRACK 01 AUDIO
          TITLE "The Nature of Consciousness"
          INDEX 01 00:00:00
        TRACK 02 AUDIO
          TITLE "The Web of Life"
          INDEX 01 18:30:00
        TRACK 03 AUDIO
          TITLE "The Myth of Myself"
          INDEX 01 42:15:00
    """.trimIndent()

    val chapters = AudioChapterExtractor.parseCueSheet(sampleCue)
    assertEquals(3, chapters.size)

    assertEquals("The Nature of Consciousness", chapters[0].title)
    assertEquals(0L, chapters[0].startMs)

    assertEquals("The Web of Life", chapters[1].title)
    assertEquals(18 * 60 * 1000L + 30 * 1000L, chapters[1].startMs)

    assertEquals("The Myth of Myself", chapters[2].title)
    assertEquals(42 * 60 * 1000L + 15 * 1000L, chapters[2].startMs)
  }
}
