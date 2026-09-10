package app.aryan447.mpvium.repository.subtitles

import android.util.Log
import app.aryan447.mpvium.preferences.SubtitlesPreferences
import app.aryan447.mpvium.repository.wyzie.WyzieSubtitle
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder

/**
 * Wyzie aggregate (sub.wyzie.io) covering SubDL, Subf2m, OpenSubtitles,
 * Podnapisi, Gestdown and AnimeTosho through one endpoint.
 *
 * This is the pre-existing authenticated fallback; its behavior is unchanged.
 * It is only available when the user configures the optional Wyzie API key,
 * so keyless installs are unaffected by it.
 */
class WyzieAggregateProvider(
    private val client: OkHttpClient,
    private val json: Json,
    private val preferences: SubtitlesPreferences,
    private val resolver: CinemetaResolver
) : SubtitleProvider {
    override val id: String = "wyzie"
    override val displayName: String = "Wyzie"
    override val isAvailable: Boolean
        get() = preferences.wyzieApiKey.get().trim().isNotBlank()

    private val wyzieBase = "https://sub.wyzie.io"

    override suspend fun search(query: SubtitleQuery): List<WyzieSubtitle> {
        val wyzieKey = preferences.wyzieApiKey.get().trim()
        if (wyzieKey.isBlank()) return emptyList()

        var searchId = query.query
        if (!query.query.startsWith("tt", ignoreCase = true) && !query.query.all { it.isDigit() }) {
            val tmdbResults = resolver.tmdbSearch(query.query, wyzieKey)
            if (tmdbResults.isNotEmpty()) {
                val result = if (query.year != null) {
                    tmdbResults.firstOrNull { it.releaseYear == query.year }
                        ?: tmdbResults.firstOrNull { it.releaseYear?.startsWith(query.year.take(3)) == true }
                        ?: tmdbResults[0]
                } else {
                    tmdbResults[0]
                }
                searchId = result.id.toString()
            } else {
                throw IOException("Could not find media ID for '${query.query}'")
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

        // Language filtering happens centrally after merging; the server-side
        // parameters only narrow what is fetched.
        return fetchWyzieSubtitles(
            id = searchId,
            season = query.season,
            episode = query.episode,
            language = languages,
            format = formatParam,
            encoding = encodingParam,
            source = sourceParam,
            hi = if (hearingImpaired) true else null
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
                Log.e("WyzieAggregateProvider", errorMsg)
                throw IOException(errorMsg)
            }
            return try {
                json.decodeFromString<List<WyzieSubtitle>>(responseBodyString)
            } catch (e: Exception) {
                Log.e("WyzieAggregateProvider", "Failed to parse response: $responseBodyString", e)
                emptyList()
            }
        }
    }

    override suspend fun downloadBytes(subtitle: WyzieSubtitle): Pair<ByteArray, String?> {
        val response = client.newCall(Request.Builder().url(subtitle.url).build()).execute()
        if (!response.isSuccessful) throw IOException("Download failed: ${response.code}")
        val bytes = response.body?.bytes() ?: throw IOException("Empty body")
        return bytes to null
    }
}
