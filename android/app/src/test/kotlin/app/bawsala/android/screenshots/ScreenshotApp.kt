package app.bawsala.android.screenshots

import app.bawsala.android.PlannerApp
import app.bawsala.android.AppContainer
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Fixed time and no external work: rendering must not depend on the runner's wall clock. */
class ScreenshotApp : PlannerApp() {
    override fun createContainer(): AppContainer {
        deleteDatabase("planner.db")
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Africa/Cairo"))
        java.util.Locale.setDefault(java.util.Locale.US)
        return AppContainer(this, CLOCK, backgroundWork = false)
    }
    override fun startBackgroundWork() = Unit

    companion object {
        val CLOCK: Clock = Clock.fixed(Instant.parse("2026-11-02T07:00:00Z"), ZoneId.of("Africa/Cairo"))
    }
}
