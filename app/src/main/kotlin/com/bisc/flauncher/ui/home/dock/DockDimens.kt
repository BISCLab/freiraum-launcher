package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.DockWidthMode

/**
 * Geometry of a single-line/pill row (the dock being the one that floats; any other row can opt into
 * the same layout). Icon size is fixed — not scaled by screen width — tuned so exactly
 * [REFERENCE_SLOT_COUNT] entries (the default width target) comfortably fill a typical ~400dp-wide phone
 * screen with icons at a generous, consistent size; see [DockWidthMode] for how the bar's own width is
 * derived from that per-icon size, and [pillWidth]/[widthForSlotCount] for the actual calculation.
 */
object DockDimens {
    val horizontalMargin = 6.dp
    val topMargin = 0.dp
    val bottomMargin = 48.dp
    val contentGap = 8.dp

    val iconSize = 64.dp
    val iconCornerRadius = 18.dp
    val innerHorizontalPadding = 12.dp
    val itemGap = 12.dp
    val innerVerticalPadding = 16.dp
    // Vertical gap between the Dock's own sub-rows when it's 2 rows tall — bigger than the near-zero
    // gap GridEntries' rowColumns path uses by default (rows just stacked back to back), since a
    // flush 2-row dock reads as cramped.
    val rowGap = 20.dp

    // Actual rendered height of one Dock sub-row's content — a bare icon cell (its 56dp image plus
    // the cell Column's own 4dp top/bottom padding) when captions are off; rowHeightWithLabel adds
    // the caption line + its spacer for when PageRow.showLabels is on. Used to size the Dock's fixed-
    // height AnimatedVisibility box exactly, instead of a flat generous guess — the previous one left
    // unused centering slack above the top row (on top of the deliberate top padding) and made the
    // whole dock taller than it needed to be.
    val rowHeight = 64.dp
    val rowHeightWithLabel = 92.dp
    val barHeight = iconSize + innerVerticalPadding * 2
    // Extra height a pill bar needs to fit a caption line under each icon (PageRow.showLabels) —
    // only relevant to PillRow's own fixed-height Box; PillRowEntries used directly inside a
    // naturally-sizing container (the live dock's DockPanelSurface, a settings preview) just grows.
    val labelExtraHeight = 20.dp

    const val REFERENCE_SLOT_COUNT = 5

    /** Max corner radius for "dock-matched" surfaces that have no [com.bisc.flauncher.data.model.PageRow]
     *  of their own to read a real `cornerRadiusPercent` from (the app overview's window-mode panel) —
     *  scaled by the global corner-roundness default instead. The dock bar itself no longer uses this,
     *  it reads its own row's `cornerRadiusPercent` (see the pill rendering in PageRowContent.kt). */
    val defaultCornerRadius = 32.dp

    /** Width needed to fit exactly [count] icons at their fixed size, with the bar's own edge padding and inter-icon gaps. */
    fun widthForSlotCount(count: Int): Dp {
        val n = count.coerceAtLeast(1)
        return innerHorizontalPadding * 2 + iconSize * n + itemGap * (n - 1)
    }

    /**
     * [DockWidthMode.FIXED] (default): the bar is always exactly [REFERENCE_SLOT_COUNT] slots wide,
     * regardless of how many are actually filled — fewer slots spread out within that width via
     * `Arrangement.SpaceBetween`, which is what makes 5 slots "find maximum space" the default look.
     * [DockWidthMode.GROW]: the bar's width tracks the actual slot count instead, growing wider as more
     * are added rather than ever compressing icon size. Either way the result is capped to
     * [availableWidth] as a narrow-screen safety net.
     */
    fun pillWidth(slotCount: Int, mode: DockWidthMode, availableWidth: Dp): Dp {
        val content = when (mode) {
            DockWidthMode.FIXED -> widthForSlotCount(REFERENCE_SLOT_COUNT)
            DockWidthMode.GROW -> widthForSlotCount(slotCount)
        }
        return content.coerceAtMost(availableWidth)
    }

    /** Non-composable snapshot of the dock's geometry, for surfaces that mirror the dock's footprint
     *  (the floating search button, the app overview's reserved bottom inset, a folder's dock replica,
     *  the system-widget height cap) but have no reason to track the user's width-mode/slot-count choice
     *  themselves — they assume the default fixed 5-slot width, same as [dockDimens] without an override. */
    fun scaled(availableWidth: Dp): ScaledDock = ScaledDock(
        pillWidth = pillWidth(REFERENCE_SLOT_COUNT, DockWidthMode.FIXED, availableWidth),
        barHeight = barHeight,
        cornerRadius = defaultCornerRadius,
        innerHorizontalPadding = innerHorizontalPadding,
        iconSize = iconSize,
        iconCornerRadius = iconCornerRadius,
        buttonSize = iconSize,
        itemSpacing = itemGap,
        innerVerticalPadding = innerVerticalPadding
    )
}

/** Corner radius of the Layer-2 widgets/cards (see WidgetCard). */
val WidgetCornerRadius = 20.dp

/** One consistently-resolved snapshot of the dock's dimensions. */
data class ScaledDock(
    val pillWidth: Dp,
    val barHeight: Dp,
    val cornerRadius: Dp,
    val innerHorizontalPadding: Dp,
    val iconSize: Dp,
    val iconCornerRadius: Dp,
    val buttonSize: Dp = iconSize,
    val itemSpacing: Dp = innerHorizontalPadding,
    val innerVerticalPadding: Dp = 0.dp
)
