package app.bawsala.android.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.Radius
import app.bawsala.android.ui.theme.TypeScale

/** A Lucide icon (res/drawable/ic_lucide_*), tinted; 22dp like the prototype's `.ico`. */
@Composable
fun LucideIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    tint: Color = Palette.ink,
    size: Dp = 22.dp,
    contentDescription: String? = null,
) {
    Icon(painterResource(icon), contentDescription, modifier.size(size), tint = tint)
}

/** `.btn` variants. */
enum class ActionStyle(val background: Color, val content: Color, val border: Color? = null) {
    Primary(Palette.green900, Palette.white),
    Secondary(Palette.white, Palette.green900, Palette.line),
    Soft(Palette.sage100, Palette.green900),
    Ghost(Color.Transparent, Palette.green900),
    Danger(Palette.dangerTint, Palette.danger),
    DarkSoft(Color(0x16FFFFFF), Palette.white),

    /** The priority card's sage button on dark green. */
    Accent(Palette.sage400, Palette.green900),
}

/** `.btn`: 50dp tall, 15dp corners, 14sp semibold, optional leading icon. */
@Composable
fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ActionStyle = ActionStyle.Primary,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.45f
    // Pressed buttons dim slightly (#64); at rest nothing changes.
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pressAlpha by
        animateFloatAsState(if (pressed) 0.85f else 1f, motionSpec(Motion.QUICK), label = "press")
    Row(
        modifier
            .graphicsLayer { this.alpha = pressAlpha }
            .heightIn(min = 50.dp)
            .clip(Radius.button)
            .background(style.background.copy(alpha = style.background.alpha * alpha))
            .then(
                style.border?.let { Modifier.border(BorderStroke(1.dp, it), Radius.button) }
                    ?: Modifier
            )
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) LucideIcon(icon, tint = style.content.copy(alpha = alpha), size = 20.dp)
        Text(
            label,
            style = TypeScale.button,
            color = style.content.copy(alpha = alpha),
            maxLines = 1,
        )
    }
}

/** `.text-btn`: a 48dp-tall text link with an optional icon. */
@Composable
fun TextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    color: Color = Palette.green900,
) {
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(Radius.sm)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = TypeScale.small.copy(fontWeight = TypeScale.button.fontWeight),
            color = color,
        )
        if (icon != null) LucideIcon(icon, tint = color, size = 16.dp)
    }
}

/** `.icon-btn`: 48dp, 15dp corners; `soft` adds a white fill and a line border. */
@Composable
fun IconAction(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    soft: Boolean = false,
    tint: Color = Palette.ink,
) {
    Box(
        modifier
            .size(48.dp)
            .clip(Radius.button)
            .then(
                if (soft)
                    Modifier.background(Palette.white).border(1.dp, Palette.line, Radius.button)
                else Modifier
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        LucideIcon(icon, tint = tint)
    }
}

/** `.pill` variants. */
enum class PillStyle(val background: Color, val content: Color, val border: Color? = null) {
    Default(Palette.sage100, Palette.green900),
    Lilac(Palette.lilac, Palette.lilacInk),
    Amber(Palette.amberTint, Palette.amber),
    Outline(Color.Transparent, Palette.muted, Palette.line),
    OnDark(Color(0x18FFFFFF), Palette.white),
}

/** `.pill`: a small rounded label, 25dp tall, 11sp, optional 14dp icon. */
@Composable
fun Pill(
    label: String,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Default,
    @DrawableRes icon: Int? = null,
) {
    Row(
        modifier
            .defaultMinSize(minHeight = 25.dp)
            .clip(Radius.pill)
            .background(style.background)
            .then(style.border?.let { Modifier.border(1.dp, it, Radius.pill) } ?: Modifier)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) LucideIcon(icon, tint = style.content, size = 14.dp)
        Text(
            label,
            style = TypeScale.tiny.copy(lineHeight = 18.7.sp),
            color = style.content,
            maxLines = 1,
        )
    }
}

/** `.chip`: 48dp tall filter choice; active = solid green, `sage` = soft without border. */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    sage: Boolean = false,
) {
    val (bg, fg, border) =
        when {
            selected -> Triple(Palette.green900, Palette.white, Palette.green900)
            sage -> Triple(Palette.sage100, Palette.ink, Color.Transparent)
            else -> Triple(Palette.white, Palette.ink, Palette.line)
        }
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(Radius.chip)
            .background(bg)
            .border(1.dp, border, Radius.chip)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) LucideIcon(icon, tint = fg, size = 16.dp)
        Text(label, style = TypeScale.chip, color = fg, maxLines = 1)
    }
}
