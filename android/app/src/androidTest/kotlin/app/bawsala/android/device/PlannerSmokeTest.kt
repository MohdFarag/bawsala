package app.bawsala.android.device

import android.graphics.Bitmap
import androidx.test.services.storage.TestStorage
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.bawsala.android.MainActivity
import app.bawsala.android.PlannerApp
import app.bawsala.android.data.Prefs
import app.bawsala.android.data.Repository
import app.bawsala.android.navigation.Screen
import app.bawsala.android.navigation.Tab
import app.bawsala.android.ui.i18n.translate
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.core.model.TaskStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith

abstract class DeviceUiFixture {
    protected val app get() = ApplicationProvider.getApplicationContext<PlannerApp>()

    @get:Rule(order = 0)
    val setup = object : ExternalResource() {
        override fun before() {
            if (Build.VERSION.SDK_INT >= 33) {
                InstrumentationRegistry.getInstrumentation().uiAutomation
                    .grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
            }
            runBlocking {
                app.container.repository.load()
                app.container.repository.savePrefs(Prefs(lang = "en", prayerFeatures = false))
            }
        }
    }

    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule(order = 2)
    val failureScreenshot = object : TestWatcher() {
        override fun failed(error: Throwable, description: Description) {
            try {
                val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                TestStorage().openOutputFile("${description.className}-${description.methodName}.png").use {
                    bitmap?.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } catch (captureError: Exception) {
                error.addSuppressed(captureError)
            }
        }
    }

    protected fun text(key: String) = translate("en", key)
    protected fun vm(): AppViewModel = ViewModelProvider(compose.activity)[AppViewModel::class.java]
    protected fun waitFor(check: () -> Boolean) = compose.waitUntil(15_000, check)
    protected fun start() {
        waitFor { app.container.repository.state.value.loaded }
        compose.onNodeWithText(text("skip")).performClick()
        compose.onNodeWithText(text("useWithoutAccount")).performScrollTo().performClick()
        waitFor { app.container.repository.state.value.prefs.onboarded }
    }
    protected fun capture(title: String) {
        compose.onNodeWithContentDescription(text("capture")).performClick()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput(title)
        compose.onNodeWithText(text("saveToTriage")).performClick()
        waitFor { app.container.repository.state.value.items.any { it.title == title } }
    }

}

@RunWith(AndroidJUnit4::class)
class PlannerSmokeTest : DeviceUiFixture() {
    @Test fun captureSurvivesActivityRestartAndFreshDatabaseRead() {
        start()
        capture("Device smoke capture")
        compose.activityRule.scenario.recreate()
        val fresh = Repository(app)
        runBlocking { fresh.load() }
        assertTrue(fresh.state.value.items.any { it.title == "Device smoke capture" })
        assertTrue(fresh.state.value.prefs.onboarded)
    }

    @Test fun triageUndoPlanFocusAndComplete() {
        start()
        capture("Complete a protected flow")
        compose.runOnIdle { vm().navigator.switchTo(Tab.TRIAGE) }
        compose.onNodeWithText(text("nextAction")).performClick()
        waitFor { app.container.repository.state.value.tasks.isNotEmpty() }
        compose.runOnIdle { vm().inbox.undoLast() }
        waitFor { app.container.repository.state.value.tasks.isEmpty() }
        compose.onNodeWithText(text("nextAction")).performClick()
        waitFor { app.container.repository.state.value.tasks.isNotEmpty() }
        val task = app.container.repository.state.value.tasks.single()
        compose.runOnIdle {
            val model = vm()
            model.tasks.saveFixedSession(task.id, model.now().plusSeconds(3600), model.now().plusSeconds(5400))
            model.focus.start(task)
            model.navigator.open(Screen.Focus)
        }
        waitFor { app.container.repository.state.value.sessions.isNotEmpty() && app.container.repository.state.value.focus != null }
        compose.runOnIdle { vm().tasks.toggleDone(app.container.repository.state.value.tasks.single()) }
        waitFor { app.container.repository.state.value.tasks.single().status == TaskStatus.COMPLETED && app.container.repository.state.value.focus == null }
        assertTrue(app.container.repository.state.value.timeLog.isNotEmpty())
    }
}
