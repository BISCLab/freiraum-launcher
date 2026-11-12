package com.bisc.flauncher.ui.home.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.bisc.flauncher.ui.theme.designGlow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Shape
import com.bisc.flauncher.data.model.ElementAppearance

/** Shared card shell for the Panel, the Dock and the Folder card. */
@Composable
fun DockPanelSurface(
    appearance: ElementAppearance,
    shape: Shape,
    modifier: Modifier = Modifier,
    /** Which kind of surface this is for a design's glowing border (dock, panel, folders, cards). */
    effectTarget: String? = null,
    /** While opening: how much of the card shows (0 to 1). The card itself grows, so its shadow and
     *  edges come along at every step; the content keeps its full size and is cut by the shape.
     *  Needs loose height constraints from outside (e.g. wrapContentHeight). */
    reveal: (() -> Float)? = null,
    revealFromBottom: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = resolveElementColors(appearance)
    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        Box(
            modifier = modifier
                .then(if (effectTarget != null) Modifier.designGlow(shape, effectTarget) else Modifier)
                .clip(shape)
                .background(colors.background, alpha = colors.backgroundAlpha)
                .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier)
                .then(
                    if (reveal == null) Modifier else Modifier.layout { measurable, constraints ->
                        val full = measurable.measure(constraints.copy(minHeight = constraints.maxHeight))
                        val shown = (full.height * reveal().coerceIn(0f, 1f)).roundToInt()
                        layout(full.width, shown) { full.place(0, if (revealFromBottom) shown - full.height else 0) }
                    }
                )
        ) {
            content()
        }
    }
}
