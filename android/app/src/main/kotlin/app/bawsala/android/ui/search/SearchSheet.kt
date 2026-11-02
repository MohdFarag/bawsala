package app.bawsala.android.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Screen
import app.bawsala.android.navigation.Tab
import app.bawsala.android.ui.components.Field
import app.bawsala.android.ui.design.ActionList
import app.bawsala.android.ui.design.ActionRow
import app.bawsala.android.ui.design.OptionRow
import app.bawsala.android.ui.design.PanelSheet
import app.bawsala.android.ui.design.isTabletWidth
import app.bawsala.android.ui.format.itemTitle
import app.bawsala.android.ui.format.listName
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.TaskStatus

/**
 * The prototype's search overlay ("ابحث في مساحتك."): one field, then matching tasks, projects and
 * ideas. Without a query it lists open tasks, as the prototype does. Closing goes back.
 */
@Composable
fun BoxScope.SearchSheet(vm: AppViewModel, s: Snapshot) {
    var q by remember { mutableStateOf("") }
    val r = search(s, q)
    val close = { vm.navigator.back() }
    val wide = isTabletWidth(androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp)
    PanelSheet(tr("searchTitle"), { close() }, tr("close"), centered = wide) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Field(q, tr("searchHint")) { q = it }
            if (r.tasks.isNotEmpty())
                ActionList(Modifier.border(1.dp, Palette.line, RoundedCornerShape(16.dp))) {
                    r.tasks.forEachIndexed { i, t ->
                        ActionRow(
                            t.title,
                            listOfNotNull(
                                s.projects.firstOrNull { it.id == t.projectId }?.title,
                                listName(s, t.listId),
                            ),
                            t.status == TaskStatus.COMPLETED,
                            { vm.tasks.toggleDone(t) },
                            { vm.navigator.replace(Screen.TaskDetail(t.id)) },
                            divider = i < r.tasks.lastIndex,
                        )
                    }
                }
            if (r.projects.isNotEmpty() || r.items.isNotEmpty())
                ActionList(Modifier.border(1.dp, Palette.line, RoundedCornerShape(16.dp))) {
                    val last = r.projects.size + r.items.size - 1
                    r.projects.forEachIndexed { i, p ->
                        OptionRow(
                            p.title,
                            R.drawable.ic_lucide_folder,
                            { vm.navigator.switchTo(Tab.PROJECTS) },
                            subtitle = tr("project"),
                            divider = i < last,
                        )
                    }
                    r.items.forEachIndexed { i, item ->
                        val waiting =
                            item.state == CaptureState.INBOX || item.state == CaptureState.DOING_NOW
                        OptionRow(
                            itemTitle(item, s.lang),
                            R.drawable.ic_lucide_inbox,
                            {
                                vm.navigator.replace(
                                    if (waiting) Screen.Process(item.id) else Screen.Collections
                                )
                            },
                            subtitle = tr(if (waiting) "triage" else "collections"),
                            divider = r.projects.size + i < last,
                        )
                    }
                }
            if (q.isNotBlank() && r.isEmpty)
                Text(
                    tr("noResultsTryFewer"),
                    style = TypeScale.small.copy(color = Palette.muted),
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Palette.white)
                            .border(1.dp, Palette.line, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                )
        }
    }
}
