package app.aryan447.mpvium.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLayoutTest {
  @Test
  fun playerLayoutContainsRequiredValues() {
    val names = PlayerLayout.entries.map { it.name }
    assertTrue(names.contains("GLASS_PILL"))
    assertTrue(names.contains("CINEMA"))
    assertEquals(2, PlayerLayout.entries.size)
  }

  @Test
  fun defaultLayoutIsGlassPill() {
    assertEquals(PlayerLayout.GLASS_PILL, PlayerLayout.valueOf("GLASS_PILL"))
  }
}
