package app.bawsala.android.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale

/**
 * `.page-header` for tab screens other than Today: a bold title with a small muted line under it,
 * and the header actions (search, page options) at the end. 69dp on phones, 96dp when [wide].
 */
@Composable
fun PageHeader(
    title: String,
    subtitle: String?,
    wide: Boolean,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(if (wide) 96.dp else 69.dp)
            .padding(
                start = if (wide) 30.dp else 22.dp,
                end = if (wide) 30.dp else 22.dp,
                top = if (wide) 22.dp else 9.dp,
                bottom = if (wide) 16.dp else 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style =
                    TypeScale.pageTitle.copy(
                        fontSize = if (wide) 24.sp else 19.sp,
                        lineHeight = if (wide) 40.sp else 32.sp,
                    ),
                maxLines = 1,
            )
            if (subtitle != null)
                Text(
                    subtitle,
                    style =
                        TypeScale.tiny.copy(
                            color = Palette.muted,
                            fontSize = if (wide) 12.sp else 11.sp,
                        ),
                    maxLines = 1,
                )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (wide) 8.dp else 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}
