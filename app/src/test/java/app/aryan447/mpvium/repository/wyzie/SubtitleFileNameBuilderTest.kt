package app.aryan447.mpvium.repository.wyzie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleFileNameBuilderTest {

  private fun subtitle(url: String, origin: String? = null) = WyzieSubtitle(
    url = url,
    origin = origin,
    source = "OpenSubtitles",
    language = "en",
  )

  @Test
  fun differentSubtitleUrlsGetDifferentNames() {
    val first = SubtitleFileNameBuilder.baseName(
      "Spider-Man.Homecoming.2017.1080p.mkv",
      "en",
      subtitle("https://example.com/one.srt", "FGT"),
    )
    val second = SubtitleFileNameBuilder.baseName(
      "Spider-Man.Homecoming.2017.1080p.mkv",
      "en",
      subtitle("https://example.com/two.srt", "FGT"),
    )

    assertNotEquals(first, second)
  }

  @Test
  fun sameUrlGetsSameBaseName() {
    val first = SubtitleFileNameBuilder.baseName(
      "Spider-Man.Homecoming.2017.mkv",
      "en",
      subtitle("https://example.com/one.srt", "FGT"),
    )
    val second = SubtitleFileNameBuilder.baseName(
      "Spider-Man.Homecoming.2017.mkv",
      "en",
      subtitle("https://example.com/one.srt", "FGT"),
    )

    assertEquals(first, second)
  }

  @Test
  fun mediaExtensionIsRemovedAndInvalidCharactersAreSanitized() {
    val name = SubtitleFileNameBuilder.baseName(
      "Spider-Man: Homecoming (2017).mkv",
      "en",
      subtitle("https://example.com/one.srt", "FGT"),
    )

    assertTrue(name.startsWith("Spider-Man_ Homecoming (2017).en.FGT."))
    assertTrue(name.endsWith(".srt").not())
  }

  @Test
  fun uniqueNameAppendsCounterWhenFileExists() {
    val existing = mutableSetOf("Movie.en.FGT.ABC123.srt")

    val name = SubtitleFileNameBuilder.uniqueName(
      "Movie.en.FGT.ABC123",
      "srt",
    ) { it in existing }

    assertEquals("Movie.en.FGT.ABC123.2.srt", name)
  }

  @Test
  fun uniqueNameKeepsOriginalWhenFree() {
    val name = SubtitleFileNameBuilder.uniqueName(
      "Movie.en.FGT.ABC123",
      "srt",
    ) { false }

    assertEquals("Movie.en.FGT.ABC123.srt", name)
  }
}
