package app.bawsala.android.widget

import app.bawsala.android.ui.format.hm
import app.bawsala.android.ui.format.prayerName
import app.bawsala.android.ui.i18n.localizeDigits
import app.bawsala.android.ui.i18n.translate
import app.bawsala.android.ui.today.NextStop
import app.bawsala.android.ui.today.TodayState
import java.time.ZoneId

/** What the home-screen widget shows, already in the user's language. */
data class WidgetModel(
    /** "Next stop" heading, e.g. "المحطة القادمة · ٦:٠٠". */
    val stopLabel: String,
    /** The prayer, appointment or session; null when nothing else is planned today. */
    val stopTitle: String?,
    /** The priority action, shown when the widget is wide. */
    val priority: String?,
    /** "Captures waiting" count, for the triage line. */
    val inbox: Int,
)

/** The widget's content, from the same state Today uses. */
fun widgetModel(state: TodayState, lang: String, zone: ZoneId): WidgetModel {
    val stop = state.nextStop
    val title =
        when (stop) {
            is NextStop.Prayer -> translate(lang, "prayerOf", prayerName(stop.anchor.prayer, lang))
            is NextStop.Event -> stop.event.title
            is NextStop.Session -> stop.item.task.title
            null -> null
        }
    val heading = translate(lang, "widgetNextStop")
    return WidgetModel(
        stopLabel =
            if (stop == null) heading
            else heading + " · " + localizeDigits(stop.start.hm(zone), lang),
        stopTitle = title,
        priority = state.priority?.title,
        inbox = state.inboxCount,
    )
}
