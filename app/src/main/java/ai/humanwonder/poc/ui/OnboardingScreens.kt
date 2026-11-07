package ai.humanwonder.poc.ui

import ai.humanwonder.poc.R
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

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

/** Welcome: the wanderer on the Liwa dunes, with Orbit 412 crossing the Milky Way above. */
@Composable
fun LandingScreen(onBegin: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        PhotoSky(
            R.drawable.bg_liwa_dunes,
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.66f), 0.32f to Color.Black.copy(alpha = 0.22f),
                0.46f to Color.Transparent, 0.62f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f),
            ),
        )
        SatelliteArc(Modifier.fillMaxSize(), arcFraction = 0.45f, dropFraction = 0.06f, periodMs = 60_000, startAt = 0.18f)

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                BrandLockup()
            }
            Rising {
                Column(
                    Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "The sky has something to tell you.",
                        color = Color.White, style = HWType.WelcomeHero.capScale(), textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Human Wonder gives the sky a voice: Orbit 412, a satellite passing over you tonight.",
                        color = HW.Ink.copy(alpha = 0.85f), style = HWType.ChatSub, textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 310.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                "Begin your first look up",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 40.dp),
                onClick = onBegin,
            )
        }
    }
}

/**
 * Name: later the same night over the Liwa dunes, a long-exposure sky turning around the pole
 * star. That star is yours: it brightens as you type your name, and each letter sends a ring out.
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
    var keystrokes by remember { mutableIntStateOf(0) }
    val submit = { if (!entering) onEnter(name.ifBlank { "Wanderer" }) }

    Box(Modifier.fillMaxSize()) {
        StarTrailSky(Modifier.fillMaxSize(), glow = min(1f, name.length / 8f), pulseKey = keystrokes)
        SatelliteArc(Modifier.fillMaxSize(), arcFraction = 0.62f, dropFraction = 0.03f, periodMs = 60_000, startAt = 0.18f)
        DuneHorizon(Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.55f), 0.30f to Color.Transparent),
            ),
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
                    Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "What should Orbit\u00A0412 call you?",
                        color = Color.White, style = HWType.WelcomeTitle.capScale(), textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "It will greet you by name every time it passes over.",
                        color = HW.Ink.copy(alpha = 0.85f), style = HWType.ChatSub, textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 300.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // The same glass as the chat composer.
                val shape = RoundedCornerShape(28.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(shape)
                        .background(Brush.linearGradient(0f to Color(0xE00E0F14), 0.55f to Color(0xD61E2030), 1f to Color(0x99464A80)))
                        .border(1.dp, if (focused) HW.Primary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.14f), shape)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HwIcon(R.drawable.ic_satellite, HW.Primary, 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        if (name.isEmpty()) {
                            Text("Your name or callsign", color = HW.InkTertiary, style = HWType.ChatInput.copy(fontSize = 17.sp))
                        }
                        BasicTextField(
                            value = name,
                            onValueChange = { value ->
                                val next = value.take(40)
                                if (next.length > name.length) keystrokes++
                                name = next
                            },
                            singleLine = true,
                            textStyle = HWType.ChatInput.copy(color = Color.White, fontSize = 17.sp),
                            cursorBrush = SolidColor(HW.Primary),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { submit() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focused = it.isFocused }
                                .semantics { contentDescription = "Your name" },
                        )
                    }
                }
                val spinner: (@Composable () -> Unit)? = if (entering) {
                    { CircularProgressIndicator(color = HW.OnAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                }
                PrimaryButton(
                    text = if (entering) "Finding Orbit 412…" else "Meet Orbit 412",
                    trailing = spinner,
                    onClick = submit,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HwIcon(R.drawable.ic_lock, HW.Primary, 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Your name stays between you and the sky.", color = HW.InkSecondary, style = HWType.ChatMeta)
                }
            }
        }
    }
}
