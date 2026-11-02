package app.bawsala.android.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.bawsala.android.data.WORK_LIST
import app.bawsala.android.navigation.Screen
import app.bawsala.core.inbox.Triage
import app.bawsala.core.inbox.TriageOutcome
import app.bawsala.core.inbox.Undo
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.WaitingFor
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Triage decisions (TRI01–TRI07), undo, and managing items in collections and trash. */
class InboxController(private val ctx: ControllerScope) {
    private var lastUndo by mutableStateOf<Undo?>(null)
    val canUndo: Boolean
        get() = lastUndo != null

    /** The idea the Triage tab shows; null shows the oldest one waiting. */
    var focusId by mutableStateOf<String?>(null)
        private set

    /** Ideas sorted since the app started, for the triage progress line. */
    var sortedCount by mutableStateOf(0)
        private set

    // V3 triage (M04/T03): the four main decisions stay on the card --------------------------

    /** Turns an idea into one next action and moves on to the next idea. */
    fun toNextAction(item: CaptureItem, listId: String = WORK_LIST) =
        decide(item, Triage.toTasks(item, listOf(item.title), listId, ctx.now()), "triagedAction")

    fun toProject(item: CaptureItem) =
        decide(item, Triage.toProject(item, ctx.now()), "triagedProject")

    fun toLater(item: CaptureItem) = decide(item, Triage.toSomeday(item), "triagedLater")

    fun deleteIdea(item: CaptureItem) = decide(item, Triage.trash(item, ctx.now()), "triagedDelete")

    fun toReference(item: CaptureItem) = decide(item, Triage.toReference(item), "triagedReference")

    // Triage decisions ---------------------------------------------------------------------

    /** Creates one task and opens it so the user can plan it. */
    fun toTask(item: CaptureItem, listId: String = WORK_LIST) {
        val outcome = Triage.toTasks(item, listOf(item.title), listId, ctx.now())
        apply(outcome) {
            ctx.navigator.replace(Screen.TaskDetail(outcome.newTasks.first().id, fresh = true))
        }
    }

    fun startDoNow(item: CaptureItem) = apply(Triage.startDoNow(item))

    fun confirmDoNow(item: CaptureItem) {
        val next = nextAfter(item)
        apply(Triage.confirmDoNow(item, ctx.now()), message = "completed") { showNext(next) }
    }

    fun toSomeday(item: CaptureItem, reviewOn: LocalDate?) =
        moveTo(item, Triage.toSomeday(item, reviewOn), "someday")

    fun toWaiting(item: CaptureItem, waiting: WaitingFor) =
        moveTo(item, Triage.toWaiting(item, waiting), "waitingFor")

    fun trash(item: CaptureItem) = moveTo(item, Triage.trash(item, ctx.now()), "trash")

    /**
     * Splits text into separate inbox items; a plain-text original is resolved once its parts
     * exist.
     */
    fun split(item: CaptureItem, parts: List<String>) {
        val outcome =
            Triage.split(item, parts, ctx.now()).let {
                if (item.entries.isEmpty())
                    it.copy(item = it.item.copy(state = CaptureState.PROCESSED))
                else it
            }
        apply(outcome, message = "movedTo", argKey = "inbox") {
            ctx.navigator.replace(Screen.Process(null))
        }
    }

    /** Resolves list entries one by one: some become tasks, some references (AC03). */
    fun resolveEntries(
        item: CaptureItem,
        taskEntryIds: List<String>,
        referenceEntryIds: List<String>,
        listId: String = WORK_LIST,
        asSubtasks: Boolean = false,
    ) =
        apply(
            Triage.resolveEntries(
                item,
                taskEntryIds,
                referenceEntryIds,
                listId,
                ctx.now(),
                asSubtasks,
            ),
            message = "movedTo",
            argKey = "tasks",
        )

    fun convertToList(item: CaptureItem) =
        ctx.scope.launch {
            val text = listOf(item.title, item.body).filter { it.isNotBlank() }.joinToString("\n")
            ctx.repository.saveItem(
                item.copy(
                    kind = CaptureKind.LIST,
                    entries = Triage.textToEntries(text) { "${item.id}:e$it" },
                )
            )
        }

    fun undoLast() {
        val undo = lastUndo ?: return
        lastUndo = null
        ctx.scope.launch {
            ctx.repository.undo(undo)
            ctx.messages.dismiss()
            if (sortedCount > 0) sortedCount--
            when (ctx.navigator.current) {
                Screen.Inbox -> focusId = undo.previous.id
                is Screen.Process,
                is Screen.TaskDetail -> ctx.navigator.replace(Screen.Process(undo.previous.id))
                else -> Unit
            }
        }
    }

    // Collections and trash ----------------------------------------------------------------

    fun restore(item: CaptureItem) =
        ctx.scope.launch { ctx.repository.saveItem(Triage.restore(item)) }

    fun returnToInbox(item: CaptureItem) =
        ctx.scope.launch { ctx.repository.saveItem(item.copy(state = CaptureState.INBOX)) }

    fun deleteForever(ids: List<String>) = ctx.scope.launch { ctx.repository.purgeItems(ids) }

    // Internals ----------------------------------------------------------------------------

    private fun moveTo(item: CaptureItem, outcome: TriageOutcome, labelKey: String) =
        decide(item, outcome, "movedTo", labelKey)

    /** Stores a decision that takes the idea out of the queue, then shows the next idea. */
    private fun decide(
        item: CaptureItem,
        outcome: TriageOutcome,
        message: String,
        argKey: String? = null,
    ) {
        val next = nextAfter(item)
        apply(outcome, message = message, argKey = argKey) {
            sortedCount++
            when (ctx.navigator.current) {
                Screen.Inbox -> focusId = next
                is Screen.Process -> showNext(next)
                else -> Unit
            }
        }
    }

    private fun apply(
        outcome: TriageOutcome,
        message: String? = null,
        argKey: String? = null,
        then: () -> Unit = {},
    ) =
        ctx.scope.launch {
            ctx.repository.apply(outcome)
            lastUndo = outcome.undo
            then()
            if (message != null) ctx.messages.show(message, argKey) { undoLast() }
        }

    private fun showNext(nextItemId: String?) = ctx.navigator.replace(Screen.Process(nextItemId))

    private fun nextAfter(item: CaptureItem): String? {
        val queue = Triage.queue(ctx.snapshot.items)
        return queue.getOrNull(queue.indexOfFirst { it.id == item.id } + 1)?.id
    }
}
