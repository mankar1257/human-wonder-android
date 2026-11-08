package ai.humanwonder.poc.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.min
import kotlin.math.roundToInt

/** The largest text scale the app follows from the phone's "Font size" setting. */
const val MAX_FONT_SCALE = 1.15f

/**
 * The narrowest width, in dp, the screens are laid out for. A bigger "Display size" (Samsung:
 * "Screen zoom") makes the phone report fewer dp; below this width the whole UI is drawn a little
 * smaller so it keeps the 360dp layout instead of crowding.
 */
const val MIN_LAYOUT_WIDTH_DP = 360

/**
 * Keeps every screen in its designed proportions whatever the phone's own size settings are:
 *  - "Font size" makes text bigger by at most [MAX_FONT_SCALE] (15%).
 *  - "Display size" can enlarge the layout only until the screen is [MIN_LAYOUT_WIDTH_DP] wide.
 * Used once around the whole app (MainActivity) and around each screen in the screen checks.
 */
@Composable
fun HumanWonderScale(content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val config = LocalConfiguration.current
    val width = config.screenWidthDp
    val shrink = if (width in 1 until MIN_LAYOUT_WIDTH_DP) width / MIN_LAYOUT_WIDTH_DP.toFloat() else 1f
    val textScale = min(base.fontScale, MAX_FONT_SCALE)
    val density = remember(base.density, textScale, shrink) { Density(base.density * shrink, textScale) }
    // Configuration readers (the side menu's width, for one) see the same sizes as the layout.
    val scaled = remember(config, textScale, shrink) {
        Configuration(config).apply {
            if (shrink < 1f) {
                screenWidthDp = (screenWidthDp / shrink).roundToInt()
                screenHeightDp = (screenHeightDp / shrink).roundToInt()
                smallestScreenWidthDp = (smallestScreenWidthDp / shrink).roundToInt()
                densityDpi = (densityDpi * shrink).roundToInt()
            }
            fontScale = textScale
        }
    }
    CompositionLocalProvider(
        LocalDensity provides density,
        LocalConfiguration provides scaled,
        content = content,
    )
}

/**
 * For dialog and menu slots: those open in a window of their own, which starts again from the
 * phone's own text and display size, so their content gets the same limits here.
 */
fun scaled(content: @Composable () -> Unit): @Composable () -> Unit = { HumanWonderScale(content) }
