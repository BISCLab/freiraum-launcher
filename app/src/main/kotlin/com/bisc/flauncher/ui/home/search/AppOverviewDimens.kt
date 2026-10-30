package com.bisc.flauncher.ui.home.search

import androidx.compose.ui.unit.dp

/**
 * Geometry for the App list screen's own chrome (floating search button, A-Z rail/list edge
 * inset) - deliberately independent from the Dock's own [com.bisc.flauncher.ui.home.dock.DockDimens],
 * the same way [SearchDimens] is independent for the Text-search screen.
 */
object AppOverviewDimens {
    // One shared edge-to-content gap for everything that touches the phone's left/right edge - the
    // app list rows, the A-Z rail and the floating search button - so they all sit at the same
    // visual inset instead of drifting apart. Bigger than a floating pill's own outer margin (see
    // DockDimens/SearchDimens) since this screen has no pill of its own to add inner padding on
    // top of - it's the only thing standing between the content and the phone's bezel.
    val horizontalMargin = 20.dp
    val bottomMargin = 48.dp
    val contentGap = 8.dp
    val innerVerticalPadding = 16.dp
    val buttonSize = 64.dp

    val barHeight = buttonSize + innerVerticalPadding * 2
}
