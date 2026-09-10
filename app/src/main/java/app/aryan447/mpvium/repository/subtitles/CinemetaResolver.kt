package app.aryan447.mpvium.repository.subtitles

import android.util.Log
import app.aryan447.mpvium.repository.wyzie.WyzieTmdbResponse
import app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder

// Stremio / Cinemeta models (keyless)
@Serializable
internal data class CinemetaSearchResponse(
    val metas: List<CinemetaMetaPreview> = emptyList(),
    val cacheMaxAge: Int? = null
)

@Serializable
internal data class CinemetaMetaPreview(
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
internal data class CinemetaMetaResponse(
    val meta: CinemetaMeta? = null
)

@Serializable
internal data class CinemetaMeta(
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
internal data class CinemetaVideo(
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

/**
 * Shared keyless title-to-IMDB resolution via Cinemeta (with Wyzie TMDB fallback).
 *
 * This logic previously lived in [app.aryan447.mpvium.repository.wyzie.WyzieSearchRepository]
 * and is used by every provider that searches by IMDB id.
 */
class CinemetaResolver(
    private val client: OkHttpClient,
    private val json: Json
) {
    val cinemetaBase = "https://v3-cinemeta.strem.io"
    private val wyzieBase = "https://sub.wyzie.io"

    // Cache numericId -> ttId for series details
    private val idToTtCache = mutableMapOf<Int, String>()

    fun resolveImdbId(query: String, year: String?, preferSeries: Boolean): String? {
        // Already an IMDB ID
        if (query.startsWith("tt", ignoreCase = true)) return query.lowercase()
        if (query.all { it.isDigit() } && query.length >= 5) return "tt$query"
        // Try Cinemeta search (type-aware)
        val cinemetaId = cinemetaSearchForImdbId(query, year, preferSeries)
        if (cinemetaId != null) return cinemetaId
        // Fallback to Wyzie TMDB search for IMDB mapping (still keyless for search)
        return try {
            val tmdbResults = tmdbSearch(query, apiKey = null)
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

    fun detectType(imdbId: String): String {
        // Fetch meta to detect type, checking both movie and series catalogs
        val ttId = if (imdbId.startsWith("tt")) imdbId else "tt$imdbId"
        listOf("movie", "series").forEach { catalogType ->
            try {
                val url = "$cinemetaBase/meta/$catalogType/$ttId.json"
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body.string()
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
            Log.w("CinemetaResolver", "Cinemeta search failed for $url: ${e.message}")
        }
        if (candidates.isEmpty()) return null
        // Prefer exact year match if provided
        val filtered = if (year != null) {
            candidates.firstOrNull { it.releaseInfo == year } ?: candidates.firstOrNull { it.releaseInfo?.startsWith(year.take(3)) == true }
        } else null
        val best = filtered ?: candidates.firstOrNull { it.name.equals(query, ignoreCase = true) } ?: candidates[0]
        return best.id.takeIf { it.startsWith("tt") } ?: best.imdbId
    }

    fun tmdbSearch(query: String, apiKey: String?): List<WyzieTmdbResult> {
        val suffix = if (!apiKey.isNullOrBlank()) "&key=${URLEncoder.encode(apiKey, "UTF-8")}" else ""
        val url = "$wyzieBase/api/tmdb/search?q=${URLEncoder.encode(query, "UTF-8")}$suffix"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("TMDb search failed: ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty body")
            return json.decodeFromString<WyzieTmdbResponse>(body).results
        }
    }

    fun findTtIdForNumericId(numericId: Int, expectedType: String): String? {
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

    fun rememberTtId(numericId: Int, ttId: String) {
        idToTtCache[numericId] = ttId
    }
}
