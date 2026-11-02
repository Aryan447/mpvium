package app.aryan447.mpvium.utils.media

/**
 * Structured, human-readable view of a subtitle release name.
 *
 * Online providers return raw scene names such as
 * `Spider-Man.Homecoming.2017.1080p.WEB-DL.DD5.1.H264-FGT`, sometimes joined
 * with several alternative releases. Showing that verbatim produces a wall of
 * text, so the search sheet parses it into a clean title plus the technical
 * tags users actually compare (quality, source, codec, group).
 */
data class ParsedSubtitleRelease(
  val releaseName: String = "",
  val title: String = "",
  val year: String? = null,
  val season: Int? = null,
  val episode: Int? = null,
  val quality: String? = null,
  val source: String? = null,
  val videoCodec: String? = null,
  val audio: String? = null,
  val releaseGroup: String? = null,
  val edition: String? = null,
) {
  /** Tags worth showing as pills, in the order users scan them. */
  val technicalTags: List<String>
    get() = listOfNotNull(quality, source, videoCodec, audio, edition, releaseGroup)

  val hasTechnicalTags: Boolean get() = technicalTags.isNotEmpty()

  val displayTitle: String
    get() = when {
      title.isBlank() -> releaseName
      year.isNullOrBlank() -> title
      else -> "$title ($year)"
    }
}

object SubtitleReleaseParser {
  private val JOINED_RELEASES = Regex("""\s+/\s+""")
  private val SUBTITLE_EXTENSION = Regex(
    """\.(?:srt|ass|ssa|vtt|sub|smi|sami|mpl|txt|lrc|sup|idx)$""",
    RegexOption.IGNORE_CASE,
  )
  private val BRACKET_PREFIX = Regex("""^\s*\[([^\]]{1,40})\]\s*""")
  private val BRACKET_GROUP = Regex("""\[[^\]]*]""")
  private val TRAILING_GROUP = Regex("""-([A-Za-z][A-Za-z0-9]{1,19})$""")
  private val TRAILING_LANGUAGE = Regex("""(?i)[._-]([a-z]{2,3})$""")

  private val QUALITY = Regex(
    """(?i)(?<![A-Za-z0-9])(2160p|1080p|720p|576p|480p|4k|uhd)(?![A-Za-z0-9])""",
  )
  private val SOURCE = Regex(
    """(?i)(?<![A-Za-z0-9])(web[-. ]?dl|web[-. ]?rip|blu[-. ]?ray|bdrip|brrip|hdrip|hdtv|dvdrip|dvdscr|remux|amzn|dsnp|atvp|hmax|stan|nf)(?![A-Za-z0-9])""",
  )
  private val VIDEO_CODEC = Regex(
    """(?i)(?<![A-Za-z0-9])(x264|x265|h[.]?264|h[.]?265|hevc|av1|xvid|divx|vp9)(?![A-Za-z0-9])""",
  )
  private val AUDIO_CODEC = Regex(
    """(?i)(?<![A-Za-z0-9])(dd\+?[ .]?5[ .]1|ddp[ .]?5[ .]1|eac3|ac3|dts(?:[-. ]?hd)?|truehd|atmos|aac|flac|opus)(?![A-Za-z0-9])""",
  )
  private val EDITION = Regex(
    """(?i)(?<![A-Za-z0-9])(extended|unrated|director'?s[ .]?cut|remastered|imax|theatrical)(?![A-Za-z0-9])""",
  )

  /** Tokens that look like a group but are really release metadata. */
  private val TECHNICAL_STOPWORDS = setOf(
    "dl", "rip", "web", "webdl", "webrip", "hd", "uhd", "sd", "hdr", "sdr",
    "hdr10", "dv", "10bit", "8bit", "proper", "repack", "remux", "multi",
    "dual", "audio", "subs", "sub", "mkv", "mp4", "avi", "mka",
  )

  /** Common subtitle language suffixes (`.en.srt`, `-eng.srt`, …). */
  private val LANGUAGE_SUFFIXES = setOf(
    "en", "eng", "es", "spa", "fr", "fre", "fra", "de", "ger", "deu",
    "it", "ita", "pt", "por", "pob", "ru", "rus", "ja", "jpn", "ko", "kor",
    "zh", "chi", "zho", "ar", "ara", "hi", "hin", "bn", "ben", "pa", "pan",
    "ta", "tam", "te", "tel", "ur", "urd", "tr", "tur", "pl", "pol", "uk",
    "ukr", "nl", "dut", "nld", "el", "gre", "ell", "hu", "hun", "sv", "swe",
    "cs", "cze", "ces", "ro", "rum", "ron", "da", "dan", "fi", "fin", "no",
    "nor", "he", "heb", "id", "ind", "ms", "may", "msa", "th", "tha", "fa",
    "per", "fas", "vi", "vie", "forced", "sdh", "cc",
  )

  /**
   * Parses [rawName], which may be a single release name or several releases
   * joined with `" / "` (SubSource does this). Only the first release is
   * inspected; the rest are alternatives for the same subtitle.
   */
  fun parse(rawName: String): ParsedSubtitleRelease {
    val joined = rawName.trim()
    val primary = joined.split(JOINED_RELEASES).firstOrNull()?.trim()
      ?.takeIf { it.isNotBlank() }
      ?: joined
    val withoutExtension = primary.replace(SUBTITLE_EXTENSION, "").trim()
    val groupName = stripLanguageSuffix(withoutExtension)
    val releaseGroup = bracketGroup(withoutExtension) ?: trailingGroup(groupName)
    val titleSource = releaseGroup?.let { group ->
      groupName.replace(Regex("""(?i)-${Regex.escape(group)}$"""), "")
    } ?: groupName

    val mediaInfo = runCatching { MediaInfoParser.parse(titleSource) }.getOrNull()
    val title = mediaInfo?.title?.takeIf { it.isNotBlank() }
      ?: fallbackTitle(withoutExtension)

    return ParsedSubtitleRelease(
      releaseName = withoutExtension,
      title = title,
      year = mediaInfo?.year,
      season = mediaInfo?.season,
      episode = mediaInfo?.episode,
      quality = QUALITY.find(withoutExtension)?.value?.let(::normalizeQuality),
      source = SOURCE.find(withoutExtension)?.value?.let(::normalizeSource),
      videoCodec = VIDEO_CODEC.find(withoutExtension)?.value?.let(::normalizeCodec),
      audio = AUDIO_CODEC.find(withoutExtension)?.value?.let(::normalizeAudio),
      releaseGroup = releaseGroup,
      edition = EDITION.find(withoutExtension)?.value?.let(::normalizeEdition),
    )
  }

  private fun bracketGroup(name: String): String? =
    BRACKET_PREFIX.find(name)?.groupValues?.get(1)?.trim()
      ?.takeIf { it.isNotBlank() && !isTechnicalToken(it) }

  private fun trailingGroup(name: String): String? {
    val candidate = TRAILING_GROUP.find(name)?.groupValues?.get(1) ?: return null
    if (isTechnicalToken(candidate)) return null
    val knownGroup = MediaInfoParser.RELEASE_GROUPS.any { it.equals(candidate, ignoreCase = true) }
    val looksLikeSceneGroup =
      candidate.any { it.isDigit() } || candidate.all { !it.isLowerCase() }
    return candidate.takeIf { knownGroup || looksLikeSceneGroup }
  }

  private fun stripLanguageSuffix(name: String): String {
    var result = name
    repeat(2) {
      val match = TRAILING_LANGUAGE.find(result) ?: return result
      if (match.groupValues[1].lowercase() !in LANGUAGE_SUFFIXES) return result
      result = result.substring(0, match.range.first)
    }
    return result
  }

  private fun isTechnicalToken(token: String): Boolean {
    val lower = token.lowercase()
    if (lower in TECHNICAL_STOPWORDS) return true
    return QUALITY.containsMatchIn(token) ||
      SOURCE.containsMatchIn(token) ||
      VIDEO_CODEC.containsMatchIn(token) ||
      AUDIO_CODEC.containsMatchIn(token)
  }

  private fun fallbackTitle(name: String): String =
    name.replace(BRACKET_GROUP, " ")
      .replace(Regex("""[._]+"""), " ")
      .replace(Regex("""\s+"""), " ")
      .trim()

  private fun normalizeQuality(raw: String): String = when (raw.lowercase()) {
    "4k", "uhd" -> "4K"
    else -> raw.lowercase()
  }

  private fun normalizeSource(raw: String): String {
    val compact = raw.lowercase().replace(".", "").replace("-", "").replace(" ", "")
    return when (compact) {
      "webdl" -> "WEB-DL"
      "webrip" -> "WEBRip"
      "bluray" -> "BluRay"
      "bdrip" -> "BDRip"
      "brrip" -> "BRRip"
      "hdrip" -> "HDRip"
      "hdtv" -> "HDTV"
      "dvdrip" -> "DVDRip"
      "dvdscr" -> "DVDScr"
      "remux" -> "REMUX"
      "amzn" -> "AMZN"
      "dsnp" -> "DSNP"
      "atvp" -> "ATVP"
      "hmax" -> "HMAX"
      "stan" -> "STAN"
      "nf" -> "NF"
      else -> raw.uppercase()
    }
  }

  private fun normalizeCodec(raw: String): String = when (raw.lowercase().replace(".", "")) {
    "h264" -> "H.264"
    "h265" -> "H.265"
    "x264" -> "x264"
    "x265" -> "x265"
    "hevc" -> "HEVC"
    "av1" -> "AV1"
    "xvid" -> "XviD"
    "divx" -> "DivX"
    "vp9" -> "VP9"
    else -> raw
  }

  private fun normalizeAudio(raw: String): String {
    val compact = raw.lowercase().replace(".", "").replace("-", "").replace(" ", "")
    return when {
      compact.startsWith("ddp") || compact.startsWith("dd+") -> "DD+5.1"
      compact.startsWith("dd5") -> "DD5.1"
      compact == "eac3" -> "EAC3"
      compact == "ac3" -> "AC3"
      compact == "dtshd" -> "DTS-HD"
      compact == "dts" -> "DTS"
      compact == "truehd" -> "TrueHD"
      compact == "atmos" -> "Atmos"
      compact == "aac" -> "AAC"
      compact == "flac" -> "FLAC"
      compact == "opus" -> "Opus"
      else -> raw.uppercase()
    }
  }

  private fun normalizeEdition(raw: String): String = when (raw.lowercase().replace(".", "").replace(" ", "")) {
    "extended" -> "Extended"
    "unrated" -> "Unrated"
    "directorscut" -> "Director's Cut"
    "remastered" -> "Remastered"
    "imax" -> "IMAX"
    "theatrical" -> "Theatrical"
    else -> raw
  }
}
