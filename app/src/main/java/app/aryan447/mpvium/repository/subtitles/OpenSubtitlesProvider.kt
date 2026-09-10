package app.aryan447.mpvium.repository.subtitles

import app.aryan447.mpvium.repository.wyzie.WyzieLanguages
import app.aryan447.mpvium.repository.wyzie.WyzieSubtitle
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

@Serializable
private data class StremioSubtitlesResponse(
    val subtitles: List<StremioSubtitle> = emptyList()
)

@Serializable
private data class StremioSubtitle(
    val id: String = "",
    val url: String = "",
    val lang: String = "",
    val SubEncoding: String? = null,
    val subtitleFileName: String? = null,
    val movieReleaseName: String? = null,
    val releaseGroup: String? = null,
    val releaseFormat: String? = null,
    val fpsMilli: Int? = null,
    val season: Int? = null,
    val episode: Int? = null
)

// ISO 639-2 (Stremio) -> ISO 639-1 (app) mapping for common languages
private val ISO3_TO_ISO2 = mapOf(
    "eng" to "en", "spa" to "es", "fre" to "fr", "fra" to "fr", "ger" to "de", "deu" to "de",
    "ita" to "it", "por" to "pt", "pob" to "pt", "rus" to "ru", "zho" to "zh", "chi" to "zh",
    "jpn" to "ja", "jap" to "ja", "kor" to "ko", "ara" to "ar", "hin" to "hi", "ben" to "bn",
    "pan" to "pa", "jav" to "jv", "vie" to "vi", "tel" to "te", "mar" to "mr", "tam" to "ta",
    "urd" to "ur", "tur" to "tr", "pol" to "pl", "ukr" to "uk", "dut" to "nl", "nld" to "nl",
    "gre" to "el", "ell" to "el", "hun" to "hu", "swe" to "sv", "cze" to "cs", "ces" to "cs",
    "rum" to "ro", "ron" to "ro", "dan" to "da", "fin" to "fi", "nor" to "no", "heb" to "he",
    "ind" to "id", "may" to "ms", "msa" to "ms", "tha" to "th", "per" to "fa", "fas" to "fa",
    "slo" to "sk", "slk" to "sk", "bul" to "bg", "hrv" to "hr", "scr" to "hr", "srp" to "sr",
    "scc" to "sr", "slv" to "sl", "est" to "et", "lav" to "lv", "lit" to "lt", "afr" to "af",
    "alb" to "sq", "sqi" to "sq", "amh" to "am", "arm" to "hy", "hye" to "hy", "aze" to "az",
    "baq" to "eu", "eus" to "eu", "bel" to "be", "bos" to "bs", "cat" to "ca", "wel" to "cy",
    "cym" to "cy", "epo" to "eo", "gle" to "ga", "glg" to "gl", "geo" to "ka", "kat" to "ka",
    "guj" to "gu", "hat" to "ht", "ice" to "is", "isl" to "is", "kan" to "kn", "kaz" to "kk",
    "khm" to "km", "kir" to "ky", "lao" to "lo", "mac" to "mk", "mkd" to "mk", "mlg" to "mg",
    "mlt" to "mt", "mao" to "mi", "mri" to "mi", "mon" to "mn", "nep" to "ne", "pus" to "ps",
    "sin" to "si", "swa" to "sw", "tgk" to "tg", "tat" to "tt", "uzb" to "uz", "yid" to "yi",
    "yor" to "yo", "zul" to "zu"
)

private fun iso3ToIso2(code: String): String {
    val lower = code.lowercase()
    return ISO3_TO_ISO2[lower] ?: lower.take(2)
}

/**
 * OpenSubtitles via the keyless Stremio addon (opensubtitles-v3.strem.io).
 *
 * This is the pre-existing subtitle source; its behavior is unchanged.
 * No API key or account is required.
 */
class OpenSubtitlesProvider(
    private val client: OkHttpClient,
    private val json: Json,
    private val resolver: CinemetaResolver
) : SubtitleProvider {
    override val id: String = "opensubtitles"
    override val displayName: String = "OpenSubtitles"
    override val isAvailable: Boolean = true

    private val stremioSubsBase = "https://opensubtitles-v3.strem.io"

    override suspend fun search(query: SubtitleQuery): List<WyzieSubtitle> {
        // When searching a TV show (season+episode), prefer the series catalog
        // so we don't accidentally resolve to a movie with a similar title.
        val isSeries = query.season != null && query.episode != null
        val imdbId = resolver.resolveImdbId(query.query, query.year, preferSeries = isSeries)
            ?: throw IOException("Could not resolve IMDB ID for '${query.query}'")
        // Determine type via Cinemeta meta: use series if season/episode supplied, otherwise probe
        val type = if (isSeries) "series" else resolver.detectType(imdbId)
        return fetchStremioSubtitles(imdbId, type, query.season, query.episode)
    }

    private fun fetchStremioSubtitles(
        imdbId: String,
        type: String,
        season: Int?,
        episode: Int?
    ): List<WyzieSubtitle> {
        val normalizedId = if (imdbId.startsWith("tt")) imdbId else "tt$imdbId"
        val url = if (type == "series" && season != null && episode != null) {
            "$stremioSubsBase/subtitles/series/$normalizedId:$season:$episode.json"
        } else if (type == "series") {
            // For series without episode, try movie-style? Use series base without episode
            "$stremioSubsBase/subtitles/series/$normalizedId.json"
        } else {
            "$stremioSubsBase/subtitles/movie/$normalizedId.json"
        }
        val request = Request.Builder().url(url).header("User-Agent", "mpvium/1.0").build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty body from $url")
            if (!response.isSuccessful) {
                if (response.code == 404) return emptyList()
                throw IOException("Stremio subtitles failed: ${response.code} $body")
            }
            val parsed = json.decodeFromString<StremioSubtitlesResponse>(body)
            return parsed.subtitles.mapNotNull { s -> stremioToWyzie(s) }
        }
    }

    private fun stremioToWyzie(s: StremioSubtitle): WyzieSubtitle? {
        if (s.url.isBlank()) return null
        val iso2 = iso3ToIso2(s.lang)
        val langName = WyzieLanguages.ALL[iso2] ?: s.lang
        val ext = s.subtitleFileName?.substringAfterLast(".", "")?.lowercase()
            ?: s.url.substringAfterLast(".", "").substringBefore("?").lowercase().takeIf { it.isNotEmpty() } ?: "srt"
        return WyzieSubtitle(
            id = s.id,
            url = s.url,
            language = iso2,
            display = langName,
            format = ext,
            fileName = s.subtitleFileName,
            release = s.movieReleaseName,
            media = s.subtitleFileName,
            source = "OpenSubtitles",
            origin = s.releaseGroup,
            downloadCount = null
        )
    }

    override suspend fun downloadBytes(subtitle: WyzieSubtitle): Pair<ByteArray, String?> {
        val response = client.newCall(Request.Builder().url(subtitle.url).build()).execute()
        if (!response.isSuccessful) throw IOException("Download failed: ${response.code}")
        val bytes = response.body?.bytes() ?: throw IOException("Empty body")
        return bytes to null
    }
}
