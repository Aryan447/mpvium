package app.aryan447.mpvium.preferences

enum class PlayerLayout {
  // The original glass-pill player: theme-tinted chrome with visible borders.
  // Historically stored as GLASS_PILL; unknown legacy values fall back here.
  CLASSIC,

  // Floating dark glass interface modeled after Apple TV+ native player:
  // clean unobstructed video, circular top buttons (Close, PiP), horizontal volume capsule,
  // center transport controls, show header (episode on top, series below), actions, and seekbar.
  MODERN;

  companion object {
    fun fromString(name: String): PlayerLayout = when (name.trim().uppercase()) {
      "MODERN", "MINIMAL", "CINEMA" -> MODERN
      "CLASSIC", "GLASS_PILL" -> CLASSIC
      else -> CLASSIC
    }
  }
}
