package app.aryan447.mpvium.preferences

/**
 * Playback option groups that can be owned by the user's mpv.conf instead of
 * the in-app settings.
 *
 * Group checkboxes are a convenience select-all action; every option can also
 * be selected on its own. Only options mpvium applies from its own settings are
 * listed, and infrastructure the player needs to keep working (surface
 * attachment, track selection, position, volume, TLS, scripts, IPC) is
 * deliberately absent.
 *
 * Ownership covers the values mpvium applies when a file starts. Adjustments
 * you make in the player afterwards are still passed through to mpv.
 */
enum class MpvConfigOwnership(
  val preferenceKey: String,
  val optionNames: Set<String>,
) {
  RENDERER(
    preferenceKey = "renderer",
    optionNames =
      setOf(
        "profile",
        "gpu-context",
        "opengl-es",
      ),
  ),
  DECODER(
    preferenceKey = "decoder",
    optionNames =
      setOf(
        "hwdec",
        "hwdec-codecs",
        "vd-lavc-dr",
        "vd-lavc-film-grain",
      ),
  ),
  SHADERS(
    preferenceKey = "shaders",
    optionNames =
      setOf(
        "glsl-shaders",
        "opengl-pbo",
        "opengl-early-flush",
      ),
  ),
  VIDEO_FILTERS(
    preferenceKey = "video_filters",
    optionNames =
      setOf(
        "vf",
        "brightness",
        "contrast",
        "saturation",
        "gamma",
        "hue",
        "sharpen",
        "deband",
      ),
  ),
  VIDEO_GEOMETRY(
    preferenceKey = "video_geometry",
    optionNames =
      setOf(
        "video-zoom",
        "video-pan-x",
        "video-pan-y",
        "video-aspect-override",
        "panscan",
      ),
  ),
  AUDIO_OUTPUT(
    preferenceKey = "audio_output",
    optionNames =
      setOf(
        "alang",
        "audio-delay",
        "audio-pitch-correction",
        "volume-max",
      ),
  ),
  AUDIO_FILTERS(
    preferenceKey = "audio_filters",
    optionNames =
      setOf(
        "af",
        "audio-channels",
      ),
  ),
  SUBTITLE_LOADING(
    preferenceKey = "subtitle_loading",
    optionNames =
      setOf(
        "slang",
        "sub-auto",
        "sub-file-paths",
        "subs-fallback",
        "sub-fonts-dir",
      ),
  ),
  SUBTITLE_STYLE(
    preferenceKey = "subtitle_style",
    optionNames =
      setOf(
        "sub-delay",
        "sub-speed",
        "sub-font",
        "sub-font-size",
        "sub-bold",
        "sub-italic",
        "sub-justify",
        "sub-color",
        "sub-back-color",
        "sub-border-color",
        "sub-border-size",
        "sub-outline-size",
        "sub-border-style",
        "sub-shadow-offset",
        "sub-scale",
        "sub-pos",
        "sub-scale-by-window",
        "sub-use-margins",
        "sub-ass-override",
        "sub-ass-justify",
        "stretch-dvd-subs",
        "secondary-sub-delay",
        "secondary-sub-speed",
        "secondary-sub-font",
        "secondary-sub-font-size",
        "secondary-sub-bold",
        "secondary-sub-italic",
        "secondary-sub-justify",
        "secondary-sub-color",
        "secondary-sub-back-color",
        "secondary-sub-border-color",
        "secondary-sub-border-size",
        "secondary-sub-border-style",
        "secondary-sub-shadow-offset",
        "secondary-sub-scale",
        "secondary-sub-pos",
        "secondary-sub-scale-by-window",
        "secondary-sub-use-margins",
        "secondary-sub-ass-override",
      ),
  ),
  PLAYBACK_TIMING(
    preferenceKey = "playback_timing",
    optionNames =
      setOf(
        "speed",
        "hr-seek",
        "hr-seek-framedrop",
        "hr-seek-demuxer-offset",
        "framedrop",
      ),
  ),
  NETWORK_BUFFERING(
    preferenceKey = "network_buffering",
    optionNames =
      setOf(
        "cache-secs",
        "demuxer-max-bytes",
        "demuxer-max-back-bytes",
        "demuxer-readahead-secs",
        "stream-buffer-size",
      ),
  ),
  ;

  companion object {
    private val allOptionNames: Set<String> = entries.flatMapTo(linkedSetOf()) { it.optionNames }

    /**
     * Expands stored selections into the set of option names the app must not
     * apply. Unknown values are ignored so a removed option cannot pin an
     * unrelated one.
     */
    fun resolveOptionNames(storedValues: Set<String>): Set<String> =
      storedValues.filterTo(linkedSetOf()) { it in allOptionNames }
  }
}

/** The ownership snapshot used by the active libmpv core. */
object MpvConfigOwnershipPolicy {
  @Volatile
  private var overriddenOptionNames: Set<String> = emptySet()

  fun configure(storedValues: Set<String>) {
    overriddenOptionNames = MpvConfigOwnership.resolveOptionNames(storedValues)
  }

  fun isOwnedByMpvConf(optionName: String): Boolean = optionName in overriddenOptionNames

  fun shouldSuppress(command: Array<out String>): Boolean =
    when (command.firstOrNull()) {
      "vf", "af" -> command.first().let(::isOwnedByMpvConf)
      "change-list" -> command.getOrNull(1)?.let(::isOwnedByMpvConf) == true
      "set", "set_property", "set_property_string" -> command.getOrNull(1)?.let(::isOwnedByMpvConf) == true
      else -> false
    }
}
