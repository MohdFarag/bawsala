package app.bawsala.android.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.bawsala.android.data.Prefs
import app.bawsala.android.domain.BlockMove
import app.bawsala.android.domain.DataExport
import app.bawsala.android.domain.PlaceFix
import app.bawsala.android.domain.PlaceRules
import app.bawsala.android.domain.SessionMove
import app.bawsala.core.model.BlockPolicy
import app.bawsala.core.model.TaskList
import app.bawsala.core.model.TimeBlock
import app.bawsala.core.prayer.PrayerSettings
import com.batoulapps.adhan2.CalculationMethod
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.launch

/** Preferences, prayer settings, time blocks, lists and data export. */
class SettingsController(private val ctx: ControllerScope) {

    /**
     * The usual method where the user now is, offered after a location change (never applied
     * alone).
     */
    var methodSuggestion by mutableStateOf<CalculationMethod?>(null)
        private set

    /** The phone is far from the saved place: Today offers to update prayer times. */
    var travelFix by mutableStateOf<PlaceFix?>(null)
        private set

    /** Today checks the location once per app start, and only if permission was already given. */
    private var travelChecked = false

    /**
     * Moves prayer times to [fix] (in the phone's time zone) and suggests the usual method there.
     */
    fun usePlace(fix: PlaceFix) {
        val current = ctx.snapshot.prayer
        savePrayerSettings(PlaceRules.withPlace(current, fix, ZoneId.systemDefault()))
        methodSuggestion = PlaceRules.usualMethod(fix.countryCode)?.takeIf { it != current.method }
        travelFix = null
        ctx.messages.show("prayerPlaceUpdated")
    }

    fun acceptMethod() {
        val method = methodSuggestion ?: return
        methodSuggestion = null
        savePrayerSettings(ctx.snapshot.prayer.copy(method = method))
    }

    fun dismissMethod() {
        methodSuggestion = null
    }

    /** Whether Today should read the location now (once per start). */
    fun shouldCheckTravel(): Boolean = !travelChecked.also { travelChecked = true }

    fun checkTravel(fix: PlaceFix?) {
        if (fix != null && PlaceRules.isFar(ctx.snapshot.prayer, fix)) travelFix = fix
    }

    fun dismissTravel() {
        travelFix = null
    }

    fun savePrefs(prefs: Prefs) = ctx.scope.launch { ctx.repository.savePrefs(prefs) }

    /** New prayer settings move dependent future sessions (AC10). */
    fun savePrayerSettings(settings: PrayerSettings) =
        ctx.scope.launch {
            ctx.repository.savePrayerSettings(settings)
            ctx.repository.savePrefs(ctx.snapshot.prefs.copy(prayerConfirmed = true))
            ctx.container.ruleScheduler.refreshAll(ctx.today())
        }

    /** Adds an every-day guiding block, 14:00–16:00, for the user to adjust. */
    fun addBlock(name: String) =
        ctx.scope.launch {
            val days = DayOfWeek.entries.toSet()
            ctx.repository.saveBlock(
                TimeBlock(
                    "block-${ctx.newId()}",
                    name,
                    LocalTime.of(14, 0),
                    LocalTime.of(16, 0),
                    days,
                    BlockPolicy.GUIDING,
                )
            )
        }

    /** What retiming [block] would do to its sessions; empty when nothing moves (BLK04). */
    fun previewBlockMove(block: TimeBlock): List<SessionMove> {
        val old = ctx.snapshot.blocks.firstOrNull { it.id == block.id } ?: return emptyList()
        return BlockMove.preview(
            old,
            block,
            ctx.snapshot.sessions,
            ctx.snapshot.prayer.zone,
            ctx.now(),
        )
    }

    /** Applies a previewed block move: the block and its sessions together. */
    fun moveBlock(block: TimeBlock, moves: List<SessionMove>) =
        ctx.scope.launch {
            ctx.repository.saveBlock(block)
            if (moves.isNotEmpty()) ctx.repository.saveSessions(moves.map { it.after })
        }

    fun saveBlock(block: TimeBlock) = ctx.scope.launch { ctx.repository.saveBlock(block) }

    fun deleteBlock(block: TimeBlock) = ctx.scope.launch { ctx.repository.deleteBlock(block.id) }

    fun addList(name: String) =
        ctx.scope.launch { ctx.repository.saveList(TaskList(ctx.newId(), name)) }

    fun exportJson(): String = DataExport.toJson(ctx.snapshot, ctx.now())
}
