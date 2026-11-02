package app.bawsala.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bawsala.android.ui.format.LocalAppClock
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Screen
import app.bawsala.android.navigation.Tab
import app.bawsala.android.navigation.showsBottomBar
import app.bawsala.android.ui.account.AccountScreen
import app.bawsala.android.ui.account.CalendarsScreen
import app.bawsala.android.ui.capture.CaptureSheet
import app.bawsala.android.ui.collections.CollectionsScreen
import app.bawsala.android.ui.collections.TasksScreen
import app.bawsala.android.ui.collections.TrashScreen
import app.bawsala.android.ui.design.Avatar
import app.bawsala.android.ui.design.BottomBar
import app.bawsala.android.ui.design.DesignGallery
import app.bawsala.android.ui.design.NavDestination
import app.bawsala.android.ui.design.SideRail
import app.bawsala.android.ui.design.Toast
import app.bawsala.android.ui.design.isTabletWidth
import app.bawsala.android.ui.focus.FocusScreen
import app.bawsala.android.ui.i18n.LocalLang
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.inbox.InboxScreen
import app.bawsala.android.ui.more.MoreScreen
import app.bawsala.android.ui.onboarding.OnboardingScreen
import app.bawsala.android.ui.plan.EventSheet
import app.bawsala.android.ui.plan.PlanScreen
import app.bawsala.android.ui.plan.ScheduleSheet
import app.bawsala.android.ui.planner.WeekPlannerScreen
import app.bawsala.android.ui.projects.ProjectsScreen
import app.bawsala.android.ui.review.ReviewScreen
import app.bawsala.android.ui.search.SearchSheet
import app.bawsala.android.ui.settings.BlocksScreen
import app.bawsala.android.ui.settings.ListsScreen
import app.bawsala.android.ui.settings.PrayerSettingsScreen
import app.bawsala.android.ui.settings.PreferencesScreen
import app.bawsala.android.ui.task.TaskDetailScreen
import app.bawsala.android.ui.theme.AppColors
import app.bawsala.android.ui.today.TodayScreen
import app.bawsala.android.ui.triage.TriageScreen
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.android.viewmodel.DuplicateShare
import app.bawsala.core.model.CaptureState
import kotlinx.coroutines.delay

private const val MESSAGE_MILLIS = 4_000L

/**
 * The app shell: current screen, bottom navigation, capture button, sheets, messages and dialogs.
 */
@Composable
fun AppRoot(vm: AppViewModel, onExit: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val screen = vm.navigator.current
    BackHandler { if (!vm.navigator.back()) onExit() }

    CompositionLocalProvider(
        LocalLang provides s.lang,
        LocalAppClock provides vm.clock,
        LocalLayoutDirection provides
            if (s.lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        Box(Modifier.fillMaxSize().background(AppColors.canvas)) {
            if (!s.loaded) return@Box
            if (!s.prefs.onboarded) {
                Box(Modifier.statusBarsPadding()) { OnboardingScreen(vm, s) }
                return@Box
            }
            Shell(vm, s, screen)
            if (screen == Screen.Search) SearchSheet(vm, s)
            if (vm.captureOpen) CaptureSheet(vm)
            vm.scheduling?.let { ScheduleSheet(vm, s, it) }
            vm.editingEvent?.let { EventSheet(vm, s, it) }
            vm.capture.duplicate?.let { DuplicateShareDialog(it, vm) }
        }
    }
}

@Composable
private fun ScreenHost(screen: Screen, vm: AppViewModel, s: Snapshot) =
    when (screen) {
        Screen.Today -> TodayScreen(vm, s)
        Screen.Inbox -> TriageScreen(vm, s)
        Screen.InboxList -> InboxScreen(vm, s)
        Screen.Plan -> WeekPlannerScreen(vm, s)
        Screen.DayPlan -> PlanScreen(vm, s)
        Screen.Review -> ReviewScreen(vm, s)
        Screen.More -> MoreScreen(vm, s)
        Screen.Projects -> ProjectsScreen(vm, s)
        is Screen.Process -> TriageScreen(vm, s, screen.itemId, opened = true)
        is Screen.TaskDetail -> TaskDetailScreen(vm, s, screen.id, screen.fresh)
        Screen.Focus -> FocusScreen(vm, s)
        Screen.Search -> Unit // drawn as a sheet by AppRoot
        Screen.Tasks -> TasksScreen(vm, s)
        Screen.Collections -> CollectionsScreen(vm, s)
        Screen.Trash -> TrashScreen(vm, s)
        Screen.PrayerSettings -> PrayerSettingsScreen(vm, s)
        Screen.Blocks -> BlocksScreen(vm, s)
        Screen.Lists -> ListsScreen(vm, s)
        Screen.Preferences -> PreferencesScreen(vm, s)
        Screen.Account -> AccountScreen(vm, s)
        Screen.Calendars -> CalendarsScreen(vm, s)
        Screen.DesignGallery -> DesignGallery(onBack = { vm.navigator.back() })
    }

/**
 * V3 navigation around the current screen: the five-slot bottom bar on phones, the side rail on
 * windows 840dp and wider (tablets). Focus hides the bottom bar, as in the design.
 */
@Composable
private fun Shell(vm: AppViewModel, s: Snapshot, current: Screen) {
    // Search opens as a sheet over the screen it was opened from (prototype `search` overlay).
    val screen = if (current == Screen.Search) vm.navigator.beneath ?: Screen.Today else current
    val inboxCount = s.items.count { it.state == CaptureState.INBOX }
    val destinations =
        Tab.entries.associateWith { tab ->
            when (tab) {
                Tab.TODAY -> NavDestination(tab.name, tr("today"), R.drawable.ic_lucide_sun)
                Tab.TRIAGE ->
                    NavDestination(tab.name, tr("triage"), R.drawable.ic_lucide_inbox, inboxCount)
                Tab.PROJECTS ->
                    NavDestination(tab.name, tr("projects"), R.drawable.ic_lucide_layers)
                Tab.REVIEW ->
                    NavDestination(tab.name, tr("review"), R.drawable.ic_lucide_calendar_days)
            }
        }
    val start = listOf(destinations.getValue(Tab.TODAY), destinations.getValue(Tab.TRIAGE))
    val end = listOf(destinations.getValue(Tab.PROJECTS), destinations.getValue(Tab.REVIEW))
    val active = vm.navigator.currentTab.name
    val select = { key: String -> vm.navigator.switchTo(Tab.valueOf(key)) }

    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        val tablet = isTabletWidth(maxWidth.value.toInt())
        if (tablet) {
            Row(Modifier.fillMaxSize()) {
                if (screen != Screen.Focus)
                    SideRail(
                        start,
                        end,
                        active,
                        select,
                        captureLabel = tr("capture"),
                        onCapture = { vm.openCapture() },
                        onCaptureHold = { vm.openCapture(voice = true) },
                        bottom = {
                            Avatar(
                                vm.account.account
                                    .collectAsStateWithLifecycle()
                                    .value
                                    ?.email
                                    ?.take(1)
                                    ?.uppercase() ?: "م",
                                onClick = { vm.navigator.open(Screen.More) },
                            )
                        },
                    )
                Box(Modifier.weight(1f).fillMaxSize()) {
                    ScreenHost(screen, vm, s)
                    Messages(vm, bottomInset = 24.dp)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    ScreenHost(screen, vm, s)
                    Messages(vm, bottomInset = 17.dp)
                }
                if (screen.showsBottomBar)
                    BottomBar(
                        start,
                        end,
                        active,
                        select,
                        captureLabel = tr("capture"),
                        onCapture = { vm.openCapture() },
                        onCaptureHold = { vm.openCapture(voice = true) },
                    )
            }
        }
    }
}

/** The current message as a V3 toast, dismissed after a few seconds; Undo when offered. */
@Composable
private fun BoxScope.Messages(vm: AppViewModel, bottomInset: androidx.compose.ui.unit.Dp) {
    val message = vm.messages.current ?: return
    LaunchedEffect(message.id) {
        delay(MESSAGE_MILLIS)
        vm.messages.dismiss(message)
    }
    val text = if (message.argKey != null) tr(message.key, tr(message.argKey)) else tr(message.key)
    Toast(
        text,
        actionLabel = message.undo?.let { tr("undo") },
        onAction = message.undo,
        bottomInset = bottomInset,
    )
}

@Composable
private fun DuplicateShareDialog(duplicate: DuplicateShare, vm: AppViewModel) {
    AlertDialog(
        onDismissRequest = vm.capture::keepDuplicate,
        title = { Text(tr("duplicateShare")) },
        text = { Text(duplicate.existing.title) },
        confirmButton = {
            TextButton(vm.capture::openExistingInsteadOfDuplicate) { Text(tr("openExisting")) }
        },
        dismissButton = { TextButton(vm.capture::keepDuplicate) { Text(tr("keepNew")) } },
    )
}
