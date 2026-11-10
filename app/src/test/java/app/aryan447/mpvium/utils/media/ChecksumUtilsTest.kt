package app.aryan447.mpvium.utils.media

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.zip.CRC32

class ChecksumUtilsTest {

  @Test
  fun getCRC32_emptyString_returnsZero() {
    assertEquals("0", ChecksumUtils.getCRC32(""))
  }

  @Test
  fun getCRC32_standardTestVectors_matchesCrc32Specification() {
    // "123456789" is the standard CRC32 check vector: 0xCBF43926
    assertEquals("CBF43926", ChecksumUtils.getCRC32("123456789"))
  }

  @Test
  fun getCRC32_returnsUppercaseHex() {
    val result = ChecksumUtils.getCRC32("mpvium-android-player")
    assertEquals(result.uppercase(), result)
  }

  @Test
  fun getCRC32_unicodeText_matchesJdkImplementation() {
    val testStrings = listOf(
      "葬送のフリーレン",
      "Breaking Bad S05E16",
      "Spirited Away (千と千尋の神隠し)",
      "Special characters: !@#$%^&*()_+~`|}{[]:;?><,./",
    )

    for (str in testStrings) {
      val expected = CRC32().apply { update(str.toByteArray(Charsets.UTF_8)) }.value
      val expectedHex = java.lang.Long.toHexString(expected).uppercase()
      assertEquals(expectedHex, ChecksumUtils.getCRC32(str))
    }
  }
}
