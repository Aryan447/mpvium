package app.aryan447.mpvium.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabsTest {

  @Test
  fun entries_containsAllExpectedTabsInCanonicalOrder() {
    val entries = MainTab.entries
    assertEquals(6, entries.size)

    assertEquals("HOME", entries[0].key)
    assertEquals(0, entries[0].canonicalIndex)
    assertEquals("Home", entries[0].label)

    assertEquals("SHOWS", entries[1].key)
    assertEquals(1, entries[1].canonicalIndex)
    assertEquals("Shows", entries[1].label)

    assertEquals("MOVIES", entries[2].key)
    assertEquals(2, entries[2].canonicalIndex)
    assertEquals("Movies", entries[2].label)

    assertEquals("FOLDERS", entries[3].key)
    assertEquals(3, entries[3].canonicalIndex)
    assertEquals("Folders", entries[3].label)

    assertEquals("LIBRARY", entries[4].key)
    assertEquals(4, entries[4].canonicalIndex)
    assertEquals("Library", entries[4].label)

    assertEquals("PLAYLISTS", entries[5].key)
    assertEquals(5, entries[5].canonicalIndex)
    assertEquals("Playlists", entries[5].label)
  }

  @Test
  fun fromCanonicalIndex_mapsCorrectly() {
    assertEquals(MainTab.HOME, MainTab.fromCanonicalIndex(0))
    assertEquals(MainTab.SHOWS, MainTab.fromCanonicalIndex(1))
    assertEquals(MainTab.MOVIES, MainTab.fromCanonicalIndex(2))
    assertEquals(MainTab.FOLDERS, MainTab.fromCanonicalIndex(3))
    assertEquals(MainTab.LIBRARY, MainTab.fromCanonicalIndex(4))
    assertEquals(MainTab.PLAYLISTS, MainTab.fromCanonicalIndex(5))

    assertNull(MainTab.fromCanonicalIndex(-1))
    assertNull(MainTab.fromCanonicalIndex(6))
  }

  @Test
  fun fromKey_mapsCorrectly() {
    assertEquals(MainTab.HOME, MainTab.fromKey("HOME"))
    assertEquals(MainTab.SHOWS, MainTab.fromKey("SHOWS"))
    assertEquals(MainTab.MOVIES, MainTab.fromKey("MOVIES"))
    assertEquals(MainTab.FOLDERS, MainTab.fromKey("FOLDERS"))
    assertEquals(MainTab.LIBRARY, MainTab.fromKey("LIBRARY"))
    assertEquals(MainTab.PLAYLISTS, MainTab.fromKey("PLAYLISTS"))

    assertNull(MainTab.fromKey("UNKNOWN"))
  }

  @Test
  fun defaultKeys_containsAllSixTabs() {
    val expected = setOf("HOME", "SHOWS", "MOVIES", "FOLDERS", "LIBRARY", "PLAYLISTS")
    assertEquals(expected, MainTab.defaultKeys)
    assertTrue("PLAYLISTS must be in defaultKeys for default visibility on", MainTab.defaultKeys.contains("PLAYLISTS"))
  }

  @Test
  fun visibleTabs_withAllKeys_returnsAllTabsInCanonicalOrder() {
    val visible = MainTab.visibleTabs(MainTab.defaultKeys)
    assertEquals(6, visible.size)
    assertEquals(listOf(MainTab.HOME, MainTab.SHOWS, MainTab.MOVIES, MainTab.FOLDERS, MainTab.LIBRARY, MainTab.PLAYLISTS), visible)
  }

  @Test
  fun visibleTabs_whenTabHidden_preservesCanonicalOrder() {
    val enabled = setOf("HOME", "MOVIES", "FOLDERS", "LIBRARY", "PLAYLISTS") // SHOWS hidden
    val visible = MainTab.visibleTabs(enabled)
    assertEquals(5, visible.size)
    assertEquals(listOf(MainTab.HOME, MainTab.MOVIES, MainTab.FOLDERS, MainTab.LIBRARY, MainTab.PLAYLISTS), visible)
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
    // At-least-one-tab protection guard
    val visible = MainTab.visibleTabs(emptySet())
    assertEquals(6, visible.size)
    assertEquals(MainTab.entries.toList(), visible)
  }

  @Test
  fun visibleTabs_whenOnlyUnknownKeys_fallsBackToAllTabs() {
    val visible = MainTab.visibleTabs(setOf("NONEXISTENT", "ANOTHER_INVALID"))
    assertEquals(6, visible.size)
    assertEquals(MainTab.entries.toList(), visible)
  }
}
