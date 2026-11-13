package com.bisc.flauncher.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * [count] tiles, [columns] to a line, every one of the same width and height: the tallest one's,
 * so a name on two lines never makes one tile stand out. A last line that is not full keeps the
 * same size and starts at the left. The tiles must not use lazy lists or BoxWithConstraints, since
 * their height is asked for before they are placed.
 */
@Composable
fun EqualGrid(count: Int, columns: Int, modifier: Modifier = Modifier, gap: Dp = 10.dp, item: @Composable (Int) -> Unit) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = { repeat(count) { item(it) } }
    ) { measurables, constraints ->
        val cols = columns.coerceAtLeast(1)
        val space = gap.roundToPx()
        val width = ((constraints.maxWidth - space * (cols - 1)) / cols).coerceAtLeast(0)
        val height = measurables.maxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0
        val placeables = measurables.map { it.measure(Constraints.fixed(width, height)) }
        val rows = (placeables.size + cols - 1) / cols
        layout(constraints.maxWidth, (rows * height + (rows - 1).coerceAtLeast(0) * space).coerceAtLeast(0)) {
            placeables.forEachIndexed { i, p -> p.place((i % cols) * (width + space), (i / cols) * (height + space)) }
        }
    }
}
