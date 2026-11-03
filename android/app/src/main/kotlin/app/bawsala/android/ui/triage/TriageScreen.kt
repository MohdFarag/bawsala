package app.bawsala.android.ui.triage

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Screen
import app.bawsala.android.navigation.Tab
import app.bawsala.android.ui.design.ActionButton
import app.bawsala.android.ui.design.ActionList
import app.bawsala.android.ui.design.ActionStyle
import app.bawsala.android.ui.design.IconAction
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.design.OptionRow
import app.bawsala.android.ui.design.PageHeader
import app.bawsala.android.ui.design.PanelSheet
import app.bawsala.android.ui.design.Pill
import app.bawsala.android.ui.design.PillStyle
import app.bawsala.android.ui.design.ProgressLine
import app.bawsala.android.ui.design.TextAction
import app.bawsala.android.ui.format.itemTitle
import app.bawsala.android.ui.format.relativeTime
import app.bawsala.android.ui.format.sourceLabel
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind
import app.bawsala.core.model.CaptureState
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.launch

/** Content width from which Triage uses the tablet layout (prototype container query, 768px). */
private const val WIDE_DP = 768

/** Panels that open over the card. */
private enum class Sheet {
    NONE,
    MORE,
    WAITING,
    LATER_WITH_DATE,
    SPLIT,
    DELETE,
}

/**
 * V3 Triage (M04 phone, T03 tablet): one idea at a time. The four main decisions are buttons and
 * swipes (toward the end for a next action, toward the start for a reference); Do Now, waiting,
 * split and a review date live under "More options". When [opened] from elsewhere (search, a task's
 * source) it shows [itemId] with a close button; the Triage tab shows the idea it last moved to.
 */
@Composable
fun TriageScreen(
    vm: AppViewModel,
    s: Snapshot,
    itemId: String? = null,
    opened: Boolean = false,
) {
    val state = buildTriageState(s.items, itemId ?: vm.inbox.focusId, vm.inbox.sortedCount)
    val item = state.item
    var sheet by rememberSaveable(item?.id) { mutableStateOf(Sheet.NONE) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.paper)) {
        val wide = maxWidth.value >= WIDE_DP
        Column(Modifier.fillMaxSize()) {
            PageHeader(
                tr("triage"),
                tr(if (item == null) "triageEmptySubtitle" else "triageSubtitle"),
                wide,
            ) {
                if (opened) IconAction(R.drawable.ic_lucide_x, tr("close"), { vm.navigator.back() })
                IconAction(
                    R.drawable.ic_lucide_search,
                    tr("search"),
                    { vm.navigator.open(Screen.Search) },
                )
                IconAction(R.drawable.ic_lucide_ellipsis, tr("pageOptions"), { sheet = Sheet.MORE })
            }
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = if (wide) 30.dp else 22.dp,
                        end = if (wide) 30.dp else 22.dp,
                        top = if (wide) 12.dp else 3.dp,
                        bottom = if (wide) 50.dp else 24.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (wide) 22.dp else 11.dp),
            ) {
                if (item == null) AllSorted(vm)
                else Sorting(vm, s, state, item, wide) { sheet = it }
            }
        }
        Sheets(vm, item, sheet, wide) { sheet = it }
    }
}

@Composable
private fun Sorting(
    vm: AppViewModel,
    s: Snapshot,
    state: TriageState,
    item: CaptureItem,
    wide: Boolean,
    openSheet: (Sheet) -> Unit,
) {
    val maxWidth = Modifier.widthIn(max = if (wide) 570.dp else 530.dp).fillMaxWidth()
    Progress(state, maxWidth)
    val swipeable = item.state == CaptureState.INBOX && item.sortsWhole()
    SwipeCard(
        item.id,
        enabled = swipeable,
        onTowardEnd = { vm.inbox.toNextAction(item) },
        onTowardStart = { vm.inbox.toReference(item) },
        modifier = maxWidth.padding(top = 7.dp),
    ) {
        IdeaCard(item, s.lang, wide)
    }
    when {
        item.state == CaptureState.DOING_NOW -> Column(maxWidth) { DoingNow(vm, item) }
        !item.sortsWhole() -> Column(maxWidth) { EntriesPanel(vm, item) }
        else -> {
            SwipeHints(Modifier.widthIn(max = if (wide) 550.dp else 510.dp).fillMaxWidth())
            Text(
                tr("triageQuestion"),
                style =
                    TypeScale.sectionTitle.copy(
                        fontSize = if (wide) 18.sp else 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                textAlign = TextAlign.Center,
            )
            MainDecisions(vm, item, wide, maxWidth)
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Always shown, as in M04; it does nothing until there is a decision to undo.
        TextAction(
            tr("undo"),
            { vm.inbox.undoLast() },
            icon = R.drawable.ic_lucide_rotate_ccw,
            color = if (vm.inbox.canUndo) Palette.green900 else Palette.muted,
        )
        TextAction(tr("deleteIdea"), { openSheet(Sheet.DELETE) }, color = Palette.muted)
    }
}

/** `.triage-progress`: ideas left and the position, over a thin progress line. */
@Composable
private fun Progress(state: TriageState, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(tr("ideasLeft", state.remaining), style = TypeScale.overline.muted())
            Box(Modifier.weight(1f))
            Text(
                tr("ideaOf", state.position, state.remaining),
                style = TypeScale.overline.muted(),
            )
        }
        ProgressLine(state.progress)
    }
}

private fun androidx.compose.ui.text.TextStyle.muted() =
    copy(color = Palette.muted, fontWeight = FontWeight.Normal)

/**
 * `.triage-stack`: the card over two paler cards, draggable sideways. Dragged past 110dp it leaves
 * the screen and [onTowardEnd] or [onTowardStart] runs; otherwise it springs back.
 */
@Composable
private fun SwipeCard(
    key: String,
    enabled: Boolean,
    onTowardEnd: () -> Unit,
    onTowardStart: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offset = remember(key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val threshold = with(LocalDensity.current) { 110.dp.toPx() }
    Box(modifier) {
        // The two cards behind (`.triage-stack:before` and `:after`).
        StackLayer(
            Color(0xFFF3F4EE),
            Modifier.matchParentSize().padding(start = 17.dp, end = 17.dp, top = 20.dp),
        )
        StackLayer(
            Color(0xFFEDF0E6),
            Modifier.matchParentSize()
                .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
        )
        Box(
            Modifier.padding(bottom = 18.dp)
                .graphicsLayer {
                    translationX = offset.value
                    rotationZ = offset.value / 45f
                }
                .draggable(
                    rememberDraggableState { delta ->
                        scope.launch { offset.snapTo(offset.value + delta) }
                    },
                    Orientation.Horizontal,
                    enabled = enabled,
                    onDragStopped = {
                        val moved = offset.value
                        if (abs(moved) < threshold) offset.animateTo(0f)
                        else {
                            offset.animateTo(sign(moved) * threshold * 5)
                            val towardEnd = if (rtl) moved < 0 else moved > 0
                            if (towardEnd) onTowardEnd() else onTowardStart()
                        }
                    },
                )
        ) {
            content()
        }
    }
}

@Composable
private fun StackLayer(color: Color, modifier: Modifier) {
    Box(
        modifier
            .border(1.dp, Color(0xFFDDE3D5), RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .background(color)
    )
}

/** `.triage-card`: where the idea came from and when, the text, and the details noticed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdeaCard(item: CaptureItem, lang: String, wide: Boolean) {
    val shape = RoundedCornerShape(25.dp)
    Column(
        Modifier.fillMaxWidth()
            .heightIn(min = if (wide) 260.dp else 210.dp)
            .shadow(28.dp, shape, ambientColor = Color(0x05243D2E), spotColor = Color(0x05243D2E))
            .clip(shape)
            .background(Palette.white)
            .border(1.dp, Palette.line, shape)
            .padding(
                horizontal = if (wide) 32.dp else 20.dp,
                vertical = if (wide) 32.dp else 19.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(
                    if (item.isVoice()) R.drawable.ic_lucide_mic else R.drawable.ic_lucide_inbox,
                    tint = Palette.muted,
                    size = 16.dp,
                )
                Text(
                    sourceLabel(item.source.service)?.let { tr("sharedFrom", it) }
                        ?: tr(if (item.isVoice()) "voiceNote" else "quickCapture"),
                    style = TypeScale.tiny,
                )
            }
            Pill(relativeTime(item.capturedAt, lang), style = PillStyle.Outline)
        }
        Text(
            itemTitle(item, lang),
            style =
                TypeScale.heading.copy(
                    fontSize = if (wide) 25.sp else 20.sp,
                    lineHeight = if (wide) 47.sp else 35.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
        if (item.body.isNotBlank())
            Text(item.body, style = TypeScale.body.copy(color = Palette.muted), maxLines = 8)
        if (item.kind == CaptureKind.LIST)
            item.entries
                .filter { it.resolvedAs == null }
                .forEach { Text("• ${it.text}", style = TypeScale.body) }
        ItemExtras(item, lang)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item.tags.forEach { Pill(it) }
            Pill(tr("stepNotSet"), style = PillStyle.Outline)
        }
    }
}

/** `.triage-hints`: which way to swipe, at the start and end edges. */
@Composable
private fun SwipeHints(modifier: Modifier) {
    Row(modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // arrow_left is mirrored in Arabic, so it always points to the start edge.
            LucideIcon(R.drawable.ic_lucide_arrow_left, tint = Palette.muted, size = 15.dp)
            Text(tr("swipeReference"), style = TypeScale.tiny.copy(fontSize = 10.sp))
        }
        Box(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tr("swipeAction"), style = TypeScale.tiny.copy(fontSize = 10.sp))
            LucideIcon(R.drawable.ic_lucide_arrow_right, tint = Palette.muted, size = 15.dp)
        }
    }
}

/** `.triage-actions`: two columns on phones, four in a row on tablets. */
@Composable
private fun MainDecisions(vm: AppViewModel, item: CaptureItem, wide: Boolean, modifier: Modifier) {
    val buttons: List<@Composable (Modifier) -> Unit> =
        listOf(
            {
                ActionButton(
                    tr("nextAction"),
                    { vm.inbox.toNextAction(item) },
                    it,
                    icon = R.drawable.ic_lucide_arrow_up_right,
                )
            },
            {
                ActionButton(
                    tr("project"),
                    { vm.inbox.toProject(item) },
                    it,
                    ActionStyle.Secondary,
                    R.drawable.ic_lucide_folder,
                )
            },
            {
                ActionButton(
                    tr("reference"),
                    { vm.inbox.toReference(item) },
                    it,
                    ActionStyle.Secondary,
                    R.drawable.ic_lucide_book_open,
                )
            },
            {
                ActionButton(
                    tr("later"),
                    { vm.inbox.toLater(item) },
                    it,
                    ActionStyle.Secondary,
                    R.drawable.ic_lucide_clock_3,
                )
            },
        )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        buttons.chunked(if (wide) 4 else 2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { it(Modifier.weight(1f)) }
            }
        }
    }
}

/** `.empty-state`: every idea has a place. */
@Composable
private fun AllSorted(vm: AppViewModel) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            Modifier.size(70.dp).clip(RoundedCornerShape(24.dp)).background(Palette.sage100),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(R.drawable.ic_lucide_check_check, tint = Palette.green900, size = 32.dp)
        }
        Text(
            tr("triageDoneTitle"),
            style = TypeScale.sectionTitle.copy(fontSize = 20.sp, lineHeight = 36.sp),
            textAlign = TextAlign.Center,
        )
        Text(
            tr("triageDoneHint"),
            style = TypeScale.overline.muted(),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp),
        )
        ActionButton(
            tr("backToToday"),
            { vm.navigator.switchTo(Tab.TODAY) },
            icon = R.drawable.ic_lucide_sun,
        )
        ActionButton(
            tr("captureNewIdea"),
            { vm.openCapture() },
            style = ActionStyle.Ghost,
            icon = R.drawable.ic_lucide_plus,
        )
    }
}

/** The sheets over the card: more options and their panels, and the delete confirmation. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.Sheets(
    vm: AppViewModel,
    item: CaptureItem?,
    sheet: Sheet,
    wide: Boolean,
    open: (Sheet) -> Unit,
) {
    val close = { open(Sheet.NONE) }
    fun then(action: () -> Unit): () -> Unit = {
        close()
        action()
    }
    when (sheet) {
        Sheet.NONE -> Unit
        Sheet.MORE ->
            PanelSheet(
                tr("pageOptions"),
                close,
                tr("close"),
                description = item?.let { tr("moreOptionsHint") },
                centered = wide,
            ) {
                ActionList(Modifier.border(1.dp, Palette.line, RoundedCornerShape(16.dp))) {
                    OptionRow(
                        tr("allIdeas"),
                        R.drawable.ic_lucide_list_filter,
                        then { vm.navigator.open(Screen.InboxList) },
                        divider = item != null,
                    )
                    if (item == null) return@ActionList
                    if (item.state == CaptureState.INBOX)
                        OptionRow(
                            tr("doNow"),
                            R.drawable.ic_lucide_zap,
                            then { vm.inbox.startDoNow(item) },
                        )
                    OptionRow(
                        tr("waitingFor"),
                        R.drawable.ic_lucide_clock_3,
                        { open(Sheet.WAITING) },
                    )
                    OptionRow(
                        tr("laterWithDate"),
                        R.drawable.ic_lucide_calendar_days,
                        { open(Sheet.LATER_WITH_DATE) },
                    )
                    OptionRow(
                        tr("split"),
                        R.drawable.ic_lucide_layout_grid,
                        { open(Sheet.SPLIT) },
                    )
                    if (item.kind == CaptureKind.TEXT)
                        OptionRow(
                            tr("convertToList"),
                            R.drawable.ic_lucide_list_checks,
                            then { vm.inbox.convertToList(item) },
                            divider = false,
                        )
                }
            }
        Sheet.WAITING ->
            if (item != null)
                PanelSheet(tr("waitingFor"), close, tr("close"), centered = wide) {
                    WaitingPanel(vm) { waiting -> then { vm.inbox.toWaiting(item, waiting) }() }
                }
        Sheet.LATER_WITH_DATE ->
            if (item != null)
                PanelSheet(tr("laterWithDate"), close, tr("close"), centered = wide) {
                    SomedayPanel(vm) { reviewOn -> then { vm.inbox.toSomeday(item, reviewOn) }() }
                }
        Sheet.SPLIT ->
            if (item != null)
                PanelSheet(tr("split"), close, tr("close"), centered = wide) {
                    SplitPanel(item) { parts -> then { vm.inbox.split(item, parts) }() }
                }
        Sheet.DELETE ->
            if (item != null)
                PanelSheet(
                    tr("deleteIdeaTitle"),
                    close,
                    tr("keepIt"),
                    description = tr("deleteIdeaHint"),
                    centered = wide,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton(
                            tr("deleteIdeaConfirm"),
                            then { vm.inbox.deleteIdea(item) },
                            style = ActionStyle.Danger,
                            icon = R.drawable.ic_lucide_x,
                        )
                        ActionButton(tr("keepIt"), close, style = ActionStyle.Secondary)
                    }
                }
    }
}
