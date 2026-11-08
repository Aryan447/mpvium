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
    assertEquals(2, PlayerLayout.entries.size)
  }

  @Test
  fun defaultLayoutIsClassic() {
    assertEquals(PlayerLayout.CLASSIC, PlayerLayout.valueOf("CLASSIC"))
  }

  @Test
  fun zenLayoutIsAvailable() {
    assertEquals(PlayerLayout.ZEN, PlayerLayout.valueOf("ZEN"))
  }

  @Test
  fun playerLayoutEntriesOrderIsPreserved() {
    assertEquals(PlayerLayout.CLASSIC, PlayerLayout.entries[0])
    assertEquals(PlayerLayout.ZEN, PlayerLayout.entries[1])
  }

  @Test
  fun fromStringHandlesLegacyAndNewValues() {
    assertEquals(PlayerLayout.ZEN, PlayerLayout.fromString("ZEN"))
    assertEquals(PlayerLayout.ZEN, PlayerLayout.fromString("MINIMAL"))
    assertEquals(PlayerLayout.ZEN, PlayerLayout.fromString("CINEMA"))
    assertEquals(PlayerLayout.CLASSIC, PlayerLayout.fromString("CLASSIC"))
    assertEquals(PlayerLayout.CLASSIC, PlayerLayout.fromString("GLASS_PILL"))
    assertEquals(PlayerLayout.CLASSIC, PlayerLayout.fromString("UNKNOWN"))
  }
}
