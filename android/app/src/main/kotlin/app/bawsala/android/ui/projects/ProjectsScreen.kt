package app.bawsala.android.ui.projects

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Screen
import app.bawsala.android.ui.components.PageHeading
import app.bawsala.android.ui.components.ScreenColumn
import app.bawsala.android.ui.design.ActionList
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.design.Rule
import app.bawsala.android.ui.design.v3Card
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.Radius
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.TaskStatus

/**
 * The "المشاريع" tab. The PARA screen (projects, areas, resources, archive) arrives in phase 4 of
 * the V3 redesign; until then this is the way to the tasks and collections that already exist.
 */
@Composable
fun ProjectsScreen(vm: AppViewModel, s: Snapshot) {
    ScreenColumn {
        PageHeading(
            tr("projects"),
            tr("projectsSubtitle"),
            onSearch = { vm.navigator.open(Screen.Search) },
            onMenu = { vm.navigator.open(Screen.More) },
        )
        Text(
            tr("projectsSoon"),
            style = TypeScale.small.copy(color = Palette.muted),
            modifier = Modifier.fillMaxWidth().v3Card(),
        )
        ActionList {
            HubRow(
                R.drawable.ic_lucide_list_checks,
                tr("allTasks"),
                tr("openCount", s.tasks.count { it.status != TaskStatus.COMPLETED }),
            ) {
                vm.navigator.open(Screen.Tasks)
            }
            Rule()
            HubRow(
                R.drawable.ic_lucide_book_open,
                tr("collections"),
                tr(
                    "openCount",
                    s.items.count {
                        it.state in
                            setOf(
                                CaptureState.REFERENCE,
                                CaptureState.SOMEDAY,
                                CaptureState.WAITING,
                            )
                    },
                ),
            ) {
                vm.navigator.open(Screen.Collections)
            }
            Rule()
            HubRow(
                R.drawable.ic_lucide_archive,
                tr("trash"),
                tr("openCount", s.items.count { it.state == CaptureState.TRASHED }),
            ) {
                vm.navigator.open(Screen.Trash)
            }
        }
    }
}

@Composable
private fun HubRow(@DrawableRes icon: Int, title: String, note: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(Radius.sm).background(Palette.sage100),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(icon, tint = Palette.green900, size = 20.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = TypeScale.button.copy(color = Palette.ink))
            Text(note, style = TypeScale.tiny)
        }
        LucideIcon(R.drawable.ic_lucide_chevron_right, tint = Palette.muted, size = 18.dp)
    }
}
