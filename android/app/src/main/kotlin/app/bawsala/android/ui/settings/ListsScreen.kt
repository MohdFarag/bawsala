package app.bawsala.android.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.bawsala.android.data.Snapshot
import app.bawsala.android.ui.components.AppButton
import app.bawsala.android.ui.components.DetailRow
import app.bawsala.android.ui.components.Field
import app.bawsala.android.ui.components.ScreenColumn
import app.bawsala.android.ui.components.TopBar
import app.bawsala.android.ui.format.listLabel
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.core.model.TaskStatus

@Composable
fun ListsScreen(vm: AppViewModel, s: Snapshot) {
    var name by remember { mutableStateOf("") }
    ScreenColumn {
        TopBar(tr("lists"), { vm.navigator.back() })
        s.lists.forEach { l ->
            DetailRow(
                listLabel(l, s.lang),
                tr(
                    "openCount",
                    s.tasks.count { it.listId == l.id && it.status != TaskStatus.COMPLETED },
                ),
            )
        }
        Field(name, tr("newList")) { name = it }
        AppButton(
            tr("addList"),
            {
                vm.settings.addList(name.trim())
                name = ""
            },
            enabled = name.isNotBlank(),
        )
    }
}
