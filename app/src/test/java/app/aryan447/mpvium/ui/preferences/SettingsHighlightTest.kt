package app.aryan447.mpvium.ui.preferences

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsHighlightTest {
  @After
  fun clearPendingHighlight() {
    SettingsHighlight.consume(AppearancePreferencesScreen)
    SettingsHighlight.consume(AudioPreferencesScreen)
    SettingsHighlight.activeKey?.let(SettingsHighlight::deactivate)
  }

  @Test
  fun requestIsConsumedOnceByItsScreen() {
    SettingsHighlight.request(AppearancePreferencesScreen, "appearance.amoled")

    assertEquals("appearance.amoled", SettingsHighlight.consume(AppearancePreferencesScreen))
    assertNull(SettingsHighlight.consume(AppearancePreferencesScreen))
  }

  @Test
  fun requestIsKeptForTheTargetScreen() {
    SettingsHighlight.request(AppearancePreferencesScreen, "appearance.amoled")

    assertNull(SettingsHighlight.consume(AudioPreferencesScreen))
    assertEquals("appearance.amoled", SettingsHighlight.consume(AppearancePreferencesScreen))
  }

  @Test
  fun deactivatingClearsTheTintedKey() {
    SettingsHighlight.activate("appearance.amoled")
    assertEquals("appearance.amoled", SettingsHighlight.activeKey)

    SettingsHighlight.deactivate("appearance.amoled")
    assertNull(SettingsHighlight.activeKey)
  }
}
