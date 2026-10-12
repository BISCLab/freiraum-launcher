package com.bisc.flauncher.ui.home.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sizing for the full-text search screen — deliberately independent from the dock's own
 * DockDimens, search is a standalone, optional feature with its own footprint. [referenceWidth]
 * is numerically copied from the dock's own default fixed width (`DockDimens.widthForSlotCount(5)`
 * = 392dp) purely as a convenient starting value picked at build time — not a live binding to the
 * dock (search redesign round 2 was explicit about this: "nur zum Programmieren... damit nicht
 * weiter verknüpft"). Only the search field/category bar and the result cards use this width; the
 * keyboard-frame strip is deliberately edge-to-edge instead (see [SearchOverlay]).
 */
object SearchDimens {
    val horizontalMargin = 16.dp
    val bottomMargin = 24.dp
    val cornerRadius = 28.dp
    val innerHorizontalPadding = 14.dp
    val innerVerticalPadding = 10.dp
    val iconSize = 46.dp
    val iconCornerRadius = 14.dp
    val itemSpacing = 10.dp
    val referenceWidth = 392.dp
}

/** One resolved snapshot of [SearchDimens] for a given available width. */
data class ScaledSearch(
    val pillWidth: Dp,
    val cornerRadius: Dp = SearchDimens.cornerRadius,
    val innerHorizontalPadding: Dp = SearchDimens.innerHorizontalPadding,
    val innerVerticalPadding: Dp = SearchDimens.innerVerticalPadding,
    val iconSize: Dp = SearchDimens.iconSize,
    val iconCornerRadius: Dp = SearchDimens.iconCornerRadius,
    val buttonSize: Dp = SearchDimens.iconSize,
    val itemSpacing: Dp = SearchDimens.itemSpacing
)

@Composable
fun searchDimens(availableWidth: Dp): ScaledSearch =
    ScaledSearch(pillWidth = SearchDimens.referenceWidth.coerceAtMost(availableWidth))
