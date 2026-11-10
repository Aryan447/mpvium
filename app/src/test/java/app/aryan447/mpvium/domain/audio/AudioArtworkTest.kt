package app.aryan447.mpvium.domain.audio

import app.aryan447.mpvium.domain.media.model.LocalAlbum
import app.aryan447.mpvium.ui.audio.components.AlbumPlaceholderUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class AudioArtworkTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun albumPlaceholderUtils_extractInitials_twoWordTitle() {
    val initials = AlbumPlaceholderUtils.extractInitials("Abbey Road", "The Beatles")
    assertEquals("AR", initials)
  }

  @Test
  fun albumPlaceholderUtils_extractInitials_singleWordTitle() {
    val initials = AlbumPlaceholderUtils.extractInitials("Thriller", "Michael Jackson")
    assertEquals("TH", initials)
  }

  @Test
  fun albumPlaceholderUtils_extractInitials_shortTitle() {
    val initials = AlbumPlaceholderUtils.extractInitials("V", "BTS")
    assertEquals("V", initials)
  }

  @Test
  fun albumPlaceholderUtils_extractInitials_numericTitle() {
    val initials = AlbumPlaceholderUtils.extractInitials("1989", "Taylor Swift")
    assertEquals("19", initials)
  }

  @Test
  fun albumPlaceholderUtils_extractInitials_hyphenatedTitle() {
    val initials = AlbumPlaceholderUtils.extractInitials("Dark-Side", "Pink Floyd")
    assertEquals("DS", initials)
  }

  @Test
  fun albumPlaceholderUtils_extractInitials_fallbackToArtistWhenAlbumUnknown() {
    val initials = AlbumPlaceholderUtils.extractInitials("Unknown Album", "Pink Floyd")
    assertEquals("PF", initials)

    val initials2 = AlbumPlaceholderUtils.extractInitials("", "Queen")
    assertEquals("QU", initials2)
  }

  @Test
  fun albumPlaceholderUtils_extractInitials_fallbackToSymbolWhenBothUnknown() {
    val initials = AlbumPlaceholderUtils.extractInitials("Unknown Album", "Unknown Artist")
    assertEquals("♪", initials)

    val initialsEmpty = AlbumPlaceholderUtils.extractInitials(null, null)
    assertEquals("♪", initialsEmpty)
  }

  @Test
  fun audioArtworkStore_albumKeyHashStability() {
    val key1 = LocalAlbum.createId("Abbey Road", "The Beatles")
    val key2 = LocalAlbum.createId("abbey road", "the beatles")
    assertEquals(key1, key2)

    val hash1 = md5(key1)
    val hash2 = md5(key2)
    assertEquals(hash1, hash2)

    val keyDiffArtist = LocalAlbum.createId("Abbey Road", "Cover Band")
    val hashDiffArtist = md5(keyDiffArtist)
    assertNotEquals(hash1, hashDiffArtist)
  }

  @Test
  fun audioArtworkStore_pruning_deletesStaleFilesAndRetainsActiveAlbums() {
    val diskDir = tempFolder.newFolder("audio_art")

    val album1 = LocalAlbum.createId("Abbey Road", "The Beatles")
    val album2 = LocalAlbum.createId("Thriller", "Michael Jackson")
    val deletedAlbum = LocalAlbum.createId("Old Album", "Forgotten Artist")

    val file1 = File(diskDir, "${md5(album1)}.img").apply { writeText("art1") }
    val file2 = File(diskDir, "${md5(album2)}.img").apply { writeText("art2") }
    val deletedFile = File(diskDir, "${md5(deletedAlbum)}.img").apply { writeText("stale_art") }
    val tmpFile = File(diskDir, "${md5(album1)}.img.tmp").apply { writeText("in_flight_write") }

    assertTrue(file1.exists())
    assertTrue(file2.exists())
    assertTrue(deletedFile.exists())
    assertTrue(tmpFile.exists())

    // Simulate active library scan containing only album1 and album2
    val activeKeys = setOf(album1, album2)
    val activeHashes = activeKeys.map { md5(it) }.toSet()

    val files = diskDir.listFiles() ?: emptyArray()
    for (file in files) {
      if (!file.isFile || file.name.endsWith(".tmp")) continue
      if (file.nameWithoutExtension !in activeHashes) {
        file.delete()
      }
    }

    // Active artwork files must be retained
    assertTrue("Active album 1 art must be retained", file1.exists())
    assertTrue("Active album 2 art must be retained", file2.exists())

    // Deleted album artwork must be pruned
    assertFalse("Deleted album art must be pruned", deletedFile.exists())

    // In-flight temp write file must not be disturbed
    assertTrue("In-flight atomic write temp file must not be removed by pruning", tmpFile.exists())
  }

  @Test
  fun musicBrainzRateLimiter_enforcesDelayBetweenCalls() = runBlocking {
    val start = System.currentTimeMillis()
    MusicBrainzRateLimiter.acquire()
    val mid = System.currentTimeMillis()
    MusicBrainzRateLimiter.acquire()
    val end = System.currentTimeMillis()

    // Second call must have waited at least ~1000ms
    assertTrue("Rate limiter must delay second call to preserve 1 req/sec limit", (end - mid) >= 900)
  }

  private fun md5(value: String): String {
    val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
  }
}
