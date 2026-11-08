package ai.humanwonder.poc.ui

import ai.humanwonder.poc.ChatMessage
import ai.humanwonder.poc.Perspective
import ai.humanwonder.poc.R
import ai.humanwonder.poc.SkyState
import ai.humanwonder.poc.Tone
import ai.humanwonder.poc.UiState
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.min

/** The three suggestions from the Stitch "Talk to Space" screen. */
private val SUGGESTIONS = listOf(
    "What are you seeing over Liwa dunes right now?",
    "How do city lights look from 400km up?",
    "Can you spot our campfire in the desert?",
)

private const val SATELLITE = "Orbit 412"

/** What Orbit 412 says while your question travels up (one per answer, in turn). */
private val LISTENING = listOf(
    "Receiving you across the dark…",
    "Your words are climbing 412 km…",
    "Turning the antenna your way…",
    "Listening across the dark…",
)

/**
 * The conversation with Orbit. Calm and readable first (after the Gemini app and the Stitch
 * home screen): true black, an aurora that rises around the composer when the sky is empty and
 * sinks low once you talk, plain off-white answers that fade in word by word.
 */
@Composable
fun ChatScreen(
    state: UiState,
    input: String,
    onInput: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onNewConversation: () -> Unit,
    onMenu: () -> Unit,
    onPerspective: (Perspective) -> Unit,
    onCycleTone: () -> Unit,
    onMic: () -> Unit,
    onRetry: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var uplinks by remember { mutableIntStateOf(0) }
    val ready = state.sky == SkyState.Ready
    val empty = state.messages.isEmpty()
    val calm by animateFloatAsState(if (empty) 0f else 1f, tween(1400, easing = FastOutSlowInEasing), label = "calm")

    val send: (String) -> Unit = { text ->
        if (text.isNotBlank() && ready && !state.generating) {
            if (state.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            uplinks++
            onSend(text)
            onInput("")
        }
    }

    // A soft tap when Orbit finishes an answer.
    val view = LocalView.current
    var wasGenerating by remember { mutableStateOf(false) }
    LaunchedEffect(state.generating) {
        if (state.haptics && wasGenerating && !state.generating && state.messages.lastOrNull()?.fromUser == false) {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
            )
        }
        wasGenerating = state.generating
    }

    Box(Modifier.fillMaxSize().background(HW.Space)) {
        GalaxyBackground(Modifier.fillMaxSize(), energy = if (state.generating) 1f else 0f, calm = calm, pulseKey = uplinks)
        DotGrid(Modifier.fillMaxSize())
        if (calm < 0.98f) SatelliteArc(Modifier.fillMaxSize(), visibility = 1f - calm, arcBelowTop = 96.dp)
        // Keeps the newest lines crisp where the low aurora meets the conversation.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 120.dp)
                .height(200.dp)
                .graphicsLayer { alpha = calm }
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))),
        )

        // The safe area (everything above the keyboard when it is open) decides what fits. The
        // input bar always stays whole; around it the tall empty-sky composer shrinks first, then
        // the suggestions and the note step aside, and the headline gives up its sub-line (Hero).
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val roomy = maxHeight >= 600.dp
            val tight = maxHeight < 440.dp
            Column(Modifier.fillMaxSize()) {
                ChatHeader(state, onMenu, onNewConversation)
                val failed = state.sky as? SkyState.Failed
                if (failed != null) {
                    LostSignal(failed.message, onRetry, Modifier.weight(1f))
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        AnimatedContent(
                            targetState = empty,
                            transitionSpec = { fadeIn(tween(500, delayMillis = 150)) togetherWith fadeOut(tween(250)) },
                            label = "chat-mode",
                        ) { isEmpty ->
                            if (isEmpty) Hero(state.name) else Conversation(state)
                        }
                    }
                    Composer(state, input, onInput, send, onStop, onPerspective, onCycleTone, onMic, large = empty && roomy)
                    if (empty && !tight) {
                        Suggestions(enabled = ready, onPick = send)
                        Spacer(Modifier.weight(0.45f))
                    }
                }
                if (!tight) {
                    Text(
                        "Human Wonder’s Orbit 412 sees far, but it can still be wrong.",
                        color = HW.InkTertiary,
                        style = HWType.ChatMeta,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 14.dp),
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------- header

@Composable
private fun ChatHeader(state: UiState, onMenu: () -> Unit, onNew: () -> Unit) {
    val last = state.messages.lastOrNull()
    val (status, busy, color) = when (val sky = state.sky) {
        is SkyState.Aligning -> Triple("Orbit 412 is rising over the horizon, ${(sky.progress * 100).toInt()}%", true, HW.Gold)
        SkyState.Waking -> Triple("Orbit 412 is finding your signal…", true, HW.Gold)
        is SkyState.Failed -> Triple("Orbit 412's signal is lost", false, HW.Error)
        SkyState.Ready -> when {
            state.generating && (last == null || last.text.isEmpty()) -> Triple("Orbit 412 is receiving you…", true, HW.Primary)
            state.generating -> Triple("Orbit 412 is transmitting…", true, HW.Primary)
            else -> Triple("Orbit 412 is overhead now", false, HW.Primary)
        }
    }
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(R.drawable.ic_menu, "Open menu", HW.IconMuted, size = 48, iconSize = 24, onClick = onMenu)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            BrandLockup(markSize = 20.dp, textSize = 16.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (busy) PulsingDot(color, size = 6.dp, periodMs = 900)
                else Box(Modifier.size(6.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text(status, color = HW.InkSecondary, style = HWType.ChatMeta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconCircle(R.drawable.ic_add, "New conversation", HW.IconMuted, size = 48, iconSize = 22, onClick = onNew)
    }
}

// --------------------------------------------------------------------------------- empty sky

@Composable
private fun Hero(name: String) {
    // Centred above the composer. With little room (keyboard open) it drops its sub-line, then
    // itself, instead of drawing over the header or the input.
    FitColumn(Modifier.fillMaxSize().padding(horizontal = 28.dp), gap = 12.dp, centered = true) {
        Text("Talk to the\ninfinite sky", color = Color.White, style = HWType.ChatHero.capScale(), textAlign = TextAlign.Center)
        Text(
            "Orbit 412 here, ${name.ifBlank { "Wanderer" }}, your Human Wonder satellite. I'm passing over you right now. What should I look at?",
            color = HW.InkSecondary,
            style = HWType.ChatSub,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
    }
}

@Composable
private fun Suggestions(enabled: Boolean, onPick: (String) -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        items(SUGGESTIONS) { s ->
            Row(
                Modifier
                    .widthIn(max = 250.dp)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .clickable(enabled = enabled) { onPick(s) }
                    .padding(start = 12.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HwIcon(R.drawable.ic_sparkle, HW.Primary, 16.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    s, color = HW.Chip, style = HWType.ChatLabel.copy(fontWeight = null),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// --------------------------------------------------------------------------------- conversation

@Composable
private fun Conversation(state: UiState) {
    val listState = rememberLazyListState()
    val newestFirst = state.messages.asReversed()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(0)
    }
    LazyColumn(
        state = listState,
        reverseLayout = true, // newest at the bottom; stays pinned there while the answer streams
        modifier = Modifier
            .fillMaxSize()
            // Older messages fade out under the header instead of being cut off.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, min(0.5f, 40.dp.toPx() / size.height) to Color.Black, 1f to Color.Black),
                    blendMode = BlendMode.DstIn,
                )
            },
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(newestFirst, key = { it.id }) { m ->
            val appear = remember(m.id) { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(
                visibleState = appear,
                enter = fadeIn(tween(450)) + slideInVertically(tween(450, easing = FastOutSlowInEasing)) { it / 4 },
            ) {
                if (m.fromUser) UserBubble(m) else OrbitAnswer(m, streaming = !m.complete)
            }
        }
    }
}

@Composable
private fun UserBubble(m: ChatMessage) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        SelectionContainer {
            Text(
                m.text,
                color = HW.Ink,
                style = HWType.ChatInput,
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp))
                    .background(HW.Bubble)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun OrbitAnswer(m: ChatMessage, streaming: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            OrbitMark(24.dp, active = streaming)
            Spacer(Modifier.width(8.dp))
            Text(SATELLITE, color = HW.InkSecondary, style = HWType.ChatLabel)
        }
        if (streaming && m.text.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            ShimmerText(LISTENING[Math.floorMod(m.id.hashCode(), LISTENING.size)])
        }
        if (m.text.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            SelectionContainer { Text(inkText(m.text, streaming), style = HWType.ChatBody) }
        }
        if (m.complete && m.text.isNotEmpty()) CopyAction(m.text)
    }
}

/** Plain, readable answer text; the newest characters fade in as they arrive (no glow). */
private fun inkText(text: String, streaming: Boolean): AnnotatedString = buildAnnotatedString {
    if (!streaming) {
        withStyle(SpanStyle(color = HW.InkAnswer)) { append(text) }
        return@buildAnnotatedString
    }
    val tail = min(18, text.length)
    val settled = text.length - tail
    withStyle(SpanStyle(color = HW.InkAnswer)) { append(text.substring(0, settled)) }
    for (i in 0 until tail) {
        val fresh = (i + 1).toFloat() / tail // newest characters are the faintest
        withStyle(SpanStyle(color = HW.InkAnswer.copy(alpha = 1f - 0.75f * fresh))) { append(text[settled + i]) }
    }
}

/** "Thinking" label with a light sweeping across it, like Gemini's. */
@Composable
private fun ShimmerText(text: String) {
    val t = rememberInfiniteTransition(label = "shimmer")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "p")
    Text(
        text,
        color = HW.InkTertiary,
        style = HWType.ChatBody,
        modifier = Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val w = size.width
                val start = (-1.3f + 1.6f * p) * w
                drawRect(
                    Brush.horizontalGradient(
                        0f to HW.InkTertiary, 0.4f to HW.InkTertiary, 0.5f to Color(0xFFF1F4F4),
                        0.6f to HW.InkTertiary, 1f to HW.InkTertiary,
                        startX = start, endX = start + 2 * w,
                    ),
                    blendMode = BlendMode.SrcIn,
                )
            },
    )
}

@Composable
private fun CopyAction(text: String) {
    val context = LocalContext.current
    var copied by remember(text) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    Row(Modifier.padding(top = 6.dp).offset(x = (-10).dp), verticalAlignment = Alignment.CenterVertically) {
        IconCircle(
            if (copied) R.drawable.ic_check else R.drawable.ic_copy,
            if (copied) "Copied" else "Copy answer",
            if (copied) HW.Primary else HW.InkSecondary,
            size = 40, iconSize = 18,
        ) {
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText("Orbit 412, Human Wonder", "$text\n\n— Orbit 412, Human Wonder"))
            copied = true
        }
        if (copied) Text("Copied", color = HW.InkSecondary, style = HWType.ChatMeta)
    }
}

@Composable
private fun LostSignal(message: String, onRetry: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OrbitMark(64.dp, active = false)
        Spacer(Modifier.height(20.dp))
        Text("The signal is lost.", color = HW.Ink, style = HWType.HeadlineMobile.capScale(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, color = HW.InkSecondary, style = HWType.ChatSub, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Try again", modifier = Modifier.widthIn(max = 280.dp), onClick = onRetry)
    }
}

// --------------------------------------------------------------------------------- composer

@Composable
private fun Composer(
    state: UiState,
    input: String,
    onInput: (String) -> Unit,
    send: (String) -> Unit,
    onStop: () -> Unit,
    onPerspective: (Perspective) -> Unit,
    onCycleTone: () -> Unit,
    onMic: () -> Unit,
    large: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    val ready = state.sky == SkyState.Ready
    val minHeight by animateDpAsState(if (large) 96.dp else 26.dp, tween(400), label = "composer")
    val border by animateColorAsState(if (focused) HW.Primary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f), label = "border")
    val shape = RoundedCornerShape(28.dp)
    val placeholder = when {
        !ready -> "Orbit 412 is rising over the horizon…"
        state.generating -> "Orbit 412 is transmitting…"
        else -> "Ask Orbit 412 anything"
    }

    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                // The Stitch prompt box (GlassBrush), the surface every screen shares.
                .glass(shape, border)
                .padding(start = 12.dp, end = 8.dp, top = 14.dp, bottom = 8.dp),
        ) {
            Box(Modifier.fillMaxWidth().heightIn(min = minHeight).padding(horizontal = 4.dp)) {
                if (input.isEmpty()) {
                    Text(placeholder, color = HW.InkTertiary, style = HWType.ChatInput)
                }
                BasicTextField(
                    value = input,
                    onValueChange = onInput,
                    textStyle = HWType.ChatInput.copy(color = Color.White),
                    cursorBrush = SolidColor(HW.Primary),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(input) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { contentDescription = "Message to Orbit" },
                )
            }
            // Mic and send are fixed 48dp targets that never shrink. If the row is too narrow,
            // the tone label goes first (level 1), then the Orbit/Ground labels (level 2).
            ComposerRow(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                flexible = { level ->
                    PerspectiveSwitch(state.perspective, labels = level < 2, onSelect = onPerspective)
                    ToneChip(state.tone, compact = level >= 1, onClick = onCycleTone)
                },
                actions = {
                    IconCircle(R.drawable.ic_mic, "Voice input", HW.IconMuted, size = 48, iconSize = 20, onClick = onMic)
                    SendButton(
                        generating = state.generating,
                        canSend = ready && input.isNotBlank(),
                        onClick = { if (state.generating) onStop() else send(input) },
                    )
                },
            )
        }
    }
}

private enum class ComposerSlot { Actions, Level0, Level1, Level2 }

/**
 * The composer's control row, measured instead of guessed: [actions] (mic, send) are measured at
 * their natural size and pinned to the end; [flexible] is composed at collapse level 0, and
 * again at level 1 and 2 only while it does not fit in the width that is left.
 */
@Composable
private fun ComposerRow(
    modifier: Modifier,
    flexible: @Composable (level: Int) -> Unit,
    actions: @Composable () -> Unit,
    gap: Dp = 4.dp,
) {
    SubcomposeLayout(modifier) { constraints ->
        val gapPx = gap.roundToPx()
        val natural = Constraints()
        fun widthOf(items: List<Placeable>): Int =
            items.sumOf { it.width } + gapPx * (items.size - 1).coerceAtLeast(0)

        val actionItems = subcompose(ComposerSlot.Actions, actions).map { it.measure(natural) }
        val actionsWidth = widthOf(actionItems)
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val room = maxWidth - actionsWidth - gapPx

        var level = 0
        var flexItems = subcompose(ComposerSlot.Level0) { flexible(0) }.map { it.measure(natural) }
        while (widthOf(flexItems) > room && level < 2) {
            level++
            val slot = if (level == 1) ComposerSlot.Level1 else ComposerSlot.Level2
            val current = level
            flexItems = subcompose(slot) { flexible(current) }.map { it.measure(natural) }
        }

        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else widthOf(flexItems) + gapPx + actionsWidth
        val height = maxOf(
            actionItems.maxOfOrNull { it.height } ?: 0,
            flexItems.maxOfOrNull { it.height } ?: 0,
            constraints.minHeight,
        )
        layout(width, height) {
            var x = 0
            flexItems.forEach { item ->
                item.placeRelative(x, (height - item.height) / 2)
                x += item.width + gapPx
            }
            var ax = width - actionsWidth
            actionItems.forEach { item ->
                item.placeRelative(ax, (height - item.height) / 2)
                ax += item.width + gapPx
            }
        }
    }
}

/**
 * Orbit / Ground: the selected side shows its name, the other only its icon. With [labels] off
 * (narrow composer) both are icon-only segments with 10dp padding.
 */
@Composable
private fun PerspectiveSwitch(selected: Perspective, labels: Boolean, onSelect: (Perspective) -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
            .padding(2.dp),
    ) {
        Perspective.entries.forEach { p ->
            val on = p == selected
            val showLabel = on && labels
            Row(
                Modifier
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(if (on) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(p) }
                    .semantics { contentDescription = "${p.label} view" }
                    .animateContentSize()
                    .padding(start = 10.dp, end = if (showLabel) 14.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HwIcon(
                    if (p == Perspective.Orbit) R.drawable.ic_satellite else R.drawable.ic_location,
                    if (on && p == Perspective.Orbit) HW.Primary else if (on) Color.White else HW.InkSecondary,
                    16.dp,
                )
                if (showLabel) {
                    Spacer(Modifier.width(6.dp))
                    Text(p.label, color = Color.White, style = HWType.ChatLabel)
                }
            }
        }
    }
}

@Composable
private fun ToneChip(tone: Tone, compact: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Tone: ${tone.label}" }
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HwIcon(R.drawable.ic_sparkle, HW.Ink, 16.dp)
        if (!compact) {
            Spacer(Modifier.width(6.dp))
            Text(tone.label, color = HW.Ink, style = HWType.ChatLabel, maxLines = 1)
        }
        Spacer(Modifier.width(6.dp))
        HwIcon(R.drawable.ic_expand_more, HW.InkSecondary, 16.dp)
    }
}

@Composable
private fun IconCircle(
    icon: Int,
    description: String,
    tint: Color,
    size: Int,
    iconSize: Int,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { HwIcon(icon, tint, iconSize.dp, contentDescription = description) }
}
