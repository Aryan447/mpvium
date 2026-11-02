package app.aryan447.mpvium.utils.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleReleaseParserTest {

  @Test
  fun parsesSceneReleaseWithSourceAndGroup() {
    val parsed = SubtitleReleaseParser.parse("Spider-Man.Homecoming.2017.WEB-DL.x264-FGT")

    assertEquals("Spider-Man Homecoming", parsed.title)
    assertEquals("2017", parsed.year)
    assertNull(parsed.quality)
    assertEquals("WEB-DL", parsed.source)
    assertEquals("x264", parsed.videoCodec)
    assertEquals("FGT", parsed.releaseGroup)
    assertEquals("Spider-Man Homecoming (2017)", parsed.displayTitle)
  }

  @Test
  fun parsesQualityCodecAndAudio() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man.Homecoming.2017.720p.WEB-DL.XviD.AC3-FGT",
    )

    assertEquals("720p", parsed.quality)
    assertEquals("WEB-DL", parsed.source)
    assertEquals("XviD", parsed.videoCodec)
    assertEquals("AC3", parsed.audio)
    assertEquals("FGT", parsed.releaseGroup)
    assertEquals(
      listOf("720p", "WEB-DL", "XviD", "AC3", "FGT"),
      parsed.technicalTags,
    )
  }

  @Test
  fun parsesDd51AudioAndH264Codec() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man.Homecoming.2017.1080p.WEB-DL.DD5.1.H264-FGT",
    )

    assertEquals("1080p", parsed.quality)
    assertEquals("DD5.1", parsed.audio)
    assertEquals("H.264", parsed.videoCodec)
    assertEquals("FGT", parsed.releaseGroup)
  }

  @Test
  fun parsesSpaceSeparatedReleaseName() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man Homecoming 2017 1080p WEB-DL DD5.1 H264-NoGRP",
    )

    assertEquals("Spider-Man Homecoming", parsed.title)
    assertEquals("2017", parsed.year)
    assertEquals("1080p", parsed.quality)
    assertEquals("WEB-DL", parsed.source)
    assertEquals("DD5.1", parsed.audio)
    assertEquals("H.264", parsed.videoCodec)
    assertEquals("NoGRP", parsed.releaseGroup)
  }

  @Test
  fun parsesReleaseGroupAfterAudioCodec() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man.Homecoming.2017.1080p.WEB-DL.H264.AC3-EVO",
    )

    assertEquals("EVO", parsed.releaseGroup)
  }

  @Test
  fun doesNotTreatTitleHyphenAsReleaseGroup() {
    val parsed = SubtitleReleaseParser.parse("Spider-Man")

    assertEquals("Spider-Man", parsed.title)
    assertNull(parsed.releaseGroup)
  }

  @Test
  fun usesOnlyFirstOfSeveralJoinedReleases() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man.Homecoming.2017.1080p.WEB-DL / Spider-Man.Homecoming.2017.720p.WEB-DL",
    )

    assertEquals("1080p", parsed.quality)
    assertEquals("Spider-Man Homecoming", parsed.title)
  }

  @Test
  fun stripsSubtitleExtensionAndLanguageSuffix() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man.Homecoming.2017.1080p.WEB-DL.H264-GROUP.en.srt",
    )

    assertEquals("Spider-Man Homecoming", parsed.title)
    assertEquals("1080p", parsed.quality)
    assertEquals("WEB-DL", parsed.source)
    assertEquals("H.264", parsed.videoCodec)
    assertEquals("GROUP", parsed.releaseGroup)
  }

  @Test
  fun stripsLanguageSuffixWithoutYear() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man.Homecoming.1080p.WEB-DL.en.srt",
    )

    assertEquals("Spider-Man Homecoming", parsed.title)
    assertEquals("1080p", parsed.quality)
    assertEquals("WEB-DL", parsed.source)
  }

  @Test
  fun parsesAnimeBracketGroup() {
    val parsed = SubtitleReleaseParser.parse("[SubsPlease] Frieren - 01 (1080p) [ABCD1234].ass")

    assertEquals("SubsPlease", parsed.releaseGroup)
    assertEquals("1080p", parsed.quality)
  }

  @Test
  fun fallsBackToRawNameWhenNothingCanBeParsed() {
    val parsed = SubtitleReleaseParser.parse("English")

    assertEquals("English", parsed.title)
    assertEquals("English", parsed.displayTitle)
    assertNull(parsed.releaseGroup)
  }
}
