package app.aryan447.mpvium.utils.media

/**
 * Formats a raw player title into a clean episode header.
 *
 * Returns null when no episode info is detected so callers fall back
 * to the raw filename/title unchanged.
 *
 * [tmdbEpisodeTitle] is used only when the filename itself carries no
 * episode name (e.g. "Dexter.S02E12.1080p.mkv"). Filename-embedded names
 * always win (e.g. "Breaking.Bad.S05E16.Felina...").
 *
 * Examples (raw "Breaking.Bad.S05E16.Felina.720p.mkv"):
 * - single line: "Breaking Bad • S5:E16 \"Felina\""
 * - two line: ("Breaking Bad", "S5:E16 \"Felina\"")
 * - episode only: "S5:E16 \"Felina\""
 *
 * No episode name ("Dexter.S02E12.1080p.mkv" with empty cache):
 * - single line: "Dexter • S2:E12"
 * - episode only: "S2:E12"
 *
 * Movie input ("The.Dark.Knight.2008...") -> null.
 */
object EpisodeTitleFormatter {
  data class CleanTitle(
    val showName: String,
    val episodePart: String,
    val singleLine: String,
    val episodeOnly: String,
  )

  fun resolve(rawTitle: String, tmdbEpisodeTitle: String? = null): CleanTitle? {
    if (rawTitle.isBlank()) return null
    val info = MediaInfoParser.parse(rawTitle)
    if (info.type != "tv") return null
    val season = info.season ?: return null
    val episode = info.episode ?: return null
    if (info.title.isBlank()) return null
    val episodeName = info.episodeTitle?.takeIf { it.isNotBlank() }
      ?: tmdbEpisodeTitle?.takeIf { it.isNotBlank() }
    val episodePart = buildString {
      append("S${season}:E$episode")
      episodeName?.let { append(" \"$it\"") }
    }
    return CleanTitle(
      showName = info.title,
      episodePart = episodePart,
      singleLine = "${info.title} • $episodePart",
      episodeOnly = episodePart,
    )
  }

  fun format(rawTitle: String): String? = resolve(rawTitle)?.singleLine

  fun formatEpisodeOnly(rawTitle: String): String? = resolve(rawTitle)?.episodeOnly
}
