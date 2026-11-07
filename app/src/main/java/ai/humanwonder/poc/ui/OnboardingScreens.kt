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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Full-bleed photograph that slowly breathes (a gentle Ken Burns zoom), as in the Stitch screens. */
@Composable
fun PhotoBackdrop(@DrawableRes image: Int, scrims: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "kenburns")
    val scale by t.animateFloat(
        initialValue = 1.0f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(24_000), RepeatMode.Reverse), label = "s",
    )
    Box(Modifier.fillMaxSize().background(HW.Background)) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scale; scaleY = scale },
        )
        scrims()
    }
}

/** Stitch screen 1, "Human Wonder — Simple Landing". */
@Composable
fun LandingScreen(onBegin: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        PhotoBackdrop(R.drawable.bg_liwa_dunes) {
            // bg-gradient-to-b from-surface-container-lowest/80 via-transparent to-surface-container-lowest/90
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to HW.Lowest.copy(alpha = 0.80f), 0.5f to Color.Transparent, 1f to HW.Lowest.copy(alpha = 0.90f),
                    ),
                ),
            )
            // bottom 3/4: from-surface-container-lowest via-lowest/70 to-transparent
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent, 0.25f to Color.Transparent,
                        0.62f to HW.Lowest.copy(alpha = 0.70f), 1f to HW.Lowest,
                    ),
                ),
            )
        }

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            // Header: ✦ Human Wonder · ZENITH 98°
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✦", color = HW.Primary, style = HWType.BodySm)
                Spacer(Modifier.size(8.dp))
                Text("Human Wonder", color = HW.OnSurface, style = HWType.HeadlineSm)
                Spacer(Modifier.weight(1f))
                GlassPill(fill = HW.SurfaceLow.copy(alpha = 0.6f), border = Color.Transparent) {
                    PulsingDot(HW.Primary)
                    MonoLabel("Zenith 98°", color = HW.OnSurfaceVariant)
                }
            }

            // Open zenith horizon with a quiet reticle
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Reticle()
            }

            // Bottom third: words, radiant CTA, coordinates
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { shown = true }
            AnimatedVisibility(
                visible = shown,
                enter = fadeIn(tween(1400)) + slideInVertically(tween(1400)) { it / 6 },
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "The sky has something to tell you.",
                        color = HW.OnSurface, style = HWType.HeadlineMobile, textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 384.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "For thousands of years, we looked up and wondered. Now, for the first time, the sky can answer.",
                        color = HW.OnSurfaceVariant, style = HWType.BodyMd.copy(fontWeight = FontWeight.Light),
                        textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 384.dp),
                    )
                    Spacer(Modifier.height(32.dp))
                    GlowButton(
                        "Begin Your First Look Up",
                        modifier = Modifier.widthIn(max = 384.dp),
                        trailing = { Text("→", color = HW.OnPrimaryContainer, style = HWType.LabelMd) },
                        onClick = onBegin,
                    )
                    Spacer(Modifier.height(28.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel("Liwa Oasis", color = HW.OnSurfaceVariant.copy(alpha = 0.7f))
                        Text("  ·  ", color = HW.Primary.copy(alpha = 0.4f), style = HWType.LabelSm)
                        MonoLabel("23.13° N, 53.79° E", color = HW.OnSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Reticle() {
    val t = rememberInfiniteTransition(label = "reticle")
    val breathe by t.animateFloat(
        initialValue = 0.18f, targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse), label = "b",
    )
    Canvas(Modifier.size(112.dp)) {
        val r = size.minDimension / 2
        drawCircle(HW.Primary.copy(alpha = breathe * 0.5f), radius = r, style = Stroke(1.dp.toPx()))
        drawCircle(HW.Primary.copy(alpha = breathe * 0.25f), radius = r * 0.55f, style = Stroke(1.dp.toPx()))
        val tick = 7.dp.toPx()
        listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(-1f, 0f), Offset(1f, 0f)).forEach { d ->
            drawLine(
                HW.Primary.copy(alpha = breathe),
                start = center + Offset(d.x * (r - tick), d.y * (r - tick)),
                end = center + Offset(d.x * r, d.y * r),
                strokeWidth = 1.dp.toPx(),
            )
        }
        drawCircle(HW.PrimaryFixed, radius = 2.dp.toPx())
    }
}

/** Stitch screen 2, "Human Wonder — Setup Your Name (Minimal)". */
@Composable
fun NameScreen(
    initialName: String,
    entering: Boolean,
    onEnter: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val submit = { if (!entering) onEnter(name.ifBlank { "Wanderer" }) }

    Box(Modifier.fillMaxSize()) {
        PhotoBackdrop(R.drawable.bg_earth_orbit) {
            // bg-gradient-to-b from-[#061617]/75 via-[#061617]/35 to-[#061617]/85 + teal glow at top
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to HW.Background.copy(alpha = 0.75f), 0.5f to HW.Background.copy(alpha = 0.35f),
                        1f to HW.Background.copy(alpha = 0.85f),
                    ),
                ),
            )
            Box(
                Modifier.fillMaxSize().drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(HW.PrimaryContainer.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width / 2, 0f), radius = size.height * 0.65f,
                        ),
                    )
                },
            )
        }

        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassPill(fill = HW.ContainerHigh.copy(alpha = 0.7f), border = Color.Transparent) {
                    PulsingDot(HW.Primary)
                    MonoLabel("Initiation", color = HW.Primary)
                }
                Spacer(Modifier.weight(1f))
                HwIcon(R.drawable.ic_explore, HW.Gold, 15.dp)
                Spacer(Modifier.size(4.dp))
                MonoLabel("23.13° N", color = HW.OnSurfaceVariant)
            }

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("What should the sky call you?", color = HW.OnSurface, style = HWType.HeadlineMobile)
                    Text(
                        "When the orbital swarm passes above your coordinates, it will speak to you by name.",
                        color = HW.OnSurfaceVariant, style = HWType.BodyMd,
                    )
                }
                Column {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        MonoLabel("Wanderer identity", color = HW.OnSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        MonoLabel("Radio telemetry", color = HW.Gold)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HW.ContainerHigh.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HwIcon(R.drawable.ic_satellite, HW.Primary.copy(alpha = 0.8f), 20.dp)
                        Spacer(Modifier.size(12.dp))
                        Box(Modifier.weight(1f)) {
                            if (name.isEmpty()) {
                                Text("Your name or callsign", color = HW.OnSurfaceVariant.copy(alpha = 0.4f), style = HWType.BodyLg)
                            }
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it.take(40) },
                                singleLine = true,
                                textStyle = HWType.BodyLg.copy(color = HW.OnSurface),
                                cursorBrush = SolidColor(HW.Primary),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Go,
                                ),
                                keyboardActions = KeyboardActions(onGo = { submit() }),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Box(
                            Modifier
                                .size(8.dp)
                                .drawBehind {
                                    drawCircle(
                                        Brush.radialGradient(listOf(HW.PrimaryContainer.copy(alpha = 0.8f), Color.Transparent)),
                                        radius = size.minDimension * 1.6f,
                                    )
                                }
                                .clip(CircleShape)
                                .background(HW.Primary),
                        )
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlowButton(
                    text = if (entering) "Aligning Zenith with ${name.ifBlank { "Wanderer" }}…" else "Enter the Sky Sanctuary",
                    trailing = {
                        if (entering) {
                            CircularProgressIndicator(
                                color = HW.OnPrimaryContainer, strokeWidth = 2.dp, modifier = Modifier.size(18.dp),
                            )
                        } else {
                            HwIcon(R.drawable.ic_arrow_forward, HW.OnPrimaryContainer, 20.dp)
                        }
                    },
                    onClick = submit,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HwIcon(R.drawable.ic_lock, HW.Primary, 14.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("Your name stays between you and the sky.", color = HW.OnSurfaceVariant, style = HWType.LabelSm.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.02.sp))
                }
            }
        }
    }
}
