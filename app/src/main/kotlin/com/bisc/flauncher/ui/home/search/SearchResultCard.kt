package com.bisc.flauncher.ui.home.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.ui.home.dock.DockPanelSurface

/** Layer 2 surface containing all search results. */
@Composable
fun SearchResultsPanel(
    appearance: ElementAppearance,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    DockPanelSurface(
        appearance = appearance,
        shape = shape,
        modifier = modifier
    ) {
        content()
    }
}
