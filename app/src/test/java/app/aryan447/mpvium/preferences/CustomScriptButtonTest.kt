package app.aryan447.mpvium.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomScriptButtonTest {

  @Test
  fun customScriptButton_roundTripEncodingAndDecoding() {
    val buttons = listOf(
      CustomScriptButton(
        id = "1",
        title = "Toggle Shaders",
        code = "script-binding anime4k/toggle",
        longPressCode = "script-binding anime4k/cycle",
        enabled = true,
      ),
      CustomScriptButton(
        id = "2",
        title = "Audio Delay +",
        code = "add audio-delay 0.1",
        longPressCode = "",
        enabled = false,
      ),
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
  fun customScriptButton_decodeIgnoresUnknownKeysAndProvidesDefaults() {
    val rawJson = """[{"id":"1","title":"Button","code":"run","extraField":"ignored"}]"""
    val decoded = CustomScriptButton.decode(rawJson)

    assertEquals(1, decoded.size)
    assertEquals("1", decoded[0].id)
    assertEquals("Button", decoded[0].title)
    assertEquals("run", decoded[0].code)
    assertEquals("", decoded[0].longPressCode)
    assertTrue(decoded[0].enabled)
    assertEquals("bottom", decoded[0].position)
  }

  @Test
  fun customScriptButton_encodeEmptyList_returnsEmptyJsonArray() {
    assertEquals("[]", CustomScriptButton.encode(emptyList()))
  }

  @Test
  fun customScriptButton_fromXml_parsesMpvExFormat() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <customButtons>
        <button slot="0">
          <id>991e516e-10a7-468c-901a-67786036bc45</id>
          <title>Delete File</title>
          <enabled>true</enabled>
          <content><![CDATA[mp.command("script-binding Script/delete_file")]]></content>
          <longPressContent><![CDATA[mp.command("script-binding Script/list_marks")]]></longPressContent>
          <onStartup><![CDATA[]]></onStartup>
        </button>
        <button slot="1">
          <id>6fe0390d-b7de-456e-a7f9-ba987c90fd89</id>
          <title>D-Crop</title>
          <enabled>false</enabled>
          <content><![CDATA[mp.command("script-binding Script/toggle_crop")]]></content>
          <longPressContent><![CDATA[]]></longPressContent>
        </button>
      </customButtons>
    """.trimIndent()

    val parsed = CustomScriptButton.fromXml(xml)

    assertEquals(2, parsed.size)
    assertEquals("991e516e-10a7-468c-901a-67786036bc45", parsed[0].id)
    assertEquals("Delete File", parsed[0].title)
    assertTrue(parsed[0].enabled)
    assertEquals("""mp.command("script-binding Script/delete_file")""", parsed[0].code)
    assertEquals("""mp.command("script-binding Script/list_marks")""", parsed[0].longPressCode)

    assertEquals("6fe0390d-b7de-456e-a7f9-ba987c90fd89", parsed[1].id)
    assertEquals("D-Crop", parsed[1].title)
    assertFalse(parsed[1].enabled)
    assertEquals("""mp.command("script-binding Script/toggle_crop")""", parsed[1].code)
    assertEquals("", parsed[1].longPressCode)
  }

  @Test
  fun customScriptButton_xmlRoundTrip() {
    val buttons = listOf(
      CustomScriptButton(
        id = "btn-1",
        title = "S-Delay +",
        code = """mp.command("script-binding Script/sub-delay+")""",
        longPressCode = """mp.command("script-binding Script/sub-delay-hold+")""",
        enabled = true,
      ),
      CustomScriptButton(
        id = "btn-2",
        title = "Pause",
        code = "cycle pause",
        longPressCode = "",
        enabled = false,
      ),
    )

    val xml = CustomScriptButton.toXml(buttons)
    val parsed = CustomScriptButton.fromXml(xml)

    assertEquals(buttons.size, parsed.size)
    assertEquals(buttons[0].id, parsed[0].id)
    assertEquals(buttons[0].title, parsed[0].title)
    assertEquals(buttons[0].code, parsed[0].code)
    assertEquals(buttons[0].longPressCode, parsed[0].longPressCode)
    assertEquals(buttons[0].enabled, parsed[0].enabled)

    assertEquals(buttons[1].id, parsed[1].id)
    assertEquals(buttons[1].title, parsed[1].title)
    assertEquals(buttons[1].code, parsed[1].code)
    assertEquals(buttons[1].longPressCode, parsed[1].longPressCode)
    assertEquals(buttons[1].enabled, parsed[1].enabled)
  }

  @Test
  fun customScriptButton_fromXml_emptyOrInvalid_returnsEmptyList() {
    assertTrue(CustomScriptButton.fromXml("").isEmpty())
    assertTrue(CustomScriptButton.fromXml("   ").isEmpty())
    assertTrue(CustomScriptButton.fromXml("not xml").isEmpty())
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

  @Test
  fun scriptFileHelper_normalizeScriptName_normalizesTxtFiles() {
    assertEquals("Script.lua", ScriptFileHelper.normalizeScriptName("Script.txt"))
    assertEquals("Script.lua", ScriptFileHelper.normalizeScriptName("Script.lua.txt"))
    assertEquals("Script.js", ScriptFileHelper.normalizeScriptName("Script.js.txt"))
    assertEquals("Script.lua", ScriptFileHelper.normalizeScriptName("Script.lua"))
    assertEquals("controls.js", ScriptFileHelper.normalizeScriptName("controls.js"))
    assertEquals("anime.LUA", ScriptFileHelper.normalizeScriptName("anime.LUA"))
    assertEquals(null, ScriptFileHelper.normalizeScriptName("video.mp4"))
    assertEquals(null, ScriptFileHelper.normalizeScriptName(""))
    assertEquals(null, ScriptFileHelper.normalizeScriptName(null))
  }

  @Test
  fun customScriptButton_fromXml_parsesMpvExExportXml() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <customButtons>
        <button slot="0">
          <id>991e516e-10a7-468c-901a-67786036bc45</id>
          <title>Delete File</title>
          <enabled>true</enabled>
          <content><![CDATA[mp.command("script-binding Script/delete_file")]]></content>
          <longPressContent><![CDATA[mp.command("script-binding Script/list_marks")]]></longPressContent>
          <onStartup><![CDATA[]]></onStartup>
        </button>
        <button slot="1">
          <id>6fe0390d-b7de-456e-a7f9-ba987c90fd89</id>
          <title>D-Crop</title>
          <enabled>true</enabled>
          <content><![CDATA[mp.command("script-binding Script/toggle_crop")]]></content>
          <longPressContent><![CDATA[]]></longPressContent>
          <onStartup><![CDATA[]]></onStartup>
        </button>
      </customButtons>
    """.trimIndent()

    val parsed = CustomScriptButton.fromXml(xml)
    assertEquals(2, parsed.size)
    assertEquals("Delete File", parsed[0].title)
    assertEquals("""mp.command("script-binding Script/delete_file")""", parsed[0].code)
    assertEquals("""mp.command("script-binding Script/list_marks")""", parsed[0].longPressCode)
    assertTrue(parsed[0].enabled)

    assertEquals("D-Crop", parsed[1].title)
    assertEquals("""mp.command("script-binding Script/toggle_crop")""", parsed[1].code)
    assertEquals("", parsed[1].longPressCode)
    assertEquals("bottom", parsed[0].position)
    assertEquals("bottom", parsed[1].position)
  }

  @Test
  fun customScriptButton_fromXml_parsesPositionTag() {
    val xml = """
      <customButtons>
        <button slot="0">
          <title>Stats</title>
          <content>script-binding stats/display-stats-toggle</content>
          <position>top</position>
        </button>
      </customButtons>
    """.trimIndent()

    val parsed = CustomScriptButton.fromXml(xml)
    assertEquals(1, parsed.size)
    assertEquals("top", parsed[0].position)
  }
}
