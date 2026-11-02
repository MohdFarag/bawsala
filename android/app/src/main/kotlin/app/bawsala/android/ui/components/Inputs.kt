package app.bawsala.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.Radius
import app.bawsala.android.ui.theme.TypeScale

@Composable
fun MenuRow(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        label,
        style = TypeScale.button.copy(color = if (danger) Palette.danger else Palette.ink),
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(Radius.sm)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 13.dp),
    )
}

@Composable
fun Field(
    value: String,
    placeholder: String,
    minHeight: Int = 0,
    secret: Boolean = false,
    onChange: (String) -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxWidth()
            .heightIn(min = maxOf(minHeight, 52).dp)
            .clip(Radius.button)
            .background(Palette.white)
            .border(1.dp, Palette.line, Radius.button)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        if (value.isEmpty()) Text(placeholder, style = TypeScale.body.copy(color = Palette.muted))
        BasicTextField(
            value,
            onChange,
            textStyle = TypeScale.body.copy(color = Palette.ink),
            singleLine = secret,
            visualTransformation =
                if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions =
                if (secret) KeyboardOptions(keyboardType = KeyboardType.Password)
                else KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
