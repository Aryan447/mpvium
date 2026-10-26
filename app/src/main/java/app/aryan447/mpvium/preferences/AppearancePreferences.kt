package app.aryan447.mpvium.preferences

import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.preference.PreferenceStore
import app.aryan447.mpvium.preferences.preference.getEnum
import app.aryan447.mpvium.ui.theme.AppTheme
import app.aryan447.mpvium.ui.theme.DarkMode

class AppearancePreferences(
  preferenceStore: PreferenceStore,
) {
  val darkMode = preferenceStore.getEnum("dark_mode", DarkMode.System)
  val appTheme = preferenceStore.getEnum("app_theme", AppTheme.Default)
  val amoledMode = preferenceStore.getBoolean("amoled_mode", false)
  val playerFullColors = preferenceStore.getBoolean("full_player_colors", false)
  val pillNavigationBar = preferenceStore.getBoolean("pill_navigation_bar", true)
  val showBottomNavLabels = preferenceStore.getBoolean("show_bottom_nav_labels", true)
  val bottomNavTabs =
    preferenceStore.getStringSet(
      "bottom_nav_tabs",
      setOf("HOME", "SHOWS", "MOVIES", "FOLDERS", "LIBRARY"),
    )
  val onboardingCompleted = preferenceStore.getBoolean("onboarding_completed", false)
  val unlimitedNameLines = preferenceStore.getBoolean("unlimited_name_lines", false)
  val hidePlayerButtonsBackground = preferenceStore.getBoolean("hide_player_buttons_background", false)
  val showUnplayedOldVideoLabel = preferenceStore.getBoolean("show_unplayed_old_video_label", true)
  val unplayedOldVideoDays = preferenceStore.getInt("unplayed_old_video_days", 7)
  val showNetworkThumbnails = preferenceStore.getBoolean("show_network_thumbnails", false)
  val showFeaturedHero = preferenceStore.getBoolean("show_featured_hero", true)
  val homeSectionOrder = preferenceStore.getString("home_section_order", HomeSection.defaultOrderCsv)
  val showContinueWatching = preferenceStore.getBoolean("show_continue_watching", true)
  val showUpNext = preferenceStore.getBoolean("show_up_next", true)
  val showRecentlyAdded = preferenceStore.getBoolean("show_recently_added", true)
  val showShowsSection = preferenceStore.getBoolean("show_shows_section", true)
  val showMoviesSection = preferenceStore.getBoolean("show_movies_section", true)
  val showFoldersSection = preferenceStore.getBoolean("show_folders_section", true)
  val seekbarStyle = preferenceStore.getEnum("seekbar_style", SeekbarStyle.Wavy)
  val volumeSliderStyle = preferenceStore.getEnum("volume_slider_style", SeekbarStyle.Wavy)
  val brightnessSliderStyle = preferenceStore.getEnum("brightness_slider_style", SeekbarStyle.Wavy)

  val topLeftControls =
    preferenceStore.getString(
      "top_left_controls",
      "BACK_ARROW,VIDEO_TITLE",
    )

  // Sane-defaults layout: only everyday controls on the bars. Everything
  // else stays one tap away in More > Pro controls or the layout editor.
  val topRightControls =
    preferenceStore.getString(
      "top_right_controls",
      "AUDIO_TRACK,SUBTITLES,MORE_OPTIONS",
    )

  val bottomRightControls =
    preferenceStore.getString(
      "bottom_right_controls",
      "PICTURE_IN_PICTURE,ASPECT_RATIO,HOLD_GESTURE_TOGGLE",
    )

  val bottomLeftControls =
    preferenceStore.getString(
      "bottom_left_controls",
      "PLAYBACK_SPEED,AB_LOOP,EXPLAIN,BOOKMARKS_CHAPTERS",
    )

  val portraitBottomControls =
    preferenceStore.getString(
      "portrait_bottom_controls",
      "AUDIO_TRACK,SUBTITLES,PLAYBACK_SPEED,BOOKMARKS_CHAPTERS,PICTURE_IN_PICTURE,MORE_OPTIONS,AB_LOOP,EXPLAIN",
    )

  fun setHomeSectionVisible(section: HomeSection, visible: Boolean) {
    when (section) {
      HomeSection.FEATURED -> showFeaturedHero.set(visible)
      HomeSection.CONTINUE_WATCHING -> showContinueWatching.set(visible)
      HomeSection.UP_NEXT -> showUpNext.set(visible)
      HomeSection.RECENTLY_ADDED -> showRecentlyAdded.set(visible)
      HomeSection.SHOWS -> showShowsSection.set(visible)
      HomeSection.MOVIES -> showMoviesSection.set(visible)
      HomeSection.FOLDERS -> showFoldersSection.set(visible)
    }
  }

  // Move a section by [delta] positions (-1 up, +1 down); no-op at the ends.
  fun moveHomeSection(section: HomeSection, delta: Int) {
    val order = HomeSection.parseOrder(homeSectionOrder.get()).toMutableList()
    val from = order.indexOf(section)
    val to = from + delta
    if (from < 0 || to !in order.indices) return
    order.add(to, order.removeAt(from))
    homeSectionOrder.set(order.joinToString(",") { it.key })
  }

  fun resetHomeSections() {
    homeSectionOrder.delete()
    showFeaturedHero.delete()
    showContinueWatching.delete()
    showUpNext.delete()
    showRecentlyAdded.delete()
    showShowsSection.delete()
    showMoviesSection.delete()
    showFoldersSection.delete()
  }

  fun parseButtons(
    csv: String,
    usedButtons: MutableSet<PlayerButton>,
  ): List<PlayerButton> =
    csv
      .splitToSequence(',')
      .map { it.trim().uppercase() }
      .mapNotNull { name ->
        try {
          PlayerButton.valueOf(name)
        } catch (_: IllegalArgumentException) {
          null
        }
      }.filter { it != PlayerButton.NONE }
      .filter { usedButtons.add(it) }
      .toList()
}
