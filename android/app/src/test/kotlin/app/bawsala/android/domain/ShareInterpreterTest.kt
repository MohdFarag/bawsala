package app.bawsala.android.domain

import app.bawsala.core.model.Attachment
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.SourceRef
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShareInterpreterTest {
    private fun share(
        text: String? = null,
        subject: String? = null,
        attachments: List<Attachment> = emptyList(),
        from: String? = "com.google.android.keep",
    ) = ShareInterpreter.toCapture(SharedContent(text, subject, attachments, from))

    @Test
    fun `known apps map to source services`() {
        assertEquals("keep", share("a").source.service)
        assertEquals("gmail", share("a", from = "com.google.android.gm").source.service)
        assertEquals("share:org.example", share("a", from = "org.example").source.service)
        assertEquals("share", share("a", from = null).source.service)
    }

    @Test
    fun `the subject is prepended unless the text already starts with it`() {
        assertEquals("Groceries\nMilk", share("Milk", subject = "Groceries").text)
        assertEquals("Groceries list", share("Groceries list", subject = "Groceries").text)
    }

    @Test
    fun `a YouTube share is a link under the video's title`() {
        val input =
            share(
                "https://youtu.be/dQw4w9WgXcQ?si=x",
                subject = "Watch “Small habits” on YouTube",
                from = "com.google.android.youtube",
            )
        assertEquals(CaptureKind.LINK, input.kind)
        assertEquals("Small habits", input.title)
        assertEquals("https://youtu.be/dQw4w9WgXcQ?si=x", input.text)
        assertEquals("youtube", input.source.service)
        val item = CaptureFactory.create("i", input, Instant.EPOCH) { "untitled" }
        assertEquals("Small habits", item.title)
        assertEquals("https://youtu.be/dQw4w9WgXcQ?si=x", item.url)
    }

    @Test
    fun `a Drive link stays a link named after the file, while a sent copy stays a file`() {
        val link =
            share(
                "https://drive.google.com/file/d/abc/view?usp=sharing",
                subject = "Budget.xlsx",
                from = "com.google.android.apps.docs",
            )
        assertEquals(CaptureKind.LINK, link.kind)
        assertEquals("Budget.xlsx", link.title)
        assertEquals("drive", link.source.service)
        val copy =
            share(
                subject = "Budget.xlsx",
                attachments = listOf(Attachment("a", "/p/a.xlsx", "application/vnd.ms-excel", 10)),
                from = "com.google.android.apps.docs",
            )
        assertEquals(CaptureKind.FILE, copy.kind)
        assertNull(copy.title)
    }

    @Test
    fun `Keep checklists become lists`() {
        assertEquals(CaptureKind.LIST, share("☐ Milk\n☐ Eggs").kind)
        assertEquals(CaptureKind.TEXT, share("Just one thought").kind)
    }

    @Test
    fun `the kind follows the attachments`() {
        val image = Attachment("a", "/m/a.jpg", "image/jpeg", 1)
        val pdf = Attachment("b", "/m/b.pdf", "application/pdf", 1)
        assertEquals(CaptureKind.IMAGE, share(attachments = listOf(image)).kind)
        assertEquals(CaptureKind.FILE, share(attachments = listOf(image, pdf)).kind)
    }

    @Test
    fun `similar earlier shares match by source and content, not by title alone`() {
        val earlier =
            CaptureItem(
                "old",
                CaptureKind.TEXT,
                "Milk",
                "Eggs",
                source = SourceRef("keep"),
                capturedAt = Instant.EPOCH,
            )
        val input = share("Milk\nEggs")
        assertEquals("old", ShareInterpreter.findSimilar(listOf(earlier), input)?.id)
        assertNull(
            ShareInterpreter.findSimilar(listOf(earlier.copy(state = CaptureState.TRASHED)), input)
        )
        assertNull(
            ShareInterpreter.findSimilar(listOf(earlier.copy(source = SourceRef("gmail"))), input)
        )
        assertNull(ShareInterpreter.findSimilar(listOf(earlier), share("Milk")))
    }
}
