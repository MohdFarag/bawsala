package app.bawsala.android.ui.projects

import app.bawsala.android.data.Snapshot
import app.bawsala.android.ui.today.priorityOrder
import app.bawsala.core.model.Area
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.Project
import app.bawsala.core.model.ProjectStatus
import app.bawsala.core.model.Task
import app.bawsala.core.model.TaskStatus

/** A project with its progress: actions done of all its actions, and the next open one. */
data class ProjectCard(
    val project: Project,
    val areaTitle: String?,
    val done: Int,
    val total: Int,
    val next: Task?,
    /** Its actions, open ones first (in priority order), then completed ones. */
    val tasks: List<Task>,
) {
    val progress: Float
        get() = if (total == 0) 0f else done.toFloat() / total
}

data class AreaCard(val area: Area, val projects: List<ProjectCard>) {
    val openActions: Int
        get() = projects.sumOf { it.total - it.done }
}

/** The four PARA tabs (M05/T07). */
data class ProjectsState(
    /** Active, then on hold; newest first within each. */
    val active: List<ProjectCard>,
    val areas: List<AreaCard>,
    /** Ideas kept as a reference, newest first. */
    val resources: List<CaptureItem>,
    /** Completed and archived projects, most recently finished first. */
    val archive: List<ProjectCard>,
)

fun buildProjectsState(s: Snapshot): ProjectsState {
    val areaTitles = s.areas.associate { it.id to it.title }
    val tasksByProject = s.tasks.filter { it.projectId != null }.groupBy { it.projectId }
    val cards =
        s.projects.map { p ->
            val tasks = tasksByProject[p.id].orEmpty()
            val (done, open) = tasks.partition { it.status == TaskStatus.COMPLETED }
            val ordered = open.sortedWith(priorityOrder)
            ProjectCard(
                project = p,
                areaTitle = p.areaId?.let(areaTitles::get),
                done = done.size,
                total = tasks.size,
                next = ordered.firstOrNull(),
                tasks = ordered + done.sortedByDescending { it.completedAt },
            )
        }
    val live = setOf(ProjectStatus.ACTIVE, ProjectStatus.ON_HOLD)
    val active =
        cards
            .filter { it.project.status in live }
            .sortedWith(
                compareBy<ProjectCard> { it.project.status.ordinal }
                    .thenByDescending { it.project.createdAt }
            )
    return ProjectsState(
        active = active,
        areas =
            s.areas
                .filterNot { it.archived }
                .sortedBy { it.createdAt }
                .map { a -> AreaCard(a, active.filter { it.project.areaId == a.id }) },
        resources =
            s.items
                .filter { it.state == CaptureState.REFERENCE }
                .sortedByDescending { it.capturedAt },
        archive =
            cards
                .filter { it.project.status !in live }
                .sortedByDescending { it.project.completedAt ?: it.project.createdAt },
    )
}
