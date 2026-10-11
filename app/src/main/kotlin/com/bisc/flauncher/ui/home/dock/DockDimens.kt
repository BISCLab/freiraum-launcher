package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometry of the dock — a proportionally-scaled port of the user's Figma dock redesign ("Ohne Namen"
 * file, `dock-bar` frame, 390dp reference width): every dimension below is the measured Figma value at
 * that reference width, and [scaled] multiplies all of them by the same factor so the whole dock scales
 * together exactly like the Figma frame would at a different width, rather than icons staying a fixed
 * size while only the pill stretches. [barHeight] is the *flat* bar height — icons/orb both center on it
 * via plain `Alignment.Center`, since [DockPillShape]'s own hill-extra padding is symmetric top/bottom
 * (see that class's doc comment for why `Alignment.Center` still lands on the bar's own middle despite
 * the Box being taller than the flat bar to make room for the hill).
 */
object DockDimens {
    val horizontalMargin = 6.dp
    val topMargin = 0.dp
    val bottomMargin = 48.dp

    val contentGap = 8.dp

    // Figma reference, all at the 390dp pill width. referenceBarHeight (71dp) is the pill's flat-sided
    // height measured directly off Figma's own rendered PNG — not the silhouette path's full 92dp
    // bounding box, which includes the hump's overflow above/below the flat region.
    private const val REFERENCE_PILL_WIDTH = 390f
    private val referencePillWidth = REFERENCE_PILL_WIDTH.dp
    val referenceBarHeight = 71.dp
    private val referenceInnerHorizontalPadding = 8.dp
    private val referenceItemGap = 15.5.dp
    private val referenceIconSize = 60.dp
    private val referenceIconCornerRadius = 19.dp
    private val referenceHubSize = 72.dp

    fun scaled(availableWidth: Dp): ScaledDock {
        val scale = (availableWidth / referencePillWidth).coerceIn(0.85f, 1.6f)
        return ScaledDock(
            scale = scale,
            pillWidth = availableWidth,
            barHeight = referenceBarHeight * scale,
            cornerRadius = (referenceBarHeight / 2) * scale,
            innerHorizontalPadding = referenceInnerHorizontalPadding * scale,
            innerVerticalPadding = 14.dp * scale,
            itemSpacing = referenceItemGap * scale,
            hubGap = referenceItemGap * scale,
            iconSize = referenceIconSize * scale,
            buttonSize = referenceHubSize * scale,
            iconCornerRadius = referenceIconCornerRadius * scale,
            buttonSlotWidth = referenceHubSize * scale + referenceItemGap * 2 * scale,
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
