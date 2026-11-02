package app.bawsala.android.ui.collections

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.ui.components.Empty
import app.bawsala.android.ui.components.ScreenColumn
import app.bawsala.android.ui.components.SectionLabel
import app.bawsala.android.ui.components.TopBar
import app.bawsala.android.ui.design.lucide
import app.bawsala.android.ui.format.shortDate
import app.bawsala.android.ui.i18n.num
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.core.model.CaptureState

@Composable
fun CollectionsScreen(vm: AppViewModel, s: Snapshot) {
    val groups =
        listOf(
            CaptureState.REFERENCE to tr("references"),
            CaptureState.SOMEDAY to tr("someday"),
            CaptureState.WAITING to tr("waitingFor"),
        )
    ScreenColumn {
        TopBar(tr("collections"), { vm.navigator.back() })
        if (s.items.none { i -> groups.any { it.first == i.state } })
            Empty(lucide(R.drawable.ic_lucide_folder), tr("emptyCollection"))
        groups.forEach { (state, label) ->
            val items = s.items.filter { it.state == state }
            if (items.isEmpty()) return@forEach
            SectionLabel("$label · ${num(items.size)}")
            items.forEach { i ->
                val extra =
                    i.waiting?.let { w ->
                        "${w.who}${w.followUpOn?.let { " · ${tr("followUpOn")} ${shortDate(it, vm.lang)}" } ?: ""}"
                    } ?: i.reviewOn?.let { "${tr("reviewOn")} ${shortDate(it, vm.lang)}" }
                ItemCard(
                    i,
                    listOf(
                        tr("returnToInbox") to
                            {
                                vm.inbox.returnToInbox(i)
                                Unit
                            },
                        tr("moveToTrash") to { vm.inbox.trash(i) },
                    ),
                    extra,
                )
            }
        }
    }
}
