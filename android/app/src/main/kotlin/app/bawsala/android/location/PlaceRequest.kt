package app.bawsala.android.location

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.bawsala.android.domain.PlaceFix
import app.bawsala.android.ui.i18n.LocalLang
import kotlinx.coroutines.launch

/**
 * "Use my location": asks for approximate location if needed, then reads it once. [onResult] gets
 * the fix, or null when permission was refused or no location is available.
 */
@Composable
fun rememberPlaceRequest(onResult: (PlaceFix?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()
    val read = { scope.launch { onResult(DeviceLocation.current(context, lang)) } }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) read() else onResult(null)
        }
    return {
        if (DeviceLocation.granted(context)) read()
        else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
