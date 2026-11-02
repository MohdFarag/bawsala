package app.bawsala.android.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Screen
import app.bawsala.android.ui.design.ActionList
import app.bawsala.android.ui.design.ChoiceChip
import app.bawsala.android.ui.design.IconAction
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.design.MetaLine
import app.bawsala.android.ui.design.OptionRow
import app.bawsala.android.ui.design.PageHeader
import app.bawsala.android.ui.design.PanelSheet
import app.bawsala.android.ui.design.Pill
import app.bawsala.android.ui.design.PillStyle
import app.bawsala.android.ui.format.hm
import app.bawsala.android.ui.format.locale
import app.bawsala.android.ui.i18n.localizeDigits
import app.bawsala.android.ui.i18n.num
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.plan.PlanItem
import app.bawsala.android.ui.plan.PlanItemKind
import app.bawsala.android.ui.plan.label
import app.bawsala.android.ui.plan.openPlanItem
import app.bawsala.android.ui.plan.opens
import app.bawsala.android.ui.plan.planItems
import app.bawsala.android.ui.review.reviewWeekStart
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.android.viewmodel.EventTarget
import app.bawsala.android.viewmodel.ScheduleTarget
import app.bawsala.core.model.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle

/** Content width from which the next-actions panel sits beside the grid (T04). */
private const val WIDE_DP = 840

private val HOUR_HEIGHT = 57.dp
private val GUTTER = 50.dp
private const val SCROLL_TO_HOUR = 8

/** Where a dragged task is, and where each day column is on screen, to find the drop time. */
private class WeekDrag {
    var task by mutableStateOf<Task?>(null)
    var pointer by mutableStateOf(Offset.Zero)
    var viewport: Rect? = null
    val columns = mutableStateMapOf<LocalDate, Rect>()

    /** The day and time under the pointer, or null when it is not over the grid. */
    fun target(hourPx: Float): Pair<LocalDate, LocalTime>? {
        if (viewport?.contains(pointer) != true) return null
        val (day, rect) =
            columns.entries
                .firstOrNull { pointer.x >= it.value.left && pointer.x < it.value.right }
                ?.toPair() ?: return null
        val minute = dropMinute((pointer.y - rect.top) / hourPx * 60f)
        return day to LocalTime.of(minute / 60, minute % 60)
    }
}

/**
 * V3 weekly planner (T04): the week from Saturday as a time grid, with sessions (green), events
 * (lilac) and prayer times (amber) as blocks, and the next actions beside it. A task is planned by
 * dragging its card onto the grid (long press) or with "+"; either way the scheduling sheet opens
 * with the time proposed, so nothing is placed without confirmation. On phones the grid scrolls
 * sideways and the next actions follow below it.
 */
@Composable
fun WeekPlannerScreen(vm: AppViewModel, s: Snapshot) {
    val today = vm.today()
    var weekOffset by rememberSaveable { mutableStateOf(0L) }
    var context by rememberSaveable { mutableStateOf<String?>(null) }
    var options by remember { mutableStateOf(false) }
    val start = reviewWeekStart(today).plusWeeks(weekOffset)
    val days = weekDays(start)
    val drag = remember { WeekDrag() }
    val zone = s.prayer.zone
    val week =
        remember(s.tasks, s.sessions, s.rules, s.events, s.blocks, s.prayer, s.prefs, start) {
            days.associateWith { placeDay(planItems(s, vm.dayPlanner, it), it, zone) }
        }
    val panel = nextActionsPanel(s, vm.now(), context)

    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.paper)) {
        val wide = maxWidth.value >= WIDE_DP
        Column(Modifier.fillMaxSize()) {
            PageHeader(tr("weekPlanner"), tr("weekPlannerSubtitle"), wide) {
                IconAction(
                    R.drawable.ic_lucide_search,
                    tr("search"),
                    { vm.navigator.open(Screen.Search) },
                )
                IconAction(R.drawable.ic_lucide_ellipsis, tr("pageOptions"), { options = true })
            }
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState(), enabled = drag.task == null)
                    .padding(
                        start = if (wide) 30.dp else 22.dp,
                        end = if (wide) 30.dp else 22.dp,
                        bottom = 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Intro(weekOffset, wide) { weekOffset += it }
                val grid: @Composable (Modifier) -> Unit = { m ->
                    WeekGrid(vm, s, days, week, today, wide, drag, m)
                }
                val actions: @Composable (Modifier) -> Unit = { m ->
                    NextActions(vm, s, panel, context, { context = it }, drag, m)
                }
                if (wide)
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        grid(Modifier.weight(2.6f))
                        actions(Modifier.weight(1f))
                    }
                else {
                    grid(Modifier)
                    actions(Modifier)
                }
            }
        }
        DragGhost(drag)
        if (options) Options(vm, today, wide) { options = false }
    }
}

/** "خطواتك، في وقتها." with the hint, and the week switcher. */
@Composable
private fun Intro(weekOffset: Long, wide: Boolean, move: (Long) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                tr("plannerHeading"),
                style = TypeScale.headingLg.copy(fontSize = if (wide) 31.sp else 25.sp),
            )
            Text(tr("plannerHint"), style = TypeScale.tiny)
        }
        IconAction(R.drawable.ic_lucide_chevron_left, tr("previousWeek"), { move(-1) })
        Text(
            when (weekOffset) {
                0L -> tr("thisWeek")
                1L -> tr("nextWeek")
                -1L -> tr("lastWeek")
                else -> tr("weeksAway", num(weekOffset))
            },
            style = TypeScale.small,
        )
        IconAction(R.drawable.ic_lucide_chevron_right, tr("nextWeekAction"), { move(1) })
    }
}

/** The grid card: range and legend, day headers, and the hours with their blocks. */
@Composable
private fun WeekGrid(
    vm: AppViewModel,
    s: Snapshot,
    days: List<LocalDate>,
    week: Map<LocalDate, List<Placed>>,
    today: LocalDate,
    wide: Boolean,
    drag: WeekDrag,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val loc = locale(s.lang)
    val dropping = drag.task != null
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.white)
            .border(
                if (dropping) 2.dp else 1.dp,
                if (dropping) Palette.green900 else Palette.line,
                RoundedCornerShape(20.dp),
            )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                rangeLabel(days.first(), days.last(), s.lang),
                style = TypeScale.button,
                modifier = Modifier.weight(1f),
            )
            Legend(tr("legendFocus"), Palette.green100)
            Legend(tr("legendEvent"), Palette.lilac)
            Legend(tr("legendPrayer"), Palette.amberTint)
        }
        val hScroll = rememberScrollState()
        val vScroll = rememberScrollState()
        LaunchedEffect(Unit) {
            vScroll.scrollTo(
                with(density) { (HOUR_HEIGHT * (SCROLL_TO_HOUR - FIRST_HOUR)).roundToPx() }
            )
        }
        Row(Modifier.fillMaxWidth()) {
            // The hour labels stay put while the days scroll sideways on phones.
            Column(Modifier.width(GUTTER)) {
                Spacer(Modifier.height(62.dp))
                Box(Modifier.height(480.dp).verticalScroll(vScroll, enabled = false)) {
                    Column {
                        for (h in FIRST_HOUR until LAST_HOUR) Box(
                            Modifier.height(HOUR_HEIGHT).fillMaxWidth().padding(top = 4.dp),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Text(
                                localizeDigits("%d:00".format(h % 24), s.lang),
                                style = TypeScale.tiny.copy(fontSize = 10.sp),
                            )
                        }
                    }
                }
            }
            // Tablets share the width among the seven days; phones scroll sideways.
            BoxWithConstraints(Modifier.weight(1f)) {
                val cell = Modifier.width(if (wide) maxWidth / 7 else 92.dp)
                Column(if (wide) Modifier else Modifier.horizontalScroll(hScroll)) {
                    Row {
                        days.forEach { day ->
                            DayHeader(day, day == today, loc, cell)
                        }
                    }
                    Box(
                        Modifier.height(480.dp)
                            .onGloballyPositioned { drag.viewport = it.boundsInRoot() }
                            .verticalScroll(vScroll)
                    ) {
                        Row {
                            days.forEach { day ->
                                DayColumn(
                                    vm,
                                    day,
                                    week[day].orEmpty(),
                                    isToday = day == today,
                                    now = if (day == today) vm.now() else null,
                                    s = s,
                                    modifier =
                                        cell.onGloballyPositioned {
                                            val p = it.positionInRoot()
                                            drag.columns[day] =
                                                Rect(
                                                    p,
                                                    androidx.compose.ui.geometry.Size(
                                                        it.size.width.toFloat(),
                                                        it.size.height.toFloat(),
                                                    ),
                                                )
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (!wide) Spacer(Modifier.height(8.dp))
    }
    // Keep the drop targets to the week shown.
    LaunchedEffect(days.first()) { drag.columns.keys.retainAll(days.toSet()) }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = TypeScale.tiny.copy(fontSize = 10.sp))
    }
}

@Composable
private fun DayHeader(day: LocalDate, isToday: Boolean, loc: java.util.Locale, modifier: Modifier) {
    Column(
        modifier
            .height(62.dp)
            .background(if (isToday) Palette.green100.copy(alpha = 0.5f) else Color.Transparent)
            .border(0.5.dp, Palette.gridLine),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Text(
            day.dayOfWeek.getDisplayName(TextStyle.FULL, loc),
            style = TypeScale.tiny.copy(fontSize = 10.sp),
            maxLines = 1,
        )
        Box(
            Modifier.size(28.dp)
                .clip(CircleShape)
                .background(if (isToday) Palette.green900 else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                num(day.dayOfMonth),
                style = TypeScale.button.copy(color = if (isToday) Palette.white else Palette.ink),
            )
        }
    }
}

/** One day: hour lines, its blocks, and the current time on today. */
@Composable
private fun DayColumn(
    vm: AppViewModel,
    day: LocalDate,
    items: List<Placed>,
    isToday: Boolean,
    now: java.time.Instant?,
    s: Snapshot,
    modifier: Modifier,
) {
    val perMin = HOUR_HEIGHT / 60
    BoxWithConstraints(
        modifier
            .height(HOUR_HEIGHT * (LAST_HOUR - FIRST_HOUR))
            .background(if (isToday) Palette.green100.copy(alpha = 0.35f) else Color.Transparent)
    ) {
        val width = maxWidth
        Column {
            repeat(LAST_HOUR - FIRST_HOUR) {
                Box(Modifier.fillMaxWidth().height(HOUR_HEIGHT).border(0.5.dp, Palette.gridLine))
            }
        }
        items.forEach { p ->
            val laneWidth = (width - 6.dp) / p.lanes
            Block(
                p.item,
                Modifier.offset(
                        x = 3.dp + laneWidth * p.lane,
                        y = perMin * (p.startMin - FIRST_HOUR * 60),
                    )
                    .width(laneWidth - 2.dp)
                    .height((perMin * (p.endMin - p.startMin)).coerceAtLeast(22.dp)),
                showTime = p.endMin - p.startMin >= 45,
                lang = s.lang,
                zone = s.prayer.zone,
            ) {
                if (p.item.opens) vm.openPlanItem(p.item, day)
            }
        }
        if (now != null) {
            val minute = now.atZone(s.prayer.zone).toLocalTime().let { it.hour * 60 + it.minute }
            if (minute in FIRST_HOUR * 60 until LAST_HOUR * 60)
                Box(
                    Modifier.offset(y = perMin * (minute - FIRST_HOUR * 60))
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Palette.amber)
                )
        }
    }
}

/** `.cal-block`: a session (green), event (lilac) or prayer (amber) with its time. */
@Composable
private fun Block(
    item: PlanItem,
    modifier: Modifier,
    showTime: Boolean,
    lang: String,
    zone: java.time.ZoneId,
    onClick: () -> Unit,
) {
    val (bg, fg) =
        when (item.kind) {
            PlanItemKind.TASK -> Palette.green100 to Palette.green900
            PlanItemKind.EVENT -> Palette.lilac to Palette.lilacInk
            PlanItemKind.PRAYER -> Palette.amberTint to Palette.amber
        }
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(
                if (item.opens) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            item.label(),
            style =
                TypeScale.tiny.copy(
                    color = fg,
                    fontSize = 11.sp,
                    fontWeight = TypeScale.button.fontWeight,
                ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (showTime)
            Text(
                localizeDigits("${item.start.hm(zone)} — ${item.end.hm(zone)}", lang),
                style = TypeScale.tiny.copy(color = fg, fontSize = 9.sp),
                maxLines = 1,
            )
    }
}

/** `.planner-side`: next actions with context filters; drag a card or tap "+" to plan it. */
@Composable
private fun NextActions(
    vm: AppViewModel,
    s: Snapshot,
    panel: NextActionsPanel,
    context: String?,
    onContext: (String?) -> Unit,
    drag: WeekDrag,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("nextActions"), style = TypeScale.sectionTitle, modifier = Modifier.weight(1f))
            Pill(tr("readyCount", panel.ready), style = PillStyle.Outline)
        }
        if (panel.contexts.isNotEmpty()) {
            Text(tr("context"), style = TypeScale.tiny)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip(tr("all"), context == null, { onContext(null) })
                panel.contexts.forEach { c ->
                    ChoiceChip(tr("context_$c"), context == c, { onContext(c) })
                }
            }
        }
        if (panel.tasks.isEmpty())
            Text(
                tr("nothingToPlan"),
                style = TypeScale.small.copy(color = Palette.muted),
            )
        panel.tasks.take(8).forEach { task -> ActionCard(vm, s, task, drag) }
        if (panel.tasks.isNotEmpty())
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LucideIcon(
                    R.drawable.ic_lucide_arrow_left_right,
                    tint = Palette.muted,
                    size = 16.dp,
                )
                Text(tr("dragCardHint"), style = TypeScale.tiny)
            }
        Text(
            tr("leaveSpace"),
            style = TypeScale.small,
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.sage100)
                    .padding(16.dp),
        )
    }
}

/** A next action: grip, title and meta, and "+"; long-press to drag it onto the week. */
@Composable
private fun ActionCard(vm: AppViewModel, s: Snapshot, task: Task, drag: WeekDrag) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val hourPx = with(LocalDensity.current) { HOUR_HEIGHT.toPx() }
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.white)
            .border(1.dp, Palette.line, RoundedCornerShape(18.dp))
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        drag.task = task
                        drag.pointer = origin + it
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        drag.pointer += amount
                    },
                    onDragEnd = {
                        drag.target(hourPx)?.let { (day, time) ->
                            vm.scheduling = ScheduleTarget(task.id, proposed = day.atTime(time))
                        }
                        drag.task = null
                    },
                    onDragCancel = { drag.task = null },
                )
            }
            .clickable { vm.navigator.open(Screen.TaskDetail(task.id)) }
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LucideIcon(R.drawable.ic_lucide_grip_vertical, tint = Palette.muted, size = 18.dp)
        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(
                task.title,
                style = TypeScale.button,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            MetaLine(
                listOfNotNull(
                    task.context?.let { tr("context_$it") },
                    task.estimateMin?.let { tr("minutesShort", num(it)) },
                )
            )
        }
        IconAction(
            R.drawable.ic_lucide_plus,
            tr("planSession"),
            { vm.scheduling = ScheduleTarget(task.id) },
        )
    }
}

/** The card following the finger while dragging. */
@Composable
private fun BoxScope.DragGhost(drag: WeekDrag) {
    val task = drag.task ?: return
    val density = LocalDensity.current
    Box(Modifier.matchParentSize()) {
        Text(
            task.title,
            style = TypeScale.button.copy(color = Palette.white),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier.offset {
                        IntOffset(
                            (drag.pointer.x - with(density) { 80.dp.toPx() }).toInt(),
                            (drag.pointer.y - with(density) { 60.dp.toPx() }).toInt(),
                        )
                    }
                    .widthIn(max = 200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.green900.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** Page options: the day view (timeline and agenda), a new event, and time settings. */
@Composable
private fun BoxScope.Options(vm: AppViewModel, today: LocalDate, wide: Boolean, close: () -> Unit) {
    fun go(action: () -> Unit): () -> Unit = {
        close()
        action()
    }
    PanelSheet(tr("pageOptions"), close, tr("close"), centered = wide) {
        ActionList(Modifier.border(1.dp, Palette.line, RoundedCornerShape(16.dp))) {
            OptionRow(
                tr("dayView"),
                R.drawable.ic_lucide_list_checks,
                go { vm.navigator.open(Screen.DayPlan) },
            )
            OptionRow(
                tr("newEvent"),
                R.drawable.ic_lucide_calendar_days,
                go { vm.editingEvent = EventTarget(today) },
            )
            OptionRow(
                tr("timeBlocks"),
                R.drawable.ic_lucide_layout_grid,
                go { vm.navigator.open(Screen.Blocks) },
            )
            OptionRow(
                tr("prayerTimes"),
                R.drawable.ic_lucide_moon,
                go { vm.navigator.open(Screen.PrayerSettings) },
                divider = false,
            )
        }
    }
}

/** "31 أكتوبر — 6 نوفمبر 2026". */
private fun rangeLabel(from: LocalDate, to: LocalDate, lang: String): String {
    val loc = locale(lang)
    fun d(x: LocalDate) =
        "${x.dayOfMonth} ${x.month.getDisplayName(TextStyle.FULL_STANDALONE, loc)}"
    return localizeDigits("${d(from)} — ${d(to)} ${to.year}", lang)
}
