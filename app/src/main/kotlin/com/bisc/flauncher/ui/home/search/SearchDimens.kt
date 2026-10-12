package com.bisc.flauncher.ui.home.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sizing for the full-text search screen (keyboard, system search field) — deliberately independent
 * from the dock's own DockDimens. Search used to derive every one of these numbers from the dock's
 * pill width/corner radius, which tied its look to however the dock happened to be sized/configured;
 * search is now a standalone, optional feature with its own footprint: nearly full screen width (a
 * bit wider than the dock pill, closer to the swipe-down Apps panel's own margin) and its own,
 * generous corner rounding.
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
fun searchDimens(availableWidth: Dp): ScaledSearch = ScaledSearch(pillWidth = availableWidth)
