package app.bawsala.android.ui.search

import app.bawsala.android.data.Snapshot
import app.bawsala.android.ui.today.priorityOrder
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.Project
import app.bawsala.core.model.ProjectStatus
import app.bawsala.core.model.Task
import app.bawsala.core.model.TaskStatus

data class SearchResults(
    val tasks: List<Task>,
    val projects: List<Project>,
    val items: List<CaptureItem>,
) {
    val isEmpty: Boolean
        get() = tasks.isEmpty() && projects.isEmpty() && items.isEmpty()
}

/**
 * Case-insensitive search of tasks (title, notes, tags), projects (title, outcome) and kept ideas.
 * An empty query shows open tasks only, as the prototype's search does.
 */
fun search(s: Snapshot, q: String): SearchResults {
    val query = q.trim().lowercase()
    if (query.isEmpty())
        return SearchResults(
            s.tasks
                .filter { it.status != TaskStatus.COMPLETED && it.parentId == null }
                .sortedWith(priorityOrder)
                .take(8),
            emptyList(),
            emptyList(),
        )
    fun has(vararg parts: String?) =
        parts.filterNotNull().joinToString(" ").lowercase().contains(query)
    return SearchResults(
        tasks = s.tasks.filter { has(it.title, it.notes, it.tags.joinToString(" ")) },
        projects =
            s.projects.filter { it.status != ProjectStatus.ARCHIVED && has(it.title, it.outcome) },
        items =
            s.items.filter {
                it.state != CaptureState.TRASHED &&
                    it.state != CaptureState.PROCESSED &&
                    has(it.title, it.body, it.url, it.entries.joinToString(" ") { e -> e.text })
            },
    )
}
