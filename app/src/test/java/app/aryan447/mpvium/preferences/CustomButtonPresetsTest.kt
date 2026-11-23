package app.aryan447.mpvium.preferences

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomButtonPresetsTest {

  @Test
  fun presets_areNotEmpty() {
    assertTrue(CustomButtonPresets.PRESETS.isNotEmpty())
  }

  @Test
  fun presets_allHaveValidTitlesAndCommands() {
    for (preset in CustomButtonPresets.PRESETS) {
      assertTrue("Title must not be blank for ${preset.title}", preset.title.isNotBlank())
      assertTrue("Code must not be blank for ${preset.title}", preset.code.isNotBlank())

      val parsed = CustomCommandParser.parse(preset.code)
      assertFalse("Parsed command list must not be empty for code: ${preset.code}", parsed.isEmpty())
      for (cmd in parsed) {
        assertFalse("Command tokens must not be empty", cmd.isEmpty())
        assertTrue("First command token must not be blank", cmd[0].isNotBlank())
      }

      if (preset.longPressCode.isNotBlank()) {
        val parsedLongPress = CustomCommandParser.parse(preset.longPressCode)
        assertFalse(
          "Parsed long-press list must not be empty for code: ${preset.longPressCode}",
          parsedLongPress.isEmpty(),
        )
      }
    }
  }

  @Test
  fun presets_uniqueTitles() {
    val titles = CustomButtonPresets.PRESETS.map { it.title }
    assertTrue("Preset titles should be unique", titles.size == titles.distinct().size)
  }
}
