package com.bisc.flauncher.ui.home.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.WidgetCornerStyle
import com.bisc.flauncher.ui.home.dock.resolveElementColors

/** Shared card shell for the built-in widgets (Music/Weather/Notes) and the element box grid. */
@Composable
fun WidgetCard(
    modifier: Modifier = Modifier,
    appearance: ElementAppearance = ElementAppearance(),
    cornerStyle: WidgetCornerStyle = WidgetCornerStyle.ROUNDED,
    horizontalMargin: Dp = 0.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = resolveElementColors(appearance)
    val shape = when (cornerStyle) {
        WidgetCornerStyle.SQUARE -> RoundedCornerShape(0.dp)
        WidgetCornerStyle.ROUNDED -> RoundedCornerShape(com.bisc.flauncher.ui.home.dock.WidgetCornerRadius)
    }

    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = horizontalMargin)
                .clip(shape)
                .background(colors.background, alpha = colors.backgroundAlpha)
                .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier)
                .padding(contentPadding),
            content = content
        )
    }
}
