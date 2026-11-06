package ai.humanwonder.poc.ui

import ai.humanwonder.poc.ChatMessage
import ai.humanwonder.poc.Perspective
import ai.humanwonder.poc.R
import ai.humanwonder.poc.SkyState
import ai.humanwonder.poc.UiState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min

/** The three suggestions from the Stitch "Talk to Space" screen. */
private val SUGGESTIONS = listOf(
    "What are you seeing over Liwa dunes right now?",
    "How do city lights look from 400km up?",
    "Can you spot our campfire in the desert?",
)

private const val SATELLITE = "Orbit 412"

/** Stitch screen 3, "Human Wonder — Talk to Space (Live Galaxy)", brought to life. */
@Composable
fun ChatScreen(
    state: UiState,
    input: String,
    onInput: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onNewConversation: () -> Unit,
    onBack: () -> Unit,
    onPerspective: (Perspective) -> Unit,
    onCycleTone: () -> Unit,
    onMic: () -> Unit,
    onRetry: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var uplinks by remember { mutableIntStateOf(0) }
    val ready = state.sky == SkyState.Ready
    val last = state.messages.lastOrNull()
    val receiving = state.generating && last != null && !last.fromUser && last.text.isNotEmpty()

    val send: (String) -> Unit = { text ->
        if (text.isNotBlank() && ready && !state.generating) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            uplinks++
            onSend(text)
            onInput("")
        }
    }

    Box(Modifier.fillMaxSize()) {
        // 1. The living sky: Stitch's galaxy shader, brighter while the sky is answering.
        GalaxyBackground(Modifier.fillMaxSize(), energy = if (state.generating) 1f else 0f, pulseKey = uplinks)
        // 2. The orbit: a satellite crossing the top of the sky; questions fly up to it.
        OrbitLayer(Modifier.fillMaxSize(), transmitting = state.generating, receiving = receiving, launches = uplinks)

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            ChatHeader(state, onBack)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                val failed = state.sky as? SkyState.Failed
                when {
                    failed != null -> LostSignal(failed.message, onRetry)
                    else -> AnimatedContent(
                        targetState = state.messages.isEmpty(),
                        transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(250)) },
                        label = "chat-mode",
                    ) { empty ->
                        if (empty) {
                            EmptySky(state, input, onInput, send, onStop, onNewConversation, onPerspective, onCycleTone, onMic)
                        } else {
                            Conversation(state, input, onInput, send, onStop, onNewConversation, onPerspective, onCycleTone, onMic)
                        }
                    }
                }
            }

            ChatFooter(state)
        }
    }
}

// --------------------------------------------------------------------------------- header/footer

@Composable
private fun ChatHeader(state: UiState, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            HwIcon(R.drawable.ic_arrow_back, HW.OnSurface.copy(alpha = 0.75f), 20.dp)
        }
        Spacer(Modifier.weight(1f))
        GlassPill {
            Text("Human Wonder", color = Color.White.copy(alpha = 0.9f), style = HWType.BodySm.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.025.em))
            Text("✦", color = HW.Primary, fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        StatusPill(state)
    }
}

@Composable
private fun StatusPill(state: UiState) {
    val last = state.messages.lastOrNull()
    val (label, color) = when (val sky = state.sky) {
        is SkyState.Aligning -> "Aligning ${(sky.progress * 100).toInt()}%" to HW.Gold
        SkyState.Waking -> "Acquiring" to HW.Gold
        is SkyState.Failed -> "No signal" to HW.Error
        SkyState.Ready -> when {
            state.generating && (last == null || last.text.isEmpty()) -> "Uplink" to HW.Emerald300
            state.generating -> "Downlink" to HW.Emerald300
            else -> SATELLITE to HW.Emerald300
        }
    }
    GlassPill(fill = color.copy(alpha = 0.10f), border = color.copy(alpha = 0.22f)) {
        PulsingDot(color, periodMs = if (state.generating) 700 else 1600)
        MonoLabel(label, color = color)
    }
}

@Composable
private fun ChatFooter(state: UiState) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(20_000)
        }
    }
    val tz = TimeZone.getDefault()
    val zone = tz.getDisplayName(tz.inDaylightTime(now), TimeZone.SHORT, Locale.US)
    val time = SimpleDateFormat("hh:mm a", Locale.US).format(now)
    val left = when {
        state.generating -> "Receiving transmission"
        state.sky is SkyState.Aligning || state.sky == SkyState.Waking -> "Tuning the receiver"
        else -> "Alt 412 km · 7.66 km/s"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(HW.Primary.copy(alpha = 0.6f)))
        Spacer(Modifier.width(8.dp))
        MonoLabel(left, color = HW.Outline.copy(alpha = 0.7f), style = HWType.LabelSm.copy(letterSpacing = 0.14.em))
        Spacer(Modifier.weight(1f))
        MonoLabel("$zone $time", color = HW.Outline.copy(alpha = 0.7f))
    }
}

// --------------------------------------------------------------------------------- empty state

@Composable
private fun EmptySky(
    state: UiState,
    input: String,
    onInput: (String) -> Unit,
    send: (String) -> Unit,
    onStop: () -> Unit,
    onNew: () -> Unit,
    onPerspective: (Perspective) -> Unit,
    onCycleTone: () -> Unit,
    onMic: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            "Talk to the\ninfinite sky",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = HWType.BodyMd.copy(
                fontSize = 34.sp, lineHeight = 37.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.03).em,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.name.isNotBlank()) "${state.name}, ask orbit anything about what it sees, hears, or feels tonight."
            else "Ask orbit anything about what it sees, hears, or feels tonight.",
            color = HW.Muted.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            style = HWType.BodyMd.copy(fontSize = 13.5.sp, lineHeight = 22.sp),
            modifier = Modifier.widthIn(max = 280.dp),
        )
        Spacer(Modifier.height(32.dp))
        PromptBox(state, input, onInput, send, onStop, onNew, onPerspective, onCycleTone, onMic, large = true)
        Spacer(Modifier.height(16.dp))
        LazyRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(SUGGESTIONS) { s ->
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                        .clickable { send(s) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✦", color = HW.Primary, fontSize = 11.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(s, color = HW.OnSurfaceVariant, style = HWType.BodySm.copy(fontSize = 11.5.sp))
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

// --------------------------------------------------------------------------------- conversation

@Composable
private fun Conversation(
    state: UiState,
    input: String,
    onInput: (String) -> Unit,
    send: (String) -> Unit,
    onStop: () -> Unit,
    onNew: () -> Unit,
    onPerspective: (Perspective) -> Unit,
    onCycleTone: () -> Unit,
    onMic: () -> Unit,
) {
    val listState = rememberLazyListState()
    val newestFirst = state.messages.asReversed()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(0)
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            reverseLayout = true, // newest at the bottom; stays pinned there while the answer streams
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // Older messages dissolve into the sky at the top edge.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Transparent, 0.16f to Color.Black, 1f to Color.Black),
                        blendMode = BlendMode.DstIn,
                    )
                },
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 96.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            items(newestFirst, key = { it.id }) { m ->
                val appear = remember(m.id) { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = appear,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 3 },
                ) {
                    if (m.fromUser) Uplink(m) else Transmission(m, streaming = !m.complete)
                }
            }
        }
        Box(Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp)) {
            PromptBox(state, input, onInput, send, onStop, onNew, onPerspective, onCycleTone, onMic, large = false)
        }
    }
}

private val clock = SimpleDateFormat("HH:mm", Locale.US)

/** The person's question, sent up to orbit. */
@Composable
private fun Uplink(m: ChatMessage) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        SelectionContainer {
            Text(
                m.text,
                color = Color.White.copy(alpha = 0.92f),
                style = HWType.BodyMd.copy(fontSize = 15.5.sp, lineHeight = 23.sp),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                    .background(
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                    .padding(horizontal = 16.dp, vertical = 11.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        MonoLabel("Uplink ▴ ${clock.format(Date(m.time))}", color = HW.Outline.copy(alpha = 0.55f), style = HWType.LabelSm.copy(fontSize = 9.sp))
    }
}

/** The sky's reply, arriving as a radio transmission from orbit. */
@Composable
private fun Transmission(m: ChatMessage, streaming: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OrbitMark(22.dp, active = streaming)
            Spacer(Modifier.width(8.dp))
            MonoLabel("$SATELLITE · ${clock.format(Date(m.time))}", color = HW.Primary.copy(alpha = 0.85f))
            Spacer(Modifier.width(10.dp))
            SignalWave(active = streaming, modifier = Modifier.width(46.dp).height(14.dp))
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    // The signal line the words travel down.
                    drawLine(
                        Brush.verticalGradient(listOf(HW.PrimaryContainer.copy(alpha = 0.7f), HW.PrimaryContainer.copy(alpha = 0.0f))),
                        start = Offset(1.dp.toPx(), 0f), end = Offset(1.dp.toPx(), size.height),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }
                .padding(start = 16.dp, end = 8.dp),
        ) {
            if (m.text.isEmpty() && streaming) {
                AcquiringSignal()
            } else {
                SelectionContainer { Text(glowingText(m.text, streaming), style = HWType.Transmission) }
            }
        }
        val endShown = m.complete && m.text.isNotEmpty()
        val endAlpha by animateFloatAsState(if (endShown) 1f else 0f, tween(900), label = "end")
        if (endShown) {
            Spacer(Modifier.height(10.dp))
            MonoLabel(
                "— End of transmission",
                color = HW.Outline.copy(alpha = 0.5f * endAlpha),
                style = HWType.LabelSm.copy(fontSize = 9.sp),
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

/**
 * Starlight-cream words; the newest characters arrive glowing aurora-teal and cool down as more
 * text streams in, followed by a soft blinking carrier.
 */
@Composable
private fun glowingText(text: String, streaming: Boolean): AnnotatedString {
    val blink = if (streaming) {
        rememberInfiniteTransition(label = "caret").animateFloat(
            initialValue = 1f, targetValue = 0.15f,
            animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "c",
        ).value
    } else {
        0f
    }
    return buildAnnotatedString {
        if (!streaming) {
            withStyle(SpanStyle(color = HW.Starlight)) { append(text) }
        } else {
            val tail = min(28, text.length)
            val settled = text.length - tail
            withStyle(SpanStyle(color = HW.Starlight)) { append(text.substring(0, settled)) }
            for (i in 0 until tail) {
                val heat = (i + 1).toFloat() / tail // newest characters are the hottest
                withStyle(
                    SpanStyle(
                        color = lerp(HW.Starlight, HW.Primary, heat),
                        shadow = Shadow(HW.PrimaryContainer.copy(alpha = 0.55f * heat), blurRadius = 14f * heat),
                    ),
                ) { append(text[settled + i]) }
            }
            withStyle(SpanStyle(color = HW.Primary.copy(alpha = blink))) { append(" ▍") }
        }
    }
}

@Composable
private fun AcquiringSignal() {
    val t = rememberInfiniteTransition(label = "acq")
    val phase by t.animateFloat(0f, 3f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "p")
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 3) {
            val d = ((phase - i) % 3f + 3f) % 3f
            val a = if (d < 1f) 1f - d * 0.6f else 0.25f
            Box(Modifier.padding(end = 5.dp).size(5.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(HW.Primary))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            "Listening across the dark…",
            color = HW.Outline,
            style = HWType.Transmission.copy(fontSize = 16.sp, fontStyle = FontStyle.Italic),
        )
    }
}

@Composable
private fun LostSignal(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OrbitMark(64.dp, active = false)
        Spacer(Modifier.height(20.dp))
        Text("The signal is lost.", color = HW.OnSurface, style = HWType.HeadlineMobile, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, color = HW.OnSurfaceVariant, style = HWType.BodyMd, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        GlowButton("Try again", onClick = onRetry)
    }
}

// --------------------------------------------------------------------------------- prompt box

@Composable
private fun PromptBox(
    state: UiState,
    input: String,
    onInput: (String) -> Unit,
    send: (String) -> Unit,
    onStop: () -> Unit,
    onNew: () -> Unit,
    onPerspective: (Perspective) -> Unit,
    onCycleTone: () -> Unit,
    onMic: () -> Unit,
    large: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    val glow by animateFloatAsState(if (focused || state.generating) 0.18f else 0.08f, tween(500), label = "glow")
    val shape = RoundedCornerShape(26.dp)
    val placeholder = when {
        state.sky is SkyState.Aligning || state.sky == SkyState.Waking -> "Waking the sky…"
        state.generating -> "Orbit is answering…"
        else -> "What shall we ask the cosmos tonight?"
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 340.dp
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    // shadow-[0_20px_60px_-15px_rgba(0,0,0,0.8),0_0_24px_rgba(47,230,200,0.08)]
                    for (i in 1..5) {
                        val s = i * 5.dp.toPx()
                        drawRoundRect(
                            HW.PrimaryContainer.copy(alpha = glow / (i * 1.6f)),
                            topLeft = Offset(-s, -s), size = Size(size.width + 2 * s, size.height + 2 * s),
                            cornerRadius = CornerRadius(26.dp.toPx() + s), style = Stroke(5.dp.toPx()),
                        )
                    }
                }
                .clip(shape)
                .background(HW.PromptBox.copy(alpha = 0.78f))
                .border(1.dp, if (focused) HW.Primary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f), shape)
                .padding(14.dp),
        ) {
            Box(Modifier.fillMaxWidth().heightIn(min = if (large) 96.dp else 26.dp).padding(top = 4.dp, start = 4.dp, end = 4.dp)) {
                if (input.isEmpty()) {
                    Text(placeholder, color = HW.Outline.copy(alpha = 0.7f), style = HWType.BodyMd.copy(fontSize = 15.5.sp))
                }
                BasicTextField(
                    value = input,
                    onValueChange = onInput,
                    textStyle = HWType.BodyMd.copy(color = Color.White, fontSize = 15.5.sp),
                    cursorBrush = SolidColor(HW.Primary),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(input) }),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(R.drawable.ic_add, HW.Muted, onClick = onNew)
                Spacer(Modifier.width(4.dp))
                PerspectiveToggle(state.perspective, compact, onPerspective)
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape)
                        .clickable(onClick = onCycleTone)
                        .padding(start = 8.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✦", color = HW.Primary, fontSize = 10.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(state.tone.label, color = HW.OnSurface, style = HWType.BodySm.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium))
                    HwIcon(R.drawable.ic_expand_more, HW.Outline, 13.dp)
                }
                Spacer(Modifier.width(2.dp))
                RoundIcon(R.drawable.ic_mic, HW.Muted, onClick = onMic)
                Spacer(Modifier.width(4.dp))
                SendButton(generating = state.generating, onClick = { if (state.generating) onStop() else send(input) })
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: Int, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { HwIcon(icon, tint, 18.dp) }
}

@Composable
private fun PerspectiveToggle(selected: Perspective, compact: Boolean, onSelect: (Perspective) -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape)
            .padding(2.dp),
    ) {
        Perspective.entries.forEach { p ->
            val on = p == selected
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(if (on) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onSelect(p) }
                    .padding(horizontal = if (compact) 6.dp else 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HwIcon(
                    if (p == Perspective.Orbit) R.drawable.ic_satellite else R.drawable.ic_location,
                    if (on && p == Perspective.Orbit) HW.Primary else if (on) Color.White else HW.Muted,
                    13.dp,
                )
                if (!compact) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        p.label,
                        color = if (on) Color.White else HW.Muted,
                        style = HWType.BodySm.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    )
                }
            }
        }
    }
}

@Composable
private fun SendButton(generating: Boolean, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "send")
    val pulse by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "p")
    val glow = if (generating) pulse else 0.55f
    Box(
        Modifier
            .size(34.dp)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(HW.Send.copy(alpha = 0.55f * glow), Color.Transparent)),
                    radius = size.minDimension * 0.95f,
                )
            }
            .clip(CircleShape)
            .background(HW.Send)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        HwIcon(if (generating) R.drawable.ic_stop else R.drawable.ic_arrow_upward, HW.OnSend, 18.dp)
    }
}

// --------------------------------------------------------------------------------- the orbit

/**
 * Draws, above the galaxy and below the UI: a faint orbital arc across the top of the sky with
 * the satellite gliding along it; signal rings and a soft downlink beam while it transmits; and
 * a spark that flies up from the prompt to the satellite each time a question is sent.
 */
@Composable
private fun OrbitLayer(modifier: Modifier, transmitting: Boolean, receiving: Boolean, launches: Int) {
    val density = LocalDensity.current
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    val measurer = rememberTextMeasurer()

    val t = rememberInfiniteTransition(label = "orbit")
    val travel by t.animateFloat(0f, 1f, infiniteRepeatable(tween(75_000, easing = LinearEasing)), label = "travel")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "ring")
    val flicker by t.animateFloat(0.7f, 1f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "flicker")
    val beam by animateFloatAsState(if (receiving) 1f else if (transmitting) 0.45f else 0f, tween(800), label = "beam")

    val launch = remember { Animatable(1f) }
    LaunchedEffect(launches) {
        if (launches > 0) {
            launch.snapTo(0f)
            launch.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
        }
    }

    Canvas(modifier) {
        val w = size.width
        val arcY = with(density) { (top + 70.dp).toPx() }
        val drop = with(density) { 78.dp.toPx() }
        // Quadratic Bézier: a shallow arc, as if part of a huge ellipse around the Earth.
        val p0 = Offset(-0.12f * w, arcY + drop)
        val p1 = Offset(0.5f * w, arcY - drop)
        val p2 = Offset(1.12f * w, arcY + drop)
        fun at(u: Float): Offset {
            val a = (1 - u) * (1 - u)
            val b = 2 * (1 - u) * u
            val c = u * u
            return Offset(a * p0.x + b * p1.x + c * p2.x, a * p0.y + b * p1.y + c * p2.y)
        }
        val arc = Path().apply {
            moveTo(p0.x, p0.y)
            quadraticTo(p1.x, p1.y, p2.x, p2.y)
        }
        val fade = Brush.horizontalGradient(
            0f to Color.Transparent, 0.25f to HW.Primary.copy(alpha = 0.28f),
            0.75f to HW.Primary.copy(alpha = 0.28f), 1f to Color.Transparent,
        )
        drawPath(arc, HW.PrimaryContainer.copy(alpha = 0.05f), style = Stroke(7.dp.toPx()))
        drawPath(arc, fade, style = Stroke(1.dp.toPx()))

        // Satellite position along the visible part of the arc.
        val u = 0.08f + 0.84f * travel
        val sat = at(u)

        // Downlink beam towards the person while words arrive.
        if (beam > 0.01f) {
            val beamH = size.height * 0.42f
            drawRect(
                Brush.verticalGradient(
                    listOf(HW.PrimaryContainer.copy(alpha = 0.16f * beam * flicker), Color.Transparent),
                    startY = sat.y, endY = sat.y + beamH,
                ),
                topLeft = Offset(sat.x - 10.dp.toPx(), sat.y), size = Size(20.dp.toPx(), beamH),
            )
        }

        // Comet-like trail.
        for (k in 1..14) {
            val pt = at(max(0f, u - k * 0.004f))
            drawCircle(HW.Primary.copy(alpha = 0.32f * (1f - k / 15f)), radius = (2.2f - k * 0.1f).dp.toPx(), center = pt)
        }
        // Signal rings while transmitting.
        if (transmitting) {
            for (i in 0 until 3) {
                val ph = (ring + i / 3f) % 1f
                drawCircle(
                    HW.Primary.copy(alpha = 0.45f * (1f - ph)),
                    radius = (6f + ph * 34f).dp.toPx(), center = sat, style = Stroke(1.dp.toPx()),
                )
            }
        }
        // The satellite: glow, solar panels, bright core.
        drawCircle(
            Brush.radialGradient(listOf(HW.Primary.copy(alpha = if (transmitting) 0.65f else 0.4f), Color.Transparent), center = sat, radius = 18.dp.toPx()),
            radius = 18.dp.toPx(), center = sat,
        )
        val panel = 5.dp.toPx()
        drawLine(HW.Primary.copy(alpha = 0.8f), Offset(sat.x - panel - 3.dp.toPx(), sat.y), Offset(sat.x - 3.dp.toPx(), sat.y), 1.6.dp.toPx())
        drawLine(HW.Primary.copy(alpha = 0.8f), Offset(sat.x + 3.dp.toPx(), sat.y), Offset(sat.x + panel + 3.dp.toPx(), sat.y), 1.6.dp.toPx())
        drawCircle(Color(0xFFE9FFFA), radius = 2.4.dp.toPx(), center = sat)
        drawText(
            measurer, SATELLITE.uppercase(Locale.ROOT),
            topLeft = Offset(sat.x + 10.dp.toPx(), sat.y + 8.dp.toPx()),
            style = HWType.LabelSm.copy(fontSize = 8.sp, color = HW.Primary.copy(alpha = 0.6f)),
        )

        // Spark from the prompt box up to the satellite.
        val pr = launch.value
        if (pr < 1f) {
            val start = Offset(w * 0.86f, size.height - with(density) { (bottom + 96.dp).toPx() })
            fun spark(q: Float): Offset {
                val x = start.x + (sat.x - start.x) * q
                val y = start.y + (sat.y - start.y) * q
                val bow = kotlin.math.sin(Math.PI * q).toFloat() * w * 0.12f
                return Offset(x - bow, y)
            }
            for (k in 0..10) {
                val q = max(0f, pr - k * 0.018f)
                drawCircle(HW.Primary.copy(alpha = 0.5f * (1f - k / 11f) * (1f - pr * 0.3f)), radius = (3f - k * 0.2f).dp.toPx(), center = spark(q))
            }
            val head = spark(pr)
            drawCircle(Brush.radialGradient(listOf(HW.Primary.copy(alpha = 0.8f), Color.Transparent), center = head, radius = 14.dp.toPx()), radius = 14.dp.toPx(), center = head)
            if (pr > 0.85f) {
                val f = (pr - 0.85f) / 0.15f
                drawCircle(HW.Primary.copy(alpha = 0.6f * (1f - f)), radius = (8f + 40f * f).dp.toPx(), center = sat, style = Stroke(1.5.dp.toPx()))
            }
        }
    }
}
