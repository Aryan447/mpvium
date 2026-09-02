package app.aryan447.mpvium.repository.wyzie

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import app.aryan447.mpvium.preferences.SubtitlesPreferences
import app.aryan447.mpvium.utils.media.ChecksumUtils
import app.aryan447.mpvium.utils.media.MediaInfoParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder

@Serializable
data class WyzieSubtitle(
    val id: String? = null,
    val url: String,
    val flagUrl: String? = null,
    val format: String? = null,
    val encoding: String? = null,
    val display: String? = null,
    val language: String? = null,
    val media: String? = null,
    val isHearingImpaired: Boolean = false,
    val source: String? = null,
    val release: String? = null,
    val releases: List<String> = emptyList(),
    val origin: String? = null,
    val fileName: String? = null,
    val matchedRelease: String? = null,
    val matchedFilter: String? = null,
    val downloadCount: Int? = null
) {
    val displayName: String get() = fileName ?: release ?: media ?: "Unknown Subtitle"
    val displayLanguage: String get() = display ?: language ?: "Unknown"
}


@Serializable
data class WyzieTmdbResult(
    val id: Int,
    val mediaType: String,
    val title: String,
    val releaseYear: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val overview: String? = null,
    val imdbId: String? = null
)

@Serializable
data class WyzieTmdbResponse(
    val results: List<WyzieTmdbResult>
)

@Serializable
data class WyzieSeason(
    val id: Int? = null,
    val name: String? = null,
    val season_number: Int,
    val episode_count: Int? = null,
    val poster_path: String? = null,
    val overview: String? = null
)

@Serializable
data class WyzieEpisode(
    val id: Int? = null,
    val name: String? = null,
    val episode_number: Int,
    val season_number: Int,
    val still_path: String? = null,
    val overview: String? = null
)

@Serializable
data class WyzieTvShowDetails(
    val id: Int,
    val name: String,
    val seasons: List<WyzieSeason> = emptyList()
)

@Serializable
data class WyzieSeasonDetails(
    val id: String? = null,
    val season_number: Int,
    val episodes: List<WyzieEpisode> = emptyList()
)

// Stremio / Cinemeta models (keyless)
@Serializable
private data class CinemetaSearchResponse(
    val metas: List<CinemetaMetaPreview> = emptyList(),
    val cacheMaxAge: Int? = null
)

@Serializable
private data class CinemetaMetaPreview(
    val id: String = "",
    @SerialName("imdb_id") val imdbId: String? = null,
    val type: String = "",
    val name: String = "",
    val poster: String? = null,
    val background: String? = null,
    val releaseInfo: String? = null,
    val description: String? = null,
    val overview: String? = null,
    val genres: List<String> = emptyList()
)

@Serializable
private data class CinemetaMetaResponse(
    val meta: CinemetaMeta? = null
)

@Serializable
private data class CinemetaMeta(
    val id: String = "",
    val imdb_id: String? = null,
    val type: String = "",
    val name: String = "",
    val poster: String? = null,
    val background: String? = null,
    val releaseInfo: String? = null,
    val year: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val runtime: String? = null,
    val videos: List<CinemetaVideo> = emptyList()
)

@Serializable
private data class CinemetaVideo(
    val id: String = "",
    val title: String? = null,
    val name: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    // some entries use number/episode interchangeably
    val number: Int? = null,
    val released: String? = null,
    val overview: String? = null,
    val thumbnail: String? = null
)

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

object WyzieSources {
    val ALL = mapOf(
        "all" to "All",
        "subdl" to "SubDL",
        "subf2m" to "Subf2m",
        "opensubtitles" to "OpenSubtitles",
        "podnapisi" to "Podnapisi",
        "gestdown" to "Gestdown",
        "animetosho" to "AnimeTosho"
    )
}

object WyzieFormats {
    val ALL = mapOf(
        "srt" to "SRT",
        "ass" to "ASS",
        "ssa" to "SSA",
        "vtt" to "VTT",
        "sub" to "SUB"
    )
}

object WyzieEncodings {
    val ALL = mapOf(
        "iso-8859-6" to "Arabic (ISO-8859-6)",
        "cp1256" to "Arabic (Cp1256)",
        "cp1257" to "Baltic (Cp1257)",
        "iso-8859-13" to "Baltic (ISO-8859-13)",
        "iso-8859-4" to "Baltic, Scandinavia (ISO-8859-4)",
        "iso-8859-14" to "Celtic (ISO-8859-14)",
        "iso-8859-2" to "Central European, Slavic (ISO-8859-2)",
        "ms936" to "Chinese, Simplified (MS936)",
        "gb18030" to "Chinese, Simplified (GB18030)",
        "euc_cn" to "Chinese, Simplified (EUC_CN)",
        "gbk" to "Chinese, Simplified (GBK)",
        "iso-2022-cn" to "Chinese, Simplified (ISO-2022-CN)",
        "ms950" to "Chinese, Traditional (MS950)",
        "ms950_hkscs" to "Chinese, Traditional (Hong Kong) (MS950_HKSCS)",
        "big5" to "Chinese, Traditional (Big5)",
        "big5-hkscs" to "Chinese, Traditional (Hong Kong) (Big5-HKSCS)",
        "cp1251" to "Cyrillic (Cp1251)",
        "iso-8859-5" to "Cyrillic (ISO-8859-5)",
        "cp1250" to "Eastern European (Cp1250)",
        "cp1253" to "Greek (Cp1253)",
        "iso-8859-7" to "Greek (ISO-8859-7)",
        "iso-8859-8" to "Hebrew (ISO-8859-8)",
        "cp1255" to "Hebrew (Cp1255)",
        "iscii91" to "Indic scripts (ISCII91)",
        "ms932" to "Japanese (MS932)",
        "euc_jp" to "Japanese (EUC_JP)",
        "shift_jis" to "Japanese (Shift_JIS)",
        "iso-2022-jp" to "Japanese (ISO-2022-JP)",
        "ms949" to "Korean (MS949)",
        "euc_kr" to "Korean (EUC_KR)",
        "iso-2022-kr" to "Korean (ISO-2022-KR)",
        "iso-8859-10" to "Nordic (ISO-8859-10)",
        "iso-8859-16" to "Romanian (ISO-8859-16)",
        "koi8_r" to "Russian (KOI8_R)",
        "iso-8859-3" to "South European (ISO-8859-3)",
        "tis-620" to "Thai (TIS-620)",
        "iso-8859-11" to "Thai (ISO-8859-11)",
        "cp1254" to "Turkish (Cp1254)",
        "iso-8859-9" to "Turkish (ISO-8859-9)",
        "utf-8" to "Unicode (UTF-8)",
        "utf-16" to "Unicode (UTF-16)",
        "utf-16be" to "Unicode (UTF-16BE)",
        "utf-16le" to "Unicode (UTF-16LE)",
        "utf-32" to "Unicode (UTF-32)",
        "utf-32be" to "Unicode (UTF-32BE)",
        "utf-32le" to "Unicode (UTF-32LE)",
        "us-ascii" to "(US-ASCII)",
        "cp1258" to "Vietnamese (Cp1258)",
        "iso-8859-1" to "Western European (ISO-8859-1)",
        "iso-8859-15" to "Western European (ISO-8859-15)",
        "cp1252" to "Western European (ANSI) (Cp1252)"
    )
}

object WyzieLanguages {
    val ALL = mapOf(
        "en" to "English", "es" to "Spanish", "fr" to "French", "de" to "German",
        "it" to "Italian", "pt" to "Portuguese", "ru" to "Russian", "zh" to "Chinese",
        "ja" to "Japanese", "ko" to "Korean", "ar" to "Arabic", "hi" to "Hindi",
        "bn" to "Bengali", "pa" to "Punjabi", "jv" to "Javanese", "vi" to "Vietnamese",
        "te" to "Telugu", "mr" to "Marathi", "ta" to "Tamil", "ur" to "Urdu",
        "tr" to "Turkish", "pl" to "Polish", "uk" to "Ukrainian", "nl" to "Dutch",
        "el" to "Greek", "hu" to "Hungarian", "sv" to "Swedish", "cs" to "Czech",
        "ro" to "Romanian", "da" to "Danish", "fi" to "Finnish", "no" to "Norwegian",
        "he" to "Hebrew", "id" to "Indonesian", "ms" to "Malay", "th" to "Thai",
        "fa" to "Persian", "sk" to "Slovak", "bg" to "Bulgarian", "hr" to "Croatian",
        "sr" to "Serbian", "sl" to "Slovenian", "et" to "Estonian", "lv" to "Latvian",
        "lt" to "Lithuanian", "af" to "Afrikaans", "sq" to "Albanian", "am" to "Amharic",
        "hy" to "Armenian", "az" to "Azerbaijani", "eu" to "Basque", "be" to "Belarusian",
        "bs" to "Bosnian", "ca" to "Catalan", "cy" to "Welsh", "eo" to "Esperanto",
        "ga" to "Irish", "gl" to "Galician", "ka" to "Georgian", "gu" to "Gujarati",
        "ht" to "Haitian Creole", "is" to "Icelandic", "kn" to "Kannada", "kk" to "Kazakh",
        "km" to "Khmer", "ky" to "Kyrgyz", "lo" to "Lao", "mk" to "Macedonian",
        "mg" to "Malagasy", "mt" to "Maltese", "mi" to "Maori", "mn" to "Mongolian",
        "ne" to "Nepali", "ps" to "Pashto", "si" to "Sinhala", "sw" to "Swahili",
        "tg" to "Tajik", "tt" to "Tatar", "uz" to "Uzbek", "yi" to "Yiddish",
        "yo" to "Yoruba", "zu" to "Zulu"
    )
    val SORTED = ALL.toList().sortedBy { it.second }.toMap()
}

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

class WyzieSearchRepository(
    private val context: Context,
    private val client: OkHttpClient,
    private val json: Json,
    private val preferences: SubtitlesPreferences
) {
    // Primary keyless provider: Stremio / Cinemeta + OpenSubtitles v3
    private val cinemetaBase = "https://v3-cinemeta.strem.io"
    private val stremioSubsBase = "https://opensubtitles-v3.strem.io"
    // Fallback Wyzie (requires API key, now optional)
    private val wyzieBase = "https://sub.wyzie.io"
    // Cache numericId -> ttId for series details
    private val idToTtCache = mutableMapOf<Int, String>()

    suspend fun search(
        query: String,
        season: Int? = null,
        episode: Int? = null,
        year: String? = null
    ): Result<List<WyzieSubtitle>> = withContext(Dispatchers.IO) {
        try {
            // Try Stremio keyless path first
            val stremioResult = runCatching { stremioSearch(query, season, episode, year) }.getOrNull()
            if (stremioResult != null && stremioResult.isNotEmpty()) {
                return@withContext Result.success(filterAndSort(stremioResult, query))
            }
            // If Stremio returned empty and we have no Wyzie key, return empty (no 401)
            val wyzieKey = preferences.wyzieApiKey.get().trim()
            if (wyzieKey.isBlank()) {
                // Stremio was empty, no Wyzie key to fallback -> return empty or stremio empty
                if (stremioResult != null) return@withContext Result.success(emptyList())
                // Try Wyzie without key to give better error? It will 401, so return friendly error
                return@withContext Result.failure(IOException("No subtitles found. Try a different title or check language filters. (Stremio keyless search)"))
            }
            // Fallback to Wyzie with key
            var searchId = query
            if (!query.startsWith("tt", ignoreCase = true) && !query.all { it.isDigit() }) {
                val tmdbResults = tmdbSearch(query)
                if (tmdbResults.isNotEmpty()) {
                    val result = if (year != null) {
                        tmdbResults.firstOrNull { it.releaseYear == year }
                            ?: tmdbResults.firstOrNull { it.releaseYear?.startsWith(year.take(3)) == true }
                            ?: tmdbResults[0]
                    } else {
                        tmdbResults[0]
                    }
                    searchId = result.id.toString()
                } else {
                    return@withContext Result.failure(Exception("Could not find media ID for '$query'"))
                }
            }

            val selectedLangsRaw = preferences.subdlLanguages.get()
            val languages = if (selectedLangsRaw.isNotEmpty() && !selectedLangsRaw.contains("all")) {
                selectedLangsRaw.joinToString(",").lowercase()
            } else null

            val sources = preferences.wyzieSources.get()
            val sourceParam = if (sources.isEmpty() || sources.contains("all")) "all" else sources.joinToString(",").lowercase()

            val formats = preferences.wyzieFormats.get()
            val formatParam = if (formats.isNotEmpty() && !formats.contains("all")) formats.joinToString(",").lowercase() else null

            val encodings = preferences.wyzieEncodings.get()
            val encodingParam = if (encodings.isNotEmpty() && !encodings.contains("all")) encodings.joinToString(",").lowercase() else null

            val hearingImpaired = preferences.wyzieHearingImpaired.get()

            val results = fetchWyzieSubtitles(
                id = searchId,
                season = season,
                episode = episode,
                language = languages,
                format = formatParam,
                encoding = encodingParam,
                source = sourceParam,
                hi = if (hearingImpaired) true else null
            )

            val filteredResults = if (languages != null && languages != "all") {
                val allowedLangs = languages.split(",").map { it.trim() }
                results.filter { sub ->
                    val subLangCode = WyzieLanguages.ALL.entries.find {
                        it.value.equals(sub.language, ignoreCase = true)
                    }?.key ?: sub.language?.lowercase()
                    allowedLangs.contains(subLangCode)
                }
            } else {
                results
            }

            Result.success(filterAndSort(filteredResults, query))
        } catch (e: Exception) {
            Log.e("WyzieSearchRepository", "Search failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun filterAndSort(results: List<WyzieSubtitle>, query: String): List<WyzieSubtitle> {
        val selectedLangsRaw = preferences.subdlLanguages.get()
        val languages = if (selectedLangsRaw.isNotEmpty() && !selectedLangsRaw.contains("all")) {
            selectedLangsRaw.joinToString(",").lowercase()
        } else null
        val filtered = if (languages != null && languages != "all") {
            val allowedLangs = languages.split(",").map { it.trim() }
            results.filter { sub ->
                val subLangCode = WyzieLanguages.ALL.entries.find {
                    it.value.equals(sub.language, ignoreCase = true)
                }?.key ?: sub.language?.lowercase()
                allowedLangs.contains(subLangCode)
            }
        } else results

        return filtered.sortedWith(compareByDescending<WyzieSubtitle> { sub ->
            val name = sub.displayName.lowercase()
            val q = query.lowercase()
            var score = 0
            if (name.contains(q)) score += 100
            if (name.contains("720p") || name.contains("1080p") || name.contains("2160p")) score += 50
            if (name.contains("web-dl") || name.contains("webrip") || name.contains("bluray")) score += 40
            if (name.contains("yify") || name.contains("sparks") || name.contains("rarbg")) score += 30
            score
        }.thenByDescending { it.displayName.length })
    }

    // ---- Stremio keyless implementation ----
    private fun stremioSearch(
        query: String,
        season: Int?,
        episode: Int?,
        year: String?
    ): List<WyzieSubtitle> {
        // Resolve IMDB ID. When searching a TV show (season+episode), prefer the series catalog
        // so we don't accidentally resolve to a movie with a similar title.
        val isSeries = season != null && episode != null
        val imdbId = resolveImdbId(query, year, preferSeries = isSeries)
            ?: throw IOException("Could not resolve IMDB ID for '$query'")
        // Determine type via Cinemeta meta: use series if season/episode supplied, otherwise probe
        val type = if (isSeries) "series" else detectType(imdbId)
        return fetchStremioSubtitles(imdbId, type, season, episode)
    }

    private fun resolveImdbId(query: String, year: String?, preferSeries: Boolean): String? {
        // Already an IMDB ID
        if (query.startsWith("tt", ignoreCase = true)) return query.lowercase()
        if (query.all { it.isDigit() } && query.length >= 5) return "tt$query"
        // Try Cinemeta search (type-aware)
        val cinemetaId = cinemetaSearchForImdbId(query, year, preferSeries)
        if (cinemetaId != null) return cinemetaId
        // Fallback to Wyzie TMDB search for IMDB mapping (still keyless for search)
        return try {
            val tmdbResults = tmdbSearch(query)
            if (tmdbResults.isEmpty()) return null
            // Prefer results matching the expected media type
            val expectedType = if (preferSeries) "tv" else "movie"
            val typed = tmdbResults.filter { it.mediaType == expectedType }
            val pool = if (typed.isNotEmpty()) typed else tmdbResults
            val result = if (year != null) {
                pool.firstOrNull { it.releaseYear == year }
                    ?: pool.firstOrNull { it.releaseYear?.startsWith(year.take(3)) == true }
                    ?: pool[0]
            } else pool[0]
            // Try to get IMDB via Cinemeta detail using TMDB? We can search again with title
            cinemetaSearchForImdbId(result.title, year, preferSeries) ?: "tt${result.id}"
        } catch (_: Exception) {
            null
        }
    }

    private fun detectType(imdbId: String): String {
        // Fetch meta to detect type, checking both movie and series catalogs
        val ttId = if (imdbId.startsWith("tt")) imdbId else "tt$imdbId"
        listOf("movie", "series").forEach { catalogType ->
            try {
                val url = "$cinemetaBase/meta/$catalogType/$ttId.json"
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: return@forEach
                        val parsed = json.decodeFromString<CinemetaMetaResponse>(body)
                        val metaType = parsed.meta?.type
                        if (!metaType.isNullOrBlank()) return metaType
                    }
                }
            } catch (_: Exception) {}
        }
        return "movie"
    }

    private fun cinemetaSearchForImdbId(query: String, year: String?, preferSeries: Boolean): String? {
        val encoded = URLEncoder.encode(query, "UTF-8")
        // Search only the relevant catalog. Searching both lets a movie with a similar name
        // shadow the TV series (and vice-versa), so restrict to the expected type.
        val url = if (preferSeries) {
            "$cinemetaBase/catalog/series/top/search=$encoded.json"
        } else {
            "$cinemetaBase/catalog/movie/top/search=$encoded.json"
        }
        val candidates = mutableListOf<CinemetaMetaPreview>()
        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val parsed = json.decodeFromString<CinemetaSearchResponse>(body)
                candidates.addAll(parsed.metas)
            }
        } catch (e: Exception) {
            Log.w("WyzieSearchRepository", "Cinemeta search failed for $url: ${e.message}")
        }
        if (candidates.isEmpty()) return null
        // Prefer exact year match if provided
        val filtered = if (year != null) {
            candidates.firstOrNull { it.releaseInfo == year } ?: candidates.firstOrNull { it.releaseInfo?.startsWith(year.take(3)) == true }
        } else null
        val best = filtered ?: candidates.firstOrNull { it.name.equals(query, ignoreCase = true) } ?: candidates[0]
        return best.id.takeIf { it.startsWith("tt") } ?: best.imdbId
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

    private fun fetchWyzieSubtitles(
        id: String,
        season: Int? = null,
        episode: Int? = null,
        language: String? = null,
        format: String? = null,
        encoding: String? = null,
        source: String = "all",
        hi: Boolean? = null
    ): List<WyzieSubtitle> {
        fun encode(s: String) = URLEncoder.encode(s, "UTF-8")
        val wyzieKey = preferences.wyzieApiKey.get().trim()
        val url = StringBuilder("$wyzieBase/search?id=${encode(id)}")
            .apply {
                if (season != null && episode != null) {
                    append("&season=$season")
                    append("&episode=$episode")
                }
                language?.filter { !it.isWhitespace() }?.let { append("&language=${encode(it)}") }
                format?.split(",")?.filter { it.isNotBlank() }?.forEach { append("&${encode(it.trim())}=true") }
                encoding?.split(",")?.filter { it.isNotBlank() }?.forEach { append("&${encode(it.trim())}=true") }
                if (source != "all") {
                    source.split(",").filter { it.isNotBlank() }.forEach { append("&${encode(it.trim())}=true") }
                }
                append("&unzip=true")
                hi?.let { append("&hi=$it") }
                if (wyzieKey.isNotBlank()) append("&key=${encode(wyzieKey)}")
            }.toString()

        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val responseBodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                if (response.code == 400 && responseBodyString.contains("No subtitles found", ignoreCase = true)) {
                    return emptyList()
                }
                if (response.code == 400 && responseBodyString.contains("season and episode", ignoreCase = true)) {
                    throw IOException("Please select both a Season and an Episode.")
                }
                if (response.code == 401) {
                    throw IOException("Wyzie API key required or invalid. Get a free key at https://store.wyzie.io/redeem and set it in Settings > Subtitles, or use the default keyless Stremio provider.")
                }
                val errorMsg = "Search failed: HTTP ${response.code} for URL: $url | Body: $responseBodyString"
                Log.e("WyzieSearchRepository", errorMsg)
                throw IOException(errorMsg)
            }
            return try {
                json.decodeFromString<List<WyzieSubtitle>>(responseBodyString)
            } catch (e: Exception) {
                Log.e("WyzieSearchRepository", "Failed to parse response: $responseBodyString", e)
                emptyList()
            }
        }
    }

    suspend fun download(subtitle: WyzieSubtitle, mediaTitle: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(Request.Builder().url(subtitle.url).build()).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Download failed: ${response.code}"))

            val bytes = response.body?.bytes() ?: return@withContext Result.failure(Exception("Empty body"))
            val urlExtension = subtitle.url.substringAfterLast("/", "").substringBefore("?").substringAfterLast(".", "")
            val extension = subtitle.format?.lowercase() ?: urlExtension.takeIf { it.isNotEmpty() } ?: "srt"

            val saveFolderUri = preferences.subtitleSaveFolder.get()
            val folderName = ChecksumUtils.getCRC32(mediaTitle)
            val fullTitle = mediaTitle.substringBeforeLast(".")
            val langCode = subtitle.language ?: "en"
            val subFileName = "${fullTitle}.${langCode}.$extension"

            if (saveFolderUri.isNotBlank()) {
                val parentDir = DocumentFile.fromTreeUri(context, Uri.parse(saveFolderUri))
                if (parentDir?.exists() == true) {
                    var movieDir = parentDir.findFile(folderName) ?: parentDir.createDirectory(folderName)
                    if (movieDir != null) {
                        val subFile = movieDir.findFile(subFileName) ?: movieDir.createFile("application/octet-stream", subFileName)
                        if (subFile != null) {
                            context.contentResolver.openOutputStream(subFile.uri)?.use { it.write(bytes) }
                            return@withContext Result.success(subFile.uri)
                        }
                    }
                }
            }

            val internalMoviesDir = File(context.getExternalFilesDir(null), "Movies")
            val movieDir = File(internalMoviesDir, folderName).apply { if (!exists()) mkdirs() }
            val file = File(movieDir, subFileName)
            FileOutputStream(file).use { it.write(bytes) }
            Result.success(Uri.fromFile(file))
        } catch (e: Exception) {
            Log.e("WyzieSearchRepository", "Download failed", e)
            Result.failure(e)
        }
    }

    suspend fun searchMedia(query: String): Result<List<WyzieTmdbResult>> = withContext(Dispatchers.IO) {
        try {
            // Try Cinemeta keyless search first
            val cinemetaResults = runCatching { cinemetaSearchMedia(query) }.getOrNull()
            if (!cinemetaResults.isNullOrEmpty()) return@withContext Result.success(cinemetaResults)
            // Fallback to Wyzie TMDB search (still keyless for this endpoint currently)
            Result.success(tmdbSearch(query))
        } catch (e: Exception) {
            Log.e("WyzieSearchRepository", "Media search failed", e)
            Result.failure(e)
        }
    }

    private fun cinemetaSearchMedia(query: String): List<WyzieTmdbResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val results = mutableListOf<WyzieTmdbResult>()
        listOf("series" to "tv", "movie" to "movie").forEach { (catalogType, mediaType) ->
            val url = "$cinemetaBase/catalog/$catalogType/top/search=$encoded.json"
            try {
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@forEach
                    val body = resp.body?.string() ?: return@forEach
                    val parsed = json.decodeFromString<CinemetaSearchResponse>(body)
                    parsed.metas.take(10).forEach { meta ->
                        val numericId = meta.id.removePrefix("tt").toIntOrNull() ?: meta.id.hashCode()
                        val ttId = meta.id.takeIf { it.startsWith("tt") } ?: meta.imdbId
                        if (ttId != null) {
                            idToTtCache[numericId] = ttId
                        }
                        results.add(
                            WyzieTmdbResult(
                                id = numericId,
                                mediaType = mediaType,
                                title = meta.name,
                                releaseYear = meta.releaseInfo?.take(4),
                                poster = meta.poster,
                                backdrop = meta.background,
                                overview = meta.description ?: meta.overview,
                                imdbId = ttId
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        // Deduplicate and keep all results across both series and movies
        return results.distinctBy { it.id to it.mediaType }
    }

    suspend fun getTvShowDetails(id: Int): Result<WyzieTvShowDetails> = withContext(Dispatchers.IO) {
        try {
            // First try Cinemeta (keyless) - need to map Int id back to ttId
            val ttId = findTtIdForNumericId(id, "series")
            if (ttId != null) {
                val cinemetaResult = runCatching { cinemetaGetTvShowDetails(ttId, id) }.getOrNull()
                if (cinemetaResult != null) return@withContext Result.success(cinemetaResult)
            }
            // Fallback to Wyzie
            val url = "$wyzieBase/api/tmdb/tv/$id"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to get TV show details: ${response.code}")
                val body = response.body?.string() ?: throw IOException("Empty body from $url")
                Result.success(json.decodeFromString<WyzieTvShowDetails>(body))
            }
        } catch (e: Exception) {
            Log.e("WyzieSearchRepository", "Failed to get TV show details", e)
            Result.failure(e)
        }
    }

    private fun findTtIdForNumericId(numericId: Int, expectedType: String): String? {
        idToTtCache[numericId]?.let { return it }
        val candidates = listOf(
            "tt" + numericId.toString().padStart(7, '0'),
            "tt$numericId"
        )
        // Verify by fetching meta
        for (tt in candidates) {
            try {
                val url = "$cinemetaBase/meta/series/$tt.json"
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        idToTtCache[numericId] = tt
                        return tt
                    }
                }
            } catch (_: Exception) {}
            // Try movie as well
            try {
                val url = "$cinemetaBase/meta/movie/$tt.json"
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        idToTtCache[numericId] = tt
                        return tt
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun cinemetaGetTvShowDetails(ttId: String, originalId: Int): WyzieTvShowDetails {
        val url = "$cinemetaBase/meta/series/$ttId.json"
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Cinemeta meta failed: ${resp.code}")
            val body = resp.body?.string() ?: throw IOException("Empty body")
            val parsed = json.decodeFromString<CinemetaMetaResponse>(body)
            val meta = parsed.meta ?: throw IOException("No meta for $ttId")
            // Group videos by season
            val seasonMap = mutableMapOf<Int, MutableList<CinemetaVideo>>()
            meta.videos.forEach { v ->
                val s = v.season ?: return@forEach
                if (s == 0) return@forEach // skip specials for now
                seasonMap.getOrPut(s) { mutableListOf() }.add(v)
            }
            val seasons = seasonMap.entries.sortedBy { it.key }.map { (seasonNum, vids) ->
                WyzieSeason(
                    id = seasonNum,
                    name = "Season $seasonNum",
                    season_number = seasonNum,
                    episode_count = vids.size,
                    poster_path = null,
                    overview = null
                )
            }
            return WyzieTvShowDetails(
                id = originalId,
                name = meta.name,
                seasons = seasons
            )
        }
    }

    suspend fun getSeasonEpisodes(id: Int, season: Int): Result<List<WyzieEpisode>> = withContext(Dispatchers.IO) {
        try {
            val ttId = findTtIdForNumericId(id, "series")
            if (ttId != null) {
                val cinemetaEps = runCatching { cinemetaGetEpisodes(ttId, season) }.getOrNull()
                if (cinemetaEps != null) return@withContext Result.success(cinemetaEps)
            }
            val url = "$wyzieBase/api/tmdb/tv/$id/$season"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to get season episodes: ${response.code}")
                val body = response.body?.string() ?: throw IOException("Empty body from $url")
                Result.success(json.decodeFromString<WyzieSeasonDetails>(body).episodes)
            }
        } catch (e: Exception) {
            Log.e("WyzieSearchRepository", "Failed to get season episodes", e)
            Result.failure(e)
        }
    }

    private fun cinemetaGetEpisodes(ttId: String, season: Int): List<WyzieEpisode> {
        val url = "$cinemetaBase/meta/series/$ttId.json"
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Cinemeta meta failed: ${resp.code}")
            val body = resp.body?.string() ?: throw IOException("Empty body")
            val parsed = json.decodeFromString<CinemetaMetaResponse>(body)
            val meta = parsed.meta ?: return emptyList()
            return meta.videos.filter { it.season == season }
                .sortedBy { it.episode ?: it.number ?: 0 }
                .map { v ->
                    val epNum = v.episode ?: v.number ?: 0
                    WyzieEpisode(
                        id = epNum,
                        name = v.title ?: v.name ?: "Episode $epNum",
                        episode_number = epNum,
                        season_number = season,
                        still_path = v.thumbnail,
                        overview = v.overview
                    )
                }
        }
    }

    private fun tmdbSearch(query: String): List<WyzieTmdbResult> {
        val wyzieKey = preferences.wyzieApiKey.get().trim()
        val suffix = if (wyzieKey.isNotBlank()) "&key=${URLEncoder.encode(wyzieKey, "UTF-8")}" else ""
        val url = "$wyzieBase/api/tmdb/search?q=${URLEncoder.encode(query, "UTF-8")}$suffix"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("TMDb search failed: ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty body")
            return json.decodeFromString<WyzieTmdbResponse>(body).results
        }
    }

    suspend fun deleteSubtitleFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = if (uri.scheme == "content") DocumentFile.fromSingleUri(context, uri) else DocumentFile.fromFile(File(uri.path ?: uri.toString()))
            if (file == null || !file.exists()) return@withContext false
            val deleted = file.delete()
            if (deleted) {
                preferences.subtitleSaveFolder.get().takeIf { it.isNotBlank() }?.let { cleanupEmptyFolders(Uri.parse(it)) }
            }
            deleted
        } catch (e: Exception) {
            Log.e("WyzieSearchRepository", "Delete failed", e)
            false
        }
    }

    private fun cleanupEmptyFolders(saveFolderUri: Uri) {
        try {
            val root = DocumentFile.fromTreeUri(context, saveFolderUri) ?: return
            root.listFiles().forEach { if (it.isDirectory && it.listFiles()?.isEmpty() == true) it.delete() }
        } catch (e: Exception) {
            Log.e("WyzieSearchRepository", "Cleanup failed", e)
        }
    }
}
