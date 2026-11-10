package app.aryan447.mpvium.utils.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeTitleFormatterTest {

  @Test
  fun resolve_tvShowWithEmbeddedEpisodeTitle_formatsCleanTitle() {
    val clean = EpisodeTitleFormatter.resolve("Breaking.Bad.S05E16.Felina.720p.mkv")

    assertNotNull(clean)
    assertEquals("Breaking Bad", clean!!.title)
    assertEquals("S5:E16 \"Felina\"", clean.subtitle)
    assertEquals("Breaking Bad • S5:E16 \"Felina\"", clean.singleLine)
    assertEquals("S5:E16 \"Felina\"", clean.detailOnly)
    assertTrue(clean.subtitleIsPrimary)
  }

  @Test
  fun resolve_tvShowWithoutEpisodeTitle_formatsSeasonAndEpisodeOnly() {
    val clean = EpisodeTitleFormatter.resolve("Dexter.S02E12.1080p.mkv")

    assertNotNull(clean)
    assertEquals("Dexter", clean!!.title)
    assertEquals("S2:E12", clean.subtitle)
    assertEquals("Dexter • S2:E12", clean.singleLine)
    assertEquals("S2:E12", clean.detailOnly)
    assertTrue(clean.subtitleIsPrimary)
  }

  @Test
  fun resolve_tvShowWithoutEmbeddedTitle_usesTmdbFallback() {
    val clean = EpisodeTitleFormatter.resolve(
      rawTitle = "Dexter.S02E12.1080p.mkv",
      tmdbEpisodeTitle = "The British Invasion",
    )

    assertNotNull(clean)
    assertEquals("Dexter", clean!!.title)
    assertEquals("S2:E12 \"The British Invasion\"", clean.subtitle)
    assertEquals("Dexter • S2:E12 \"The British Invasion\"", clean.singleLine)
    assertEquals("S2:E12 \"The British Invasion\"", clean.detailOnly)
    assertTrue(clean.subtitleIsPrimary)
  }

  @Test
  fun resolve_tvShowWithEmbeddedTitle_ignoresTmdbFallback() {
    val clean = EpisodeTitleFormatter.resolve(
      rawTitle = "Breaking.Bad.S05E16.Felina.720p.mkv",
      tmdbEpisodeTitle = "Ignore This Name",
    )

    assertNotNull(clean)
    assertEquals("Breaking Bad", clean!!.title)
    assertEquals("S5:E16 \"Felina\"", clean.subtitle)
    assertEquals("Breaking Bad • S5:E16 \"Felina\"", clean.singleLine)
  }

  @Test
  fun resolve_movieWithYear_formatsTitleAndYear() {
    val clean = EpisodeTitleFormatter.resolve("The.Dark.Knight.2008.1080p.BluRay.x264.mkv")

    assertNotNull(clean)
    assertEquals("The Dark Knight", clean!!.title)
    assertEquals("2008", clean.subtitle)
    assertEquals("The Dark Knight • 2008", clean.singleLine)
    assertEquals("The Dark Knight", clean.detailOnly)
    assertFalse(clean.subtitleIsPrimary)
  }

  @Test
  fun resolve_movieWithoutYear_formatsTitleOnly() {
    val clean = EpisodeTitleFormatter.resolve("Inception.mkv")

    assertNotNull(clean)
    assertEquals("Inception", clean!!.title)
    assertNull(clean.subtitle)
    assertEquals("Inception", clean.singleLine)
    assertEquals("Inception", clean.detailOnly)
    assertFalse(clean.subtitleIsPrimary)
  }

  @Test
  fun resolve_tvShow_formatsS2E5WithEpisodeTitle() {
    val clean = EpisodeTitleFormatter.resolve("Show.Name.S02E05.Episode.Title.1080p.mkv")

    assertNotNull(clean)
    assertEquals("Show Name", clean!!.title)
    assertEquals("S2:E5 \"Episode Title\"", clean.subtitle)
    assertEquals("Show Name • S2:E5 \"Episode Title\"", clean.singleLine)
    assertEquals("S2:E5 \"Episode Title\"", clean.detailOnly)
  }

  @Test
  fun resolve_networkStreams_returnsNull() {
    assertNull(EpisodeTitleFormatter.resolve("http://commondatastorage.googleapis.com/sample.mp4"))
    assertNull(EpisodeTitleFormatter.resolve("https://example.com/live/stream.m3u8"))
    assertNull(EpisodeTitleFormatter.resolve("rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4"))
  }

  @Test
  fun resolve_blankOrInvalidInput_returnsNull() {
    assertNull(EpisodeTitleFormatter.resolve(""))
    assertNull(EpisodeTitleFormatter.resolve("   "))
    assertNull(EpisodeTitleFormatter.resolve("   \t\n  "))
  }

  @Test
  fun format_and_formatDetailOnly_helpersMatchResolveOutputs() {
    val rawShow = "Game.of.Thrones.S08E03.The.Long.Night.1080p.mkv"
    assertEquals(
      EpisodeTitleFormatter.resolve(rawShow)?.singleLine,
      EpisodeTitleFormatter.format(rawShow),
    )
    assertEquals(
      EpisodeTitleFormatter.resolve(rawShow)?.detailOnly,
      EpisodeTitleFormatter.formatDetailOnly(rawShow),
    )

    val rawUrl = "https://example.com/stream.mp4"
    assertNull(EpisodeTitleFormatter.format(rawUrl))
    assertNull(EpisodeTitleFormatter.formatDetailOnly(rawUrl))
  }
}
