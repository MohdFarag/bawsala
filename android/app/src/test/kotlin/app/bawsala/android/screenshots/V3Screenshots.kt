package app.bawsala.android.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.bawsala.android.PlannerApp
import app.bawsala.android.data.Prefs
import app.bawsala.android.navigation.Screen
import app.bawsala.android.navigation.Tab
import app.bawsala.android.ui.AppRoot
import app.bawsala.android.ui.theme.PlannerTheme
import app.bawsala.android.viewmodel.AppViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Phone: the Penpot mobile boards are 390 × 844. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = ScreenshotApp::class, sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class PhoneScreenshots : V3Screenshots("phone")

/** Tablet: the Penpot tablet boards are 1280 × 900, landscape. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = ScreenshotApp::class, sdk = [34], qualifiers = "w1280dp-h900dp-land-mdpi")
class TabletScreenshots : V3Screenshots("tablet")

/**
 * The V3 screens with the sample data of the Penpot boards, rendered by Robolectric and saved as
 * PNGs (V3 phase 7). They are written only when recording (`./gradlew recordRoborazziDebug`) and
 * compared side by side with the boards in docs/design.
 */
abstract class V3Screenshots(private val device: String) {
    @get:Rule val compose = createComposeRule()

    private fun shot(name: String, onboarded: Boolean = true, open: (AppViewModel) -> Unit = {}) {
        val app = ApplicationProvider.getApplicationContext<PlannerApp>()
        val repository = app.container.repository
        runBlocking {
            repository.load()
            SampleData.seed(repository)
            repository.savePrefs(Prefs(prayerConfirmed = true, onboarded = onboarded))
        }
        val vm = AppViewModel(app)
        open(vm)
        compose.setContent { PlannerTheme { AppRoot(vm, onExit = {}) } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$device/$name.png")
        // Stop the screen's background work (reminders, sync scheduling) so it cannot fail after
        // this test, inside the next one.
        vm.viewModelScope.cancel()
    }

    @Test fun today() = shot("01-today")

    @Test fun capture() = shot("02-capture") { it.openCapture() }

    @Test fun triage() = shot("03-triage") { it.navigator.switchTo(Tab.TRIAGE) }

    @Test fun projects() = shot("04-projects") { it.navigator.switchTo(Tab.PROJECTS) }

    @Test fun review() = shot("05-review") { it.navigator.switchTo(Tab.REVIEW) }

    @Test
    fun planner() =
        shot("06-planner") {
            it.navigator.switchTo(Tab.REVIEW)
            it.navigator.open(Screen.Plan)
        }

    @Test
    fun taskDetail() =
        shot("07-task") {
            it.navigator.switchTo(Tab.TODAY)
            it.navigator.open(Screen.TaskDetail(SampleData.FIRST_TASK))
        }

    @Test
    fun search() =
        shot("08-search") {
            it.navigator.switchTo(Tab.TODAY)
            it.navigator.open(Screen.Search)
        }

    @Test
    fun more() =
        shot("09-more") {
            it.navigator.switchTo(Tab.TODAY)
            it.navigator.open(Screen.More)
        }

    @Test fun onboarding() = shot("10-onboarding", onboarded = false)
}
