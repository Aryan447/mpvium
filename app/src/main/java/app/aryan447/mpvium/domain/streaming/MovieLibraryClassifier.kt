package app.aryan447.mpvium.domain.streaming

import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import java.util.Locale

/**
 * Whether a standalone video belongs in the Movies library.
 */
enum class MovieLibraryKind {
  MOVIE,
  SHORT_VIDEO,
}

/**
 * Separates movie-library entries from ordinary short videos.
 *
 * The scanner has no reliable "this is a film" flag. Every video without
 * season/episode markers becomes a standalone movie, which also pulls in
 * reels, trailers, messaging-app exports and personal recordings. This
 * classifier therefore combines several existing signals instead of relying
 * on duration alone:
 *
 * - source folder: social-app caches, clip folders, camera/screen recordings
 *   and downloads (library-source information from the file path);
 * - filename: Reddit/WhatsApp/social exports, trailers, samples;
 * - shape: portrait or square videos are short-form rather than feature films;
 * - duration: short clips, with explicit long-form and unknown-metadata
 *   guards so genuine movies are not hidden.
 *
 * Classification is a read-only display decision. It never deletes, moves or
 * renames a file, and the Folders tab reads media files directly, so excluded
 * clips stay browsable and playable there.
 */
object MovieLibraryClassifier {
  private const val MINUTE_MS = 60_000L

  /** Anything at or under this length is a clip in any folder/orientation. */
  private const val VERY_SHORT_MAX_MS = 5 * MINUTE_MS

  /** Portrait/square short-form videos are reels/status clips, not movies. */
  private const val PORTRAIT_MAX_MS = 20 * MINUTE_MS

  /** Explicit clip names/folders (social exports, trailers, clip collections). */
  private const val CLIP_SOURCE_MAX_MS = 30 * MINUTE_MS

  /** Downloads/camera/recordings: ambiguous sources, so use a lower cap. */
  private const val TRANSIENT_SOURCE_MAX_MS = 20 * MINUTE_MS

  /**
   * Long-form guard: never hide a feature-length file, even when its name or
   * folder looks like a clip source. This protects mislabelled movies.
   */
  private const val FEATURE_LENGTH_MS = 40 * MINUTE_MS

  private val WHITESPACE_REGEX = Regex("""\s+""")

  /**
   * Filename markers produced by common downloaders/social apps, plus
   * trailer/sample markers. Only applied below [CLIP_SOURCE_MAX_MS].
   */
  private val CLIP_NAME_PATTERNS = listOf(
    // Reddit video downloader: RDT_20240101_211604.mp4
    Regex("""(^|[^a-z0-9])rdt[ _-]?\d""", RegexOption.IGNORE_CASE),
    // WhatsApp: VID-20240101-WA0001.mp4 / WA0001.mp4
    Regex("""^vid[-_]\d{8}[-_]wa\d+""", RegexOption.IGNORE_CASE),
    Regex("""(^|[^a-z0-9])wa\d{4}([^a-z0-9]|$)""", RegexOption.IGNORE_CASE),
    // AI/social exports seen in Downloads.
    Regex("""(^|[^a-z0-9])grok[ _-]?video([^a-z0-9]|$)""", RegexOption.IGNORE_CASE),
    // Platform-branded exports. Underscores count as separators here, so
    // WhatsApp_Video_2024.mp4 matches too.
    Regex(
      """(^|[^a-z0-9])(youtube|whatsapp|telegram|instagram|snapchat|tiktok|facebook|messenger|twitter|reddit)([^a-z0-9]|$)""",
      RegexOption.IGNORE_CASE,
    ),
    // Screen recordings, with or without a separator.
    Regex(
      """(^|[^a-z0-9])screen[ _-]?record(ing|er|ed)?([^a-z0-9]|$)""",
      RegexOption.IGNORE_CASE,
    ),
    // Trailers/teasers/featurettes are non-movie extras.
    Regex(
      """(^|[^a-z0-9])(trailer|teaser|featurette)([^a-z0-9]|$)""",
      RegexOption.IGNORE_CASE,
    ),
    // Release samples.
    Regex("""(^|[^a-z0-9])sample([^a-z0-9]|$)""", RegexOption.IGNORE_CASE),
    // Camera roll exports copied out of DCIM: VID_20240101_120000.mp4,
    // PXL_20240101_120000.mp4, IMG_1234.MOV.
    Regex("""^vid[_-]\d{8}[_-]\d{6}""", RegexOption.IGNORE_CASE),
    Regex("""^pxl[_-]\d{8}""", RegexOption.IGNORE_CASE),
    Regex("""^img[_-]\d{3,4}""", RegexOption.IGNORE_CASE),
  )

  /** Folder names that explicitly hold short-form/clip content. */
  private val CLIP_FOLDER_NAMES = setOf(
    "clips",
    "shorts",
    "reels",
    "status",
    "statuses",
    "stories",
    "instagram reels",
    "youtube shorts",
  )

  /** Folders managed by messaging/social apps. */
  private val SOCIAL_FOLDER_NAMES = setOf(
    "whatsapp",
    "whatsapp video",
    "whatsapp video sent",
    "whatsapp animated gifs",
    "telegram",
    "telegram video",
    "instagram",
    "snapchat",
    "tiktok",
    "reddit",
    "facebook",
    "messenger",
    "twitter",
    "threads",
    "sharechat",
    "moj",
    "roposo",
    "likee",
    "9gag",
    "imgur",
  )

  /** Ambiguous catch-all folders where short videos are common. */
  private val TRANSIENT_FOLDER_NAMES = setOf(
    "download",
    "downloads",
    "dcim",
    "camera",
    "screen recordings",
    "screen recorder",
    "screenrecords",
    "recordings",
  )

  /**
   * Classifies an already-detected movie-library candidate.
   */
  fun classify(movie: LocalMovie): MovieLibraryKind = classify(
    fileName = movie.video.displayName,
    folderPath = movie.video.path.substringBeforeLast('/', ""),
    durationMs = movie.video.duration,
    width = movie.video.width,
    height = movie.video.height,
  )

  fun isMovieLibraryItem(movie: LocalMovie): Boolean =
    classify(movie) == MovieLibraryKind.MOVIE

  /**
   * Pure classification entry point used by the scanner-facing wrapper and by
   * unit tests. [folderPath] is the containing folder, not the file path.
   */
  fun classify(
    fileName: String,
    folderPath: String,
    durationMs: Long,
    width: Int,
    height: Int,
  ): MovieLibraryKind {
    // Missing metadata must never hide a file: MediaStore occasionally reports
    // no duration, and the filesystem fallback may not have dimensions.
    if (durationMs <= 0L) return MovieLibraryKind.MOVIE

    // A feature-length video stays a movie even in a social/Downloads folder.
    if (durationMs >= FEATURE_LENGTH_MS) return MovieLibraryKind.MOVIE

    // Too short to be a feature; safe to treat as a clip in any location.
    if (durationMs <= VERY_SHORT_MAX_MS) return MovieLibraryKind.SHORT_VIDEO

    val folderSegments = folderPath
      .lowercase(Locale.ROOT)
      .split('/', '\\')
      // Treat underscores/hyphens as spaces so Screen_Recordings and
      // whatsapp-video match the folder names below.
      .map { it.trim().replace('_', ' ').replace('-', ' ').replace(WHITESPACE_REGEX, " ") }
      .filter { it.isNotEmpty() }

    val clipName = CLIP_NAME_PATTERNS.any { it.containsMatchIn(fileName) }
    val clipFolder = folderSegments.any { it in CLIP_FOLDER_NAMES }
    val socialSource = folderSegments.any { it in SOCIAL_FOLDER_NAMES }
    val transientSource = folderSegments.any { it in TRANSIENT_FOLDER_NAMES }
    val portraitOrSquare =
      width > 0 && height > 0 && width <= height * 1.05

    return when {
      (clipName || clipFolder) && durationMs <= CLIP_SOURCE_MAX_MS ->
        MovieLibraryKind.SHORT_VIDEO

      socialSource && durationMs <= CLIP_SOURCE_MAX_MS ->
        MovieLibraryKind.SHORT_VIDEO

      // Vertical/square video of talk/reel length is short-form content.
      portraitOrSquare && durationMs <= PORTRAIT_MAX_MS ->
        MovieLibraryKind.SHORT_VIDEO

      // Downloads and camera/recording folders are ambiguous, so require a
      // shorter duration before hiding anything from Movies.
      transientSource && durationMs <= TRANSIENT_SOURCE_MAX_MS ->
        MovieLibraryKind.SHORT_VIDEO

      else -> MovieLibraryKind.MOVIE
    }
  }
}
