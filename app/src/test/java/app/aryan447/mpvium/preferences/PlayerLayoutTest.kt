package app.aryan447.mpvium.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLayoutTest {
  @Test
  fun playerLayoutContainsRequiredValues() {
    val names = PlayerLayout.entries.map { it.name }
    assertTrue(names.contains("CLASSIC"))
    assertTrue(names.contains("ZEN"))
    assertTrue(names.contains("MODERN"))
    assertEquals(3, PlayerLayout.entries.size)
  }

  @Test
  fun modernIsTheFirstAndDefaultLayout() {
    assertEquals(PlayerLayout.MODERN, PlayerLayout.entries[0])
  }

  @Test
  fun modernLayoutIsAvailable() {
    assertEquals(PlayerLayout.MODERN, PlayerLayout.valueOf("MODERN"))
  }

  @Test
  fun playerLayoutEntriesOrderIsPreserved() {
    assertEquals(PlayerLayout.MODERN, PlayerLayout.entries[0])
    assertEquals(PlayerLayout.ZEN, PlayerLayout.entries[1])
    assertEquals(PlayerLayout.CLASSIC, PlayerLayout.entries[2])
  }

  @Test
  fun fromStringHandlesLegacyAndNewValues() {
    assertEquals(PlayerLayout.MODERN, PlayerLayout.fromString("MODERN"))
    assertEquals(PlayerLayout.MODERN, PlayerLayout.fromString("MINIMAL"))
    assertEquals(PlayerLayout.MODERN, PlayerLayout.fromString("CINEMA"))
    assertEquals(PlayerLayout.ZEN, PlayerLayout.fromString("ZEN"))
    assertEquals(PlayerLayout.CLASSIC, PlayerLayout.fromString("CLASSIC"))
    assertEquals(PlayerLayout.CLASSIC, PlayerLayout.fromString("GLASS_PILL"))
    assertEquals(PlayerLayout.MODERN, PlayerLayout.fromString("UNKNOWN"))
  }
}
