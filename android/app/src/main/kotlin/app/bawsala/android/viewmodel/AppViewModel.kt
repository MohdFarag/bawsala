package app.bawsala.android.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.bawsala.android.PlannerApp
import app.bawsala.android.data.Snapshot
import app.bawsala.android.domain.DayPlanner
import app.bawsala.android.navigation.Navigator
import app.bawsala.android.widget.BawsalaWidget
import app.bawsala.android.work.Reminders
import app.bawsala.core.inbox.Triage
import app.bawsala.core.model.TaskSession
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/** Which task the scheduling sheet plans, and the session being edited if any. */
data class ScheduleTarget(
    val taskId: String,
    val editing: TaskSession? = null,
    /** A start proposed by dropping the task on the timeline (PLN02). */
    val proposed: LocalDateTime? = null,
)

/**
 * Screen-level state shared by the whole app, and the feature controllers screens call into.
 * Business rules live in core and `domain/`; this class only wires them to the UI.
 */
@OptIn(FlowPreview::class) // debounce
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as PlannerApp).container
    val clock: Clock = container.clock

    val state: StateFlow<Snapshot> = container.repository.state
    val navigator = Navigator()
    val messages = Messages()
    val dayPlanner: DayPlanner = container.dayPlanner
    val mediaDir: File = container.mediaDir

    // Sheets shown over the current screen.
    var captureOpen by mutableStateOf(false)

    /** Set when capture was opened by holding the capture button: start in voice and record. */
    var captureVoice by mutableStateOf(false)

    /** Set when capture was opened from focus (M08): it says the timer keeps running. */
    var captureFromFocus by mutableStateOf(false)

    fun openCapture(voice: Boolean = false, fromFocus: Boolean = false) {
        captureVoice = voice
        captureFromFocus = fromFocus
        captureOpen = true
    }

    var scheduling by mutableStateOf<ScheduleTarget?>(null)
    var editingEvent by mutableStateOf<EventTarget?>(null)

    /**
     * Controller work is fire-and-forget; a failure there (storage full, unreadable data) is logged
     * and reported instead of crashing the app.
     */
    private val failures = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "Action failed", e)
        messages.show("actionFailed")
    }
    private val ctx =
        ControllerScope(viewModelScope + failures, container, navigator, messages, clock)
    val capture = CaptureController(ctx)
    val inbox = InboxController(ctx)
    val projects = ProjectController(ctx)
    val review = ReviewController(ctx)
    val focus = FocusController(ctx)
    val tasks = TaskController(ctx, focus)
    val settings = SettingsController(ctx)
    val account = AccountController(ctx)
    val google = GoogleController(ctx)
    val events = EventController(ctx)

    init {
        ctx.scope.launch {
            container.repository.load()
            container.ruleScheduler.refreshAll(today())
            container.repository.purgeItems(Triage.purgeDue(state.value.items, now()).map { it.id })
        }
        // Re-arm local reminders when what they depend on changes. Tables a write did not touch
        // keep
        // the same list instance, so the identity checks skip all other writes cheaply.
        viewModelScope.launch(failures) {
            state
                .filter { it.loaded && container.backgroundWork }
                .distinctUntilChanged { a, b ->
                    a.tasks === b.tasks &&
                        a.sessions === b.sessions &&
                        a.prefs.reminderMinutes == b.prefs.reminderMinutes
                }
                .collect { Reminders.sync(getApplication(), it) }
        }
        // Redraw the home-screen widget when what it shows changes (a moment later, so a burst
        // of edits or a sync redraws it once).
        viewModelScope.launch(failures) {
            state
                .filter { it.loaded && container.backgroundWork }
                .distinctUntilChanged { a, b ->
                    a.tasks === b.tasks &&
                        a.sessions === b.sessions &&
                        a.events === b.events &&
                        a.items === b.items &&
                        a.prayer == b.prayer &&
                        a.prefs.lang == b.prefs.lang
                }
                .debounce(WIDGET_REFRESH_MS)
                .collect { BawsalaWidget.refresh(getApplication()) }
        }
    }

    /** True while a pull-to-refresh is running (#67). */
    var refreshing by mutableStateOf(false)
        private set

    /** Bumped by each refresh so screens recompute time-based content (next stop, prayers). */
    var refreshTick by mutableIntStateOf(0)
        private set

    /**
     * Pull down to refresh: recompute the screen now, sync with the server when signed in (and
     * Google when connected), then say how it went.
     */
    fun refresh() {
        if (refreshing) return
        refreshing = true
        refreshTick++
        viewModelScope.launch(failures) {
            try {
                val result = container.syncRunner.run()
                if (state.value.calendars != null) google.syncNow()
                messages.show(RefreshRules.messageKey(result))
            } finally {
                refreshing = false
            }
        }
    }

    /** The UI language ("en" or "ar"). */
    val lang: String
        get() = state.value.lang

    fun now(): Instant = ctx.now()

    fun today(): LocalDate = ctx.today()

    private companion object {
        const val TAG = "AppViewModel"
        const val WIDGET_REFRESH_MS = 2_000L
    }
}
