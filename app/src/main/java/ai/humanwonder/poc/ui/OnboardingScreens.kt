package ai.humanwonder.poc.ui

import ai.humanwonder.poc.R
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import kotlinx.coroutines.launch

/**
 * Full-bleed photograph from the Stitch design, at full colour, breathing with a slow zoom.
 * [scrim] darkens only where text sits. (No dot grid over photos.)
 */
@Composable
private fun PhotoSky(@DrawableRes image: Int, scrim: Brush) {
    val t = rememberInfiniteTransition(label = "kenburns")
    val scale by t.animateFloat(
        initialValue = 1.0f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(24_000), RepeatMode.Reverse), label = "s",
    )
    Box(Modifier.fillMaxSize().background(HW.Space)) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale },
        )
        Box(Modifier.fillMaxSize().background(scrim))
    }
}

/** A headline and line of copy that rise into place once, when the screen opens. */
@Composable
private fun Rising(content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(1200)) + slideInVertically(tween(1200)) { it / 5 },
    ) { content() }
}

/**
 * The opening screen on every launch: the Liwa dunes under the Milky Way with Orbit 412 passing
 * over. No button: swipe up (or tap the chevron ascent) to continue. The first time that leads to
 * "What should Orbit 412 call you?"; once a profile exists the headline is "Welcome back, <name>."
 * and the swipe goes straight into the chat. [greetingName] is the saved name when the profile is
 * set up, otherwise null.
 */
@Composable
fun LandingScreen(greetingName: String?, onContinue: () -> Unit) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { 96.dp.toPx() }
    val maxPull = with(density) { 160.dp.toPx() }
    val pull = remember { Animatable(0f) }
    // The drag handler is set up once, so it calls the latest onContinue through this.
    // (No "already left" latch: navigation ignores a second trigger, and a latch could leave the
    // screen stuck after a quick Back from Name.)
    val continueNow by rememberUpdatedState(onContinue)
    val go: () -> Unit = { continueNow() }
    val label = if (greetingName != null) "Swipe up to talk to Orbit\u00A0412" else "Swipe up to begin"

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // The drag total lives here (not in the animated value, which updates a frame later).
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (-total > threshold) go()
                        total = 0f
                        scope.launch { pull.animateTo(0f, spring()) }
                    },
                    onDragCancel = {
                        total = 0f
                        scope.launch { pull.animateTo(0f, spring()) }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    total = (total + dragAmount).coerceIn(-maxPull, 0f)
                    scope.launch { pull.snapTo(total) }
                }
            },
    ) {
        PhotoSky(
            R.drawable.bg_liwa_dunes,
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.66f), 0.32f to Color.Black.copy(alpha = 0.22f),
                0.46f to Color.Transparent, 0.62f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f),
            ),
        )
        SatelliteArc(Modifier.fillMaxSize(), arcFraction = 0.45f, dropFraction = 0.06f, startAt = 0.18f)

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                // The words follow the finger a little, so the swipe feels physical.
                .graphicsLayer {
                    translationY = pull.value * 0.5f
                    alpha = 1f - (-pull.value / maxPull) * 0.4f
                },
        ) {
            Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                BrandLockup()
            }
            Rising {
                Column(
                    Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (greetingName != null) "Welcome back, $greetingName." else "The sky has something to tell you.",
                        color = Color.White, style = HWType.WelcomeHero.capScale(), textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (greetingName != null) {
                            "${greetingFor()}. Orbit\u00A0412 is overhead now, and it remembers your conversations."
                        } else {
                            "Human Wonder gives the sky a voice: Orbit\u00A0412, a satellite passing over you tonight."
                        },
                        color = HW.Ink.copy(alpha = 0.85f),
                        style = HWType.ChatSub.copy(lineBreak = LineBreak.Paragraph),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 310.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Ascent(label, onClick = go)
        }
    }
}

/**
 * The swipe hint ("ascent"): three thin chevrons that light up one after another from the bottom,
 * the top one in teal with a soft glow, over a label with a slow light sweep. No visible button.
 * Tapping it does what the swipe does. Same timing as the preview (2 s wave, 3.2 s sweep).
 */
@Composable
private fun Ascent(label: String, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "ascent")
    val phase by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "wave",
    )
    val sweep by t.animateFloat(
        // Left to right, like the preview's light sweep.
        initialValue = -0.6f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "sweep",
    )
    val dim = HW.Ink.copy(alpha = 0.62f)
    Column(
        Modifier
            .padding(bottom = 34.dp)
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Canvas(Modifier.size(width = 30.dp, height = 43.dp)) {
            val u = size.width / 30f // the chevron is drawn on a 30 x 13 grid
            for (i in 0..2) { // 0 = bottom
                val local = ((phase - i * 0.1f) % 1f + 1f) % 1f
                val lit = when {
                    local < 0.35f -> FastOutSlowInEasing.transform(local / 0.35f)
                    local < 0.70f -> 1f - FastOutSlowInEasing.transform((local - 0.35f) / 0.35f)
                    else -> 0f
                }
                val top = (2 - i) * 15f * u - 3.dp.toPx() * lit
                val path = Path().apply {
                    moveTo(3f * u, top + 11.5f * u)
                    lineTo(15f * u, top + 2.5f * u)
                    lineTo(27f * u, top + 11.5f * u)
                }
                val color = if (i == 2) HW.Primary else Color.White
                val alpha = 0.16f + 0.84f * lit
                if (i == 2) {
                    drawPath(path, color.copy(alpha = 0.22f * alpha), style = Stroke(7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawPath(path, color.copy(alpha = alpha), style = Stroke(2.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Text(
            label,
            color = Color.White,
            style = HWType.ChipStrong.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em),
            textAlign = TextAlign.Center, // wraps rather than clips at large text sizes
            modifier = Modifier
                .clearAndSetSemantics {}
                // Paint the words with a light band that sweeps across them.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val cx = size.width * sweep
                    val band = size.width * 0.32f
                    drawRect(
                        Brush.horizontalGradient(
                            0f to dim, 0.5f to Color.White, 1f to dim,
                            startX = cx - band, endX = cx + band,
                        ),
                        blendMode = BlendMode.SrcIn,
                    )
                },
        )
    }
}

/** "Good morning|afternoon|evening|night" for the local time. */
fun greetingFor(time: LocalTime = LocalTime.now()): String = when (time.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

/**
 * Name: black sky over the Earth at night seen from orbit (the Gulf and Liwa, the view Orbit 412
 * has of you), then the chat's own input bar and send arrow ("Meet Orbit 412"), with Orbit 412
 * gliding along the horizon. Matches the preview's Name artboard: the photo starts 17% down.
 */
@Composable
fun NameScreen(
    initialName: String,
    entering: Boolean,
    onBack: () -> Unit,
    onEnter: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var focused by remember { mutableStateOf(false) }
    val canMeet = name.isNotBlank() && !entering
    val submit = { if (canMeet) onEnter(name.trim()) }

    BoxWithConstraints(Modifier.fillMaxSize().background(HW.Space)) {
        val h = maxHeight
        Box(
            Modifier
                .offset(y = h * 0.17f)
                .fillMaxWidth()
                .height(h * 0.83f)
                .clipToBounds(),
        ) {
            Image(
                painter = painterResource(R.drawable.bg_earth_orbit),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black, 0.17f to Color.Black, 0.33f to Color.Transparent,
                    0.58f to Color.Transparent, 0.76f to Color.Black.copy(alpha = 0.78f), 0.92f to Color.Black,
                    1f to Color.Black,
                ),
            ),
        )
        // The one motion on this screen: Orbit 412 gliding just above the Earth's horizon.
        SatelliteArc(
            Modifier.fillMaxSize(),
            place = ::nameScreenArc,
            labelAbove = true,
            startAt = 0.22f,
        )

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { HwIcon(R.drawable.ic_arrow_back, HW.IconMuted, 22.dp, contentDescription = "Back") }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { BrandLockup() }
                Spacer(Modifier.width(48.dp))
            }
            Rising {
                Column(
                    Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "What should Orbit\u00A0412 call you?",
                        color = Color.White, style = HWType.NameTitle.capScale(), textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "It will greet you by name every time it passes over.",
                        color = HW.InkSecondary, style = HWType.ChatSub.copy(lineBreak = LineBreak.Paragraph),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 300.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // The same bar as the chat composer: shared glass, chat type, and the chat's send
                // button (teal arrow once there is a name) instead of a separate big button.
                val shape = RoundedCornerShape(28.dp)
                val border by animateColorAsState(
                    if (focused) HW.Primary.copy(alpha = 0.45f) else HW.GlassBorder, label = "name-border",
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .glass(shape, border)
                        .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (name.isEmpty()) {
                            Text("Your name or callsign", color = HW.InkTertiary, style = HWType.ChatInput)
                        }
                        BasicTextField(
                            value = name,
                            onValueChange = { value -> name = value.take(40) },
                            singleLine = true,
                            textStyle = HWType.ChatInput.copy(color = Color.White),
                            cursorBrush = SolidColor(HW.Primary),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { submit() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focused = it.isFocused }
                                .semantics { contentDescription = "Your name" },
                        )
                    }
                    SendButton(generating = false, canSend = canMeet, onClick = submit, description = "Meet Orbit 412")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HwIcon(R.drawable.ic_lock, HW.InkTertiary, 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Your name stays on this phone.", color = HW.InkTertiary, style = HWType.Subline)
                }
            }
        }
    }
}
