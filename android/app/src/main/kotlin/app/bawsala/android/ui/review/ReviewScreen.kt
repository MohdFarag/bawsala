package app.bawsala.android.ui.review

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Screen
import app.bawsala.android.navigation.Tab
import app.bawsala.android.ui.components.Field
import app.bawsala.android.ui.design.ActionButton
import app.bawsala.android.ui.design.ActionStyle
import app.bawsala.android.ui.design.IconAction
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.design.PageHeader
import app.bawsala.android.ui.design.PanelSheet
import app.bawsala.android.ui.design.Pill
import app.bawsala.android.ui.design.PillStyle
import app.bawsala.android.ui.i18n.num
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel

/** Content width from which Review uses the tablet layout (two columns, T08). */
private const val WIDE_DP = 768

/**
 * V3 weekly review (M06 phone, T08 tablet): the week card with Start, this week's numbers, the way
 * to the planner, and the three steps. The design's AI note is left out (decision 2).
 */
@Composable
fun ReviewScreen(vm: AppViewModel, s: Snapshot) {
    val state = buildReviewState(s, vm.today(), s.prayer.zone)
    var reviewing by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.paper)) {
        val wide = maxWidth.value >= WIDE_DP
        Column(Modifier.fillMaxSize()) {
            PageHeader(tr("review"), tr("reviewSubtitle"), wide) {
                IconAction(
                    R.drawable.ic_lucide_search,
                    tr("search"),
                    { vm.navigator.open(Screen.Search) },
                )
                IconAction(
                    R.drawable.ic_lucide_ellipsis,
                    tr("pageOptions"),
                    { vm.navigator.open(Screen.More) },
                )
            }
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = if (wide) 30.dp else 22.dp,
                        end = if (wide) 30.dp else 22.dp,
                        top = if (wide) 8.dp else 9.dp,
                        bottom = if (wide) 32.dp else 24.dp,
                    )
            ) {
                val main: @Composable ColumnScope.() -> Unit = {
                    WeekCard(state, wide) { reviewing = true }
                    Metrics(state)
                    PlanCard { vm.navigator.open(Screen.Plan) }
                }
                if (wide)
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(
                            Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            content = main,
                        )
                        Column(Modifier.weight(1f)) { Steps(state) }
                    }
                else
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        main()
                        Steps(state)
                    }
            }
        }
        if (reviewing) ReviewSheet(vm, state, wide) { reviewing = false }
    }
}

/** `.review-header`: dark green, the week number, the heading and Start. */
@Composable
private fun WeekCard(state: ReviewState, wide: Boolean, onStart: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Palette.green900)
            .padding(if (wide) 30.dp else 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Pill(
            tr("weekNumber", state.weekNumber),
            style = PillStyle.OnDark,
            icon = R.drawable.ic_lucide_calendar_days,
        )
        Text(
            tr("reviewHeading"),
            style =
                TypeScale.heading.copy(
                    color = Palette.white,
                    fontSize = if (wide) 30.sp else 25.sp,
                ),
        )
        Text(tr("reviewIntro"), style = TypeScale.small.copy(color = Color(0xE6FFFFFF)))
        val r = state.review
        ActionButton(
            tr(
                when {
                    r.completedAt != null -> "openReview"
                    r.stepsDone > 0 -> "continueReview"
                    else -> "startReview"
                }
            ),
            onStart,
            if (wide) Modifier else Modifier.fillMaxWidth(),
            ActionStyle.Accent,
            R.drawable.ic_lucide_arrow_up_right,
        )
    }
}

/** `.review-metrics`: three numbers for this week. */
@Composable
private fun Metrics(state: ReviewState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Metric(state.actionsDone, tr("actionsDone"), Modifier.weight(1f))
        Metric(state.projectsAdvanced, tr("projectsAdvanced"), Modifier.weight(1f))
        Metric(state.focusSessions, tr("focusSessions"), Modifier.weight(1f))
    }
}

@Composable
private fun Metric(value: Int, label: String, modifier: Modifier) {
    Column(
        modifier
            .heightIn(min = 110.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.white)
            .border(1.dp, Palette.line, RoundedCornerShape(20.dp))
            .padding(vertical = 18.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Text(num(value), style = TypeScale.headingLg.copy(fontSize = 30.sp, lineHeight = 40.sp))
        Text(label, style = TypeScale.tiny, maxLines = 1)
    }
}

/** `.review-calendar-cta`: the way to the weekly planner. */
@Composable
private fun PlanCard(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.white)
            .border(1.dp, Palette.line, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(R.drawable.ic_lucide_calendar_days, tint = Palette.green900)
        Column(Modifier.weight(1f)) {
            Text(tr("planYourWeek"), style = TypeScale.button)
            Text(tr("planYourWeekHint"), style = TypeScale.tiny)
        }
        LucideIcon(R.drawable.ic_lucide_chevron_right, tint = Palette.green900, size = 20.dp)
    }
}

/** "ثلاث وقفات بسيطة": the steps, ticked as the review goes, and the week's outcomes. */
@Composable
private fun Steps(state: ReviewState) {
    val done = state.review.stepsDone
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.white)
            .border(1.dp, Palette.line, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("threeSteps"), style = TypeScale.sectionTitle, modifier = Modifier.weight(1f))
            Pill(tr("stepsOf", done), style = PillStyle.Outline)
        }
        listOf("step1", "step2", "step3").forEachIndexed { i, key ->
            StepRow(i + 1, tr(key), tr(key + "Hint"), done > i)
        }
        if (state.review.outcomes.isNotEmpty()) {
            Text(tr("weekDirection"), style = TypeScale.button)
            state.review.outcomes.forEach { Pill(it, icon = R.drawable.ic_lucide_flag) }
        }
    }
}

@Composable
private fun StepRow(n: Int, title: String, hint: String, done: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp)
                .clip(CircleShape)
                .background(if (done) Palette.green900 else Palette.white)
                .border(1.dp, if (done) Palette.green900 else Palette.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) LucideIcon(R.drawable.ic_lucide_check, tint = Palette.white, size = 16.dp)
            else Text(num(n), style = TypeScale.overline)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = TypeScale.button)
            Text(hint, style = TypeScale.tiny)
        }
    }
}

/** The guided review: one sheet per step, resuming where the user stopped this week. */
@Composable
private fun BoxScope.ReviewSheet(
    vm: AppViewModel,
    state: ReviewState,
    wide: Boolean,
    close: () -> Unit,
) {
    var step by remember { mutableStateOf(state.review.stepsDone.coerceAtMost(2)) }
    val outcomes = remember {
        mutableStateListOf(*Array(3) { state.review.outcomes.getOrElse(it) { "" } })
    }
    fun next() {
        vm.review.finishStep(step + 1)
        step++
    }
    val keys = listOf("step1", "step2", "step3")
    PanelSheet(
        tr(keys[step] + "Title"),
        close,
        tr("close"),
        description = tr("stepOfLine", num(step + 1), tr(keys[step])),
        centered = wide,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (step) {
                0 -> {
                    Note(
                        if (state.inboxCount > 0) tr("ideasWaiting", state.inboxCount)
                        else tr("triageClear")
                    )
                    if (state.inboxCount > 0)
                        Wide(tr("sortIdeas"), R.drawable.ic_lucide_inbox, ActionStyle.Primary) {
                            close()
                            vm.navigator.switchTo(Tab.TRIAGE)
                        }
                    Wide(tr("addMissingIdea"), R.drawable.ic_lucide_plus, ActionStyle.Secondary) {
                        vm.openCapture()
                    }
                    Wide(tr("nextStep"), R.drawable.ic_lucide_chevron_right, ActionStyle.Ghost) {
                        next()
                    }
                }
                1 -> {
                    Note(tr("commitmentsNote", state.activeProjects, state.waitingCount))
                    if (state.projectsWithoutNext.isNotEmpty()) {
                        Text(tr("noNextAction"), style = TypeScale.overline)
                        state.projectsWithoutNext.forEach {
                            Pill(it, style = PillStyle.Amber, icon = R.drawable.ic_lucide_folder)
                        }
                    }
                    Wide(tr("openProjects"), R.drawable.ic_lucide_layers, ActionStyle.Secondary) {
                        close()
                        vm.navigator.switchTo(Tab.PROJECTS)
                    }
                    Wide(tr("nextStep"), R.drawable.ic_lucide_chevron_right, ActionStyle.Primary) {
                        next()
                    }
                }
                else -> {
                    outcomes.indices.forEach { i ->
                        Field(outcomes[i], tr("outcomeN", num(i + 1))) { outcomes[i] = it }
                    }
                    Wide(tr("finishReview"), R.drawable.ic_lucide_check, ActionStyle.Primary) {
                        vm.review.complete(outcomes.toList())
                        close()
                    }
                    Wide(tr("openPlanner"), R.drawable.ic_lucide_calendar_days, ActionStyle.Ghost) {
                        vm.review.complete(outcomes.toList())
                        close()
                        vm.navigator.open(Screen.Plan)
                    }
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = TypeScale.small,
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(Palette.sage100)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

@Composable
private fun Wide(label: String, @DrawableRes icon: Int, style: ActionStyle, onClick: () -> Unit) {
    ActionButton(label, onClick, Modifier.fillMaxWidth(), style, icon)
}
