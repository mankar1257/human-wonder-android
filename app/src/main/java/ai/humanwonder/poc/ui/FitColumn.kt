package ai.humanwonder.poc.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A headline block that never spills over the rest of the screen. Children are stacked top to
 * bottom and centred horizontally, like a Column; when the height it is given runs out (the
 * keyboard is open, a short phone, big text) the last children are left out instead of being
 * squeezed or drawn over the input below. A headline + sub-line therefore loses the sub-line
 * first, then the headline, and the input bar always keeps its full size.
 *
 * [gap] goes between the children that are shown. With [centered] the shown children sit in the
 * vertical middle of the space; otherwise at the top.
 */
@Composable
fun FitColumn(
    modifier: Modifier = Modifier,
    gap: Dp = 0.dp,
    centered: Boolean = false,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        val placeables = measurables.map { it.measure(loose) }
        val gapPx = gap.roundToPx()
        val bounded = constraints.hasBoundedHeight

        var used = 0
        var shown = 0
        for (p in placeables) {
            val next = used + (if (shown > 0) gapPx else 0) + p.height
            if (bounded && next > constraints.maxHeight) break
            used = next
            shown++
        }

        val width = if (constraints.hasBoundedWidth) constraints.maxWidth
        else (placeables.take(shown).maxOfOrNull { it.width } ?: 0).coerceAtLeast(constraints.minWidth)
        val height = if (centered && bounded) constraints.maxHeight
        else used.coerceIn(constraints.minHeight, if (bounded) constraints.maxHeight else Int.MAX_VALUE)

        layout(width, height) {
            var y = if (centered && bounded) (height - used) / 2 else 0
            placeables.take(shown).forEach { p ->
                p.placeRelative((width - p.width) / 2, y)
                y += p.height + gapPx
            }
        }
    }
}
