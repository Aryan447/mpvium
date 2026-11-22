package app.aryan447.mpvium.domain.network

/**
 * File types the network browser offers for playback.
 *
 * Network listings only give us a name (and sometimes a server-provided MIME
 * type), so playback eligibility is decided by extension. HLS playlists and
 * their MIME types are included: some servers report `.m3u8` as
 * `application/vnd.apple.mpegurl` and others report no content type at all
 * (issue #198).
 */
object NetworkMediaTypes {
  val VIDEO_EXTENSIONS = setOf(
    "mp4", "m4v", "mkv", "mk3d", "avi", "mov", "wmv", "flv", "webm",
    "mpeg", "mpg", "m2v", "ogv", "ogm", "ts", "mts", "m2ts", "vob",
    "divx", "xvid", "f4v", "rm", "rmvb", "asf", "3gp", "3g2",
    "m3u", "m3u8",
  )

  /** Playlists reference their media relative to the playlist URL. */
  val PLAYLIST_EXTENSIONS = setOf("m3u", "m3u8")

  private val PLAYLIST_MIME_TYPES = setOf(
    "application/vnd.apple.mpegurl",
    "application/x-mpegurl",
    "audio/mpegurl",
    "audio/x-mpegurl",
  )

  /**
   * Lowercase extension without any query string or fragment.
   */
  fun extensionOf(fileName: String): String =
    fileName.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase()

  fun isVideoFileName(fileName: String): Boolean = extensionOf(fileName) in VIDEO_EXTENSIONS

  fun isPlaylistFileName(fileName: String): Boolean = extensionOf(fileName) in PLAYLIST_EXTENSIONS

  fun isPlaylistMimeType(mimeType: String?): Boolean =
    mimeType != null && mimeType.lowercase() in PLAYLIST_MIME_TYPES

  fun getMimeType(fileName: String): String? =
    when (val extension = extensionOf(fileName)) {
      "mp4", "m4v" -> "video/mp4"
      "mkv", "mk3d" -> "video/x-matroska"
      "avi", "divx", "xvid" -> "video/x-msvideo"
      "mov" -> "video/quicktime"
      "wmv", "asf" -> "video/x-ms-wmv"
      "flv", "f4v" -> "video/x-flv"
      "webm" -> "video/webm"
      "mpeg", "mpg", "m2v", "vob" -> "video/mpeg"
      "3gp", "3g2" -> "video/3gpp"
      "ts", "mts", "m2ts" -> "video/mp2t"
      "ogv", "ogm" -> "video/ogg"
      "rm", "rmvb" -> "video/vnd.rn-realvideo"
      "m3u8" -> "application/vnd.apple.mpegurl"
      "m3u" -> "audio/mpegurl"
      else -> if (extension in VIDEO_EXTENSIONS) "video/*" else null
    }
}
