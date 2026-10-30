package app.aryan447.mpvium.ui.theme

import androidx.annotation.StringRes
import app.aryan447.mpvium.R

enum class ThemeSource(
  @StringRes val titleRes: Int,
  @StringRes val summaryRes: Int,
) {
  APP_THEME(
    titleRes = R.string.pref_appearance_theme_source_app_theme,
    summaryRes = R.string.pref_appearance_theme_source_app_theme_summary,
  ),
  CONTENT(
    titleRes = R.string.pref_appearance_theme_source_content,
    summaryRes = R.string.pref_appearance_theme_source_content_summary,
  ),
}
