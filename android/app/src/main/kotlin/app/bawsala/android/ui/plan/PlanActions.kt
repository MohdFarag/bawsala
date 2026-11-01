package app.bawsala.android.ui.plan

import app.bawsala.android.navigation.Screen
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.android.viewmodel.EventTarget
import java.time.LocalDate

/** Whether tapping the item opens something (tasks and events do; prayers do not). */
internal val PlanItem.opens: Boolean
    get() = taskId != null || eventId != null

/** Opens what a plan item stands for: its task, or the event editor. */
internal fun AppViewModel.openPlanItem(item: PlanItem, date: LocalDate) {
    val task = item.taskId
    val event = item.eventId
    when {
        task != null -> navigator.open(Screen.TaskDetail(task))
        event != null -> editingEvent = EventTarget(date, event)
    }
}
