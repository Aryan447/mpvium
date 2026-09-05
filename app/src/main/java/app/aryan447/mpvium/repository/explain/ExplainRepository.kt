package app.aryan447.mpvium.repository.explain

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * A word meaning from the keyless Wiktionary API.
 */
data class WordDefinition(
  val word: String,
  val phonetic: String?,
  val partOfSpeech: String?,
  val definition: String,
  val example: String?,
  val translation: String? = null,
)

/**
 * A cultural-reference explanation from the keyless Wikipedia API.
 */
data class ReferenceExplanation(
  val title: String,
  val extract: String,
  val url: String?,
)

@Serializable
private data class WiktDefinition(
  val definition: String? = null,
  val examples: List<String> = emptyList(),
)

@Serializable
private data class WiktEntry(
  val partOfSpeech: String? = null,
  val language: String? = null,
  val definitions: List<WiktDefinition> = emptyList(),
)

@Serializable
private data class MyMemoryData(
  val translatedText: String? = null,
)

@Serializable
private data class MyMemoryResponse(
  val responseData: MyMemoryData? = null,
  val responseStatus: Int? = null,
  val quotaFinished: Boolean? = null,
)

@Serializable
private data class WikiSearchItem(
  val title: String? = null,
)

@Serializable
private data class WikiSearchQuery(
  val search: List<WikiSearchItem> = emptyList(),
)

@Serializable
private data class WikiSearchResponse(
  val query: WikiSearchQuery? = null,
)

@Serializable
private data class WikiPage(
  val pageid: Long? = null,
  val title: String? = null,
  val extract: String? = null,
)

@Serializable
private data class WikiPagesQuery(
  val pages: Map<String, WikiPage> = emptyMap(),
)

@Serializable
private data class WikiExtractsResponse(
  val query: WikiPagesQuery? = null,
)

/**
 * Keyless lookups for the player Explain feature:
 * - word meanings via the Wiktionary REST API (no key required)
 * - word translations via the MyMemory API (no key required)
 * - dialogue/reference explanations via the Wikipedia API (no key required)
 *
 * Results are cached in memory per query. A missing dictionary entry is
 * cached as a miss so repeated taps don't hit the network.
 */
class ExplainRepository(
  private val client: OkHttpClient,
  private val json: Json,
) {
  companion object {
    private const val WIKTIONARY_BASE = "https://en.wiktionary.org/api/rest_v1/page/definition/"
    private const val MYMEMORY_BASE = "https://api.mymemory.translated.net/get"
    private const val WIKIPEDIA_BASE = "https://en.wikipedia.org/w/api.php"
    private const val USER_AGENT = "mpvium (https://github.com/aryan447/mpvium; Android)"
  }

  private val wordCache = ConcurrentHashMap<String, WordDefinition>()
  private val wordMisses = ConcurrentHashMap.newKeySet<String>()
  private val translationCache = ConcurrentHashMap<String, String>()
  private val refsCache = ConcurrentHashMap<String, List<ReferenceExplanation>>()

  /**
   * Look up a single English word. Returns null when the word has no
   * dictionary entry. Throws [IOException] on network/API failures so
   * callers can distinguish "not found" from "couldn't load".
   */
  suspend fun define(rawWord: String): WordDefinition? = withContext(Dispatchers.IO) {
    val word = rawWord.lowercase().trim().trim { !it.isLetterOrDigit() }
    if (word.isBlank() || word.contains(' ') || word.length > 40) return@withContext null
    wordCache[word]?.let { return@withContext it }
    if (wordMisses.contains(word)) return@withContext null

    val url = WIKTIONARY_BASE + URLEncoder.encode(word, "UTF-8")
    val response = client.newCall(Request.Builder().url(url).header("User-Agent", USER_AGENT).build()).execute()
    response.use {
      if (it.code == 404) {
        wordMisses.add(word)
        return@withContext null
      }
      if (!it.isSuccessful) throw IOException("Unexpected code $it")
      val body = it.body?.string() ?: throw IOException("Empty body")
      val entries = runCatching { json.decodeFromString<Map<String, List<WiktEntry>>>(body) }.getOrNull()
        ?.get("en").orEmpty()
      val entry = entries.firstOrNull { e -> e.definitions.any { d -> !d.definition.isNullOrBlank() } }
      val def = entry?.definitions?.firstOrNull { d -> !d.definition.isNullOrBlank() }
      val result = if (entry != null && def?.definition != null) {
        WordDefinition(
          word = word,
          phonetic = null,
          partOfSpeech = entry.partOfSpeech,
          definition = stripHtml(def.definition),
          example = def.examples.firstOrNull { e -> e.isNotBlank() }?.let(::stripHtml),
        )
      } else {
        null
      }
      if (result != null) {
        wordCache[word] = result
      } else {
        wordMisses.add(word)
      }
      result
    }
  }

  /**
   * Translate English [text] into [targetLang] (ISO code, e.g. "hi").
   * Returns null when translation is unavailable or unnecessary.
   * Translation is best-effort: failures return null instead of throwing.
   */
  suspend fun translate(text: String, targetLang: String): String? = withContext(Dispatchers.IO) {
    val target = targetLang.lowercase().trim()
    val clean = text.replace(Regex("\\s+"), " ").trim()
    if (target.isBlank() || target == "en" || clean.isBlank()) return@withContext null
    val key = "$clean|$target"
    translationCache[key]?.let { return@withContext it }

    val url = "$MYMEMORY_BASE?q=${URLEncoder.encode(clean, "UTF-8")}&langpair=en|$target"
    val translated = client.newCall(Request.Builder().url(url).header("User-Agent", USER_AGENT).build()).execute().use {
      if (!it.isSuccessful) return@withContext null
      val body = it.body?.string() ?: return@withContext null
      val parsed = runCatching { json.decodeFromString<MyMemoryResponse>(body) }.getOrNull()
      if (parsed?.responseStatus != 200 || parsed.quotaFinished == true) return@withContext null
      parsed.responseData?.translatedText?.trim()?.takeIf { t ->
        t.isNotBlank() && !t.startsWith("MYMEMORY WARNING") && !t.startsWith("INVALID") && !t.startsWith("QUERY LENGTH")
      }
    } ?: return@withContext null
    translationCache[key] = translated
    translated
  }

  private fun stripHtml(raw: String): String =
    raw.replace(Regex("<[^>]*>"), " ")
      .replace("&nbsp;", " ")
      .replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&#39;", "'")
      .replace(Regex("\\s+"), " ")
      .trim()

  /**
   * Explain the references in a subtitle line via Wikipedia search +
   * article extracts. Returns an empty list when nothing matches.
   * Throws [IOException] on network/API failures.
   */
  suspend fun explain(rawQuery: String): List<ReferenceExplanation> = withContext(Dispatchers.IO) {
    val query = rawQuery.replace(Regex("\\s+"), " ").trim().take(200)
    if (query.isBlank()) return@withContext emptyList()
    refsCache[query]?.let { return@withContext it }

    val searchUrl = "$WIKIPEDIA_BASE?action=query&format=json&list=search" +
      "&srsearch=${URLEncoder.encode(query, "UTF-8")}&srlimit=5&srnamespace=0"
    val titles = client.newCall(Request.Builder().url(searchUrl).header("User-Agent", USER_AGENT).build()).execute().use {
      if (!it.isSuccessful) throw IOException("Unexpected code $it")
      val body = it.body?.string() ?: throw IOException("Empty body")
      runCatching { json.decodeFromString<WikiSearchResponse>(body) }.getOrNull()
        ?.query?.search?.mapNotNull { item -> item.title?.takeIf { t -> t.isNotBlank() } }
        ?.take(3).orEmpty()
    }
    if (titles.isEmpty()) {
      refsCache[query] = emptyList()
      return@withContext emptyList()
    }

    val titlesParam = titles.joinToString("|") { URLEncoder.encode(it, "UTF-8") }
    val extractsUrl = "$WIKIPEDIA_BASE?action=query&format=json&prop=extracts" +
      "&exintro&explaintext&exsentences=3&titles=$titlesParam"
    val result = client.newCall(Request.Builder().url(extractsUrl).header("User-Agent", USER_AGENT).build()).execute().use {
      if (!it.isSuccessful) throw IOException("Unexpected code $it")
      val body = it.body?.string() ?: throw IOException("Empty body")
      runCatching { json.decodeFromString<WikiExtractsResponse>(body) }.getOrNull()
        ?.query?.pages?.values
        ?.mapNotNull { page ->
          val title = page.title?.takeIf { t -> t.isNotBlank() } ?: return@mapNotNull null
          val extract = page.extract?.trim()?.takeIf { e -> e.isNotBlank() } ?: return@mapNotNull null
          ReferenceExplanation(
            title = title,
            extract = extract,
            url = page.pageid?.let { id -> "https://en.wikipedia.org/?curid=$id" },
          )
        }.orEmpty()
    }
    refsCache[query] = result
    result
  }
}
