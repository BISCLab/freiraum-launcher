package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometry of the dock. Icon and orb sizes are deliberately *fixed* (not scaled by screen width) — the
 * same approach iOS's own dock and most real launchers use: a comfortable, constant touch-target size on
 * every device, phone or tablet, rather than icons ballooning on a wide screen or shrinking on a narrow
 * one. Only the pill's own width and the gaps between its icons flex to fill whatever space is actually
 * available (see [scaled]), which is what makes this responsive across screen sizes in the first place.
 */
object DockDimens {
    val horizontalMargin = 12.dp
    val topMargin = 0.dp
    val bottomMargin = 48.dp

    val contentGap = 8.dp

    // Fixed sizes. The orb is noticeably larger than a quick-launch icon — it's the one special control —
    // and larger than the bar itself, so unioning its circle into the bar's own rounded-rect silhouette
    // (see SearchBar's Canvas draw) naturally produces a shallow "crown" around it instead of a flat top.
    val iconSize = 56.dp
    val iconCornerRadius = 18.dp
    private val barVerticalPadding = 10.dp
    val barHeight = iconSize + barVerticalPadding * 2
    val cornerRadius = barHeight / 2
    val hubSize = 88.dp
    val innerHorizontalPadding = 14.dp

    // The aurora/glass dock style's icons are circular rather than squircle, and a touch larger.
    val auroraIconSize = 60.dp

    private const val SIDE_ICON_COUNT = 4
    private val minItemGap = 10.dp

    fun scaled(availableWidth: Dp): ScaledDock {
        val contentWidth = iconSize * SIDE_ICON_COUNT + hubSize
        val naturalGap = (availableWidth - innerHorizontalPadding * 2 - contentWidth) / SIDE_ICON_COUNT
        val gap = if (naturalGap > minItemGap) naturalGap else minItemGap
        val pillWidth = (contentWidth + innerHorizontalPadding * 2 + gap * SIDE_ICON_COUNT).coerceAtMost(availableWidth)
        return ScaledDock(
            scale = 1f,
            pillWidth = pillWidth,
            barHeight = barHeight,
            cornerRadius = cornerRadius,
            innerHorizontalPadding = innerHorizontalPadding,
            innerVerticalPadding = barVerticalPadding,
            itemSpacing = gap,
            hubGap = gap,
            iconSize = iconSize,
            buttonSize = hubSize,
            iconCornerRadius = iconCornerRadius,
            buttonSlotWidth = hubSize + gap * 2,
            hubSize = hubSize,
            auroraIconSize = auroraIconSize
        )
    }
}

/** Which look the dock pill itself uses; the orb design ([DockOrbDesign]) is chosen independently. */
enum class DockStyle { PLAIN, AURORA }

/** Corner radius of the Layer-2 widgets/cards (see WidgetCard). */
val WidgetCornerRadius = 20.dp

/** When non-null, replaces the scaled pill corner radius of the dock and of every panel that shares its shape. */
val LocalDockCornerRadius = androidx.compose.runtime.compositionLocalOf<Dp?> { null }

/** Scaled dock dimensions including the configured corner radius. */
@androidx.compose.runtime.Composable
fun dockDimens(availableWidth: Dp): ScaledDock {
    val d = DockDimens.scaled(availableWidth)
    val override = LocalDockCornerRadius.current
    return if (override != null) d.copy(cornerRadius = override) else d
}

/** One consistently-scaled snapshot of every dock dimension. */
data class ScaledDock(
    val scale: Float,
    val pillWidth: Dp,
    val barHeight: Dp,
    val cornerRadius: Dp,
    val innerHorizontalPadding: Dp,
    val innerVerticalPadding: Dp,
    val itemSpacing: Dp,
    val hubGap: Dp,
    val iconSize: Dp,
    val buttonSize: Dp,
    val iconCornerRadius: Dp,
    val buttonSlotWidth: Dp,
    val hubSize: Dp,
    val auroraIconSize: Dp = iconSize
)
