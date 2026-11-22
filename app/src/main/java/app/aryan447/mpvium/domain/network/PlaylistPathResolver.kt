package app.aryan447.mpvium.domain.network

/**
 * Resolves the URIs found inside an HLS playlist to paths the network clients
 * can open.
 *
 * Playlists use URLs relative to their own location, but [NetworkConnection]
 * clients expect either a share path (SMB), an FTP path, or a path relative to
 * the connection's base directory (WebDAV).
 */
object PlaylistPathResolver {
  /**
   * Resolve [relativeUri] against the playlist's remote path.
   *
   * Query strings and fragments are dropped because the network clients only
   * accept a file path.
   */
  fun resolve(playlistPath: String, relativeUri: String): String {
    val relative = relativeUri.substringBefore('?').substringBefore('#')
    val lastSlash = playlistPath.lastIndexOf('/')
    val baseDir = if (lastSlash >= 0) playlistPath.substring(0, lastSlash) else ""
    val combined = when {
      baseDir.isEmpty() && playlistPath.startsWith("/") -> "/$relative"
      baseDir.isEmpty() -> relative
      else -> "$baseDir/$relative"
    }
    return normalize(combined)
  }

  /**
   * Collapse "." and ".." segments without touching the scheme/authority.
   * Extra ".." segments at the root are dropped so a playlist cannot escape
   * the connection's path.
   */
  fun normalize(path: String): String {
    val schemeEnd = path.indexOf("://")
    val prefix = if (schemeEnd >= 0) path.substring(0, schemeEnd + 3) else ""
    val rest = if (schemeEnd >= 0) path.substring(schemeEnd + 3) else path
    val leadingSlash = rest.startsWith("/")

    val parts = ArrayDeque<String>()
    rest.split('/').forEach { part ->
      when (part) {
        "", "." -> Unit
        ".." -> if (parts.isNotEmpty()) parts.removeLast()
        else -> parts.addLast(part)
      }
    }

    val joined = parts.joinToString("/")
    return prefix + when {
      leadingSlash && joined.isNotEmpty() -> "/$joined"
      else -> joined
    }
  }
}
