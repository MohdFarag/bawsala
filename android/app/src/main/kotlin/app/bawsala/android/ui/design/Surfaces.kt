package app.bawsala.android.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bawsala.android.R
import app.bawsala.android.ui.theme.ArefRuqaa
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.Radius
import app.bawsala.android.ui.theme.TypeScale

/** `.card`: white, line border, 18dp corners, 18dp padding. */
fun Modifier.v3Card(shape: RoundedCornerShape = Radius.md, padded: Boolean = true): Modifier =
    clip(shape)
        .background(Palette.white)
        .border(1.dp, Palette.line, shape)
        .then(if (padded) Modifier.padding(18.dp) else Modifier)

/** The بوصلة wordmark in Aref Ruqaa (91 × 41 in the phone header). */
@Composable
fun Wordmark(modifier: Modifier = Modifier, fontSize: Int = 34) {
    Text(
        "بوصلة",
        modifier,
        style =
            TypeScale.heading.copy(
                fontFamily = ArefRuqaa,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.2).sp,
            ),
        color = Palette.green900,
    )
}

/** `.section-title`: 17sp title, optional muted note, optional "show all" link at the end. */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TypeScale.sectionTitle)
            if (note != null)
                Text(
                    note,
                    style =
                        TypeScale.overline.copy(
                            color = Palette.muted,
                            fontWeight = TypeScale.small.fontWeight,
                        ),
                )
        }
        if (action != null && onAction != null)
            TextAction(action, onAction, icon = R.drawable.ic_lucide_chevron_left)
    }
}

/** `.avatar`: 40dp green-100 circle with an initial. */
@Composable
fun Avatar(initial: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Palette.green100)
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial,
            style = TypeScale.button.copy(fontWeight = TypeScale.sectionTitle.fontWeight),
            color = Palette.green900,
        )
    }
}

/** `.progress-track`: 6dp, green on green-100; `onDark` = sage on translucent white. */
@Composable
fun ProgressLine(fraction: Float, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val track = if (onDark) Color(0x25FFFFFF) else Palette.green100
    val fill = if (onDark) Palette.sage400 else Palette.green900
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(8.dp)).background(track)) {
        Box(
            Modifier.fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(8.dp))
                .background(fill)
        )
    }
}

/** `.check-box` in a 48dp `.check-hit`: 20dp, 7dp corners; checked = green with a check. */
@Composable
fun RoundCheck(
    checked: Boolean,
    onToggle: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(48.dp)
            .clip(Radius.sm)
            .clickable(role = Role.Checkbox, onClickLabel = label, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(20.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (checked) Palette.green900 else Color.Transparent)
                .border(
                    1.4.dp,
                    if (checked) Palette.green900 else Color(0xFFBDCAB8),
                    RoundedCornerShape(7.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) LucideIcon(R.drawable.ic_lucide_check, tint = Palette.white, size = 13.dp)
        }
    }
}

/** `.task-list`: white rows with dividers, 16dp outer corners. */
@Composable
fun ActionList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.white),
        content = content,
    )
}

/**
 * `.task-row`: checkbox, title, "project · context" meta with a sage dot, and minutes at the end.
 * Rows are separated by a line except after the last one ([divider] = false).
 */
@Composable
fun ActionRow(
    title: String,
    meta: List<String>,
    done: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    minutes: String? = null,
    divider: Boolean = true,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 83.dp)
                .clickable(onClick = onOpen)
                .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundCheck(done, onToggle, title)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style =
                        TypeScale.button.copy(
                            fontWeight = TypeScale.button.fontWeight,
                            lineHeight = 25.2.sp,
                        ),
                    color = Palette.ink.copy(alpha = if (done) 0.5f else 1f),
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                )
                if (meta.isNotEmpty()) MetaLine(meta)
            }
            if (minutes != null) Text(minutes, style = TypeScale.tiny)
        }
        if (divider) Spacer(Modifier.fillMaxWidth().height(1.dp).background(Palette.line))
    }
}

/** `.task-meta`: 10sp muted parts joined by a small sage dot. */
@Composable
fun MetaLine(parts: List<String>, modifier: Modifier = Modifier, color: Color = Palette.muted) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        parts.forEachIndexed { i, part ->
            if (i > 0) Box(Modifier.size(4.dp).clip(CircleShape).background(Palette.sage400))
            Text(
                part,
                style = TypeScale.nav.copy(fontWeight = TypeScale.small.fontWeight),
                color = color,
                maxLines = 1,
            )
        }
    }
}

/**
 * `.priority-card`: dark green, 24dp corners, two faint rings at the top start, overline and
 * optional pill, the title, a meta line, a sage action button and a note.
 */
@Composable
fun PriorityCard(
    overline: String,
    title: String,
    meta: List<String>,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    pill: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxWidth().clip(Radius.priorityCard).background(Palette.green900)) {
        // The two decorative rings (prototype :before/:after). The prototype places them with an
        // absolute `left`, so they stay at the top-left in both directions.
        Canvas(Modifier.matchParentSize()) {
            val ring = Stroke(1.dp.toPx())
            drawCircle(
                Color(0x0CFFFFFF),
                90.dp.toPx(),
                Offset(30.dp.toPx(), 30.dp.toPx()),
                style = ring,
            )
            drawCircle(
                Color(0x0AFFFFFF),
                110.dp.toPx(),
                Offset(27.dp.toPx(), 27.dp.toPx()),
                style = ring,
            )
        }
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 21.dp, bottom = 17.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    overline,
                    style = TypeScale.overline,
                    color = Palette.white,
                    modifier = Modifier.weight(1f),
                )
                pill?.invoke()
            }
            Text(
                title,
                style = TypeScale.priorityTitle,
                modifier = Modifier.padding(top = 11.dp, bottom = 6.dp),
            )
            MetaLine(meta, color = Color(0xFFD2E1D5))
            ActionButton(
                actionLabel,
                onAction,
                Modifier.fillMaxWidth().padding(top = 15.dp),
                style = ActionStyle.Accent,
                icon = R.drawable.ic_lucide_play,
            )
            if (note != null)
                Text(
                    note,
                    style = TypeScale.nav.copy(fontWeight = TypeScale.small.fontWeight),
                    color = Color(0xFFC2D5C5),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
        }
    }
}

/** A 1dp line, `.rule`. */
@Composable
fun Rule(modifier: Modifier = Modifier) {
    Spacer(modifier.fillMaxWidth().height(1.dp).background(Palette.line))
}

/** A vertical 1dp line, for rails and panels. */
@Composable
fun VerticalRule(modifier: Modifier = Modifier) {
    Spacer(modifier.fillMaxHeight().width(1.dp).background(Palette.line))
}
