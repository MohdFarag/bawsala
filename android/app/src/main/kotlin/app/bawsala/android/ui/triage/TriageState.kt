package app.bawsala.android.ui.triage

import app.bawsala.core.inbox.Triage
import app.bawsala.core.model.Attachment
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind

/** What the Triage card shows: one idea, its place in the queue, and the progress line. */
data class TriageState(
    val item: CaptureItem?,
    /** 1-based position of [item] among the ideas waiting. */
    val position: Int,
    val remaining: Int,
    /** Ideas sorted / (sorted + waiting); 1 when nothing is left. */
    val progress: Float,
)

/**
 * The idea to show: [focusId] when it is still waiting, otherwise the oldest one. [sorted] is how
 * many ideas were sorted in this sitting.
 */
fun buildTriageState(items: List<CaptureItem>, focusId: String?, sorted: Int): TriageState {
    val queue = Triage.queue(items)
    val item = queue.firstOrNull { it.id == focusId } ?: queue.firstOrNull()
    val total = sorted + queue.size
    return TriageState(
        item = item,
        position = if (item == null) 0 else queue.indexOf(item) + 1,
        remaining = queue.size,
        progress = if (total == 0) 1f else sorted.toFloat() / total,
    )
}

/** Recorded speech shows the microphone and "Voice note"; everything else is a quick capture. */
fun CaptureItem.isVoice(): Boolean =
    kind == CaptureKind.AUDIO || attachments.any(Attachment::isAudio)

private fun Attachment.isAudio() = mimeType.startsWith("audio")

/** Text that can be swiped or sorted with the four main buttons (lists sort entry by entry). */
fun CaptureItem.sortsWhole(): Boolean =
    !(kind == CaptureKind.LIST && entries.any { it.resolvedAs == null })
