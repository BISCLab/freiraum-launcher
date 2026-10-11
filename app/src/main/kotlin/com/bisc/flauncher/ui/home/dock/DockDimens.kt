package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Geometry of the dock, scaled to the available width. */
object DockDimens {
    // Visible breathing room from the screen edges, not an edge-to-edge bar.
    val horizontalMargin = 22.dp
    val topMargin = 0.dp
    val bottomMargin = 56.dp

    val contentGap = 8.dp

    // One continuous pill whose own silhouette bulges around the Hub orb — a real union of the bar shape
    // and the orb's circle, not two independent layers that merely overlap. The orb's center sits exactly
    // on the bar's own center, both axes — not perched on top of it — and the orb is deliberately larger
    // than the bar is tall, so centering it still leaves it clearly, symmetrically breaking out above and
    // below: it widens the dock through its own size, not through being offset upward. A shorter bar also
    // makes its own rounded ends read as nearly full semicircles instead of a long flat-sided capsule.
    // Icons pair up tight to each other (referencePairGap) and leave generous, clearly-wider room around
    // the orb (referenceHubGap) so the grouping itself signals "these four are equivalent, that one is
    // special" — and the orb stays a clear accent, not the dominant element.
    private val referenceBarHeight = 80.dp
    private val referenceCornerRadius = referenceBarHeight / 2
    private val referenceInnerHorizontalPadding = 18.dp
    private val referenceInnerVerticalPadding = 14.dp
    private val referencePairGap = 12.dp
    private val referenceHubGap = 22.dp
    private val referenceIconSize = 52.dp
    private val referenceIconCornerRadius = 22.dp
    private val referenceHubSize = 116.dp

    private val referencePillWidth =
        referenceInnerHorizontalPadding * 2 + referenceIconSize * 4 + referencePairGap * 2 + referenceHubGap * 2 + referenceHubSize

    fun scaled(availableWidth: Dp): ScaledDock {
        val scale = (availableWidth / referencePillWidth).coerceIn(0.85f, 1.4f)
        return ScaledDock(
            scale = scale,
            pillWidth = referencePillWidth * scale,
            barHeight = referenceBarHeight * scale,
            cornerRadius = referenceCornerRadius * scale,
            innerHorizontalPadding = referenceInnerHorizontalPadding * scale,
            innerVerticalPadding = referenceInnerVerticalPadding * scale,
            itemSpacing = referencePairGap * scale,
            hubGap = referenceHubGap * scale,
            iconSize = referenceIconSize * scale,
            buttonSize = referenceHubSize * scale,
            iconCornerRadius = referenceIconCornerRadius * scale,
            buttonSlotWidth = referenceHubSize * scale + referenceHubGap * 2 * scale,
            hubSize = referenceHubSize * scale
        )
    }
}

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
    val hubSize: Dp
)
