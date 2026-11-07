package ai.humanwonder.poc.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HwIcon(
    @DrawableRes id: Int,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Icon(painterResource(id), contentDescription = contentDescription, tint = tint, modifier = modifier.size(size))
}

@Composable
fun MonoLabel(
    text: String,
    color: Color = HW.OnSurfaceVariant,
    modifier: Modifier = Modifier,
    style: TextStyle = HWType.LabelSm,
) {
    Text(text.uppercase(Locale.ROOT), modifier = modifier, color = color, style = style, maxLines = 1)
}

/** A small dot that breathes, like Tailwind's animate-pulse in the Stitch screens. */
@Composable
fun PulsingDot(color: Color, size: Dp = 6.dp, periodMs: Int = 1600) {
    val t = rememberInfiniteTransition(label = "pulse")
    val alpha by t.animateFloat(
        initialValue = 1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(periodMs / 2), RepeatMode.Reverse), label = "a",
    )
    Box(
        Modifier
            .size(size)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(color),
    )
}

/** Glass pill used across the Stitch screens (white/4% fill, white/8% hairline). */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    fill: Color = Color.White.copy(alpha = 0.04f),
    border: Color = Color.White.copy(alpha = 0.08f),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, border, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** The primary action: a solid teal pill, the same accent as the chat's send button. */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(if (pressed) 0.98f else 1f)
            .clip(CircleShape)
            .background(if (enabled) HW.Primary else HW.ContainerHigh)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = if (enabled) HW.OnAccent else HW.InkTertiary, style = HWType.ButtonLabel)
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

/**
 * The Human Wonder logo: the orbit mark (a ring with Orbit 412 circling it) and the wordmark.
 * It is a logo, so the wordmark is sized in dp and does not grow with the system font size.
 */
@Composable
fun BrandLockup(modifier: Modifier = Modifier, markSize: Dp = 22.dp, textSize: Dp = 17.dp) {
    val density = LocalDensity.current
    val fixed = with(density) { HWType.ChatTitle.copy(fontSize = textSize.toSp(), lineHeight = 22.dp.toSp(), letterSpacing = (-0.01).em) }
    Row(
        modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OrbitMark(markSize, active = false)
        Spacer(Modifier.width(if (markSize >= 22.dp) 9.dp else 8.dp))
        Text("Human Wonder", color = Color.White, style = fixed, maxLines = 1, softWrap = false)
    }
}

/** The person's initial on a quiet solid circle (white 12% border), as on Home and in the side menu. */
@Composable
fun InitialAvatar(name: String, size: Dp = 36.dp, fontSize: TextUnit = 15.sp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(HW.AvatarFill)
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialOf(name),
            color = HW.Ink,
            style = HWType.ChatTitle.copy(fontSize = fontSize, lineHeight = fontSize, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

/** First letter of the name, upper case ("W" for Wanderer when there is no name yet). */
fun initialOf(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "W"
    // Whole code point, so a name that starts with an emoji or a non-Latin letter stays intact.
    return String(Character.toChars(trimmed.codePointAt(0))).uppercase()
}

/** A 48dp round icon button (the header buttons on every screen). */
@Composable
fun HwIconButton(
    @DrawableRes icon: Int,
    description: String,
    modifier: Modifier = Modifier,
    tint: Color = HW.IconMuted,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { HwIcon(icon, tint, iconSize, contentDescription = description) }
}

/** Glass card or group: radius 24, rgba(20, 24, 30, 0.72), white 10% hairline. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp)): Modifier =
    this.clip(shape).background(HW.Glass).border(1.dp, HW.GlassBorder, shape)

/** A 1dp white 7% divider inside glass groups. */
@Composable
fun GlassDivider(modifier: Modifier = Modifier, inset: Dp = 16.dp) {
    Box(modifier.fillMaxWidth().padding(horizontal = inset).height(1.dp).background(HW.Divider))
}

/** Section label above a group: 13/18 medium #8D9997, sentence case. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, color = HW.InkTertiary, style = HWType.ChatLabel, modifier = modifier)
}

/**
 * Orbit's mark, alive: a ring (the Earth) with one point of light travelling round it (the
 * satellite). It circles slowly at rest and quickly while Orbit is thinking or answering.
 */
@Composable
fun OrbitMark(size: Dp, active: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "mark")
    val angle by t.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (active) 1600 else 12000, easing = LinearEasing)),
        label = "angle",
    )
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2 * 0.75f
        val c = center
        drawCircle(HW.PrimaryContainer.copy(alpha = 0.08f), radius = r)
        drawCircle(HW.Primary.copy(alpha = 0.9f), radius = r, style = Stroke(1.5.dp.toPx()))
        val a = Math.toRadians(angle.toDouble() - 45.0)
        val p = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
        drawCircle(
            Brush.radialGradient(listOf(HW.Primary.copy(alpha = 0.7f), Color.Transparent), center = p, radius = 4.dp.toPx()),
            radius = 4.dp.toPx(), center = p,
        )
        drawCircle(Color.White, radius = (this.size.minDimension * 0.105f), center = p)
    }
}
