package app.aryan447.mpvium.ui.player

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.util.AttributeSet

import android.view.KeyCharacterMap
import android.view.KeyEvent
import app.aryan447.mpvium.preferences.AdvancedPreferences
import app.aryan447.mpvium.preferences.AudioPreferences
import app.aryan447.mpvium.preferences.DecoderPreferences
import app.aryan447.mpvium.preferences.MpvConfigOwnershipPolicy
import app.aryan447.mpvium.preferences.PlayerPreferences
import app.aryan447.mpvium.preferences.SubtitlesPreferences
import app.aryan447.mpvium.domain.anime4k.Anime4KManager
import app.aryan447.mpvium.ui.player.PlayerActivity.Companion.TAG
import app.aryan447.mpvium.ui.player.controls.components.panels.toColorHexString
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.KeyMapping
import `is`.xyz.mpv.MPVLib
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.roundToInt
import kotlin.reflect.KProperty

class MPVView(
  context: Context,
  attributes: AttributeSet,
) : BaseMPVView(context, attributes),
  KoinComponent {
  private val audioPreferences: AudioPreferences by inject()
  private val playerPreferences: PlayerPreferences by inject()
  private val decoderPreferences: DecoderPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()
  private val anime4kManager: Anime4KManager by inject()

  var isExiting = false

  fun getVideoOutAspect(): Double? {
    // Try to get aspect from video-params/aspect first
    val rawAspect = MPVLib.getPropertyDouble("video-params/aspect")
    val rotate = MPVLib.getPropertyInt("video-params/rotate") ?: 0

    // If aspect is not available or 0, calculate from width and height
    val finalAspect = if (rawAspect == null || rawAspect < 0.001) {
      val width = runCatching {
        MPVLib.getPropertyInt("width") ?: MPVLib.getPropertyInt("video-params/w") ?: 0
      }.getOrDefault(0)

      val height = runCatching {
        MPVLib.getPropertyInt("height") ?: MPVLib.getPropertyInt("video-params/h") ?: 0
      }.getOrDefault(0)

      if (width > 0 && height > 0) {
        width.toDouble() / height.toDouble()
      } else {
        null
      }
    } else {
      rawAspect
    }

    return finalAspect?.let { aspect ->
      if (aspect <= 0.001) {
        return null
      }
      val isRotated = (rotate % 180 == 90)
      val correctedAspect = if (isRotated) 1.0 / aspect else aspect
      correctedAspect
    }
  }

  class TrackDelegate(
    private val name: String,
  ) {
    operator fun getValue(
      thisRef: Any?,
      property: KProperty<*>,
    ): Int {
      val v = MPVLib.getPropertyString(name)
      // we can get null here for "no" or other invalid value
      return v?.toIntOrNull() ?: -1
    }

    operator fun setValue(
      thisRef: Any?,
      property: KProperty<*>,
      value: Int,
    ) {
      if (value == -1) MPVLib.setPropertyString(name, "no") else MPVLib.setPropertyInt(name, value)
    }
  }

  var sid: Int by TrackDelegate("sid")
  var secondarySid: Int by TrackDelegate("secondary-sid")
  var aid: Int by TrackDelegate("aid")

  override fun initOptions() {
    MpvConfigOwnershipPolicy.configure(advancedPreferences.mpvConfOverrides.get())

    val profile = decoderPreferences.profile.get()
    MpvConfigWriter.setOptionString("profile", profile)
    // The video output is always app-owned: the Android surface is attached to
    // this view, so mpv.conf cannot choose a vo that would bypass it.
    setVo(if (decoderPreferences.gpuNext.get()) "gpu-next" else "gpu")

    // Set GPU API context (Vulkan or OpenGL)
    if (decoderPreferences.useVulkan.get()) {
      MpvConfigWriter.setOptionString("gpu-context", "androidvk")
    } else {
      MpvConfigWriter.setOptionString("gpu-context", "android")
      MpvConfigWriter.setOptionString("opengl-es", "yes")
    }

    // Initial decoder from settings. HW falls back to SW; HW+ also tries direct output first.
    MpvConfigWriter.setOptionString("hwdec", decoderPreferences.defaultDecoder.get().defaultHwdec)
    MpvConfigWriter.setOptionString("hwdec-codecs", "all")

    if (decoderPreferences.useYUV420P.get()) {
      MpvConfigWriter.setOptionString("vf", "format=yuv420p")
    }

    // Demuxer cache configuration.
    // OFF path is sacred: matches mpv-android: bounds memory usage without constraining readahead buffers.
    // ON path: dynamically resolves safe cache budgets and readahead from device capabilities.
    if (advancedPreferences.leanSeekCache.get()) {
      val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
      val memoryClass = activityManager?.memoryClass ?: 256
      val isLowRam = activityManager?.isLowRamDevice ?: false
      val profile = SeekCacheResolver.resolve(
        SeekCacheResolver.Input(
          memoryClassMb = memoryClass,
          isLowRamDevice = isLowRam,
          sourceType = SeekCacheResolver.SourceType.Local,
        )
      )
      MpvConfigWriter.setOptionString("demuxer-max-bytes", "${profile.demuxerMaxBytes}")
      MpvConfigWriter.setOptionString("demuxer-max-back-bytes", "${profile.demuxerMaxBackBytes}")
      MpvConfigWriter.setOptionString("demuxer-readahead-secs", "${profile.demuxerReadaheadSecs.roundToInt()}")
      MpvConfigWriter.setOptionString("cache-secs", "${profile.cacheSecs.roundToInt()}")
      MpvConfigWriter.setOptionString("stream-buffer-size", "${profile.streamBufferSize}")
    } else {
      val cacheMegs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) 64 else 32
      val backCacheMegs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) 128 else 32
      MpvConfigWriter.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
      MpvConfigWriter.setOptionString("demuxer-max-back-bytes", "${backCacheMegs * 1024 * 1024}")
      MpvConfigWriter.setOptionString("demuxer-readahead-secs", "10")
      MpvConfigWriter.setOptionString("cache-secs", "20")
      MpvConfigWriter.setOptionString("stream-buffer-size", "${1024 * 1024}")
    }

    val logLevel = if (advancedPreferences.verboseLogging.get()) "v" else "warn"
    MpvConfigWriter.setOptionString("msg-level", "all=$logLevel")

    MpvConfigWriter.setPropertyBoolean("keep-open", true)
    MpvConfigWriter.setPropertyBoolean("input-default-bindings", true)
    MpvConfigWriter.setOptionString(
      "load-scripts",
      if (advancedPreferences.enableScripts.get()) "yes" else "no",
    )

    MpvConfigWriter.setOptionString("tls-verify", "yes")
    MpvConfigWriter.setOptionString("tls-ca-file", "${context.filesDir.path}/cacert.pem")

    val screenshotDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    screenshotDir.mkdirs()
    MpvConfigWriter.setOptionString("screenshot-directory", screenshotDir.path)

    VideoFilters.entries.forEach {
      MpvConfigWriter.setOptionString(it.mpvProperty, it.preference(decoderPreferences).get().toString())
    }

    MpvConfigWriter.setOptionString("speed", playerPreferences.defaultSpeed.get().toString())
    MpvConfigWriter.setOptionString("vd-lavc-film-grain", "cpu")

    val preciseSeek = playerPreferences.usePreciseSeeking.get()
    MpvConfigWriter.setOptionString("hr-seek", if (preciseSeek) "yes" else "no")
    // Exact (hr-)seeks must never stall on intermediate frames: let the vo
    // drop late frames while catching up and keep the demuxer offset at zero
    // so the target timestamp is decoded, not approximated around. These are
    // also re-asserted once duration is known (see PlayerViewModel), but
    // setting them at init covers streams whose duration never arrives.
    MpvConfigWriter.setOptionString("hr-seek-framedrop", "yes")
    MpvConfigWriter.setOptionString("hr-seek-demuxer-offset", "0")
    MpvConfigWriter.setOptionString("framedrop", "vo")

    // Anime4K shader initialization (MUST be in initOptions, not after file load!)
    applyAnime4KShaders()

    setupSubtitlesOptions()
    setupAudioOptions()
  }

  override fun observeProperties() {
    for ((name, format) in observedProps) MPVLib.observeProperty(name, format)
  }

  override fun postInitOptions() {
    when (decoderPreferences.debanding.get()) {
      Debanding.None -> {}
      Debanding.CPU -> MpvConfigWriter.command("vf", "add", "@deband:gradfun=radius=12")
      Debanding.GPU -> MpvConfigWriter.setOptionString("deband", "yes")
    }

    advancedPreferences.enabledStatisticsPage.get().let {
      if (it != 0) {
        MpvConfigWriter.command("script-binding", "stats/display-stats-toggle")
        MpvConfigWriter.command("script-binding", "stats/display-page-$it")
      }
    }
  }

  @Suppress("ReturnCount", "DEPRECATION")
  fun onKey(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_MULTIPLE || KeyEvent.isModifierKey(event.keyCode)) {
      return false
    }

    var mapped = KeyMapping[event.keyCode]
    if (mapped == null) {
      // Fallback to produced glyph
      if (!event.isPrintingKey) {
        return false
      }

      val ch = event.unicodeChar
      if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0) {
        return false // dead key
      }
      mapped = ch.toChar().toString()
    }

    if (event.repeatCount > 0) {
      return true
    }

    val mod: MutableList<String> = mutableListOf()
    event.isShiftPressed && mod.add("shift")
    event.isCtrlPressed && mod.add("ctrl")
    event.isAltPressed && mod.add("alt")
    event.isMetaPressed && mod.add("meta")

    val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
    mod.add(mapped)
    MPVLib.command(action, mod.joinToString("+"))

    return true
  }

  private val observedProps =
    mapOf(
      "pause" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
      "paused-for-cache" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
      "video-params/aspect" to MPVLib.MpvFormat.MPV_FORMAT_DOUBLE,
      "video-params/w" to MPVLib.MpvFormat.MPV_FORMAT_INT64,
      "video-params/h" to MPVLib.MpvFormat.MPV_FORMAT_INT64,
      "eof-reached" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
      "user-data/mpvium/show_text" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvium/toggle_ui" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvium/show_panel" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvium/set_button_title" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvium/reset_button_title" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvium/toggle_button" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvium/seek_by" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvium/seek_to" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvium/seek_by_with_text" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvium/seek_to_with_text" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvium/software_keyboard" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
    )

  private fun setupAudioOptions() {
    // Disable MPV's automatic audio selection
    // App will handle track selection manually via TrackSelector to respect user choices
    MpvConfigWriter.setOptionString("alang", "")
    MpvConfigWriter.setOptionString("audio-delay", (audioPreferences.defaultAudioDelay.get() / 1000.0).toString())
    MpvConfigWriter.setOptionString("audio-pitch-correction", audioPreferences.audioPitchCorrection.get().toString())
    MpvConfigWriter.setOptionString("volume-max", (audioPreferences.volumeBoostCap.get() + 100).toString())

    // Volume normalization using dynamic audio normalization filter
    if (audioPreferences.volumeNormalization.get()) {
      MpvConfigWriter.setOptionString("af", "dynaudnorm")
    }
  }

  // Setup
  private fun setupSubtitlesOptions() {
    // Disable MPV's automatic subtitle selection
    // App will handle track selection manually via TrackSelector to respect user choices
    MpvConfigWriter.setOptionString("slang", "")
    MpvConfigWriter.setOptionString("sub-auto", "no")
    MpvConfigWriter.setOptionString("sub-file-paths", "")
    MpvConfigWriter.setOptionString("subs-fallback", "no")

    val fontsDirPath = "${context.filesDir.path}/fonts/"
    MpvConfigWriter.setOptionString("sub-fonts-dir", fontsDirPath)

    // Delay and speed for both primary and secondary
    val subDelay = (subtitlesPreferences.defaultSubDelay.get() / 1000.0).toString()
    val subSpeed = subtitlesPreferences.defaultSubSpeed.get().toString()
    MpvConfigWriter.setOptionString("sub-delay", subDelay)
    MpvConfigWriter.setOptionString("sub-speed", subSpeed)
    MpvConfigWriter.setOptionString("secondary-sub-delay", subDelay)
    MpvConfigWriter.setOptionString("secondary-sub-speed", subSpeed)

    val preferredFont = subtitlesPreferences.font.get()
    if (preferredFont.isNotBlank()) {
      MpvConfigWriter.setOptionString("sub-font", preferredFont)
      MpvConfigWriter.setOptionString("secondary-sub-font", preferredFont)
    }
    // If blank, MPV uses its default font

    // "no" makes mpv ignore sub-scale and sub-pos for bitmap subtitles
    // (VOBSUB/DVD, PGS), so image subs would stop following the Scale and
    // Position sliders. "scale" keeps them responsive, same as
    // PlayerActivity.applySubtitlePreferences() does at playback start.
    if (subtitlesPreferences.overrideAssSubs.get()) {
      MpvConfigWriter.setOptionString("sub-ass-override", "force")
      MpvConfigWriter.setOptionString("sub-ass-justify", "yes")
      MpvConfigWriter.setOptionString("secondary-sub-ass-override", "force")
    } else {
      MpvConfigWriter.setOptionString("sub-ass-override", "scale")
      MpvConfigWriter.setOptionString("secondary-sub-ass-override", "scale")
    }

    // Typography and styling for both primary and secondary
    val fontSize = subtitlesPreferences.fontSize.get().toString()
    val bold = if (subtitlesPreferences.bold.get()) "yes" else "no"
    val italic = if (subtitlesPreferences.italic.get()) "yes" else "no"
    val justify = subtitlesPreferences.justification.get().value
    val textColor = subtitlesPreferences.textColor.get().toColorHexString()
    val backgroundColor = subtitlesPreferences.backgroundColor.get().toColorHexString()
    val borderColor = subtitlesPreferences.borderColor.get().toColorHexString()
    val borderSize = subtitlesPreferences.borderSize.get().toString()
    val borderStyle = subtitlesPreferences.borderStyle.get().value
    val shadowOffset = subtitlesPreferences.shadowOffset.get().toString()
    val subPos = subtitlesPreferences.subPos.get().toString()
    val subScale = subtitlesPreferences.subScale.get().toString()

    MpvConfigWriter.setOptionString("sub-font-size", fontSize)
    MpvConfigWriter.setOptionString("sub-bold", bold)
    MpvConfigWriter.setOptionString("sub-italic", italic)
    MpvConfigWriter.setOptionString("sub-justify", justify)
    MpvConfigWriter.setOptionString("sub-color", textColor)
    MpvConfigWriter.setOptionString("sub-back-color", backgroundColor)
    MpvConfigWriter.setOptionString("sub-border-color", borderColor)
    MpvConfigWriter.setOptionString("sub-border-size", borderSize)
    MpvConfigWriter.setOptionString("sub-border-style", borderStyle)
    MpvConfigWriter.setOptionString("sub-shadow-offset", shadowOffset)
    MpvConfigWriter.setOptionString("sub-scale", subScale)
    MpvConfigWriter.setOptionString("sub-pos", subPos)

    MpvConfigWriter.setOptionString("secondary-sub-font-size", fontSize)
    MpvConfigWriter.setOptionString("secondary-sub-bold", bold)
    MpvConfigWriter.setOptionString("secondary-sub-italic", italic)
    MpvConfigWriter.setOptionString("secondary-sub-justify", justify)
    MpvConfigWriter.setOptionString("secondary-sub-color", textColor)
    MpvConfigWriter.setOptionString("secondary-sub-back-color", backgroundColor)
    MpvConfigWriter.setOptionString("secondary-sub-border-color", borderColor)
    MpvConfigWriter.setOptionString("secondary-sub-border-size", borderSize)
    MpvConfigWriter.setOptionString("secondary-sub-border-style", borderStyle)
    MpvConfigWriter.setOptionString("secondary-sub-shadow-offset", shadowOffset)
    MpvConfigWriter.setOptionString("secondary-sub-scale", subScale)
    // Position secondary subtitle at bottom (just above primary) to keep all non-ASS subs at bottom
    // Primary is at subPos (default 100). Secondary is placed slightly above to avoid exact overlap
    // For ASS subtitles with override disabled, mpv respects the ASS file's native positioning
    val secondaryPos = (subPos.toIntOrNull() ?: 100).let { (it - 10).coerceIn(0, 100) }.toString()
    MpvConfigWriter.setOptionString("secondary-sub-pos", secondaryPos)

    val scaleByWindow = if (subtitlesPreferences.scaleByWindow.get()) "yes" else "no"
    MpvConfigWriter.setOptionString("sub-scale-by-window", scaleByWindow)
    MpvConfigWriter.setOptionString("sub-use-margins", scaleByWindow)
    MpvConfigWriter.setOptionString("secondary-sub-scale-by-window", scaleByWindow)
    MpvConfigWriter.setOptionString("secondary-sub-use-margins", scaleByWindow)

    // DVD subtitles (VOBSUB) are authored for anamorphic storage, so mpv would
    // otherwise stretch the bitmap canvas onto the video area and distort the
    // text. No effect on square-pixel video or on PGS/text subtitles.
    MpvConfigWriter.setOptionString("stretch-dvd-subs", "yes")
  }


  fun applyAnime4KShaders() {
    runCatching {
      val enabled = decoderPreferences.enableAnime4K.get()
      if (!enabled) {
        return
      }

      // Anime4K requires the legacy GPU path unless gpu-next is running on Vulkan.
      val gpuNextActive = decoderPreferences.gpuNext.get()
      val useVulkan = decoderPreferences.useVulkan.get()
      if (gpuNextActive && !useVulkan) {
        return  // Abort shader loading to prevent incompatible state
      }

      // Initialize shader files if needed - THIS IS CRITICAL!
      if (!anime4kManager.initialize()) {
        return
      }

      // Get preferences
      val modeStr = decoderPreferences.anime4kMode.get()

      // Check if mode is OFF - if so, don't apply any shaders
      if (modeStr == "OFF") {
        return  // Exit early - user wants it OFF
      }

      // Parse user's selected mode
      val mode = try {
          Anime4KManager.Mode.valueOf(modeStr)
      } catch (e: IllegalArgumentException) {
          Anime4KManager.Mode.OFF
      }

      val qualityStr = decoderPreferences.anime4kQuality.get()
      val quality = try {
        Anime4KManager.Quality.valueOf(qualityStr)
      } catch (e: IllegalArgumentException) {
        Anime4KManager.Quality.BALANCED
      }

      // Get shader chain from manager
      val shaderChain = anime4kManager.getShaderChain(mode, quality)

      if (shaderChain.isNotEmpty()) {
        // OpenGL-only tuning should not be pushed onto the Vulkan backend.
        if (!useVulkan) {
          MpvConfigWriter.setOptionString("opengl-pbo", "yes")
          MpvConfigWriter.setOptionString("opengl-early-flush", "no")
        }
        MpvConfigWriter.setOptionString("vd-lavc-dr", "yes")

        // Apply shaders (MUST use setOptionString in initOptions!)
        MpvConfigWriter.setOptionString("glsl-shaders", shaderChain)
      }
    }.onFailure {
      // Don't crash - just continue without shaders
    }
  }
}
