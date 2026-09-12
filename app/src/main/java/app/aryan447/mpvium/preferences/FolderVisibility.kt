package app.aryan447.mpvium.preferences

/**
 * Shared folder-visibility rules for blacklist / whitelist-only mode.
 *
 * - Whitelist-only ON (and whitelist non-empty): a folder is visible only if it
 *   equals a whitelisted folder or is a descendant of one. The blacklist is ignored.
 * - Otherwise: a folder is hidden if it equals or is a descendant of a blacklisted
 *   folder. Descendant matching fixes blacklisting a parent while its subfolders
 *   still show up.
 * - Whitelist-only ON with an empty whitelist is a fail-open no-op so users can't
 *   lock themselves out of their whole library.
 */
object FolderVisibility {
  fun isFolderVisible(
    folderPath: String,
    whitelist: Set<String>,
    blacklist: Set<String>,
    whitelistOnly: Boolean,
  ): Boolean {
    val normalized = folderPath.trimEnd('/')
    if (whitelistOnly && whitelist.isNotEmpty()) {
      return whitelist.any { isSameOrDescendant(normalized, it) }
    }
    return blacklist.none { isSameOrDescendant(normalized, it) }
  }

  /**
   * True when navigating through [path] could still lead to visible content.
   * Used by the file browser so a hidden parent doesn't block traversal into a
   * whitelisted child (e.g. /storage/emulated/0 -> /storage/emulated/0/Movies).
   */
  fun canContainVisible(
    path: String,
    whitelist: Set<String>,
    blacklist: Set<String>,
    whitelistOnly: Boolean,
  ): Boolean {
    val normalized = path.trimEnd('/')
    if (whitelistOnly && whitelist.isNotEmpty()) {
      return whitelist.any {
        val entry = it.trimEnd('/')
        entry == normalized || entry.startsWith("$normalized/") || normalized.startsWith("$entry/")
      }
    }
    // In blacklist mode every directory remains traversable; only listed
    // folders (and descendants) are hidden from listings.
    return true
  }

  private fun isSameOrDescendant(path: String, entry: String): Boolean {
    val normalizedEntry = entry.trimEnd('/')
    if (normalizedEntry.isEmpty()) return false
    return path == normalizedEntry || path.startsWith("$normalizedEntry/")
  }
}
