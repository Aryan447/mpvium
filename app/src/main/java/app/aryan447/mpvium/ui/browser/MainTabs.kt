package app.aryan447.mpvium.ui.browser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Canonical bottom-navigation tabs in fixed order.
 *
 * Each key is persisted in the bottom_nav_tabs preference set and the
 * canonical index preserves the historical mapping used by launcher
 * shortcuts and MainScreen.requestTab:
 *   - 0: HOME
 *   - 1: TITLES (replaces historical SHOWS at 1 and MOVIES at 2)
 *   - 2: RESERVED (historical MOVIES; maps to TITLES with Movies filter preselected)
 *   - 3: FOLDERS
 *   - 4: LIBRARY
 *   - 5: PLAYLISTS
 *
 * All tabs (including PLAYLISTS) are enabled by default.
 */
enum class MainTab(
  val key: String,
  val canonicalIndex: Int,
  val icon: ImageVector,
  val label: String,
) {
  HOME("HOME", 0, Icons.Filled.Home, "Home"),
  TITLES("TITLES", 1, Icons.Filled.Movie, "Titles"),
  FOLDERS("FOLDERS", 3, Icons.Filled.Folder, "Folders"),
  LIBRARY("LIBRARY", 4, Icons.Filled.VideoLibrary, "Library"),
  PLAYLISTS("PLAYLISTS", 5, Icons.AutoMirrored.Filled.PlaylistPlay, "Playlists"),
  ;

  companion object {
    /**
     * Historical canonical index for the separate Movies tab (spec 10).
     * Index 2 is kept reserved so launcher shortcuts or external requests
     * with extra_tab=2 map to TITLES with the Movies filter preselected.
     */
    const val RESERVED_MOVIES_CANONICAL_INDEX = 2

    val defaultKeys: Set<String> = entries.map { it.key }.toSet()

    fun fromKey(key: String): MainTab? = when (key) {
      "SHOWS", "MOVIES" -> TITLES
      else -> entries.firstOrNull { it.key == key }
    }

    fun fromCanonicalIndex(index: Int): MainTab? = when (index) {
      RESERVED_MOVIES_CANONICAL_INDEX -> TITLES
      else -> entries.firstOrNull { it.canonicalIndex == index }
    }

    /** Visible tabs in canonical order, filtered by enabled keys. Never empty. */
    fun visibleTabs(enabledKeys: Set<String>): List<MainTab> {
      val effectiveKeys = if ("SHOWS" in enabledKeys || "MOVIES" in enabledKeys) {
        enabledKeys + TITLES.key
      } else {
        enabledKeys
      }
      val filtered = entries.filter { it.key in effectiveKeys }
      return filtered.ifEmpty { entries.toList() }
    }
  }
}
