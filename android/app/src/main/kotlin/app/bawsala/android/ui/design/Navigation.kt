package app.bawsala.android.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.bawsala.android.R
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale

/**
 * One destination in the bottom bar or side rail. [badge] shows a count on the icon (e.g. inbox).
 */
data class NavDestination(
    val key: String,
    val label: String,
    @DrawableRes val icon: Int,
    val badge: Int = 0,
)

/**
 * `.mobile-nav`: 88dp white bar, five slots (two destinations, the raised capture button, two
 * destinations). In RTL the first destination sits at the right, as in the design.
 */
@Composable
fun BottomBar(
    start: List<NavDestination>,
    end: List<NavDestination>,
    activeKey: String,
    onSelect: (String) -> Unit,
    captureLabel: String,
    onCapture: () -> Unit,
    onCaptureHold: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(Palette.white)) {
        Rule()
        Row(
            Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .height(80.dp)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            start.forEach {
                NavItem(it, it.key == activeKey, { onSelect(it.key) }, Modifier.weight(1f))
            }
            CaptureSlot(captureLabel, onCapture, onCaptureHold, Modifier.width(70.dp))
            end.forEach {
                NavItem(it, it.key == activeKey, { onSelect(it.key) }, Modifier.weight(1f))
            }
        }
    }
}

/** `.nav-item`: icon in a 45 × 29 pill (green-100 when active) above a 10sp label. */
@Composable
private fun NavItem(
    item: NavDestination,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconWidth: Dp = 45.dp,
    iconHeight: Dp = 29.dp,
    minHeight: Dp = 60.dp,
    gap: Dp = 3.dp,
) {
    val color = if (active) Palette.green900 else Palette.muted
    Column(
        modifier
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { selected = active },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically),
    ) {
        Box(
            Modifier.size(iconWidth, iconHeight)
                .clip(RoundedCornerShape(30.dp))
                .background(if (active) Palette.green100 else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(item.icon, tint = color)
            if (item.badge > 0)
                NavBadge(item.badge, Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-3).dp))
        }
        Text(
            item.label,
            style =
                TypeScale.nav.copy(
                    fontWeight =
                        if (active) TypeScale.sectionTitle.fontWeight else TypeScale.nav.fontWeight
                ),
            color = color,
            maxLines = 1,
        )
    }
}

/** `.nav-badge`: 17dp green bubble with a white ring and a 9sp count. */
@Composable
private fun NavBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.green900)
            .border(2.dp, Palette.white, RoundedCornerShape(20.dp))
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            app.bawsala.android.ui.i18n.localizeDigits(count.toString(), "ar"),
            style = TypeScale.badge,
        )
    }
}

/**
 * The raised capture button: 62dp, 22dp corners, green, with a 5dp paper outline and a soft shadow,
 * lifted 27dp above the bar. Tap writes, hold records voice.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CaptureButton(
    label: String,
    onClick: () -> Unit,
    onHold: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 62.dp,
    corner: Dp = 22.dp,
    outline: Dp = 5.dp,
) {
    val shape = RoundedCornerShape(corner + outline)
    Box(
        modifier
            .size(size + outline * 2)
            .clip(shape)
            .background(if (outline > 0.dp) Palette.paper else Color.Transparent)
            .padding(outline),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(size)
                .shadow(
                    18.dp,
                    RoundedCornerShape(corner),
                    ambientColor = Color(0x2405473F),
                    spotColor = Color(0x2405473F),
                )
                .clip(RoundedCornerShape(corner))
                .background(Palette.green900)
                .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onHold)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(R.drawable.ic_capture_compass, tint = Palette.white, size = 30.dp)
        }
    }
}

/** `.capture-slot`: the button lifted above the bar, with its 10sp label under it. */
@Composable
private fun CaptureSlot(
    label: String,
    onClick: () -> Unit,
    onHold: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.offset(y = (-27).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        CaptureButton(label, onClick, onHold)
        Text(label, style = TypeScale.nav)
    }
}

/**
 * `.side-rail` (tablet): 94dp white rail at the start edge with the wordmark, the destinations, the
 * capture button between them, and help and the avatar at the bottom.
 */
@Composable
fun SideRail(
    start: List<NavDestination>,
    end: List<NavDestination>,
    activeKey: String,
    onSelect: (String) -> Unit,
    captureLabel: String,
    onCapture: () -> Unit,
    onCaptureHold: () -> Unit,
    bottom: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxHeight()) {
        Column(
            Modifier.fillMaxHeight()
                .width(94.dp)
                .background(Palette.white)
                .padding(horizontal = 10.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Wordmark(Modifier.padding(bottom = 25.dp), fontSize = 30)
            start.forEach { RailItem(it, it.key == activeKey) { onSelect(it.key) } }
            Column(
                Modifier.padding(top = 13.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CaptureButton(
                    captureLabel,
                    onCapture,
                    onCaptureHold,
                    size = 58.dp,
                    corner = 20.dp,
                    outline = 0.dp,
                )
                Text(captureLabel, style = TypeScale.nav)
            }
            end.forEach { RailItem(it, it.key == activeKey) { onSelect(it.key) } }
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                bottom()
            }
        }
        VerticalRule()
    }
}

@Composable
private fun RailItem(item: NavDestination, active: Boolean, onClick: () -> Unit) {
    NavItem(
        item,
        active,
        onClick,
        Modifier.widthIn(min = 72.dp),
        iconWidth = 44.dp,
        iconHeight = 30.dp,
        minHeight = 66.dp,
        gap = 6.dp,
    )
}

/** Whether this window gets the tablet layout (side rail, two panes): 840dp and wider. */
fun isTabletWidth(widthDp: Int): Boolean = widthDp >= 840

/** A round dot, e.g. for legends. */
@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}
