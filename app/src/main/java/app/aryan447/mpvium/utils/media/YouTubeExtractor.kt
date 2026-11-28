package app.aryan447.mpvium.utils.media

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

data class YouTubeResolvedStream(
  val videoUrl: String,
  val audioUrl: String? = null,
  val title: String? = null,
  val uploader: String? = null,
  val isLive: Boolean = false,
)

/**
 * Resolves YouTube watch/shorts/live/youtu.be URLs into direct stream URLs
 * playable by libmpv using NewPipeExtractor.
 */
object YouTubeExtractor {
  private const val TAG = "YouTubeExtractor"

  private val YOUTUBE_HOSTS = setOf(
    "youtube.com",
    "youtu.be",
    "youtube-nocookie.com",
  )

  @Volatile
  private var initialized = false

  private fun ensureInitialized() {
    if (initialized) return
    synchronized(this) {
      if (initialized) return
      val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
      NewPipe.init(OkHttpDownloader(client))
      initialized = true
    }
  }

  /**
   * Returns true if [url] points to a YouTube domain that should be resolved
   * before passing to libmpv.
   */
  fun isYouTubeUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "http" && scheme != "https") return false
    val host = uri.host?.lowercase()?.removeSuffix(".") ?: return false
    return YOUTUBE_HOSTS.any { domain ->
      host == domain || host.endsWith(".$domain")
    }
  }

  /**
   * Extracts playable video/audio stream URLs and metadata for a YouTube URL.
   * Must be called off the main thread; runs on [Dispatchers.IO].
   */
  suspend fun resolve(url: String): Result<YouTubeResolvedStream> = withContext(Dispatchers.IO) {
    runCatching {
      ensureInitialized()
      val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, url.trim())
      selectBestStreams(streamInfo)
    }.onFailure { error ->
      Log.e(TAG, "Failed to resolve YouTube URL: $url", error)
    }
  }

  private fun selectBestStreams(streamInfo: StreamInfo): YouTubeResolvedStream {
    val title = streamInfo.name?.takeIf { it.isNotBlank() }
    val uploader = streamInfo.uploaderName?.takeIf { it.isNotBlank() }
    val isLive = streamInfo.streamType == StreamType.LIVE_STREAM ||
      streamInfo.streamType == StreamType.AUDIO_LIVE_STREAM

    if (isLive) {
      val liveUrl = streamInfo.hlsUrl?.takeIf { it.isNotBlank() }
        ?: streamInfo.dashMpdUrl?.takeIf { it.isNotBlank() }
      if (liveUrl != null) {
        return YouTubeResolvedStream(
          videoUrl = liveUrl,
          title = title,
          uploader = uploader,
          isLive = true,
        )
      }
    }

    val bestVideoOnly = streamInfo.videoOnlyStreams
      .filter { it.content.isNotBlank() }
      .maxWithOrNull(compareBy<VideoStream> { it.height }.thenBy { it.bitrate })

    val bestAudio = streamInfo.audioStreams
      .filter { it.content.isNotBlank() }
      .maxByOrNull { it.averageBitrate }

    if (bestVideoOnly != null && bestAudio != null) {
      return YouTubeResolvedStream(
        videoUrl = bestVideoOnly.content,
        audioUrl = bestAudio.content,
        title = title,
        uploader = uploader,
        isLive = isLive,
      )
    }

    val bestMuxed = streamInfo.videoStreams
      .filter { it.content.isNotBlank() }
      .maxWithOrNull(compareBy<VideoStream> { it.height }.thenBy { it.bitrate })

    if (bestMuxed != null) {
      return YouTubeResolvedStream(
        videoUrl = bestMuxed.content,
        title = title,
        uploader = uploader,
        isLive = isLive,
      )
    }

    val fallbackUrl = streamInfo.hlsUrl?.takeIf { it.isNotBlank() }
      ?: streamInfo.dashMpdUrl?.takeIf { it.isNotBlank() }
      ?: bestAudio?.content

    if (fallbackUrl != null) {
      return YouTubeResolvedStream(
        videoUrl = fallbackUrl,
        title = title,
        uploader = uploader,
        isLive = isLive,
      )
    }

    throw IllegalStateException("No playable streams found")
  }

  private class OkHttpDownloader(
    private val client: OkHttpClient,
  ) : Downloader() {
    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
      val headersBuilder = okhttp3.Headers.Builder()
      request.headers().forEach { (key, values) ->
        values.forEach { value ->
          headersBuilder.add(key, value)
        }
      }

      val dataToSend = request.dataToSend()
      val body = if (dataToSend != null) {
        val contentType = request.headers()["Content-Type"]?.firstOrNull()
          ?: "application/json; charset=utf-8"
        dataToSend.toRequestBody(contentType.toMediaTypeOrNull())
      } else {
        null
      }

      val okRequest = okhttp3.Request.Builder()
        .url(request.url())
        .headers(headersBuilder.build())
        .method(request.httpMethod(), body)
        .build()

      client.newCall(okRequest).execute().use { response ->
        if (response.code == 429) {
          throw ReCaptchaException("reCaptcha Challenge requested", request.url())
        }
        val responseBody = response.body.string()
        return Response(
          response.code,
          response.message,
          response.headers.toMultimap(),
          responseBody,
          response.request.url.toString(),
        )
      }
    }
  }
}
