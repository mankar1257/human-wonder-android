package ai.humanwonder.poc.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
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

/** One calm pass of Orbit 412 across the screen (same everywhere; the owner asked for slow). */
const val ORBIT_PASS_MS = 90_000

/**
 * Orbit 412 gliding across the sky with a comet trail and its name beside it. Its path is an
 * invisible arc placed by [place] (apex y and end drop in px, from the canvas size; used over the
 * Earth photo), or a fixed distance below the top inset ([arcBelowTop], used in the chat), or at a
 * fraction of the screen height ([arcFraction] with [dropFraction], over the welcome photo).
 * Matches the preview canvas exactly (trail, glow, core and label values).
 */
@Composable
fun SatelliteArc(
    modifier: Modifier = Modifier,
    visibility: Float = 1f,
    arcBelowTop: Dp? = null,
    arcFraction: Float = 0.4f,
    dropFraction: Float = 0.05f,
    periodMs: Int = ORBIT_PASS_MS,
    startAt: Float = 0.3f,
    place: ((Size) -> Offset)? = null,
    label: Boolean = true,
    labelAbove: Boolean = false,
) {
    val density = LocalDensity.current
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val measurer = rememberTextMeasurer()
    val labelStyle = HWType.ChatMeta.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = HW.Ink.copy(alpha = 0.78f))
    val t = rememberInfiniteTransition(label = "orbit")
    val travel by t.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "travel")

    Canvas(modifier.graphicsLayer { alpha = visibility }) {
        val w = size.width
        val arcY: Float
        val drop: Float
        if (place != null) {
            val spot = place(size)
            arcY = spot.x
            drop = spot.y
        } else if (arcBelowTop != null) {
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
        if (label) {
            val text = measurer.measure(AnnotatedString("Orbit 412"), labelStyle)
            val y = if (labelAbove) sat.y - 7.dp.toPx() - text.size.height else sat.y + 7.dp.toPx()
            drawText(text, topLeft = Offset(sat.x + 9.dp.toPx(), y))
        }
    }
}

// Orbit 412's pass over bg_earth_orbit.jpg (768 x 1376). The top of the airglow peaks at y=402 in
// the middle of the photo and sits ~26 px lower at the edges; Orbit 412 flies 45 px above it
// (apex y=357, ends 40 px lower). Same numbers as design/preview/gen/orbit_js.py.
private const val EARTH_W = 768f
private const val EARTH_H = 1376f
private const val EARTH_ARC_Y = 357f
private const val EARTH_ARC_DROP = 40f

/** Name screen: the photo box starts 17% down, is 83% tall, cropped to fill and top-aligned. */
fun nameScreenArc(size: Size): Offset {
    val s = max(size.width / EARTH_W, 0.83f * size.height / EARTH_H)
    return Offset(0.17f * size.height + EARTH_ARC_Y * s, EARTH_ARC_DROP * s)
}

// --------------------------------------------------------------------------------- star trails

/** The pre-drawn trails: one bitmap, rotated around the pole star every frame. */
private class StarTrails(val image: ImageBitmap, val sizeDp: Float)

private class TrailColor(val r: Int, val g: Int, val b: Int, val weight: Int)

private val TRAIL_PALETTE = listOf(
    TrailColor(255, 255, 255, 34), TrailColor(196, 216, 255, 26), TrailColor(255, 222, 176, 15),
    TrailColor(255, 186, 128, 10), TrailColor(176, 255, 238, 8), TrailColor(214, 196, 255, 7),
)

/**
 * Draws the long-exposure trails once (off the main thread). Same seed, palette, count and sweep
 * as the preview canvas, so both skies are identical. Geometry is in dp; the bitmap holds at most
 * 2 px per dp to keep memory small (the trails are soft, so upscaling is invisible).
 */
private fun buildStarTrails(widthDp: Float, heightDp: Float, density: Float): StarTrails {
    val px = 0.5f * widthDp
    val py = 0.42f * heightDp
    val maxR = maxOf(
        hypot(px, py), hypot(widthDp - px, py),
        hypot(px, 0.8f * heightDp - py), hypot(widthDp - px, 0.8f * heightDp - py),
    )
    val sizeDp = ceil(2 * maxR + 8)
    val scale = min(density, 2f)
    val bitmapPx = (sizeDp * scale).roundToInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(bitmapPx, bitmapPx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.scale(scale, scale)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    var seed = 412L
    fun rand(): Double {
        seed = (seed * 16807L) % 2147483647L
        return (seed - 1) / 2147483646.0
    }
    fun pick(): TrailColor {
        val r = rand() * 100
        var acc = 0
        for (c in TRAIL_PALETTE) {
            acc += c.weight
            if (r <= acc) return c
        }
        return TRAIL_PALETTE[0]
    }
    val c = sizeDp / 2
    val sweep = 0.85
    val rect = RectF()
    repeat(950) {
        val r = sqrt(rand()) * maxR
        if (r < 6) return@repeat
        val a0 = rand() * PI * 2
        val col = pick()
        val bright = 0.16 + rand().pow(3) * 0.8
        val width = 0.5 + rand() * rand() * 1.4
        paint.strokeWidth = width.toFloat()
        rect.set((c - r).toFloat(), (c - r).toFloat(), (c + r).toFloat(), (c + r).toFloat())
        for (j in 0 until 10) {
            val alpha = bright * (1 - j / 10.0).pow(1.5)
            paint.color = android.graphics.Color.argb((alpha * 255).roundToInt().coerceIn(0, 255), col.r, col.g, col.b)
            val start = Math.toDegrees(a0 + sweep * j / 10).toFloat()
            canvas.drawArc(rect, start, Math.toDegrees(sweep / 10).toFloat(), false, paint)
        }
    }
    return StarTrails(bitmap.asImageBitmap(), sizeDp)
}

/**
 * A night sky photographed with a long exposure: every star draws an arc around the pole star and
 * the whole sky turns slowly (one degree a second). The pole star is "your star": [glow] (0..1)
 * brightens it, and each change of [pulseKey] sends a soft ring out from it.
 */
@Composable
fun StarTrailSky(modifier: Modifier = Modifier, glow: Float = 0f, pulseKey: Int = 0) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val wDp = maxWidth.value
        val hDp = maxHeight.value
        val trails by produceState<StarTrails?>(null, wDp, hDp) {
            value = withContext(Dispatchers.Default) { buildStarTrails(wDp, hDp, density.density) }
        }
        val t = rememberInfiniteTransition(label = "trails")
        val turn by t.animateFloat(0f, -360f, infiniteRepeatable(tween(360_000, easing = LinearEasing)), label = "turn")
        val twinkle by t.animateFloat(0.7f, 1f, infiniteRepeatable(tween(1320), RepeatMode.Reverse), label = "twinkle")
        val appear by animateFloatAsState(if (trails != null) 1f else 0f, tween(1600), label = "appear")
        val lit by animateFloatAsState(glow.coerceIn(0f, 1f), tween(500), label = "lit")
        val pulse = remember { Animatable(1f) }
        LaunchedEffect(pulseKey) {
            if (pulseKey > 0) {
                pulse.snapTo(0f)
                pulse.animateTo(1f, tween(1400, easing = LinearEasing))
            }
        }

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0xFF010208), 0.42f to Color(0xFF02051A), 0.70f to Color(0xFF07103A),
                    0.80f to Color(0xFF1A1650), 1f to Color(0xFF04040C),
                ),
            ),
        )
        Canvas(Modifier.fillMaxSize()) {
            // Horizon glow: an ellipse 90% wide and 22% tall, centred 80% down.
            val glowCenter = Offset(size.width * 0.5f, size.height * 0.8f)
            val rx = size.width * 0.9f
            val ry = size.height * 0.22f
            withTransform({ scale(1f, ry / rx, pivot = glowCenter) }) {
                drawCircle(
                    Brush.radialGradient(
                        0f to Color(0x597C5CFF), 0.55f to Color(0x1A2FE6C8), 1f to Color.Transparent,
                        center = glowCenter, radius = rx,
                    ),
                    radius = rx, center = glowCenter,
                )
            }
            val tr = trails ?: return@Canvas
            val pole = Offset(size.width * 0.5f, size.height * 0.42f)
            val side = tr.sizeDp.dp.toPx()
            rotate(turn, pivot = pole) {
                drawImage(
                    tr.image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(tr.image.width, tr.image.height),
                    dstOffset = IntOffset((pole.x - side / 2).roundToInt(), (pole.y - side / 2).roundToInt()),
                    dstSize = IntSize(side.roundToInt(), side.roundToInt()),
                    alpha = appear,
                )
            }
            val glowR = (14f + 16f * lit).dp.toPx()
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(200, 255, 245).copy(alpha = ((0.35f + 0.45f * lit) * twinkle * appear).coerceIn(0f, 1f)), Color.Transparent),
                    center = pole, radius = glowR,
                ),
                radius = glowR, center = pole,
            )
            drawCircle(Color.White.copy(alpha = appear), radius = (1.8f + 1.2f * lit).dp.toPx(), center = pole)
            val q = pulse.value
            if (q < 1f) {
                drawCircle(
                    HW.Primary.copy(alpha = 0.5f * (1f - q)),
                    radius = (6f + 64f * q).dp.toPx(), center = pole, style = Stroke(1.2.dp.toPx()),
                )
            }
        }
    }
}

/** Liwa dunes in silhouette along the bottom of the night sky, with a faint moonlit rim. */
@Composable
fun DuneHorizon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sx = size.width / 390f
        val sy = size.height / 844f
        fun ridge(close: Boolean) = Path().apply {
            moveTo(0f, 652 * sy)
            cubicTo(52 * sx, 634 * sy, 110 * sx, 622 * sy, 172 * sx, 640 * sy)
            cubicTo(228 * sx, 656 * sy, 262 * sx, 684 * sy, 318 * sx, 676 * sy)
            cubicTo(350 * sx, 671 * sy, 372 * sx, 660 * sy, 390 * sx, 654 * sy)
            if (close) {
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        }
        drawPath(ridge(close = true), Color.Black)
        drawPath(ridge(close = false), HW.Primary.copy(alpha = 0.22f), style = Stroke(1.dp.toPx()))
        val front = Path().apply {
            moveTo(0f, 700 * sy)
            cubicTo(70 * sx, 690 * sy, 150 * sx, 712 * sy, 230 * sx, 706 * sy)
            cubicTo(300 * sx, 700 * sy, 350 * sx, 712 * sy, 390 * sx, 708 * sy)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(front, Color(0xFF020304))
    }
}
