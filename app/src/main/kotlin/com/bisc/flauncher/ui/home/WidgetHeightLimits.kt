package com.bisc.flauncher.ui.home

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.ui.home.dock.DockDimens
import kotlin.math.roundToInt

/**
 * The largest a SYSTEM_WIDGET row is ever allowed to be sized to, in px. [PagedRows] will still clip a
 * too-tall widget at render time as a last resort, but that abrupt, last-moment squeeze is itself what
 * has caused real widget-process crashes for some third-party widgets — so this cap exists to keep a
 * widget from ever being sized past what genuinely fits in the first place. Shared by the settings height
 * stepper and the dock-conflict auto-relocate check so both agree on exactly the same threshold; this is
 * a page-level approximation (it doesn't know what other rows already sit above this one on the page).
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
        val safetyMarginPx = 24.dp.toPx()
        val viewportPx = screenHeightPx - topChromePx - bottomReservePx - safetyMarginPx
        return viewportPx.roundToInt().coerceAtLeast(PageRowDimens.rowUnitHeight.toPx().roundToInt())
    }
}
