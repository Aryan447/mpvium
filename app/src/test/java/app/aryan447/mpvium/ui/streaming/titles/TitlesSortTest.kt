package app.aryan447.mpvium.ui.streaming.titles

import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.streaming.model.LocalEpisode
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.ui.streaming.components.MovieDurationFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TitlesSortTest {

  private fun createVideo(
    id: Long,
    title: String,
    durationMs: Long,
    dateAdded: Long = 1000L,
  ): Video = Video(
    id = id,
    title = title,
    displayName = "$title.mp4",
    path = "/storage/emulated/0/Movies/$title.mp4",
    duration = durationMs,
    size = 1024L * 1024L,
    dateModified = dateAdded,
    dateAdded = dateAdded,
  )

  private fun createMovie(
    id: Long,
    title: String,
    durationMs: Long,
    dateAdded: Long = 1000L,
    rating: Float? = null,
    isWatched: Boolean = false,
    progressPercentage: Float = 0f,
  ): TitleItem.Movie = TitleItem.Movie(
    LocalMovie(
      video = createVideo(id, title, durationMs, dateAdded),
      title = title,
      rating = rating,
      isWatched = isWatched,
      progressPercentage = progressPercentage,
    )
  )

  private fun createSeries(
    id: String,
    title: String,
    totalEpisodes: Int,
    totalDurationMs: Long = totalEpisodes * 45 * 60 * 1000L,
    newestDateAdded: Long = 1000L,
    rating: Float? = null,
    watchedEpisodes: Int = 0,
  ): TitleItem.Show {
    val episodes = (1..totalEpisodes).map { epNum ->
      LocalEpisode(
        video = createVideo(
          id = id.hashCode().toLong() + epNum,
          title = "$title S01E$epNum",
          durationMs = totalDurationMs / totalEpisodes.coerceAtLeast(1),
          dateAdded = newestDateAdded,
        ),
        seasonNumber = 1,
        episodeNumber = epNum,
      )
    }
    return TitleItem.Show(
      LocalSeries(
        id = id,
        title = title,
        rating = rating,
        totalEpisodes = totalEpisodes,
        totalDurationMs = totalDurationMs,
        watchedEpisodesCount = watchedEpisodes,
        seasons = mapOf(1 to episodes),
      )
    )
  }

  @Test
  fun sortByName_normalizesMixedShowsAndMovies() {
    val items: List<TitleItem> = listOf(
      createMovie(1L, "Zootopia", 6000000L),
      createSeries("bb", "Breaking Bad", 62),
      createMovie(2L, "Avatar", 9000000L),
      createSeries("ch", "Chernobyl", 5),
    )

    val sorted = items.sortedByTitlesSort(TitlesSort.NAME)
    assertEquals(listOf("Avatar", "Breaking Bad", "Chernobyl", "Zootopia"), sorted.map { it.title })
  }

  @Test
  fun sortByRecentlyAdded_normalizesMixedShowsAndMovies() {
    val items: List<TitleItem> = listOf(
      createMovie(1L, "Older Movie", 6000000L, dateAdded = 100L),
      createSeries("new_show", "New Show", 10, newestDateAdded = 500L),
      createMovie(2L, "Newest Movie", 6000000L, dateAdded = 900L),
      createSeries("old_show", "Old Show", 10, newestDateAdded = 200L),
    )

    val sorted = items.sortedByTitlesSort(TitlesSort.RECENTLY_ADDED)
    assertEquals(listOf("Newest Movie", "New Show", "Old Show", "Older Movie"), sorted.map { it.title })
  }

  @Test
  fun sortByRating_ordersHighestFirstWithUnratedAtEnd() {
    val items: List<TitleItem> = listOf(
      createMovie(1L, "Unrated Movie", 6000000L, rating = null),
      createSeries("high_show", "High Show", 10, rating = 9.2f),
      createMovie(2L, "Medium Movie", 6000000L, rating = 7.5f),
      createSeries("unrated_show", "Unrated Show", 10, rating = null),
      createMovie(3L, "Top Movie", 6000000L, rating = 9.8f),
    )

    val sorted = items.sortedByTitlesSort(TitlesSort.RATING)
    assertEquals(listOf("Top Movie", "High Show", "Medium Movie", "Unrated Movie", "Unrated Show"), sorted.map { it.title })
  }

  @Test
  fun sortByLongest_ordersShowsByEpisodesAndMoviesByDuration() {
    // Shows: 100 eps, 10 eps, 1 ep
    // Movies: 3h (180 min), 1.5h (90 min), 30 min
    val longShow = createSeries("long_show", "Long Show (100 eps)", totalEpisodes = 100)
    val midShow = createSeries("mid_show", "Mid Show (10 eps)", totalEpisodes = 10)
    val shortShow = createSeries("short_show", "Short Show (1 ep)", totalEpisodes = 1)

    val longMovie = createMovie(1L, "Long Movie (3h)", 3 * 3600 * 1000L)
    val midMovie = createMovie(2L, "Mid Movie (1.5h)", 90 * 60 * 1000L)
    val shortMovie = createMovie(3L, "Short Movie (30m)", 30 * 60 * 1000L)

    val items: List<TitleItem> = listOf(shortMovie, midShow, longMovie, shortShow, longShow, midMovie)
    val sorted = items.sortedByTitlesSort(TitlesSort.LONGEST)

    // Long Show (100 eps ~ 75h) > Mid Show (10 eps ~ 7.5h) > Long Movie (3h) > Mid Movie (1.5h) > Short Show (1 ep ~ 45m) > Short Movie (30m)
    assertEquals(
      listOf("Long Show (100 eps)", "Mid Show (10 eps)", "Long Movie (3h)", "Mid Movie (1.5h)", "Short Show (1 ep)", "Short Movie (30m)"),
      sorted.map { it.title }
    )
  }

  @Test
  fun sortByShortest_reversesLongestOrder() {
    val longShow = createSeries("long_show", "Long Show", totalEpisodes = 50)
    val shortShow = createSeries("short_show", "Short Show", totalEpisodes = 1)
    val longMovie = createMovie(1L, "Long Movie", 2 * 3600 * 1000L)
    val shortMovie = createMovie(2L, "Short Movie", 30 * 60 * 1000L)

    val items: List<TitleItem> = listOf(longShow, shortShow, longMovie, shortMovie)
    val sorted = items.sortedByTitlesSort(TitlesSort.SHORTEST)

    assertEquals(listOf("Short Movie", "Short Show", "Long Movie", "Long Show"), sorted.map { it.title })
  }

  @Test
  fun watchFilter_matchesShowsAndMoviesCorrectly() {
    val unwatchedMovie = createMovie(1L, "Unwatched Movie", 3600000L, isWatched = false, progressPercentage = 0f)
    val startedMovie = createMovie(2L, "Started Movie", 3600000L, isWatched = false, progressPercentage = 0.45f)
    val watchedMovie = createMovie(3L, "Watched Movie", 3600000L, isWatched = true, progressPercentage = 1f)

    val unwatchedShow = createSeries("s1", "Unwatched Show", totalEpisodes = 10, watchedEpisodes = 0)
    val inProgressShow = createSeries("s2", "In Progress Show", totalEpisodes = 10, watchedEpisodes = 4)
    val completedShow = createSeries("s3", "Completed Show", totalEpisodes = 10, watchedEpisodes = 10)

    val all = listOf(unwatchedMovie, startedMovie, watchedMovie, unwatchedShow, inProgressShow, completedShow)

    assertEquals(all, all.filter { TitleWatchFilter.ALL.matches(it) })
    assertEquals(listOf(startedMovie, inProgressShow), all.filter { TitleWatchFilter.IN_PROGRESS.matches(it) })
    assertEquals(listOf(unwatchedMovie, unwatchedShow), all.filter { TitleWatchFilter.UNWATCHED.matches(it) })
    assertEquals(listOf(watchedMovie, completedShow), all.filter { TitleWatchFilter.WATCHED.matches(it) })
  }

  @Test
  fun durationFilter_matchesShowsAndMoviesCorrectly() {
    val movie20m = createMovie(1L, "20m Movie", 20 * 60 * 1000L)
    val movie45m = createMovie(2L, "45m Movie", 45 * 60 * 1000L)
    val movie90m = createMovie(3L, "90m Movie", 90 * 60 * 1000L)

    val showShort = createSeries("s1", "Short Show", totalEpisodes = 1, totalDurationMs = 20 * 60 * 1000L)
    val showLong = createSeries("s2", "Long Show", totalEpisodes = 10, totalDurationMs = 450 * 60 * 1000L)

    val all = listOf(movie20m, movie45m, movie90m, showShort, showLong)

    assertEquals(all, all.filter { MovieDurationFilter.ALL.matches(it) })
    assertEquals(listOf(movie45m, movie90m, showLong), all.filter { MovieDurationFilter.AT_LEAST_30M.matches(it) })
    assertEquals(listOf(movie90m, showLong), all.filter { MovieDurationFilter.AT_LEAST_1H.matches(it) })
    assertEquals(listOf(movie90m, showLong), all.filter { MovieDurationFilter.AT_LEAST_1H30M.matches(it) })
  }
}
