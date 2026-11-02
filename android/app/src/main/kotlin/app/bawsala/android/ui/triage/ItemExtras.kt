package app.bawsala.android.ui.triage

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.bawsala.android.R
import app.bawsala.android.ui.components.ImagePreview
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.format.attachmentLabel
import app.bawsala.android.ui.i18n.num
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.core.model.Attachment
import app.bawsala.core.model.CaptureItem
import java.io.File

/** A captured link and attachments, each opening in another app; nothing when there are none. */
@Composable
internal fun ItemExtras(item: CaptureItem, lang: String) {
    if (item.url == null && item.attachments.isEmpty()) return
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item.url?.let { url ->
            ExtraRow(R.drawable.ic_lucide_arrow_up_right, url) {
                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
            }
        }
        item.attachments.forEach { a ->
            val open = { runCatching { context.startActivity(viewIntent(context, a)) } }
            if (a.mimeType.startsWith("image"))
                ImagePreview(
                    a.localPath,
                    attachmentLabel(a, lang),
                    Modifier.clickable { open() },
                )
            else
                ExtraRow(
                    if (a.mimeType.startsWith("audio")) R.drawable.ic_lucide_mic
                    else R.drawable.ic_lucide_archive,
                    "${attachmentLabel(a, lang)} · ${num(a.sizeBytes / 1024)} KB",
                ) {
                    open()
                }
        }
    }
}

@Composable
private fun ExtraRow(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        Modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(icon, tint = Palette.green900, size = 16.dp)
        Text(
            label,
            style = TypeScale.small,
            color = Palette.green900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun viewIntent(context: android.content.Context, a: Attachment): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", File(a.localPath))
    return Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, a.mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
