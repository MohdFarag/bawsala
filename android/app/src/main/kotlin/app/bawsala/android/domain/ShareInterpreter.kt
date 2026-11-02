package app.bawsala.android.domain

import app.bawsala.core.model.Attachment
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind
import app.bawsala.core.model.CaptureState
import app.bawsala.core.model.SourceRef

/** Content handed over by the Android share sheet, after files were copied into app storage. */
data class SharedContent(
    val text: String?,
    val subject: String?,
    val attachments: List<Attachment>,
    val fromPackage: String?,
)

/** Interprets a share from Keep or any other app as capture input (CAP05). */
object ShareInterpreter {
    private val SERVICES =
        mapOf(
            "com.google.android.keep" to "keep",
            "com.google.android.gm" to "gmail",
            "com.google.android.youtube" to "youtube",
            "com.google.android.apps.docs" to "drive",
        )

    fun toCapture(share: SharedContent): CaptureInput {
        val service = serviceOf(share.fromPackage)
        // YouTube: the video as a link, under its own title (not "youtu.be").
        val video =
            CaptureFactory.firstUrl(share.text.orEmpty())?.takeIf {
                SourceLinks.youTubeId(it) != null
            }
        if (video != null)
            return CaptureInput(
                kind = CaptureKind.LINK,
                text = video,
                title = SourceLinks.youTubeTitle(share.subject),
                source = SourceRef(service, url = video),
            )
        // Drive: keep the link to the file (it stays in Drive), named after the file.
        val drive = SourceLinks.driveLink(share.text)
        if (drive != null && share.attachments.isEmpty())
            return CaptureInput(
                kind = CaptureKind.LINK,
                text = drive,
                title = share.subject?.takeIf { it.isNotBlank() },
                source = SourceRef(service, url = drive),
            )
        val text =
            listOfNotNull(
                    share.subject?.takeIf { it.isNotBlank() && share.text?.startsWith(it) != true },
                    share.text,
                )
                .joinToString("\n")
        return CaptureInput(
            kind = kindOf(text, share.attachments),
            text = text,
            attachments = share.attachments,
            source = SourceRef(service, url = CaptureFactory.firstUrl(text)),
        )
    }

    /**
     * An earlier capture from the same app with the same text, so the user can open it instead of
     * keeping a copy. Matching is by content and source, never by title alone.
     */
    fun findSimilar(items: List<CaptureItem>, input: CaptureInput): CaptureItem? {
        if (input.text.isBlank()) return null
        return items.firstOrNull {
            it.state != CaptureState.TRASHED &&
                it.source.service == input.source.service &&
                "${it.title}\n${it.body}".trim() == input.text.trim()
        }
    }

    private fun serviceOf(pkg: String?) =
        when (pkg) {
            null -> "share"
            else -> SERVICES[pkg] ?: "share:$pkg"
        }

    private fun kindOf(text: String, attachments: List<Attachment>): CaptureKind =
        when {
            attachments.isEmpty() -> if (looksLikeList(text)) CaptureKind.LIST else CaptureKind.TEXT
            attachments.all { it.mimeType.startsWith("image") } -> CaptureKind.IMAGE
            attachments.all { it.mimeType.startsWith("video") } -> CaptureKind.VIDEO
            attachments.all { it.mimeType.startsWith("audio") } -> CaptureKind.AUDIO
            else -> CaptureKind.FILE
        }

    /** Keep shares checklists as lines starting with ☐ or "- ". */
    private fun looksLikeList(text: String): Boolean {
        val lines = text.lines().filter { it.isNotBlank() }
        return lines.size > 1 &&
            lines.any { it.trimStart().startsWith("☐") || it.trimStart().startsWith("- ") }
    }
}
