package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Geometry of the dock, scaled to the available width. */
object DockDimens {
    val horizontalMargin = 24.dp
    val topMargin = 0.dp
    val bottomMargin = 56.dp

    val contentGap = 8.dp

    private val referencePillWidth = 354.dp
    private val referenceBarHeight = 84.dp
    private val referenceCornerRadius = 32.dp
    private val referenceInnerHorizontalPadding = 16.dp
    private val referenceInnerVerticalPadding = 16.dp
    private val referenceItemSpacing = 12.dp
    private val referenceIconSize = 52.dp
    private val referenceButtonSize = 52.dp
    private val referenceDividerWidth = 1.dp
    private val referenceIconCornerRadius = 16.dp

    fun scaled(availableWidth: Dp): ScaledDock {
        val scale = (availableWidth / referencePillWidth).coerceIn(0.85f, 1.4f)
        return ScaledDock(
            scale = scale,
            pillWidth = referencePillWidth * scale,
            barHeight = referenceBarHeight * scale,
            cornerRadius = referenceCornerRadius * scale,
            innerHorizontalPadding = referenceInnerHorizontalPadding * scale,
            innerVerticalPadding = referenceInnerVerticalPadding * scale,
            itemSpacing = referenceItemSpacing * scale,
            iconSize = referenceIconSize * scale,
            buttonSize = referenceButtonSize * scale,
            iconCornerRadius = referenceIconCornerRadius * scale,
            buttonSlotWidth = referenceButtonSize * scale + referenceInnerHorizontalPadding * scale +
                referenceItemSpacing * scale + referenceDividerWidth * scale
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
    val iconSize: Dp,
    val buttonSize: Dp,
    val iconCornerRadius: Dp,
    val buttonSlotWidth: Dp
)
