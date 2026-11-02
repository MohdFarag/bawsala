package app.bawsala.android.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Screen
import app.bawsala.android.ui.components.Field
import app.bawsala.android.ui.design.ActionButton
import app.bawsala.android.ui.design.ActionList
import app.bawsala.android.ui.design.ActionRow
import app.bawsala.android.ui.design.ActionStyle
import app.bawsala.android.ui.design.ChoiceChip
import app.bawsala.android.ui.design.IconAction
import app.bawsala.android.ui.design.OptionRow
import app.bawsala.android.ui.design.PanelSheet
import app.bawsala.android.ui.design.ProgressLine
import app.bawsala.android.ui.format.itemTitle
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.ui.triage.ItemExtras
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.core.model.ProjectStatus
import app.bawsala.core.model.TaskStatus

/** What opens over the Projects screen. */
internal sealed interface ProjectsSheet {
    data object Options : ProjectsSheet

    data class NewProject(val areaId: String?) : ProjectsSheet

    data object NewArea : ProjectsSheet

    data class ProjectDetail(val id: String) : ProjectsSheet

    data class AreaDetail(val id: String) : ProjectsSheet

    data class Resource(val id: String) : ProjectsSheet
}

@Composable
internal fun BoxScope.ProjectSheets(
    vm: AppViewModel,
    s: Snapshot,
    state: ProjectsState,
    sheet: ProjectsSheet,
    wide: Boolean,
    open: (ProjectsSheet?) -> Unit,
) {
    val close = { open(null) }
    when (sheet) {
        ProjectsSheet.Options ->
            PanelSheet(tr("pageOptions"), close, tr("close"), centered = wide) {
                ActionList(Modifier.border(1.dp, Palette.line, RoundedCornerShape(16.dp))) {
                    fun go(screen: Screen): () -> Unit = {
                        close()
                        vm.navigator.open(screen)
                    }
                    OptionRow(tr("allTasks"), R.drawable.ic_lucide_list_checks, go(Screen.Tasks))
                    OptionRow(
                        tr("collections"),
                        R.drawable.ic_lucide_book_open,
                        go(Screen.Collections),
                    )
                    OptionRow(tr("lists"), R.drawable.ic_lucide_layers, go(Screen.Lists))
                    OptionRow(
                        tr("trash"),
                        R.drawable.ic_lucide_trash,
                        go(Screen.Trash),
                        divider = false,
                    )
                }
            }
        is ProjectsSheet.NewProject -> NewProjectSheet(vm, s, sheet.areaId, wide, close)
        ProjectsSheet.NewArea -> NewAreaSheet(vm, wide, close)
        is ProjectsSheet.ProjectDetail -> {
            val card =
                (state.active + state.archive).firstOrNull { it.project.id == sheet.id } ?: return
            ProjectDetailSheet(vm, s, card, wide, close)
        }
        is ProjectsSheet.AreaDetail -> {
            val area = state.areas.firstOrNull { it.area.id == sheet.id } ?: return
            AreaDetailSheet(vm, area, wide, open)
        }
        is ProjectsSheet.Resource -> {
            val item = s.items.firstOrNull { it.id == sheet.id } ?: return
            PanelSheet(itemTitle(item, s.lang), close, tr("close"), centered = wide) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (item.body.isNotBlank())
                        Text(item.body, style = TypeScale.body.copy(color = Palette.muted))
                    ItemExtras(item, s.lang)
                    ActionButton(
                        tr("backToTriage"),
                        {
                            close()
                            vm.inbox.returnToInbox(item)
                        },
                        Modifier.fillMaxWidth(),
                        ActionStyle.Secondary,
                        R.drawable.ic_lucide_inbox,
                    )
                }
            }
        }
    }
}

/** M05 "new project": the outcome in the user's words, and optionally an area. */
@Composable
private fun BoxScope.NewProjectSheet(
    vm: AppViewModel,
    s: Snapshot,
    presetArea: String?,
    wide: Boolean,
    close: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var areaId by remember { mutableStateOf(presetArea) }
    PanelSheet(
        tr("newProjectTitle"),
        close,
        tr("close"),
        description = tr("newProjectHint"),
        centered = wide,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Field(title, tr("newProjectPlaceholder"), minHeight = 96) { title = it }
            AreaChoice(s, areaId) { areaId = it }
            ActionButton(
                tr("createProject"),
                {
                    vm.projects.create(title, areaId)
                    close()
                },
                Modifier.fillMaxWidth(),
                icon = R.drawable.ic_lucide_folder,
                enabled = title.isNotBlank(),
            )
        }
    }
}

@Composable
private fun BoxScope.NewAreaSheet(vm: AppViewModel, wide: Boolean, close: () -> Unit) {
    var title by remember { mutableStateOf("") }
    PanelSheet(
        tr("newArea"),
        close,
        tr("close"),
        description = tr("newAreaHint"),
        centered = wide,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Field(title, tr("areaPlaceholder")) { title = it }
            ActionButton(
                tr("createArea"),
                {
                    vm.projects.createArea(title)
                    close()
                },
                Modifier.fillMaxWidth(),
                icon = R.drawable.ic_lucide_layers,
                enabled = title.isNotBlank(),
            )
        }
    }
}

/** No area, or one of the user's areas. Nothing shows until there is at least one area. */
@Composable
private fun AreaChoice(s: Snapshot, selected: String?, onSelect: (String?) -> Unit) {
    val areas = s.areas.filterNot { it.archived }
    if (areas.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(tr("area"), style = TypeScale.overline)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChoiceChip(tr("noArea"), selected == null, { onSelect(null) })
            areas.forEach { a -> ChoiceChip(a.title, selected == a.id, { onSelect(a.id) }) }
        }
    }
}

/**
 * M05 project detail: area and outcome, progress, its actions (tick to complete, tap to open), a
 * field to add one, the next step, and the project's status.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.ProjectDetailSheet(
    vm: AppViewModel,
    s: Snapshot,
    card: ProjectCard,
    wide: Boolean,
    close: () -> Unit,
) {
    val p = card.project
    var step by remember(p.id) { mutableStateOf("") }
    var confirmDelete by remember(p.id) { mutableStateOf(false) }
    val description =
        listOfNotNull(
                card.areaTitle,
                p.outcome.takeIf { it.isNotBlank() }?.let { tr("outcomeIs", it) },
            )
            .joinToString(" · ")
            .ifEmpty { null }
    PanelSheet(p.title, close, tr("close"), description = description, centered = wide) {
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            ProgressLine(card.progress)
            Text(tr("nextActions"), style = TypeScale.button)
            if (card.tasks.isNotEmpty())
                ActionList(Modifier.border(1.dp, Palette.line, RoundedCornerShape(16.dp))) {
                    card.tasks.forEachIndexed { i, t ->
                        ActionRow(
                            t.title,
                            meta = emptyList(),
                            done = t.status == TaskStatus.COMPLETED,
                            onToggle = { vm.tasks.toggleDone(t) },
                            onOpen = {
                                close()
                                vm.navigator.open(Screen.TaskDetail(t.id))
                            },
                            divider = i < card.tasks.lastIndex,
                        )
                    }
                }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) { Field(step, tr("addAction")) { step = it } }
                IconAction(
                    R.drawable.ic_lucide_plus,
                    tr("addAction"),
                    {
                        if (step.isNotBlank()) {
                            vm.projects.addStep(p.id, step)
                            step = ""
                        }
                    },
                    soft = true,
                )
            }
            Text(
                tr("nextStepNote", card.next?.title ?: tr("setNextAction")),
                style = TypeScale.small,
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(Palette.sage100)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            AreaChoice(s, p.areaId) { vm.projects.setArea(p.id, it) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (p.status) {
                    ProjectStatus.ACTIVE ->
                        ActionButton(
                            tr("pauseProject"),
                            { vm.projects.setStatus(p.id, ProjectStatus.ON_HOLD) },
                            style = ActionStyle.Secondary,
                            icon = R.drawable.ic_lucide_pause,
                        )
                    ProjectStatus.ON_HOLD ->
                        ActionButton(
                            tr("resumeProject"),
                            { vm.projects.setStatus(p.id, ProjectStatus.ACTIVE) },
                            style = ActionStyle.Secondary,
                            icon = R.drawable.ic_lucide_play,
                        )
                    else ->
                        ActionButton(
                            tr("reactivateProject"),
                            { vm.projects.setStatus(p.id, ProjectStatus.ACTIVE) },
                            style = ActionStyle.Secondary,
                            icon = R.drawable.ic_lucide_rotate_ccw,
                        )
                }
                if (p.status != ProjectStatus.COMPLETED)
                    ActionButton(
                        tr("completeProject"),
                        {
                            vm.projects.setStatus(p.id, ProjectStatus.COMPLETED)
                            close()
                        },
                        icon = R.drawable.ic_lucide_check,
                    )
                if (p.status != ProjectStatus.ARCHIVED)
                    ActionButton(
                        tr("archiveProject"),
                        {
                            vm.projects.setStatus(p.id, ProjectStatus.ARCHIVED)
                            close()
                        },
                        style = ActionStyle.Soft,
                        icon = R.drawable.ic_lucide_archive,
                    )
                ActionButton(
                    tr(if (confirmDelete) "deleteProjectConfirm" else "deleteProject"),
                    {
                        if (confirmDelete) {
                            close()
                            vm.projects.delete(p.id)
                        } else confirmDelete = true
                    },
                    style = ActionStyle.Danger,
                    icon = R.drawable.ic_lucide_trash,
                )
            }
        }
    }
}

/** An area's active projects, a way to add one there, and deleting the area. */
@Composable
private fun BoxScope.AreaDetailSheet(
    vm: AppViewModel,
    area: AreaCard,
    wide: Boolean,
    open: (ProjectsSheet?) -> Unit,
) {
    var confirmDelete by remember(area.area.id) { mutableStateOf(false) }
    PanelSheet(
        area.area.title,
        { open(null) },
        tr("close"),
        description = tr("areaMeta", area.projects.size, area.openActions),
        centered = wide,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            if (area.projects.isNotEmpty())
                ActionList(Modifier.border(1.dp, Palette.line, RoundedCornerShape(16.dp))) {
                    area.projects.forEachIndexed { i, card ->
                        OptionRow(
                            card.project.title,
                            projectIcon(card.project.icon),
                            { open(ProjectsSheet.ProjectDetail(card.project.id)) },
                            divider = i < area.projects.lastIndex,
                        )
                    }
                }
            ActionButton(
                tr("addProjectHere"),
                { open(ProjectsSheet.NewProject(area.area.id)) },
                Modifier.fillMaxWidth(),
                icon = R.drawable.ic_lucide_plus,
            )
            ActionButton(
                tr(if (confirmDelete) "deleteAreaConfirm" else "deleteArea"),
                {
                    if (confirmDelete) {
                        open(null)
                        vm.projects.deleteArea(area.area.id)
                    } else confirmDelete = true
                },
                Modifier.fillMaxWidth(),
                ActionStyle.Danger,
                R.drawable.ic_lucide_trash,
            )
        }
    }
}
