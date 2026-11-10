package app.aryan447.mpvium.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabsTest {

  @Test
  fun entries_containsAllExpectedTabsInCanonicalOrder() {
    val entries = MainTab.entries
    assertEquals(8, entries.size)

    assertEquals("HOME", entries[0].key)
    assertEquals(0, entries[0].canonicalIndex)
    assertEquals("Home", entries[0].label)

    assertEquals("TITLES", entries[1].key)
    assertEquals(1, entries[1].canonicalIndex)
    assertEquals("Titles", entries[1].label)

    assertEquals("SHOWS", entries[2].key)
    assertEquals(1, entries[2].canonicalIndex)
    assertEquals("TV Shows", entries[2].label)

    assertEquals("MOVIES", entries[3].key)
    assertEquals(2, entries[3].canonicalIndex)
    assertEquals("Movies", entries[3].label)

    assertEquals("FOLDERS", entries[4].key)
    assertEquals(3, entries[4].canonicalIndex)
    assertEquals("Folders", entries[4].label)

    assertEquals("LIBRARY", entries[5].key)
    assertEquals(4, entries[5].canonicalIndex)
    assertEquals("Library", entries[5].label)

    assertEquals("PLAYLISTS", entries[6].key)
    assertEquals(5, entries[6].canonicalIndex)
    assertEquals("Playlists", entries[6].label)

    assertEquals("AUDIO", entries[7].key)
    assertEquals(6, entries[7].canonicalIndex)
    assertEquals("Audio", entries[7].label)
  }

  @Test
  fun fromCanonicalIndex_mapsCorrectly() {
    assertEquals(MainTab.HOME, MainTab.fromCanonicalIndex(0))
    assertEquals(MainTab.TITLES, MainTab.fromCanonicalIndex(1))
    assertEquals(MainTab.MOVIES, MainTab.fromCanonicalIndex(2))
    assertEquals(MainTab.FOLDERS, MainTab.fromCanonicalIndex(3))
    assertEquals(MainTab.LIBRARY, MainTab.fromCanonicalIndex(4))
    assertEquals(MainTab.PLAYLISTS, MainTab.fromCanonicalIndex(5))
    assertEquals(MainTab.AUDIO, MainTab.fromCanonicalIndex(6))

    assertNull(MainTab.fromCanonicalIndex(-1))
    assertNull(MainTab.fromCanonicalIndex(7))
  }

  @Test
  fun fromKey_mapsCorrectly() {
    assertEquals(MainTab.HOME, MainTab.fromKey("HOME"))
    assertEquals(MainTab.TITLES, MainTab.fromKey("TITLES"))
    assertEquals(MainTab.SHOWS, MainTab.fromKey("SHOWS"))
    assertEquals(MainTab.MOVIES, MainTab.fromKey("MOVIES"))
    assertEquals(MainTab.FOLDERS, MainTab.fromKey("FOLDERS"))
    assertEquals(MainTab.LIBRARY, MainTab.fromKey("LIBRARY"))
    assertEquals(MainTab.PLAYLISTS, MainTab.fromKey("PLAYLISTS"))
    assertEquals(MainTab.AUDIO, MainTab.fromKey("AUDIO"))

    assertNull(MainTab.fromKey("UNKNOWN"))
  }

  @Test
  fun defaultKeys_containsAllExpectedTabs() {
    val expected = setOf("HOME", "TITLES", "FOLDERS", "LIBRARY", "PLAYLISTS", "AUDIO")
    assertEquals(expected, MainTab.defaultKeys)
    assertTrue("PLAYLISTS must be in defaultKeys for default visibility on", MainTab.defaultKeys.contains("PLAYLISTS"))
    assertTrue("TITLES must be in defaultKeys", MainTab.defaultKeys.contains("TITLES"))
    assertTrue("AUDIO must be in defaultKeys", MainTab.defaultKeys.contains("AUDIO"))
  }

  @Test
  fun visibleTabs_withAllDefaultKeysAndHasAudio_returnsSixTabsInCanonicalOrder() {
    val visible = MainTab.visibleTabs(MainTab.defaultKeys, hasAudio = true)
    assertEquals(6, visible.size)
    assertEquals(listOf(MainTab.HOME, MainTab.TITLES, MainTab.FOLDERS, MainTab.LIBRARY, MainTab.PLAYLISTS, MainTab.AUDIO), visible)
  }

  @Test
  fun visibleTabs_withAllDefaultKeysWhenNoAudio_autoHidesAudioTab() {
    val visible = MainTab.visibleTabs(MainTab.defaultKeys, hasAudio = false)
    assertEquals(5, visible.size)
    assertEquals(listOf(MainTab.HOME, MainTab.TITLES, MainTab.FOLDERS, MainTab.LIBRARY, MainTab.PLAYLISTS), visible)
  }

  @Test
  fun visibleTabs_withSeparateShowsAndMoviesKeys_returnsExpectedTabsInCanonicalOrder() {
    val enabled = setOf("HOME", "SHOWS", "MOVIES", "FOLDERS", "LIBRARY", "PLAYLISTS", "AUDIO")
    val visible = MainTab.visibleTabs(enabled, hasAudio = true)
    assertEquals(7, visible.size)
    assertEquals(listOf(MainTab.HOME, MainTab.SHOWS, MainTab.MOVIES, MainTab.FOLDERS, MainTab.LIBRARY, MainTab.PLAYLISTS, MainTab.AUDIO), visible)
  }

  @Test
  fun visibleTabs_whenTitlesEnabled_suppressesShowsAndMovies() {
    val enabled = setOf("HOME", "TITLES", "SHOWS", "MOVIES", "FOLDERS", "LIBRARY", "PLAYLISTS", "AUDIO")
    val visible = MainTab.visibleTabs(enabled, hasAudio = true)
    assertEquals(6, visible.size)
    assertEquals(listOf(MainTab.HOME, MainTab.TITLES, MainTab.FOLDERS, MainTab.LIBRARY, MainTab.PLAYLISTS, MainTab.AUDIO), visible)
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
  fun visibleTabs_whenEmptySet_fallsBackToDefaultTabs() {
    val visible = MainTab.visibleTabs(emptySet())
    assertEquals(5, visible.size)
    assertEquals(listOf(MainTab.HOME, MainTab.TITLES, MainTab.FOLDERS, MainTab.LIBRARY, MainTab.PLAYLISTS), visible)
  }

  @Test
  fun visibleTabs_whenOnlyUnknownKeys_fallsBackToDefaultTabs() {
    val visible = MainTab.visibleTabs(setOf("NONEXISTENT", "ANOTHER_INVALID"))
    assertEquals(5, visible.size)
    assertEquals(listOf(MainTab.HOME, MainTab.TITLES, MainTab.FOLDERS, MainTab.LIBRARY, MainTab.PLAYLISTS), visible)
  }
}
