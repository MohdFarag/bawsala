package app.bawsala.android.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.bawsala.android.domain.CaptureFactory
import app.bawsala.android.domain.CaptureInput
import app.bawsala.android.domain.ShareInterpreter
import app.bawsala.android.domain.SharedContent
import app.bawsala.android.domain.SourceLinks
import app.bawsala.android.media.MediaFiles
import app.bawsala.android.navigation.Screen
import app.bawsala.android.ui.format.kindKey
import app.bawsala.android.ui.format.shortDate
import app.bawsala.android.ui.i18n.localizeDigits
import app.bawsala.android.ui.i18n.translate
import app.bawsala.core.model.CaptureItem
import app.bawsala.core.model.CaptureKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Raw content from the Android share sheet; files are still behind temporary content URIs. */
data class SharePayload(
    val text: String?,
    val subject: String?,
    val uris: List<Uri>,
    val mime: String?,
    val fromPackage: String?,
)

/** A share that matches an earlier capture: the user opens the old one or keeps the new copy. */
data class DuplicateShare(val existing: CaptureItem, val newItemId: String)

/** Saving captures and incoming shares to the inbox (CAP01–CAP05). */
class CaptureController(private val ctx: ControllerScope) {
    var duplicate by mutableStateOf<DuplicateShare?>(null)
        private set

    /** Saves a capture; [onSaved] runs only after the durable local write (CAP02). */
    fun save(input: CaptureInput, itemId: String = ctx.newId(), onSaved: () -> Unit = {}) =
        ctx.scope.launch {
            ctx.repository.saveItem(CaptureFactory.create(itemId, input, ctx.now(), ::untitled))
            onSaved()
            ctx.messages.show("savedToInbox")
        }

    /** Copies shared files now, while the temporary URI grant is still valid (CAP05). */
    fun receiveShare(payload: SharePayload) =
        ctx.scope.launch {
            val attachments =
                withContext(Dispatchers.IO) {
                    payload.uris.mapNotNull {
                        MediaFiles.copyIn(
                            ctx.container.appContext,
                            it,
                            ctx.container.mediaDir,
                            payload.mime,
                        )
                    }
                }
            val shared =
                ShareInterpreter.toCapture(
                    SharedContent(payload.text, payload.subject, attachments, payload.fromPackage)
                )
            // A YouTube video keeps its thumbnail with the capture, fetched once now.
            val thumbnail =
                SourceLinks.youTubeId(shared.source.url)?.let { id ->
                    withContext(Dispatchers.IO) {
                        MediaFiles.download(
                            SourceLinks.youTubeThumbnail(id),
                            ctx.container.mediaDir,
                            name = "YouTube",
                        )
                    }
                }
            val input =
                thumbnail?.let { shared.copy(attachments = shared.attachments + it) } ?: shared
            val similar = ShareInterpreter.findSimilar(ctx.snapshot.items, input)
            val newId = ctx.newId()
            save(input, newId).join()
            if (similar != null) duplicate = DuplicateShare(similar, newId)
        }

    /** Keeps the earlier capture: the copy just saved is removed and the earlier one opened. */
    fun openExistingInsteadOfDuplicate() {
        val dup = duplicate ?: return
        duplicate = null
        ctx.scope.launch { ctx.repository.purgeItems(listOf(dup.newItemId)) }
        ctx.navigator.open(Screen.Process(dup.existing.id))
    }

    fun keepDuplicate() {
        duplicate = null
    }

    /** Title for a capture with no text or file name, e.g. "Photo · 30 Oct, 00:07". */
    private fun untitled(kind: CaptureKind): String {
        val lang = ctx.snapshot.lang
        val at = ctx.now().atZone(ctx.snapshot.prayer.zone)
        val time = localizeDigits(at.toLocalTime().withSecond(0).withNano(0).toString(), lang)
        return "${translate(lang, kindKey(kind))} · ${shortDate(at.toLocalDate(), lang)}, $time"
    }
}
