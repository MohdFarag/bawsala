package app.bawsala.android.ui.today

import app.bawsala.android.data.Snapshot
import app.bawsala.android.domain.DayPlanner
import app.bawsala.core.inbox.Triage
import app.bawsala.core.model.CalendarEvent
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.SessionState
import app.bawsala.core.model.Task
import app.bawsala.core.model.TaskSession
import app.bawsala.core.model.TaskStatus
import app.bawsala.core.prayer.PrayerAnchor
import app.bawsala.core.schedule.Planner
import app.bawsala.core.time.Interval
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** The hours used for "time left for unplanned work". */
private val WORKDAY_START: LocalTime = LocalTime.of(9, 0)
private val WORKDAY_END: LocalTime = LocalTime.of(21, 0)

data class ScheduledTask(val session: TaskSession, val task: Task)

/** One row under "Your day": a prayer or a planned session, in time order. */
sealed interface DayRow {
    val at: Instant

    data class Prayer(val anchor: PrayerAnchor) : DayRow {
        override val at: Instant
            get() = anchor.adhan
    }

    data class Session(val item: ScheduledTask) : DayRow {
        override val at: Instant
            get() = item.session.start
    }
}

/** "المحطة القادمة": the next appointment, planned session or prayer today. */
sealed interface NextStop {
    val start: Instant

    data class Event(val event: CalendarEvent) : NextStop {
        override val start: Instant
            get() = event.start
    }

    data class Session(val item: ScheduledTask) : NextStop {
        override val start: Instant
            get() = item.session.start
    }

    data class Prayer(val anchor: PrayerAnchor) : NextStop {
        override val start: Instant
            get() = anchor.adhan
    }
}

data class TodayState(
    val tasksDone: Int,
    val tasksTotal: Int,
    val upNext: ScheduledTask?,
    val rows: List<DayRow>,
    val followUpsDue: Int,
    val freeTime: Duration,
    /**
     * The priority card's task (V3): the session in progress or next, else [priorityOrder] first.
     */
    val priority: Task? = null,
    /** Up to three more open tasks, in [priorityOrder]. */
    val nextActions: List<Task> = emptyList(),
    val nextStop: NextStop? = null,
    /** Whether the priority task's estimate fits before the next stop. */
    val priorityFits: Boolean = false,
    /** Unprocessed inbox items, for the tablet's "calmer mind" card. */
    val inboxCount: Int = 0,
)

/**
 * The order tasks are suggested in, without any AI: important first, then the nearest deadline,
 * then the nearest own target date, then the oldest.
 */
val priorityOrder: Comparator<Task> =
    compareByDescending<Task> { it.important }
        .thenBy(nullsLast()) { it.deadline }
        .thenBy(nullsLast()) { it.targetDate }
        .thenBy { it.createdAt }

/** Everything the Today screen shows, computed from stored data — no UI code here. */
fun buildTodayState(
    snapshot: Snapshot,
    dayPlanner: DayPlanner,
    today: LocalDate,
    now: Instant,
): TodayState {
    val zone = snapshot.prayer.zone
    val dayStart = today.atStartOfDay(zone).toInstant()
    val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant()
    val tasksById = snapshot.tasks.associateBy { it.id }

    val scheduled =
        snapshot.sessions
            .filter {
                it.state != SessionState.CANCELED && it.start >= dayStart && it.start < dayEnd
            }
            .mapNotNull { session -> tasksById[session.taskId]?.let { ScheduledTask(session, it) } }
            .sortedBy { it.session.start }

    // Each task counts once, even with several sessions (AC07).
    val completedToday =
        snapshot.tasks.filter { it.completedAt?.atZone(zone)?.toLocalDate() == today }
    val todaysTasks = (scheduled.map { it.task } + completedToday).associateBy { it.id }.values
    val upNext = scheduled.firstOrNull {
        it.task.status != TaskStatus.COMPLETED && it.session.end.isAfter(now)
    }

    val open =
        snapshot.tasks
            .filter { it.status != TaskStatus.COMPLETED && it.parentId == null }
            .sortedWith(priorityOrder)
    val priority = upNext?.task ?: open.firstOrNull()
    val nextStop = nextStop(snapshot, dayPlanner, today, now, scheduled)
    val untilNextStop = nextStop?.let { Duration.between(now, it.start) }

    return TodayState(
        priority = priority,
        nextActions = open.filter { it.id != priority?.id }.take(3),
        nextStop = nextStop,
        priorityFits =
            priority?.estimateMin != null &&
                untilNextStop != null &&
                untilNextStop >= Duration.ofMinutes(priority.estimateMin!!.toLong()),
        inboxCount = snapshot.items.count { it.state == CaptureState.INBOX },
        tasksDone = todaysTasks.count { it.status == TaskStatus.COMPLETED },
        tasksTotal = todaysTasks.size,
        upNext = upNext,
        rows =
            dayRows(
                scheduled.filter { it != upNext },
                dayPlanner.anchors(snapshot, today),
                scheduled,
                now,
            ),
        followUpsDue = Triage.followUpsDue(snapshot.items, today).size,
        freeTime = freeTime(snapshot, dayPlanner, today, now),
    )
}

/**
 * Sessions plus the prayers that fall before the day's last session (or at least the next prayer).
 */
private fun dayRows(
    sessions: List<ScheduledTask>,
    anchors: List<PrayerAnchor>,
    allScheduled: List<ScheduledTask>,
    now: Instant,
): List<DayRow> {
    val lastEnd = allScheduled.maxOfOrNull { it.session.end } ?: now
    val upcoming = anchors.filter { it.end.isAfter(now) }
    val prayers = upcoming.filter { it.adhan.isBefore(lastEnd) }.ifEmpty { upcoming.take(1) }
    return (prayers.map(DayRow::Prayer) + sessions.map(DayRow::Session)).sortedBy { it.at }
}

/** The earliest event, session or prayer still to start today. */
private fun nextStop(
    snapshot: Snapshot,
    dayPlanner: DayPlanner,
    today: LocalDate,
    now: Instant,
    scheduled: List<ScheduledTask>,
): NextStop? {
    val dayEnd = today.plusDays(1).atStartOfDay(snapshot.prayer.zone).toInstant()
    val candidates =
        snapshot.events
            .filter { !it.allDay && it.start.isAfter(now) && it.start.isBefore(dayEnd) }
            .map(NextStop::Event) +
            scheduled
                .filter {
                    it.task.status != TaskStatus.COMPLETED && it.session.start.isAfter(now)
                }
                .map(NextStop::Session) +
            dayPlanner
                .anchors(snapshot, today)
                .filter { it.adhan.isAfter(now) }
                .map(NextStop::Prayer)
    return candidates.minByOrNull { it.start }
}

private fun freeTime(
    snapshot: Snapshot,
    dayPlanner: DayPlanner,
    today: LocalDate,
    now: Instant,
): Duration {
    val zone = snapshot.prayer.zone
    val end = today.atTime(WORKDAY_END).atZone(zone).toInstant()
    if (!now.isBefore(end)) return Duration.ZERO
    val start = maxOf(now, today.atTime(WORKDAY_START).atZone(zone).toInstant())
    return Planner.freeTime(dayPlanner.dayContext(snapshot, today), Interval(start, end))
}
