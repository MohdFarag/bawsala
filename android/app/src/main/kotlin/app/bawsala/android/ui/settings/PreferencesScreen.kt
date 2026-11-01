package app.bawsala.android.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.bawsala.android.data.Snapshot
import app.bawsala.android.ui.components.DetailRow
import app.bawsala.android.ui.components.ScreenColumn
import app.bawsala.android.ui.components.SectionLabel
import app.bawsala.android.ui.components.Segmented
import app.bawsala.android.ui.components.TopBar
import app.bawsala.android.ui.i18n.num
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.viewmodel.AppViewModel

@Composable
fun PreferencesScreen(vm: AppViewModel, s: Snapshot) {
    val prefs = s.prefs
    ScreenColumn {
        TopBar(tr("preferences"), { vm.navigator.back() })
        SectionLabel(tr("language"))
        Segmented(
            listOf("en" to "English", "ar" to "العربية"),
            prefs.lang,
            { vm.settings.savePrefs(prefs.copy(lang = it)) },
            Modifier.fillMaxWidth(),
        )
        DetailRow(tr("reminderBefore"), num(prefs.reminderMinutes)) {
            val steps = listOf(0, 5, 10, 15, 30)
            vm.settings.savePrefs(
                prefs.copy(
                    reminderMinutes = steps[(steps.indexOf(prefs.reminderMinutes) + 1) % steps.size]
                )
            )
        }
    }
}
