package app.bawsala.android.screenshots

import app.bawsala.android.data.Repository
import app.bawsala.android.data.WORK_LIST
import app.bawsala.android.domain.CaptureTags
import app.bawsala.core.model.Area
import app.bawsala.core.model.CalendarEvent
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind
import app.bawsala.core.model.Project
import app.bawsala.core.model.ProjectStatus
import app.bawsala.core.model.Task
import app.bawsala.core.model.TaskSession
import app.bawsala.core.model.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The content of the Penpot boards (prototype `defaults`), so screenshots compare like for like.
 */
object SampleData {
    const val FIRST_TASK = "t1"

    private val zone = ZoneId.of("Africa/Cairo")

    private fun today() = LocalDate.now(ScreenshotApp.CLOCK)

    private fun at(time: String, day: LocalDate = today()) =
        day.atTime(LocalTime.parse(time)).atZone(zone).toInstant()

    private val ago = ScreenshotApp.CLOCK.instant().minusSeconds(2 * 3600)

    suspend fun seed(r: Repository) {
        r.saveArea(Area("work", "العمل", "laptop", ago))
        r.saveArea(Area("health", "الصحة", "sun", ago))
        val projects =
            listOf(
                Project(
                    "p1",
                    "إطلاق النسخة التجريبية",
                    "نسخة صغيرة قابلة للاختبار.",
                    "work",
                    icon = "layers",
                    createdAt = ago.plusSeconds(50),
                ),
                Project(
                    "p2",
                    "تجديد الهوية البصرية",
                    areaId = "work",
                    status = ProjectStatus.ON_HOLD,
                    icon = "layout-grid",
                    createdAt = ago.plusSeconds(40),
                ),
                Project(
                    "p3",
                    "ترتيب الشؤون المالية",
                    icon = "chart-no-axes-combined",
                    createdAt = ago.plusSeconds(30),
                ),
                Project(
                    "p4",
                    "بناء روتين صباحي",
                    areaId = "health",
                    icon = "sun",
                    createdAt = ago.plusSeconds(20),
                ),
            )
        projects.forEach { r.saveProject(it) }
        listOf(
                Task(
                    FIRST_TASK,
                    "إنهاء مخطط تجربة المستخدم",
                    listId = WORK_LIST,
                    projectId = "p1",
                    context = "computer",
                    estimateMin = 45,
                    important = true,
                    createdAt = ago,
                ),
                Task(
                    "t2",
                    "أرسل ملاحظات النسخة التجريبية",
                    listId = WORK_LIST,
                    projectId = "p1",
                    context = "computer",
                    estimateMin = 15,
                    createdAt = ago,
                ),
                Task(
                    "t3",
                    "أراجع مصاريف الأسبوع",
                    listId = WORK_LIST,
                    projectId = "p3",
                    context = "home",
                    estimateMin = 20,
                    createdAt = ago,
                ),
                Task(
                    "t4",
                    "أرتب سطح المكتب",
                    listId = WORK_LIST,
                    projectId = "p4",
                    context = "home",
                    estimateMin = 10,
                    status = TaskStatus.COMPLETED,
                    completedAt = ago,
                    createdAt = ago,
                ),
                Task(
                    "t5",
                    "اختبار تدفّق الالتقاط",
                    listId = WORK_LIST,
                    projectId = "p1",
                    context = "computer",
                    estimateMin = 30,
                    status = TaskStatus.COMPLETED,
                    completedAt = ago,
                    createdAt = ago,
                ),
                Task(
                    "t6",
                    "مراجعة ملاحظات المستخدمين",
                    listId = WORK_LIST,
                    context = "computer",
                    estimateMin = 30,
                    createdAt = ago,
                ),
            )
            .forEach { r.saveTask(it) }
        listOf(
                "أرتّب أفكاري للنسخة التجريبية من بوصلة وأحدد أول خطوة.",
                "أحجز موعد أسنان بكرة الساعة ٦.",
                "مقال جميل عن بناء عادات صغيرة تستمر.",
                "أراجع ميزانية الشهر يوم الخميس.",
                "أكتب الأسئلة قبل اجتماع الفريق.",
                "فكرة لهدية نهاية الأسبوع.",
                "أرتب ملفات المشروع القديمة.",
            )
            .forEachIndexed { i, text ->
                r.saveItem(
                    CaptureItem(
                        "i$i",
                        CaptureKind.TEXT,
                        text,
                        capturedAt = ago.plusSeconds(i * 60L),
                        tags = CaptureTags.detect(text, "ar"),
                    )
                )
            }
        r.saveSessions(
            listOf(
                TaskSession("s1", FIRST_TASK, at("09:00"), at("10:30")),
                TaskSession("s2", "t3", at("13:00"), at("13:30")),
                TaskSession(
                    "s3",
                    "t2",
                    at("10:00", today().plusDays(1)),
                    at("11:00", today().plusDays(1)),
                ),
            )
        )
        r.saveEvent(CalendarEvent("e1", "اجتماع الفريق", at("11:00"), at("11:30"), "local"))
        r.saveEvent(
            CalendarEvent(
                "e2",
                "مراجعة التصميم",
                at("10:00", today().plusDays(2)),
                at("11:00", today().plusDays(2)),
                "local",
            )
        )
    }
}
