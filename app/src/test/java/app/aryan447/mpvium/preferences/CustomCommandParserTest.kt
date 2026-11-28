package app.aryan447.mpvium.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomCommandParserTest {

  @Test
  fun parse_emptyOrBlank_returnsEmptyList() {
    assertTrue(CustomCommandParser.parse("").isEmpty())
    assertTrue(CustomCommandParser.parse("   ").isEmpty())
  }

  @Test
  fun parse_plainMpvCommand() {
    val result = CustomCommandParser.parse("cycle pause")
    assertEquals(1, result.size)
    assertEquals(listOf("cycle", "pause"), result[0])
  }

  @Test
  fun parse_plainMpvCommandWithScriptBinding() {
    val result = CustomCommandParser.parse("script-binding Script/delete_file")
    assertEquals(1, result.size)
    assertEquals(listOf("script-binding", "Script/delete_file"), result[0])
  }

  @Test
  fun parse_plainMpvCommandWithQuotedArguments() {
    val result = CustomCommandParser.parse("""show-text "dynamic crop enabled"""")
    assertEquals(1, result.size)
    assertEquals(listOf("show-text", "dynamic crop enabled"), result[0])
  }

  @Test
  fun parse_mpviumSnippetDoubleQuotes() {
    val result = CustomCommandParser.parse("""mp.command("script-binding Script/delete_file")""")
    assertEquals(1, result.size)
    assertEquals(listOf("script-binding", "Script/delete_file"), result[0])
  }

  @Test
  fun parse_mpviumSnippetSingleQuotes() {
    val result = CustomCommandParser.parse("""mp.command('script-binding Script/delete_file')""")
    assertEquals(1, result.size)
    assertEquals(listOf("script-binding", "Script/delete_file"), result[0])
  }

  @Test
  fun parse_mpviumSnippetDoubleBrackets() {
    val result = CustomCommandParser.parse("""mp.command([[script-binding Script/toggle_crop]])""")
    assertEquals(1, result.size)
    assertEquals(listOf("script-binding", "Script/toggle_crop"), result[0])
  }

  @Test
  fun parse_mpviumCommandv() {
    val result = CustomCommandParser.parse("""mp.commandv("script-binding", "Script/delete_file")""")
    assertEquals(1, result.size)
    assertEquals(listOf("script-binding", "Script/delete_file"), result[0])
  }

  @Test
  fun parse_chainedCommandsSemicolon() {
    val result = CustomCommandParser.parse("""cycle pause; show-text "Playback toggled"""")
    assertEquals(2, result.size)
    assertEquals(listOf("cycle", "pause"), result[0])
    assertEquals(listOf("show-text", "Playback toggled"), result[1])
  }

  @Test
  fun parse_semicolonInsideQuotesIsNotSplit() {
    val result = CustomCommandParser.parse("""show-text "part 1; part 2"""")
    assertEquals(1, result.size)
    assertEquals(listOf("show-text", "part 1; part 2"), result[0])
  }
}
