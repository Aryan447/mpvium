package app.aryan447.mpvium.domain.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkMediaTypesTest {
  @Test
  fun `hls playlist extensions are playable`() {
    assertTrue(NetworkMediaTypes.isVideoFileName("playlist.m3u8"))
    assertTrue(NetworkMediaTypes.isVideoFileName("playlist.m3u"))
    assertTrue(NetworkMediaTypes.isPlaylistFileName("playlist.m3u8"))
    assertTrue(NetworkMediaTypes.isPlaylistFileName("playlist.m3u"))
  }

  @Test
  fun `extension lookup ignores case and query strings`() {
    assertTrue(NetworkMediaTypes.isVideoFileName("PLAYLIST.M3U8?token=abc"))
    assertTrue(NetworkMediaTypes.isPlaylistFileName("Playlist.M3U8#start"))
    assertEquals("m3u8", NetworkMediaTypes.extensionOf("Show.M3U8?token=abc"))
  }

  @Test
  fun `hls playlists keep their streaming mime types`() {
    assertEquals("application/vnd.apple.mpegurl", NetworkMediaTypes.getMimeType("playlist.m3u8"))
    assertEquals("audio/mpegurl", NetworkMediaTypes.getMimeType("playlist.m3u"))
  }

  @Test
  fun `playlist mime types are recognised case-insensitively`() {
    assertTrue(NetworkMediaTypes.isPlaylistMimeType("application/vnd.apple.mpegurl"))
    assertTrue(NetworkMediaTypes.isPlaylistMimeType("application/x-mpegURL"))
    assertTrue(NetworkMediaTypes.isPlaylistMimeType("audio/mpegurl"))
    assertFalse(NetworkMediaTypes.isPlaylistMimeType("video/mp4"))
    assertFalse(NetworkMediaTypes.isPlaylistMimeType(null))
  }

  @Test
  fun `regular video files still resolve`() {
    assertEquals("video/x-matroska", NetworkMediaTypes.getMimeType("movie.mkv"))
    assertTrue(NetworkMediaTypes.isVideoFileName("movie.mkv"))
    assertFalse(NetworkMediaTypes.isPlaylistFileName("movie.mkv"))
    assertFalse(NetworkMediaTypes.isVideoFileName("notes.txt"))
    assertNull(NetworkMediaTypes.getMimeType("notes.txt"))
  }
}
