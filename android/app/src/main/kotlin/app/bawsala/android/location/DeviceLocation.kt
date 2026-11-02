package app.bawsala.android.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import app.bawsala.android.domain.PlaceFix
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The phone's approximate location, read only while the app is open and only with permission
 * (coarse, city-level). Nothing runs in the background.
 */
object DeviceLocation {
    private const val FIX_TIMEOUT_MS = 10_000L

    fun granted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** A fresh fix (or the last known one), named in [lang]; null when unavailable. */
    suspend fun current(context: Context, lang: String): PlaceFix? {
        if (!granted(context)) return null
        val location = locate(context) ?: return null
        val (label, country) = withContext(Dispatchers.IO) { name(context, location, lang) }
        return PlaceFix(location.latitude, location.longitude, label, country)
    }

    @SuppressLint("MissingPermission") // checked in current()
    private suspend fun locate(context: Context): Location? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers =
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).filter {
                runCatching { manager.isProviderEnabled(it) }.getOrDefault(false)
            }
        val last =
            (providers + LocationManager.PASSIVE_PROVIDER)
                .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
        val provider = providers.firstOrNull() ?: return last
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return last
        val fresh =
            withTimeoutOrNull(FIX_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    manager.getCurrentLocation(provider, signal, context.mainExecutor) {
                        if (cont.isActive) cont.resume(it)
                    }
                }
            }
        return fresh ?: last
    }

    /** The city and country code, from Android's on-device geocoder when it has one. */
    @Suppress("DEPRECATION") // the blocking call works on every supported version; run off main
    private fun name(context: Context, location: Location, lang: String): Pair<String?, String?> {
        if (!Geocoder.isPresent()) return null to null
        val address =
            runCatching {
                Geocoder(context, Locale.forLanguageTag(lang))
                    .getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
            }
                .getOrNull() ?: return null to null
        val city = address.locality ?: address.subAdminArea ?: address.adminArea
        return city to address.countryCode
    }
}
