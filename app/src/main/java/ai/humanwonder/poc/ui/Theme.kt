package ai.humanwonder.poc.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import kotlin.random.Random

/** Colour tokens from the Stitch "Human Wonder" design system (DESIGN.md). */
object HW {
    val Background = Color(0xFF061617)
    val SurfaceLowest = Color(0xFF021112)
    val SurfaceLow = Color(0xFF0E1E1F)
    val Surface = Color(0xFF122223)
    val SurfaceHigh = Color(0xFF1D2D2E)
    val SurfaceHighest = Color(0xFF283839)
    val OnSurface = Color(0xFFD4E6E7)
    val OnSurfaceVariant = Color(0xFFBACAC4)
    val Outline = Color(0xFF84948F)
    val OutlineVariant = Color(0xFF3B4A46)
    val Primary = Color(0xFF83FFE5)
    val PrimaryContainer = Color(0xFF2FE6C8)
    val OnPrimaryContainer = Color(0xFF00382F)
    val Gold = Color(0xFFEDBF70)
    val Error = Color(0xFFFFB4AB)

    /** Newsreader in the Stitch design; the system serif is the offline stand-in. */
    val Headline = FontFamily.Serif
    /** JetBrains Mono in the Stitch design, used for telemetry-style labels. */
    val Mono = FontFamily.Monospace
}

private val colors = darkColorScheme(
    primary = HW.PrimaryContainer,
    onPrimary = HW.OnPrimaryContainer,
    primaryContainer = HW.PrimaryContainer,
    onPrimaryContainer = HW.OnPrimaryContainer,
    secondary = HW.Gold,
    background = HW.Background,
    onBackground = HW.OnSurface,
    surface = HW.Background,
    onSurface = HW.OnSurface,
    surfaceVariant = HW.SurfaceHigh,
    onSurfaceVariant = HW.OnSurfaceVariant,
    surfaceContainer = HW.Surface,
    surfaceContainerHigh = HW.SurfaceHigh,
    surfaceContainerHighest = HW.SurfaceHighest,
    outline = HW.Outline,
    outlineVariant = HW.OutlineVariant,
    error = HW.Error,
)

@Composable
fun HumanWonderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}

/** Night-sky backdrop: phthalo base, a faint aurora glow from the zenith, and still stars. */
@Composable
fun SkyBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val stars = remember {
        val rnd = Random(7)
        List(70) { Triple(rnd.nextFloat(), rnd.nextFloat() * 0.75f, 0.15f + rnd.nextFloat() * 0.45f) }
    }
    Box(
        modifier
            .fillMaxSize()
            .background(HW.Background),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(HW.PrimaryContainer.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(size.width / 2f, 0f),
                    radius = size.height * 0.7f,
                ),
            )
            stars.forEach { (x, y, a) ->
                drawCircle(
                    color = Color.White.copy(alpha = a),
                    radius = 1.2f + a * 1.6f,
                    center = Offset(x * size.width, y * size.height),
                )
            }
        }
        content()
    }
}
