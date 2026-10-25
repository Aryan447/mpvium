package app.aryan447.mpvium.repository.wyzie

import android.util.Log
import app.aryan447.mpvium.preferences.SubtitlesPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

@Serializable
private data class SubsourceSearchResponse(
  val data: List<SubsourceMovie> = emptyList(),
)

/**
 * Numbers in SubSource responses are usually plain integers, but some
 * endpoints have been observed returning them as strings, so these fields
 * are decoded as primitives and coerced here instead of failing.
 */
@Serializable
private data class SubsourceMovie(
  val movieId: JsonPrimitive? = null,
  val title: String = "",
  val type: String = "",
  val releaseYear: JsonPrimitive? = null,
  val imdbId: String? = null,
  val season: JsonPrimitive? = null,
) {
  val movieIdValue: Int? get() = movieId?.content?.toDoubleOrNull()?.toInt()?.takeIf { it > 0 }
  val releaseYearValue: String?
    get() = releaseYear?.content?.takeIf { it != "null" && it.isNotBlank() }
  val seasonValue: Int? get() = season?.content?.toDoubleOrNull()?.toInt()
}

@Serializable
private data class SubsourceSubtitlesResponse(
  val data: List<SubsourceEntry> = emptyList(),
)

@Serializable
private data class SubsourceEntry(
  val subtitleId: JsonPrimitive? = null,
  val language: String = "",
  val releaseInfo: JsonElement? = null,
  val hearingImpaired: JsonPrimitive? = null,
  val downloads: JsonPrimitive? = null,
) {
  val subtitleIdValue: Long? get() = subtitleId?.content?.toDoubleOrNull()?.toLong()?.takeIf { it > 0 }
  val isHearingImpaired: Boolean
    get() = hearingImpaired?.content.equals("true", ignoreCase = true) || hearingImpaired?.content == "1"
  val downloadsValue: Long get() = downloads?.content?.toDoubleOrNull()?.toLong() ?: 0L

  /** `releaseInfo` may be a single string or a list of release names. */
  val releaseNames: List<String>
    get() = when (val info = releaseInfo) {
      null -> emptyList()
      is JsonArray -> info.mapNotNull { (it as? JsonPrimitive)?.content?.takeIf(::isUsableReleaseName) }
      is JsonPrimitive -> listOfNotNull(info.content.takeIf(::isUsableReleaseName))
      else -> emptyList()
    }

  private fun isUsableReleaseName(value: String): Boolean = value.isNotBlank() && value != "null"
}

/** Raised when SubSource rejects (or is missing) the configured API key. */
class SubsourceAuthException(message: String) : IOException(message)

/**
 * SubSource (subsource.net) via its official API v1.
 *
 * Requires a personal API key from the API tab of a subsource.net account
 * (Settings → Subtitles → SubSource API Key). Subtitles are looked up by
 * IMDb id (falling back to a text search) and downloads arrive as ZIP
 * archives, which [SubtitleArchive] unpacks.
 */
class SubsourceProvider(
  private val client: OkHttpClient,
  private val json: Json,
  private val preferences: SubtitlesPreferences,
) {
  companion object {
    private const val TAG = "SubsourceProvider"
    private const val API_BASE = "https://api.subsource.net/api/v1"
    private const val USER_AGENT = "mpvium/1.0"
    private const val PAGE_LIMIT = 100
    private const val HANDLE_PREFIX = "subsource://"
    private val SEASON_EPISODE_REGEX = Regex("""(?i)[Ss](\d{1,2})[\s._-]*[Ee](\d{1,3})""")

    /**
     * SubSource spells a few languages differently from ISO display names
     * (e.g. "brazilian_portuguese", "farsi_persian"). Everything else maps
     * from the plain English name with spaces replaced by underscores.
     */
    private val LANGUAGE_ALIASES = mapOf(
      "pt" to listOf("portuguese", "brazilian_portuguese"),
      "zh" to listOf(
        "chinese", "chinese_simplified", "chinese_traditional",
        "chinese_bilingual", "chinese_cantonese", "big_5_code", "chinese_bg_code"
      ),
      "fa" to listOf("farsi_persian", "persian"),
      "es" to listOf("spanish", "spanish_spain", "spanish_latin_america"),
      "fr" to listOf("french", "french_france", "french_canada"),
    )

    private val NAME_TO_ISO: Map<String, String> by lazy {
      val map = mutableMapOf<String, String>()
      WyzieLanguages.ALL.forEach { (code, name) ->
        map[defaultLanguageName(name)] = code
      }
      LANGUAGE_ALIASES.forEach { (code, names) -> names.forEach { map[it] = code } }
      map
    }

    private fun defaultLanguageName(displayName: String): String =
      displayName.lowercase().replace(" ", "_")
  }

  val isConfigured: Boolean get() = apiKey().isNotBlank()

  fun isSubsourceHandle(url: String): Boolean = url.startsWith(HANDLE_PREFIX)

  fun subtitleIdFromHandle(handle: String): Long? =
    handle.removePrefix(HANDLE_PREFIX).substringBefore("?").toLongOrNull()

  suspend fun search(
    query: String,
    imdbId: String?,
    season: Int?,
    episode: Int?,
    year: String?,
  ): List<WyzieSubtitle> = withContext(Dispatchers.IO) {
    if (!isConfigured) return@withContext emptyList()
    val key = apiKey()
    val movie = pickBestMovie(searchMovies(key, query, imdbId, season), query, season, year)
      ?: return@withContext emptyList()
    val movieId = movie.movieIdValue ?: return@withContext emptyList()
    val entries = fetchSubtitles(key, movieId, season, episode)
    mapEntries(entries, movie.title, season, episode)
  }

  /** Downloads the subtitle archive for a [search] result and unpacks it. */
  suspend fun downloadAndExtract(handle: String): Pair<ByteArray, String> = withContext(Dispatchers.IO) {
    val subtitleId = subtitleIdFromHandle(handle)
      ?: throw IOException("Invalid SubSource subtitle link")
    val (season, episode) = seasonEpisodeFromHandle(handle)
    SubtitleArchive.unwrapIfZip(
      bytes = downloadBytes(subtitleId),
      fallbackExtension = null,
      season = season,
      episode = episode,
    )
  }

  private fun searchMovies(
    key: String,
    query: String,
    imdbId: String?,
    season: Int?,
  ): List<SubsourceMovie> {
    val common = buildList {
      if (season != null) add("season" to season.toString())
    }
    if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt", ignoreCase = true)) {
      val byImdb = runCatching {
        requestMovies(key, common + listOf("searchType" to "imdb", "imdb" to imdbId.lowercase()))
      }.onFailure {
        if (it is SubsourceAuthException) throw it
        Log.w(TAG, "IMDb lookup failed, falling back to text search: ${it.message}")
      }.getOrDefault(emptyList())
      if (byImdb.isNotEmpty()) return byImdb
    }
    return requestMovies(key, common + listOf("searchType" to "text", "q" to query))
  }

  private fun requestMovies(key: String, params: List<Pair<String, String>>): List<SubsourceMovie> {
    val body = requestBody(key, apiUrl("/movies/search", params))
    return json.decodeFromString<SubsourceSearchResponse>(body).data
  }

  private fun pickBestMovie(
    candidates: List<SubsourceMovie>,
    query: String,
    season: Int?,
    year: String?,
  ): SubsourceMovie? {
    if (candidates.isEmpty()) return null
    var pool = candidates
    val wantSeries = season != null
    val typed = pool.filter { movie ->
      val type = movie.type.lowercase()
      if (wantSeries) type == "series" || type == "tvseries" || type == "tv"
      else type == "movie" || type.isBlank()
    }
    if (typed.isNotEmpty()) pool = typed
    if (season != null) {
      val seasonMatches = pool.filter { it.seasonValue == season }
      if (seasonMatches.isNotEmpty()) pool = seasonMatches
    }
    if (year != null) {
      pool.firstOrNull { it.releaseYearValue == year }?.let { return it }
    }
    return pool.firstOrNull { it.title.equals(query, ignoreCase = true) } ?: pool.first()
  }

  private fun fetchSubtitles(
    key: String,
    movieId: Int,
    season: Int?,
    episode: Int?,
  ): List<SubsourceEntry> {
    val baseParams = buildList {
      add("movieId" to movieId.toString())
      add("limit" to PAGE_LIMIT.toString())
      if (season != null && episode != null) {
        add("season" to season.toString())
        add("episode" to episode.toString())
      }
    }
    // The API accepts one language per request, so fan out over the selection.
    val languages = selectedLanguageNames()
    val paramsList = if (languages.isEmpty()) listOf(baseParams) else languages.map { baseParams + ("language" to it) }
    val entries = mutableListOf<SubsourceEntry>()
    paramsList.forEach { params ->
      runCatching {
        json.decodeFromString<SubsourceSubtitlesResponse>(requestBody(key, apiUrl("/subtitles", params))).data
      }.onFailure {
        if (it is SubsourceAuthException) throw it
        Log.w(TAG, "Subtitle listing failed: ${it.message}")
      }.getOrNull()?.let { entries += it }
    }
    return entries.distinctBy { it.subtitleIdValue }
  }

  private fun mapEntries(
    entries: List<SubsourceEntry>,
    mediaTitle: String,
    season: Int?,
    episode: Int?,
  ): List<WyzieSubtitle> {
    val results = mutableListOf<WyzieSubtitle>()
    // SubSource always lists hearing-impaired tracks; the app treats them as
    // opt-in, matching the keyless provider that filters them server side.
    val includeHearingImpaired = preferences.wyzieHearingImpaired.get()
    entries.forEach { entry ->
      val subtitleId = entry.subtitleIdValue ?: return@forEach
      if (entry.isHearingImpaired && !includeHearingImpaired) return@forEach
      val releases = entry.releaseNames
      if (season != null && episode != null && !matchesEpisode(releases, season, episode)) return@forEach
      val iso2 = languageNameToIso2(entry.language)
      results.add(
        WyzieSubtitle(
          id = "subsource-$subtitleId",
          url = handleUrl(subtitleId, season, episode),
          language = iso2,
          display = iso2?.let { WyzieLanguages.ALL[it] } ?: prettyLanguageName(entry.language),
          format = null,
          release = releases.joinToString(" / ").takeIf { it.isNotBlank() } ?: mediaTitle,
          media = mediaTitle,
          isHearingImpaired = entry.isHearingImpaired,
          source = "SubSource",
          downloadCount = entry.downloadsValue.toInt().takeIf { it > 0 },
        )
      )
    }
    return results
  }

  /** Keeps entries that match the episode or carry no episode marker (a season pack). */
  private fun matchesEpisode(releases: List<String>, season: Int, episode: Int): Boolean {
    var sawMarker = false
    releases.forEach { release ->
      SEASON_EPISODE_REGEX.findAll(release).forEach { match ->
        sawMarker = true
        val matchSeason = match.groupValues[1].toIntOrNull()
        val matchEpisode = match.groupValues[2].toIntOrNull()
        if (matchSeason == season && matchEpisode == episode) return true
      }
    }
    return !sawMarker
  }

  private fun handleUrl(subtitleId: Long, season: Int?, episode: Int?): String = buildString {
    append(HANDLE_PREFIX)
    append(subtitleId)
    if (season != null && episode != null) append("?season=$season&episode=$episode")
  }

  private fun seasonEpisodeFromHandle(handle: String): Pair<Int?, Int?> =
    intQueryParam(handle, "season") to intQueryParam(handle, "episode")

  private fun intQueryParam(handle: String, name: String): Int? =
    handle.substringAfter("?", "")
      .split("&")
      .firstOrNull { it.startsWith("$name=") }
      ?.substringAfter("=")
      ?.toIntOrNull()

  private fun downloadBytes(subtitleId: Long): ByteArray {
    val request = Request.Builder()
      .url("$API_BASE/subtitles/$subtitleId/download")
      .header("X-API-Key", apiKey())
      .header("User-Agent", USER_AGENT)
      .build()
    client.newCall(request).execute().use { response ->
      if (response.code == 401 || response.code == 403) {
        throw SubsourceAuthException("SubSource API key missing or invalid. Set it in Settings > Subtitles.")
      }
      if (response.code == 429) throw IOException("SubSource rate limit reached. Try again in a minute.")
      if (!response.isSuccessful) throw IOException("SubSource download failed: HTTP ${response.code}")
      return response.body?.bytes() ?: throw IOException("Empty SubSource download body")
    }
  }

  private fun requestBody(key: String, url: String): String {
    val request = Request.Builder()
      .url(url)
      .header("X-API-Key", key)
      .header("User-Agent", USER_AGENT)
      .build()
    client.newCall(request).execute().use { response ->
      if (response.code == 401 || response.code == 403) {
        throw SubsourceAuthException("SubSource API key missing or invalid. Set it in Settings > Subtitles.")
      }
      if (response.code == 429) throw IOException("SubSource rate limit reached. Try again in a minute.")
      if (!response.isSuccessful) throw IOException("SubSource request failed: HTTP ${response.code}")
      return response.body?.string() ?: throw IOException("Empty response from SubSource")
    }
  }

  private fun apiUrl(path: String, params: List<Pair<String, String>>): String {
    val builder = "$API_BASE$path".toHttpUrl().newBuilder()
    params.forEach { (name, value) -> builder.addQueryParameter(name, value) }
    return builder.build().toString()
  }

  private fun apiKey(): String = preferences.subsourceApiKey.get().trim()

  private fun selectedLanguageNames(): List<String> {
    val selected = preferences.subdlLanguages.get()
    if (selected.isEmpty() || selected.contains("all")) return emptyList()
    return selected.filter { it.isNotBlank() }
      .flatMap { code ->
        val normalized = code.trim().lowercase()
        LANGUAGE_ALIASES[normalized] ?: listOf(defaultLanguageName(WyzieLanguages.ALL[normalized] ?: normalized))
      }
      .distinct()
  }

  private fun languageNameToIso2(name: String): String? {
    val normalized = name.trim().lowercase().replace(' ', '_').replace('-', '_')
    return NAME_TO_ISO[normalized]
  }

  private fun prettyLanguageName(name: String): String =
    name.replace('_', ' ').trim().split(' ')
      .filter { it.isNotBlank() }
      .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }
}