package app.bawsala.android.ui.review

import app.bawsala.android.data.Snapshot
import app.bawsala.android.ui.projects.buildProjectsState
import app.bawsala.core.focus.EntryKind
import app.bawsala.core.inbox.Triage
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.ProjectStatus
import app.bawsala.core.model.TaskStatus
import app.bawsala.core.model.WeeklyReview
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

/** Focus intervals shorter than this are not counted as a session. */
private val MIN_SESSION: Duration = Duration.ofMinutes(5)

/** The review week starts on Saturday, as the design's week does (السبت … الجمعة). */
fun reviewWeekStart(today: LocalDate): LocalDate =
    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))

/** Everything the Review screen (M06/T08) and its three steps show. */
data class ReviewState(
    val weekNumber: Int,
    val review: WeeklyReview,
    val actionsDone: Int,
    val projectsAdvanced: Int,
    val focusSessions: Int,
    val inboxCount: Int,
    val activeProjects: Int,
    /** Active projects with no open action. */
    val projectsWithoutNext: List<String>,
    val waitingCount: Int,
)

fun buildReviewState(s: Snapshot, today: LocalDate, zone: ZoneId): ReviewState {
    val start = reviewWeekStart(today)
    val from = start.atStartOfDay(zone).toInstant()
    val to = start.plusDays(7).atStartOfDay(zone).toInstant()
    fun inWeek(at: java.time.Instant?) = at != null && !at.isBefore(from) && at.isBefore(to)

    val done = s.tasks.filter { it.status == TaskStatus.COMPLETED && inWeek(it.completedAt) }
    val projects = buildProjectsState(s).active.filter { it.project.status == ProjectStatus.ACTIVE }
    return ReviewState(
        weekNumber = today.get(WeekFields.ISO.weekOfWeekBasedYear()),
        review = s.reviews.firstOrNull { it.weekStart == start } ?: WeeklyReview(start),
        actionsDone = done.size,
        projectsAdvanced = done.mapNotNull { it.projectId }.distinct().size,
        focusSessions =
            s.timeLog.count {
                it.kind == EntryKind.WORK && inWeek(it.start) && it.duration >= MIN_SESSION
            },
        inboxCount = Triage.queue(s.items).size,
        activeProjects = projects.size,
        projectsWithoutNext = projects.filter { it.next == null }.map { it.project.title },
        waitingCount = s.items.count { it.state == CaptureState.WAITING },
    )
}
