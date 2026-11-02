package app.bawsala.android.widget

import app.bawsala.android.ui.today.NextStop
import app.bawsala.android.ui.today.TodayState
import app.bawsala.core.model.CalendarEvent
import app.bawsala.core.model.PrayerName
import app.bawsala.core.model.Task
import app.bawsala.core.prayer.PrayerAnchor
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WidgetModelTest {
    private val at = Instant.parse("2026-11-02T15:00:00Z")
    private val base =
        TodayState(
            tasksDone = 0,
            tasksTotal = 0,
            upNext = null,
            rows = emptyList(),
            followUpsDue = 0,
            freeTime = Duration.ZERO,
        )

    @Test
    fun `a prayer is named in the user's language with its time`() {
        val anchor =
            PrayerAnchor(
                PrayerName.MAGHRIB,
                LocalDate.of(2026, 11, 2),
                at,
                at.plusSeconds(600),
                at.plusSeconds(1800),
            )
        val model =
            widgetModel(
                base.copy(
                    nextStop = NextStop.Prayer(anchor),
                    priority = Task("t", "Write", listId = "w", createdAt = at),
                    inboxCount = 3,
                ),
                "ar",
                ZoneOffset.UTC,
            )
        assertEquals("المحطة القادمة · ١٥:٠٠", model.stopLabel)
        assertEquals("صلاة المغرب", model.stopTitle)
        assertEquals("Write", model.priority)
        assertEquals(3, model.inbox)
    }

    @Test
    fun `an appointment keeps its title, and nothing next leaves the stop empty`() {
        val event = CalendarEvent("e", "Team meeting", at, at.plusSeconds(1800), "local")
        assertEquals(
            "Team meeting",
            widgetModel(base.copy(nextStop = NextStop.Event(event)), "en", ZoneOffset.UTC).stopTitle,
        )
        val empty = widgetModel(base, "en", ZoneOffset.UTC)
        assertEquals("Next stop", empty.stopLabel)
        assertNull(empty.stopTitle)
        assertNull(empty.priority)
    }
}
