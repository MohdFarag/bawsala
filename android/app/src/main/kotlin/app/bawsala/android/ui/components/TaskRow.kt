package app.bawsala.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.AppColors
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.Radius
import app.bawsala.android.ui.theme.TypeScale

@Composable
fun Checkbox(checked: Boolean) {
    Box(
        Modifier.size(20.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (checked) Palette.green900 else Palette.white)
            .border(
                1.5.dp,
                if (checked) Palette.green900 else Palette.line,
                RoundedCornerShape(7.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked)
            Icon(
                Icons.Outlined.Check,
                null,
                tint = AppColors.onPrimary,
                modifier = Modifier.size(14.dp),
            )
    }
}

@Composable
fun TaskRowView(
    title: String,
    meta: String,
    done: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val doneLabel = tr("complete")
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 76.dp).card().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Completion only follows an explicit tap on the checkbox.
        Box(
            Modifier.size(width = 36.dp, height = 48.dp)
                .clickable(role = Role.Checkbox, onClick = onToggle)
                .semantics {
                    contentDescription = doneLabel
                    stateDescription = if (done) "done" else "open"
                },
            contentAlignment = Alignment.Center,
        ) {
            Checkbox(done)
        }
        Column(
            Modifier.weight(1f).clickable(onClick = onOpen),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                style =
                    TypeScale.button.copy(
                        color = if (done) Palette.muted else Palette.ink,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(meta, style = TypeScale.tiny)
        }
        trailing?.invoke(this)
    }
}

@Composable
fun PrayerRow(name: String, time: String) {
    Row(
        Modifier.fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clip(Radius.md)
            .background(Palette.amberTint)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.NightsStay, null, tint = Palette.amber)
        Column {
            Text(name, style = TypeScale.button, color = Palette.amber)
            Text("$time · ${tr("protectedTime")}", style = TypeScale.tiny)
        }
    }
}
