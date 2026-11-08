package com.bisc.flauncher.ui.home.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bisc.flauncher.ui.theme.designGlow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.WidgetArrowStyle
import com.bisc.flauncher.data.model.WidgetPaging
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.noRippleClickable

/** One paging button of a widget: [forward] picks the direction, the size its frame (upright for
 *  the weather hours, wide for the days). A disabled one stays in place, just fainter. */
@Composable
fun PagingButton(
    forward: Boolean,
    width: Dp,
    height: Dp,
    style: WidgetPaging,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(percent = style.cornerRadiusPercent.coerceIn(0, 100) / 2)
    val own = style.appearance
    val colors = own?.let { resolveElementColors(it) }
    val tint = colors?.contentColor ?: LocalContentColor.current
    Box(
        modifier = modifier
            .size(width, height)
            .alpha(style.alphaPercent.coerceIn(0, 100) / 100f * if (enabled) 1f else 0.35f)
            .then(Modifier.designGlow(shape, "buttons"))
            .clip(shape)
            .then(
                if (colors != null) Modifier.background(colors.background, alpha = colors.backgroundAlpha)
                else Modifier.background(SolidColor(LocalContentColor.current.copy(alpha = 0.10f)))
            )
            .then(if (own?.borderEnabled == true) Modifier.border(colors!!.borderWidth, colors.borderColor, shape) else Modifier)
            .noRippleClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val (icon, turned) = arrowIcon(style.arrowStyle, forward)
        Icon(
            icon,
            contentDescription = stringResource(if (forward) R.string.widget_page_next else R.string.widget_page_previous),
            tint = tint,
            modifier = Modifier.size(minOf(width, height) * 0.7f).then(if (turned) Modifier.rotate(180f) else Modifier)
        )
    }
}

/** The icon for [style] and whether it has to be turned around to point back. */
fun arrowIcon(style: WidgetArrowStyle, forward: Boolean): Pair<ImageVector, Boolean> = when (style) {
    WidgetArrowStyle.CHEVRON -> (if (forward) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft) to false
    WidgetArrowStyle.TRIANGLE -> Icons.Filled.PlayArrow to !forward
    WidgetArrowStyle.ARROW -> (if (forward) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack) to false
}
