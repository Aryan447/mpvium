package app.aryan447.mpvium.domain.audio

import android.net.Uri
import app.aryan447.mpvium.domain.media.model.LocalAlbum
import app.aryan447.mpvium.domain.media.model.LocalAudioArtist
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import app.aryan447.mpvium.utils.storage.FileTypeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioModelsTest {

  @Test
  fun localAlbum_idDerivedFromAlbumAndArtist_neverTitleAlone() {
    val albumTitle = "Greatest Hits"
    val artistA = "Queen"
    val artistB = "ABBA"

    val idA = LocalAlbum.createId(album = albumTitle, artist = artistA)
    val idB = LocalAlbum.createId(album = albumTitle, artist = artistB)

    // Even though album title is identical, different artists must produce different IDs
    assertNotEquals("Albums with same title by different artists must have distinct IDs", idA, idB)
    assertTrue("Album ID must contain artist", idA.contains("queen"))
    assertTrue("Album ID must contain album title", idA.contains("greatest hits"))

    // Case and whitespace trimming normalization
    val idNorm = LocalAlbum.createId("  Greatest Hits  ", "QUEEN  ")
    assertEquals(idA, idNorm)
  }

  @Test
  fun localAudioTrack_albumKeyMatchesLocalAlbumId() {
    val track = LocalAudioTrack(
      id = 101L,
      title = "Bohemian Rhapsody",
      displayName = "Bohemian Rhapsody.mp3",
      artist = "Queen",
      album = "A Night at the Opera",
      uri = Uri.parse("content://media/external/audio/media/101"),
    )

    val expectedAlbumId = LocalAlbum.createId("A Night at the Opera", "Queen")
    assertEquals(expectedAlbumId, track.albumKey)
  }

  @Test
  fun localAudioArtist_idNormalized() {
    val id1 = LocalAudioArtist.createId("The Beatles")
    val id2 = LocalAudioArtist.createId("  the beatles  ")
    assertEquals(id1, id2)
  }

  @Test
  fun fileTypeUtils_audioExtensionsSeparateFromVideoExtensions() {
    // Audio extensions must be recognized
    assertTrue(FileTypeUtils.isAudioFileName("song.mp3"))
    assertTrue(FileTypeUtils.isAudioFileName("track.flac"))
    assertTrue(FileTypeUtils.isAudioFileName("audio.wav"))
    assertTrue(FileTypeUtils.isAudioFileName("music.m4a"))
    assertTrue(FileTypeUtils.isAudioFileName("sound.opus"))

    // Audio files must NOT be reported as video files (prevents polluting video folders/tabs)
    assertFalse(FileTypeUtils.VIDEO_EXTENSIONS.contains("mp3"))
    assertFalse(FileTypeUtils.VIDEO_EXTENSIONS.contains("flac"))
    assertFalse(FileTypeUtils.VIDEO_EXTENSIONS.contains("wav"))
    assertFalse(FileTypeUtils.VIDEO_EXTENSIONS.contains("m4a"))
    assertFalse(FileTypeUtils.isVideoFile(File("/storage/song.mp3")))
  }
}
