package app.aryan447.mpvium.ui.browser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Canonical bottom-navigation tabs in fixed order.
 *
 * Each key is persisted in the bottom_nav_tabs preference set and the
 * canonical index preserves the historical mapping used by launcher
 * shortcuts and MainScreen.requestTab:
 *   - 0: HOME
 *   - 1: TITLES (unified) or SHOWS (separate)
 *   - 2: MOVIES (separate; reserved when TITLES is unified)
 *   - 3: FOLDERS
 *   - 4: LIBRARY
 *   - 5: PLAYLISTS
 *   - 6: AUDIO (auto-hidden when no audio exists on device)
 *
 * By default, unified TITLES is enabled (5 visible tabs). Users can switch
 * to separate SHOWS and MOVIES under Settings > Appearance > Bottom navigation.
 */
enum class MainTab(
  val key: String,
  val canonicalIndex: Int,
  val icon: ImageVector,
  val label: String,
) {
  HOME("HOME", 0, Icons.Filled.Home, "Home"),
  TITLES("TITLES", 1, Icons.Filled.Movie, "Titles"),
  SHOWS("SHOWS", 1, Icons.Filled.Tv, "TV Shows"),
  MOVIES("MOVIES", 2, Icons.Filled.Movie, "Movies"),
  FOLDERS("FOLDERS", 3, Icons.Filled.Folder, "Folders"),
  LIBRARY("LIBRARY", 4, Icons.Filled.VideoLibrary, "Library"),
  PLAYLISTS("PLAYLISTS", 5, Icons.AutoMirrored.Filled.PlaylistPlay, "Playlists"),
  AUDIO("AUDIO", 6, Icons.Filled.Audiotrack, "Audio"),
  ;

  companion object {
    /**
     * Historical canonical index for the separate Movies tab (spec 10).
     * Index 2 is kept reserved when unified Titles tab is active so launcher
     * shortcuts or external requests map to TITLES with Movies filter preselected.
     */
    const val RESERVED_MOVIES_CANONICAL_INDEX = 2

    val defaultKeys: Set<String> = setOf(HOME.key, TITLES.key, FOLDERS.key, LIBRARY.key, PLAYLISTS.key, AUDIO.key)

    fun fromKey(key: String): MainTab? = entries.firstOrNull { it.key == key }

    fun fromCanonicalIndex(index: Int): MainTab? = when (index) {
      0 -> HOME
      1 -> TITLES
      2 -> MOVIES
      3 -> FOLDERS
      4 -> LIBRARY
      5 -> PLAYLISTS
      6 -> AUDIO
      else -> null
    }

    /** Visible tabs in canonical order, filtered by enabled keys. Never empty. */
    fun visibleTabs(enabledKeys: Set<String>, hasAudio: Boolean = true): List<MainTab> {
      val effectiveKeys = if (TITLES.key in enabledKeys) {
        enabledKeys - SHOWS.key - MOVIES.key
      } else {
        enabledKeys
      }
      val filtered = entries.filter { tab ->
        tab.key in effectiveKeys && (tab != AUDIO || hasAudio)
      }
      return filtered.ifEmpty { listOf(HOME, TITLES, FOLDERS, LIBRARY, PLAYLISTS) }
    }
  }
}
