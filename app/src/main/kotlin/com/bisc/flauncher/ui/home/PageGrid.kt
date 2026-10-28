package com.bisc.flauncher.ui.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.ui.home.dock.DockDimens

/**
 * A carousel page's row grid: how many row units fit ([unitCount]) and how tall one unit is drawn
 * ([unit], stretched so the units exactly fill the page). Pages don't scroll, so this decides both
 * when a page is full (PageContentRepository) and how its rows are drawn (HomeScreen); the widget
 * editor uses it to offer row-exact heights.
 */
data class PageGrid(val unitCount: Int, val unit: Dp) {
    companion object {
        /** The height left for rows: the window minus the top inset and margin above them, and below
         *  them either the Dock ([dock], with one row gap) or a margin, plus the navigation bar. */
        fun viewport(windowHeight: Dp, topInset: Dp, navigationBar: Dp, dock: PageRow?): Dp =
            windowHeight - topInset - 16.dp - navigationBar -
                if (dock != null) DockDimens.height(dock) + PageRowDimens.rowSpacing else 16.dp

        fun of(windowHeight: Dp, topInset: Dp, navigationBar: Dp, dock: PageRow?): PageGrid {
            val viewport = viewport(windowHeight, topInset, navigationBar, dock)
            val count = PageRowDimens.unitCount(viewport)
            return PageGrid(count, PageRowDimens.stretchedUnit(viewport, count))
        }
    }
}
