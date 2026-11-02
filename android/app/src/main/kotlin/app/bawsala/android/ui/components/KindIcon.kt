package app.bawsala.android.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import app.bawsala.android.R
import app.bawsala.android.ui.design.lucide
import app.bawsala.core.model.CaptureKind

@androidx.compose.runtime.Composable
fun kindIcon(kind: CaptureKind): ImageVector =
    when (kind) {
        CaptureKind.TEXT -> lucide(R.drawable.ic_lucide_type)
        CaptureKind.LIST -> lucide(R.drawable.ic_lucide_list_checks)
        CaptureKind.LINK -> lucide(R.drawable.ic_lucide_link)
        CaptureKind.AUDIO -> lucide(R.drawable.ic_lucide_mic)
        CaptureKind.IMAGE -> lucide(R.drawable.ic_lucide_image)
        CaptureKind.VIDEO -> lucide(R.drawable.ic_lucide_video)
        CaptureKind.FILE -> lucide(R.drawable.ic_lucide_file_text)
    }
