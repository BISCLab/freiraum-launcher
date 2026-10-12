package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometry of the dock — a proportionally-scaled port of the user's Figma dock redesign ("Ohne Namen"
 * file, `dock-bar` frame, 390dp reference width): every dimension below is the measured Figma value at
 * that reference width, and [scaled] multiplies all of them by the same factor so the whole dock scales
 * together exactly like the Figma frame would at a different width. The bar is now a flat, uniform row —
 * every slot (app or power button) the same size — with no special center element, so there's no longer a
 * separate "hub" size/gap to track; see [DockCornerStyle]/[LocalDockCornerRadius] for the bar's own corner
 * radius, which is a user choice rather than a Figma-derived constant.
 */
object DockDimens {
    val horizontalMargin = 6.dp
    val topMargin = 0.dp
    val bottomMargin = 48.dp

    val contentGap = 8.dp

    // Figma reference, all at the 390dp pill width.
    private const val REFERENCE_PILL_WIDTH = 390f
    private val referencePillWidth = REFERENCE_PILL_WIDTH.dp
    private val referenceBarHeight = 84.dp
    private val referenceInnerHorizontalPadding = 12.dp
    private val referenceIconSize = 56.dp
    private val referenceIconCornerRadius = 16.dp

    fun scaled(availableWidth: Dp): ScaledDock {
        val scale = (availableWidth / referencePillWidth).coerceIn(0.85f, 1.6f)
        val iconSize = referenceIconSize * scale
        val barHeight = referenceBarHeight * scale
        return ScaledDock(
            scale = scale,
            pillWidth = availableWidth,
            barHeight = barHeight,
            cornerRadius = DockCornerStyle.ROUNDED.radius,
            innerHorizontalPadding = referenceInnerHorizontalPadding * scale,
            iconSize = iconSize,
            iconCornerRadius = referenceIconCornerRadius * scale,
            // Kept for the other dock-matched surfaces that size themselves off these (the keyboard's own
            // action bar/buttons, the app-list overlay's floating search button) — there's no more special
            // "hub" button to derive them from, so they just alias to the now-uniform icon size/padding.
            buttonSize = iconSize,
            itemSpacing = referenceInnerHorizontalPadding * scale,
            innerVerticalPadding = ((barHeight - iconSize) / 2).coerceAtLeast(0.dp)
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
    val iconSize: Dp,
    val iconCornerRadius: Dp,
    val buttonSize: Dp = iconSize,
    val itemSpacing: Dp = innerHorizontalPadding,
    val innerVerticalPadding: Dp = 0.dp
)
