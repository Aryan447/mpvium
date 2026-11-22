package app.aryan447.mpvium.domain.network

/**
 * Rewrites the URIs inside an HLS playlist so they can be fetched through the
 * local network streaming proxy.
 *
 * Playlists reference media relative to their own URL. When the playlist is
 * served from the proxy, those relative references would resolve to the proxy
 * and fail, so each relative URI is replaced with a proxy URL. Absolute URIs
 * (CDN links, data URIs, protocol-relative URLs) are left untouched.
 */
object HlsPlaylistRewriter {
  private val URI_ATTRIBUTE = Regex("""([:,])\s*URI="([^"]*)"""")
  private val ABSOLUTE_URI = Regex("""^[a-zA-Z][a-zA-Z0-9+.-]*:""")

  /**
   * @param proxyUriFor maps a playlist-relative URI to the proxy URL that
   *   serves it. It is only called for relative URIs.
   */
  fun rewrite(content: String, proxyUriFor: (String) -> String): String =
    content.lineSequence().joinToString("\n") { line ->
      val trimmed = line.trim()
      when {
        trimmed.isEmpty() -> line
        trimmed.startsWith("#") -> rewriteAttributes(line, proxyUriFor)
        isAbsolute(trimmed) -> line
        else -> {
          val indent = line.substringBefore(trimmed)
          val suffix = line.substringAfter(trimmed)
          "$indent${proxyUriFor(trimmed)}$suffix"
        }
      }
    }

  private fun rewriteAttributes(line: String, proxyUriFor: (String) -> String): String =
    line.replace(URI_ATTRIBUTE) { match ->
      val uri = match.groupValues[2]
      if (isAbsolute(uri)) {
        match.value
      } else {
        "${match.groupValues[1]}URI=\"${proxyUriFor(uri)}\""
      }
    }

  private fun isAbsolute(uri: String): Boolean =
    uri.startsWith("//") || ABSOLUTE_URI.containsMatchIn(uri)
}
