package app.aryan447.mpvium.domain.streaming

import android.content.Context
import android.graphics.BitmapFactory
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository.Companion.TMDB_IMAGE_BASE_ORIGINAL
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository.Companion.TMDB_IMAGE_BASE_W1280
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository.Companion.TMDB_IMAGE_BASE_W780
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StreamingArtworkQualityTest {

  private lateinit var context: Context
  private lateinit var okHttpClient: OkHttpClient
  private lateinit var artworkStore: StreamingArtworkStore

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    okHttpClient = OkHttpClient()
    artworkStore = StreamingArtworkStore(context, okHttpClient)
  }

  @Test
  fun formatImageUrl_upgradesLowResTmdbPosterUrl() {
    val rawWyziePoster = "https://image.tmdb.org/t/p/w185/yfYohBszGqoAW8oM0qydOtJ4kPh.jpg"
    val upgraded = StreamingMetadataRepository.formatImageUrl(rawWyziePoster, TMDB_IMAGE_BASE_W780)
    assertEquals("https://image.tmdb.org/t/p/w780/yfYohBszGqoAW8oM0qydOtJ4kPh.jpg", upgraded)
  }

  @Test
  fun formatImageUrl_upgradesLowResTmdbBackdropUrl() {
    val rawWyzieBackdrop = "https://image.tmdb.org/t/p/w500/8PHBAdPyfIwS5W6cfKLhItEb875.jpg"
    val phoneBackdrop = StreamingMetadataRepository.formatImageUrl(rawWyzieBackdrop, TMDB_IMAGE_BASE_W1280, isTablet = false)
    assertEquals("https://image.tmdb.org/t/p/w1280/8PHBAdPyfIwS5W6cfKLhItEb875.jpg", phoneBackdrop)

    val tabletBackdrop = StreamingMetadataRepository.formatImageUrl(rawWyzieBackdrop, TMDB_IMAGE_BASE_ORIGINAL, isTablet = true)
    assertEquals("https://image.tmdb.org/t/p/original/8PHBAdPyfIwS5W6cfKLhItEb875.jpg", tabletBackdrop)
  }

  @Test
  fun formatImageUrl_upgradesCinemetaMetahubUrls() {
    val smallPoster = "https://images.metahub.space/poster/small/tt33764258/img"
    val upgradedPoster = StreamingMetadataRepository.formatImageUrl(smallPoster, TMDB_IMAGE_BASE_W780)
    assertEquals("https://images.metahub.space/poster/large/tt33764258/img", upgradedPoster)

    val mediumBgPhone = "https://images.metahub.space/background/medium/tt33764258/img"
    val upgradedBgPhone = StreamingMetadataRepository.formatImageUrl(mediumBgPhone, TMDB_IMAGE_BASE_W1280, isTablet = false)
    assertEquals("https://images.metahub.space/background/medium/tt33764258/img", upgradedBgPhone)

    val mediumBgTablet = "https://images.metahub.space/background/medium/tt33764258/img"
    val upgradedBgTablet = StreamingMetadataRepository.formatImageUrl(mediumBgTablet, TMDB_IMAGE_BASE_ORIGINAL, isTablet = true)
    assertEquals("https://images.metahub.space/background/large/tt33764258/img", upgradedBgTablet)
  }

  @Test
  fun formatImageUrl_handlesRelativePaths() {
    val relativePathWithSlash = "/yfYohBszGqoAW8oM0qydOtJ4kPh.jpg"
    assertEquals(
      "https://image.tmdb.org/t/p/w780/yfYohBszGqoAW8oM0qydOtJ4kPh.jpg",
      StreamingMetadataRepository.formatImageUrl(relativePathWithSlash, TMDB_IMAGE_BASE_W780)
    )

    val relativePathWithoutSlash = "8PHBAdPyfIwS5W6cfKLhItEb875.jpg"
    assertEquals(
      "https://image.tmdb.org/t/p/w1280/8PHBAdPyfIwS5W6cfKLhItEb875.jpg",
      StreamingMetadataRepository.formatImageUrl(relativePathWithoutSlash, TMDB_IMAGE_BASE_W1280)
    )
  }

  @Test
  fun formatImageUrl_preservesCustomAndLocalUris() {
    val localFilePath = "/data/user/0/app.aryan447.mpvium/files/streaming_art/custom_art.img"
    assertEquals(localFilePath, StreamingMetadataRepository.formatImageUrl(localFilePath, TMDB_IMAGE_BASE_W780))

    val contentUri = "content://media/external/images/media/42"
    assertEquals(contentUri, StreamingMetadataRepository.formatImageUrl(contentUri, TMDB_IMAGE_BASE_W780))
  }

  @Test
  fun calculateInSampleSize_doesNotPrematurelyDownsamplePosters() {
    // TMDb w780 posters are 780x1170. With the old check, 1170 > 1080 caused inSampleSize = 2 (halved to 390x585).
    // The corrected rule keeps full quality (sampleSize = 1).
    val posterOptions = BitmapFactory.Options().apply {
      outWidth = 780
      outHeight = 1170
    }

    assertEquals(1, artworkStore.calculateInSampleSize(posterOptions, 1080))
    assertEquals(1, artworkStore.calculateInSampleSize(posterOptions, 2048))
  }

  @Test
  fun calculateInSampleSize_doesNotPrematurelyDownsampleBackdrops() {
    // TMDb w1280 backdrops are 1280x720. Should not be downsampled when target is 1080 or above.
    val backdropOptions = BitmapFactory.Options().apply {
      outWidth = 1280
      outHeight = 720
    }

    assertEquals(1, artworkStore.calculateInSampleSize(backdropOptions, 1080))
    assertEquals(1, artworkStore.calculateInSampleSize(backdropOptions, 2048))
  }

  @Test
  fun calculateInSampleSize_downsamplesOversizedImagesGracefully() {
    // 4K image (3840x2160) on a 1920 target should sample by 2 (resulting in 1920x1080).
    val fourKOptions = BitmapFactory.Options().apply {
      outWidth = 3840
      outHeight = 2160
    }

    assertEquals(2, artworkStore.calculateInSampleSize(fourKOptions, 1920))
  }
}
