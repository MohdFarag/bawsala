package app.bawsala.android.ui.search

import app.bawsala.android.data.Snapshot
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.Project
import app.bawsala.core.model.ProjectStatus
import app.bawsala.core.model.Task
import app.bawsala.core.model.TaskStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchResultsTest {
    private val at = Instant.EPOCH

    private val s =
        Snapshot(
            tasks =
                listOf(
                    Task("t1", "Call the dentist", listId = "work", createdAt = at),
                    Task("t2", "Read", notes = "Dentist article", listId = "work", createdAt = at),
                    Task(
                        "t3",
                        "Done",
                        listId = "work",
                        status = TaskStatus.COMPLETED,
                        createdAt = at,
                    ),
                    Task("sub", "Step", listId = "work", parentId = "t1", createdAt = at),
                ),
            projects =
                listOf(
                    Project("p1", "Dentist visits", createdAt = at),
                    Project("p2", "Old dentist", status = ProjectStatus.ARCHIVED, createdAt = at),
                ),
            items =
                listOf(
                    CaptureItem("i1", CaptureKind.TEXT, "Find a DENTIST", capturedAt = at),
                    CaptureItem(
                        "i2",
                        CaptureKind.TEXT,
                        "dentist",
                        state = CaptureState.TRASHED,
                        capturedAt = at,
                    ),
                ),
        )

    @Test
    fun `matches tasks, live projects and kept ideas, ignoring case`() {
        val r = search(s, "  Dentist ")
        assertEquals(listOf("t1", "t2"), r.tasks.map { it.id })
        assertEquals(listOf("p1"), r.projects.map { it.id })
        assertEquals(listOf("i1"), r.items.map { it.id })
    }

    @Test
    fun `an empty query lists open top-level tasks only`() {
        val r = search(s, "")
        assertEquals(listOf("t1", "t2"), r.tasks.map { it.id })
        assertTrue(r.projects.isEmpty() && r.items.isEmpty())
        assertTrue(search(s, "zzz").isEmpty)
    }
}
