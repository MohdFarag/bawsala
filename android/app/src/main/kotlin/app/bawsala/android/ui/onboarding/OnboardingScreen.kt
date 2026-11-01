package app.bawsala.android.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bawsala.android.BuildConfig
import app.bawsala.android.R
import app.bawsala.android.data.CITIES
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Screen
import app.bawsala.android.ui.account.GoogleCode
import app.bawsala.android.ui.account.rememberGoogleCodeRequest
import app.bawsala.android.ui.components.AppButton
import app.bawsala.android.ui.components.ButtonStyle
import app.bawsala.android.ui.components.Chip
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.settings.PrayerSettingsEdits
import app.bawsala.android.ui.theme.AppColors
import app.bawsala.android.ui.theme.AppType
import app.bawsala.android.viewmodel.AppViewModel

private const val PAGES = 4

/**
 * First launch: what the app is for, how capture works, the city for prayer times, and signing in.
 * Every page can be skipped; nothing here is required to use the app offline.
 */
@Composable
fun OnboardingScreen(vm: AppViewModel, s: Snapshot) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val account by vm.account.account.collectAsStateWithLifecycle()
    val finish: () -> Unit = { vm.settings.savePrefs(s.prefs.copy(onboarded = true)) }

    // Signing in on the last page completes onboarding.
    LaunchedEffect(account) { if (account != null && page == PAGES - 1) finish() }

    Column(
        Modifier.fillMaxSize()
            .background(AppColors.canvas)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Dots(page)
            Spacer(Modifier.weight(1f))
            if (page < PAGES - 1)
                Text(
                    tr("skip"),
                    style = AppType.label,
                    color = AppColors.secondary,
                    modifier =
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .clickable { page = PAGES - 1 }
                            .padding(8.dp),
                )
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            when (page) {
                0 -> Welcome(vm, s)
                1 -> Capture()
                2 -> Prayers(vm, s)
                else ->
                    SignIn(
                        vm,
                        onEmail = {
                            finish()
                            vm.navigator.open(Screen.Account)
                        },
                    )
            }
        }
        if (page < PAGES - 1) {
            AppButton(tr("next"), { page++ })
        } else {
            AppButton(tr("notNow"), finish, style = ButtonStyle.QUIET)
        }
        if (page > 0) AppButton(tr("back"), { page-- }, style = ButtonStyle.QUIET)
    }
}

@Composable
private fun Dots(page: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(PAGES) {
            Box(
                Modifier.size(width = if (it == page) 20.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (it == page) AppColors.primary else AppColors.border)
            )
        }
    }
}

@Composable
private fun ColumnScope.Heading(title: String, body: String) {
    Text(title, style = AppType.h1, color = AppColors.text)
    Text(body, style = AppType.body, color = AppColors.secondary)
}

@Composable
private fun ColumnScope.Symbol(icon: ImageVector) {
    Box(
        Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(AppColors.tint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = AppColors.primary, modifier = Modifier.size(36.dp))
    }
}

/** The name, the needle, and the language, so every later page reads right. */
@Composable
private fun ColumnScope.Welcome(vm: AppViewModel, s: Snapshot) {
    Image(painterResource(R.drawable.brand_mark), null, Modifier.size(160.dp))
    Heading(tr("appName"), tr("onboardWelcome"))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("English", s.lang == "en", { vm.settings.savePrefs(s.prefs.copy(lang = "en")) })
        Chip("العربية", s.lang == "ar", { vm.settings.savePrefs(s.prefs.copy(lang = "ar")) })
    }
}

@Composable
private fun ColumnScope.Capture() {
    Symbol(Icons.Outlined.MoveToInbox)
    Heading(tr("onboardCaptureTitle"), tr("onboardCaptureBody"))
}

/** Picking a city sets the prayer times the plan protects (PRY01). */
@Composable
private fun ColumnScope.Prayers(vm: AppViewModel, s: Snapshot) {
    Symbol(Icons.Outlined.Mosque)
    Heading(tr("onboardPrayerTitle"), tr("onboardPrayerBody"))
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CITIES.forEach { city ->
            Chip(
                if (s.lang == "ar") city.nameAr else city.name,
                s.prayer.label == city.name,
                { vm.settings.savePrayerSettings(PrayerSettingsEdits.withCity(s.prayer, city)) },
            )
        }
    }
    Text(tr("onboardPrayerNote"), style = AppType.micro, color = AppColors.secondary)
}

/** Google signs in and connects Tasks and Calendar in one step; email and offline stay open. */
@Composable
private fun ColumnScope.SignIn(vm: AppViewModel, onEmail: () -> Unit) {
    val server = BuildConfig.DEFAULT_SERVER_URL
    val requestGoogleCode = rememberGoogleCodeRequest { result ->
        when (result) {
            is GoogleCode.Received -> vm.account.finishGoogleSignIn(server, result.code)
            is GoogleCode.Failed -> vm.account.googleSignInFailed(result.reason)
            GoogleCode.Cancelled -> Unit
        }
    }
    Symbol(Icons.Outlined.Sync)
    Heading(tr("onboardSyncTitle"), tr("onboardSyncBody"))
    if (server.isNotBlank()) {
        AppButton(
            tr("continueWithGoogle"),
            { vm.account.startGoogleSignIn(server, requestGoogleCode) },
            enabled = !vm.account.busy,
        )
    }
    AppButton(tr("useEmailInstead"), onEmail, style = ButtonStyle.SECONDARY)
    vm.account.error?.let {
        Text(
            it.detail?.let { d -> tr(it.key) + ": " + d } ?: tr(it.key),
            style = AppType.small,
            color = AppColors.warningText,
            textAlign = TextAlign.Start,
        )
    }
}
