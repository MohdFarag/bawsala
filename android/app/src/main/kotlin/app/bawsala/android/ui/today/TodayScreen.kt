package app.bawsala.android.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.location.DeviceLocation
import app.bawsala.android.navigation.Screen
import app.bawsala.android.navigation.Tab
import app.bawsala.android.ui.design.ActionButton
import app.bawsala.android.ui.design.ActionList
import app.bawsala.android.ui.design.ActionRow
import app.bawsala.android.ui.design.ActionStyle
import app.bawsala.android.ui.design.Avatar
import app.bawsala.android.ui.design.IconAction
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.design.Pill
import app.bawsala.android.ui.design.PillStyle
import app.bawsala.android.ui.design.PriorityCard
import app.bawsala.android.ui.design.ProgressLine
import app.bawsala.android.ui.design.SectionTitle
import app.bawsala.android.ui.design.Wordmark
import app.bawsala.android.ui.design.v3Card
import app.bawsala.android.ui.format.cityLabel
import app.bawsala.android.ui.format.dayLabel
import app.bawsala.android.ui.format.hm
import app.bawsala.android.ui.format.listName
import app.bawsala.android.ui.format.prayerName
import app.bawsala.android.ui.i18n.num
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.settings.MethodSuggestionCard
import app.bawsala.android.ui.settings.TravelCard
import app.bawsala.android.ui.settings.UseMyLocation
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.core.model.Task
import app.bawsala.core.model.TaskStatus
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Content width from which Today uses the tablet layout (prototype container query, 768px). */
private const val WIDE_DP = 768

/**
 * V3 Today (M01 phone, T01 tablet): greeting, date and free time; the priority card; next actions;
 * the next stop (appointment, session or prayer). Tablets add the "calmer mind" inbox card and the
 * day's progress beside the priority card.
 */
@Composable
fun TodayScreen(vm: AppViewModel, s: Snapshot) {
    val today = vm.today()
    val now = vm.now()
    val state = buildTodayState(s, vm.dayPlanner, today, now)
    val account by vm.account.account.collectAsStateWithLifecycle()
    val initial = account?.email?.take(1)?.uppercase() ?: "م"
    // Away from the saved place? Checked once per start, and only if location was already allowed:
    // Today never asks for permission by itself.
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (
            s.prefs.prayerConfirmed &&
                DeviceLocation.granted(context) &&
                vm.settings.shouldCheckTravel()
        )
            vm.settings.checkTravel(DeviceLocation.current(context, s.lang))
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.paper)) {
        val wide = maxWidth.value >= WIDE_DP
        Column(Modifier.fillMaxSize()) {
            Header(wide, initial, onSearch = { vm.navigator.open(Screen.Search) }) {
                vm.navigator.open(Screen.More)
            }
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = if (wide) 30.dp else 22.dp,
                        end = if (wide) 30.dp else 22.dp,
                        top = if (wide) 8.dp else 9.dp,
                        bottom = if (wide) 32.dp else 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(if (wide) 24.dp else 20.dp),
            ) {
                Intro(state, s, today, now, wide) { vm.navigator.open(Screen.Collections) }
                if (!s.prefs.prayerConfirmed) {
                    SetCityCard(cityLabel(s.prayer, s.lang)) {
                        vm.navigator.open(Screen.PrayerSettings)
                    }
                    UseMyLocation(vm)
                }
                vm.settings.travelFix?.let { TravelCard(vm, it) }
                vm.settings.methodSuggestion?.let { MethodSuggestionCard(vm, it) }
                if (wide) {
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Box(Modifier.weight(1.15f)) { Priority(vm, s, state, wide = true) }
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(15.dp),
                        ) {
                            InboxCard(state.inboxCount, Modifier.weight(1f)) {
                                vm.navigator.switchTo(Tab.TRIAGE)
                            }
                            DoneCard(state.tasksDone, state.tasksTotal, Modifier.weight(1f))
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Column(Modifier.weight(1.15f)) {
                            NextActions(vm, s, state.nextActions, wide)
                        }
                        Column(Modifier.weight(1f)) {
                            NextStopSection(vm, s, state.nextStop, now, wide)
                        }
                    }
                } else {
                    Priority(vm, s, state, wide = false)
                    NextActions(vm, s, state.nextActions, wide)
                    NextStopSection(vm, s, state.nextStop, now, wide)
                }
            }
        }
    }
}

/** `.page-header` on Today: the wordmark at the start, search and the account avatar at the end. */
@Composable
private fun Header(wide: Boolean, initial: String, onSearch: () -> Unit, onAccount: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .height(if (wide) 96.dp else 69.dp)
            .padding(
                start = if (wide) 30.dp else 22.dp,
                end = if (wide) 30.dp else 22.dp,
                top = if (wide) 22.dp else 9.dp,
                bottom = if (wide) 16.dp else 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Wordmark(fontSize = if (wide) 38 else 34)
        Spacer(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (wide) 8.dp else 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(R.drawable.ic_lucide_search, tr("search"), onSearch)
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Avatar(initial, onClick = onAccount)
            }
        }
    }
}

/** `.today-intro`: greeting and date, the heading, and the free-time pill (plus follow-ups). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Intro(
    state: TodayState,
    s: Snapshot,
    today: java.time.LocalDate,
    now: Instant,
    wide: Boolean,
    onFollowUps: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (wide) 8.dp else 7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                LucideIcon(R.drawable.ic_lucide_sun, tint = Palette.amber, size = 18.dp)
                Text(
                    tr(greetingKey(now, s.prayer.zone)),
                    style = TypeScale.small.copy(color = Palette.muted),
                )
            }
            Text(
                dayLabel(today, s.lang),
                style =
                    TypeScale.overline.copy(
                        color = Palette.muted,
                        fontWeight = TypeScale.small.fontWeight,
                        fontSize = if (wide) 13.sp else 12.sp,
                    ),
            )
        }
        Text(
            tr("todayHeading"),
            style = TypeScale.headingLg.copy(fontSize = if (wide) 32.sp else 28.sp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.freeTime > Duration.ZERO)
                Pill(
                    tr("freeMinutes", state.freeTime.toMinutes()),
                    style = PillStyle.Outline,
                    icon = R.drawable.ic_lucide_clock_3,
                )
            if (state.followUpsDue > 0)
                Box(Modifier.clip(RoundedCornerShape(30.dp)).clickable(onClick = onFollowUps)) {
                    Pill(
                        tr("followUpsDue", state.followUpsDue),
                        style = PillStyle.Amber,
                        icon = R.drawable.ic_lucide_bell,
                    )
                }
        }
    }
}

private fun greetingKey(now: Instant, zone: ZoneId): String =
    if (now.atZone(zone).hour < 12) "greetingMorning" else "greetingEvening"

/** The priority card, or a quiet card when there is nothing to do. */
@Composable
private fun Priority(vm: AppViewModel, s: Snapshot, state: TodayState, wide: Boolean) {
    val task = state.priority
    if (task == null) {
        Column(Modifier.fillMaxWidth().v3Card(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("noPriority"), style = TypeScale.sectionTitle)
            Text(tr("noPriorityHint"), style = TypeScale.small.copy(color = Palette.muted))
            ActionButton(
                tr("captureIdea"),
                { vm.openCapture() },
                style = ActionStyle.Soft,
                icon = R.drawable.ic_lucide_plus,
            )
        }
        return
    }
    val focusing = s.focus?.taskId == task.id
    PriorityCard(
        overline = tr("priorityNow"),
        title = task.title,
        meta = taskMeta(s, task, includeMinutes = true),
        actionLabel = if (focusing) tr("resume") else tr("startFocus"),
        onAction = {
            vm.focus.start(task)
            vm.navigator.open(Screen.Focus)
        },
        note = if (state.priorityFits) tr("fitsBeforeNext") else null,
        modifier = if (wide) Modifier.heightIn(min = 255.dp) else Modifier,
    )
}

/**
 * `.task-meta`, as in M01: the project (or the list when the task has none), its context, and
 * optionally its estimate.
 */
@Composable
private fun taskMeta(s: Snapshot, task: Task, includeMinutes: Boolean): List<String> =
    listOfNotNull(
        s.projects.firstOrNull { it.id == task.projectId }?.title ?: listName(s, task.listId),
        task.context?.let { tr("context_$it") },
        if (includeMinutes) task.estimateMin?.let { tr("minutes", it) } else null,
    )

/** `.next-section`: up to three next actions, each checkable, with "show all". */
@Composable
private fun NextActions(vm: AppViewModel, s: Snapshot, tasks: List<Task>, wide: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            tr("nextActions"),
            action = tr("showAll"),
            onAction = { vm.navigator.open(Screen.Tasks) },
        )
        if (tasks.isEmpty()) {
            Text(
                tr("noNextActions"),
                style = TypeScale.small.copy(color = Palette.muted),
                modifier = Modifier.fillMaxWidth().v3Card(),
            )
            return
        }
        ActionList {
            tasks.forEachIndexed { i, task ->
                ActionRow(
                    title = task.title,
                    meta = taskMeta(s, task, includeMinutes = false),
                    done = task.status == TaskStatus.COMPLETED,
                    onToggle = { vm.tasks.toggleDone(task) },
                    onOpen = { vm.navigator.open(Screen.TaskDetail(task.id)) },
                    minutes = task.estimateMin?.let { tr("minShort", it) },
                    divider = i < tasks.lastIndex,
                    modifier = if (wide) Modifier.heightIn(min = 84.dp) else Modifier,
                )
            }
        }
    }
}

/** `.agenda-section`: the next appointment, session or prayer, with a link to the planner. */
@Composable
private fun NextStopSection(
    vm: AppViewModel,
    s: Snapshot,
    stop: NextStop?,
    now: Instant,
    wide: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            tr("nextStop"),
            action = tr("calendarLink"),
            onAction = { vm.navigator.switchTo(Tab.REVIEW) },
        )
        val zone = s.prayer.zone
        val (title, detail) =
            when (stop) {
                null -> tr("nothingNext") to tr("nothingNextHint")
                is NextStop.Event ->
                    stop.event.title to
                        listOfNotNull(
                                tr(
                                    "minutes",
                                    Duration.between(stop.event.start, stop.event.end).toMinutes(),
                                ),
                                stop.event.location,
                            )
                            .joinToString(" · ")
                is NextStop.Session ->
                    stop.item.task.title to
                        tr(
                            "minutes",
                            Duration.between(stop.item.session.start, stop.item.session.end)
                                .toMinutes(),
                        )
                is NextStop.Prayer ->
                    tr("prayerOf", prayerName(stop.anchor.prayer, s.lang)) to tr("prayerProtected")
            }
        val open = {
            if (stop is NextStop.Session) vm.navigator.open(Screen.TaskDetail(stop.item.task.id))
            else vm.navigator.switchTo(Tab.REVIEW)
        }
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.sage100)
                .clickable(onClick = open)
                .heightIn(min = if (wide) 96.dp else 0.dp)
                .padding(if (wide) 18.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.width(3.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Palette.sage400)
            )
            if (stop != null)
                Text(
                    num(stop.start.hm(zone)),
                    style =
                        TypeScale.small.copy(
                            fontWeight = TypeScale.sectionTitle.fontWeight,
                            color = Palette.ink,
                        ),
                    modifier = Modifier.width(44.dp),
                )
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style =
                        TypeScale.small.copy(
                            fontWeight = TypeScale.button.fontWeight,
                            color = Palette.ink,
                            fontSize = if (wide) 14.sp else 13.sp,
                        ),
                )
                Text(detail, style = TypeScale.tiny)
            }
            if (stop != null)
                LucideIcon(
                    R.drawable.ic_lucide_arrow_up_right,
                    tint = Palette.green900,
                    size = 18.dp,
                )
        }
    }
}

/** Tablet: "مساحة ذهنية أهدأ", how many ideas wait in triage, and a button to triage one. */
@Composable
private fun InboxCard(count: Int, modifier: Modifier = Modifier, onTriage: () -> Unit) {
    Column(modifier.fillMaxWidth().v3Card(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("calmerMind"),
                style = TypeScale.sectionTitle.copy(fontWeight = TypeScale.button.fontWeight),
                modifier = Modifier.weight(1f),
            )
            LucideIcon(R.drawable.ic_lucide_inbox, tint = Palette.green900)
        }
        Text(
            if (count > 0) tr("inboxWaiting", count) else tr("inboxClear"),
            style =
                TypeScale.overline.copy(
                    color = Palette.muted,
                    fontWeight = TypeScale.small.fontWeight,
                ),
        )
        if (count > 0)
            ActionButton(
                tr("triageOne"),
                onTriage,
                style = ActionStyle.Soft,
                icon = R.drawable.ic_lucide_arrow_up_right,
            )
    }
}

/** Tablet: the day's progress, "١ من ٤ خطوة اكتملت اليوم". */
@Composable
private fun DoneCard(done: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().v3Card(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Palette.sage100),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(R.drawable.ic_lucide_check_check, tint = Palette.green900)
        }
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(num(done), style = TypeScale.heading)
                Text(tr("ofTotal", total), style = TypeScale.small.copy(color = Palette.muted))
            }
            Text(tr("stepsDoneToday"), style = TypeScale.small.copy(color = Palette.muted))
        }
        Box(Modifier.width(90.dp)) { ProgressLine(if (total == 0) 0f else done.toFloat() / total) }
    }
}

/** Shown until the prayer city is confirmed: prayer times drive the plan. */
@Composable
private fun SetCityCard(city: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.amberTint)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LucideIcon(R.drawable.ic_lucide_moon, tint = Palette.amber, size = 18.dp)
        Text(
            tr("setCity") + " · $city",
            style =
                TypeScale.small.copy(
                    color = Palette.amber,
                    fontWeight = TypeScale.button.fontWeight,
                ),
            modifier = Modifier.weight(1f),
        )
        LucideIcon(R.drawable.ic_lucide_chevron_right, tint = Palette.amber, size = 16.dp)
    }
}
