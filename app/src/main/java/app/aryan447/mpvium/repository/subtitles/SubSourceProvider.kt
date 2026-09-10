package app.aryan447.mpvium.repository.subtitles

import app.aryan447.mpvium.repository.wyzie.WyzieLanguages
import app.aryan447.mpvium.repository.wyzie.WyzieSubtitle
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@Serializable
private data class SubSourceSearchResponse(
    val success: Boolean = false,
    val results: List<SubSourceSearchItem> = emptyList()
)

@Serializable
private data class SubSourceSearchItem(
    val id: Int = 0,
    val title: String = "",
    val type: String = "",
    val link: String = "",
    val releaseYear: Int? = null
)

@Serializable
private data class SubSourceListResponse(
    val media_type: String = "",
    val subtitles: List<SubSourceEntry> = emptyList()
)

@Serializable
private data class SubSourceEntry(
    val id: Long = 0,
    val language: String = "",
    val release_type: String? = null,
    val release_info: String = "",
    val hearing_impaired: Int = 0,
    val rating: String? = null,
    val uploader_displayname: String? = null,
    val link: String = "",
    val upload_date: String? = null
)

@Serializable
private data class SubSourceDetailResponse(
    val subtitle: SubSourceDetail? = null
)

@Serializable
private data class SubSourceDetail(
    val id: Long = 0,
    val download_token: String? = null
)

/** Matches S01E02 / s1e2 / 1x02 style markers inside release names. */
private val SEASON_EPISODE_PATTERNS = listOf(
    Regex("""(?i)[Ss](\d{1,2})[\s._-]*[Ee](\d{1,3})"""),
    Regex("""(?i)(?:^|[\s._-])(\d{1,2})[xX](\d{1,3})(?:$|[\s._-])""")
)

/** Full language names that don't exactly match [WyzieLanguages.ALL] values. */
private val LANGUAGE_NAME_ALIASES = mapOf(
    "brazilian portuguese" to "pt",
    "chinese simplified" to "zh",
    "chinese traditional" to "zh",
    "chinese bilingual" to "zh",
    "persian" to "fa",
    "farsi" to "fa",
    "hebrew" to "he"
)

private fun languageNameToIso2(name: String): String? {
    val lower = name.lowercase().trim()
    LANGUAGE_NAME_ALIASES[lower]?.let { return it }
    WyzieLanguages.ALL.entries.firstOrNull { it.value.lowercase() == lower }?.let { return it.key }
    // Lenient fallback for variants like "English (US)" or "Spanish (Latin America)".
    return WyzieLanguages.ALL.entries.firstOrNull { (code, label) ->
        lower.startsWith(label.lowercase()) || (code == "pt" && "portuguese" in lower)
    }?.key
}

private fun matchesSeasonEpisode(releaseInfo: String, season: Int, episode: Int): Boolean =
    SEASON_EPISODE_PATTERNS.any { pattern ->
        pattern.findAll(releaseInfo).any { match ->
            match.groupValues.getOrNull(1)?.toIntOrNull() == season &&
                match.groupValues.getOrNull(2)?.toIntOrNull() == episode
        }
    }

/**
 * SubSource (subsource.net) via its anonymous keyless JSON API.
 *
 * Flow: POST /v1/movie/search -> GET /v1/subtitles/{slug} ->
 * GET /v1/subtitle/{link} (download token) -> GET /v1/subtitle/download/{token} (ZIP).
 * No API key or account is required. Movies and series are supported;
 * series results are narrowed client-side by season/episode markers in the
 * release name, since one listing covers the whole show.
 */
class SubSourceProvider(
    private val client: OkHttpClient,
    private val json: Json,
    private val resolver: CinemetaResolver
) : SubtitleProvider {
    override val id: String = "subsource"
    override val displayName: String = "SubSource"
    override val isAvailable: Boolean = true

    private val apiBase = "https://api.subsource.net/v1"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun search(query: SubtitleQuery): List<WyzieSubtitle> {
        val isSeries = query.season != null && query.episode != null
        // Prefer an IMDB id for precise matching; fall back to the raw title.
        val searchTerm = resolver.resolveImdbId(query.query, query.year, preferSeries = isSeries)
            ?: query.query

        val found = searchMedia(searchTerm)
        if (found.isEmpty()) return emptyList()
        val best = pickBest(found, query)
            ?: return emptyList()

        // Series links look like /series/{slug}; the subtitle listing for both
        // movies and series lives under /subtitles/{slug}.
        val listPath = best.link.replace("series", "subtitles")
        val entries = fetchSubtitles(listPath)
        return mapAndFilter(entries, mediaTitle = best.title, query = query)
    }

    private fun searchMedia(term: String): List<SubSourceSearchItem> {
        val body = """{"query":"${term.replace("\"", "")}"}""".toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$apiBase/movie/search")
            .header("User-Agent", "mpvium/1.0")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val payload = response.body?.string() ?: throw IOException("Empty SubSource search body")
            if (!response.isSuccessful) throw IOException("SubSource search failed: ${response.code}")
            return json.decodeFromString<SubSourceSearchResponse>(payload).results
        }
    }

    private fun pickBest(
        candidates: List<SubSourceSearchItem>,
        query: SubtitleQuery
    ): SubSourceSearchItem? {
        if (candidates.isEmpty()) return null
        val wantSeries = query.season != null || query.episode != null
        val typeFiltered = candidates.filter {
            if (wantSeries) it.type.equals("tvseries", ignoreCase = true) || it.type.equals("series", ignoreCase = true)
            else it.type.equals("movie", ignoreCase = true)
        }
        val pool = if (typeFiltered.isNotEmpty()) typeFiltered else candidates
        query.year?.let { year ->
            pool.firstOrNull { it.releaseYear?.toString() == year }
                ?: pool.firstOrNull { it.releaseYear?.toString()?.startsWith(year.take(3)) == true }
        }?.let { return it }
        return pool.firstOrNull { it.title.equals(query.query, ignoreCase = true) } ?: pool[0]
    }

    private fun fetchSubtitles(listPath: String): List<SubSourceEntry> {
        val path = if (listPath.startsWith("/")) listPath else "/$listPath"
        val request = Request.Builder()
            .url("$apiBase$path")
            .header("User-Agent", "mpvium/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            val payload = response.body?.string() ?: throw IOException("Empty SubSource list body")
            if (!response.isSuccessful) throw IOException("SubSource list failed: ${response.code}")
            return json.decodeFromString<SubSourceListResponse>(payload).subtitles
        }
    }

    private fun mapAndFilter(
        entries: List<SubSourceEntry>,
        mediaTitle: String,
        query: SubtitleQuery
    ): List<WyzieSubtitle> {
        val withLang = entries.mapNotNull { entry ->
            if (entry.id == 0L || entry.link.isBlank()) return@mapNotNull null
            val iso2 = languageNameToIso2(entry.language) ?: return@mapNotNull null
            if (query.languages != null && iso2 !in query.languages) return@mapNotNull null
            entry to iso2
        }
        if (withLang.isEmpty()) return emptyList()

        val narrowed = if (query.season != null && query.episode != null) {
            val matched = withLang.filter { (entry, _) ->
                matchesSeasonEpisode(entry.release_info, query.season, query.episode)
            }
            // Fall back to the language-filtered listing (capped) instead of
            // returning nothing when no release carries an S/E marker.
            if (matched.isNotEmpty()) matched else withLang.take(40)
        } else {
            withLang.take(100)
        }

        return narrowed.map { (entry, iso2) ->
            val prettyLang = entry.language.lowercase().replaceFirstChar { it.titlecase() }
            WyzieSubtitle(
                id = "subsource-${entry.id}",
                // Opaque handle: resolved to a download token at download time.
                url = "subsource://${entry.link}",
                language = iso2,
                display = WyzieLanguages.ALL[iso2] ?: prettyLang,
                format = null,
                fileName = null,
                release = entry.release_info.takeIf { it.isNotBlank() },
                media = mediaTitle,
                isHearingImpaired = entry.hearing_impaired == 1,
                source = displayName,
                origin = entry.uploader_displayname,
                downloadCount = null
            )
        }
    }

    override suspend fun downloadBytes(subtitle: WyzieSubtitle): Pair<ByteArray, String?> {
        val link = subtitle.url.removePrefix("subsource://")
        val token = fetchDownloadToken(link)
            ?: throw IOException("SubSource did not return a download token")
        val request = Request.Builder()
            .url("$apiBase/subtitle/download/$token")
            .header("User-Agent", "mpvium/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("SubSource download failed: ${response.code}")
            val bytes = response.body?.bytes() ?: throw IOException("Empty SubSource download body")
            // SubSource serves a ZIP archive; extraction happens centrally in
            // the repository via SubtitleZip. The "zip" hint is only a fallback.
            return bytes to "zip"
        }
    }

    private fun fetchDownloadToken(link: String): String? {
        val request = Request.Builder()
            .url("$apiBase/subtitle/$link")
            .header("User-Agent", "mpvium/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            val payload = response.body?.string() ?: throw IOException("Empty SubSource detail body")
            if (!response.isSuccessful) throw IOException("SubSource detail failed: ${response.code}")
            return json.decodeFromString<SubSourceDetailResponse>(payload).subtitle?.download_token
        }
    }
}
