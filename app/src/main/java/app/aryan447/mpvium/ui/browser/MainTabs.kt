package app.aryan447.mpvium.ui.browser

import androidx.compose.material.icons.Icons
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
 * canonical index preserves the historical 0..4 mapping used by launcher
 * shortcuts and MainScreen.requestTab.
 */
enum class MainTab(
  val key: String,
  val canonicalIndex: Int,
  val icon: ImageVector,
  val label: String,
) {
  HOME("HOME", 0, Icons.Filled.Home, "Home"),
  SHOWS("SHOWS", 1, Icons.Filled.Tv, "Shows"),
  MOVIES("MOVIES", 2, Icons.Filled.Movie, "Movies"),
  FOLDERS("FOLDERS", 3, Icons.Filled.Folder, "Folders"),
  LIBRARY("LIBRARY", 4, Icons.Filled.VideoLibrary, "Library"),
  ;

  companion object {
    val defaultKeys: Set<String> = entries.map { it.key }.toSet()

    fun fromKey(key: String): MainTab? = entries.firstOrNull { it.key == key }

    fun fromCanonicalIndex(index: Int): MainTab? = entries.firstOrNull { it.canonicalIndex == index }

    /** Visible tabs in canonical order, filtered by enabled keys. Never empty. */
    fun visibleTabs(enabledKeys: Set<String>): List<MainTab> {
      val filtered = entries.filter { it.key in enabledKeys }
      return filtered.ifEmpty { entries.toList() }
    }
  }
}
