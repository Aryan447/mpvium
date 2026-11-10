package app.aryan447.mpvium.domain.streaming

import app.aryan447.mpvium.database.entities.CustomSeriesEntity
import app.aryan447.mpvium.database.entities.CustomSeriesItemEntity
import app.aryan447.mpvium.database.repository.CustomSeriesItemDraft
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.streaming.model.LocalEpisode
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomSeriesTest {

  @Test
  fun customSeries_deterministicIdFormat() {
    val seriesId = 42
    val deterministicId = "custom:$seriesId"
    assertTrue(deterministicId.startsWith("custom:"))
    assertEquals(seriesId, deterministicId.removePrefix("custom:").toIntOrNull())
  }

  @Test
  fun customSeriesEntity_propertiesAndDefaults() {
    val now = 1700000000000L
    val entity = CustomSeriesEntity(
      id = 1,
      title = "My Curated Show",
      year = "2024",
      posterPath = "/data/user/0/app.aryan447.mpvium/files/streaming_art/custom_art.img",
      tmdbId = 12345,
      createdAt = now,
      updatedAt = now,
    )

    assertEquals(1, entity.id)
    assertEquals("My Curated Show", entity.title)
    assertEquals("2024", entity.year)
    assertEquals("/data/user/0/app.aryan447.mpvium/files/streaming_art/custom_art.img", entity.posterPath)
    assertEquals(12345, entity.tmdbId)
    assertEquals(now, entity.createdAt)
    assertEquals(now, entity.updatedAt)
  }

  @Test
  fun customSeriesItemEntity_properties() {
    val item = CustomSeriesItemEntity(
      id = 10,
      seriesId = 1,
      filePath = "/storage/emulated/0/Videos/EP01.mkv",
      fileName = "EP01.mkv",
      season = 1,
      episode = 1,
      position = 0,
    )

    assertEquals(10, item.id)
    assertEquals(1, item.seriesId)
    assertEquals("/storage/emulated/0/Videos/EP01.mkv", item.filePath)
    assertEquals("EP01.mkv", item.fileName)
    assertEquals(1, item.season)
    assertEquals(1, item.episode)
    assertEquals(0, item.position)
  }

  @Test
  fun claimedFilesExclusion_preventsVideosFromBeingGroupedTwice() {
    val claimedPath = "/storage/emulated/0/Videos/ShowA.mkv"
    val unclaimedPath = "/storage/emulated/0/Videos/ShowB.mkv"

    val customItems = listOf(
      CustomSeriesItemEntity(
        id = 1,
        seriesId = 99,
        filePath = claimedPath,
        fileName = "ShowA.mkv",
        season = 1,
        episode = 1,
        position = 0,
      )
    )

    val allVideos = listOf(
      Video(id = 1L, title = "ShowA", path = claimedPath, duration = 3000L, size = 100L, dateAdded = 0L, dateModified = 0L, displayName = "ShowA.mkv"),
      Video(id = 2L, title = "ShowB", path = unclaimedPath, duration = 4000L, size = 200L, dateAdded = 0L, dateModified = 0L, displayName = "ShowB.mkv"),
    )

    val claimedPaths = customItems.mapTo(HashSet()) { it.filePath }
    val scannerCandidates = allVideos.filter { it.path !in claimedPaths }

    assertEquals(1, scannerCandidates.size)
    assertEquals(unclaimedPath, scannerCandidates.first().path)
    assertFalse(scannerCandidates.any { it.path == claimedPath })
  }

  @Test
  fun customSeriesMapping_constructsSeasonsAndEpisodesCorrectly() {
    val video1 = Video(id = 101L, title = "V1", path = "/path/1.mkv", duration = 120_000L, size = 100L, dateAdded = 0L, dateModified = 0L, displayName = "1.mkv")
    val video2 = Video(id = 102L, title = "V2", path = "/path/2.mkv", duration = 180_000L, size = 100L, dateAdded = 0L, dateModified = 0L, displayName = "2.mkv")
    val video3 = Video(id = 103L, title = "V3", path = "/path/3.mkv", duration = 200_000L, size = 100L, dateAdded = 0L, dateModified = 0L, displayName = "3.mkv")

    val episodes = listOf(
      LocalEpisode(video = video1, seasonNumber = 1, episodeNumber = 1, isWatched = true, playbackPositionMs = 120_000L),
      LocalEpisode(video = video2, seasonNumber = 1, episodeNumber = 2, isWatched = false, playbackPositionMs = 50_000L),
      LocalEpisode(video = video3, seasonNumber = 2, episodeNumber = 1, isWatched = false, playbackPositionMs = 0L),
    )

    val seasonsMap = episodes
      .groupBy { it.seasonNumber }
      .mapValues { (_, eps) -> eps.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber })) }
      .toSortedMap()

    assertEquals(2, seasonsMap.size)
    assertEquals(2, seasonsMap[1]?.size)
    assertEquals(1, seasonsMap[2]?.size)

    val allEpisodes = seasonsMap.values.flatten()
    val totalEpisodes = allEpisodes.size
    val totalDuration = allEpisodes.sumOf { it.video.duration }
    val watchedCount = allEpisodes.count { it.isWatched }

    assertEquals(3, totalEpisodes)
    assertEquals(500_000L, totalDuration)
    assertEquals(1, watchedCount)

    val customSeries = LocalSeries(
      id = "custom:10",
      title = "Custom Anime Selection",
      year = "2024",
      posterUrl = "/path/to/poster.img",
      totalEpisodes = totalEpisodes,
      totalDurationMs = totalDuration,
      watchedEpisodesCount = watchedCount,
      seasons = seasonsMap,
    )

    assertEquals("custom:10", customSeries.id)
    assertEquals("Custom Anime Selection", customSeries.title)
    assertEquals("2024", customSeries.year)
    assertEquals(2, customSeries.seasonCount)
    assertFalse(customSeries.isCompleted)
    assertEquals(1f / 3f, customSeries.progressPercentage, 0.001f)
  }

  @Test
  fun dragReorder_updatesPositionsCorrectly() {
    val items = mutableListOf(
      CustomSeriesItemDraft(filePath = "/a.mkv", fileName = "a.mkv", season = 1, episode = 1, position = 0),
      CustomSeriesItemDraft(filePath = "/b.mkv", fileName = "b.mkv", season = 1, episode = 2, position = 1),
      CustomSeriesItemDraft(filePath = "/c.mkv", fileName = "c.mkv", season = 1, episode = 3, position = 2),
    )

    // Move index 2 ("/c.mkv") to index 0
    val moved = items.removeAt(2)
    items.add(0, moved)
    val reindexed = items.mapIndexed { index, draft -> draft.copy(position = index) }

    assertEquals("/c.mkv", reindexed[0].filePath)
    assertEquals(0, reindexed[0].position)
    assertEquals("/a.mkv", reindexed[1].filePath)
    assertEquals(1, reindexed[1].position)
    assertEquals("/b.mkv", reindexed[2].filePath)
    assertEquals(2, reindexed[2].position)
  }
}
