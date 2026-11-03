package app.bawsala.android.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.bawsala.android.R
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale

private val Scrim = Color(0x59132E26)

/**
 * `.overlay` + `.sheet`: a scrim and a white panel with a grip, title, close button and a muted
 * description. On phones it rises from the bottom (28dp top corners); on tablets ([centered]) it is
 * a centered window, as in T02.
 */
@Composable
fun BoxScope.PanelSheet(
    title: String,
    onDismiss: () -> Unit,
    closeLabel: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    centered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Opening (#64): 0 → 1 once, so at rest the sheet looks exactly as before.
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { opened = true }
    val progress by
        animateFloatAsState(if (opened) 1f else 0f, motionSpec(Motion.STANDARD), label = "sheet")
    Box(
        Modifier.matchParentSize()
            .graphicsLayer { alpha = progress }
            .background(Scrim)
            .clickable(
                remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = if (centered) Alignment.Center else Alignment.BottomCenter,
    ) {
        val shape =
            if (centered) RoundedCornerShape(28.dp)
            else RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Column(
            modifier
                .graphicsLayer {
                    if (centered) {
                        val scale = 0.96f + 0.04f * progress
                        scaleX = scale
                        scaleY = scale
                    } else translationY = (1f - progress) * size.height
                }
                .then(if (centered) Modifier.width(560.dp) else Modifier.fillMaxWidth())
                .heightIn(max = 760.dp)
                .shadow(
                    30.dp,
                    shape,
                    ambientColor = Color(0x16123026),
                    spotColor = Color(0x16123026),
                )
                .clip(shape)
                .background(Palette.white)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .verticalScroll(rememberScrollState())
                .then(if (centered) Modifier else Modifier.navigationBarsPadding())
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, top = 11.dp, bottom = 29.dp)
        ) {
            if (!centered)
                Box(
                    Modifier.align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                        .size(36.dp, 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFD8DFD3))
                )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(title, style = TypeScale.pageTitle, modifier = Modifier.weight(1f))
                IconAction(R.drawable.ic_lucide_x, closeLabel, onDismiss)
            }
            if (description != null)
                Text(
                    description,
                    style =
                        TypeScale.overline.copy(
                            color = Palette.muted,
                            fontWeight = TypeScale.small.fontWeight,
                        ),
                    modifier = Modifier.padding(top = 2.dp, bottom = 17.dp),
                )
            content()
        }
    }
}

/**
 * `.toast`: green, 17dp corners, 56dp tall, 20dp from the sides and 105dp above the bottom (over
 * the bottom bar), with an optional action such as Undo.
 */
@Composable
fun BoxScope.Toast(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    bottomInset: androidx.compose.ui.unit.Dp = 105.dp,
) {
    Row(
        modifier
            .align(Alignment.BottomCenter)
            .padding(start = 20.dp, end = 20.dp, bottom = bottomInset)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .shadow(
                36.dp,
                RoundedCornerShape(17.dp),
                ambientColor = Color(0x10143C30),
                spotColor = Color(0x10143C30),
            )
            .clip(RoundedCornerShape(17.dp))
            .background(Palette.green900)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            message,
            style = TypeScale.small,
            color = Palette.white,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null)
            Text(
                actionLabel,
                style = TypeScale.overline,
                color = Color(0xFFD4E2C3),
                modifier =
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
            )
    }
}

/** Fills the screen (helper for previews and the gallery). */
@Composable
fun FullScreen(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Palette.paper), content = content)
}
