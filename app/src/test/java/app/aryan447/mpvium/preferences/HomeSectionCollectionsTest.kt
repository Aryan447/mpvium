package app.aryan447.mpvium.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSectionCollectionsTest {

  @Test
  fun homeSection_containsContinueListening() {
    val section = HomeSection.fromKey("CONTINUE_LISTENING")
    assertEquals(HomeSection.CONTINUE_LISTENING, section)
    assertEquals("Continue Listening", HomeSection.CONTINUE_LISTENING.title)
  }

  @Test
  fun parseOrder_appendsMissingContinueListeningToExistingSavedOrder() {
    // Simulates an existing user configuration that was saved before CONTINUE_LISTENING existed
    val oldSavedCsv = "FEATURED,CONTINUE_WATCHING,UP_NEXT,RECENTLY_ADDED,SHOWS,MOVIES,FOLDERS"
    val parsed = HomeSection.parseOrder(oldSavedCsv)

    assertTrue("Parsed order must include CONTINUE_LISTENING", parsed.contains(HomeSection.CONTINUE_LISTENING))
    assertEquals("First section must remain FEATURED", HomeSection.FEATURED, parsed.first())
  }

  @Test
  fun defaultOrderCsv_includesContinueListening() {
    assertTrue(HomeSection.defaultOrderCsv.contains("CONTINUE_LISTENING"))
  }
}
