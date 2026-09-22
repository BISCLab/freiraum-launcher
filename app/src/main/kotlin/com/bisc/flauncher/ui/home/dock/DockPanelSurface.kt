package com.bisc.flauncher.ui.home.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.ElementAppearance

/** Shared card shell for the Sidebar overview's floating panels and the Folder card. */
@Composable
fun DockPanelSurface(
    appearance: ElementAppearance,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = resolveElementColors(appearance)
    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(colors.background, alpha = colors.backgroundAlpha)
                .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier)
        ) {
            content()
        }
    }
}
