package app.bawsala.android.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.bawsala.android.R
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.Radius
import app.bawsala.android.ui.theme.TypeScale

/**
 * Every V3 token and component on one screen, for comparing against the Penpot "Foundations" board.
 * Debug builds only (More → Design gallery). Always right to left, like the design.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignGallery(onBack: () -> Unit) {
    var sheet by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(false) }
    var chip by remember { mutableStateOf("medium") }
    var done by remember { mutableStateOf(false) }
    var nav by remember { mutableStateOf("today") }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FullScreen {
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconAction(R.drawable.ic_lucide_arrow_left, "رجوع", onBack, soft = true)
                    Wordmark()
                }
                Text("من فكرة عابرة، لخطوة واضحة.", style = TypeScale.headingLg)

                Section("الألوان") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                                "green-900" to Palette.green900,
                                "green-800" to Palette.green800,
                                "green-100" to Palette.green100,
                                "sage-400" to Palette.sage400,
                                "sage-100" to Palette.sage100,
                                "paper" to Palette.paper,
                                "white" to Palette.white,
                                "ink" to Palette.ink,
                                "muted" to Palette.muted,
                                "line" to Palette.line,
                                "amber" to Palette.amber,
                                "amber-tint" to Palette.amberTint,
                                "lilac" to Palette.lilac,
                                "lilac-ink" to Palette.lilacInk,
                                "danger" to Palette.danger,
                                "danger-tint" to Palette.dangerTint,
                                "grid-line" to Palette.gridLine,
                            )
                            .forEach { (name, color) -> Swatch(name, color) }
                    }
                }

                Section("الخطوط") {
                    listOf(
                            "headingLg 28" to TypeScale.headingLg,
                            "heading 24" to TypeScale.heading,
                            "pageTitle 19" to TypeScale.pageTitle,
                            "sectionTitle 17" to TypeScale.sectionTitle,
                            "body 15" to TypeScale.body,
                            "button 14" to TypeScale.button,
                            "small 13" to TypeScale.small,
                            "overline 12" to TypeScale.overline,
                            "tiny 11" to TypeScale.tiny,
                            "nav 10" to TypeScale.nav,
                        )
                        .forEach { (name, style) -> TypeSample(name, style) }
                }

                Section("الأزرار") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ActionButton("إجراء أساسي", {}, icon = R.drawable.ic_lucide_arrow_up_right)
                        ActionButton("إجراء ثانوي", {}, style = ActionStyle.Secondary)
                        ActionButton("هادئ", {}, style = ActionStyle.Soft)
                        ActionButton("شفاف", {}, style = ActionStyle.Ghost)
                        ActionButton(
                            "حذف",
                            {},
                            style = ActionStyle.Danger,
                            icon = R.drawable.ic_lucide_trash,
                        )
                        ActionButton("غير متاح", {}, enabled = false)
                        IconAction(R.drawable.ic_lucide_mic, "تسجيل", {}, soft = true)
                        IconAction(R.drawable.ic_lucide_search, "بحث", {})
                        TextAction("عرض الكل", {}, icon = R.drawable.ic_lucide_chevron_right)
                    }
                }

                Section("السياق والوسوم") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChoiceChip("الكل", chip == "all", { chip = "all" })
                        ChoiceChip(
                            "على الكمبيوتر",
                            chip == "medium",
                            { chip = "medium" },
                            icon = R.drawable.ic_lucide_laptop,
                        )
                        ChoiceChip(
                            "في المنزل",
                            chip == "home",
                            { chip = "home" },
                            icon = R.drawable.ic_lucide_house,
                        )
                        ChoiceChip("هادئ", false, {}, sage = true)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Pill("عمل", icon = R.drawable.ic_lucide_laptop)
                        Pill("اجتماع", style = PillStyle.Lilac)
                        Pill("بانتظار مراجعة", style = PillStyle.Amber)
                        Pill("نشط", style = PillStyle.Outline)
                        Pill(
                            "٩٠ دقيقة متاحة",
                            style = PillStyle.Outline,
                            icon = R.drawable.ic_lucide_clock_3,
                        )
                    }
                }

                Section("بطاقة الأولوية") {
                    PriorityCard(
                        overline = "الأولوية الآن",
                        title = "إنهاء مخطط تجربة المستخدم",
                        meta = listOf("عمل عميق", "٤٥ دقيقة"),
                        actionLabel = "ابدأ التركيز",
                        onAction = {},
                        note = "تناسب الوقت المتاح قبل موعدك التالي",
                    )
                }

                Section("الإجراءات التالية") {
                    SectionTitle("الإجراءات التالية", action = "عرض الكل", onAction = {})
                    ActionList {
                        ActionRow(
                            "أرسل ملاحظات النسخة التجريبية",
                            listOf("إطلاق بوصلة", "على الكمبيوتر"),
                            done,
                            { done = !done },
                            {},
                            minutes = "١٥ د",
                        )
                        ActionRow(
                            "أراجع مصاريف الأسبوع",
                            listOf("المالية الشخصية", "في المنزل"),
                            false,
                            {},
                            {},
                            minutes = "٢٠ د",
                            divider = false,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar("م")
                        Column(Modifier.weight(1f).padding(top = 16.dp)) { ProgressLine(0.66f) }
                    }
                    Box(
                        Modifier.fillMaxWidth()
                            .clip(Radius.md)
                            .background(Palette.green900)
                            .padding(16.dp)
                    ) {
                        ProgressLine(0.4f, onDark = true)
                    }
                }

                Section("التنقل") {
                    BottomBar(
                        start =
                            listOf(
                                NavDestination("today", "اليوم", R.drawable.ic_lucide_sun),
                                NavDestination(
                                    "triage",
                                    "الفرز",
                                    R.drawable.ic_lucide_inbox,
                                    badge = 7,
                                ),
                            ),
                        end =
                            listOf(
                                NavDestination("projects", "المشاريع", R.drawable.ic_lucide_layers),
                                NavDestination(
                                    "review",
                                    "المراجعة",
                                    R.drawable.ic_lucide_calendar_days,
                                ),
                            ),
                        activeKey = nav,
                        onSelect = { nav = it },
                        captureLabel = "التقاط",
                        onCapture = { sheet = true },
                        onCaptureHold = { toast = true },
                        modifier = Modifier.padding(top = 32.dp),
                    )
                    Row(
                        Modifier.height(520.dp)
                            .fillMaxWidth()
                            .clip(Radius.md)
                            .border(1.dp, Palette.line, Radius.md)
                    ) {
                        SideRail(
                            start =
                                listOf(
                                    NavDestination("today", "اليوم", R.drawable.ic_lucide_sun),
                                    NavDestination(
                                        "triage",
                                        "الفرز",
                                        R.drawable.ic_lucide_inbox,
                                        badge = 7,
                                    ),
                                ),
                            end =
                                listOf(
                                    NavDestination(
                                        "projects",
                                        "المشاريع",
                                        R.drawable.ic_lucide_layers,
                                    ),
                                    NavDestination(
                                        "review",
                                        "المراجعة",
                                        R.drawable.ic_lucide_calendar_days,
                                    ),
                                ),
                            activeKey = nav,
                            onSelect = { nav = it },
                            captureLabel = "التقاط",
                            onCapture = { sheet = true },
                            onCaptureHold = {},
                            bottom = {
                                IconAction(R.drawable.ic_lucide_circle_help, "مساعدة", {})
                                Avatar("م")
                            },
                        )
                    }
                }

                Section("النوافذ والتنبيهات") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton("افتح نافذة", { sheet = true }, style = ActionStyle.Secondary)
                        ActionButton(
                            "أظهر تنبيهًا",
                            { toast = true },
                            style = ActionStyle.Secondary,
                        )
                    }
                }
                Box(Modifier.height(120.dp))
            }

            if (toast) Toast("حُفظت في الفرز.", actionLabel = "تراجع", onAction = { toast = false })
            if (sheet)
                PanelSheet(
                    "فكرة الآن، مكانها بعدين.",
                    { sheet = false },
                    "إغلاق",
                    description = "اكتب كما تفكّر. بوصلة تلتقط التفاصيل معك.",
                ) {
                    Box(Modifier.fillMaxWidth().height(110.dp).v3Card())
                    ActionButton(
                        "احفظ في الفرز",
                        {
                            sheet = false
                            toast = true
                        },
                        Modifier.fillMaxWidth().padding(top = 16.dp),
                        icon = R.drawable.ic_lucide_arrow_up_right,
                    )
                }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().v3Card(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = TypeScale.sectionTitle)
        content()
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(Modifier.width(96.dp).clip(Radius.sm).border(1.dp, Palette.line, Radius.sm)) {
        Box(Modifier.fillMaxWidth().height(44.dp).background(color))
        Column(Modifier.background(Palette.white).fillMaxWidth().padding(8.dp)) {
            Text(name, style = TypeScale.tiny.copy(color = Palette.ink))
            Text(
                "#" + Integer.toHexString(colorArgb(color)).takeLast(6).uppercase(),
                style = TypeScale.tiny,
            )
        }
    }
}

private fun colorArgb(c: Color): Int =
    (((c.alpha * 255).toInt() shl 24) or
        ((c.red * 255).toInt() shl 16) or
        ((c.green * 255).toInt() shl 8) or
        (c.blue * 255).toInt())

@Composable
private fun TypeSample(name: String, style: TextStyle) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("خطوتك التالية، بوضوح.", style = style.copy(color = Palette.ink))
        Text(name, style = TypeScale.tiny, modifier = Modifier.padding(top = 4.dp))
    }
}
