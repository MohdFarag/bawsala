package app.bawsala.android.device

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import app.bawsala.android.AppContainer
import app.bawsala.android.PlannerApp

/** Uses real storage, crypto and networking without uncontrolled scheduled work. */
class DeviceTestRunner : AndroidJUnitRunner() {
    override fun newApplication(loader: ClassLoader, name: String, context: Context): Application =
        super.newApplication(loader, DeviceTestApp::class.java.name, context)
}

class DeviceTestApp : PlannerApp() {
    override fun createContainer() = AppContainer(this, backgroundWork = false)
    override fun startBackgroundWork() = Unit
}
