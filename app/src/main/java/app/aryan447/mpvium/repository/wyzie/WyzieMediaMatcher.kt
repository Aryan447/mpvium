package app.aryan447.mpvium.repository.wyzie

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Picks the TMDb/Cinemeta result that most likely matches a file on disk.
 *
 * Search APIs rank by relevance, but the first entry is not always the right
 * edition: remakes, sequels, spin-offs and same-name movies/series all compete
 * for the top spots. Selecting the first result blindly produced wrong posters
 * (e.g. an old remake, a movie for a show, or a sequel for the original), so
 * candidates are scored on normalized title similarity and release year.
 *
 * Weak matches return null instead of attaching obviously wrong artwork.
 */
object WyzieMediaMatcher {

  private val LEADING_ARTICLES = setOf("the", "a", "an")
  private val COMBINING_MARKS = Regex("""\p{M}+""")
  private val NON_WORD = Regex("""[^\p{L}\p{N}]+""")
  private val APOSTROPHES = Regex("""['’`]""")
  private val YEAR = Regex("""(?:19|20)\d{2}""")

  /**
   * @param expectedMediaType "tv" or "movie". Results of the other type are
   *   never used: a movie poster on a show is worse than no poster at all.
   * @param title Title parsed from the file or folder name.
   * @param year Release year parsed from the file, when available.
   */
  fun pickBest(
    results: List<WyzieTmdbResult>,
    expectedMediaType: String,
    title: String,
    year: String?,
  ): WyzieTmdbResult? {
    if (results.isEmpty() || title.isBlank()) return null

    val isSeries = expectedMediaType.equals("tv", ignoreCase = true)
    val typedResults = results.filter { it.mediaType.equals(expectedMediaType, ignoreCase = true) }
    if (typedResults.isEmpty()) return null

    val query = normalizeTitle(title)
    if (query.isEmpty()) return null
    val queryYear = parseYear(year)

    var best: ScoredCandidate? = null
    typedResults.forEachIndexed { index, result ->
      val candidateTitle = normalizeTitle(result.title)
      val similarity = max(
        titleSimilarity(query, candidateTitle),
        result.originalTitle?.let { titleSimilarity(query, normalizeTitle(it)) } ?: 0.0,
      )
      val resultYear = parseYear(result.releaseYear)
      val yearDelta = if (queryYear != null && resultYear != null) {
        abs(queryYear - resultYear)
      } else {
        null
      }

      // Titles like "Wonder Woman 1984" or "Blade Runner 2049" carry the year
      // in the title, while TMDb reports a different release year. When the
      // filename year completes the parsed title, that is a strong signal.
      val titleWithYear = if (queryYear != null) query + " " + queryYear else null
      val matchesTitleWithYear = titleWithYear != null &&
        (candidateTitle == titleWithYear ||
          result.originalTitle?.let { normalizeTitle(it) == titleWithYear } == true)

      val yearScore = when {
        yearDelta == null -> 0.0
        yearDelta == 0 -> 35.0
        // A show keeps running after its premiere year, so a file from a
        // later season must not be treated as a year mismatch.
        isSeries && queryYear != null && resultYear != null && queryYear >= resultYear -> 4.0
        yearDelta == 1 -> 8.0
        else -> -30.0
      }
      val titleYearScore = if (matchesTitleWithYear) 50.0 else 0.0

      // Search order encodes relevance; use it only as a fine tie-breaker.
      val orderScore = (typedResults.size - index).toDouble() / typedResults.size
      val candidate = ScoredCandidate(
        result = result,
        similarity = similarity,
        yearDelta = yearDelta,
        matchesTitleWithYear = matchesTitleWithYear,
        score = similarity * 100.0 + yearScore + titleYearScore + orderScore,
      )
      val current = best
      if (current == null || candidate.score > current.score) {
        best = candidate
      }
    }

    val winner = best ?: return null
    val yearConflict = winner.yearDelta != null &&
      winner.yearDelta > 1 &&
      !winner.matchesTitleWithYear &&
      (!isSeries || !hasRunningShowYear(queryYear, winner.result.releaseYear))

    return when {
      // Punctuation/spelling variants normalize to the same title. Keep the
      // match even if the year differs: the year in the filename can refer to
      // a season, a remaster, or be missing entirely.
      winner.similarity >= 0.95 -> winner.result
      winner.matchesTitleWithYear -> winner.result
      yearConflict -> null
      winner.similarity >= 0.50 -> winner.result
      // TMDb sometimes resolves a well-known alias to a different official
      // title (e.g. "Fast & Furious 9" -> "F9"). When that is the only typed
      // result and the year agrees, trust the search.
      typedResults.size == 1 && (winner.yearDelta == null || winner.yearDelta <= 1) ->
        winner.result
      else -> null
    }
  }

  /** Lowercase, de-accent and remove punctuation so titles compare cleanly. */
  fun normalizeTitle(raw: String): String {
    if (raw.isBlank()) return ""
    val decomposed = Normalizer.normalize(raw, Normalizer.Form.NFKD)
    val stripped = COMBINING_MARKS.replace(decomposed, "")
      .lowercase(Locale.ROOT)
      .replace("&", " and ")
    val withoutApostrophes = APOSTROPHES.replace(stripped, "")
    val words = NON_WORD.replace(withoutApostrophes, " ")
      .trim()
      .split(' ')
      .filter { it.isNotEmpty() }
    // "The Matrix" and "Matrix" should be treated as the same title.
    val meaningful = if (words.size > 1 && words.first() in LEADING_ARTICLES) {
      words.drop(1)
    } else {
      words
    }
    return meaningful.joinToString(" ")
  }

  internal fun titleSimilarity(a: String, b: String): Double {
    if (a.isEmpty() || b.isEmpty()) return 0.0
    if (a == b) return 1.0

    val compactA = a.replace(" ", "")
    val compactB = b.replace(" ", "")
    if (compactA == compactB) return 1.0

    val shorter: String
    val longer: String
    if (compactA.length <= compactB.length) {
      shorter = compactA
      longer = compactB
    } else {
      shorter = compactB
      longer = compactA
    }
    val containsScore = if (longer.contains(shorter)) {
      shorter.length.toDouble() / longer.length * 0.85
    } else {
      0.0
    }

    val tokensA = a.split(' ').toSet()
    val tokensB = b.split(' ').toSet()
    val union = tokensA.union(tokensB).size
    val jaccard = if (union > 0) tokensA.intersect(tokensB).size.toDouble() / union else 0.0

    val editSimilarity = 1.0 - levenshtein(compactA, compactB).toDouble() /
      max(compactA.length, compactB.length)

    return maxOf(editSimilarity, containsScore, jaccard * 0.9)
  }

  private fun parseYear(raw: String?): Int? =
    raw?.let { YEAR.find(it)?.value?.toIntOrNull() }

  private fun hasRunningShowYear(queryYear: Int?, resultYearRaw: String?): Boolean {
    val resultYear = parseYear(resultYearRaw) ?: return false
    return queryYear != null && queryYear >= resultYear
  }

  private fun levenshtein(a: String, b: String): Int {
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    var previous = IntArray(b.length + 1) { it }
    var current = IntArray(b.length + 1)
    for (i in 1..a.length) {
      current[0] = i
      for (j in 1..b.length) {
        val substitutionCost = if (a[i - 1] == b[j - 1]) 0 else 1
        current[j] = minOf(
          current[j - 1] + 1,
          previous[j] + 1,
          previous[j - 1] + substitutionCost,
        )
      }
      val swap = previous
      previous = current
      current = swap
    }
    return previous[b.length]
  }

  private data class ScoredCandidate(
    val result: WyzieTmdbResult,
    val similarity: Double,
    val yearDelta: Int?,
    val matchesTitleWithYear: Boolean,
    val score: Double,
  )
}
