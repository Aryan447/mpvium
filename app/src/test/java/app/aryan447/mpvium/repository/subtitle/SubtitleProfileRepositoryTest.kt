package app.aryan447.mpvium.repository.subtitle

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SubtitleProfileRepositoryTest {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  private fun createRepository(file: File): SubtitleProfileRepository {
    return SubtitleProfileRepository(
      json = json,
      cacheFile = file,
    )
  }

  @Test
  fun twoEpisodesOfSameShow_shareResolutionKey() {
    val ep1Key = SubtitleProfileRepository.resolveKey("Breaking.Bad.S01E01.720p.mkv")
    val ep2Key = SubtitleProfileRepository.resolveKey("Breaking.Bad.S02E05.1080p.mkv")

    assertEquals(ep1Key, ep2Key)
    assertEquals("series_breakingbad", ep1Key)
  }

  @Test
  fun secondShow_doesNotShareKeyWithFirstShow() {
    val show1Key = SubtitleProfileRepository.resolveKey("Breaking.Bad.S01E01.mkv")
    val show2Key = SubtitleProfileRepository.resolveKey("Better.Call.Saul.S01E01.mkv")

    assertEquals("series_breakingbad", show1Key)
    assertEquals("series_bettercallsaul", show2Key)
    assertNotEquals(show1Key, show2Key)
  }

  @Test
  fun moviesDoNotInheritShowProfile() {
    val showKey = SubtitleProfileRepository.resolveKey("Fargo.S01E01.mkv")
    val movieKey = SubtitleProfileRepository.resolveKey("Fargo.1996.1080p.mkv")

    assertEquals("series_fargo", showKey)
    assertEquals("movie_fargo@1996", movieKey)
    assertNotEquals(showKey, movieKey)
  }

  @Test
  fun standaloneMovie_resolvesNormalizedTitleAndYear() {
    val keyWithYear = SubtitleProfileRepository.resolveKey("The.Dark.Knight.2008.BluRay.mkv")
    val keyWithoutYear = SubtitleProfileRepository.resolveKey("Inception.mkv")

    assertEquals("movie_thedarkknight@2008", keyWithYear)
    assertEquals("movie_inception", keyWithoutYear)
    assertNotEquals(keyWithYear, keyWithoutYear)
  }

  @Test
  fun folderFallback_resolvesSeriesFromSeasonAndShowFolder() {
    val key = SubtitleProfileRepository.resolveKey(
      mediaTitle = "01.mkv",
      filePath = "/storage/emulated/0/TV/Breaking Bad/Season 1/01.mkv",
    )
    assertEquals("series_breakingbad", key)
  }

  @Test
  fun saveAndGetProfile_survivesRestart() = runBlocking {
    val cacheFile = File(tempFolder.root, "test_profiles.json")
    val repo1 = createRepository(cacheFile)

    val key = SubtitleProfileRepository.resolveKey("Breaking.Bad.S01E01.mkv")
    repo1.saveProfile(key, subPos = 85, subScale = 1.25f)

    val profile1 = repo1.getProfile(key)
    assertNotNull(profile1)
    assertEquals(85, profile1!!.subPos)
    assertEquals(1.25f, profile1.subScale, 0.001f)

    // Simulate restart with a fresh repository instance pointing to the same file
    val repo2 = createRepository(cacheFile)
    val secondEpisodeKey = SubtitleProfileRepository.resolveKey("Breaking.Bad.S01E02.mkv")
    val profile2 = repo2.getProfile(secondEpisodeKey)

    assertNotNull(profile2)
    assertEquals(85, profile2!!.subPos)
    assertEquals(1.25f, profile2.subScale, 0.001f)
  }

  @Test
  fun movieProfiles_areIndependentPerMovie() = runBlocking {
    val cacheFile = File(tempFolder.root, "test_profiles_movies.json")
    val repo = createRepository(cacheFile)

    val movie1Key = SubtitleProfileRepository.resolveKey("The.Dark.Knight.2008.mkv")
    val movie2Key = SubtitleProfileRepository.resolveKey("Inception.2010.mkv")

    repo.saveProfile(movie1Key, subPos = 92, subScale = 1.15f)
    repo.saveProfile(movie2Key, subPos = 85, subScale = 1.0f)

    val profile1 = repo.getProfile(movie1Key)
    val profile2 = repo.getProfile(movie2Key)
    assertNotNull(profile1)
    assertNotNull(profile2)
    assertEquals(92, profile1!!.subPos)
    assertEquals(85, profile2!!.subPos)
  }

  @Test
  fun multipleMovies_retainSeparatePositionsWithoutResetting() = runBlocking {
    val cacheFile = File(tempFolder.root, "test_profiles_multiple_movies.json")
    val repo = createRepository(cacheFile)

    val movie1Key = SubtitleProfileRepository.resolveKey("Movie.One.2021.mkv")
    val movie2Key = SubtitleProfileRepository.resolveKey("Movie.Two.2022.mkv")
    val movie3Key = SubtitleProfileRepository.resolveKey("Movie.Three.2023.mkv")
    val movie4Key = SubtitleProfileRepository.resolveKey("Movie.Four.2024.mkv")
    val movie5Key = SubtitleProfileRepository.resolveKey("Movie.Five.2025.mkv")

    repo.saveProfile(movie1Key, subPos = 70, subScale = 1.0f)
    repo.saveProfile(movie2Key, subPos = 75, subScale = 1.1f)
    repo.saveProfile(movie3Key, subPos = 80, subScale = 1.2f)
    repo.saveProfile(movie4Key, subPos = 85, subScale = 1.3f)
    repo.saveProfile(movie5Key, subPos = 90, subScale = 1.4f)

    // Reopen movie 1: its position should still be 70
    assertEquals(70, repo.getProfile(movie1Key)?.subPos)
    assertEquals(75, repo.getProfile(movie2Key)?.subPos)
    assertEquals(80, repo.getProfile(movie3Key)?.subPos)
    assertEquals(85, repo.getProfile(movie4Key)?.subPos)
    assertEquals(90, repo.getProfile(movie5Key)?.subPos)
  }

  @Test
  fun deleteProfile_removesProfile() = runBlocking {
    val cacheFile = File(tempFolder.root, "test_profiles_delete.json")
    val repo = createRepository(cacheFile)

    val showKey = SubtitleProfileRepository.resolveKey("Show.A.S01E01.mkv")
    val movieKey = SubtitleProfileRepository.resolveKey("Movie.B.2020.mkv")

    repo.saveProfile(showKey, subPos = 90, subScale = 1.1f)
    repo.saveProfile(movieKey, subPos = 95, subScale = 1.0f)

    assertNotNull(repo.getProfile(showKey))
    assertNotNull(repo.getProfile(movieKey))

    repo.deleteProfile(showKey)

    assertNull(repo.getProfile(showKey))
    assertNotNull(repo.getProfile(movieKey))
  }

  @Test
  fun clearAllProfiles_removesAllEntries() = runBlocking {
    val cacheFile = File(tempFolder.root, "test_profiles_clear.json")
    val repo = createRepository(cacheFile)

    repo.saveProfile("series_show1", subPos = 80, subScale = 1.2f)
    repo.saveProfile("movie_film1", subPos = 90, subScale = 1.0f)

    assertEquals(2, repo.getAllProfiles().size)

    repo.clearAllProfiles()

    assertEquals(0, repo.getAllProfiles().size)
    assertNull(repo.getProfile("series_show1"))
    assertNull(repo.getProfile("movie_film1"))
  }

  @Test
  fun pruneStale_evictsDeletedMediaAndPreservesRemaining() = runBlocking {
    val cacheFile = File(tempFolder.root, "test_profiles_prune.json")
    val repo = createRepository(cacheFile)

    val showKeepKey = SubtitleProfileRepository.seriesKeyFor("showkeep")
    val showDeleteKey = SubtitleProfileRepository.seriesKeyFor("showdelete")
    val movieKeepKey = SubtitleProfileRepository.movieKeyFor("Movie Keep", "2021")
    val movieDeleteKey = SubtitleProfileRepository.movieKeyFor("Movie Delete", "2019")

    repo.saveProfile(showKeepKey, subPos = 88, subScale = 1.1f)
    repo.saveProfile(showDeleteKey, subPos = 80, subScale = 0.9f)
    repo.saveProfile(movieKeepKey, subPos = 92, subScale = 1.0f)
    repo.saveProfile(movieDeleteKey, subPos = 94, subScale = 1.0f)

    assertEquals(4, repo.getAllProfiles().size)

    // Run prune where only showkeep and movieKeep are present in library
    repo.pruneStale(
      presentSeriesIds = setOf("showkeep"),
      presentMovieKeys = setOf(movieKeepKey),
    )

    val remaining = repo.getAllProfiles()
    assertEquals(2, remaining.size)
    assertNotNull(repo.getProfile(showKeepKey))
    assertNotNull(repo.getProfile(movieKeepKey))
    assertNull(repo.getProfile(showDeleteKey))
    assertNull(repo.getProfile(movieDeleteKey))

    // Verify pruned state persists to disk
    val repoRestarted = createRepository(cacheFile)
    assertEquals(2, repoRestarted.getAllProfiles().size)
    assertNotNull(repoRestarted.getProfile(showKeepKey))
    assertNull(repoRestarted.getProfile(showDeleteKey))
  }

  @Test
  fun deleteProfileForVideo_deletesMovieProfileImmediately() = runBlocking {
    val cacheFile = File(tempFolder.root, "test_profiles_delete_video.json")
    val repo = createRepository(cacheFile)

    val movieFile = "Gladiator.2000.mkv"
    val moviePath = "/storage/emulated/0/Movies/Gladiator.2000.mkv"
    val key = SubtitleProfileRepository.resolveKey(movieFile, moviePath)

    repo.saveProfile(key, subPos = 82, subScale = 1.1f)
    assertNotNull(repo.getProfile(key))

    repo.deleteProfileForVideo(movieFile, moviePath)
    assertNull(repo.getProfile(key))
  }

  @Test
  fun deleteProfileForVideo_preservesSeriesProfileWhenSingleEpisodeDeleted() = runBlocking {
    val cacheFile = File(tempFolder.root, "test_profiles_preserve_series.json")
    val repo = createRepository(cacheFile)

    val epFile = "Breaking.Bad.S01E01.mkv"
    val epPath = "/storage/emulated/0/TV/Breaking Bad/Season 1/01.mkv"
    val seriesKey = SubtitleProfileRepository.resolveKey(epFile, epPath)

    repo.saveProfile(seriesKey, subPos = 85, subScale = 1.2f)
    assertNotNull(repo.getProfile(seriesKey))

    // Deleting a single episode file should NOT delete the series profile
    repo.deleteProfileForVideo(epFile, epPath)
    assertNotNull(repo.getProfile(seriesKey))
  }

  @Test
  fun onVideoRenamed_migratesMovieProfile() = runBlocking {
    val cacheFile = File(tempFolder.root, "test_profiles_rename.json")
    val repo = createRepository(cacheFile)

    val oldTitle = "Interstellar.mkv"
    val oldPath = "/storage/emulated/0/Movies/Interstellar.mkv"
    val newTitle = "Interstellar.2014.1080p.mkv"
    val newPath = "/storage/emulated/0/Movies/Interstellar.2014.1080p.mkv"

    val oldKey = SubtitleProfileRepository.resolveKey(oldTitle, oldPath)
    val newKey = SubtitleProfileRepository.resolveKey(newTitle, newPath)

    repo.saveProfile(oldKey, subPos = 78, subScale = 1.25f)
    assertEquals(78, repo.getProfile(oldKey)?.subPos)

    repo.onVideoRenamed(oldTitle, oldPath, newTitle, newPath)

    assertNull(repo.getProfile(oldKey))
    val migrated = repo.getProfile(newKey)
    assertNotNull(migrated)
    assertEquals(78, migrated?.subPos)
  }
}
