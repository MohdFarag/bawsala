package app.bawsala.android.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.bawsala.android.data.OutboxEntry
import app.bawsala.android.sync.McpToken
import app.bawsala.android.work.SyncWorker
import kotlinx.coroutines.launch

/** Account and sync actions for the account screen (UX03, SYN01, SEC03). */
class AccountController(private val ctx: ControllerScope) {
    private val accounts = ctx.container.accounts
    private val service = ctx.container.accountService

    val account = accounts.account
    val status = accounts.status

    private val action = ServerAction(ctx.scope)
    val busy: Boolean
        get() = action.busy

    val error: AccountError?
        get() = action.error

    /** Assistant (MCP) tokens of the signed-in account. */
    var assistantTokens by mutableStateOf<List<McpToken>>(emptyList())
        private set

    /** A token just created; its secret is shown once, then forgotten. */
    var newAssistantToken by mutableStateOf<String?>(null)
        private set

    /**
     * Signs in, or creates an account with [name]. A new account works right away; [onCreated] gets
     * the email so the confirmation screen (V3 AM05) can show it.
     */
    fun signIn(
        server: String,
        email: String,
        password: String,
        create: Boolean,
        name: String? = null,
        onCreated: (String) -> Unit = {},
    ) = act {
        if (create && password.length < MIN_PASSWORD) {
            action.fail(AccountError("passwordTooShort"))
            return@act
        }
        service.signIn(server, email, password, create, ctx.snapshot.lang, name)
        SyncWorker.schedulePeriodic(ctx.container.appContext)
        if (create) onCreated(email.trim()) else ctx.messages.show("signedIn")
        ctx.container.syncRunner.run()
    }

    /** "Forgot password" (V3 AM03); [onSent] shows the "check your email" screen (AM04). */
    fun forgotPassword(server: String, email: String, onSent: () -> Unit) = act {
        service.requestPasswordReset(server, email)
        onSent()
    }

    /** Sends the confirmation or reset link again (AM04/AM05). */
    fun resend(server: String, email: String, reset: Boolean) = act {
        if (reset) service.requestPasswordReset(server, email)
        else service.resendVerification(server, email)
        ctx.messages.show("linkResent")
    }

    /**
     * Sign in with Google, step 1: asks the server which web client to request a code for, then
     * [requestCode] opens Google's account picker and consent.
     */
    fun startGoogleSignIn(server: String, requestCode: (webClientId: String) -> Unit) = act {
        val clientId = service.googleWebClientId(server)
        if (clientId == null) action.fail(AccountError("googleNotConfigured"))
        else requestCode(clientId)
    }

    /** Step 2: Google returned a code; the server signs in and connects Tasks and Calendar. */
    fun finishGoogleSignIn(server: String, code: String) = act {
        service.signInWithGoogle(server, code, ctx.snapshot.lang)
        SyncWorker.schedulePeriodic(ctx.container.appContext)
        ctx.messages.show("signedIn")
        ctx.container.syncRunner.run()
    }

    /** Google's screen failed (not when the user simply closed it). */
    fun googleSignInFailed(reason: String) = action.fail(AccountError("googleSignInFailed", reason))

    fun signOut() = act {
        SyncWorker.cancel(ctx.container.appContext)
        service.signOut()
    }

    fun deleteAccount(password: String) = act {
        SyncWorker.cancel(ctx.container.appContext)
        service.deleteAccount(password)
        ctx.messages.show("accountDeleted")
    }

    fun loadAssistantTokens() = act { assistantTokens = service.assistantTokens() }

    fun createAssistantToken(name: String) = act {
        newAssistantToken = service.createAssistantToken(name.trim()).second
        assistantTokens = service.assistantTokens()
    }

    fun revokeAssistantToken(id: String) = act {
        service.revokeAssistantToken(id)
        assistantTokens = service.assistantTokens()
    }

    fun dismissNewToken() {
        newAssistantToken = null
    }

    fun syncNow() = ctx.scope.launch { ctx.container.syncRunner.run() }

    /** Drops a change the server rejected (see [app.bawsala.android.data.Repository.discard]). */
    fun discard(entry: OutboxEntry) = ctx.scope.launch { ctx.repository.discard(entry) }

    fun clearError() = action.clearError()

    private fun act(block: suspend () -> Unit) = action.run(block)

    companion object {
        const val MIN_PASSWORD = 10
    }
}
