package app.aryan447.mpvium.preferences

enum class PlayerLayout {
  // The original glass-pill player: theme-tinted chrome with visible borders.
  // Historically stored as GLASS_PILL; unknown legacy values fall back here.
  CLASSIC,

  // Borderless, video-first chrome with neutral dark discs and a lower-key
  // top bar. Opt-in only.
  CINEMA,
}
