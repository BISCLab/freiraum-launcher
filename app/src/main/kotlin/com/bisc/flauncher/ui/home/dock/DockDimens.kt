package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometry of a single-line/pill row (the dock being the one that floats; any other row can opt into
 * the same layout). Icon size is fixed — not scaled by screen width — tuned so exactly
 * [REFERENCE_SLOT_COUNT] entries comfortably fill a typical ~400dp-wide phone screen at a generous,
 * consistent size. [horizontalMargin] is the fine standard side margin every in-page element keeps
 * unless it's set edge to edge.
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
    // Gap from the top row's icons to the Dock's own top edge — matches the gap from the leftmost
    // icon all the way to the *phone's* left edge (horizontalMargin, the pill's own outer inset,
    // plus innerHorizontalPadding, the icon's inset from the pill's edge), not just
    // innerHorizontalPadding alone, since that gap by itself is shorter than the true side gap.
    val topPadding = horizontalMargin + innerHorizontalPadding
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

    /** Non-composable snapshot of the dock's geometry, for surfaces that mirror the dock's footprint
     *  (a folder's dock replica, the system-widget height cap) but have no reason to track the user's
     *  width-mode/slot-count choice themselves — they assume the default fixed 5-slot width. */
    fun scaled(availableWidth: Dp): ScaledDock = ScaledDock(
        pillWidth = widthForSlotCount(REFERENCE_SLOT_COUNT).coerceAtMost(availableWidth),
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
