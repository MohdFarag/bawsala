package app.bawsala.android.ui.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.bawsala.android.R
import app.bawsala.android.domain.CaptureInput
import app.bawsala.android.domain.CaptureTags
import app.bawsala.android.ui.design.ActionButton
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.design.PanelSheet
import app.bawsala.android.ui.design.isTabletWidth
import app.bawsala.android.ui.format.attachmentLabel
import app.bawsala.android.ui.format.clock
import app.bawsala.android.ui.i18n.LocalLang
import app.bawsala.android.ui.i18n.num
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel
import kotlinx.coroutines.delay

/**
 * V3 quick capture (M02 writing, M03 voice, T02 tablet window, M08 during focus): write or speak,
 * see the details noticed in the text as removable tags, and save to triage. Photo, video, files
 * and lists (CAP01) stay one tap away behind the "+" button. Drafts survive closing (CAP03).
 */
@Composable
fun BoxScope.CaptureSheet(vm: AppViewModel) {
    val state = rememberCaptureSheetState(vm.mediaDir)
    val lang = LocalLang.current
    val fromFocus = vm.captureFromFocus
    val voice = state.mode == CaptureMode.VOICE

    // Opened by holding the capture button: go straight to voice.
    LaunchedEffect(Unit) { if (vm.captureVoice) state.mode = CaptureMode.VOICE }

    fun close() {
        state.stopRecording()
        state.saveDraft()
        vm.captureOpen = false
    }

    fun save() {
        state.stopRecording()
        val input =
            CaptureInput(
                captureKind(state.mode, state.attachments),
                state.text,
                state.attachments.toList(),
                tags = state.tags(lang),
            )
        vm.capture.save(input) { state.clearDraft() }
        vm.captureOpen = false
    }

    BackHandler(onBack = ::close)
    PanelSheet(
        title = tr(if (fromFocus) "captureFocusTitle" else "captureTitle"),
        onDismiss = ::close,
        closeLabel = tr("close"),
        description = tr(if (fromFocus) "captureFocusDesc" else "captureDesc"),
        centered = isTabletWidth(LocalConfiguration.current.screenWidthDp),
    ) {
        Recorder(
            state,
            autoRecord = vm.captureVoice,
            onAutoRecordStarted = { vm.captureVoice = false },
        ) { record ->
            if (voice)
                VoicePanel(state) { if (state.recording) state.stopRecording() else record() }
            else WritingField(state)
            Attachments(state, lang)
            DetectedTags(state, lang)
            Row(
                Modifier.fillMaxWidth().padding(top = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                ActionButton(
                    tr(if (fromFocus) "saveAndBack" else "saveToTriage"),
                    ::save,
                    Modifier.weight(1f),
                    icon = R.drawable.ic_lucide_arrow_up_right,
                    enabled = state.canSave || state.recording,
                )
                SoftSquare(
                    if (voice) R.drawable.ic_lucide_list_checks else R.drawable.ic_lucide_mic,
                    tr(if (voice) "writeInstead" else "voiceInstead"),
                ) {
                    if (voice) {
                        state.stopRecording()
                        state.mode = CaptureMode.TEXT
                    } else {
                        state.mode = CaptureMode.VOICE
                        record()
                    }
                }
                OtherKinds(state)
            }
            if (state.micUnavailable)
                Text(
                    tr("micDenied"),
                    style = TypeScale.tiny.copy(color = Palette.danger),
                    modifier = Modifier.padding(top = 8.dp),
                )
            Text(
                tr(if (voice) "voiceHint" else "captureHint"),
                style = TypeScale.nav.copy(fontWeight = TypeScale.small.fontWeight),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 13.dp),
            )
        }
    }
}

/** `.capture-input`: a paper-colored field, 18sp, at least 128dp tall (155dp on tablets). */
@Composable
private fun WritingField(state: CaptureSheetState) {
    val focus = remember { FocusRequester() }
    val tablet = isTabletWidth(LocalConfiguration.current.screenWidthDp)
    LaunchedEffect(state.mode) { if (state.mode.isWriting) runCatching { focus.requestFocus() } }
    val shape = RoundedCornerShape(18.dp)
    val style =
        TypeScale.body.copy(
            fontSize = if (tablet) 20.sp else 18.sp,
            lineHeight = if (tablet) 38.sp else 34.2.sp,
        )
    Box(
        Modifier.fillMaxWidth()
            .heightIn(min = if (tablet) 155.dp else 128.dp)
            .clip(shape)
            .background(Palette.paper)
            .border(1.dp, Palette.line, shape)
            .padding(16.dp)
    ) {
        if (state.text.isEmpty())
            Text(
                tr(if (state.mode == CaptureMode.LIST) "listPlaceholder" else "captureQuestion"),
                style = style.copy(color = Color(0xFF879389)),
            )
        BasicTextField(
            state.text,
            state::updateText,
            Modifier.fillMaxWidth().focusRequester(focus),
            textStyle = style,
        )
    }
}

/**
 * `.voice-panel`: the recording dot and status, a waveform that moves while recording, the time,
 * and the round stop (or record again) button. No transcript: transcription is a P2 feature.
 */
@Composable
private fun VoicePanel(state: CaptureSheetState, onToggle: () -> Unit) {
    LaunchedEffect(state.recording) {
        while (state.recording) {
            state.refreshRecordingTime()
            delay(250)
        }
    }
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(Palette.paper)
            .border(1.dp, Palette.line, shape)
            .padding(start = 15.dp, end = 15.dp, top = 22.dp, bottom = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(17.dp).clip(CircleShape).background(Color(0x0BA94E43)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(7.dp)
                        .clip(CircleShape)
                        .background(if (state.recording) Palette.danger else Palette.muted)
                )
            }
            Text(
                tr(if (state.recording) "listening" else "recordingStopped"),
                style = TypeScale.overline.copy(fontWeight = TypeScale.button.fontWeight),
            )
        }
        Waveform(state.recording)
        if (state.text.isNotBlank())
            Text(
                state.text,
                style = TypeScale.body,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
        Text(
            num(clock(state.recordedSeconds)),
            style =
                TypeScale.overline.copy(
                    color = Palette.muted,
                    fontWeight = TypeScale.small.fontWeight,
                ),
        )
        val label = tr(if (state.recording) "stopRecord" else "record")
        Box(
            Modifier.padding(top = 12.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Palette.white)
                .border(1.dp, Palette.line, CircleShape)
                .clickable(onClick = onToggle)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(
                if (state.recording) R.drawable.ic_lucide_square else R.drawable.ic_lucide_mic,
                tint = Palette.danger,
                size = 20.dp,
            )
        }
    }
}

/**
 * `.waveform`: 30 bars, 4dp wide, gently moving while recording (left to right in any language).
 */
@Composable
private fun Waveform(active: Boolean) {
    val heights =
        listOf(
            13,
            19,
            29,
            43,
            25,
            56,
            37,
            24,
            47,
            64,
            31,
            45,
            23,
            58,
            71,
            42,
            27,
            50,
            33,
            64,
            41,
            23,
            39,
            57,
            28,
            17,
            32,
            47,
            25,
            18,
        )
    val wave = rememberInfiniteTransition(label = "wave")
    val phase by
        wave.animateFloat(
            0f,
            1f,
            infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label = "phase",
        )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier.height(74.dp).padding(vertical = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            heights.forEachIndexed { i, h ->
                // Each bar breathes between 55% and 100% of its height, out of step with its
                // neighbours.
                val k = if (active) 0.55f + 0.45f * (((phase + i * 0.075f) % 1f)) else 1f
                Box(
                    Modifier.width(4.dp)
                        .height((h * k).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Palette.green900.copy(alpha = 0.75f))
                )
            }
        }
    }
}

/** Files, photos and recordings already attached to this capture. */
@Composable
private fun Attachments(state: CaptureSheetState, lang: String) {
    if (state.attachments.isEmpty()) return
    Text(
        tr("attachments", state.attachments.size) +
            ": " +
            state.attachments.joinToString { attachmentLabel(it, lang) },
        style = TypeScale.tiny.copy(color = Palette.green900),
        modifier = Modifier.padding(top = 10.dp),
    )
}

/** `.smart-label` and `.smart-tags`: what the text rules noticed, each removable. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetectedTags(state: CaptureSheetState, lang: String) {
    Row(
        Modifier.padding(top = 13.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LucideIcon(R.drawable.ic_lucide_sparkles, tint = Palette.muted, size = 14.dp)
        Text(tr("detectedDetails"), style = TypeScale.tiny)
    }
    val tags = state.tags(lang)
    FlowRow(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (tags.isEmpty())
            Text(
                tr("noTagsHint"),
                style = TypeScale.small.copy(color = Palette.muted),
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        tags.forEach { tag ->
            val label = tr("removeTag", tag)
            Row(
                Modifier.heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.sage100)
                    .clickable { state.removeTag(tag) }
                    .semantics { contentDescription = label }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(tag, style = TypeScale.tiny.copy(color = Palette.ink))
                LucideIcon(R.drawable.ic_lucide_x, tint = Palette.muted, size = 12.dp)
            }
        }
    }
}

/** `.capture-bottom .icon-btn`: 52dp, paper fill, line border. */
@Composable
private fun SoftSquare(icon: Int, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        Modifier.size(52.dp)
            .clip(shape)
            .background(Palette.paper)
            .border(1.dp, Palette.line, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        LucideIcon(icon)
    }
}

/** The other capture kinds (CAP01): a list, a photo, a video or files. */
@Composable
private fun OtherKinds(state: CaptureSheetState) {
    var open by remember { mutableStateOf(false) }
    val takePhoto =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            state.onCameraResult(it, "image/jpeg")
        }
    val takeVideo =
        rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) {
            state.onCameraResult(it, "video/mp4")
        }
    val pickFiles =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments(),
            state::onFilesPicked,
        )
    Box {
        SoftSquare(R.drawable.ic_lucide_plus, tr("otherKinds")) { open = true }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(
                { Text(tr("list"), style = TypeScale.small) },
                {
                    open = false
                    state.stopRecording()
                    state.mode = CaptureMode.LIST
                },
            )
            DropdownMenuItem(
                { Text(tr("takePhoto"), style = TypeScale.small) },
                {
                    open = false
                    takePhoto.launch(state.newCameraFile("jpg"))
                },
            )
            DropdownMenuItem(
                { Text(tr("recordVideo"), style = TypeScale.small) },
                {
                    open = false
                    takeVideo.launch(state.newCameraFile("mp4"))
                },
            )
            DropdownMenuItem(
                { Text(tr("chooseFile"), style = TypeScale.small) },
                {
                    open = false
                    pickFiles.launch(arrayOf("*/*"))
                },
            )
        }
    }
}

/**
 * Owns the microphone permission request and hands [content] a `record()` that starts recording
 * (asking first if needed). Holding the capture button starts recording at once ([autoRecord]).
 */
@Composable
private fun Recorder(
    state: CaptureSheetState,
    autoRecord: Boolean,
    onAutoRecordStarted: () -> Unit,
    content: @Composable (record: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val askMic =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) state.startRecording() else state.onMicPermissionDenied()
        }
    val record = {
        if (!state.recording) {
            if (hasPermission(context, Manifest.permission.RECORD_AUDIO)) state.startRecording()
            else askMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    LaunchedEffect(autoRecord) {
        if (!autoRecord) return@LaunchedEffect
        onAutoRecordStarted()
        record()
    }
    content(record)
}

private fun hasPermission(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/** Kept for the tags shown and saved: what the rules notice, minus what the user removed. */
fun CaptureSheetState.tags(lang: String): List<String> =
    CaptureTags.detect(text, lang) - removedTags.toSet()
