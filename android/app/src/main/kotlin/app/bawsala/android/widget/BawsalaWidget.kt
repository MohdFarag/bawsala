package app.bawsala.android.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.bawsala.android.LaunchExtras
import app.bawsala.android.MainActivity
import app.bawsala.android.PlannerApp
import app.bawsala.android.R
import app.bawsala.android.ui.i18n.translate
import app.bawsala.android.ui.today.buildTodayState
import java.time.Instant

private val SMALL = DpSize(120.dp, 110.dp)
private val WIDE = DpSize(250.dp, 110.dp)

private val Green = ColorProvider(Color(0xFF05473F))
private val Sage = ColorProvider(Color(0xFFB2C18F))
private val White = ColorProvider(Color.White)
private val Faded = ColorProvider(Color(0xCCFFFFFF))

/**
 * The home-screen widget (P1): the next stop (prayer, appointment or session) and a capture button;
 * stretched wide it adds today's priority action. Tapping it opens Today.
 */
class BawsalaWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as PlannerApp).container
        if (!container.repository.state.value.loaded) container.repository.load()
        val s = container.repository.state.value
        val now = Instant.now()
        val today = now.atZone(s.prayer.zone).toLocalDate()
        val model =
            widgetModel(buildTodayState(s, container.dayPlanner, today, now), s.lang, s.prayer.zone)
        val captureLabel = translate(s.lang, "capture")
        val nothingNext = translate(s.lang, "nothingNext")
        val priorityLabel = translate(s.lang, "widgetPriority")
        provideContent { Content(context, model, captureLabel, nothingNext, priorityLabel) }
    }

    @Composable
    private fun Content(
        context: Context,
        model: WidgetModel,
        captureLabel: String,
        nothingNext: String,
        priorityLabel: String,
    ) {
        val wide = LocalSize.current.width >= WIDE.width
        Row(
            GlanceModifier.fillMaxSize()
                .background(Green)
                .cornerRadius(24.dp)
                .padding(14.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    model.stopLabel,
                    style = TextStyle(color = Faded, fontSize = 11.sp),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    model.stopTitle ?: nothingNext,
                    style =
                        TextStyle(color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    maxLines = 2,
                )
                if (wide && model.priority != null) {
                    Spacer(GlanceModifier.height(10.dp))
                    Text(
                        priorityLabel,
                        style = TextStyle(color = Sage, fontSize = 11.sp),
                        maxLines = 1,
                    )
                    Text(
                        model.priority,
                        style = TextStyle(color = White, fontSize = 13.sp),
                        maxLines = 1,
                    )
                }
            }
            Spacer(GlanceModifier.width(10.dp))
            Box(
                GlanceModifier.size(44.dp)
                    .background(Sage)
                    .cornerRadius(16.dp)
                    .clickable(
                        actionStartActivity(
                            // Its own action: intents differing only in extras would share one
                            // link.
                            Intent(context, MainActivity::class.java)
                                .setAction(LaunchExtras.ACTION_CAPTURE)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    ImageProvider(R.drawable.ic_lucide_plus),
                    contentDescription = captureLabel,
                    modifier = GlanceModifier.size(22.dp),
                    colorFilter = androidx.glance.ColorFilter.tint(Green),
                )
            }
        }
    }

    companion object {
        /** Redraws every placed widget, e.g. after the day's data changed. */
        suspend fun refresh(context: Context) = BawsalaWidget().updateAll(context)
    }
}

class BawsalaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BawsalaWidget()
}
