package app.aryan447.mpvium.domain.network

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistPathResolverTest {
  @Test
  fun `webdav paths resolve relative to the playlist folder`() {
    assertEquals(
      "shows/season 1/episode 1/segment.ts",
      PlaylistPathResolver.resolve("shows/season 1/episode 1/playlist.m3u8", "segment.ts"),
    )
    assertEquals(
      "shows/season 1/episode 1/segments/segment.ts",
      PlaylistPathResolver.resolve("shows/season 1/episode 1/playlist.m3u8", "segments/segment.ts"),
    )
    assertEquals(
      "shows/season 1/segment.ts",
      PlaylistPathResolver.resolve("shows/season 1/episode 1/playlist.m3u8", "../segment.ts"),
    )
  }

  @Test
  fun `ftp paths keep their leading slash`() {
    assertEquals(
      "/media/show/segment.ts",
      PlaylistPathResolver.resolve("/media/show/playlist.m3u8", "segment.ts"),
    )
    assertEquals(
      "/segment.ts",
      PlaylistPathResolver.resolve("/playlist.m3u8", "segment.ts"),
    )
  }

  @Test
  fun `smb paths keep their scheme and share`() {
    assertEquals(
      "smb://192.168.1.10/media/show/segment.ts",
      PlaylistPathResolver.resolve("smb://192.168.1.10/media/show/playlist.m3u8", "segment.ts"),
    )
    assertEquals(
      "smb://192.168.1.10/media/show/sub/segment.ts",
      PlaylistPathResolver.resolve("smb://192.168.1.10/media/show/playlist.m3u8", "sub/segment.ts"),
    )
  }

  @Test
  fun `query strings and fragments are dropped`() {
    assertEquals(
      "show/segment.ts",
      PlaylistPathResolver.resolve("show/playlist.m3u8", "segment.ts?token=abc#frag"),
    )
  }

  @Test
  fun `paths cannot escape above the connection root`() {
    assertEquals(
      "segment.ts",
      PlaylistPathResolver.resolve("show/playlist.m3u8", "../../segment.ts"),
    )
  }
}
