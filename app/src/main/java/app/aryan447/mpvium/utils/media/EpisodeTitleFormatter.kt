package app.aryan447.mpvium.utils.media

/**
 * Formats a raw player title into a clean header for shows and movies.
 *
 * Returns null when the title cannot be parsed so callers fall back to the
 * raw filename/title unchanged.
 *
 * [tmdbEpisodeTitle] is used only for shows, and only when the filename itself
 * carries no episode name (e.g. "Dexter.S02E12.1080p.mkv"). Filename-embedded
 * names always win (e.g. "Breaking.Bad.S05E16.Felina...").
 *
 * Examples (raw "Breaking.Bad.S05E16.Felina.720p.mkv"):
 * - single line: "Breaking Bad • S5:E16 \"Felina\""
 * - two line: ("Breaking Bad", "S5:E16 \"Felina\"")
 * - detail only: "S5:E16 \"Felina\""
 *
 * No episode name ("Dexter.S02E12.1080p.mkv" with empty cache):
 * - single line: "Dexter • S2:E12"
 * - detail only: "S2:E12"
 *
 * Movie input ("The.Dark.Knight.2008.1080p...") -> "The Dark Knight • 2008",
 * with "2008" as the second line in two-line mode. The detail-only value is the
 * movie title, since there is no episode to show.
 */
object EpisodeTitleFormatter {
  data class CleanTitle(
    val title: String,
    val subtitle: String?,
    val singleLine: String,
    val detailOnly: String,
    /** True when the second line is the main line (show name over episode). */
    val subtitleIsPrimary: Boolean = true,
  )

  fun resolve(rawTitle: String, tmdbEpisodeTitle: String? = null): CleanTitle? {
    if (rawTitle.isBlank()) return null
    // Network streams expose URLs as titles; leave those untouched.
    if (rawTitle.contains("://")) return null
    val info = MediaInfoParser.parse(rawTitle)
    if (info.title.isBlank()) return null

    if (info.type == "tv") {
      val season = info.season ?: return null
      val episode = info.episode ?: return null
      val episodeName = info.episodeTitle?.takeIf { it.isNotBlank() }
        ?: tmdbEpisodeTitle?.takeIf { it.isNotBlank() }
      val episodePart = buildString {
        append("S${season}:E$episode")
        episodeName?.let { append(" \"$it\"") }
      }
      return CleanTitle(
        title = info.title,
        subtitle = episodePart,
        singleLine = "${info.title} • $episodePart",
        detailOnly = episodePart,
      )
    }

    // Movie: the parser already removed the filename noise and release year.
    val year = info.year?.takeIf { it.isNotBlank() }
    return CleanTitle(
      title = info.title,
      subtitle = year,
      singleLine = if (year != null) "${info.title} • $year" else info.title,
      detailOnly = info.title,
      subtitleIsPrimary = false,
    )
  }

  fun format(rawTitle: String): String? = resolve(rawTitle)?.singleLine

  fun formatDetailOnly(rawTitle: String): String? = resolve(rawTitle)?.detailOnly
}
