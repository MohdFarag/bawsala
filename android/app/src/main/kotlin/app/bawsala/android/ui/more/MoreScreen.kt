package app.bawsala.android.ui.more

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bawsala.android.BuildConfig
import app.bawsala.android.R
import app.bawsala.android.data.Snapshot
import app.bawsala.android.navigation.Screen
import app.bawsala.android.ui.design.ActionList
import app.bawsala.android.ui.design.Avatar
import app.bawsala.android.ui.design.IconAction
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.design.OptionRow
import app.bawsala.android.ui.design.PageHeader
import app.bawsala.android.ui.format.cityLabel
import app.bawsala.android.ui.format.listLabel
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel
import app.bawsala.core.model.TaskStatus
import java.io.File

/**
 * "More", reached from the avatar and the header menus: the account card, then your space, settings
 * and data as V3 option lists.
 */
@Composable
fun MoreScreen(vm: AppViewModel, s: Snapshot) {
    val context = LocalContext.current
    val account = vm.account.account.collectAsStateWithLifecycle().value
    Column(Modifier.fillMaxSize().background(Palette.paper)) {
        PageHeader(tr("more"), tr("moreSubtitle"), wide = false) {
            IconAction(R.drawable.ic_lucide_x, tr("close"), { vm.navigator.back() })
        }
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 9.dp, bottom = 104.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AccountCard(account?.email) { vm.navigator.open(Screen.Account) }
                Section(tr("yourSpace")) {
                    OptionRow(
                        tr("allTasks"),
                        R.drawable.ic_lucide_list_checks,
                        { vm.navigator.open(Screen.Tasks) },
                        subtitle =
                            tr("openCount", s.tasks.count { it.status != TaskStatus.COMPLETED }),
                    )
                    OptionRow(
                        tr("collections"),
                        R.drawable.ic_lucide_book_open,
                        { vm.navigator.open(Screen.Collections) },
                        subtitle = tr("collectionsSub"),
                    )
                    OptionRow(
                        tr("lists"),
                        R.drawable.ic_lucide_list,
                        { vm.navigator.open(Screen.Lists) },
                        subtitle = s.lists.joinToString { listLabel(it, s.lang) },
                        divider = false,
                    )
                }
                Section(tr("settings")) {
                    OptionRow(
                        tr("prayerTimes"),
                        R.drawable.ic_lucide_moon,
                        { vm.navigator.open(Screen.PrayerSettings) },
                        subtitle =
                            if (s.prefs.prayerFeatures) cityLabel(s.prayer, s.lang) else tr("off"),
                    )
                    OptionRow(
                        tr("timeBlocks"),
                        R.drawable.ic_lucide_layout_grid,
                        { vm.navigator.open(Screen.Blocks) },
                        subtitle = tr("timeBlocksSub"),
                    )
                    OptionRow(
                        tr("preferences"),
                        R.drawable.ic_lucide_settings_2,
                        { vm.navigator.open(Screen.Preferences) },
                        subtitle = tr("preferencesSub"),
                        divider = BuildConfig.DEBUG,
                    )
                    if (BuildConfig.DEBUG)
                        OptionRow(
                            tr("designGallery"),
                            R.drawable.ic_lucide_palette,
                            { vm.navigator.open(Screen.DesignGallery) },
                            subtitle = "V3",
                            divider = false,
                        )
                }
                Section(tr("data")) {
                    OptionRow(
                        tr("exportData"),
                        R.drawable.ic_lucide_send,
                        { shareExport(context, vm) },
                    )
                    OptionRow(
                        tr("trash"),
                        R.drawable.ic_lucide_trash,
                        { vm.navigator.open(Screen.Trash) },
                        divider = false,
                    )
                }
            }
        }
    }
}

/** The signed-in account, or "only on this device", opening account and sync. */
@Composable
private fun AccountCard(email: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.white)
            .border(1.dp, Palette.line, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Avatar(email?.take(1)?.uppercase() ?: "م")
        Column(Modifier.weight(1f)) {
            Text(tr("accountSync"), style = TypeScale.button)
            Text(email ?: tr("localOnly"), style = TypeScale.tiny, maxLines = 1)
        }
        LucideIcon(R.drawable.ic_lucide_chevron_right, tint = Palette.muted, size = 18.dp)
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = TypeScale.overline.copy(color = Palette.muted),
        modifier = Modifier.padding(top = 12.dp, start = 4.dp),
    )
    ActionList(
        Modifier.border(1.dp, Palette.line, RoundedCornerShape(16.dp)),
        content = content,
    )
}

/** A documented, restorable JSON export, shared through the system sheet (SEC02). */
private fun shareExport(context: Context, vm: AppViewModel) {
    val f = File(vm.mediaDir, "bawsala-export.json").apply { writeText(vm.settings.exportJson()) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", f)
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND)
                .setType("application/json")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            null,
        )
    )
}
