package app.bawsala.android.ui.projects

import androidx.annotation.DrawableRes
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Screen
import app.bawsala.android.ui.design.IconAction
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.design.PageHeader
import app.bawsala.android.ui.design.Pill
import app.bawsala.android.ui.design.PillStyle
import app.bawsala.android.ui.design.ProgressLine
import app.bawsala.android.ui.design.TextAction
import app.bawsala.android.ui.format.itemTitle
import app.bawsala.android.ui.format.relativeTime
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.ui.today.priorityOrder
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind
import app.bawsala.core.model.ProjectStatus

/** Content width from which Projects uses the tablet layout (prototype container query). */
private const val WIDE_DP = 768

/** Content width from which project cards sit three across (T07 at full width). */
private const val WIDEST_DP = 1100

/** The PARA tabs in reading order, with their icons. */
internal enum class Para(val key: String, @DrawableRes val icon: Int) {
    PROJECTS("paraProjects", R.drawable.ic_lucide_folder),
    AREAS("paraAreas", R.drawable.ic_lucide_layers),
    RESOURCES("paraResources", R.drawable.ic_lucide_book_open),
    ARCHIVE("paraArchive", R.drawable.ic_lucide_archive),
}

/**
 * V3 Projects (M05 phone, T07 tablet), organized with PARA: active projects as cards with their
 * progress and next action; areas; resources (ideas kept as a reference); and the archive. Tasks,
 * collections, lists and trash stay one tap away under page options.
 */
@Composable
fun ProjectsScreen(vm: AppViewModel, s: Snapshot) {
    val state = buildProjectsState(s)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf<ProjectsSheet?>(null) }
    val para = Para.entries[tab]

    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.paper)) {
        val wide = maxWidth.value >= WIDE_DP
        val widest = maxWidth.value >= WIDEST_DP
        Column(Modifier.fillMaxSize()) {
            PageHeader(tr("projects"), tr("projectsSubtitle"), wide) {
                IconAction(
                    R.drawable.ic_lucide_search,
                    tr("search"),
                    { vm.navigator.open(Screen.Search) },
                )
                IconAction(
                    R.drawable.ic_lucide_ellipsis,
                    tr("pageOptions"),
                    { sheet = ProjectsSheet.Options },
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
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        tr("projectsHeading"),
                        style =
                            TypeScale.headingLg.copy(
                                fontSize = if (wide) 31.sp else 28.sp,
                                lineHeight = if (wide) 50.sp else 45.sp,
                            ),
                        modifier = Modifier.weight(1f),
                    )
                    IconAction(
                        R.drawable.ic_lucide_plus,
                        tr(if (para == Para.AREAS) "newArea" else "newProject"),
                        {
                            sheet =
                                if (para == Para.AREAS) ProjectsSheet.NewArea
                                else ProjectsSheet.NewProject(null)
                        },
                        soft = true,
                    )
                }
                ParaTabs(tab, wide) { tab = it }
                when (para) {
                    Para.PROJECTS -> ProjectsTab(state, wide, widest) { sheet = it }
                    Para.AREAS -> AreasTab(state, wide, widest) { sheet = it }
                    Para.RESOURCES -> ResourcesTab(state, s.lang, wide, widest) { sheet = it }
                    Para.ARCHIVE -> ArchiveTab(state, wide, widest) { sheet = it }
                }
            }
        }
        sheet?.let { ProjectSheets(vm, s, state, it, wide) { next -> sheet = next } }
    }
}

/** `.para-tabs`: four equal tabs on phones; content-sized tabs on tablets. */
@Composable
private fun ParaTabs(selected: Int, wide: Boolean, onSelect: (Int) -> Unit) {
    Row(
        Modifier.then(if (wide) Modifier else Modifier.fillMaxWidth())
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.white)
            .border(1.dp, Palette.line, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(if (wide) 5.dp else 6.dp),
    ) {
        Para.entries.forEachIndexed { i, para ->
            val active = i == selected
            val color = if (active) Palette.white else Palette.ink
            Row(
                Modifier.then(if (wide) Modifier.widthIn(min = 100.dp) else Modifier.weight(1f))
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) Palette.green900 else Palette.white)
                    .clickable(role = Role.Tab) { onSelect(i) }
                    .padding(horizontal = if (wide) 22.dp else 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(para.icon, tint = color, size = 15.dp)
                Text(
                    tr(para.key),
                    style = TypeScale.tiny.copy(fontSize = if (wide) 12.sp else 11.sp),
                    color = color,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ProjectsTab(
    state: ProjectsState,
    wide: Boolean,
    widest: Boolean,
    open: (ProjectsSheet) -> Unit,
) {
    // "الأولوية": projects whose next action is most urgent first; projects with none go last.
    var byPriority by rememberSaveable { mutableStateOf(false) }
    val projects =
        if (byPriority) state.active.sortedWith(compareBy(nullsLast(priorityOrder)) { it.next })
        else state.active
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tr("activeProjects", state.active.size),
            style = TypeScale.overline.mutedNormal(),
            modifier = Modifier.weight(1f),
        )
        TextAction(
            tr("byPriority"),
            { byPriority = !byPriority },
            icon = R.drawable.ic_lucide_sliders_horizontal,
            color = if (byPriority) Palette.green900 else Palette.muted,
        )
    }
    if (state.active.isEmpty()) Empty(tr("noProjectsYet"), R.drawable.ic_lucide_folder)
    Grid(
        projects,
        columns = if (widest) 3 else if (wide) 2 else 1,
        gap = if (wide) 18.dp else 12.dp,
        Modifier.padding(top = 19.dp),
    ) { i, card ->
        ProjectCardView(card, i, wide) { open(ProjectsSheet.ProjectDetail(card.project.id)) }
    }
}

@Composable
private fun AreasTab(
    state: ProjectsState,
    wide: Boolean,
    widest: Boolean,
    open: (ProjectsSheet) -> Unit,
) {
    if (state.areas.isEmpty()) Empty(tr("noAreasYet"), R.drawable.ic_lucide_layers)
    Grid(
        state.areas,
        columns = if (widest) 4 else if (wide) 3 else 2,
        gap = 12.dp,
        Modifier.padding(top = 20.dp),
    ) { _, area ->
        Tile(
            areaIcon(area.area.icon),
            area.area.title,
            tr("areaMeta", area.projects.size, area.openActions),
        ) {
            open(ProjectsSheet.AreaDetail(area.area.id))
        }
    }
}

@Composable
private fun ResourcesTab(
    state: ProjectsState,
    lang: String,
    wide: Boolean,
    widest: Boolean,
    open: (ProjectsSheet) -> Unit,
) {
    if (state.resources.isEmpty()) Empty(tr("noResourcesYet"), R.drawable.ic_lucide_book_open)
    Grid(
        state.resources,
        columns = if (widest) 4 else if (wide) 3 else 2,
        gap = 12.dp,
        Modifier.padding(top = 20.dp),
    ) { _, item ->
        Tile(resourceIcon(item), itemTitle(item, lang), relativeTime(item.capturedAt, lang)) {
            open(ProjectsSheet.Resource(item.id))
        }
    }
}

@Composable
private fun ArchiveTab(
    state: ProjectsState,
    wide: Boolean,
    widest: Boolean,
    open: (ProjectsSheet) -> Unit,
) {
    if (state.archive.isEmpty()) Empty(tr("noArchiveYet"), R.drawable.ic_lucide_archive)
    Grid(
        state.archive,
        columns = if (widest) 4 else if (wide) 3 else 2,
        gap = 12.dp,
        Modifier.padding(top = 20.dp),
    ) { _, card ->
        Tile(
            if (card.project.status == ProjectStatus.COMPLETED) R.drawable.ic_lucide_check_check
            else R.drawable.ic_lucide_archive,
            card.project.title,
            tr(statusKey(card.project.status)),
        ) {
            open(ProjectsSheet.ProjectDetail(card.project.id))
        }
    }
}

/** `.project-card`: icon and status, title and area, progress, and the next action. */
@Composable
private fun ProjectCardView(card: ProjectCard, index: Int, wide: Boolean, onClick: () -> Unit) {
    val lilac = index % 2 == 1
    Column(
        Modifier.fillMaxWidth()
            .heightIn(min = if (wide) 211.dp else 165.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.white)
            .border(1.dp, Palette.line, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(if (wide) 23.dp else 19.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (lilac) Palette.lilac else Palette.sage100),
                contentAlignment = Alignment.Center,
            ) {
                LucideIcon(
                    projectIcon(card.project.icon),
                    tint = if (lilac) Palette.lilacInk else Palette.green900,
                    size = 18.dp,
                )
            }
            Spacer(Modifier.weight(1f))
            StatusPill(card.project.status)
        }
        Column {
            Text(
                card.project.title,
                style =
                    TypeScale.sectionTitle.copy(
                        fontSize = if (wide) 19.sp else 16.sp,
                        lineHeight = if (wide) 34.sp else 29.sp,
                    ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(card.areaTitle ?: tr("newProject"), style = TypeScale.overline.mutedNormal())
        }
        val metaStyle = TypeScale.tiny.copy(fontSize = if (wide) 12.sp else 11.sp)
        Row(Modifier.fillMaxWidth()) {
            Text(
                if (card.total == 0) tr("noActionsYet") else tr("actionsOf", card.done, card.total),
                style = metaStyle,
            )
            Spacer(Modifier.weight(1f))
            Text(tr("percent", (card.progress * 100).toInt()), style = metaStyle)
        }
        ProgressLine(card.progress)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("nextIs", card.next?.title ?: tr("setNextAction")),
                style = metaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            LucideIcon(R.drawable.ic_lucide_chevron_right, tint = Palette.muted, size = 17.dp)
        }
    }
}

@Composable
internal fun StatusPill(status: ProjectStatus) {
    Pill(
        tr(statusKey(status)),
        style = if (status == ProjectStatus.ON_HOLD) PillStyle.Lilac else PillStyle.Outline,
    )
}

internal fun statusKey(status: ProjectStatus) =
    when (status) {
        ProjectStatus.ACTIVE -> "statusActive"
        ProjectStatus.ON_HOLD -> "statusOnHold"
        ProjectStatus.COMPLETED -> "statusCompleted"
        ProjectStatus.ARCHIVED -> "statusArchived"
    }

/** `.area-card`: an icon, a bold title and a muted line. */
@Composable
private fun Tile(@DrawableRes icon: Int, title: String, line: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .heightIn(min = 145.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.white)
            .border(1.dp, Palette.line, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LucideIcon(icon, tint = Palette.green900)
        Text(
            title,
            style = TypeScale.button.copy(fontWeight = TypeScale.sectionTitle.fontWeight),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(line, style = TypeScale.tiny, maxLines = 2)
    }
}

@Composable
private fun Empty(text: String, @DrawableRes icon: Int) {
    Row(
        Modifier.fillMaxWidth()
            .padding(top = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.sage100)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(icon, tint = Palette.green900, size = 20.dp)
        Text(text, style = TypeScale.small.copy(color = Palette.muted))
    }
}

/** Equal-width columns; the last row keeps its cells the same width. */
@Composable
private fun <T> Grid(
    items: List<T>,
    columns: Int,
    gap: Dp,
    modifier: Modifier = Modifier,
    cell: @Composable (index: Int, item: T) -> Unit,
) {
    if (items.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        items.withIndex().chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { (i, item) -> Box(Modifier.weight(1f)) { cell(i, item) } }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun androidx.compose.ui.text.TextStyle.mutedNormal() =
    copy(color = Palette.muted, fontWeight = TypeScale.small.fontWeight)

/** The Lucide icon for a project's stored icon name; a folder when unknown. */
@DrawableRes
internal fun projectIcon(name: String): Int =
    when (name) {
        "layers" -> R.drawable.ic_lucide_layers
        "layout-grid" -> R.drawable.ic_lucide_layout_grid
        "book-open" -> R.drawable.ic_lucide_book_open
        "sun" -> R.drawable.ic_lucide_sun
        "chart-no-axes-combined" -> R.drawable.ic_lucide_chart_no_axes_combined
        "house" -> R.drawable.ic_lucide_house
        "laptop" -> R.drawable.ic_lucide_laptop
        else -> R.drawable.ic_lucide_folder
    }

@DrawableRes
internal fun areaIcon(name: String): Int =
    projectIcon(name).let {
        if (it == R.drawable.ic_lucide_folder) R.drawable.ic_lucide_layers else it
    }

@DrawableRes
private fun resourceIcon(item: CaptureItem): Int =
    when (item.kind) {
        CaptureKind.LINK -> R.drawable.ic_lucide_arrow_up_right
        CaptureKind.AUDIO -> R.drawable.ic_lucide_mic
        CaptureKind.LIST -> R.drawable.ic_lucide_list_checks
        CaptureKind.IMAGE,
        CaptureKind.VIDEO -> R.drawable.ic_lucide_layout_grid
        else -> R.drawable.ic_lucide_book_open
    }
