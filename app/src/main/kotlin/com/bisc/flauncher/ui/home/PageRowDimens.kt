package com.bisc.flauncher.ui.home

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor

/** One [com.bisc.flauncher.data.model.PageRow.spanY] unit's height. */
object PageRowDimens {
    /** The smallest a row unit gets; pages stretch it so a full page ends exactly one row gap above
     *  the Dock (see [LocalRowUnitHeight]). */
    val rowUnitHeight = 110.dp
    val rowSpacing = 16.dp

    /** How many row units fit into [viewport] (rows separated by [rowSpacing]). */
    fun unitCount(viewport: Dp): Int =
        floor((viewport + rowSpacing) / (rowUnitHeight + rowSpacing)).toInt().coerceAtLeast(1)

    /** The height of a row [span] units tall: its units plus the gaps they span, so a 2-unit widget
     *  lines up exactly with two 1-unit widgets stacked. */
    fun spanHeight(unit: Dp, span: Int): Dp = unit * span + rowSpacing * (span - 1).coerceAtLeast(0)

    /** The row unit stretched so that [count] units (with their gaps) exactly fill [viewport] — capped,
     *  so a mismatch can never blow rows up. [count] 0 (not known yet) keeps the plain unit. */
    fun stretchedUnit(viewport: Dp, count: Int): Dp =
        if (count <= 0 || viewport <= 0.dp) rowUnitHeight
        else ((viewport + rowSpacing) / count - rowSpacing).coerceIn(rowUnitHeight, rowUnitHeight * 1.3f)

    /** How many row units a row of natural height [height] takes once rounded up to the grid. */
    fun unitsFor(height: Dp, unit: Dp): Int =
        kotlin.math.ceil((height + rowSpacing) / (unit + rowSpacing)).toInt().coerceAtLeast(1)

    // Matches ElementBoxGrid's actual cell layout (56dp icon + 4dp top/bottom padding + 8dp spacer + a
    // label line) and WidgetCard's own 12dp top/bottom content padding. An element box isn't a fixed
    // spanY-based size like other rows — it grows with how many icons it holds — so page-overflow
    // rebalancing needs this to predict its real height instead of assuming the spanY default.
    private val elementBoxIconCellHeight = 88.dp
    private val elementBoxCardPadding = 24.dp

    fun elementBoxHeight(entryCount: Int, columns: Int): Dp {
        val iconRows = if (entryCount <= 0) 1 else (entryCount + columns - 1) / columns
        return elementBoxCardPadding + elementBoxIconCellHeight * iconRows
    }
}

/** The current page's row unit height — [PageRowDimens.rowUnitHeight] stretched so that the page's
 *  unit count exactly fills its height. */
val LocalRowUnitHeight = staticCompositionLocalOf { PageRowDimens.rowUnitHeight }

/** Rounds a row's height up to whole row units of [unit] (content keeps its natural layout inside),
 *  so rows whose height comes from their content still line up with the page's row grid. */
fun Modifier.snapHeightToRowGrid(unit: Dp): Modifier = layout { measurable, constraints ->
    val natural = measurable.minIntrinsicHeight(constraints.maxWidth).toDp()
    val units = PageRowDimens.unitsFor(natural, unit)
    val height = PageRowDimens.spanHeight(unit, units).roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}
