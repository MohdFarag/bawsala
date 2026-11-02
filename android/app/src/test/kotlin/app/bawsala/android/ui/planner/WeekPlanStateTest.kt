package app.bawsala.android.ui.planner

import app.bawsala.android.data.Snapshot
import app.bawsala.android.ui.plan.PlanItem
import app.bawsala.android.ui.plan.PlanItemKind
import app.bawsala.core.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class WeekPlanStateTest {
    private val day = LocalDate.of(2026, 11, 2)
    private val utc = ZoneOffset.UTC

    private fun at(h: Int, m: Int = 0) =
        day.atTime(h % 24, m).toInstant(utc).let { if (h == 24) it.plusSeconds(86_400) else it }

    private fun item(key: String, from: Instant, to: Instant) =
        PlanItem(key, PlanItemKind.TASK, from, to, title = key)

    @Test
    fun `the week runs Saturday to Friday`() {
        val days = weekDays(LocalDate.of(2026, 10, 31))
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 11, 6), days.last())
    }

    @Test
    fun `overlapping items share lanes and separate ones get the full width`() {
        val placed =
            placeDay(
                    listOf(
                        item("a", at(9), at(10, 30)),
                        item("b", at(10), at(11)),
                        item("c", at(10, 30), at(12)),
                        item("d", at(13), at(14)),
                    ),
                    day,
                    utc,
                )
                .associateBy { it.item.key }
        assertEquals(0 to 2, placed.getValue("a").let { it.lane to it.lanes })
        assertEquals(1 to 2, placed.getValue("b").let { it.lane to it.lanes })
        // "c" starts when "a" ends, so it reuses lane 0.
        assertEquals(0 to 2, placed.getValue("c").let { it.lane to it.lanes })
        assertEquals(0 to 1, placed.getValue("d").let { it.lane to it.lanes })
        assertEquals(9 * 60, placed.getValue("a").startMin)
    }

    @Test
    fun `items are clipped to the grid's hours and those outside are dropped`() {
        val placed =
            placeDay(listOf(item("early", at(2), at(5)), item("night", at(1), at(3))), day, utc)
        assertEquals(listOf("early"), placed.map { it.item.key })
        assertEquals(FIRST_HOUR * 60, placed.single().startMin)
    }

    @Test
    fun `drops snap to quarter hours inside the grid`() {
        assertEquals(FIRST_HOUR * 60, dropMinute(0f))
        assertEquals(FIRST_HOUR * 60 + 30, dropMinute(37f))
        assertEquals(LAST_HOUR * 60 - 15, dropMinute(10_000f))
        assertEquals(FIRST_HOUR * 60, dropMinute(-50f))
    }

    @Test
    fun `next actions are open, unplanned, top-level and filtered by context`() {
        fun task(
            id: String,
            context: String? = null,
            parent: String? = null,
            important: Boolean = false,
        ) =
            Task(
                id,
                id,
                listId = "work",
                context = context,
                parentId = parent,
                important = important,
                createdAt = Instant.EPOCH,
            )
        val s =
            Snapshot(
                tasks =
                    listOf(
                        task("a", "home"),
                        task("b", "computer", important = true),
                        task("sub", "home", parent = "a"),
                        task("c"),
                    )
            )
        val all = nextActionsPanel(s, Instant.EPOCH, null)
        assertEquals(listOf("b", "a", "c"), all.tasks.map { it.id })
        assertEquals(listOf("computer", "home"), all.contexts)
        assertEquals(3, all.ready)
        assertEquals(listOf("a"), nextActionsPanel(s, Instant.EPOCH, "home").tasks.map { it.id })
    }
}
