package app.aryan447.mpvium.repository.subtitles

import app.aryan447.mpvium.repository.wyzie.WyzieSubtitle

/**
 * Query shared by all subtitle providers.
 *
 * @param query Raw title text, TMDB id, or IMDB id (tt...).
 * @param season Season number for series, null for movies.
 * @param episode Episode number for series, null for movies.
 * @param year Release year, when known.
 * @param languages Allowed ISO 639-1 language codes (lowercased), or null for all.
 */
data class SubtitleQuery(
    val query: String,
    val season: Int? = null,
    val episode: Int? = null,
    val year: String? = null,
    val languages: Set<String>? = null
)

/**
 * Common interface for online subtitle sources.
 *
 * Each provider owns its search logic, result parsing, and download
 * mechanism. The repository fans out to all available providers
 * concurrently and normalizes everything into [WyzieSubtitle].
 */
interface SubtitleProvider {
    /** Stable id used for download routing (e.g. "opensubtitles"). */
    val id: String

    /** User-facing name, also used as [WyzieSubtitle.source]. */
    val displayName: String

    /**
     * False when the provider cannot be used right now
     * (e.g. its optional API key is not configured).
     */
    val isAvailable: Boolean

    /** Search this provider. Must not throw for empty results; throw only on real failures. */
    suspend fun search(query: SubtitleQuery): List<WyzieSubtitle>

    /**
     * Fetch the raw subtitle payload for a result previously returned by [search].
     * @return payload bytes plus an extension hint (e.g. "zip", "srt"), or null when unknown.
     */
    suspend fun downloadBytes(subtitle: WyzieSubtitle): Pair<ByteArray, String?>
}
