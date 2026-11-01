package app.bawsala.android.ui.account

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/** Identity, Google Tasks and Calendar: one consent screen signs in and connects (§6). */
private val SCOPES =
    listOf(
        "openid",
        "email",
        "profile",
        "https://www.googleapis.com/auth/tasks",
        "https://www.googleapis.com/auth/calendar",
    )

/** How a Google sign-in attempt ended on the device, before the server is involved. */
sealed interface GoogleCode {
    data class Received(val code: String) : GoogleCode

    /** The user closed Google's screen. */
    data object Cancelled : GoogleCode

    data class Failed(val reason: String) : GoogleCode
}

/**
 * Returns a function that asks Google (account picker and consent, in one screen) for a server auth
 * code for [webClientId]; the result arrives in [onResult]. The server exchanges the code, so the
 * app never holds Google tokens.
 */
@Composable
fun rememberGoogleCodeRequest(onResult: (GoogleCode) -> Unit): (webClientId: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val consent =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode != Activity.RESULT_OK)
                return@rememberLauncherForActivityResult onResult(GoogleCode.Cancelled)
            onResult(
                runCatching {
                    Identity.getAuthorizationClient(context)
                        .getAuthorizationResultFromIntent(it.data)
                        .toCode()
                }
                    .getOrElse { e -> GoogleCode.Failed(e.message ?: "authorization") }
            )
        }
    return { webClientId ->
        scope.launch {
            val activity = context.findActivity()
            if (activity == null) {
                onResult(GoogleCode.Failed("no activity"))
                return@launch
            }
            val request =
                AuthorizationRequest.builder()
                    .setRequestedScopes(SCOPES.map(::Scope))
                    // A refresh token on every exchange, so the server can sync in the background.
                    .requestOfflineAccess(webClientId, true)
                    .build()
            try {
                val result = Identity.getAuthorizationClient(activity).authorize(request).await()
                val pending = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    consent.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    onResult(result.toCode())
                }
            } catch (e: ApiException) {
                onResult(GoogleCode.Failed("google_${e.statusCode}"))
            }
        }
    }
}

private fun AuthorizationResult.toCode(): GoogleCode =
    serverAuthCode?.let { GoogleCode.Received(it) } ?: GoogleCode.Failed("no code")

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
