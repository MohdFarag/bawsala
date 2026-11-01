package app.bawsala.android.ui.process

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Tab
import app.bawsala.android.ui.components.AppButton
import app.bawsala.android.ui.components.AppIconButton
import app.bawsala.android.ui.components.Corner12
import app.bawsala.android.ui.components.Empty
import app.bawsala.android.ui.components.ScreenColumn
import app.bawsala.android.ui.components.TopBar
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.AppColors
import app.bawsala.android.ui.theme.AppType
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.core.inbox.Triage
import app.bawsala.core.model.CaptureKind
import app.bawsala.core.model.CaptureState

internal enum class Panel {
    NONE,
    MENU,
    WAITING,
    SOMEDAY,
    SPLIT,
}

@Composable
fun ProcessScreen(vm: AppViewModel, s: Snapshot, itemId: String?) {
    val queue = Triage.queue(s.items)
    val item = queue.firstOrNull { it.id == itemId } ?: queue.firstOrNull()
    var panel by remember(item?.id) { mutableStateOf(Panel.NONE) }

    ScreenColumn(gap = 20.dp, bottomPadding = 32.dp) {
        TopBar(tr("inbox"), { vm.navigator.back() }) {
            AppIconButton(Icons.Outlined.Close, tr("close"), { vm.navigator.switchTo(Tab.INBOX) })
        }
        if (item == null) {
            Empty(Icons.Outlined.DoneAll, tr("inboxZero"), tr("inboxZeroHint"))
            AppButton(tr("backToInbox"), { vm.navigator.switchTo(Tab.INBOX) })
            return@ScreenColumn
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tr("itemOf", queue.indexOf(item) + 1, queue.size), style = AppType.micro)
            Row(
                Modifier.clip(Corner12)
                    .clickable(enabled = vm.inbox.canUndo) { vm.inbox.undoLast() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Undo,
                    null,
                    tint = if (vm.inbox.canUndo) AppColors.secondary else AppColors.border,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    tr("undo"),
                    style = AppType.label,
                    color = if (vm.inbox.canUndo) AppColors.secondary else AppColors.border,
                )
            }
        }

        CapturePreview(item, s.lang)

        when {
            item.state == CaptureState.DOING_NOW -> DoingNow(vm, item)
            item.kind == CaptureKind.LIST && item.entries.any { it.resolvedAs == null } ->
                EntriesPanel(vm, item)
            else -> Decisions(vm, item, panel) { panel = it }
        }
    }
}
