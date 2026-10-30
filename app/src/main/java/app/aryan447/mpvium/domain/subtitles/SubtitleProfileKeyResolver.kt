package app.aryan447.mpvium.domain.subtitles

import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.utils.media.MediaInfoParser
import app.aryan447.mpvium.utils.media.ParsedMediaInfo
import java.io.File

/**
 * Resolves the per-title key used by subtitle profiles.
 *
 * Episodes share one key per show (the same normalized series id the library
 * uses), while standalone movies use the normalized title + release year key.
 * The show/movie detection mirrors SeriesDetector so a file opened from the
 * player resolves to the same title it has in the library.
 */
object SubtitleProfileKeyResolver {
  private const val MAX_FALLBACK_EPISODE_DURATION_MS = 90 * 60 * 1000L

  private val SEASON_FOLDER_REGEX =
    Regex("""\b(?:Season|Series|Specials|S)[\s._-]*(\d{1,2})\b""", RegexOption.IGNORE_CASE)
  private val SPECIALS_FOLDER_REGEX = Regex("""^(?:Specials|Extras)$""", RegexOption.IGNORE_CASE)
  private val EPISODE_NUM_REGEX = Regex("""(?:^|[^\d])(?:E|EP|Episode|#)?\s*(\d{1,4})(?:[^\d]|$)""", RegexOption.IGNORE_CASE)
  private val PART_OR_EPISODE_TITLE_REGEX =
    Regex("""(?:cd|disc|disk|part|pt|episode|ep|e)?[\s._-]*\d{1,3}""", RegexOption.IGNORE_CASE)

  private val GENERIC_FOLDER_NAMES = setOf(
    "movies", "movie", "films", "film", "videos", "video", "tv", "tvshows",
    "shows", "series", "media", "download", "downloads", "storage", "emulated",
    "0", "dcim", "camera", "whatsapp", "telegram", "documents", "desktop",
    "sdcard", "all", "other", "misc", "new", "unsorted", "season", "specials",
    "extras", "featurettes", "trailers", "bonus",
  )

  private val GENERIC_VIDEO_TITLES = setOf(
    "movie", "video", "film", "feature", "untitled", "unknown", "index",
    "main", "play", "watch", "sample", "trailer",
  )

  fun resolve(
    fileName: String,
    filePath: String? = null,
    durationMs: Long = 0L,
  ): String? {
    val name = fileName.trim()
    if (name.isBlank()) return null

    val parsed = MediaInfoParser.parse(name)
    val parentFile = parentFileOf(filePath)
    val parentName = parentFile?.name.orEmpty()
    val grandParentName = parentFile?.parentFile?.name.orEmpty()

    val seasonMatch = SEASON_FOLDER_REGEX.find(parentName)
    val isSpecialsFolder = SPECIALS_FOLDER_REGEX.matches(parentName.trim())
    val parentSeasonNum = when {
      seasonMatch != null -> seasonMatch.groupValues[1].toIntOrNull() ?: 1
      isSpecialsFolder -> 0
      else -> null
    }

    var isTvSeries = parsed.type == "tv" || parsed.season != null || parsed.episode != null
    var seriesTitle = parsed.title
    var seriesYear = parsed.year
    var seasonNum = parsed.season ?: parentSeasonNum ?: 1
    var episodeNum = parsed.episode
    var isFolderFallback = false

    // Folder-level fallback: Show / Season 1 / 01.mkv
    if (!isTvSeries && parentSeasonNum != null) {
      isTvSeries = true
      isFolderFallback = true
      seasonNum = parentSeasonNum
      resolveFolderMedia(grandParentName)?.let { folder ->
        seriesTitle = folder.title
        seriesYear = seriesYear ?: folder.year
      }
      if (episodeNum == null) {
        val epMatch = EPISODE_NUM_REGEX.find(name)
        episodeNum = epMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
      }
    }

    // A bare episode number directly inside a show folder: Show / 01.mkv
    if (!isTvSeries && parentSeasonNum == null &&
      parsed.title.matches(Regex("""\d{1,3}""")) &&
      (durationMs == 0L || durationMs < MAX_FALLBACK_EPISODE_DURATION_MS)
    ) {
      resolveFolderMedia(parentName)?.let { folder ->
        isTvSeries = true
        isFolderFallback = true
        seriesTitle = folder.title
        seriesYear = seriesYear ?: folder.year
        seasonNum = 1
        episodeNum = parsed.title.toIntOrNull() ?: 1
      }
    }

    // A long video in a "Season" folder is usually a movie, not an episode.
    if (isFolderFallback && durationMs >= MAX_FALLBACK_EPISODE_DURATION_MS) {
      isTvSeries = false
    }

    // Bare filenames like S01E01.mkv carry no show name; recover it from the
    // containing show/season folder.
    if (isTvSeries && seriesTitle.isBlank()) {
      val primaryFolder = if (parentSeasonNum != null) grandParentName else parentName
      val secondaryFolder = if (parentSeasonNum != null) "" else grandParentName
      val folder = resolveFolderMedia(primaryFolder) ?: resolveFolderMedia(secondaryFolder)
      if (folder != null) {
        seriesTitle = folder.title
        seriesYear = seriesYear ?: folder.year
      }
    }

    if (isTvSeries && seriesTitle.isNotBlank()) {
      return SubtitleProfileRepository.seriesKeyFor(StreamingMetadataRepository.normalizeKey(seriesTitle))
    }

    var movieTitle = parsed.title.ifBlank { name.substringBeforeLast('.') }
    var movieYear = parsed.year
    if (shouldUseFolderTitle(movieTitle)) {
      val movieFolderName = if (parentSeasonNum != null) grandParentName else parentName
      resolveFolderMedia(movieFolderName)?.let { folder ->
        movieTitle = folder.title
        movieYear = movieYear ?: folder.year
      }
    }
    if (movieTitle.isBlank()) return null
    return SubtitleProfileRepository.movieKeyFor(movieTitle, movieYear)
  }

  private fun parentFileOf(path: String?): File? {
    if (path.isNullOrBlank()) return null
    val normalized = path.removePrefix("file://")
    if (normalized.startsWith("content://") || normalized.startsWith("http://") || normalized.startsWith("https://")) {
      return null
    }
    return runCatching { File(normalized).parentFile }.getOrNull()
  }

  private fun resolveFolderMedia(name: String): ParsedMediaInfo? {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return null
    val parsed = MediaInfoParser.parse(trimmed)
    val title = parsed.title.ifBlank { trimmed }
    val key = StreamingMetadataRepository.normalizeKey(title)
    if (key.isBlank() || key in GENERIC_FOLDER_NAMES) return null
    if (title.matches(Regex("""\d+"""))) return null
    return parsed.copy(title = title)
  }

  private fun shouldUseFolderTitle(title: String): Boolean {
    val normalized = title.lowercase(java.util.Locale.ROOT).trim()
    if (normalized.isBlank()) return true
    if (normalized in GENERIC_VIDEO_TITLES) return true
    return PART_OR_EPISODE_TITLE_REGEX.matches(normalized)
  }
}
