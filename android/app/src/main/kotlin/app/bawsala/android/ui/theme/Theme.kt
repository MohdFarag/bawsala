package app.bawsala.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bawsala.android.R

/**
 * The Bawsala V3 palette, named and valued exactly as the Penpot "Bawsala V3" token set
 * (docs/redesign-v3-plan.md, section 2).
 */
object Palette {
    val green900 = Color(0xFF05473F)
    val green800 = Color(0xFF0E5B50)
    val green100 = Color(0xFFE8EFDF)
    val sage400 = Color(0xFFB2C18F)
    val sage100 = Color(0xFFF1F4E9)
    val paper = Color(0xFFF7F8F3)
    val white = Color(0xFFFFFFFF)
    val ink = Color(0xFF213B35)
    val muted = Color(0xFF68776E)
    val line = Color(0xFFE1E8DC)
    val amber = Color(0xFFA56A2A)
    val amberTint = Color(0xFFFBF1E3)
    val lilac = Color(0xFFEAE6F4)
    val lilacInk = Color(0xFF6D6089)
    val danger = Color(0xFFA94E43)
    val dangerTint = Color(0xFFFAECE8)

    /** Weekly grid hour lines and column borders (used in Penpot, not in its library). */
    val gridLine = Color(0xFFEDF0E8)
}

/** Corner radii of the V3 design. */
object Radius {
    val sm = RoundedCornerShape(12.dp)
    val md = RoundedCornerShape(18.dp)
    val lg = RoundedCornerShape(26.dp)
    val button = RoundedCornerShape(15.dp)
    val chip = RoundedCornerShape(14.dp)
    val priorityCard = RoundedCornerShape(24.dp)
    val captureButton = RoundedCornerShape(22.dp)
    val pill = RoundedCornerShape(30.dp)
}

/** The V3 spacing scale (Penpot tokens bawsala.space.4 to .48). */
object Space {
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s20 = 20.dp
    val s24 = 24.dp
    val s32 = 32.dp
    val s40 = 40.dp
    val s48 = 48.dp

    /** Minimum touch target. */
    val touch = 48.dp
}

/** Noto Sans Arabic for the interface; Latin text falls back to the system font. */
val NotoSansArabic =
    FontFamily(
        Font(R.font.noto_sans_arabic_regular, FontWeight.Normal),
        Font(R.font.noto_sans_arabic_medium, FontWeight.Medium),
        Font(R.font.noto_sans_arabic_semibold, FontWeight.SemiBold),
        Font(R.font.noto_sans_arabic_bold, FontWeight.Bold),
    )

/** Aref Ruqaa, for the بوصلة wordmark only. */
val ArefRuqaa = FontFamily(Font(R.font.aref_ruqaa_bold, FontWeight.Bold))

private fun style(
    size: Int,
    lineHeight: Double,
    weight: FontWeight,
    color: Color = Palette.ink,
): TextStyle =
    TextStyle(
        fontFamily = NotoSansArabic,
        fontSize = size.sp,
        lineHeight = (size * lineHeight).sp,
        fontWeight = weight,
        color = color,
    )

/**
 * V3 type scale: sizes and line heights from the prototype's CSS, weights from Penpot (where the
 * CSS says 650, Penpot uses 700).
 */
object TypeScale {
    val headingLg = style(28, 1.6, FontWeight.Bold)
    val heading = style(24, 1.7, FontWeight.Bold)
    val priorityTitle = style(21, 1.75, FontWeight.SemiBold, Palette.white)
    val pageTitle = style(19, 1.7, FontWeight.Bold)
    val sectionTitle = style(17, 1.8, FontWeight.Bold)
    val body = style(15, 1.9, FontWeight.Normal)
    val button = style(14, 1.9, FontWeight.SemiBold)
    val small = style(13, 1.8, FontWeight.Normal)
    val overline = style(12, 1.8, FontWeight.SemiBold)
    val chip = style(12, 1.8, FontWeight.Normal)
    val tiny = style(11, 1.8, FontWeight.Normal, Palette.muted)
    val nav = style(10, 1.5, FontWeight.Medium, Palette.muted)
    val badge = style(9, 1.2, FontWeight.Bold, Palette.white)
    val timer = style(39, 1.2, FontWeight.Medium)
}

/**
 * Names the existing screens use, mapped onto the V3 palette so every screen takes the new look at
 * once. Screens move to [Palette] and [TypeScale] as they are redesigned (phases 3–6).
 */
object AppColors {
    val canvas = Palette.paper
    val surface = Palette.white
    val border = Palette.line
    val text = Palette.ink
    val secondary = Palette.muted
    val primary = Palette.green900
    val onPrimary = Palette.white
    val subtle = Palette.sage100
    val tint = Palette.green100
    val highlight = Palette.sage400
    val dark = Color(0xFF132E26)
    val warning = Palette.amberTint
    val warningText = Palette.amber
    val danger = Palette.danger
    val now = Color(0xFFC0522F)
    val scrim = Color(0x6B0F2117)
}

/** Text style names the existing screens use, mapped onto [TypeScale]. */
object AppType {
    val h1 = TypeScale.headingLg
    val h2 = TypeScale.heading
    val h3 = TypeScale.sectionTitle
    val body = TypeScale.body
    val button = TypeScale.button
    val label = TypeScale.button.copy(color = Palette.ink)
    val small = TypeScale.small.copy(color = Palette.muted)
    val micro = TypeScale.tiny
    val timer = TypeScale.timer.copy(fontSize = 64.sp, lineHeight = 80.sp)
}

@Composable
fun PlannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = Palette.green900,
                onPrimary = Palette.white,
                background = Palette.paper,
                surface = Palette.white,
                onSurface = Palette.ink,
                onBackground = Palette.ink,
                secondary = Palette.muted,
                outline = Palette.line,
                surfaceVariant = Palette.sage100,
                primaryContainer = Palette.green100,
                onPrimaryContainer = Palette.green900,
                error = Palette.danger,
                errorContainer = Palette.dangerTint,
            ),
        typography =
            Typography(
                bodyLarge = TypeScale.body,
                bodyMedium = TypeScale.small,
                labelLarge = TypeScale.button,
                titleLarge = TypeScale.sectionTitle,
            ),
        content = content,
    )
}
