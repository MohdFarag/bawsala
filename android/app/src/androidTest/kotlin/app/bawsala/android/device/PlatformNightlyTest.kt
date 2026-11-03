package app.bawsala.android.device

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import app.bawsala.android.LaunchExtras
import app.bawsala.android.MainActivity
import app.bawsala.android.location.rememberPlaceRequest
import app.bawsala.android.navigation.Tab
import app.bawsala.core.model.CaptureState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class PlatformNightlyTest : DeviceUiFixture() {
    private fun permissionButton(allow: Boolean) {
        val id = if (allow) ".*:id/permission_allow(_foreground_only)?_button" else ".*:id/permission_deny_button"
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val button = device.wait(Until.findObject(By.res(Pattern.compile(id))), 10_000)
        assertNotNull("Expected a runtime permission dialog", button)
        button.click()
    }

    @Test fun microphoneDeniedShowsRecovery() {
        start()
        compose.runOnIdle { vm().openCapture() }
        compose.onNodeWithContentDescription(text("voiceInstead")).performClick()
        permissionButton(false)
        compose.onNodeWithText(text("micDenied")).assertIsDisplayed()
    }

    @Test fun microphoneCanBeGranted() {
        start()
        compose.runOnIdle { vm().openCapture() }
        compose.onNodeWithContentDescription(text("voiceInstead")).performClick()
        permissionButton(true)
        waitFor { app.packageManager.checkPermission(Manifest.permission.RECORD_AUDIO, app.packageName) == PackageManager.PERMISSION_GRANTED }
        assertTrue(vm().captureOpen)
    }

    @Test fun locationDenialReturnsToCaller() {
        start()
        var answered = false
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val request = rememberPlaceRequest { answered = true }
                Button(onClick = request) { Text("Request test location") }
            }
        }
        compose.onNodeWithText("Request test location").performClick()
        permissionButton(false)
        waitFor { answered }
        assertEquals(PackageManager.PERMISSION_DENIED, ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @Test fun locationCanBeGranted() {
        start()
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val request = rememberPlaceRequest { }
                Button(onClick = request) { Text("Request test location") }
            }
        }
        compose.onNodeWithText("Request test location").performClick()
        permissionButton(true)
        waitFor { ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED }
    }

    @Test fun shareAndWidgetCaptureEntryPoints() {
        start()
        compose.activityRule.scenario.onActivity { activity ->
            activity.startActivity(Intent(activity, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Shared from another app"))
        }
        waitFor { app.container.repository.state.value.items.any { it.title == "Shared from another app" } }
        compose.activityRule.scenario.onActivity { activity ->
            activity.startActivity(Intent(activity, MainActivity::class.java).setAction(LaunchExtras.ACTION_CAPTURE))
        }
        compose.onNodeWithText(text("saveToTriage")).assertIsDisplayed()
    }

    @Test fun mainTriageDecisionsAreUndoableOnDevice() {
        start()
        capture("Try every main triage decision")
        compose.runOnIdle { vm().navigator.switchTo(Tab.TRIAGE) }
        for (key in listOf("nextAction", "project", "reference", "later")) {
            compose.onNodeWithText(text(key)).performClick()
            waitFor { app.container.repository.state.value.items.single().state != CaptureState.INBOX }
            compose.runOnIdle { vm().inbox.undoLast() }
            waitFor { app.container.repository.state.value.items.single().state == CaptureState.INBOX }
            assertTrue(app.container.repository.state.value.tasks.isEmpty())
            assertTrue(app.container.repository.state.value.projects.isEmpty())
        }
    }
}
