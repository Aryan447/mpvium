package app.aryan447.mpvium.utils.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeUrlParsingTest {
  @Test
  fun `isYouTubeUrl recognizes standard, short, mobile, music, and shorts URLs`() {
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://www.youtube.com/watch?v=QDia3e12czc"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://youtu.be/dQw4w9WgXcQ?si=xyz123"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://youtube.com/shorts/abc123"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://youtube.com/live/abc123"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ"))
  }

  @Test
  fun `isYouTubeUrl rejects non-YouTube and non-HTTP URLs`() {
    assertFalse(YouTubeExtractor.isYouTubeUrl("https://example.com/video.mp4"))
    assertFalse(YouTubeExtractor.isYouTubeUrl("https://notyoutube.com/watch?v=dQw4w9WgXcQ"))
    assertFalse(YouTubeExtractor.isYouTubeUrl("file:///sdcard/Movies/video.mp4"))
    assertFalse(YouTubeExtractor.isYouTubeUrl("rtsp://192.168.1.2/live"))
    assertFalse(YouTubeExtractor.isYouTubeUrl(""))
    assertFalse(YouTubeExtractor.isYouTubeUrl(null))
  }

  @Test
  fun `extractUrlFromSharedText extracts bare and wrapped URLs`() {
    assertEquals(
      "https://www.youtube.com/watch?v=QDia3e12czc",
      HttpUtils.extractUrlFromSharedText("https://www.youtube.com/watch?v=QDia3e12czc"),
    )
    assertEquals(
      "https://youtu.be/dQw4w9WgXcQ?si=abc",
      HttpUtils.extractUrlFromSharedText("https://youtu.be/dQw4w9WgXcQ?si=abc"),
    )
    assertEquals(
      "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
      HttpUtils.extractUrlFromSharedText("  https://www.youtube.com/watch?v=dQw4w9WgXcQ \n"),
    )
    assertEquals(
      "https://www.youtube.com/watch?v=QDia3e12czc",
      HttpUtils.extractUrlFromSharedText("https://www.youtube.com/watch?v=QDia3e12czc"),
    )
    assertEquals(
      "https://youtu.be/dQw4w9WgXcQ?si=abc",
      HttpUtils.extractUrlFromSharedText("Video Title\nhttps://youtu.be/dQw4w9WgXcQ?si=abc"),
    )
    assertEquals(
      "https://youtube.com/shorts/abc123",
      HttpUtils.extractUrlFromSharedText("Watch this (https://youtube.com/shorts/abc123)."),
    )
    assertEquals(
      "rtsp://192.168.1.10:554/stream",
      HttpUtils.extractUrlFromSharedText("rtsp://192.168.1.10:554/stream"),
    )
    assertNull(HttpUtils.extractUrlFromSharedText("Just a plain text message"))
    assertNull(HttpUtils.extractUrlFromSharedText("   "))
  }
}
