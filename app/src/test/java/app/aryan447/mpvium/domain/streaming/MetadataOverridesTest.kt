package app.aryan447.mpvium.domain.streaming

import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataOverridesTest {
  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  private val sampleVideo = Video(
    id = 42L,
    title = "Inception.2010.1080p.mkv",
    path = "/storage/emulated/0/Movies/Inception.2010.1080p.mkv",
    duration = 8880000L,
    size = 2000000000L,
    dateAdded = 1600000000L,
    dateModified = 1600000000L,
  )

  @Test
  fun cachedMediaMetadata_serializationWithCustomOverrides() {
    val original = CachedMediaMetadata(
      tmdbId = 27205,
      title = "Inception",
      posterUrl = "https://image.tmdb.org/t/p/w500/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg",
      year = "2010",
      manuallySelected = false,
      customTitle = "My Inception Director's Cut",
      customYear = "2011",
      customPosterPath = "/data/user/0/app.aryan447.mpvium/files/streaming_art/custom_art.img",
    )

    val encoded = json.encodeToString(original)
    val decoded = json.decodeFromString<CachedMediaMetadata>(encoded)

    assertEquals(original.tmdbId, decoded.tmdbId)
    assertEquals(original.title, decoded.title)
    assertEquals(original.customTitle, decoded.customTitle)
    assertEquals(original.customYear, decoded.customYear)
    assertEquals(original.customPosterPath, decoded.customPosterPath)
    assertTrue(decoded.hasCustomOverrides)
  }

  @Test
  fun cachedMediaMetadata_backwardCompatibilityWithoutOverrides() {
    // Legacy JSON without customTitle, customYear, customPosterPath
    val legacyJson = """
      {
        "tmdbId": 27205,
        "title": "Inception",
        "posterUrl": "https://image.tmdb.org/t/p/w500/test.jpg",
        "year": "2010",
        "manuallySelected": false
      }
    """.trimIndent()

    val decoded = json.decodeFromString<CachedMediaMetadata>(legacyJson)
    assertEquals(27205, decoded.tmdbId)
    assertEquals("Inception", decoded.title)
    assertNull(decoded.customTitle)
    assertNull(decoded.customYear)
    assertNull(decoded.customPosterPath)
    assertFalse(decoded.hasCustomOverrides)
  }

  @Test
  fun migration_preservesManuallySelectedAndCustomOverrides() {
    val entries = mapOf(
      "movie_auto" to CachedMediaMetadata(
        title = "Auto Match",
        manuallySelected = false,
      ),
      "movie_manual" to CachedMediaMetadata(
        title = "Manually Matched",
        manuallySelected = true,
      ),
      "movie_override_title" to CachedMediaMetadata(
        title = "Auto Title",
        manuallySelected = false,
        customTitle = "Custom Title",
      ),
      "movie_override_poster" to CachedMediaMetadata(
        title = "Auto Title",
        manuallySelected = false,
        customPosterPath = "/path/to/poster.img",
      ),
      "movie_override_year" to CachedMediaMetadata(
        title = "Auto Title",
        manuallySelected = false,
        customYear = "1999",
      ),
    )

    val migrated = entries.filterValues { it.manuallySelected || it.hasCustomOverrides }

    assertEquals(4, migrated.size)
    assertFalse(migrated.containsKey("movie_auto"))
    assertTrue(migrated.containsKey("movie_manual"))
    assertTrue(migrated.containsKey("movie_override_title"))
    assertTrue(migrated.containsKey("movie_override_poster"))
    assertTrue(migrated.containsKey("movie_override_year"))
  }

  @Test
  fun resolutionOrder_customOverrideWinsOverCachedTmdbAndFilename() {
    val filenameTitle = "inception.2010.mkv"
    val filenameYear = "2009"
    val movie = LocalMovie(
      video = sampleVideo,
      title = filenameTitle,
      year = filenameYear,
      posterUrl = null,
    )

    val cachedWithCustom = CachedMediaMetadata(
      title = "Inception",
      year = "2010",
      posterUrl = "https://image.tmdb.org/t/p/w500/tmdb.jpg",
      customTitle = "Inception: Special Edition",
      customYear = "2015",
      customPosterPath = "/storage/custom.img",
    )

    // Resolution order: custom override -> cached TMDb -> filename parse
    val resolvedTitle = cachedWithCustom.customTitle?.trim()?.takeIf { it.isNotBlank() }
      ?: cachedWithCustom.title.trim().takeIf { it.isNotBlank() }
      ?: movie.title

    val resolvedYear = cachedWithCustom.customYear?.trim()?.takeIf { it.isNotBlank() }
      ?: cachedWithCustom.year?.trim()?.takeIf { it.isNotBlank() }
      ?: movie.year

    val resolvedPoster = cachedWithCustom.customPosterPath?.trim()?.takeIf { it.isNotBlank() }
      ?: cachedWithCustom.posterUrl?.trim()?.takeIf { it.isNotBlank() }
      ?: movie.posterUrl

    assertEquals("Inception: Special Edition", resolvedTitle)
    assertEquals("2015", resolvedYear)
    assertEquals("/storage/custom.img", resolvedPoster)
  }

  @Test
  fun resolutionOrder_cachedTmdbWinsWhenCustomOverrideAbsent() {
    val filenameTitle = "inception.2010.mkv"
    val filenameYear = "2009"
    val movie = LocalMovie(
      video = sampleVideo,
      title = filenameTitle,
      year = filenameYear,
      posterUrl = null,
    )

    val cachedWithoutCustom = CachedMediaMetadata(
      title = "Inception",
      year = "2010",
      posterUrl = "https://image.tmdb.org/t/p/w500/tmdb.jpg",
      customTitle = null,
      customYear = null,
      customPosterPath = null,
    )

    val resolvedTitle = cachedWithoutCustom.customTitle?.trim()?.takeIf { it.isNotBlank() }
      ?: cachedWithoutCustom.title.trim().takeIf { it.isNotBlank() }
      ?: movie.title

    val resolvedYear = cachedWithoutCustom.customYear?.trim()?.takeIf { it.isNotBlank() }
      ?: cachedWithoutCustom.year?.trim()?.takeIf { it.isNotBlank() }
      ?: movie.year

    val resolvedPoster = cachedWithoutCustom.customPosterPath?.trim()?.takeIf { it.isNotBlank() }
      ?: cachedWithoutCustom.posterUrl?.trim()?.takeIf { it.isNotBlank() }
      ?: movie.posterUrl

    assertEquals("Inception", resolvedTitle)
    assertEquals("2010", resolvedYear)
    assertEquals("https://image.tmdb.org/t/p/w500/tmdb.jpg", resolvedPoster)
  }

  @Test
  fun resolutionOrder_filenameWinsWhenNoCachedMetadata() {
    val filenameTitle = "inception.2010.mkv"
    val filenameYear = "2009"
    val movie = LocalMovie(
      video = sampleVideo,
      title = filenameTitle,
      year = filenameYear,
      posterUrl = null,
    )

    val emptyMetadata = CachedMediaMetadata(
      title = "",
      year = null,
      posterUrl = null,
    )

    val resolvedTitle = emptyMetadata.customTitle?.trim()?.takeIf { it.isNotBlank() }
      ?: emptyMetadata.title.trim().takeIf { it.isNotBlank() }
      ?: movie.title

    val resolvedYear = emptyMetadata.customYear?.trim()?.takeIf { it.isNotBlank() }
      ?: emptyMetadata.year?.trim()?.takeIf { it.isNotBlank() }
      ?: movie.year

    val resolvedPoster = emptyMetadata.customPosterPath?.trim()?.takeIf { it.isNotBlank() }
      ?: emptyMetadata.posterUrl?.trim()?.takeIf { it.isNotBlank() }
      ?: movie.posterUrl

    assertEquals(filenameTitle, resolvedTitle)
    assertEquals(filenameYear, resolvedYear)
    assertNull(resolvedPoster)
  }

  @Test
  fun forcedRefresh_preservesCustomOverrides() {
    val previousCached = CachedMediaMetadata(
      tmdbId = 100,
      title = "Old TMDb Title",
      year = "2010",
      posterUrl = "https://image.tmdb.org/t/p/w500/old.jpg",
      customTitle = "My Preserved Custom Title",
      customYear = "2024",
      customPosterPath = "/local/artwork.img",
    )

    // Simulating refreshed TMDb match
    val refreshedTmdbTitle = "Refreshed TMDb Title"
    val refreshedTmdbPoster = "https://image.tmdb.org/t/p/w500/new.jpg"
    val refreshedTmdbYear = "2010"

    val mergedAfterRefresh = CachedMediaMetadata(
      tmdbId = 200,
      title = refreshedTmdbTitle,
      posterUrl = refreshedTmdbPoster,
      year = refreshedTmdbYear,
      customTitle = previousCached.customTitle,
      customYear = previousCached.customYear,
      customPosterPath = previousCached.customPosterPath,
    )

    assertEquals("My Preserved Custom Title", mergedAfterRefresh.customTitle)
    assertEquals("2024", mergedAfterRefresh.customYear)
    assertEquals("/local/artwork.img", mergedAfterRefresh.customPosterPath)
    assertEquals(200, mergedAfterRefresh.tmdbId)
    assertEquals("Refreshed TMDb Title", mergedAfterRefresh.title)
  }

  @Test
  fun resetToAutomatic_clearsOverridesAndReverts() {
    val overridden = CachedMediaMetadata(
      tmdbId = 100,
      title = "TMDb Title",
      year = "2020",
      posterUrl = "https://image.tmdb.org/t/p/w500/tmdb.jpg",
      customTitle = "Custom Name",
      customYear = "2025",
      customPosterPath = "/custom/cover.img",
    )

    val reset = overridden.copy(
      customTitle = null,
      customYear = null,
      customPosterPath = null,
    )

    assertNull(reset.customTitle)
    assertNull(reset.customYear)
    assertNull(reset.customPosterPath)
    assertFalse(reset.hasCustomOverrides)
    assertEquals("TMDb Title", reset.title)
    assertEquals("2020", reset.year)
    assertEquals("https://image.tmdb.org/t/p/w500/tmdb.jpg", reset.posterUrl)
  }
}
