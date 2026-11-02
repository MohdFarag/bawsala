package app.bawsala.android.viewmodel

import app.bawsala.android.data.WORK_LIST
import app.bawsala.core.model.Area
import app.bawsala.core.model.Project
import app.bawsala.core.model.ProjectStatus
import app.bawsala.core.model.Task
import kotlinx.coroutines.launch

/** PARA projects and areas (V3 decision 4: separate from lists; tasks may join a project). */
class ProjectController(private val ctx: ControllerScope) {

    fun create(title: String, areaId: String? = null) =
        ctx.scope.launch {
            ctx.repository.saveProject(
                Project(
                    id = ctx.newId(),
                    title = title.trim(),
                    areaId = areaId,
                    createdAt = ctx.now(),
                )
            )
            ctx.messages.show("triagedProject")
        }

    /** Completing records when; going back to active or on hold clears it. */
    fun setStatus(projectId: String, status: ProjectStatus) =
        ctx.scope.launch {
            ctx.repository.updateProject(projectId) {
                it.copy(
                    status = status,
                    completedAt =
                        when (status) {
                            ProjectStatus.COMPLETED -> it.completedAt ?: ctx.now()
                            ProjectStatus.ARCHIVED -> it.completedAt
                            else -> null
                        },
                )
            }
        }

    fun setArea(projectId: String, areaId: String?) =
        ctx.scope.launch { ctx.repository.updateProject(projectId) { it.copy(areaId = areaId) } }

    /** Its actions stay, without a project. */
    fun delete(projectId: String) = ctx.scope.launch { ctx.repository.deleteProject(projectId) }

    /** Adds an action to the project, kept in the default list like a triaged next action. */
    fun addStep(projectId: String, title: String) =
        ctx.scope.launch {
            ctx.repository.saveTask(
                Task(
                    id = ctx.newId(),
                    title = title.trim(),
                    listId = WORK_LIST,
                    projectId = projectId,
                    createdAt = ctx.now(),
                )
            )
        }

    fun createArea(title: String) =
        ctx.scope.launch {
            ctx.repository.saveArea(Area(ctx.newId(), title.trim(), createdAt = ctx.now()))
        }

    /** Its projects stay, without an area. */
    fun deleteArea(areaId: String) = ctx.scope.launch { ctx.repository.deleteArea(areaId) }
}
