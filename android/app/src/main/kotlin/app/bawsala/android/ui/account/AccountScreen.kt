package app.bawsala.android.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bawsala.android.BuildConfig
import app.bawsala.android.data.Snapshot
import app.bawsala.android.sync.Account
import app.bawsala.android.sync.SyncStatus
import app.bawsala.android.ui.auth.AuthFlow
import app.bawsala.android.ui.auth.AuthStep
import app.bawsala.android.ui.components.AppButton
import app.bawsala.android.ui.components.ButtonStyle
import app.bawsala.android.ui.components.DetailRow
import app.bawsala.android.ui.components.Field
import app.bawsala.android.ui.components.ScreenColumn
import app.bawsala.android.ui.components.SectionLabel
import app.bawsala.android.ui.components.Segmented
import app.bawsala.android.ui.components.TopBar
import app.bawsala.android.ui.components.card
import app.bawsala.android.ui.format.relative
import app.bawsala.android.ui.i18n.LocalLang
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.AppColors
import app.bawsala.android.ui.theme.AppType
import app.bawsala.android.viewmodel.AccountError
import app.bawsala.android.viewmodel.AppViewModel
import java.time.Instant

/** Sign in to sync between devices (UX03), see sync state (SYN01), sign out or delete (SEC03). */
@Composable
fun AccountScreen(vm: AppViewModel, s: Snapshot) {
    val account by vm.account.account.collectAsStateWithLifecycle()
    val status by vm.account.status.collectAsStateWithLifecycle()
    // Signed out, with the app's own server: the V3 sign-in screens (AM01–AM06). The older form
    // below stays for self-hosting, where the server address is typed in.
    val otherServer = BuildConfig.DEFAULT_SERVER_URL.isBlank()
    if (account == null && !otherServer) {
        Column {
            AuthFlow(vm, AuthStep.LOGIN, onBack = { vm.navigator.back() }, onDone = {})
        }
        return
    }
    ScreenColumn {
        TopBar(tr("accountSync"), { vm.navigator.back() })
        val current = account
        if (current == null) SignInForm(vm) else SignedIn(vm, s, current, status)
        vm.account.error?.let { ErrorText(it) }
    }
}

@Composable
private fun SignInForm(vm: AppViewModel) {
    var create by rememberSaveable { mutableStateOf(false) }
    var server by rememberSaveable { mutableStateOf(BuildConfig.DEFAULT_SERVER_URL) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var advanced by rememberSaveable { mutableStateOf(BuildConfig.DEFAULT_SERVER_URL.isBlank()) }
    val requestGoogleCode = rememberGoogleCodeRequest { result ->
        when (result) {
            is GoogleCode.Received -> vm.account.finishGoogleSignIn(server, result.code)
            is GoogleCode.Failed -> vm.account.googleSignInFailed(result.reason)
            GoogleCode.Cancelled -> Unit
        }
    }
    Text(tr("syncIntro"), style = AppType.small)
    AppButton(
        tr("continueWithGoogle"),
        { vm.account.startGoogleSignIn(server, requestGoogleCode) },
        enabled = !vm.account.busy && server.isNotBlank(),
    )
    Text(tr("googleSignInNote"), style = AppType.micro)
    SectionLabel(tr("orUseEmail"))
    Segmented(
        listOf("signIn" to tr("signIn"), "create" to tr("createAccount")),
        if (create) "create" else "signIn",
        { create = it == "create" },
        Modifier.fillMaxWidth(),
    )
    SectionLabel(tr("email"))
    Field(email, "name@example.com") { email = it }
    SectionLabel(tr("password"))
    Field(password, if (create) tr("passwordHint") else "", secret = true) { password = it }
    AppButton(
        if (create) tr("createAccount") else tr("signIn"),
        { vm.account.signIn(server, email, password, create) },
        enabled =
            !vm.account.busy && server.isNotBlank() && email.isNotBlank() && password.isNotEmpty(),
        style = ButtonStyle.SECONDARY,
    )
    Text(tr("dataStaysNote"), style = AppType.micro)
    // The app's own server is built in; another one is only for self-hosting or testing.
    if (advanced) {
        SectionLabel(tr("serverAddress"))
        Field(server, "https://") { server = it }
    } else {
        AppButton(tr("advancedServer"), { advanced = true }, style = ButtonStyle.QUIET)
    }
}

@Composable
private fun SignedIn(vm: AppViewModel, s: Snapshot, account: Account, status: SyncStatus) {
    val lang = LocalLang.current
    Column(
        Modifier.fillMaxWidth().card().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(account.email, style = AppType.label)
        // Only worth showing when it is not the app's own server (self-hosting, testing).
        if (account.serverUrl.trimEnd('/') != BuildConfig.DEFAULT_SERVER_URL.trimEnd('/'))
            Text(account.serverUrl, style = AppType.micro)
    }
    DetailRow(tr("syncState"), syncStateText(status, s.outbox.queued.size, lang))
    AppButton(
        tr("syncNow"),
        { vm.account.syncNow() },
        enabled = !status.running,
        style = ButtonStyle.SECONDARY,
    )

    if (s.outbox.failures.isNotEmpty()) {
        SectionLabel(tr("needsAttention"))
        s.outbox.failures.forEach { entry ->
            Row(
                Modifier.fillMaxWidth().card().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(recordTitle(s, entry.table.name, entry.id), style = AppType.label)
                    Text(rejectionText(entry.error), style = AppType.micro)
                }
                AppButton(
                    tr("discardChange"),
                    { vm.account.discard(entry) },
                    style = ButtonStyle.QUIET,
                )
            }
        }
    }

    GoogleSection(vm)
    AssistantAccess(vm, account)

    SectionLabel(tr("account"))
    AppButton(
        tr("signOut"),
        { vm.account.signOut() },
        enabled = !vm.account.busy,
        style = ButtonStyle.SECONDARY,
    )
    DeleteAccount(vm)
}

@Composable
private fun DeleteAccount(vm: AppViewModel) {
    var open by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    if (!open) {
        AppButton(tr("deleteAccount"), { open = true }, style = ButtonStyle.QUIET)
        return
    }
    SectionLabel(tr("deleteAccount"))
    Text(tr("deleteAccountExplain"), style = AppType.small)
    Field(password, tr("passwordOrEmail"), secret = true) { password = it }
    AppButton(
        tr("deleteAccountConfirm"),
        { vm.account.deleteAccount(password) },
        enabled = password.isNotEmpty() && !vm.account.busy,
    )
    AppButton(tr("cancel"), { open = false }, style = ButtonStyle.QUIET)
}

@Composable
private fun ErrorText(error: AccountError) {
    Text(
        error.detail?.let { tr(error.key) + ": " + it } ?: tr(error.key),
        style = AppType.small.copy(color = AppColors.warningText),
    )
}

@Composable
private fun syncStateText(status: SyncStatus, waiting: Int, lang: String): String =
    when {
        status.running -> tr("syncing")
        status.error != null -> tr(errorKey(status.error))
        waiting > 0 -> tr("waitingToSync", waiting)
        status.lastSyncedAt != null ->
            tr("lastSynced", relative(Instant.ofEpochMilli(status.lastSyncedAt), lang))
        else -> tr("notSyncedYet")
    }

private fun errorKey(error: String) =
    when (error) {
        "offline" -> "syncOffline"
        "signedOut" -> "signedOutError"
        else -> "syncFailed"
    }

@Composable
private fun rejectionText(reason: String?): String =
    if (reason == "deleted") tr("deletedElsewhere") else tr("serverRejected", reason.orEmpty())

/** A readable name for a record in the outbox. */
private fun recordTitle(s: Snapshot, table: String, id: String): String =
    when (table) {
        "items" -> s.items.firstOrNull { it.id == id }?.title
        "tasks" -> s.tasks.firstOrNull { it.id == id }?.title
        "lists" -> s.lists.firstOrNull { it.id == id }?.name
        "projects" -> s.projects.firstOrNull { it.id == id }?.title
        "areas" -> s.areas.firstOrNull { it.id == id }?.title
        "blocks" -> s.blocks.firstOrNull { it.id == id }?.name
        else -> null
    } ?: table
