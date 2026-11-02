package app.bawsala.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bawsala.android.BuildConfig
import app.bawsala.android.R
import app.bawsala.android.ui.account.GoogleCode
import app.bawsala.android.ui.account.rememberGoogleCodeRequest
import app.bawsala.android.ui.design.ActionButton
import app.bawsala.android.ui.design.ActionStyle
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel

/** The auth boards, in the order a user can reach them. */
enum class AuthStep {
    LOGIN, // AM01, and AM06 when sign-in fails
    SIGN_UP, // AM02
    FORGOT, // AM03
    RESET_SENT, // AM04
    VERIFY, // AM05
}

/**
 * V3 sign-in and sign-up (AM01–AM06). [onBack] leaves the flow from its first step; [onDone] runs
 * once the user is signed in (after the confirmation screen for a new account), and [onSkip], when
 * given, keeps using the app on this device only.
 */
@Composable
fun AuthFlow(
    vm: AppViewModel,
    start: AuthStep,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onSkip: (() -> Unit)? = null,
) {
    val server = BuildConfig.DEFAULT_SERVER_URL
    var step by rememberSaveable { mutableStateOf(start) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    // A new password account shows the confirmation screen (AM05) before going on.
    var creating by rememberSaveable { mutableStateOf(false) }
    val account by vm.account.account.collectAsStateWithLifecycle()
    LaunchedEffect(account) {
        if (account != null && !creating && (step == AuthStep.LOGIN || step == AuthStep.SIGN_UP))
            onDone()
    }
    val busy = vm.account.busy
    val error = vm.account.error
    fun go(next: AuthStep) {
        vm.account.clearError()
        step = next
    }
    val back = {
        if (step == start) onBack() else go(if (step == AuthStep.SIGN_UP) AuthStep.LOGIN else start)
    }
    val requestGoogleCode = rememberGoogleCodeRequest { result ->
        when (result) {
            is GoogleCode.Received -> vm.account.finishGoogleSignIn(server, result.code)
            is GoogleCode.Failed -> vm.account.googleSignInFailed(result.reason)
            GoogleCode.Cancelled -> Unit
        }
    }
    val google: @Composable () -> Unit = {
        if (server.isNotBlank())
            ActionButton(
                tr("continueWithGoogle"),
                { vm.account.startGoogleSignIn(server, requestGoogleCode) },
                Modifier.fillMaxWidth().padding(top = 12.dp),
                ActionStyle.Secondary,
                enabled = !busy,
            )
    }
    val skip: @Composable () -> Unit = {
        if (onSkip != null) PlainLink(tr("useWithoutAccount"), onClick = onSkip)
    }

    when (step) {
        AuthStep.LOGIN -> {
            // AM06: wrong email or password marks both fields and explains below them.
            val wrong = error?.key == "wrongCredentials"
            AuthPage(tr("welcomeBack"), tr("welcomeBackHint"), back) {
                AuthField(
                    tr("email"),
                    email,
                    { email = it },
                    "you@example.com",
                    R.drawable.ic_lucide_mail,
                    email = true,
                    error = wrong,
                )
                AuthField(
                    tr("password"),
                    password,
                    { password = it },
                    tr("enterPassword"),
                    R.drawable.ic_lucide_lock,
                    Modifier.padding(top = 18.dp),
                    secret = true,
                    error = wrong,
                )
                if (error != null) ErrorNotice(errorText(error.key, error.detail))
                Text(
                    tr("forgotPassword"),
                    style =
                        TypeScale.tiny.copy(
                            fontWeight = FontWeight.Bold,
                            color = Palette.green900,
                            fontSize = 12.sp,
                        ),
                    modifier =
                        Modifier.padding(top = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { go(AuthStep.FORGOT) }
                            .padding(vertical = 12.dp),
                )
                ActionButton(
                    tr(if (wrong) "tryAgain" else "signIn"),
                    { vm.account.signIn(server, email, password, create = false) },
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    icon = R.drawable.ic_lucide_arrow_up_right,
                    enabled = !busy && email.isNotBlank() && password.isNotEmpty(),
                )
                google()
                SwitchLink(tr("firstTimeHere"), tr("createAccountAction")) { go(AuthStep.SIGN_UP) }
                skip()
            }
        }
        AuthStep.SIGN_UP ->
            AuthPage(tr("signUpTitle"), tr("signUpHint"), back) {
                AuthField(
                    tr("name"),
                    name,
                    { name = it },
                    tr("namePlaceholder"),
                    R.drawable.ic_lucide_user,
                )
                AuthField(
                    tr("email"),
                    email,
                    { email = it },
                    "you@example.com",
                    R.drawable.ic_lucide_mail,
                    Modifier.padding(top = 18.dp),
                    email = true,
                    error = error?.key == "emailTaken",
                )
                AuthField(
                    tr("password"),
                    password,
                    { password = it },
                    tr("createPassword"),
                    R.drawable.ic_lucide_lock,
                    Modifier.padding(top = 18.dp),
                    secret = true,
                    error = error?.key == "passwordTooShort",
                )
                Text(
                    tr("passwordRule"),
                    style = TypeScale.tiny.copy(fontSize = 10.sp),
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (error != null) ErrorNotice(errorText(error.key, error.detail))
                ActionButton(
                    tr("createAccountAction"),
                    {
                        creating = true
                        vm.account.signIn(server, email, password, create = true, name = name) {
                            go(AuthStep.VERIFY)
                        }
                    },
                    Modifier.fillMaxWidth().padding(top = 20.dp),
                    icon = R.drawable.ic_lucide_arrow_up_right,
                    enabled = !busy && email.isNotBlank() && password.isNotEmpty(),
                )
                Text(
                    tr("termsNote"),
                    style = TypeScale.tiny.copy(fontSize = 10.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
                google()
                SwitchLink(tr("haveAccount"), tr("signIn")) { go(AuthStep.LOGIN) }
                skip()
            }
        AuthStep.FORGOT ->
            AuthPage(
                tr("forgotTitle"),
                tr("forgotHint"),
                back,
                symbol = R.drawable.ic_lucide_lock,
            ) {
                AuthField(
                    tr("email"),
                    email,
                    { email = it },
                    "you@example.com",
                    R.drawable.ic_lucide_mail,
                    email = true,
                )
                if (error != null) ErrorNotice(errorText(error.key, error.detail))
                ActionButton(
                    tr("sendResetLink"),
                    { vm.account.forgotPassword(server, email) { go(AuthStep.RESET_SENT) } },
                    Modifier.fillMaxWidth().padding(top = 18.dp),
                    icon = R.drawable.ic_lucide_mail,
                    enabled = !busy && email.isNotBlank(),
                )
                Note(tr("forgotNote"))
                PlainLink(tr("backToSignIn"), Modifier.padding(top = 18.dp)) { go(AuthStep.LOGIN) }
            }
        AuthStep.RESET_SENT,
        AuthStep.VERIFY -> {
            val reset = step == AuthStep.RESET_SENT
            AuthPage(
                tr(if (reset) "checkEmailTitle" else "verifyTitle"),
                tr(if (reset) "checkEmailHint" else "verifyHint"),
                back,
                footer = tr("authFooterSaved"),
                symbol = R.drawable.ic_lucide_mail,
            ) {
                Text(
                    email.trim(),
                    style = TypeScale.button,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Palette.sage100)
                            .padding(16.dp),
                )
                Column(
                    Modifier.fillMaxWidth()
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Palette.white)
                        .border(1.dp, Palette.line, RoundedCornerShape(18.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        tr(if (reset) "checkEmailStep" else "verifyStep"),
                        style =
                            TypeScale.tiny.copy(
                                color = Palette.ink,
                                fontSize = 12.sp,
                                fontWeight = if (reset) FontWeight.Normal else FontWeight.Bold,
                            ),
                    )
                    Text(
                        tr(if (reset) "checkSpam" else "verifyAfter"),
                        style = TypeScale.tiny.copy(fontSize = 12.sp),
                    )
                }
                if (error != null) ErrorNotice(errorText(error.key, error.detail))
                ActionButton(
                    tr(if (reset) "backToSignIn" else "continueToApp"),
                    { if (reset) go(AuthStep.LOGIN) else onDone() },
                    Modifier.fillMaxWidth().padding(top = 22.dp),
                    icon = R.drawable.ic_lucide_arrow_up_right,
                )
                PlainLink(tr("resendLink"), Modifier.padding(top = 10.dp)) {
                    vm.account.resend(server, email, reset)
                }
                PlainLink(tr("changeEmail")) {
                    go(if (reset) AuthStep.FORGOT else AuthStep.SIGN_UP)
                }
            }
        }
    }
}

/** The small muted note with a shield under the reset form. */
@Composable
private fun Note(text: String) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        app.bawsala.android.ui.design.LucideIcon(
            R.drawable.ic_lucide_shield_check,
            tint = Palette.muted,
            size = 16.dp,
        )
        Text(text, style = TypeScale.tiny.copy(fontSize = 11.sp))
    }
}

@Composable
private fun errorText(key: String, detail: String?): String =
    when (key) {
        "wrongCredentials" -> tr("wrongCredentialsLong")
        else -> detail?.let { tr(key) + ": " + it } ?: tr(key)
    }
