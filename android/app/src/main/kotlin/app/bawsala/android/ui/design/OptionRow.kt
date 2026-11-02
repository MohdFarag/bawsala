package app.bawsala.android.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.bawsala.android.R
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale

/**
 * A menu choice inside an [ActionList]: icon, label and a forward chevron, 56dp tall, with a line
 * under it unless it is the last one ([divider] = false).
 */
@Composable
fun OptionRow(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    divider: Boolean = true,
    subtitle: String? = null,
) {
    val color = if (danger) Palette.danger else Palette.ink
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LucideIcon(icon, tint = if (danger) Palette.danger else Palette.green900, size = 20.dp)
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(label, style = TypeScale.button, color = color)
                if (subtitle != null) Text(subtitle, style = TypeScale.tiny, maxLines = 1)
            }
            LucideIcon(R.drawable.ic_lucide_chevron_right, tint = Palette.muted, size = 18.dp)
        }
        if (divider) Spacer(Modifier.fillMaxWidth().height(1.dp).background(Palette.line))
    }
}
