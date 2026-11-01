package app.aryan447.mpvium.utils.media

import app.aryan447.mpvium.domain.streaming.SeriesDetector
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaInfoParserTest {

  private data class ParseTestCase(
    val fileName: String,
    val expectedTitle: String,
    val expectedYear: String?,
    val expectedSeason: Int?,
    val expectedEpisode: Int?,
  )

  @Test
  fun realFilenameParseTestTable_atLeastTwentyRealFilenames() {
    val testTable = listOf(
      // 1. Regular TV series
      ParseTestCase(
        fileName = "Dexter.S01E02.1080p.BluRay.mkv",
        expectedTitle = "Dexter",
        expectedYear = null,
        expectedSeason = 1,
        expectedEpisode = 2,
      ),
      ParseTestCase(
        fileName = "Breaking.Bad.S05E16.Felina.720p.mkv",
        expectedTitle = "Breaking Bad",
        expectedYear = null,
        expectedSeason = 5,
        expectedEpisode = 16,
      ),
      ParseTestCase(
        fileName = "Game.of.Thrones.S08E03.1080p.WEB-DL.mkv",
        expectedTitle = "Game of Thrones",
        expectedYear = null,
        expectedSeason = 8,
        expectedEpisode = 3,
      ),

      // 2. TV series with year in title
      ParseTestCase(
        fileName = "Doctor.Who.2005.S01E01.Rose.1080p.mkv",
        expectedTitle = "Doctor Who",
        expectedYear = "2005",
        expectedSeason = 1,
        expectedEpisode = 1,
      ),

      // 3. Cross-format episode
      ParseTestCase(
        fileName = "The.Office.US.02x05.Halloween.mkv",
        expectedTitle = "The Office US",
        expectedYear = null,
        expectedSeason = 2,
        expectedEpisode = 5,
      ),

      // 4. Multi-episode files (first episode wins deterministically, no garbage numbers)
      ParseTestCase(
        fileName = "Friends.S06E15E16.The.One.That.Could.Have.Been.mkv",
        expectedTitle = "Friends",
        expectedYear = null,
        expectedSeason = 6,
        expectedEpisode = 15,
      ),
      ParseTestCase(
        fileName = "Avatar.The.Last.Airbender.S03E20-E21.Sozins.Comet.mkv",
        expectedTitle = "Avatar The Last Airbender",
        expectedYear = null,
        expectedSeason = 3,
        expectedEpisode = 20,
      ),
      ParseTestCase(
        fileName = "Star.Trek.TNG.S01E01-02.Encounter.at.Farpoint.mkv",
        expectedTitle = "Star Trek TNG",
        expectedYear = null,
        expectedSeason = 1,
        expectedEpisode = 1,
      ),
      ParseTestCase(
        fileName = "Seinfeld.04x11-12.The.Contest.mkv",
        expectedTitle = "Seinfeld",
        expectedYear = null,
        expectedSeason = 4,
        expectedEpisode = 11,
      ),
      ParseTestCase(
        fileName = "[SubsPlease] Frieren - 01-02 (1080p).mkv",
        expectedTitle = "Frieren",
        expectedYear = null,
        expectedSeason = 1,
        expectedEpisode = 1,
      ),

      // 5. Anime with fansub tags, CRC hashes, and quality tags
      ParseTestCase(
        fileName = "[SubsPlease] Frieren - Beyond Journey's End - 01 (1080p) [ABCD1234].mkv",
        expectedTitle = "Frieren Beyond Journey's End",
        expectedYear = null,
        expectedSeason = 1,
        expectedEpisode = 1,
      ),
      ParseTestCase(
        fileName = "[HorribleSubs] Hunter X Hunter - 135 [720p].mkv",
        expectedTitle = "Hunter X Hunter",
        expectedYear = null,
        expectedSeason = 1,
        expectedEpisode = 135,
      ),
      ParseTestCase(
        fileName = "[Erai-raws] Kimetsu no Yaiba - 26 [1080p][Multiple Subtitle].mkv",
        expectedTitle = "Kimetsu no Yaiba",
        expectedYear = null,
        expectedSeason = 1,
        expectedEpisode = 26,
      ),
      ParseTestCase(
        fileName = "【Fansub】 Spy x Family - 05 【1080p】.mkv",
        expectedTitle = "Spy x Family",
        expectedYear = null,
        expectedSeason = 1,
        expectedEpisode = 5,
      ),
      ParseTestCase(
        fileName = "[ASW] Vinland Saga - 24 [1080p x265 10Bit][E-AC3].mkv",
        expectedTitle = "Vinland Saga",
        expectedYear = null,
        expectedSeason = 1,
        expectedEpisode = 24,
      ),

      // 6. Dash-episode detection when year is present
      ParseTestCase(
        fileName = "Jujutsu Kaisen (2020) - 05.mkv",
        expectedTitle = "Jujutsu Kaisen",
        expectedYear = "2020",
        expectedSeason = 1,
        expectedEpisode = 5,
      ),
      ParseTestCase(
        fileName = "Kaguya-sama 2019 - 11.mkv",
        expectedTitle = "Kaguya-sama",
        expectedYear = "2019",
        expectedSeason = 1,
        expectedEpisode = 11,
      ),
      ParseTestCase(
        fileName = "[SubsPlease] Jujutsu Kaisen (2020) - 05 (1080p) [12345678].mkv",
        expectedTitle = "Jujutsu Kaisen",
        expectedYear = "2020",
        expectedSeason = 1,
        expectedEpisode = 5,
      ),

      // 7. Title vs Year: numbers in title kept, release year taken from parentheses
      ParseTestCase(
        fileName = "Blade Runner 2049 (2017).1080p.BluRay.mkv",
        expectedTitle = "Blade Runner 2049",
        expectedYear = "2017",
        expectedSeason = null,
        expectedEpisode = null,
      ),
      ParseTestCase(
        fileName = "1917 (2019).mkv",
        expectedTitle = "1917",
        expectedYear = "2019",
        expectedSeason = null,
        expectedEpisode = null,
      ),
      ParseTestCase(
        fileName = "2012 (2009).mkv",
        expectedTitle = "2012",
        expectedYear = "2009",
        expectedSeason = null,
        expectedEpisode = null,
      ),

      // 8. Never return bare year as title
      ParseTestCase(
        fileName = "2019.mkv",
        expectedTitle = "",
        expectedYear = "2019",
        expectedSeason = null,
        expectedEpisode = null,
      ),

      // 9. Regular movies: keep movie titles safe
      ParseTestCase(
        fileName = "The.Dark.Knight.2008.1080p.BluRay.mkv",
        expectedTitle = "The Dark Knight",
        expectedYear = "2008",
        expectedSeason = null,
        expectedEpisode = null,
      ),
      ParseTestCase(
        fileName = "Spider-Man (2002).mkv",
        expectedTitle = "Spider-Man",
        expectedYear = "2002",
        expectedSeason = null,
        expectedEpisode = null,
      ),

      // 10. Non-Latin titles
      ParseTestCase(
        fileName = "[SubsPlease] 葬送のフリーレン - 01 (1080p) [ABCD1234].mkv",
        expectedTitle = "葬送のフリーレン",
        expectedYear = null,
        expectedSeason = 1,
        expectedEpisode = 1,
      ),
      ParseTestCase(
        fileName = "【Sub】進撃の巨人 The Final Season - 01.mkv",
        expectedTitle = "進撃の巨人 The Final Season",
        expectedYear = null,
        expectedSeason = 1,
        expectedEpisode = 1,
      ),
      ParseTestCase(
        fileName = "기생충 (Parasite) (2019).mkv",
        expectedTitle = "기생충 Parasite",
        expectedYear = "2019",
        expectedSeason = null,
        expectedEpisode = null,
      ),

      // 11. Folder fallbacks
      ParseTestCase(
        fileName = "Show/Season 2/01 - Title.mkv",
        expectedTitle = "Show",
        expectedYear = null,
        expectedSeason = 2,
        expectedEpisode = 1,
      ),
      ParseTestCase(
        fileName = "Show/01x02.mkv",
        expectedTitle = "Show",
        expectedYear = null,
        expectedSeason = 1,
        expectedEpisode = 2,
      ),
      ParseTestCase(
        fileName = "Inception (2010)/movie.mkv",
        expectedTitle = "Inception",
        expectedYear = "2010",
        expectedSeason = null,
        expectedEpisode = null,
      ),
      ParseTestCase(
        fileName = "Spider-Man (2002)/movie.mkv",
        expectedTitle = "Spider-Man",
        expectedYear = "2002",
        expectedSeason = null,
        expectedEpisode = null,
      ),
    )

    for (testCase in testTable) {
      val parsed = MediaInfoParser.parse(testCase.fileName)
      assertEquals(
        "Title mismatch for: ${testCase.fileName}",
        testCase.expectedTitle,
        parsed.title,
      )
      assertEquals(
        "Year mismatch for: ${testCase.fileName}",
        testCase.expectedYear,
        parsed.year,
      )
      assertEquals(
        "Season mismatch for: ${testCase.fileName}",
        testCase.expectedSeason,
        parsed.season,
      )
      assertEquals(
        "Episode mismatch for: ${testCase.fileName}",
        testCase.expectedEpisode,
        parsed.episode,
      )
    }
  }

  @Test
  fun groupingLevelCases_messyFilenamesCollapseToSingleSeriesId() {
    val frierenFilenames = listOf(
      "Frieren: Beyond Journey's End",
      "[SubsPlease] Frieren - Beyond Journey's End - 01 (1080p) [ABCD1234].mkv",
      "Frieren_Beyond_Journeys_End",
      "【Fansub】 Frieren - Beyond Journey's End 【1080p】",
      "Frieren: Beyond Journey's End (2023)",
      "Frieren.Beyond.Journeys.End.S01E01.1080p.mkv",
      MediaInfoParser.parse("[SubsPlease] Frieren - Beyond Journey's End - 01 (1080p) [ABCD1234].mkv").title,
    )

    val expectedFrierenKey = "frierenbeyondjourneysend"
    for (filename in frierenFilenames) {
      val key = SeriesDetector.normalizeKey(filename)
      assertEquals(
        "Normalized key mismatch for Frieren filename: $filename",
        expectedFrierenKey,
        key,
      )
    }

    val jujutsuFilenames = listOf(
      "Jujutsu Kaisen",
      "Jujutsu Kaisen (2020) - 05.mkv",
      "[SubsPlease] Jujutsu Kaisen (2020) - 05 (1080p) [12345678].mkv",
      "Jujutsu_Kaisen_2020",
      "Jujutsu.Kaisen.S02E05.1080p.WEBRip.mkv",
      MediaInfoParser.parse("Jujutsu Kaisen (2020) - 05.mkv").title,
    )

    val expectedJujutsuKey = "jujutsukaisen"
    for (filename in jujutsuFilenames) {
      val key = SeriesDetector.normalizeKey(filename)
      assertEquals(
        "Normalized key mismatch for Jujutsu Kaisen filename: $filename",
        expectedJujutsuKey,
        key,
      )
    }

    val nonLatinFilenames = listOf(
      "葬送のフリーレン",
      "[SubsPlease] 葬送のフリーレン - 01 (1080p) [ABCD1234].mkv",
      "【Sub】葬送のフリーレン - 01 [1080p]",
      "葬送のフリーレン S01E01",
      MediaInfoParser.parse("[SubsPlease] 葬送のフリーレン - 01 (1080p) [ABCD1234].mkv").title,
    )

    val expectedNonLatinKey = "葬送のフリーレン"
    for (filename in nonLatinFilenames) {
      val key = SeriesDetector.normalizeKey(filename)
      assertEquals(
        "Normalized key mismatch for Japanese title: $filename",
        expectedNonLatinKey,
        key,
      )
    }
  }
}
