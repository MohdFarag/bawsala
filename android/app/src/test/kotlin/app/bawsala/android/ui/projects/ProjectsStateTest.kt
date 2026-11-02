package app.bawsala.android.ui.projects

import app.bawsala.android.data.Snapshot
import app.bawsala.core.model.Area
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.Project
import app.bawsala.core.model.ProjectStatus
import app.bawsala.core.model.Task
import app.bawsala.core.model.TaskStatus
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProjectsStateTest {
    private fun at(day: Long) = Instant.EPOCH.plusSeconds(day * 86_400)

    private fun task(
        id: String,
        project: String?,
        done: Boolean = false,
        deadline: LocalDate? = null,
    ) =
        Task(
            id,
            "Task $id",
            listId = "work",
            projectId = project,
            status = if (done) TaskStatus.COMPLETED else TaskStatus.NEEDS_ACTION,
            deadline = deadline,
            createdAt = at(1),
            completedAt = if (done) at(2) else null,
        )

    private val snapshot =
        Snapshot(
            projects =
                listOf(
                    Project("old", "Old", areaId = "health", createdAt = at(1)),
                    Project("new", "New", createdAt = at(5)),
                    Project("paused", "Paused", status = ProjectStatus.ON_HOLD, createdAt = at(9)),
                    Project(
                        "done",
                        "Done",
                        status = ProjectStatus.COMPLETED,
                        createdAt = at(1),
                        completedAt = at(3),
                    ),
                    Project("shelf", "Shelf", status = ProjectStatus.ARCHIVED, createdAt = at(4)),
                ),
            areas =
                listOf(
                    Area("health", "Health", createdAt = at(1)),
                    Area("gone", "Gone", createdAt = at(1), archived = true),
                ),
            tasks =
                listOf(
                    task("a", "old"),
                    task("b", "old", deadline = LocalDate.of(1970, 1, 3)),
                    task("c", "old", done = true),
                    task("loose", null),
                ),
            items =
                listOf(
                    CaptureItem(
                        "r1",
                        CaptureKind.TEXT,
                        "Ref 1",
                        state = CaptureState.REFERENCE,
                        capturedAt = at(1),
                    ),
                    CaptureItem(
                        "r2",
                        CaptureKind.LINK,
                        "Ref 2",
                        state = CaptureState.REFERENCE,
                        capturedAt = at(2),
                    ),
                    CaptureItem("i", CaptureKind.TEXT, "Idea", capturedAt = at(3)),
                ),
        )

    @Test
    fun `active projects come first, newest first, then those on hold`() {
        assertEquals(
            listOf("new", "old", "paused"),
            buildProjectsState(snapshot).active.map { it.project.id },
        )
    }

    @Test
    fun `progress counts the project's actions and the next is the most urgent open one`() {
        val old = buildProjectsState(snapshot).active.first { it.project.id == "old" }
        assertEquals("Health", old.areaTitle)
        assertEquals(1, old.done)
        assertEquals(3, old.total)
        assertEquals(1f / 3, old.progress)
        assertEquals("b", old.next?.id)
        assertEquals(listOf("b", "a", "c"), old.tasks.map { it.id })

        val empty = buildProjectsState(snapshot).active.first { it.project.id == "new" }
        assertEquals(0f, empty.progress)
        assertNull(empty.next)
    }

    @Test
    fun `areas list their active projects; resources and archive have their own tabs`() {
        val state = buildProjectsState(snapshot)
        assertEquals(listOf("health"), state.areas.map { it.area.id })
        assertEquals(listOf("old"), state.areas.single().projects.map { it.project.id })
        assertEquals(2, state.areas.single().openActions)
        assertEquals(listOf("r2", "r1"), state.resources.map { it.id })
        assertEquals(listOf("shelf", "done"), state.archive.map { it.project.id })
    }
}
