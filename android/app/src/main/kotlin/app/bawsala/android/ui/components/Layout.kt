package app.bawsala.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.bawsala.android.R
import app.bawsala.android.ui.design.IconAction
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.TypeScale

@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    gap: Dp = 16.dp,
    bottomPadding: Dp = 104.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier,
        contentPadding =
            PaddingValues(start = 22.dp, end = 22.dp, top = 9.dp, bottom = bottomPadding),
    ) {
        item { Column(verticalArrangement = Arrangement.spacedBy(gap)) { content() } }
    }
}

@Composable
fun PageHeading(
    title: String,
    subtitle: String,
    onSearch: (() -> Unit)?,
    onMenu: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = TypeScale.pageTitle, modifier = Modifier.weight(1f))
            if (onSearch != null) IconAction(R.drawable.ic_lucide_search, tr("search"), onSearch)
            // V3: settings and the rest of "More" sit behind the header's menu button.
            if (onMenu != null) IconAction(R.drawable.ic_lucide_ellipsis, tr("menu"), onMenu)
        }
        Text(subtitle, style = TypeScale.tiny)
    }
}

@Composable
fun TopBar(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable () -> Unit = { Spacer(Modifier.size(48.dp)) },
) {
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // "Back" is drawn pointing left and mirrored in Arabic, like the design's chevrons.
        IconAction(R.drawable.ic_lucide_arrow_left, tr("back"), onBack)
        Text(
            title,
            style = TypeScale.sectionTitle,
            maxLines = 1,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        trailing()
    }
}
