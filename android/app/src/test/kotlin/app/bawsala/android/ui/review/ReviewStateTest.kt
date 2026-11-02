package app.bawsala.android.ui.review

import app.bawsala.android.data.Snapshot
import app.bawsala.core.focus.EntryKind
import app.bawsala.core.focus.TimeEntry
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.Project
import app.bawsala.core.model.Task
import app.bawsala.core.model.TaskStatus
import app.bawsala.core.model.WeeklyReview
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewStateTest {
    private val utc = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 2) // a Monday in ISO week 45
    private val saturday = LocalDate.of(2026, 10, 31)

    private fun day(d: LocalDate, hour: Int = 10) = d.atTime(hour, 0).toInstant(utc)

    private fun done(id: String, at: Instant, project: String? = null) =
        Task(
            id,
            id,
            listId = "work",
            status = TaskStatus.COMPLETED,
            completedAt = at,
            createdAt = Instant.EPOCH,
            projectId = project,
        )

    @Test
    fun `the week starts on Saturday`() {
        assertEquals(saturday, reviewWeekStart(today))
        assertEquals(saturday, reviewWeekStart(saturday))
        assertEquals(saturday, reviewWeekStart(LocalDate.of(2026, 11, 6)))
    }

    @Test
    fun `counts this week's actions, projects advanced and focus sessions`() {
        val s =
            Snapshot(
                tasks =
                    listOf(
                        done("a", day(saturday), "p1"),
                        done("b", day(today), "p1"),
                        done("c", day(today), "p2"),
                        done("old", day(saturday.minusDays(1))),
                        Task(
                            "open",
                            "Open",
                            listId = "work",
                            projectId = "p1",
                            createdAt = Instant.EPOCH,
                        ),
                    ),
                projects =
                    listOf(
                        Project("p1", "One", createdAt = Instant.EPOCH),
                        Project("p3", "Empty", createdAt = Instant.EPOCH),
                    ),
                timeLog =
                    listOf(
                        TimeEntry(
                            "a",
                            EntryKind.WORK,
                            day(today, 9),
                            day(today, 9).plusSeconds(1500),
                        ),
                        TimeEntry(
                            "a",
                            EntryKind.WORK,
                            day(today, 11),
                            day(today, 11).plusSeconds(60),
                        ),
                        TimeEntry("a", EntryKind.BREAK, day(today, 12), day(today, 13)),
                    ),
                items =
                    listOf(
                        CaptureItem("i", CaptureKind.TEXT, "Idea", capturedAt = Instant.EPOCH),
                        CaptureItem(
                            "w",
                            CaptureKind.TEXT,
                            "Wait",
                            state = CaptureState.WAITING,
                            capturedAt = Instant.EPOCH,
                        ),
                    ),
                reviews = listOf(WeeklyReview(saturday, stepsDone = 2)),
            )
        val state = buildReviewState(s, today, utc)
        assertEquals(45, state.weekNumber)
        assertEquals(3, state.actionsDone)
        assertEquals(2, state.projectsAdvanced)
        assertEquals(1, state.focusSessions)
        assertEquals(1, state.inboxCount)
        assertEquals(2, state.activeProjects)
        assertEquals(listOf("Empty"), state.projectsWithoutNext)
        assertEquals(1, state.waitingCount)
        assertEquals(2, state.review.stepsDone)
    }
}
