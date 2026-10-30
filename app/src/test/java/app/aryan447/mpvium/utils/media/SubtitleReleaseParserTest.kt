package app.aryan447.mpvium.utils.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleReleaseParserTest {

  @Test
  fun parsesDottedSceneRelease() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man.Homecoming.2017.1080p.WEB-DL.DD5.1.H264-FGT"
    )

    assertEquals("Spider-Man Homecoming", parsed.title)
    assertEquals("2017", parsed.year)
    assertEquals("1080p", parsed.resolution)
    assertEquals("WEB-DL", parsed.source)
    assertEquals("H.264", parsed.videoCodec)
    assertEquals("DD5.1", parsed.audio)
    assertEquals("FGT", parsed.releaseGroup)
  }

  @Test
  fun parsesSpacedReleaseAndNoGRP() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man Homecoming 2017 1080p WEB-DL DD5.1 H264-NoGRP"
    )

    assertEquals("Spider-Man Homecoming", parsed.title)
    assertEquals("2017", parsed.year)
    assertEquals("1080p", parsed.resolution)
    assertEquals("WEB-DL", parsed.source)
    assertEquals("H.264", parsed.videoCodec)
    assertEquals("DD5.1", parsed.audio)
    assertEquals("NoGRP", parsed.releaseGroup)
  }

  @Test
  fun parsesReleaseWithoutResolution() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man.Homecoming.2017.WEB-DL.x264-FGT"
    )

    assertEquals("Spider-Man Homecoming", parsed.title)
    assertNull(parsed.resolution)
    assertEquals("WEB-DL", parsed.source)
    assertEquals("x264", parsed.videoCodec)
    assertEquals("FGT", parsed.releaseGroup)
  }

  @Test
  fun parsesLegacyCodecAndAudioTags() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man.Homecoming.2017.WEB-DL.XviD.MP3-FGT"
    )

    assertEquals("XviD", parsed.videoCodec)
    assertEquals("MP3", parsed.audio)
    assertEquals("FGT", parsed.releaseGroup)
  }

  @Test
  fun parsesCompoundAudioAndEditionTags() {
    val parsed = SubtitleReleaseParser.parse(
      "The.Matrix.1999.2160p.UHD.BluRay.REMUX.DTS-HD.MA.7.1.Atmos-GROUP"
    )

    assertEquals("The Matrix", parsed.title)
    assertEquals("1999", parsed.year)
    assertEquals("2160p", parsed.resolution)
    assertEquals("BluRay", parsed.source)
    assertEquals("DTS-HD MA", parsed.audio)
    assertEquals("GROUP", parsed.releaseGroup)
    assertEquals(listOf("REMUX"), parsed.tags)
  }

  @Test
  fun keepsYearLikeTitleNumbers() {
    val parsed = SubtitleReleaseParser.parse(
      "1917.2019.1080p.BluRay.x264-GROUP"
    )

    assertEquals("1917", parsed.title)
    assertEquals("2019", parsed.year)
    assertEquals("BluRay", parsed.source)
    assertEquals("x264", parsed.videoCodec)
  }

  @Test
  fun parsesSeriesRelease() {
    val parsed = SubtitleReleaseParser.parse(
      "Some.Show.S01E02.720p.WEBRip.x265-NTb"
    )

    assertEquals("Some Show", parsed.title)
    assertEquals("720p", parsed.resolution)
    assertEquals("WEBRip", parsed.source)
    assertEquals("x265", parsed.videoCodec)
    assertEquals("NTb", parsed.releaseGroup)
  }

  @Test
  fun parsesBracketReleaseGroup() {
    val parsed = SubtitleReleaseParser.parse(
      "Movie.2019.1080p.BluRay.x264-[YTS.MX]"
    )

    assertEquals("YTS.MX", parsed.releaseGroup)
  }

  @Test
  fun handlesPlainSubtitleFileName() {
    val parsed = SubtitleReleaseParser.parse("No.Metadata.Here.srt")

    assertEquals("No Metadata Here", parsed.title)
    assertNull(parsed.year)
    assertNull(parsed.resolution)
    assertNull(parsed.source)
    assertNull(parsed.videoCodec)
    assertNull(parsed.audio)
    assertNull(parsed.releaseGroup)
    assertEquals(emptyList<String>(), parsed.tags)
  }

  @Test
  fun picksBestReleaseFromJoinedProviderNames() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man.Homecoming.2017.WEB-DL.x264-FGT / " +
        "Spider-Man.Homecoming.2017.720p.WEB-DL.XviD.AC3-FGT / " +
        "Spider-Man.Homecoming.2017.1080p.WEB-DL.DD5.1.H264-FGT"
    )

    assertEquals("Spider-Man Homecoming", parsed.title)
    assertEquals("2017", parsed.year)
    assertEquals("1080p", parsed.resolution)
    assertEquals("WEB-DL", parsed.source)
    assertEquals("H.264", parsed.videoCodec)
    assertEquals("DD5.1", parsed.audio)
    assertEquals("FGT", parsed.releaseGroup)
  }

  @Test
  fun parsesDownloadedFileName() {
    val parsed = SubtitleReleaseParser.parse(
      "Spider-Man.Homecoming.2017.en.FGT.srt"
    )

    assertEquals("Spider-Man Homecoming", parsed.title)
    assertEquals("2017", parsed.year)
  }
}
