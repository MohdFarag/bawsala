package app.bawsala.android.ui.triage

import app.bawsala.core.model.Attachment
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.ListEntry
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TriageStateTest {
    private fun item(
        id: String,
        minute: Long,
        state: CaptureState = CaptureState.INBOX,
        kind: CaptureKind = CaptureKind.TEXT,
    ) =
        CaptureItem(
            id,
            kind,
            "Idea $id",
            state = state,
            capturedAt = Instant.EPOCH.plusSeconds(minute * 60),
        )

    private val items =
        listOf(
            item("b", 2),
            item("a", 1),
            item("c", 3),
            item("ref", 0, CaptureState.REFERENCE),
        )

    @Test
    fun `shows the oldest waiting idea and counts only waiting ones`() {
        val s = buildTriageState(items, focusId = null, sorted = 0)
        assertEquals("a", s.item?.id)
        assertEquals(1, s.position)
        assertEquals(3, s.remaining)
        assertEquals(0f, s.progress)
    }

    @Test
    fun `keeps the focused idea while it waits, and falls back when it was sorted`() {
        assertEquals(2, buildTriageState(items, "b", 1).position)
        assertEquals("a", buildTriageState(items, "ref", 1).item?.id)
    }

    @Test
    fun `progress grows with sorted ideas and is full when nothing is left`() {
        assertEquals(0.25f, buildTriageState(items, null, sorted = 1).progress)
        val done = buildTriageState(emptyList(), null, sorted = 0)
        assertNull(done.item)
        assertEquals(0, done.remaining)
        assertEquals(1f, done.progress)
    }

    @Test
    fun `voice ideas and lists still being sorted entry by entry`() {
        assertTrue(item("v", 0, kind = CaptureKind.AUDIO).isVoice())
        val withRecording =
            item("t", 0).copy(attachments = listOf(Attachment("x", "/p", "audio/mp4", 1)))
        assertTrue(withRecording.isVoice())
        assertFalse(item("t", 0).isVoice())

        val list =
            item("l", 0, kind = CaptureKind.LIST)
                .copy(entries = listOf(ListEntry("e0", "Milk"), ListEntry("e1", "Eggs", "task:t")))
        assertFalse(list.sortsWhole())
        assertTrue(list.copy(entries = list.entries.map { it.copy(resolvedAs = "x") }).sortsWhole())
        assertTrue(item("t", 0).sortsWhole())
    }
}
