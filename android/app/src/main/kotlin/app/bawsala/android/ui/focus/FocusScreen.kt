package app.bawsala.android.ui.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.domain.siblingOrder
import app.bawsala.android.navigation.Screen
import app.bawsala.android.navigation.Tab
import app.bawsala.android.ui.design.ActionButton
import app.bawsala.android.ui.design.IconAction
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.design.RoundCheck
import app.bawsala.android.ui.design.motionSpec
import app.bawsala.android.ui.format.clock
import app.bawsala.android.ui.format.listName
import app.bawsala.android.ui.i18n.num
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.android.viewmodel.ScheduleTarget
import app.bawsala.core.focus.FocusSession
import app.bawsala.core.focus.FocusState
import app.bawsala.core.model.Task
import app.bawsala.core.model.TaskStatus
import java.time.Instant
import kotlinx.coroutines.delay

private const val TICK_MILLIS = 1_000L
private const val WIDE_DP = 768

/**
 * V3 calm focus (M07 phone, T06 tablet): Pomodoro or open timer, the ring, the task and its
 * subtasks as a checklist, pause/resume and end, and "a stray thought? leave it here", which opens
 * capture without stopping the timer (M08). Time running out never completes the task (DO01).
 */
@Composable
fun FocusScreen(vm: AppViewModel, s: Snapshot) {
    val focus = s.focus
    val task = focus?.let { f -> s.tasks.firstOrNull { it.id == f.taskId } }
    var open by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFF4F6EF))) {
        val wide = maxWidth.value >= WIDE_DP
        Column(
            Modifier.fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = if (wide) 50.dp else 25.dp)
                .padding(bottom = if (wide) 32.dp else 25.dp)
        ) {
            Header(vm, task, wide, focus?.state)
            if (focus == null || task == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(tr("noFocus"), style = TypeScale.body.copy(color = Palette.muted))
                }
                return@Column
            }
            Column(
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = if (wide) 13.dp else 17.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ModeSwitch(open, wide) { open = it }
                Ring(focus, open, wide, rememberTicker(focus.state, vm::now))
                Text(
                    task.title,
                    style =
                        TypeScale.priorityTitle.copy(
                            color = Palette.ink,
                            fontSize = if (wide) 28.sp else 21.sp,
                            lineHeight = if (wide) 50.sp else 37.8.sp,
                        ),
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.widthIn(max = if (wide) 620.dp else 340.dp).clickable {
                            vm.navigator.open(Screen.TaskDetail(task.id))
                        },
                )
                val meta =
                    listOfNotNull(listName(s, task.listId), task.context?.let { tr("context_$it") })
                        .joinToString(" · ")
                if (meta.isNotEmpty())
                    Text(
                        meta,
                        style = TypeScale.tiny.copy(fontSize = if (wide) 13.sp else 11.sp),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                Checklist(
                    vm,
                    s.tasks.filter { it.parentId == task.id }.sortedWith(siblingOrder),
                    wide,
                )
                Controls(vm, focus.state, wide)
            }
            StrayThought(wide) { vm.openCapture(fromFocus = true) }
        }
    }
}

/** The current time, refreshed every second while the timer runs or a break is counting. */
@Composable
private fun rememberTicker(state: FocusState, now: () -> Instant): Instant {
    var current by remember { mutableStateOf(now()) }
    LaunchedEffect(state) {
        while (state == FocusState.RUNNING || state == FocusState.BREAK) {
            current = now()
            delay(TICK_MILLIS)
        }
        current = now()
    }
    return current
}

/** `.focus-head`: back, "مساحة تركيز" with headphones (tablet adds a quiet line), and the menu. */
@Composable
private fun Header(vm: AppViewModel, task: Task?, wide: Boolean, state: FocusState?) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(top = if (wide) 25.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // "Back": drawn left to right and mirrored in Arabic, where the design points it right.
        IconAction(R.drawable.ic_lucide_chevron_left, tr("back"), { vm.navigator.back() })
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            LucideIcon(R.drawable.ic_lucide_headphones, tint = Palette.muted, size = 17.dp)
            Text(
                tr(if (state == FocusState.BREAK) "onBreakCalm" else "focusSpace"),
                style = TypeScale.overline.copy(color = Palette.muted),
            )
        }
        Spacer(Modifier.weight(1f))
        if (wide)
            Text(
                tr("oneStepEnough"),
                style =
                    TypeScale.overline.copy(
                        color = Palette.muted,
                        fontWeight = TypeScale.small.fontWeight,
                    ),
            )
        if (wide) Spacer(Modifier.weight(1f))
        Box {
            IconAction(R.drawable.ic_lucide_ellipsis, tr("focusOptions"), { menu = true })
            DropdownMenu(menu, { menu = false }) {
                if (task != null) {
                    DropdownMenuItem(
                        { Text(tr("addFive"), style = TypeScale.small) },
                        {
                            menu = false
                            vm.focus.extendFiveMinutes()
                        },
                    )
                    if (state == FocusState.RUNNING || state == FocusState.PAUSED)
                        DropdownMenuItem(
                            { Text(tr("takeBreak"), style = TypeScale.small) },
                            {
                                menu = false
                                vm.focus.takeBreak()
                            },
                        )
                    DropdownMenuItem(
                        { Text(tr("planSession"), style = TypeScale.small) },
                        {
                            menu = false
                            vm.scheduling = ScheduleTarget(task.id)
                        },
                    )
                    DropdownMenuItem(
                        { Text(tr("complete"), style = TypeScale.small) },
                        {
                            menu = false
                            vm.tasks.toggleDone(task)
                            vm.navigator.switchTo(Tab.TODAY)
                        },
                    )
                }
            }
        }
    }
}

/** `.segment`: Pomodoro (counts down the planned time) or open (counts up the time worked). */
@Composable
private fun ModeSwitch(open: Boolean, wide: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.padding(top = if (wide) 0.dp else 8.dp, bottom = if (wide) 22.dp else 26.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFFE9EEE1))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false to tr("pomodoro"), true to tr("openTimer")).forEach { (value, label) ->
            val active = value == open
            Box(
                Modifier.heightIn(min = 48.dp)
                    .widthIn(min = if (wide) 125.dp else 105.dp)
                    .then(
                        if (active)
                            Modifier.shadow(
                                2.dp,
                                RoundedCornerShape(12.dp),
                                ambientColor = Color(0x051E3029),
                                spotColor = Color(0x051E3029),
                            )
                        else Modifier
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) Palette.white else Color.Transparent)
                    .clickable { onChange(value) }
                    .padding(horizontal = 20.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style =
                        TypeScale.chip.copy(
                            color = if (active) Palette.green900 else Palette.ink,
                            fontWeight =
                                if (active) TypeScale.button.fontWeight
                                else TypeScale.chip.fontWeight,
                        ),
                )
            }
        }
    }
}

/**
 * `.timer-ring`: 207dp (245dp on tablets) with a soft inner band, a thin track, the progress arc,
 * and the decorative quarter arc of the design; the time in the middle, always left to right.
 */
@Composable
private fun Ring(focus: FocusSession, open: Boolean, wide: Boolean, now: Instant) {
    val size = if (wide) 245.dp else 207.dp
    val worked = focus.worked(now).seconds
    val remaining = focus.remaining(now).seconds
    val shown = if (open) worked else remaining
    val target =
        if (open) 1f
        else (remaining.toFloat() / focus.planned.seconds.coerceAtLeast(1)).coerceIn(0f, 1f)
    // The ring glides between the once-a-second updates (#64).
    val progress by animateFloatAsState(target, motionSpec(1_000), label = "ring")
    Box(
        Modifier.padding(bottom = if (wide) 22.dp else 23.dp).size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val r = this.size.minDimension / 2
            drawCircle(
                Color(0xFFEDF2E6),
                r - 1.dp.toPx() - 4.dp.toPx(),
                style = Stroke(8.dp.toPx()),
            )
            drawCircle(Color(0xFFD8E2CD), r - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
            // The design's decorative arc: top and end borders of an inset circle, turned 33°.
            val inset = 7.dp.toPx() + 1.5.dp.toPx()
            drawArc(
                Palette.green900.copy(alpha = 0.85f),
                startAngle = -135f + 33f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - 2 * inset, this.size.height - 2 * inset),
                style = Stroke(3.dp.toPx()),
            )
            // Progress on the SVG circle r=48 of a 100 viewBox: what is left of the planned time.
            val pr = r * 0.96f
            val tl = Offset(center.x - pr, center.y - pr)
            drawCircle(Color(0xFFDCE5D3), pr, style = Stroke(r * 0.024f))
            drawArc(
                Palette.green900,
                -90f,
                360f * progress,
                false,
                tl,
                Size(pr * 2, pr * 2),
                style = Stroke(r * 0.029f, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    num(clock(shown)),
                    style =
                        TypeScale.timer.copy(
                            fontSize = if (wide) 53.sp else 42.sp,
                            lineHeight = if (wide) 80.sp else 63.sp,
                            letterSpacing = (-1).sp,
                        ),
                )
            }
            Text(
                when {
                    focus.state == FocusState.BREAK -> tr("breakNote")
                    !open && remaining < 0 -> tr("overtime")
                    open -> tr("openCaption")
                    else -> tr("pomodoroCaption", focus.planned.toMinutes())
                },
                style =
                    TypeScale.nav.copy(
                        fontSize = if (wide) 12.sp else 10.sp,
                        fontWeight = TypeScale.small.fontWeight,
                    ),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/** `.focus-checklist`: the task's subtasks, ticked here as in the task itself. */
@Composable
private fun Checklist(vm: AppViewModel, subtasks: List<Task>, wide: Boolean) {
    if (subtasks.isEmpty()) return
    Column(
        Modifier.fillMaxWidth()
            .widthIn(max = if (wide) 430.dp else 350.dp)
            .padding(top = if (wide) 24.dp else 22.dp)
    ) {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE0E7D8)))
        Spacer(Modifier.height(12.dp))
        subtasks.forEach { sub ->
            val done = sub.status == TaskStatus.COMPLETED
            Row(
                Modifier.fillMaxWidth().heightIn(min = if (wide) 49.dp else 46.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(Modifier.size(36.dp, 48.dp), contentAlignment = Alignment.Center) {
                    RoundCheck(done, { vm.tasks.toggleDone(sub) }, sub.title)
                }
                Text(
                    sub.title,
                    style =
                        TypeScale.overline.copy(
                            fontWeight = TypeScale.small.fontWeight,
                            fontSize = if (wide) 14.sp else 12.sp,
                            color = Palette.ink.copy(alpha = if (done) 0.55f else 1f),
                        ),
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                )
            }
        }
    }
}

/** `.focus-controls`: start, pause or resume, and the square end button. */
@Composable
private fun Controls(vm: AppViewModel, state: FocusState, wide: Boolean) {
    Row(
        Modifier.padding(top = if (wide) 20.dp else 22.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val running = state == FocusState.RUNNING
        ActionButton(
            tr(if (running) "pause" else "resume"),
            { if (running) vm.focus.pause() else vm.focus.resume() },
            Modifier.widthIn(min = if (wide) 218.dp else 172.dp),
            icon = if (running) R.drawable.ic_lucide_pause else R.drawable.ic_lucide_play,
        )
        Box(
            Modifier.size(50.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Palette.white)
                .border(1.dp, Palette.line, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            IconAction(
                R.drawable.ic_lucide_square,
                tr("endSession"),
                {
                    vm.focus.end()
                    vm.navigator.back()
                },
            )
        }
    }
}

/** `.brain-dump` and `.focus-footer`: capture a stray thought without leaving focus. */
@Composable
private fun StrayThought(wide: Boolean, onCapture: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.padding(top = 8.dp)
                .heightIn(min = 49.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onCapture)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LucideIcon(R.drawable.ic_lucide_inbox, tint = Palette.muted, size = 17.dp)
            Text(
                tr("strayThought"),
                style =
                    TypeScale.overline.copy(
                        color = Palette.muted,
                        fontWeight = TypeScale.small.fontWeight,
                        fontSize = if (wide) 13.sp else 12.sp,
                    ),
            )
        }
        Text(
            tr("strayThoughtNote"),
            style =
                TypeScale.nav.copy(
                    color = Color(0xFF8C9888),
                    fontWeight = TypeScale.small.fontWeight,
                    fontSize = if (wide) 11.sp else 10.sp,
                ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
