package app.aryan447.mpvium.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabsTest {

  @Test
  fun entries_containsAllExpectedTabsInCanonicalOrder() {
    val entries = MainTab.entries
    assertEquals(5, entries.size)

    assertEquals("HOME", entries[0].key)
    assertEquals(0, entries[0].canonicalIndex)
    assertEquals("Home", entries[0].label)

    assertEquals("TITLES", entries[1].key)
    assertEquals(1, entries[1].canonicalIndex)
    assertEquals("Titles", entries[1].label)

    assertEquals("FOLDERS", entries[2].key)
    assertEquals(3, entries[2].canonicalIndex)
    assertEquals("Folders", entries[2].label)

    assertEquals("LIBRARY", entries[3].key)
    assertEquals(4, entries[3].canonicalIndex)
    assertEquals("Library", entries[3].label)

    assertEquals("PLAYLISTS", entries[4].key)
    assertEquals(5, entries[4].canonicalIndex)
    assertEquals("Playlists", entries[4].label)
  }

  @Test
  fun fromCanonicalIndex_mapsCorrectly() {
    assertEquals(MainTab.HOME, MainTab.fromCanonicalIndex(0))
    assertEquals(MainTab.TITLES, MainTab.fromCanonicalIndex(1))
    // Index 2 is reserved for historical Movies tab, resolving to TITLES
    assertEquals(MainTab.TITLES, MainTab.fromCanonicalIndex(2))
    assertEquals(MainTab.FOLDERS, MainTab.fromCanonicalIndex(3))
    assertEquals(MainTab.LIBRARY, MainTab.fromCanonicalIndex(4))
    assertEquals(MainTab.PLAYLISTS, MainTab.fromCanonicalIndex(5))

    assertNull(MainTab.fromCanonicalIndex(-1))
    assertNull(MainTab.fromCanonicalIndex(6))
  }

  @Test
  fun fromKey_mapsCorrectly() {
    assertEquals(MainTab.HOME, MainTab.fromKey("HOME"))
    assertEquals(MainTab.TITLES, MainTab.fromKey("TITLES"))
    // Historical SHOWS and MOVIES map to TITLES
    assertEquals(MainTab.TITLES, MainTab.fromKey("SHOWS"))
    assertEquals(MainTab.TITLES, MainTab.fromKey("MOVIES"))
    assertEquals(MainTab.FOLDERS, MainTab.fromKey("FOLDERS"))
    assertEquals(MainTab.LIBRARY, MainTab.fromKey("LIBRARY"))
    assertEquals(MainTab.PLAYLISTS, MainTab.fromKey("PLAYLISTS"))

    assertNull(MainTab.fromKey("UNKNOWN"))
  }

  @Test
  fun defaultKeys_containsAllFiveTabs() {
    val expected = setOf("HOME", "TITLES", "FOLDERS", "LIBRARY", "PLAYLISTS")
    assertEquals(expected, MainTab.defaultKeys)
    assertTrue("PLAYLISTS must be in defaultKeys for default visibility on", MainTab.defaultKeys.contains("PLAYLISTS"))
    assertTrue("TITLES must be in defaultKeys", MainTab.defaultKeys.contains("TITLES"))
  }

  @Test
  fun visibleTabs_withAllKeys_returnsAllTabsInCanonicalOrder() {
    val visible = MainTab.visibleTabs(MainTab.defaultKeys)
    assertEquals(5, visible.size)
    assertEquals(listOf(MainTab.HOME, MainTab.TITLES, MainTab.FOLDERS, MainTab.LIBRARY, MainTab.PLAYLISTS), visible)
  }

  @Test
  fun visibleTabs_withHistoricalShowsOrMoviesKey_mapsToTitles() {
    val enabled = setOf("HOME", "MOVIES", "FOLDERS", "LIBRARY", "PLAYLISTS")
    val visible = MainTab.visibleTabs(enabled)
    assertEquals(5, visible.size)
    assertEquals(listOf(MainTab.HOME, MainTab.TITLES, MainTab.FOLDERS, MainTab.LIBRARY, MainTab.PLAYLISTS), visible)
  }

  @Test
  fun visibleTabs_whenTabHidden_preservesCanonicalOrder() {
    val enabled = setOf("HOME", "FOLDERS", "LIBRARY", "PLAYLISTS") // TITLES hidden
    val visible = MainTab.visibleTabs(enabled)
    assertEquals(4, visible.size)
    assertEquals(listOf(MainTab.HOME, MainTab.FOLDERS, MainTab.LIBRARY, MainTab.PLAYLISTS), visible)
  }

  @Test
  fun visibleTabs_whenOnlyPlaylistsAndHome_preservesCanonicalOrder() {
    val enabled = setOf("PLAYLISTS", "HOME")
    val visible = MainTab.visibleTabs(enabled)
    assertEquals(2, visible.size)
    assertEquals(listOf(MainTab.HOME, MainTab.PLAYLISTS), visible)
  }

  @Test
  fun visibleTabs_whenEmptySet_fallsBackToAllTabs() {
    val visible = MainTab.visibleTabs(emptySet())
    assertEquals(5, visible.size)
    assertEquals(MainTab.entries.toList(), visible)
  }

  @Test
  fun visibleTabs_whenOnlyUnknownKeys_fallsBackToAllTabs() {
    val visible = MainTab.visibleTabs(setOf("NONEXISTENT", "ANOTHER_INVALID"))
    assertEquals(5, visible.size)
    assertEquals(MainTab.entries.toList(), visible)
  }
}
