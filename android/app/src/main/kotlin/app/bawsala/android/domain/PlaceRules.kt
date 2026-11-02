package app.bawsala.android.domain

import app.bawsala.core.prayer.PrayerSettings
import com.batoulapps.adhan2.CalculationMethod
import java.time.ZoneId
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Where the phone is, as Android reported it (city-level accuracy is enough for prayer times). */
data class PlaceFix(
    val latitude: Double,
    val longitude: Double,
    /** The city, when the phone could name it. */
    val label: String?,
    /** ISO 3166 code, e.g. "EG", when known. */
    val countryCode: String?,
)

/**
 * Rules for setting prayer times from the phone's location (P1: location-based prayer settings).
 */
object PlaceRules {
    /** Farther than this from the saved place, Today offers to update prayer times. */
    const val FAR_KM = 50.0

    private const val EARTH_RADIUS_KM = 6371.0

    /** Great-circle distance in kilometres. */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    fun isFar(settings: PrayerSettings, fix: PlaceFix): Boolean =
        distanceKm(settings.latitude, settings.longitude, fix.latitude, fix.longitude) > FAR_KM

    /**
     * The calculation method commonly used in a country, to suggest (never to apply on its own).
     * Null when there is no clear national convention.
     */
    fun usualMethod(countryCode: String?): CalculationMethod? =
        when (countryCode?.uppercase()) {
            "EG",
            "SD",
            "LY",
            "SY",
            "LB",
            "IQ",
            "JO",
            "PS",
            "MY" -> CalculationMethod.EGYPTIAN
            "SA",
            "YE" -> CalculationMethod.UMM_AL_QURA
            "AE" -> CalculationMethod.DUBAI
            "QA",
            "BH",
            "OM" -> CalculationMethod.QATAR
            "KW" -> CalculationMethod.KUWAIT
            "PK",
            "IN",
            "BD",
            "AF" -> CalculationMethod.KARACHI
            "TR" -> CalculationMethod.TURKEY
            "SG",
            "ID",
            "BN" -> CalculationMethod.SINGAPORE
            "US",
            "CA" -> CalculationMethod.NORTH_AMERICA
            "GB",
            "IE" -> CalculationMethod.MOON_SIGHTING_COMMITTEE
            "MA",
            "DZ",
            "TN",
            "FR",
            "DE",
            "NL",
            "BE",
            "ES",
            "IT" -> CalculationMethod.MUSLIM_WORLD_LEAGUE
            else -> null
        }

    /**
     * Moves prayer times to [fix] in [zone]. Everything the user chose (method, madhab, iqama,
     * period ends, adjustments, Friday) stays; the method is only suggested separately.
     */
    fun withPlace(current: PrayerSettings, fix: PlaceFix, zone: ZoneId): PrayerSettings =
        current.copy(
            latitude = fix.latitude,
            longitude = fix.longitude,
            zone = zone,
            label = fix.label.orEmpty(),
        )
}
