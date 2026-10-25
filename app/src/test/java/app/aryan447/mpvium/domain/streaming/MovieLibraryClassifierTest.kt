package app.aryan447.mpvium.domain.streaming

import app.aryan447.mpvium.domain.streaming.MovieLibraryKind.MOVIE
import app.aryan447.mpvium.domain.streaming.MovieLibraryKind.SHORT_VIDEO
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieLibraryClassifierTest {
  private companion object {
    const val MINUTE_MS = 60_000L
    const val MOVIES_FOLDER = "/storage/emulated/0/Movies"
    const val DOWNLOAD_FOLDER = "/storage/emulated/0/Download"
  }

  private fun classify(
    fileName: String = "Feature Film (2024).mkv",
    folderPath: String = MOVIES_FOLDER,
    durationMs: Long = 100 * MINUTE_MS,
    width: Int = 1920,
    height: Int = 1080,
  ): MovieLibraryKind = MovieLibraryClassifier.classify(
    fileName = fileName,
    folderPath = folderPath,
    durationMs = durationMs,
    width = width,
    height = height,
  )

  @Test
  fun featureLengthLandscapeMovieInMoviesFolder_staysMovie() {
    assertEquals(MOVIE, classify())
  }

  @Test
  fun featureLengthMovieInDownloads_staysMovie() {
    // 2h25m, even in an ambiguous source folder.
    assertEquals(
      MOVIE,
      classify(durationMs = 145 * MINUTE_MS, folderPath = DOWNLOAD_FOLDER),
    )
  }

  @Test
  fun veryShortLandscapeVideo_isShortVideo() {
    assertEquals(SHORT_VIDEO, classify(durationMs = 18_000L))
  }

  @Test
  fun portraitReel_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(durationMs = 8 * MINUTE_MS, width = 1080, height = 1920),
    )
  }

  @Test
  fun portraitTalkUnderTwentyMinutes_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(durationMs = 14 * MINUTE_MS + 58_000L, width = 1080, height = 1920),
    )
  }

  @Test
  fun portraitVideoOverTwentyMinutes_staysMovie() {
    // Conservative: long vertical video is not assumed to be a clip.
    assertEquals(
      MOVIE,
      classify(durationMs = 25 * MINUTE_MS, width = 1080, height = 1920),
    )
  }

  @Test
  fun squareClip_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(durationMs = 8 * MINUTE_MS, width = 1080, height = 1080),
    )
  }

  @Test
  fun landscapeShortFilmInMoviesFolder_staysMovie() {
    // No clip source signal; just over five minutes is plausible for a short film.
    assertEquals(
      MOVIE,
      classify(durationMs = 6 * MINUTE_MS, width = 1920, height = 1080),
    )
  }

  @Test
  fun landscapeClipJustUnderThreeMinutes_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(durationMs = 2 * MINUTE_MS + 45_000L, width = 1920, height = 1080),
    )
  }

  @Test
  fun landscapeClipJustUnderFiveMinutes_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(durationMs = 4 * MINUTE_MS + 59_000L, width = 1920, height = 1080),
    )
  }

  @Test
  fun landscapeVideoAtFiveMinutes_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(durationMs = 5 * MINUTE_MS, width = 1920, height = 1080),
    )
  }

  @Test
  fun landscapeVideoJustOverFiveMinutes_staysMovie() {
    assertEquals(
      MOVIE,
      classify(durationMs = 5 * MINUTE_MS + 1_000L, width = 1920, height = 1080),
    )
  }

  @Test
  fun landscapeDownloadUnderTwentyMinutes_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "RDT 211604.mp4",
        folderPath = DOWNLOAD_FOLDER,
        durationMs = 14 * MINUTE_MS + 58_000L,
      ),
    )
  }

  @Test
  fun landscapeDownloadOverTwentyMinutes_staysMovie() {
    assertEquals(
      MOVIE,
      classify(
        fileName = "Documentary.mkv",
        folderPath = DOWNLOAD_FOLDER,
        durationMs = 25 * MINUTE_MS,
      ),
    )
  }

  @Test
  fun socialAppCacheUnderThirtyMinutes_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "family video.mp4",
        folderPath = "/storage/emulated/0/Telegram/Telegram Video",
        durationMs = 20 * MINUTE_MS,
      ),
    )
  }

  @Test
  fun socialAppCacheFeatureLength_staysMovie() {
    assertEquals(
      MOVIE,
      classify(
        fileName = "Feature Film.mkv",
        folderPath = "/storage/emulated/0/Telegram/Telegram Video",
        durationMs = 90 * MINUTE_MS,
      ),
    )
  }

  @Test
  fun redditExportFileName_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "RDT_20240101_211604.mp4",
        folderPath = MOVIES_FOLDER,
        durationMs = 12 * MINUTE_MS,
      ),
    )
  }

  @Test
  fun whatsAppExportFileName_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "VID-20240101-WA0001.mp4",
        folderPath = MOVIES_FOLDER,
        durationMs = 9 * MINUTE_MS,
      ),
    )
  }

  @Test
  fun underscoreSeparatedPlatformName_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "WhatsApp_Video_20240101.mp4",
        folderPath = MOVIES_FOLDER,
        durationMs = 12 * MINUTE_MS,
        width = 1920,
        height = 1080,
      ),
    )
  }

  @Test
  fun underscoreSeparatedScreenRecording_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "Screen_Recording_20240101.mp4",
        folderPath = MOVIES_FOLDER,
        durationMs = 8 * MINUTE_MS,
        width = 1920,
        height = 1080,
      ),
    )
  }

  @Test
  fun cameraFileNameCopiedToMoviesFolder_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "VID_20240101_120000.mp4",
        folderPath = MOVIES_FOLDER,
        durationMs = 8 * MINUTE_MS,
        width = 1920,
        height = 1080,
      ),
    )
  }

  @Test
  fun pixelFileNameCopiedToMoviesFolder_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "PXL_20240101_120000123.mp4",
        folderPath = MOVIES_FOLDER,
        durationMs = 9 * MINUTE_MS,
        width = 1920,
        height = 1080,
      ),
    )
  }

  @Test
  fun underscoreSeparatedClipFolder_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "clip.mp4",
        folderPath = "/storage/emulated/0/Screen_Recordings",
        durationMs = 9 * MINUTE_MS,
        width = 1920,
        height = 1080,
      ),
    )
  }

  @Test
  fun trailerFileName_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "Spider-Man 2026 Official Trailer.mp4",
        folderPath = MOVIES_FOLDER,
        durationMs = 8 * MINUTE_MS,
      ),
    )
  }

  @Test
  fun sampleReleaseFile_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "Feature.Film.2024.1080p.sample.mkv",
        durationMs = 8 * MINUTE_MS,
      ),
    )
  }

  @Test
  fun clipFolder_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "clip.mp4",
        folderPath = "/storage/emulated/0/Shorts",
        durationMs = 8 * MINUTE_MS,
      ),
    )
  }

  @Test
  fun socialFolderMatchingIsCaseInsensitive() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "clip.mp4",
        folderPath = "/storage/emulated/0/WHATSAPP VIDEO",
        durationMs = 6 * MINUTE_MS,
      ),
    )
  }

  @Test
  fun cameraRecordingUnderTwentyMinutes_isShortVideo() {
    assertEquals(
      SHORT_VIDEO,
      classify(
        fileName = "VID_20240101_120000.mp4",
        folderPath = "/storage/emulated/0/DCIM/Camera",
        durationMs = 8 * MINUTE_MS,
      ),
    )
  }

  @Test
  fun unknownDuration_staysMovie() {
    assertEquals(MOVIE, classify(durationMs = 0L))
  }

  @Test
  fun unknownDimensions_doNotHideAmbiguousVideo() {
    assertEquals(
      MOVIE,
      classify(durationMs = 8 * MINUTE_MS, width = 0, height = 0),
    )
  }

  @Test
  fun featureLengthGuardProtectsMislabelledMovie() {
    assertEquals(
      MOVIE,
      classify(
        fileName = "RDT_20240101_211604.mp4",
        folderPath = "/storage/emulated/0/Telegram/Telegram Video",
        durationMs = 2 * 60 * MINUTE_MS,
      ),
    )
  }
}
