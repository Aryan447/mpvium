package app.aryan447.mpvium.preferences

import app.aryan447.mpvium.preferences.preference.PreferenceStore
import app.aryan447.mpvium.preferences.preference.getEnum

/**
 * Scan mode for the media library.
 */
enum class LibraryScanMode {
  AUTO,
  MANUAL,
}

/**
 * Preferences for folder management
 */
class FoldersPreferences(
  preferenceStore: PreferenceStore,
) {
  // Set of folder paths that should be hidden from the folder list
  val blacklistedFolders = preferenceStore.getStringSet("blacklisted_folders", emptySet())

  // Set of folder paths that are allowed when whitelist-only mode is enabled.
  // Subfolders of a whitelisted folder are included automatically.
  val whitelistedFolders = preferenceStore.getStringSet("whitelisted_folders", emptySet())

  // When true (and the whitelist is non-empty), only whitelisted folders are shown.
  val whitelistOnlyEnabled = preferenceStore.getBoolean("whitelist_only_enabled", false)

  // Scan mode: AUTO (scan storage on tab open) vs MANUAL (load snapshot, rescan on pull-to-refresh)
  val libraryScanMode = preferenceStore.getEnum("library_scan_mode", LibraryScanMode.AUTO)
}
