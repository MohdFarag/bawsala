package app.bawsala.android.ui.auth

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bawsala.android.R
import app.bawsala.android.ui.design.IconAction
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.design.Wordmark
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale

/**
 * The page every auth board shares (AM01–AM06): back button and wordmark, an optional symbol, the
 * heading and its muted line, the content, and the small reassurance at the bottom.
 */
@Composable
internal fun AuthPage(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    footer: String = tr("authFooter"),
    @DrawableRes symbol: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize()
            .background(Palette.paper)
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Wordmark(fontSize = 34)
                Spacer(Modifier.weight(1f))
                // The boards' back button is a forward arrow at the end edge in Arabic.
                if (onBack != null)
                    IconAction(R.drawable.ic_lucide_arrow_right, tr("back"), onBack, soft = true)
            }
            if (symbol != null)
                Box(
                    Modifier.padding(top = 36.dp)
                        // AM03 shows a small lock square; AM04/AM05 a larger mail square.
                        .size(if (symbol == R.drawable.ic_lucide_lock) 66.dp else 96.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Palette.green100),
                    contentAlignment = Alignment.Center,
                ) {
                    LucideIcon(symbol, tint = Palette.green900, size = 30.dp)
                }
            Text(
                title,
                style = TypeScale.headingLg.copy(fontSize = 28.sp, lineHeight = 44.sp),
                color = Palette.green900,
                modifier = Modifier.padding(top = if (symbol != null) 18.dp else 40.dp),
            )
            Text(
                subtitle,
                style = TypeScale.small.copy(color = Palette.muted),
                modifier = Modifier.padding(top = 6.dp, bottom = 26.dp),
            )
            content()
            Row(
                Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(R.drawable.ic_lucide_shield_check, tint = Palette.muted, size = 14.dp)
                Text(footer, style = TypeScale.tiny.copy(fontSize = 10.sp))
            }
        }
    }
}

/** A labelled input: icon at the start, optional eye toggle for passwords, red when [error]. */
@Composable
internal fun AuthField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    secret: Boolean = false,
    email: Boolean = false,
    error: Boolean = false,
) {
    var shown by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = TypeScale.overline)
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(shape)
                .background(if (error) Color(0xFFFFF8F7) else Palette.white)
                .border(1.dp, if (error) Palette.danger else Palette.line, shape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LucideIcon(icon, tint = Palette.muted, size = 20.dp)
            Box(Modifier.weight(1f)) {
                // Emails are written left to right even in Arabic.
                val textStyle =
                    TypeScale.body.copy(
                        color = Palette.ink,
                        textDirection = if (email) TextDirection.Ltr else TextDirection.Content,
                    )
                if (value.isEmpty())
                    Text(
                        placeholder,
                        style = textStyle.copy(color = Palette.muted),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign =
                            if (email && LocalLayoutDirection.current == LayoutDirection.Rtl)
                                TextAlign.End
                            else TextAlign.Start,
                    )
                BasicTextField(
                    value,
                    onChange,
                    textStyle = textStyle,
                    singleLine = true,
                    visualTransformation =
                        if (secret && !shown) PasswordVisualTransformation()
                        else VisualTransformation.None,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                when {
                                    secret -> KeyboardType.Password
                                    email -> KeyboardType.Email
                                    else -> KeyboardType.Text
                                }
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (secret)
                IconAction(
                    if (shown) R.drawable.ic_lucide_eye_off else R.drawable.ic_lucide_eye,
                    tr(if (shown) "hidePassword" else "showPassword"),
                    { shown = !shown },
                    tint = Palette.muted,
                )
        }
    }
}

/** "Question? **Action**": a muted question with a bold link, centered. */
@Composable
internal fun SwitchLink(question: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(question, style = TypeScale.small.copy(color = Palette.muted))
        Text(
            action,
            style = TypeScale.small.copy(fontWeight = FontWeight.Bold, color = Palette.green900),
            modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
        )
    }
}

/** A bold green link on its own line, e.g. "Back to sign in". */
@Composable
internal fun PlainLink(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        label,
        style = TypeScale.small.copy(fontWeight = FontWeight.Bold, color = Palette.green900),
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 14.dp),
    )
}

/** AM06's red notice under the fields. */
@Composable
internal fun ErrorNotice(text: String) {
    Row(
        Modifier.fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.dangerTint)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(R.drawable.ic_lucide_circle_alert, tint = Palette.danger, size = 18.dp)
        Text(text, style = TypeScale.tiny.copy(color = Palette.danger, fontSize = 12.sp))
    }
}
