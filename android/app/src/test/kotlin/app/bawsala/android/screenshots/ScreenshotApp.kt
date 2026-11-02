package app.bawsala.android.screenshots

import app.bawsala.android.PlannerApp

/** The real app without notification channels and scheduled work, for screenshot tests. */
class ScreenshotApp : PlannerApp() {
    override fun startBackgroundWork() = Unit
}
