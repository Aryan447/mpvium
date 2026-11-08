package app.aryan447.mpvium.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLayoutTest {
  @Test
  fun playerLayoutContainsRequiredValues() {
    val names = PlayerLayout.entries.map { it.name }
    assertTrue(names.contains("CLASSIC"))
    assertTrue(names.contains("CINEMA"))
    assertEquals(2, PlayerLayout.entries.size)
  }

  @Test
  fun defaultLayoutIsClassic() {
    assertEquals(PlayerLayout.CLASSIC, PlayerLayout.valueOf("CLASSIC"))
  }

  @Test
  fun cinemaLayoutIsAvailable() {
    assertEquals(PlayerLayout.CINEMA, PlayerLayout.valueOf("CINEMA"))
  }

  @Test
  fun playerLayoutEntriesOrderIsPreserved() {
    assertEquals(PlayerLayout.CLASSIC, PlayerLayout.entries[0])
    assertEquals(PlayerLayout.CINEMA, PlayerLayout.entries[1])
  }
}
