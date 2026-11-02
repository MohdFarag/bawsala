package app.bawsala.android.domain

import app.bawsala.core.prayer.PrayerSettings
import com.batoulapps.adhan2.CalculationMethod
import java.time.ZoneId
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaceRulesTest {
    private val cairo =
        PrayerSettings(
            30.0444,
            31.2357,
            ZoneId.of("Africa/Cairo"),
            method = CalculationMethod.EGYPTIAN,
            fridayExtraMin = 45,
            label = "Cairo",
        )

    @Test
    fun `distance between Cairo and Alexandria is about 180 km`() {
        val km = PlaceRules.distanceKm(30.0444, 31.2357, 31.2001, 29.9187)
        assertTrue(abs(km - 180) < 5, "was $km")
        assertEquals(0.0, PlaceRules.distanceKm(1.0, 2.0, 1.0, 2.0))
    }

    @Test
    fun `only a place more than 50 km away counts as far`() {
        assertFalse(PlaceRules.isFar(cairo, PlaceFix(30.06, 31.33, "Heliopolis", "EG")))
        assertTrue(PlaceRules.isFar(cairo, PlaceFix(31.2001, 29.9187, "Alexandria", "EG")))
    }

    @Test
    fun `usual methods by country, and none where there is no clear convention`() {
        assertEquals(CalculationMethod.UMM_AL_QURA, PlaceRules.usualMethod("sa"))
        assertEquals(CalculationMethod.EGYPTIAN, PlaceRules.usualMethod("EG"))
        assertEquals(CalculationMethod.DUBAI, PlaceRules.usualMethod("AE"))
        assertNull(PlaceRules.usualMethod("ZZ"))
        assertNull(PlaceRules.usualMethod(null))
    }

    @Test
    fun `a new place keeps the user's choices and takes the phone's time zone`() {
        val riyadh =
            PlaceRules.withPlace(
                cairo,
                PlaceFix(24.7136, 46.6753, "Riyadh", "SA"),
                ZoneId.of("Asia/Riyadh"),
            )
        assertEquals(24.7136, riyadh.latitude)
        assertEquals(ZoneId.of("Asia/Riyadh"), riyadh.zone)
        assertEquals("Riyadh", riyadh.label)
        assertEquals(CalculationMethod.EGYPTIAN, riyadh.method)
        assertEquals(45, riyadh.fridayExtraMin)
        assertEquals(
            "",
            PlaceRules.withPlace(cairo, PlaceFix(1.0, 2.0, null, null), ZoneId.of("UTC")).label,
        )
    }
}
