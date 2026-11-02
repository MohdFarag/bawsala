package app.bawsala.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bawsala.android.ui.theme.AppColors
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.Radius
import app.bawsala.android.ui.theme.TypeScale

val Corner8 = RoundedCornerShape(8.dp)

val Corner12 = RoundedCornerShape(12.dp)

val Corner16 = RoundedCornerShape(16.dp)

val Corner24 = RoundedCornerShape(24.dp)

fun Modifier.card(shape: RoundedCornerShape = Corner16, bg: Color = AppColors.surface) =
    this.clip(shape).background(bg).border(1.dp, AppColors.border, shape)

@Composable
fun Badge(
    text: String,
    bg: Color = AppColors.highlight,
    fg: Color = AppColors.dark,
    onClick: (() -> Unit)? = null,
) {
    Box(
        Modifier.height(25.dp)
            .clip(Radius.pill)
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TypeScale.tiny, color = fg)
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier
            .height(48.dp)
            .clip(Radius.chip)
            .background(if (selected) Palette.green900 else Palette.white)
            .border(1.dp, if (selected) Palette.green900 else Palette.line, Radius.chip)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null)
            Icon(
                icon,
                null,
                tint = if (selected) Palette.white else Palette.ink,
                modifier = Modifier.size(18.dp),
            )
        Text(
            text,
            style = TypeScale.chip,
            color = if (selected) Palette.white else Palette.ink,
        )
    }
}

@Composable
fun Segmented(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(Radius.md)
            .background(Palette.white)
            .border(1.dp, Palette.line, Radius.md)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (key, label) ->
            val on = key == selected
            Box(
                Modifier.weight(1f)
                    .height(40.dp)
                    .clip(Radius.sm)
                    .background(if (on) Palette.green900 else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = TypeScale.chip,
                    color = if (on) Palette.white else Palette.ink,
                )
            }
        }
    }
}

/** A labelled row inside a details card. */
@Composable
fun DetailRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = TypeScale.small.copy(color = Palette.muted))
        Text(value, style = TypeScale.button)
    }
}

@Composable
fun SectionLabel(text: String) =
    Text(
        text,
        style = TypeScale.overline.copy(color = Palette.muted),
        modifier = Modifier.padding(top = 6.dp),
    )

@Composable
fun Empty(icon: ImageVector, title: String, hint: String? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        // `.empty-state`: the icon in a 70dp sage square, a title and a muted line.
        Box(
            Modifier.size(70.dp).clip(RoundedCornerShape(24.dp)).background(Palette.sage100),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Palette.green900, modifier = Modifier.size(32.dp))
        }
        Text(
            title,
            style = TypeScale.sectionTitle.copy(fontSize = 20.sp),
            textAlign = TextAlign.Center,
        )
        if (hint != null)
            Text(
                hint,
                style =
                    TypeScale.overline.copy(color = Palette.muted, fontWeight = FontWeight.Normal),
                textAlign = TextAlign.Center,
            )
    }
}
