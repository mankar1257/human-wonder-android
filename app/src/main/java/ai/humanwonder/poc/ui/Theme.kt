package ai.humanwonder.poc.ui

import ai.humanwonder.poc.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Colour tokens from the Stitch "Human Wonder" design system (DESIGN.md + the three screens). */
object HW {
    val Background = Color(0xFF061617)
    val Lowest = Color(0xFF021112)
    val SurfaceLow = Color(0xFF0E1E1F)
    val Container = Color(0xFF122223)
    val ContainerHigh = Color(0xFF1D2D2E)
    val ContainerHighest = Color(0xFF283839)
    val PromptBox = Color(0xFF0C1C1F)

    val OnSurface = Color(0xFFD4E6E7)
    val OnSurfaceVariant = Color(0xFFBACAC4)
    val Muted = Color(0xFF9CB3AD)
    val Outline = Color(0xFF84948F)
    val OutlineVariant = Color(0xFF3B4A46)

    val Primary = Color(0xFF83FFE5)
    val PrimaryFixed = Color(0xFF51FCDD)
    val PrimaryContainer = Color(0xFF2FE6C8)
    val OnPrimaryContainer = Color(0xFF006354)
    val Send = Color(0xFF3FF2D0)
    val OnSend = Color(0xFF002E26)

    val Gold = Color(0xFFEDBF70)
    /** "Starlight cream for words": the sky's own voice. */
    val Starlight = Color(0xFFF4EEDD)
    val Violet = Color(0xFF7C3AED)

    val Emerald300 = Color(0xFF6EE7B7)
    val Emerald400 = Color(0xFF34D399)
    val Emerald500 = Color(0xFF10B981)
    val Error = Color(0xFFFFB4AB)

    // Chat (Gemini-inspired, readable): true black space, calm ink, one teal accent.
    val Space = Color(0xFF000000)
    val Ink = Color(0xFFECEFF0)
    val InkAnswer = Color(0xFFE8ECEC)
    val InkSecondary = Color(0xFFA7B2B0)
    val InkTertiary = Color(0xFF8D9997)
    val InkDisabled = Color(0xFF6F7B79)
    val IconMuted = Color(0xFFC5CDCB)
    val Bubble = Color(0xFF1D2225)
    val Chip = Color(0xFFD5DCDB)
    val OnAccent = Color(0xFF00201A)
}

/** Typefaces from the Stitch design, bundled so the app needs no network. */
object HWFont {
    val Geist = FontFamily(
        Font(R.font.geist, FontWeight.Light),
        Font(R.font.geist, FontWeight.Normal),
        Font(R.font.geist, FontWeight.Medium),
        Font(R.font.geist, FontWeight.SemiBold),
    )
    val Newsreader = FontFamily(
        Font(R.font.newsreader, FontWeight.Light),
        Font(R.font.newsreader, FontWeight.Normal),
        Font(R.font.newsreader, FontWeight.Medium),
    )
    val Mono = FontFamily(
        Font(R.font.jetbrains_mono, FontWeight.Normal),
        Font(R.font.jetbrains_mono, FontWeight.Medium),
    )
}

/** Type scale from DESIGN.md. */
object HWType {
    val HeadlineMobile = TextStyle(
        fontFamily = HWFont.Newsreader, fontWeight = FontWeight.Normal,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em,
    )
    val HeadlineSm = TextStyle(
        fontFamily = HWFont.Newsreader, fontWeight = FontWeight.Normal,
        fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em,
    )
    val BodyLg = TextStyle(fontFamily = HWFont.Geist, fontSize = 18.sp, lineHeight = 28.sp)
    val BodyMd = TextStyle(fontFamily = HWFont.Geist, fontSize = 15.sp, lineHeight = 24.sp)
    val BodySm = TextStyle(fontFamily = HWFont.Geist, fontSize = 13.sp, lineHeight = 20.sp)
    val LabelSm = TextStyle(
        fontFamily = HWFont.Mono, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.1.em,
    )
    val LabelMd = TextStyle(
        fontFamily = HWFont.Mono, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.06.em,
    )
    // Chat type scale (Material 3 roles: body 16sp, labels never below 12sp).
    val ChatHero = TextStyle(
        fontFamily = HWFont.Geist, fontWeight = FontWeight.Light,
        fontSize = 42.sp, lineHeight = 46.sp, letterSpacing = (-0.02).em,
    )
    val ChatSub = TextStyle(fontFamily = HWFont.Geist, fontSize = 15.sp, lineHeight = 22.sp)
    val ChatTitle = TextStyle(fontFamily = HWFont.Geist, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp)
    val ChatBody = TextStyle(fontFamily = HWFont.Geist, fontSize = 16.sp, lineHeight = 25.sp)
    val ChatInput = TextStyle(fontFamily = HWFont.Geist, fontSize = 16.sp, lineHeight = 24.sp)
    val ChatLabel = TextStyle(fontFamily = HWFont.Geist, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
    val ChatMeta = TextStyle(fontFamily = HWFont.Geist, fontSize = 12.sp, lineHeight = 16.sp)
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
    surfaceVariant = HW.ContainerHigh,
    onSurfaceVariant = HW.OnSurfaceVariant,
    surfaceContainer = HW.Container,
    surfaceContainerHigh = HW.ContainerHigh,
    surfaceContainerHighest = HW.ContainerHighest,
    outline = HW.Outline,
    outlineVariant = HW.OutlineVariant,
    error = HW.Error,
)

@Composable
fun HumanWonderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography.let { t ->
            t.copy(
                bodyLarge = t.bodyLarge.copy(fontFamily = HWFont.Geist),
                bodyMedium = t.bodyMedium.copy(fontFamily = HWFont.Geist),
                bodySmall = t.bodySmall.copy(fontFamily = HWFont.Geist),
                labelLarge = t.labelLarge.copy(fontFamily = HWFont.Geist),
                titleLarge = t.titleLarge.copy(fontFamily = HWFont.Newsreader),
            )
        },
        content = content,
    )
}
