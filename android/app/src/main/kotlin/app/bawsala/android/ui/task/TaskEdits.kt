package app.bawsala.android.ui.task

import app.bawsala.core.model.Project
import app.bawsala.core.model.ProjectStatus
import app.bawsala.core.model.Task
import app.bawsala.core.model.TaskList
import java.time.LocalDate

/** Edits offered on the task screen. Pure functions: each returns the updated task. */
object TaskEdits {
    private val ESTIMATES = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)

    /** Contexts offered on the task screen (ORG01); null means "anywhere". */
    val CONTEXTS = listOf(null, "home", "computer", "phone", "errands", "outside")

    fun nextContext(task: Task): Task =
        task.copy(context = CONTEXTS[(CONTEXTS.indexOf(task.context) + 1) % CONTEXTS.size])

    /** Steps through common estimates; after the longest comes "not set". */
    fun nextEstimate(task: Task): Task {
        val index = ESTIMATES.indexOf(task.estimateMin)
        return task.copy(
            estimateMin = if (index == ESTIMATES.lastIndex) null else ESTIMATES[index + 1]
        )
    }

    /**
     * The next of: no project, then each active or on-hold project in turn (V3 decision 4: a task
     * may belong to a project, separately from its list). A finished project the task is in stays
     * shown until the user moves on from it.
     */
    fun nextProject(task: Task, projects: List<Project>): Task {
        val choices =
            listOf<String?>(null) +
                projects
                    .filter {
                        it.status == ProjectStatus.ACTIVE || it.status == ProjectStatus.ON_HOLD
                    }
                    .sortedBy { it.createdAt }
                    .map { it.id }
        val index = choices.indexOf(task.projectId)
        return task.copy(projectId = choices[(index + 1) % choices.size])
    }

    fun nextList(task: Task, lists: List<TaskList>): Task {
        if (lists.isEmpty()) return task
        val index = lists.indexOfFirst { it.id == task.listId }
        return task.copy(listId = lists[(index + 1) % lists.size].id)
    }

    /**
     * Sets the real deadline; an early target after it no longer makes sense and is cleared
     * (PAR01).
     */
    fun withDeadline(task: Task, deadline: LocalDate): Task =
        task.copy(
            deadline = deadline,
            targetDate = task.targetDate?.takeIf { !it.isAfter(deadline) },
        )

    /**
     * Sets "my completion target"; refused (null) when it would fall after the real deadline
     * (PAR01).
     */
    fun withTarget(task: Task, target: LocalDate): Task? =
        if (task.deadline != null && target.isAfter(task.deadline)) null
        else task.copy(targetDate = target)

    fun withTags(task: Task, text: String): Task =
        task.copy(tags = text.split(',').map(String::trim).filter(String::isNotEmpty).toSet())
}
