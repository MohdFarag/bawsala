package app.bawsala.android.ui.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Screen
import app.bawsala.android.ui.components.Empty
import app.bawsala.android.ui.components.Field
import app.bawsala.android.ui.components.ScreenColumn
import app.bawsala.android.ui.components.TaskRowView
import app.bawsala.android.ui.components.TopBar
import app.bawsala.android.ui.format.listName
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.inbox.InboxCard
import app.bawsala.android.ui.inbox.syncBadge
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.TaskStatus

@Composable
fun SearchScreen(vm: AppViewModel, s: Snapshot) {
    var q by remember { mutableStateOf("") }
    val query = q.trim().lowercase()
    val tasks =
        if (query.isEmpty()) emptyList()
        else
            s.tasks.filter {
                (it.title + " " + it.notes + " " + it.tags.joinToString(" "))
                    .lowercase()
                    .contains(query)
            }
    val items =
        if (query.isEmpty()) emptyList()
        else
            s.items.filter {
                it.state != CaptureState.TRASHED &&
                    it.state != CaptureState.PROCESSED &&
                    (it.title +
                            " " +
                            it.body +
                            " " +
                            (it.url ?: "") +
                            " " +
                            it.entries.joinToString(" ") { e -> e.text })
                        .lowercase()
                        .contains(query)
            }
    ScreenColumn {
        TopBar(tr("search"), { vm.navigator.back() })
        Field(q, tr("searchHint")) { q = it }
        tasks.forEach { t ->
            TaskRowView(
                t.title,
                listName(s, t.listId).orEmpty(),
                t.status == TaskStatus.COMPLETED,
                { vm.tasks.toggleDone(t) },
                { vm.navigator.open(Screen.TaskDetail(t.id)) },
            )
        }
        val signedIn = vm.account.account.collectAsStateWithLifecycle().value != null
        items.forEach { i ->
            InboxCard(i, vm.lang, syncBadge(i, s.outbox, signedIn)) {
                if (i.state == CaptureState.INBOX || i.state == CaptureState.DOING_NOW)
                    vm.navigator.open(Screen.Process(i.id))
                else vm.navigator.open(Screen.Collections)
            }
        }
        if (query.isNotEmpty() && tasks.isEmpty() && items.isEmpty())
            Empty(Icons.Outlined.Search, tr("noResults"))
    }
}
