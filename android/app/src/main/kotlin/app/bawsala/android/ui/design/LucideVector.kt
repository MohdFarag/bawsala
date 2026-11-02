package app.bawsala.android.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource

/** A Lucide drawable as an [ImageVector], for components that take one (e.g. `Icon`). */
@Composable fun lucide(@DrawableRes id: Int): ImageVector = ImageVector.vectorResource(id)
