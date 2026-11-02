package app.bawsala.android.ui.planner

import app.bawsala.android.data.Snapshot
import app.bawsala.android.ui.plan.PlanItem
import app.bawsala.android.ui.plan.unplannedTasks
import app.bawsala.android.ui.today.priorityOrder
import app.bawsala.core.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Hours shown on the week grid: from before Fajr to after Isha. */
const val FIRST_HOUR = 4
const val LAST_HOUR = 24

/** Dropped tasks snap to quarter hours. */
private const val SNAP_MIN = 15

/** The seven days shown, Saturday first as in the design. */
fun weekDays(start: LocalDate): List<LocalDate> = (0L..6L).map(start::plusDays)

/**
 * A plan item placed in its day column: minutes since midnight (clipped to the grid's hours), and a
 * lane when it overlaps others, so overlapping items sit side by side.
 */
data class Placed(
    val item: PlanItem,
    val startMin: Int,
    val endMin: Int,
    val lane: Int,
    val lanes: Int,
)

/** Places [items] of [date] on the grid; items wholly outside the grid's hours are left out. */
fun placeDay(items: List<PlanItem>, date: LocalDate, zone: ZoneId): List<Placed> {
    val midnight = date.atStartOfDay(zone).toInstant()
    fun minutes(at: Instant) = java.time.Duration.between(midnight, at).toMinutes().toInt()
    val first = FIRST_HOUR * 60
    val last = LAST_HOUR * 60
    val spans =
        items
            .map {
                Triple(
                    it,
                    minutes(it.start).coerceAtLeast(first),
                    minutes(it.end).coerceAtMost(last),
                )
            }
            .filter { (_, s, e) -> e > s }
            .sortedWith(compareBy({ it.second }, { -it.third }))

    val placed = mutableListOf<Placed>()
    var cluster = mutableListOf<Pair<Triple<PlanItem, Int, Int>, Int>>()
    var clusterEnd = Int.MIN_VALUE
    fun flush() {
        val lanes = (cluster.maxOfOrNull { it.second } ?: -1) + 1
        cluster.forEach { (span, lane) ->
            placed += Placed(span.first, span.second, span.third, lane, lanes)
        }
        cluster = mutableListOf()
    }
    val laneEnds = mutableListOf<Int>()
    for (span in spans) {
        if (span.second >= clusterEnd) {
            flush()
            laneEnds.clear()
        }
        val lane =
            laneEnds.indexOfFirst { it <= span.second }.let { if (it < 0) laneEnds.size else it }
        if (lane == laneEnds.size) laneEnds += span.third else laneEnds[lane] = span.third
        cluster += span to lane
        clusterEnd = maxOf(clusterEnd, span.third)
    }
    flush()
    return placed
}

/** The minute of the day at [offsetMin] minutes below the grid's top, snapped to a quarter hour. */
fun dropMinute(offsetMin: Float): Int {
    val raw = FIRST_HOUR * 60 + offsetMin
    val snapped = (raw / SNAP_MIN).roundToInt() * SNAP_MIN
    return snapped.coerceIn(FIRST_HOUR * 60, LAST_HOUR * 60 - SNAP_MIN)
}

/** The next-actions panel: open tasks not yet planned, most urgent first, for a [context]. */
data class NextActionsPanel(val tasks: List<Task>, val contexts: List<String>, val ready: Int)

fun nextActionsPanel(s: Snapshot, now: Instant, context: String?): NextActionsPanel {
    val open = unplannedTasks(s, now).filter { it.parentId == null }.sortedWith(priorityOrder)
    return NextActionsPanel(
        tasks = open.filter { context == null || it.context == context },
        contexts = open.mapNotNull { it.context }.distinct().sorted(),
        ready = open.size,
    )
}
