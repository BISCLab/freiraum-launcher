package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometry of the dock. Icon size is fixed — not scaled by screen width — tuned so exactly
 * [REFERENCE_SLOT_COUNT] slots (the default width target) comfortably fill a typical ~400dp-wide phone
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
    val barHeight = iconSize + innerVerticalPadding * 2

    const val REFERENCE_SLOT_COUNT = 5

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
        cornerRadius = DockCornerStyle.ROUNDED.radius,
        innerHorizontalPadding = innerHorizontalPadding,
        iconSize = iconSize,
        iconCornerRadius = iconCornerRadius,
        buttonSize = iconSize,
        itemSpacing = itemGap,
        innerVerticalPadding = innerVerticalPadding
    )
}

/** Whether the dock bar's width is pinned to [DockDimens.REFERENCE_SLOT_COUNT] slots or grows with the actual slot count. */
enum class DockWidthMode { FIXED, GROW }

/** Corner radius of the Layer-2 widgets/cards (see WidgetCard). */
val WidgetCornerRadius = 20.dp

/** When non-null, replaces the scaled pill corner radius of the dock and of every panel that shares its shape. */
val LocalDockCornerRadius = androidx.compose.runtime.compositionLocalOf<Dp?> { null }

/** Dock dimensions, with the configured corner radius resolved. [availableWidth] is unused for sizing now
 *  (icon size is fixed) but kept so existing callers (the dock itself, the search keyboard, etc.) don't
 *  need to change; [pillWidth] field here is just [availableWidth] passed through for layout padding math
 *  elsewhere — the dock's own actual width comes from [DockDimens.pillWidth] instead, which also needs
 *  the live slot count. */
@androidx.compose.runtime.Composable
fun dockDimens(availableWidth: Dp): ScaledDock {
    val override = LocalDockCornerRadius.current
    return ScaledDock(
        pillWidth = availableWidth,
        barHeight = DockDimens.barHeight,
        cornerRadius = override ?: DockCornerStyle.ROUNDED.radius,
        innerHorizontalPadding = DockDimens.innerHorizontalPadding,
        iconSize = DockDimens.iconSize,
        iconCornerRadius = DockDimens.iconCornerRadius,
        buttonSize = DockDimens.iconSize,
        itemSpacing = DockDimens.itemGap,
        innerVerticalPadding = DockDimens.innerVerticalPadding
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
