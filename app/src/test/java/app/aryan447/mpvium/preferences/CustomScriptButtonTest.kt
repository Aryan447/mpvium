package app.aryan447.mpvium.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomScriptButtonTest {

  @Test
  fun customScriptButton_roundTripEncodingAndDecoding() {
    val buttons = listOf(
      CustomScriptButton(id = "1", title = "Toggle Shaders", code = "script-binding anime4k/toggle"),
      CustomScriptButton(id = "2", title = "Audio Delay +", code = "add audio-delay 0.1"),
    )

    val encoded = CustomScriptButton.encode(buttons)
    val decoded = CustomScriptButton.decode(encoded)

    assertEquals(buttons, decoded)
  }

  @Test
  fun customScriptButton_decodeEmptyOrBlank_returnsEmptyList() {
    assertTrue(CustomScriptButton.decode("").isEmpty())
    assertTrue(CustomScriptButton.decode("   ").isEmpty())
  }

  @Test
  fun customScriptButton_decodeMalformedJson_returnsEmptyListWithoutThrowing() {
    assertTrue(CustomScriptButton.decode("invalid json").isEmpty())
    assertTrue(CustomScriptButton.decode("{'id': 1}").isEmpty())
  }

  @Test
  fun customScriptButton_decodeIgnoresUnknownKeys() {
    val rawJson = """[{"id":"1","title":"Button","code":"run","extraField":"ignored"}]"""
    val decoded = CustomScriptButton.decode(rawJson)

    assertEquals(1, decoded.size)
    assertEquals("1", decoded[0].id)
    assertEquals("Button", decoded[0].title)
    assertEquals("run", decoded[0].code)
  }

  @Test
  fun customScriptButton_encodeEmptyList_returnsEmptyJsonArray() {
    assertEquals("[]", CustomScriptButton.encode(emptyList()))
  }

  @Test
  fun scriptFileHelper_isScriptFile_detectsSupportedExtensions() {
    assertTrue(ScriptFileHelper.isScriptFile("anime4k.lua"))
    assertTrue(ScriptFileHelper.isScriptFile("controls.js"))
    assertTrue(ScriptFileHelper.isScriptFile("UPPERCASE.LUA"))
    assertTrue(ScriptFileHelper.isScriptFile("mixedCase.Js"))
    assertTrue(ScriptFileHelper.isScriptFile("/path/to/my.script.lua"))
  }

  @Test
  fun scriptFileHelper_isScriptFile_rejectsUnsupportedOrInvalidNames() {
    assertFalse(ScriptFileHelper.isScriptFile("video.mp4"))
    assertFalse(ScriptFileHelper.isScriptFile("script.py"))
    assertFalse(ScriptFileHelper.isScriptFile("script.sh"))
    assertFalse(ScriptFileHelper.isScriptFile("lua"))
    assertFalse(ScriptFileHelper.isScriptFile(""))
    assertFalse(ScriptFileHelper.isScriptFile("   "))
    assertFalse(ScriptFileHelper.isScriptFile(null))
  }
}
