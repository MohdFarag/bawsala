package app.bawsala.android.ui.task

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Screen
import app.bawsala.android.ui.components.AppButton
import app.bawsala.android.ui.components.AppIconButton
import app.bawsala.android.ui.components.ButtonStyle
import app.bawsala.android.ui.components.ScreenColumn
import app.bawsala.android.ui.components.TopBar
import app.bawsala.android.ui.components.card
import app.bawsala.android.ui.components.kindIcon
import app.bawsala.android.ui.components.pickDate
import app.bawsala.android.ui.components.rememberDraft
import app.bawsala.android.ui.design.ActionList
import app.bawsala.android.ui.design.MetaLine
import app.bawsala.android.ui.design.OptionRow
import app.bawsala.android.ui.design.Pill
import app.bawsala.android.ui.design.PillStyle
import app.bawsala.android.ui.design.RoundCheck
import app.bawsala.android.ui.design.lucide
import app.bawsala.android.ui.format.listName
import app.bawsala.android.ui.format.shortDate
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.AppColors
import app.bawsala.android.ui.theme.AppType
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.android.viewmodel.ScheduleTarget
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.Task
import app.bawsala.core.model.TaskStatus

@Composable
fun TaskDetailScreen(vm: AppViewModel, s: Snapshot, id: String, fresh: Boolean) {
    val task = s.tasks.firstOrNull { it.id == id }
    if (task == null) {
        LaunchedEffect(Unit) { vm.navigator.back() }
        return
    }
    val schedule = taskSchedule(s, task.id, vm.now())
    val save: (Task) -> Unit = { vm.tasks.save(it) }

    Column(Modifier.fillMaxSize()) {
        ScreenColumn(Modifier.weight(1f), bottomPadding = 16.dp) {
            TaskTopBar(vm, task, fresh)
            TitleAndNotes(
                task,
                listOfNotNull(
                    s.projects.firstOrNull { it.id == task.projectId }?.title,
                    listName(s, task.listId),
                ),
                onToggle = { vm.tasks.toggleDone(task) },
            ) { change ->
                vm.tasks.edit(task.id, change)
            }
            StatusBadges(task, save)
            DetailsCard(task, s, vm, save)
            s.items
                .firstOrNull { it.id == task.sourceItemId }
                ?.let { source ->
                    SourceLink(source) { vm.navigator.open(Screen.Process(source.id)) }
                }
            task.keepUrl?.let { KeepLink(it) }
            Subtasks(vm, task, s.tasks)
            SessionsCard(schedule, s.prayer.zone, vm, task, googleConnected = s.calendars != null)
        }
        BottomActions(vm, s, task, fresh, hasSessions = schedule.upcoming.isNotEmpty())
    }
}

@Composable
private fun TaskTopBar(vm: AppViewModel, task: Task, fresh: Boolean) {
    var menuOpen by remember { mutableStateOf(false) }
    TopBar(if (fresh) tr("newTask") else tr("task"), { vm.navigator.back() }) {
        AppIconButton(lucide(R.drawable.ic_lucide_ellipsis), tr("more"), { menuOpen = !menuOpen })
    }
    if (menuOpen) {
        ActionList(Modifier.border(1.dp, Palette.line, RoundedCornerShape(16.dp))) {
            OptionRow(
                tr(if (task.status == TaskStatus.COMPLETED) "reopen" else "complete"),
                R.drawable.ic_lucide_check,
                {
                    vm.tasks.toggleDone(task)
                    menuOpen = false
                },
            )
            OptionRow(
                tr("deleteTask"),
                R.drawable.ic_lucide_trash,
                { vm.tasks.delete(task) },
                danger = true,
                divider = false,
            )
        }
    }
}

@Composable
private fun TitleAndNotes(
    task: Task,
    meta: List<String>,
    onToggle: () -> Unit,
    edit: ((Task) -> Task) -> Unit,
) {
    // Each field saves only itself, onto the stored task, so the two never overwrite each other.
    var title by rememberDraft("${task.id}:title", task.title) { t -> edit { it.copy(title = t) } }
    var notes by rememberDraft("${task.id}:notes", task.notes) { n -> edit { it.copy(notes = n) } }
    // A white card like a task row: the check, then the title and notes, edited in place.
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.white)
            .border(1.dp, Palette.line, RoundedCornerShape(20.dp))
            .padding(start = 8.dp, end = 18.dp, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RoundCheck(task.status == TaskStatus.COMPLETED, onToggle, task.title)
        Column(
            Modifier.weight(1f).padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BasicTextField(
                title,
                { title = it },
                textStyle =
                    TypeScale.heading.copy(
                        fontSize = 21.sp,
                        lineHeight = 34.sp,
                        color = Palette.ink,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (meta.isNotEmpty()) MetaLine(meta)
            Box {
                if (notes.isEmpty())
                    Text(tr("notesHint"), style = TypeScale.small.copy(color = Palette.muted))
                BasicTextField(
                    notes,
                    { notes = it },
                    textStyle = TypeScale.small.copy(color = Palette.ink),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Tappable pills toggling importance and "in progress". */
@Composable
private fun StatusBadges(task: Task, save: (Task) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TogglePill(
            tr(if (task.important) "important" else "markImportant"),
            if (task.important) PillStyle.Amber else PillStyle.Outline,
            R.drawable.ic_lucide_flag,
        ) {
            save(task.copy(important = !task.important))
        }
        when (task.status) {
            TaskStatus.COMPLETED -> Pill(tr("completed"), icon = R.drawable.ic_lucide_check_check)
            TaskStatus.IN_PROGRESS ->
                TogglePill(tr("inProgress"), PillStyle.Default, R.drawable.ic_lucide_play) {
                    save(task.copy(status = TaskStatus.NEEDS_ACTION))
                }
            TaskStatus.NEEDS_ACTION ->
                TogglePill(tr("inProgress") + "?", PillStyle.Outline, R.drawable.ic_lucide_play) {
                    save(task.copy(status = TaskStatus.IN_PROGRESS))
                }
        }
    }
}

/** A pill that toggles something, with a 48dp touch target. */
@Composable
private fun TogglePill(label: String, style: PillStyle, icon: Int, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(30.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Pill(label, style = style, icon = icon)
    }
}

@Composable
private fun DetailsCard(task: Task, s: Snapshot, vm: AppViewModel, save: (Task) -> Unit) {
    val context = LocalContext.current
    val lang = s.lang
    // Each row shows its value under the label; tapping cycles it or opens a date picker.
    ActionList(Modifier.border(1.dp, Palette.line, RoundedCornerShape(16.dp))) {
        OptionRow(
            tr("project"),
            R.drawable.ic_lucide_folder,
            { save(TaskEdits.nextProject(task, s.projects)) },
            subtitle = s.projects.firstOrNull { it.id == task.projectId }?.title ?: tr("noProject"),
        )
        OptionRow(
            tr("listLabel"),
            R.drawable.ic_lucide_list,
            { save(TaskEdits.nextList(task, s.lists)) },
            subtitle = listName(s, task.listId) ?: "—",
        )
        OptionRow(
            tr("context"),
            R.drawable.ic_lucide_house,
            { save(TaskEdits.nextContext(task)) },
            subtitle = task.context?.let { tr("context_$it") } ?: tr("anywhere"),
        )
        OptionRow(
            tr("estimate"),
            R.drawable.ic_lucide_clock_3,
            { save(TaskEdits.nextEstimate(task)) },
            subtitle = task.estimateMin?.let { tr("minutes", it) } ?: tr("notSet"),
        )
        OptionRow(
            tr("deadline"),
            R.drawable.ic_lucide_flag,
            {
                pickDate(context, task.deadline ?: vm.today()) {
                    save(TaskEdits.withDeadline(task, it))
                }
            },
            subtitle = task.deadline?.let { shortDate(it, lang) } ?: tr("notSet"),
        )
        OptionRow(
            tr("target"),
            R.drawable.ic_lucide_calendar_days,
            {
                pickDate(context, task.targetDate ?: vm.today()) { date ->
                    TaskEdits.withTarget(task, date)?.let(save)
                }
            },
            subtitle = task.targetDate?.let { shortDate(it, lang) } ?: tr("notSet"),
        )
        Column(Modifier.padding(horizontal = 14.dp)) { TagsField(task, save) }
    }
}

@Composable
private fun TagsField(task: Task, save: (Task) -> Unit) {
    var text by remember(task.id) { mutableStateOf(task.tags.joinToString(", ")) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tr("tags"), style = AppType.small)
        BasicTextField(
            text,
            {
                text = it
                save(TaskEdits.withTags(task, it))
            },
            textStyle = AppType.label,
            modifier = Modifier.size(width = 180.dp, height = 24.dp),
        )
    }
}

/** Link back to the inbox item this task came from. */
@Composable
private fun SourceLink(source: CaptureItem, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().card().clickable(onClick = onOpen).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(kindIcon(source.kind), null, tint = AppColors.primary)
        Column(Modifier.weight(1f)) {
            Text(tr("originalNote"), style = AppType.label)
            Text(source.title, style = AppType.micro)
        }
    }
}

/** A task fresh from triage offers planning or moving on; otherwise focus and scheduling. */
@Composable
private fun BottomActions(
    vm: AppViewModel,
    s: Snapshot,
    task: Task,
    fresh: Boolean,
    hasSessions: Boolean,
) {
    Column(
        Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val schedule = { vm.scheduling = ScheduleTarget(task.id) }
        if (fresh) {
            AppButton(tr("planSession"), schedule)
            AppButton(
                tr("continueProcessing"),
                { vm.navigator.replace(Screen.Process()) },
                style = ButtonStyle.SECONDARY,
            )
            return@Column
        }
        if (task.status != TaskStatus.COMPLETED) {
            val focusing = s.focus?.taskId == task.id
            AppButton(
                if (focusing) tr("resume") else tr("startFocus"),
                {
                    vm.focus.start(task)
                    vm.navigator.open(Screen.Focus)
                },
            )
        }
        AppButton(
            if (hasSessions) tr("addSession") else tr("planSession"),
            schedule,
            style = ButtonStyle.SECONDARY,
        )
    }
}

/** The Keep note a Google task came from; opened in Keep, not copied (TSK02). */
@Composable
private fun KeepLink(url: String) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth()
            .card()
            .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(lucide(R.drawable.ic_lucide_sticky_note), null, tint = AppColors.primary)
        Text(tr("openInKeep"), style = AppType.label)
    }
}
