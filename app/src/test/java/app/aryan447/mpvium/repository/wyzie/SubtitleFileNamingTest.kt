package app.aryan447.mpvium.repository.wyzie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleFileNamingTest {

    private fun subtitle(url: String, fileName: String? = null, origin: String? = null) = WyzieSubtitle(
        url = url,
        fileName = fileName,
        origin = origin,
    )

    @Test
    fun `same result url always maps to the same tag`() {
        val result = subtitle(
            url = "https://example.com/sub/1.srt",
            fileName = "Spider-Man.Homecoming.2017.1080p.WEB-DL.x264-FGT.srt",
        )

        assertEquals(
            WyzieSearchRepository.subtitleFileTag(result),
            WyzieSearchRepository.subtitleFileTag(result.copy(url = result.url)),
        )
    }

    @Test
    fun `different result urls map to different tags`() {
        val first = subtitle(
            url = "https://example.com/sub/1.srt",
            fileName = "Spider-Man.Homecoming.2017.1080p.WEB-DL.x264-FGT.srt",
        )
        val second = subtitle(
            url = "https://example.com/sub/2.srt",
            fileName = "Spider-Man.Homecoming.2017.1080p.WEB-DL.x264-FGT.srt",
        )

        assertNotEquals(
            WyzieSearchRepository.subtitleFileTag(first),
            WyzieSearchRepository.subtitleFileTag(second),
        )
    }

    @Test
    fun `tag prefers release group then origin`() {
        val withGroup = subtitle(
            url = "https://example.com/sub/1.srt",
            fileName = "Movie.2020.1080p.BluRay.x264-SPARKS.srt",
        )
        val withOrigin = subtitle(
            url = "https://example.com/sub/2.srt",
            fileName = "Movie.2020.1080p.BluRay.srt",
            origin = "SubSource",
        )

        assertTrue(WyzieSearchRepository.subtitleFileTag(withGroup).startsWith("SPARKS-"))
        assertTrue(WyzieSearchRepository.subtitleFileTag(withOrigin).startsWith("SubSource-"))
    }

    @Test
    fun `unsafe filename characters are stripped`() {
        val sanitized = WyzieSearchRepository.sanitizeFileSegment("Bad/Name:With*Illegal?\"<>|Chars\u0000")

        assertFalse(sanitized.contains('/'))
        assertFalse(sanitized.contains(':'))
        assertFalse(sanitized.contains('*'))
        assertFalse(sanitized.contains('?'))
        assertFalse(sanitized.contains('<'))
        assertFalse(sanitized.contains('>'))
        assertFalse(sanitized.contains('|'))
        assertEquals("BadNameWithIllegalChars", sanitized)
    }

    @Test
    fun `tag is safe to use as a filename segment`() {
        val tag = WyzieSearchRepository.subtitleFileTag(
            subtitle(
                url = "https://example.com/sub/1.srt",
                fileName = "Weird: Release / Name?.srt",
            ),
        )

        assertTrue(tag.isNotBlank())
        assertTrue(tag.all { it.isLetterOrDigit() || it == '-' })
    }
}
