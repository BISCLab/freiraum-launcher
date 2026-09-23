package com.bisc.flauncher.ui.home

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.ui.home.dock.DockDimens
import kotlin.math.roundToInt

/**
 * The largest a SYSTEM_WIDGET row is ever allowed to be sized to, in px. Pages don't scroll — a page is
 * exactly one screen — so a widget sized past this would simply overflow off-screen with nothing to
 * compensate; this cap keeps that from ever happening in the first place. Shared by the settings height
 * stepper and the page-overflow rebalancing check so both agree on exactly the same threshold; this is
 * a page-level approximation (it doesn't know what other rows already sit above this one on the page —
 * rebalancing handles that separately by moving rows to a new page once they no longer fit together).
 */
fun safeSystemWidgetMaxHeightPx(
    pageHasDock: Boolean,
    screenWidthPx: Int,
    screenHeightPx: Int,
    densityValue: Float,
    statusBarPx: Int,
    navigationBarPx: Int
): Int {
    val density = Density(densityValue)
    with(density) {
        val innerWidthDp = ((screenWidthPx / densityValue).dp - DockDimens.horizontalMargin * 2).coerceAtLeast(0.dp)
        val dockFootprintPx = DockDimens.scaled(innerWidthDp)
            .let { it.barHeight + DockDimens.topMargin + DockDimens.bottomMargin }.toPx() + DockDimens.contentGap.toPx()
        val topChromePx = statusBarPx + 16.dp.toPx()
        val bottomReservePx = if (pageHasDock) dockFootprintPx else navigationBarPx + 16.dp.toPx()
        // Was 24dp — that on top of an *already* accurate top-chrome reservation (real status bar +
        // display cutout, not a guess) ate enough budget to reject page layouts that visibly fit fine,
        // like 3+2+1 row-unit built-in widgets stacked above a dock. A small margin is enough now that
        // the inputs themselves are no longer the imprecise part.
        val safetyMarginPx = 4.dp.toPx()
        val viewportPx = screenHeightPx - topChromePx - bottomReservePx - safetyMarginPx
        return viewportPx.roundToInt().coerceAtLeast(PageRowDimens.rowUnitHeight.toPx().roundToInt())
    }
}
