package app.bawsala.android.domain

import app.bawsala.android.data.Codec
import app.bawsala.android.data.Prefs
import app.bawsala.android.data.Snapshot
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The prayer module switch (#74): off removes prayers everywhere they are read. */
class PrayerSwitchTest {
    private val planner = DayPlanner()
    private val day = LocalDate.of(2026, 11, 2)

    @Test
    fun `prayers are there by default and gone when the switch is off`() {
        val on = Snapshot()
        assertEquals(5, planner.anchors(on, day).size)
        assertEquals(5, planner.dayContext(on, day).prayers.size)

        val off = Snapshot(prefs = Prefs(prayerFeatures = false))
        assertTrue(planner.anchors(off, day).isEmpty())
        assertEquals(emptyList(), planner.anchorsOrNull(off, day))
        assertTrue(planner.dayContext(off, day).prayers.isEmpty())
    }

    @Test
    fun `the switch is stored, and older saved preferences read as on`() {
        val off = Prefs(prayerFeatures = false)
        assertFalse(Codec.prefs(Codec.encode(off)).prayerFeatures)
        val older = Codec.encode(Prefs()).apply { remove("prayerFeatures") }
        assertTrue(Codec.prefs(older).prayerFeatures)
    }
}
