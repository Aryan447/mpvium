package app.aryan447.mpvium.preferences

enum class PlayerLayout {
  // The original glass-pill player: theme-tinted chrome with visible borders.
  // Historically stored as GLASS_PILL; unknown legacy values fall back here.
  CLASSIC,

  // Floating dark glass pill player: clean unobstructed video, circular top buttons (Close, PiP),
  // and three floating bottom pills (Transport, Timeline, Actions) with gesture exclusion.
  ZEN;

  companion object {
    fun fromString(name: String): PlayerLayout = when (name.trim().uppercase()) {
      "ZEN", "MINIMAL", "CINEMA" -> ZEN
      "CLASSIC", "GLASS_PILL" -> CLASSIC
      else -> CLASSIC
    }
  }
}
