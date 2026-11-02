package app.bawsala.android.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SourceLinksTest {
    @Test
    fun `finds the video id in every YouTube link shape`() {
        listOf(
                "https://youtu.be/dQw4w9WgXcQ",
                "https://youtu.be/dQw4w9WgXcQ?si=abc",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://m.youtube.com/watch?feature=share&v=dQw4w9WgXcQ",
                "https://youtube.com/shorts/dQw4w9WgXcQ",
            )
            .forEach { assertEquals("dQw4w9WgXcQ", SourceLinks.youTubeId(it), it) }
        assertNull(SourceLinks.youTubeId("https://example.com/watch?v=dQw4w9WgXcQ"))
        assertNull(SourceLinks.youTubeId(null))
    }

    @Test
    fun `cleans YouTube's share subject to the title`() {
        assertEquals(
            "Building habits",
            SourceLinks.youTubeTitle("Watch “Building habits” on YouTube"),
        )
        assertEquals("عادات صغيرة", SourceLinks.youTubeTitle("شاهِد \"عادات صغيرة\" على YouTube"))
        assertEquals("Plain title", SourceLinks.youTubeTitle(" Plain title "))
        assertNull(SourceLinks.youTubeTitle("  "))
    }

    @Test
    fun `finds Drive and Docs links`() {
        assertEquals(
            "https://drive.google.com/file/d/abc/view?usp=sharing",
            SourceLinks.driveLink(
                "Budget.xlsx https://drive.google.com/file/d/abc/view?usp=sharing"
            ),
        )
        assertEquals(
            "https://docs.google.com/document/d/xyz/edit",
            SourceLinks.driveLink("https://docs.google.com/document/d/xyz/edit"),
        )
        assertNull(SourceLinks.driveLink("https://example.com"))
    }
}
