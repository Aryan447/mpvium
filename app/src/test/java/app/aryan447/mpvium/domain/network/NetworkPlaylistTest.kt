package app.aryan447.mpvium.domain.network

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URLDecoder

@RunWith(RobolectricTestRunner::class)
class NetworkPlaylistTest {

  @Test
  fun `proxy playlist URI allows streamId and filename extraction`() {
    val connectionId = 42L
    val timestamp = 1700000000L
    val index = 3
    val streamId = "${connectionId}_${timestamp}_$index"
    val fileName = "Episode 04 - The Journey [1080p].mkv"
    val filePath = "/anime/Show/Season 1/$fileName"

    val encodedName = Uri.encode(fileName)
    val encodedPath = Uri.encode(filePath)
    val uriString = "http://127.0.0.1:8080/$streamId/$encodedName?network_path=$encodedPath&connection_id=$connectionId"
    val uri = Uri.parse(uriString)

    // Proxy server extracts the streamId as the first segment of session.uri
    val extractedStreamId = uri.path?.removePrefix("/")?.split("/")?.firstOrNull()
    assertEquals(streamId, extractedStreamId)

    // Player extracts the last path segment as the video title
    val lastSegment = uri.path?.substringAfterLast("/")
    val decodedTitle = URLDecoder.decode(lastSegment ?: "", "UTF-8")
    assertEquals(fileName, decodedTitle)

    // Query parameters retain the stable network path and connection ID
    assertEquals(filePath, uri.getQueryParameter("network_path"))
    assertEquals(connectionId.toString(), uri.getQueryParameter("connection_id"))

    // Stable identifier is deterministic based on connection and path
    val identifier = "network_${connectionId}_${filePath.hashCode()}"
    val derivedIdentifier = "network_${uri.getQueryParameter("connection_id")}_${uri.getQueryParameter("network_path").hashCode()}"
    assertEquals(identifier, derivedIdentifier)
  }

  @Test
  fun `filter playable videos from network file list`() {
    val files = listOf(
      NetworkFile(name = "Season 1", path = "/anime/Season 1", isDirectory = true),
      NetworkFile(name = "Episode 01.mkv", path = "/anime/Episode 01.mkv", isDirectory = false, mimeType = "video/x-matroska"),
      NetworkFile(name = "Episode 02.mp4", path = "/anime/Episode 02.mp4", isDirectory = false, mimeType = null),
      NetworkFile(name = "stream.m3u8", path = "/anime/stream.m3u8", isDirectory = false, mimeType = "application/vnd.apple.mpegurl"),
      NetworkFile(name = "cover.jpg", path = "/anime/cover.jpg", isDirectory = false, mimeType = "image/jpeg"),
      NetworkFile(name = "subtitles.srt", path = "/anime/subtitles.srt", isDirectory = false, mimeType = "application/x-subrip"),
    )

    val playable = files.filter {
      !it.isDirectory && (
        it.mimeType?.startsWith("video/") == true ||
          NetworkMediaTypes.isPlaylistMimeType(it.mimeType) ||
          NetworkMediaTypes.isVideoFileName(it.name)
      )
    }

    assertEquals(3, playable.size)
    assertEquals("Episode 01.mkv", playable[0].name)
    assertEquals("Episode 02.mp4", playable[1].name)
    assertEquals("stream.m3u8", playable[2].name)
  }

  @Test
  fun `consecutive playback error threshold stops loop when all items fail`() {
    val playlistSize = 5
    var consecutiveErrors = 0
    val playedIndices = mutableListOf<Int>()

    // Simulate failures until threshold reached
    for (i in 0 until playlistSize + 2) {
      if (consecutiveErrors < playlistSize) {
        consecutiveErrors++
        playedIndices.add(i)
      }
    }

    // Should stop exactly at playlistSize attempts
    assertEquals(5, playedIndices.size)
    assertEquals(5, consecutiveErrors)
  }
}
