package app.bawsala.android.domain

import app.bawsala.core.inbox.Triage
import app.bawsala.core.model.Attachment
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind
import app.bawsala.core.model.SourceRef
import java.net.URI
import java.time.Instant

/** What the user captured, before it becomes an inbox item. */
data class CaptureInput(
    val kind: CaptureKind,
    val text: String,
    val attachments: List<Attachment> = emptyList(),
    val source: SourceRef = SourceRef("app"),
    val tags: List<String> = emptyList(),
    /** A title known from the source (a YouTube video, a Drive file), used instead of the text. */
    val title: String? = null,
)

/**
 * Turns raw capture input into an inbox item: detects links, parses lists, picks title and body
 * (CAP01, CAP04).
 */
object CaptureFactory {
    private const val TITLE_MAX = 120
    private val URL = Regex("""https?://\S+""")

    fun firstUrl(text: String): String? = URL.find(text)?.value

    /**
     * [untitled] names captures that have neither text nor a file name, e.g. "Photo · 30 Oct,
     * 00:07".
     */
    fun create(
        id: String,
        input: CaptureInput,
        capturedAt: Instant,
        untitled: (CaptureKind) -> String,
    ): CaptureItem {
        val text = input.text.trim()
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        val url = firstUrl(text)
        val kind =
            if (input.kind == CaptureKind.TEXT && url != null && text == url) CaptureKind.LINK
            else input.kind
        val entries =
            if (kind == CaptureKind.LIST) Triage.textToEntries(text) { "$id:e$it" } else emptyList()

        val title =
            when {
                !input.title.isNullOrBlank() -> input.title.trim().take(TITLE_MAX)
                kind == CaptureKind.LINK -> hostOf(url!!) ?: url
                kind == CaptureKind.LIST ->
                    entries.firstOrNull()?.let { "${it.text}…" } ?: firstLine
                firstLine.isNotEmpty() -> firstLine.take(TITLE_MAX)
                else -> input.attachments.firstOrNull()?.name ?: untitled(kind)
            }
        val body =
            when (kind) {
                CaptureKind.LIST,
                CaptureKind.LINK -> ""
                else ->
                    text.lines().drop(1).joinToString("\n").trim().ifEmpty {
                        if (firstLine.length > TITLE_MAX) text else ""
                    }
            }
        return CaptureItem(
            id = id,
            kind = kind,
            title = title,
            body = body,
            url = url,
            source = input.source,
            capturedAt = capturedAt,
            attachments = input.attachments,
            entries = entries,
            tags = input.tags,
        )
    }

    private fun hostOf(url: String): String? = runCatching {
        URI(url).host?.removePrefix("www.")
    }
        .getOrNull()
}
