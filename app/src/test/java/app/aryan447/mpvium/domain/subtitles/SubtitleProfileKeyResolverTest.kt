package app.aryan447.mpvium.domain.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleProfileKeyResolverTest {
  private companion object {
    const val TV_ROOT = "/storage/emulated/0/TV"
    const val MOVIE_ROOT = "/storage/emulated/0/Movies"
  }

  @Test
  fun episodesOfOneShow_shareAKey() {
    val first = SubtitleProfileKeyResolver.resolve(
      "Show.Name.S01E01.1080p.mkv",
      "$TV_ROOT/Show Name/Season 1/Show.Name.S01E01.1080p.mkv",
    )
    val second = SubtitleProfileKeyResolver.resolve(
      "Show.Name.S01E02.1080p.mkv",
      "$TV_ROOT/Show Name/Season 1/Show.Name.S01E02.1080p.mkv",
    )

    assertEquals(first, second)
    assertTrue(first!!.startsWith("series_"))
  }

  @Test
  fun differentShows_doNotShareAKey() {
    val first = SubtitleProfileKeyResolver.resolve(
      "Show.Name.S01E01.mkv",
      "$TV_ROOT/Show Name/Season 1/Show.Name.S01E01.mkv",
    )
    val second = SubtitleProfileKeyResolver.resolve(
      "Other.Show.S01E01.mkv",
      "$TV_ROOT/Other Show/Season 1/Other.Show.S01E01.mkv",
    )

    assertNotEquals(first, second)
  }

  @Test
  fun folderFallback_matchesTheSameShowKey() {
    val explicit = SubtitleProfileKeyResolver.resolve(
      "Breaking.Bad.S01E01.mkv",
      "$TV_ROOT/Breaking Bad/Season 1/Breaking.Bad.S01E01.mkv",
    )
    val fallback = SubtitleProfileKeyResolver.resolve(
      "01.mkv",
      "$TV_ROOT/Breaking Bad/Season 1/01.mkv",
    )

    assertEquals(explicit, fallback)
  }

  @Test
  fun movies_useTheirOwnKeysAndYears() {
    val movie2010 = SubtitleProfileKeyResolver.resolve(
      "Movie.Title.2010.1080p.mkv",
      "$MOVIE_ROOT/Movie Title (2010)/Movie.Title.2010.1080p.mkv",
    )
    val movie2020 = SubtitleProfileKeyResolver.resolve(
      "Movie.Title.2020.1080p.mkv",
      "$MOVIE_ROOT/Movie Title (2020)/Movie.Title.2020.1080p.mkv",
    )

    assertTrue(movie2010!!.startsWith("movie_"))
    assertTrue(movie2020!!.startsWith("movie_"))
    assertNotEquals(movie2010, movie2020)
  }

  @Test
  fun aMovie_doesNotInheritAShowProfile() {
    val show = SubtitleProfileKeyResolver.resolve(
      "Show.Name.S01E01.mkv",
      "$TV_ROOT/Show Name/Season 1/Show.Name.S01E01.mkv",
    )
    val movie = SubtitleProfileKeyResolver.resolve(
      "Show.Name.2010.mkv",
      "$MOVIE_ROOT/Show Name (2010)/Show.Name.2010.mkv",
    )

    assertNotEquals(show, movie)
  }
}
