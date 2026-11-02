package app.bawsala.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Kinds of material that can be captured into the inbox (CAP01). */
enum class CaptureKind {
    TEXT,
    LIST,
    LINK,
    AUDIO,
    IMAGE,
    VIDEO,
    FILE,
}

/**
 * Lifecycle of a captured item. PROCESSED items keep their history and links; TRASHED items are
 * restorable until [CaptureItem.purgeAt] (DEL01).
 */
enum class CaptureState {
    INBOX,
    DOING_NOW,
    PROCESSED,
    REFERENCE,
    SOMEDAY,
    WAITING,
    TRASHED,
}

/** Where a capture came from. Keep shares are copies; no stable note URL is invented (CAP05). */
data class SourceRef(
    val service: String, // "app", "keep", "share:<package>", "gmail", ...
    val externalId: String? = null,
    val url: String? = null,
    val account: String? = null,
)

enum class UploadState {
    LOCAL_ONLY,
    UPLOADING,
    UPLOADED,
    FAILED,
}

/** Binary media stays in app storage until upload completes (CAP01/CAP02). */
data class Attachment(
    val id: String,
    val localPath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long? = null,
    val uploadState: UploadState = UploadState.LOCAL_ONLY,
    /**
     * Original file name when known (picked or shared files); null for camera and microphone
     * captures.
     */
    val name: String? = null,
    /** The server's id for the uploaded copy; set once an upload has started (CAP02). */
    val mediaId: String? = null,
)

/** One entry of a captured list; each has its own id so it can be processed alone (CAP04). */
data class ListEntry(val id: String, val text: String, val resolvedAs: String? = null)

data class WaitingFor(
    val who: String,
    val expected: String? = null,
    val requestedOn: LocalDate,
    val followUpOn: LocalDate?,
)

data class CaptureItem(
    val id: String,
    val kind: CaptureKind,
    val title: String,
    val body: String = "",
    val url: String? = null,
    val source: SourceRef = SourceRef("app"),
    val state: CaptureState = CaptureState.INBOX,
    val capturedAt: Instant,
    val attachments: List<Attachment> = emptyList(),
    val entries: List<ListEntry> = emptyList(),
    val parentId: String? = null, // set on items created by Split (TRI07)
    val derivedTaskIds: List<String> = emptyList(),
    val waiting: WaitingFor? = null,
    val reviewOn: LocalDate? = null, // Someday review date (TRI05)
    val completedAt: Instant? = null, // Do Now completion record (TRI02)
    val trashedAt: Instant? = null,
    val purgeAt: Instant? = null,
    val stateBeforeTrash: CaptureState? = null,
    /** Details noticed at capture (e.g. "tomorrow", "6:00", "@home"), shown during triage. */
    val tags: List<String> = emptyList(),
)

enum class TaskStatus {
    NEEDS_ACTION,
    IN_PROGRESS,
    COMPLETED,
}

data class Task(
    val id: String,
    val title: String,
    val notes: String = "",
    val listId: String,
    val tags: Set<String> = emptySet(),
    val important: Boolean = false,
    val status: TaskStatus = TaskStatus.NEEDS_ACTION,
    val estimateMin: Int? = null,
    val deadline: LocalDate? = null, // real deadline (NOT04)
    val targetDate: LocalDate? = null, // early "my completion target"
    val sourceItemId: String? = null,
    val createdAt: Instant,
    val completedAt: Instant? = null,
    /** The parent task when this is a subtask (TSK03). */
    val parentId: String? = null,
    /** Order among siblings, as Google Tasks sorts it. */
    val position: String? = null,
    /** The Keep note a Google task links to; shown, never copied into the inbox (TSK02). */
    val keepUrl: String? = null,
    val googleUrl: String? = null,
    /** Where the task can be done, e.g. "home" or "phone" (ORG01). */
    val context: String? = null,
    /** The project this task moves forward, if any; separate from [listId] (V3 decision 4). */
    val projectId: String? = null,
)

data class TaskList(val id: String, val name: String)

/** Where a project stands; ARCHIVED projects leave the active views but keep their history. */
enum class ProjectStatus {
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    ARCHIVED,
}

/**
 * An outcome that takes more than one step (PARA "Projects"). Tasks join it through
 * [Task.projectId]; lists stay a separate way to group tasks.
 */
data class Project(
    val id: String,
    val title: String,
    /** What "done" looks like, in the user's words. */
    val outcome: String = "",
    val areaId: String? = null,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    /** A Lucide icon name, e.g. "folder". */
    val icon: String = "folder",
    val createdAt: Instant,
    val completedAt: Instant? = null,
    /** The captured idea this project came from, when made during triage. */
    val sourceItemId: String? = null,
)

/**
 * One week's review (V3 M06): how far the three steps got and the outcomes chosen for the week. The
 * id is the week's first day, so each week has a single review on every device.
 */
data class WeeklyReview(
    val weekStart: LocalDate,
    /** Steps finished, 0 to 3: collect, review commitments, choose the week's direction. */
    val stepsDone: Int = 0,
    /** Up to three outcomes that matter this week. */
    val outcomes: List<String> = emptyList(),
    val completedAt: Instant? = null,
) {
    val id: String
        get() = weekStart.toString()
}

/** An ongoing responsibility with no end date (PARA "Areas"), e.g. health or family. */
data class Area(
    val id: String,
    val title: String,
    val icon: String = "layers",
    val createdAt: Instant,
    val archived: Boolean = false,
)

/** How a session's time is determined (REL01). */
sealed interface TimingRule {
    data object Fixed : TimingRule

    data class Relative(val rule: RelativeRule) : TimingRule
}

enum class PrayerName {
    FAJR,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
}

enum class PrayerPoint {
    ADHAN,
    IQAMA,
    END,
}

enum class Relation {
    STARTS_AFTER,
    STARTS_BEFORE,
    ENDS_BEFORE,
}

data class RelativeRule(
    val id: String,
    val prayer: PrayerName,
    val anchor: PrayerPoint,
    val relation: Relation,
    val offsetMin: Int,
    val durationMin: Int,
    val days: Set<java.time.DayOfWeek>? = null, // null = every day
    val from: LocalDate,
    val until: LocalDate? = null,
)

enum class SessionState {
    PLANNED,
    NEEDS_REVIEW,
    DONE,
    CANCELED,
}

/** A planned work period for a task; a task may have several (SES01, AC07). */
data class TaskSession(
    val id: String,
    val taskId: String,
    val start: Instant,
    val end: Instant,
    val timing: TimingRule = TimingRule.Fixed,
    val occurrenceDate: LocalDate? = null, // for rule-generated sessions
    val pinned: Boolean = false, // manual exception: fixed time for this occurrence (REL03)
    val state: SessionState = SessionState.PLANNED,
    val blockId: String? = null,
    /** The Google Calendar event mirroring this session, once published (SES01). */
    val googleEventId: String? = null,
)

enum class BlockPolicy {
    GUIDING,
    PROTECTED,
}

/** A recurring window allocated to an activity category (BLK01–04). */
data class TimeBlock(
    val id: String,
    val name: String,
    val start: LocalTime,
    val end: LocalTime,
    val days: Set<java.time.DayOfWeek>,
    val policy: BlockPolicy,
    val allowedListIds: Set<String> = emptySet(),
    val color: Long = 0xFFE6F0E7,
)

/** A standalone appointment, possibly mirrored from Google Calendar. */
data class CalendarEvent(
    val id: String,
    val title: String,
    val start: Instant,
    val end: Instant,
    val calendarId: String,
    val readOnly: Boolean = false,
    val allDay: Boolean = false,
    val location: String? = null,
    val description: String? = null,
    /** Set once the event exists in Google (CAL01). */
    val googleId: String? = null,
    /** Timed relative to a prayer on its date; re-timed when prayer times change (REL01). */
    val rule: RelativeRule? = null,
    /** The time block the event belongs to, without needing a task (CAL03). */
    val blockId: String? = null,
)
