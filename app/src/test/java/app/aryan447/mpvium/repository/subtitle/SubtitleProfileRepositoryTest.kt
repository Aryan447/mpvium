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
import java.io.File

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
    assertEquals(SubtitleProfileRepository.SHARED_MOVIE_KEY, movieKey)
    assertNotEquals(showKey, movieKey)
  }

  @Test
  fun allStandaloneMovies_shareCommonMovieProfileKey() {
    val movie1Key = SubtitleProfileRepository.resolveKey("The.Dark.Knight.2008.BluRay.mkv")
    val movie2Key = SubtitleProfileRepository.resolveKey("Inception.mkv")

    assertEquals(SubtitleProfileRepository.SHARED_MOVIE_KEY, movie1Key)
    assertEquals(SubtitleProfileRepository.SHARED_MOVIE_KEY, movie2Key)
    assertEquals(movie1Key, movie2Key)
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
  fun movieProfile_sharedAcrossDifferentMovies() = runBlocking {
    val cacheFile = File(tempFolder.root, "test_profiles_movies.json")
    val repo = createRepository(cacheFile)

    val movie1Key = SubtitleProfileRepository.resolveKey("The.Dark.Knight.2008.mkv")
    val movie2Key = SubtitleProfileRepository.resolveKey("Inception.2010.mkv")

    repo.saveProfile(movie1Key, subPos = 92, subScale = 1.15f)

    // Movie 2 should see the profile saved in Movie 1
    val profileForMovie2 = repo.getProfile(movie2Key)
    assertNotNull(profileForMovie2)
    assertEquals(92, profileForMovie2!!.subPos)
    assertEquals(1.15f, profileForMovie2.subScale, 0.001f)
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
    repo.saveProfile(SubtitleProfileRepository.SHARED_MOVIE_KEY, subPos = 90, subScale = 1.0f)

    assertEquals(2, repo.getAllProfiles().size)

    repo.clearAllProfiles()

    assertEquals(0, repo.getAllProfiles().size)
    assertNull(repo.getProfile("series_show1"))
    assertNull(repo.getProfile(SubtitleProfileRepository.SHARED_MOVIE_KEY))
  }

  @Test
  fun pruneStale_evictsDeletedMediaAndPreservesRemaining() = runBlocking {
    val cacheFile = File(tempFolder.root, "test_profiles_prune.json")
    val repo = createRepository(cacheFile)

    val showKeepKey = SubtitleProfileRepository.seriesKeyFor("showkeep")
    val showDeleteKey = SubtitleProfileRepository.seriesKeyFor("showdelete")
    val movieKey = SubtitleProfileRepository.SHARED_MOVIE_KEY

    repo.saveProfile(showKeepKey, subPos = 88, subScale = 1.1f)
    repo.saveProfile(showDeleteKey, subPos = 80, subScale = 0.9f)
    repo.saveProfile(movieKey, subPos = 92, subScale = 1.0f)

    assertEquals(3, repo.getAllProfiles().size)

    // Run prune where only showkeep and at least one movie are present in library
    repo.pruneStale(
      presentSeriesIds = setOf("showkeep"),
      presentMovieKeys = setOf("movie_key_1"),
    )

    val remaining = repo.getAllProfiles()
    assertEquals(2, remaining.size)
    assertNotNull(repo.getProfile(showKeepKey))
    assertNotNull(repo.getProfile(movieKey))
    assertNull(repo.getProfile(showDeleteKey))

    // Prune when all movies are deleted
    repo.pruneStale(
      presentSeriesIds = setOf("showkeep"),
      presentMovieKeys = emptySet(),
    )
    val afterMoviesDeleted = repo.getAllProfiles()
    assertEquals(1, afterMoviesDeleted.size)
    assertNotNull(repo.getProfile(showKeepKey))
    assertNull(repo.getProfile(movieKey))
  }
}
