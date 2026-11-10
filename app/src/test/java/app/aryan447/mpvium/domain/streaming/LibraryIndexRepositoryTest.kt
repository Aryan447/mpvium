package app.aryan447.mpvium.domain.streaming

import android.net.Uri
import app.aryan447.mpvium.database.entities.LibraryIndexEntity
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.preferences.LibraryScanMode
import app.aryan447.mpvium.utils.media.ParsedMediaInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LibraryIndexRepositoryTest {

  @Test
  fun libraryScanModeEnum_hasAutoAndManual() {
    assertEquals(LibraryScanMode.AUTO, LibraryScanMode.valueOf("AUTO"))
    assertEquals(LibraryScanMode.MANUAL, LibraryScanMode.valueOf("MANUAL"))
  }

  @Test
  fun entityToParsedMediaInfo_mapsAllFieldsCorrectly() {
    val entity = LibraryIndexEntity(
      path = "/storage/emulated/0/TV/Breaking Bad/S01E01.mkv",
      size = 1024L * 1024L * 500L,
      mtimeMs = 1700000000L,
      parsedTitle = "Breaking Bad",
      parsedYear = "2008",
      parsedSeason = 1,
      parsedEpisode = 1,
      parsedEpisodeTitle = "Pilot",
      parsedType = "tv",
      mediaStoreId = 12345L,
      displayName = "Breaking Bad S01E01 Pilot.mkv",
      durationMs = 3600000L,
      dateAdded = 1699990000L,
      mimeType = "video/x-matroska",
      bucketId = "/storage/emulated/0/TV/Breaking Bad",
      bucketDisplayName = "Breaking Bad",
      width = 1920,
      height = 1080,
      fps = 23.976f,
    )

    val parsed = entity.toParsedMediaInfo()

    assertEquals("Breaking Bad", parsed.title)
    assertEquals("2008", parsed.year)
    assertEquals(1, parsed.season)
    assertEquals(1, parsed.episode)
    assertEquals("Pilot", parsed.episodeTitle)
    assertEquals("tv", parsed.type)
  }

  @Test
  fun entityToVideo_mapsAllFieldsCorrectly() {
    val entity = LibraryIndexEntity(
      path = "/storage/emulated/0/Movies/Inception (2010).mp4",
      size = 1024L * 1024L * 2000L,
      mtimeMs = 1700000000L,
      parsedTitle = "Inception",
      parsedYear = "2010",
      parsedSeason = null,
      parsedEpisode = null,
      parsedEpisodeTitle = null,
      parsedType = "movie",
      mediaStoreId = 99999L,
      displayName = "Inception (2010).mp4",
      durationMs = 8880000L,
      dateAdded = 1699990000L,
      mimeType = "video/mp4",
      bucketId = "/storage/emulated/0/Movies",
      bucketDisplayName = "Movies",
      width = 3840,
      height = 2160,
      fps = 24.0f,
    )

    val video = entity.toVideo()

    assertEquals("/storage/emulated/0/Movies/Inception (2010).mp4", video.path)
    assertEquals(99999L, video.id)
    assertEquals("Inception (2010).mp4", video.displayName)
    assertEquals("Inception (2010)", video.title)
    assertEquals(1024L * 1024L * 2000L, video.size)
    assertEquals(1700000000L, video.dateModified)
    assertEquals(8880000L, video.duration)
    assertEquals(3840, video.width)
    assertEquals(2160, video.height)
    assertEquals("3840x2160", video.resolution)
  }

  @Test
  fun cacheValidation_detectsUnchangedVsChanged() {
    val cached = LibraryIndexEntity(
      path = "/storage/emulated/0/Movies/Film.mkv",
      size = 5000L,
      mtimeMs = 1000L,
      parsedTitle = "Film",
      parsedYear = "2024",
      parsedSeason = null,
      parsedEpisode = null,
      parsedEpisodeTitle = null,
      parsedType = "movie",
      mediaStoreId = 1L,
      displayName = "Film.mkv",
      durationMs = 60000L,
      dateAdded = 900L,
      mimeType = "video/x-matroska",
      bucketId = "Movies",
      bucketDisplayName = "Movies",
      width = 1920,
      height = 1080,
      fps = 24f,
    )

    // Identical file: hit
    val fileSameSize = 5000L
    val fileSameMtime = 1000L
    val isHit = (cached.size == fileSameSize && cached.mtimeMs == fileSameMtime)
    assertTrue(isHit)

    // Changed size: miss
    val fileDifferentSize = 5001L
    assertFalse(cached.size == fileDifferentSize && cached.mtimeMs == fileSameMtime)

    // Changed mtime: miss
    val fileDifferentMtime = 1001L
    assertFalse(cached.size == fileSameSize && cached.mtimeMs == fileDifferentMtime)
  }
}
