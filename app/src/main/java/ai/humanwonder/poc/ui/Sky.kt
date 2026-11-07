package ai.humanwonder.poc.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.max

/** The faint dot grid from the Stitch home screen, shared by every Human Wonder screen. */
@Composable
fun DotGrid(modifier: Modifier = Modifier, alpha: Float = 0.09f) {
    Box(
        modifier.drawWithCache {
            val step = 22.dp.toPx()
            val w = size.width
            val h = size.height
            val points = buildList {
                var y = step / 2
                while (y < h) {
                    var x = step / 2
                    while (x < w) {
                        add(Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }
            onDrawBehind {
                drawPoints(points, PointMode.Points, Color.White.copy(alpha = alpha), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
        },
    )
}

/**
 * Orbit 412 gliding across the sky with a comet trail and its name beside it. Its path is an
 * invisible arc placed either a fixed distance below the top inset ([arcBelowTop], used in the
 * chat) or at a fraction of the screen height ([arcFraction] with [dropFraction], over photos).
 * Matches the preview canvas exactly (trail, glow, core and label values).
 */
@Composable
fun SatelliteArc(
    modifier: Modifier = Modifier,
    visibility: Float = 1f,
    arcBelowTop: Dp? = null,
    arcFraction: Float = 0.4f,
    dropFraction: Float = 0.05f,
    periodMs: Int = 75_000,
    startAt: Float = 0.3f,
) {
    val density = LocalDensity.current
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val measurer = rememberTextMeasurer()
    val t = rememberInfiniteTransition(label = "orbit")
    val travel by t.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "travel")

    Canvas(modifier.graphicsLayer { alpha = visibility }) {
        val w = size.width
        val arcY: Float
        val drop: Float
        if (arcBelowTop != null) {
            arcY = with(density) { (top + arcBelowTop).toPx() }
            drop = 70.dp.toPx()
        } else {
            arcY = size.height * arcFraction
            drop = size.height * dropFraction
        }
        val p0 = Offset(-0.12f * w, arcY + drop)
        val p1 = Offset(0.5f * w, arcY - drop)
        val p2 = Offset(1.12f * w, arcY + drop)
        fun at(u: Float): Offset {
            val a = (1 - u) * (1 - u)
            val b = 2 * (1 - u) * u
            val c = u * u
            return Offset(a * p0.x + b * p1.x + c * p2.x, a * p0.y + b * p1.y + c * p2.y)
        }
        val u = 0.08f + 0.84f * ((travel + startAt) % 1f)
        val sat = at(u)
        // Comet trail: only where Orbit 412 has just been.
        for (k in 1..18) {
            drawCircle(HW.Primary.copy(alpha = 0.32f * (1f - k / 19f)), radius = (2.0f - k * 0.09f).dp.toPx(), center = at(max(0f, u - k * 0.004f)))
        }
        drawCircle(
            Brush.radialGradient(listOf(HW.Primary.copy(alpha = 0.5f), Color.Transparent), center = sat, radius = 13.dp.toPx()),
            radius = 13.dp.toPx(), center = sat,
        )
        drawCircle(Color.White, radius = 2.3.dp.toPx(), center = sat)
        drawText(
            measurer, "Orbit 412",
            topLeft = Offset(sat.x + 9.dp.toPx(), sat.y + 7.dp.toPx()),
            style = HWType.ChatMeta.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = HW.Ink.copy(alpha = 0.78f)),
        )
    }
}
