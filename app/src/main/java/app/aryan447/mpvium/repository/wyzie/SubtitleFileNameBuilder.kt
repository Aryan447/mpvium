package app.aryan447.mpvium.repository.wyzie

import app.aryan447.mpvium.utils.media.ChecksumUtils

/**
 * Builds readable, collision-resistant file names for downloaded subtitles.
 *
 * The old `<title>.<language>.<extension>` scheme made a second download in the
 * same language overwrite the first file. That also made the player skip the new
 * subtitle because its URI was already tracked.
 */
internal object SubtitleFileNameBuilder {

  private val MEDIA_EXTENSIONS = setOf(
    "mkv", "mp4", "avi", "mov", "wmv", "flv", "webm", "m4v",
    "mpg", "mpeg", "m2ts", "ts", "3gp", "ogv"
  )

  fun baseName(
    mediaTitle: String,
    langCode: String,
    subtitle: WyzieSubtitle,
  ): String {
    val fileName = mediaTitle.substringAfterLast('/').substringAfterLast('\\')
    val extension = fileName.substringAfterLast('.', "").lowercase()
    val title = if (extension in MEDIA_EXTENSIONS) fileName.substringBeforeLast('.') else fileName
    val safeTitle = sanitizeFilePart(title).take(80).trim().ifBlank { "subtitle" }
    val tag = (subtitle.origin ?: subtitle.source ?: subtitle.id)
      ?.let(::sanitizeFilePart)
      ?.replace(" ", "")
      ?.take(16)
      ?.takeIf { it.isNotBlank() }
    // A short fingerprint of the provider URL keeps two different subtitles
    // apart even if they share a language and release group.
    val fingerprint = ChecksumUtils.getCRC32(subtitle.url).take(6)
    return listOfNotNull(safeTitle, langCode, tag, fingerprint).joinToString(".")
  }

  fun uniqueName(
    baseName: String,
    extension: String,
    exists: (String) -> Boolean,
  ): String {
    val first = "$baseName.$extension"
    if (!exists(first)) return first
    for (index in 2..999) {
      val candidate = "$baseName.$index.$extension"
      if (!exists(candidate)) return candidate
    }
    return "$baseName.${System.currentTimeMillis()}.$extension"
  }

  private fun sanitizeFilePart(value: String): String =
    value
      .replace(Regex("""[\\/:*?"<>|]"""), "_")
      .replace(Regex("""\s+"""), " ")
      .trim()
      .trim('.', ' ')
}
