package app.aryan447.mpvium.preferences

import app.aryan447.mpvium.preferences.preference.PreferenceStore

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
}
