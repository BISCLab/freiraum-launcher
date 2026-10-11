package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometry of the dock, scaled to the available width. Proportions are a direct port of the user's Figma
 * dock redesign ("Ohne Namen" file, `dock-bar` frame, 390dp reference width): a near-edge-to-edge pill
 * (just [horizontalMargin] of breathing room, not the old 22dp inset) with 4 quick-launch icons and the
 * Hub orb spaced evenly across its full width — see [DockBarShapes] for the pill's own silhouette path,
 * drawn from the same reference and scaled by [ScaledDock.scale].
 */
object DockDimens {
    // Figma's own dock sits ~6dp from the screen edge; a few dp more here keeps the pill from touching the
    // display cutout/rounded corners on narrower phones, while still reading as "edge to edge" like the
    // reference, not the old 22dp pill-in-the-middle look.
    val horizontalMargin = 10.dp
    val topMargin = 0.dp
    val bottomMargin = 56.dp

    val contentGap = 8.dp

    // Figma reference, all at the 390dp pill width: 8dp padding from each pill edge to the first/last
    // icon, then one uniform 15.5dp gap between every adjacent element (icon-icon or icon-orb) — the orb
    // takes the center slot rather than being dropped on top of the bar. Icons are a 60dp "squircle"
    // (19dp corner radius — deliberately short of a true circle); the orb is 72dp, i.e. *smaller* than the
    // bar's own 92dp bounding height, so it reads as nested into the pill's shallow central hill rather
    // than a huge ball perched above it.
    private const val REFERENCE_PILL_WIDTH = 390f
    private val referencePillWidth = REFERENCE_PILL_WIDTH.dp
    private val referenceBarHeight = DockBarShapes.PLAIN_VIEWBOX_HEIGHT.dp
    private val referenceInnerHorizontalPadding = 8.dp
    private val referenceItemGap = 15.5.dp
    private val referenceIconSize = 60.dp
    private val referenceIconCornerRadius = 19.dp
    private val referenceHubSize = 72.dp

    // The aurora/glass dock style scales its icons up slightly (64dp reference) and makes them fully
    // circular rather than squircles — see AURORA_VIEWBOX in DockBarShapes for the matching pill body.
    private val referenceAuroraIconSize = 64.dp

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
            hubSize = referenceHubSize * scale,
            auroraIconSize = referenceAuroraIconSize * scale
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
