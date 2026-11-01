package app.bawsala.android.ui.plan

import app.bawsala.android.data.Snapshot
import app.bawsala.android.domain.DayPlanner
import app.bawsala.core.model.PrayerName
import app.bawsala.core.model.PrayerPoint
import app.bawsala.core.model.Relation
import app.bawsala.core.model.RelativeRule
import app.bawsala.core.model.Task
import app.bawsala.core.model.TaskSession
import app.bawsala.core.schedule.CheckResult
import app.bawsala.core.schedule.Planner
import app.bawsala.core.schedule.RelativeScheduler
import app.bawsala.core.time.Interval
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** A session at a clock time, or relative to a prayer (REL01). */
enum class ScheduleMode {
    FIXED,
    RELATIVE,
}

private val DURATIONS = listOf(10, 15, 20, 30, 45, 60, 90, 120)
private val OFFSETS = listOf(0, 5, 10, 15, 20, 30, 45, 60)
private const val DEFAULT_MINUTES = 30

/** Where suggestions for a free slot are looked for. */
private val SUGGEST_FROM: LocalTime = LocalTime.of(7, 0)
private val SUGGEST_UNTIL: LocalTime = LocalTime.of(22, 0)

/** The choices in the scheduling sheet. Immutable; every edit produces a new draft. */
data class ScheduleDraft(
    val mode: ScheduleMode,
    val date: LocalDate,
    val start: LocalTime,
    val minutes: Int,
    val prayer: PrayerName = PrayerName.ASR,
    val anchor: PrayerPoint = PrayerPoint.END,
    val relation: Relation = Relation.STARTS_AFTER,
    val offsetMinutes: Int = 10,
    val repeatDaily: Boolean = true,
) {
    fun nextDuration() = copy(minutes = next(DURATIONS, minutes))

    fun nextOffset() = copy(offsetMinutes = next(OFFSETS, offsetMinutes))

    /** The prayer-relative rule this draft describes; a new id is assigned when it is saved. */
    fun rule(id: String = "draft") =
        RelativeRule(
            id,
            prayer,
            anchor,
            relation,
            offsetMinutes,
            minutes,
            from = date,
            until = if (repeatDaily) null else date,
        )

    companion object {
        /**
         * Starts from the session being edited, else a proposed start (a task dropped on the
         * timeline), else the next quarter hour today.
         */
        fun initial(
            task: Task,
            editing: TaskSession?,
            snapshot: Snapshot,
            today: LocalDate,
            now: Instant,
            proposed: LocalDateTime? = null,
        ): ScheduleDraft {
            val zone = snapshot.prayer.zone
            val clock = now.atZone(zone).toLocalTime()
            val nextQuarter = LocalTime.of(clock.hour, 0).plusMinutes((clock.minute / 15 + 1) * 15L)
            return ScheduleDraft(
                mode = ScheduleMode.FIXED,
                date =
                    editing?.start?.atZone(zone)?.toLocalDate() ?: proposed?.toLocalDate() ?: today,
                start =
                    editing?.start?.atZone(zone)?.toLocalTime()
                        ?: proposed?.toLocalTime()
                        ?: nextQuarter,
                minutes =
                    editing?.let { Duration.between(it.start, it.end).toMinutes().toInt() }
                        ?: task.estimateMin
                        ?: DEFAULT_MINUTES,
            )
        }

        private fun next(options: List<Int>, current: Int) =
            options[(options.indexOf(current) + 1) % options.size]
    }
}

/**
 * The time a draft resolves to, whether it fits, and free alternatives when it does not (PLN02).
 */
data class ScheduleEvaluation(
    val slot: Interval?,
    val check: CheckResult?,
    val suggestions: List<Interval>,
)

fun evaluateSchedule(
    draft: ScheduleDraft,
    task: Task,
    editing: TaskSession?,
    snapshot: Snapshot,
    dayPlanner: DayPlanner,
    now: Instant,
): ScheduleEvaluation {
    val zone = snapshot.prayer.zone
    val slot =
        when (draft.mode) {
            ScheduleMode.FIXED -> {
                val start = draft.date.atTime(draft.start).atZone(zone).toInstant()
                Interval(start, start.plus(Duration.ofMinutes(draft.minutes.toLong())))
            }
            ScheduleMode.RELATIVE ->
                dayPlanner
                    .anchors(snapshot, draft.date)
                    .takeIf { it.isNotEmpty() }
                    ?.let { RelativeScheduler.resolve(draft.rule(), it) }
        }
    val day = dayPlanner.dayContext(snapshot, draft.date)
    val check = slot?.let { Planner.check(day, it, task.listId, ignoreSessionId = editing?.id) }
    val suggestions =
        if (check != null && !check.ok && draft.mode == ScheduleMode.FIXED) {
            val from = maxOf(draft.date.atTime(SUGGEST_FROM).atZone(zone).toInstant(), now)
            val until = draft.date.atTime(SUGGEST_UNTIL).atZone(zone).toInstant()
            if (from < until)
                Planner.suggest(
                    day,
                    Interval(from, until),
                    Duration.ofMinutes(draft.minutes.toLong()),
                    task.listId,
                )
            else emptyList()
        } else {
            emptyList()
        }
    return ScheduleEvaluation(slot, check, suggestions)
}
