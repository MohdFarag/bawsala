package app.bawsala.android.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.ui.auth.AuthFlow
import app.bawsala.android.ui.auth.AuthStep
import app.bawsala.android.ui.auth.SwitchLink
import app.bawsala.android.ui.design.ActionButton
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.design.Wordmark
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel

private const val PAGES = 3

/**
 * First launch (V3 OM01–OM03): three pages, then sign-up or sign-in (AM01–AM06). Skipping goes
 * straight to the account step, which can also be skipped: the app works on this device only.
 */
@Composable
fun OnboardingScreen(vm: AppViewModel, s: Snapshot) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var auth by rememberSaveable { mutableStateOf<AuthStep?>(null) }
    val account by vm.account.account.collectAsStateWithLifecycle()
    val finish: () -> Unit = { vm.settings.savePrefs(s.prefs.copy(onboarded = true)) }

    // Already signed in (e.g. this device was set up before): nothing left to ask.
    LaunchedEffect(Unit) { if (account != null) finish() }

    val step = auth
    if (step != null) {
        AuthFlow(vm, step, onBack = { auth = null }, onDone = finish, onSkip = finish)
        return
    }
    Pages(
        page,
        onNext = { if (page < PAGES - 1) page++ else auth = AuthStep.SIGN_UP },
        onSkip = { auth = AuthStep.SIGN_UP },
        onSignIn = { auth = AuthStep.LOGIN },
    )
}

/** One page's content, as in OM01–OM03. */
private data class Page(
    val eyebrowIcon: Int,
    val eyebrow: String,
    val title: String,
    val body: String,
)

@Composable
private fun Pages(page: Int, onNext: () -> Unit, onSkip: () -> Unit, onSignIn: () -> Unit) {
    val pages =
        listOf(
            Page(R.drawable.ic_lucide_sun, "onboard1Eyebrow", "onboard1Title", "onboard1Body"),
            Page(R.drawable.ic_lucide_inbox, "onboard2Eyebrow", "onboard2Title", "onboard2Body"),
            Page(
                R.drawable.ic_lucide_check_check,
                "onboard3Eyebrow",
                "onboard3Title",
                "onboard3Body",
            ),
        )
    val p = pages[page]
    val last = page == PAGES - 1
    Column(
        Modifier.fillMaxSize()
            .background(Palette.paper)
            .navigationBarsPadding()
            .padding(horizontal = 26.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Wordmark(fontSize = 34)
            Spacer(Modifier.weight(1f))
            if (!last)
                Text(
                    tr("skip"),
                    style =
                        TypeScale.small.copy(
                            color = Palette.muted,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    modifier =
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClick = onSkip)
                            .padding(12.dp),
                )
        }
        Column(
            Modifier.weight(1f).widthIn(max = 460.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Illustration(page, Modifier.padding(top = 28.dp))
            Row(
                Modifier.padding(top = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(p.eyebrowIcon, tint = Palette.muted, size = 16.dp)
                Text(tr(p.eyebrow), style = TypeScale.tiny.copy(fontWeight = FontWeight.SemiBold))
            }
            Text(
                tr(p.title),
                style = TypeScale.headingLg.copy(fontSize = 29.sp, lineHeight = 50.sp),
                color = Palette.green900,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                tr(p.body),
                style = TypeScale.small.copy(color = Palette.muted),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp),
            )
            Dots(page, Modifier.padding(top = 24.dp))
        }
        ActionButton(
            tr(if (last) "startWithBawsala" else "next"),
            onNext,
            Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            icon = R.drawable.ic_lucide_arrow_up_right,
        )
        if (last) SwitchLink(tr("haveAccount"), tr("signIn"), onSignIn)
        else
            Text(
                tr("onboardFooter"),
                style = TypeScale.tiny.copy(fontSize = 10.sp),
                modifier = Modifier.padding(vertical = 20.dp),
            )
    }
}

/** The active page is a long bar, the others small dots. */
@Composable
private fun Dots(page: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(PAGES) {
            Box(
                Modifier.size(width = if (it == page) 24.dp else 6.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(if (it == page) Palette.green900 else Palette.line)
            )
        }
    }
}

/** The sage circle with each page's picture and its two floating notes. */
@Composable
private fun Illustration(page: Int, modifier: Modifier) {
    Box(modifier.size(width = 320.dp, height = 220.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(190.dp).clip(CircleShape).background(Palette.green100))
        when (page) {
            0 -> Image(painterResource(R.drawable.brand_mark), null, Modifier.size(130.dp))
            1 -> CaptureCards()
            else -> CalendarCard()
        }
        val (top, bottom) =
            when (page) {
                0 ->
                    Note(R.drawable.ic_lucide_sun, "onboard1NoteTop") to
                        Note(R.drawable.ic_lucide_clock_3, "onboard1NoteBottom")
                1 ->
                    Note(R.drawable.ic_lucide_inbox, "onboard2NoteTop") to
                        Note(R.drawable.ic_lucide_mic, "onboard2NoteBottom")
                else ->
                    Note(R.drawable.ic_lucide_check_check, "onboard3NoteTop", lilac = true) to
                        Note(R.drawable.ic_lucide_clock_3, "onboard3NoteBottom")
            }
        FloatingNote(top, Modifier.align(Alignment.TopStart).offset(y = 8.dp))
        FloatingNote(bottom, Modifier.align(Alignment.BottomEnd).offset(y = (-24).dp))
    }
}

private data class Note(@DrawableRes val icon: Int, val key: String, val lilac: Boolean = false)

@Composable
private fun BoxScope.FloatingNote(note: Note, modifier: Modifier) {
    val (bg, fg) =
        if (note.lilac) Palette.lilac to Palette.lilacInk else Palette.white to Palette.ink
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (note.key.endsWith("Bottom")) Palette.green100 else bg)
            .border(1.dp, Palette.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(note.icon, tint = if (note.lilac) fg else Palette.green900, size = 16.dp)
        Text(tr(note.key), style = TypeScale.tiny.copy(color = fg, fontSize = 11.sp))
    }
}

/** OM02: two captured notes and the green capture button. */
@Composable
private fun CaptureCards() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf(true, false).forEach { done ->
            Row(
                Modifier.width(130.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.white)
                    .border(1.dp, Palette.line, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (done)
                    LucideIcon(R.drawable.ic_lucide_check, tint = Palette.green900, size = 14.dp)
                else Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.sage400))
                Box(
                    Modifier.weight(1f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Palette.sage400.copy(alpha = 0.6f))
                )
            }
        }
        Box(
            Modifier.size(50.dp).clip(CircleShape).background(Palette.green900),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(R.drawable.ic_lucide_plus, tint = Palette.white, size = 28.dp)
        }
    }
}

/** OM03: a week with a few blocks, and a check. */
@Composable
private fun CalendarCard() {
    Box {
        Column(
            Modifier.size(width = 150.dp, height = 140.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Palette.white)
                .border(1.dp, Palette.line, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(width = 60.dp, height = 22.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.sage400)
            )
            Box(
                Modifier.size(width = 26.dp, height = 22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Palette.lilac)
            )
            Box(
                Modifier.size(width = 46.dp, height = 22.dp)
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.green100)
            )
        }
        Box(
            Modifier.align(Alignment.BottomEnd)
                .offset(x = 16.dp, y = 14.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(Palette.green900)
                .border(3.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(R.drawable.ic_lucide_check, tint = Palette.white, size = 26.dp)
        }
    }
}
