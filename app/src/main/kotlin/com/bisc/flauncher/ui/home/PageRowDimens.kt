package com.bisc.flauncher.ui.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One [com.bisc.flauncher.data.model.PageRow.spanY] unit's height. */
object PageRowDimens {
    val rowUnitHeight = 110.dp
    val rowSpacing = 16.dp

    // Matches AppsRowGrid's actual cell layout (56dp icon + 4dp top/bottom padding + 8dp spacer + a
    // label line) and WidgetCard's own 12dp top/bottom content padding. An apps row isn't a fixed
    // spanY-based size like other rows — it grows with how many icons it holds — so page-overflow
    // rebalancing needs this to predict its real height instead of assuming the spanY default.
    private val appsRowIconCellHeight = 88.dp
    private val appsRowCardPadding = 24.dp

    fun appsRowHeight(entryCount: Int, columns: Int): Dp {
        val iconRows = if (entryCount <= 0) 1 else (entryCount + columns - 1) / columns
        return appsRowCardPadding + appsRowIconCellHeight * iconRows
    }
}
