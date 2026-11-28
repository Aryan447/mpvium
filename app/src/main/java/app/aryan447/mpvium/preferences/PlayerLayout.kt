package app.aryan447.mpvium.preferences

enum class PlayerLayout {
  // Floating dark glass interface that keeps the video unobstructed:
  // clean unobstructed video, circular top buttons (Close, PiP), horizontal volume capsule,
  // center transport controls, show header (episode on top, series below), actions, and seekbar.
  // The default since 1.5.0; the old Cinema and Minimal layouts map here.
  MODERN,

  // Minimalist frosted-glass pill layout: top-left Close with on-demand single-line title,
  // top-right title toggle & playlist/chapters/PiP, and a single bottom row of three pills
  // (transport, seekbar + timers, and action shortcuts).
  ZEN,

  // The original glass-pill player: theme-tinted chrome with visible borders.
  // Historically stored as GLASS_PILL.
  CLASSIC;

  companion object {
    fun fromString(name: String): PlayerLayout = when (name.trim().uppercase()) {
      "MODERN", "MINIMAL", "CINEMA" -> MODERN
      "ZEN" -> ZEN
      "CLASSIC", "GLASS_PILL" -> CLASSIC
      else -> MODERN
    }
  }
}
