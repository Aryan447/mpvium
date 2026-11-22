package app.aryan447.mpvium.domain.network

import org.junit.Assert.assertEquals
import org.junit.Test

class HlsPlaylistRewriterTest {
  private val proxy: (String) -> String = { "http://127.0.0.1:9/proxy/$it" }

  @Test
  fun `relative segment and playlist lines are proxied`() {
    val input = """
      #EXTM3U
      #EXT-X-TARGETDURATION:6
      #EXTINF:6.0,
      segment-1.ts
      #EXTINF:6.0,
      sub/variant.m3u8
    """.trimIndent()

    val expected = """
      #EXTM3U
      #EXT-X-TARGETDURATION:6
      #EXTINF:6.0,
      http://127.0.0.1:9/proxy/segment-1.ts
      #EXTINF:6.0,
      http://127.0.0.1:9/proxy/sub/variant.m3u8
    """.trimIndent()

    assertEquals(expected, HlsPlaylistRewriter.rewrite(input, proxy))
  }

  @Test
  fun `absolute uris are left alone`() {
    val input = """
      #EXTM3U
      #EXT-X-KEY:METHOD=AES-128,URI="https://cdn.example.com/key.bin"
      #EXT-X-MAP:URI="data:application/octet-stream;base64,AAAA"
      https://cdn.example.com/segment.ts
      //cdn.example.com/other.ts
    """.trimIndent()

    assertEquals(input, HlsPlaylistRewriter.rewrite(input, proxy))
  }

  @Test
  fun `uri attributes in tags are proxied`() {
    val input = """
      #EXTM3U
      #EXT-X-KEY:METHOD=AES-128,URI="key.bin"
      #EXT-X-MAP:URI="init.mp4"
      #EXT-X-MEDIA:TYPE=AUDIO,URI="audio/index.m3u8"
    """.trimIndent()

    val expected = """
      #EXTM3U
      #EXT-X-KEY:METHOD=AES-128,URI="http://127.0.0.1:9/proxy/key.bin"
      #EXT-X-MAP:URI="http://127.0.0.1:9/proxy/init.mp4"
      #EXT-X-MEDIA:TYPE=AUDIO,URI="http://127.0.0.1:9/proxy/audio/index.m3u8"
    """.trimIndent()

    assertEquals(expected, HlsPlaylistRewriter.rewrite(input, proxy))
  }

  @Test
  fun `relative query strings are passed to the mapper unchanged`() {
    val input = "segment.ts?token=abc"

    assertEquals(
      "http://127.0.0.1:9/proxy/segment.ts?token=abc",
      HlsPlaylistRewriter.rewrite(input, proxy),
    )
  }
}
