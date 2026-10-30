package app.aryan447.mpvium.utils.media

/**
 * Structured view of an online subtitle release name.
 *
 * Providers return raw scene names such as
 * `Spider-Man.Homecoming.2017.1080p.WEB-DL.DD5.1.H264-FGT`. Showing that
 * verbatim makes the result list hard to scan, so the useful pieces are
 * extracted here and rendered as a title plus compact quality tags.
 */
data class ParsedSubtitleRelease(
  val title: String,
  val year: String? = null,
  val resolution: String? = null,
  val source: String? = null,
  val videoCodec: String? = null,
  val audio: String? = null,
  val releaseGroup: String? = null,
  val tags: List<String> = emptyList(),
) {
  /** Compact, ordered tags for the secondary line. */
  val qualityTags: List<String>
    get() = buildList {
      resolution?.let(::add)
      source?.let(::add)
      videoCodec?.let(::add)
      audio?.let(::add)
      releaseGroup?.let(::add)
      addAll(tags)
    }
}

object SubtitleReleaseParser {

  private val SUBTITLE_EXTENSIONS = setOf(
    "srt", "ass", "ssa", "vtt", "sub", "sup", "zip", "rar", "7z", "txt"
  )

  private val YEAR_REGEX = Regex("""\b(19|20)\d{2}\b""")

  private val RESOLUTION_REGEX =
    Regex("""(?i)\b(2160p|1440p|1080p|1080i|720p|720i|576p|480p|4k|uhd)\b""")

  private val SOURCE_PATTERNS = listOf(
    Regex("""(?i)\bweb[ ._-]?dl\b""") to "WEB-DL",
    Regex("""(?i)\bweb[ ._-]?rip\b""") to "WEBRip",
    Regex("""(?i)\bblu[ ._-]?ray\b""") to "BluRay",
    Regex("""(?i)\bbdrip\b""") to "BDRip",
    Regex("""(?i)\bbrrip\b""") to "BRRip",
    Regex("""(?i)\bdvdrip\b""") to "DVDRip",
    Regex("""(?i)\bhdrip\b""") to "HDRip",
    Regex("""(?i)\bhdtv\b""") to "HDTV",
    Regex("""(?i)\bremux\b""") to "REMUX",
    Regex("""(?i)\bamzn\b""") to "AMZN",
    Regex("""(?i)\bdsnp\b""") to "DSNP",
    Regex("""(?i)\batvp\b""") to "ATVP",
    Regex("""(?i)\bhmax\b""") to "HMAX",
    Regex("""(?i)\bstan\b""") to "STAN",
    Regex("""(?i)\bhulu\b""") to "HULU",
    Regex("""(?i)\bnf\b""") to "NF",
    Regex("""(?i)\bhdts\b""") to "HDTS",
    Regex("""(?i)\bhdcam\b""") to "HDCAM",
    Regex("""(?i)\bcam\b""") to "CAM",
    Regex("""(?i)\bdvdscr\b""") to "DVDScr",
    Regex("""(?i)\btvrip\b""") to "TVRip",
  )

  private val VIDEO_CODEC_PATTERNS = listOf(
    Regex("""(?i)\bx265\b""") to "x265",
    Regex("""(?i)\bh[ ._-]?265\b""") to "H.265",
    Regex("""(?i)\bhevc\b""") to "HEVC",
    Regex("""(?i)\bx264\b""") to "x264",
    Regex("""(?i)\bh[ ._-]?264\b""") to "H.264",
    Regex("""(?i)\bavc\b""") to "AVC",
    Regex("""(?i)\bav1\b""") to "AV1",
    Regex("""(?i)\bxvid\b""") to "XviD",
    Regex("""(?i)\bdivx\b""") to "DivX",
    Regex("""(?i)\bvp9\b""") to "VP9",
  )

  private val AUDIO_PATTERNS = listOf(
    Regex("""(?i)\bdts[ ._-]?hd[ ._-]?ma\b""") to "DTS-HD MA",
    Regex("""(?i)\bdts[ ._-]?hd\b""") to "DTS-HD",
    Regex("""(?i)\bdts[ ._-]?x\b""") to "DTS:X",
    Regex("""(?i)\btruehd\b""") to "TrueHD",
    Regex("""(?i)\batmos\b""") to "Atmos",
    Regex("""(?i)\bddp[ ._-]?5[ ._-]?1\b""") to "DDP5.1",
    Regex("""(?i)\bdd\+[ ._-]?5[ ._-]?1\b""") to "DDP5.1",
    Regex("""(?i)\bdd[ ._-]?5[ ._-]?1\b""") to "DD5.1",
    Regex("""(?i)\beac3\b""") to "EAC3",
    Regex("""(?i)\bac3\b""") to "AC3",
    Regex("""(?i)\baac[ ._-]?5[ ._-]?1\b""") to "AAC5.1",
    Regex("""(?i)\baac\b""") to "AAC",
    Regex("""(?i)\bmp3\b""") to "MP3",
    Regex("""(?i)\bflac\b""") to "FLAC",
    Regex("""(?i)\bopus\b""") to "Opus",
  )

  private val CHANNEL_REGEX = Regex("""(?i)\b([257])[ ._-]([01])\b""")

  private val TAG_PATTERNS = listOf(
    Regex("""(?i)\bproper\b""") to "PROPER",
    Regex("""(?i)\brepack\b""") to "REPACK",
    Regex("""(?i)\bextended\b""") to "EXTENDED",
    Regex("""(?i)\bunrated\b""") to "UNRATED",
    Regex("""(?i)\bimax\b""") to "IMAX",
    Regex("""(?i)\bremastered\b""") to "REMASTERED",
    Regex("""(?i)\buncut\b""") to "UNCUT",
    Regex("""(?i)\blimited\b""") to "LIMITED",
    Regex("""(?i)\bremux\b""") to "REMUX",
    Regex("""(?i)\bhdr10\+\b""") to "HDR10+",
    Regex("""(?i)\bhdr10\b""") to "HDR10",
    Regex("""(?i)\bhdr\b""") to "HDR",
    Regex("""(?i)\bdv\b""") to "DV",
    Regex("""(?i)\b10[ ._-]?bit\b""") to "10-bit",
    Regex("""(?i)\b8[ ._-]?bit\b""") to "8-bit",
    Regex("""(?i)\bmulti\b""") to "MULTI",
    Regex("""(?i)\bdual\b""") to "DUAL",
  )

  // Release groups are normally a short alphanumeric suffix after a dash, or
  // a bracketed tag. Dots are excluded so a title like
  // "Spider-Man.Homecoming" cannot be mistaken for a group.
  private val TRAILING_GROUP_REGEX = Regex("""-([A-Za-z][A-Za-z0-9_]{1,19})$""")
  private val BRACKET_GROUP_REGEX = Regex("""[\[\{]([A-Za-z][A-Za-z0-9._-]{1,19})[\]\}]\s*$""")
  private val CRC32_REGEX = Regex("""^[A-Fa-f0-9]{8}$""")

  private val NON_GROUP_TOKENS = setOf(
    "x264", "x265", "h264", "h265", "hevc", "avc", "av1", "xvid", "divx", "vp9",
    "web", "dl", "webdl", "webrip", "bluray", "bdrip", "brrip", "dvdrip", "hdrip",
    "hdtv", "remux", "aac", "ac3", "eac3", "dts", "dd", "ddp", "truehd", "atmos",
    "mp3", "flac", "opus", "proper", "repack", "extended", "unrated", "imax",
    "hdr", "hdr10", "10bit", "8bit", "multi", "dual", "sdr", "uhd", "4k",
  )

  fun parse(displayName: String): ParsedSubtitleRelease {
    val raw = displayName.trim()
    if (raw.isBlank()) return ParsedSubtitleRelease(title = "")

    // SubSource joins several matching release names with " / ". Parse each one
    // and show the most useful (highest resolution, then most metadata) instead
    // of mixing tags from unrelated releases.
    val segments = raw.split(" / ").map { it.trim() }.filter { it.isNotBlank() }
    if (segments.size > 1) {
      return segments
        .map(::parseSingle)
        .maxByOrNull(::qualityScore)
        ?: parseSingle(raw)
    }
    return parseSingle(raw)
  }

  private fun qualityScore(release: ParsedSubtitleRelease): Int {
    val resolutionScore = when (release.resolution) {
      "2160p", "4K" -> 4
      "1440p" -> 3
      "1080p" -> 2
      "720p" -> 1
      else -> 0
    }
    return resolutionScore * 100 + release.qualityTags.size
  }

  private fun parseSingle(displayName: String): ParsedSubtitleRelease {
    val raw = displayName.trim()
    if (raw.isBlank()) return ParsedSubtitleRelease(title = "")

    val name = raw.substringAfterLast('/').substringAfterLast('\\')
    val cleaned = stripExtension(name)
    val info = MediaInfoParser.parse(cleaned)
    val fallbackTitle = cleaned
      .replace(Regex("""[._]+"""), " ")
      .replace(Regex("""\s+"""), " ")
      .trim()
    val title = info.title.ifBlank { fallbackTitle }.trim()

    val resolution = RESOLUTION_REGEX.find(cleaned)?.value?.let(::normalizeResolution)
    val source = findMapped(cleaned, SOURCE_PATTERNS)
    val videoCodec = findMapped(cleaned, VIDEO_CODEC_PATTERNS)
    val audio = findMapped(cleaned, AUDIO_PATTERNS)
      ?: CHANNEL_REGEX.find(cleaned)?.let { "${it.groupValues[1]}.${it.groupValues[2]}" }
    val releaseGroup = extractReleaseGroup(cleaned)
    val tags = TAG_PATTERNS.mapNotNull { (regex, label) ->
      label.takeIf { regex.containsMatchIn(cleaned) }
    }.distinct()

    return ParsedSubtitleRelease(
      title = title,
      year = info.year,
      resolution = resolution,
      source = source,
      videoCodec = videoCodec,
      audio = audio,
      releaseGroup = releaseGroup,
      tags = tags,
    )
  }

  private fun stripExtension(name: String): String {
    val extension = name.substringAfterLast('.', "").lowercase()
    return if (extension in SUBTITLE_EXTENSIONS) name.substringBeforeLast('.') else name
  }

  private fun normalizeResolution(value: String): String = when (value.lowercase()) {
    "4k", "uhd" -> "4K"
    else -> value.lowercase()
  }

  private fun findMapped(text: String, patterns: List<Pair<Regex, String>>): String? =
    patterns.firstOrNull { (regex, _) -> regex.containsMatchIn(text) }?.second

  private fun extractReleaseGroup(name: String): String? {
    val match = TRAILING_GROUP_REGEX.find(name) ?: BRACKET_GROUP_REGEX.find(name)
    val candidate = match?.groupValues?.getOrNull(1)?.trim() ?: return null
    if (candidate.length < 2) return null
    if (CRC32_REGEX.matches(candidate)) return null

    val normalized = candidate.lowercase().replace(Regex("""[._-]"""), "")
    if (normalized in NON_GROUP_TOKENS) return null

    // Only treat a dash suffix as a group when the name actually contains
    // release metadata; otherwise "Movie-Title" would lose its title.
    val hasMetadata = YEAR_REGEX.containsMatchIn(name) ||
      RESOLUTION_REGEX.containsMatchIn(name) ||
      SOURCE_PATTERNS.any { (regex, _) -> regex.containsMatchIn(name) } ||
      VIDEO_CODEC_PATTERNS.any { (regex, _) -> regex.containsMatchIn(name) } ||
      AUDIO_PATTERNS.any { (regex, _) -> regex.containsMatchIn(name) }
    if (!hasMetadata) return null

    return candidate
  }
}
