package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.home.dock.resolveElementColors

/** App overview's own floating search button - jumps straight into the Text-search utility page.
 *  Its own fixed-size, fixed-inset footprint (see [AppOverviewDimens]), independent of the Dock. */
@Composable
fun AppOverviewSearchButton(
    onClick: () -> Unit,
    handedness: Handedness,
    cornerRadiusPercent: Int,
    appearance: ElementAppearance,
    modifier: Modifier = Modifier
) {
    val colors = resolveElementColors(appearance)
    val shape = RoundedCornerShape(percent = cornerRadiusPercent.coerceIn(0, 100))
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = if (handedness == Handedness.LEFT) AppOverviewDimens.horizontalMargin else 0.dp,
                end = if (handedness == Handedness.LEFT) 0.dp else AppOverviewDimens.horizontalMargin,
                bottom = AppOverviewDimens.bottomMargin + AppOverviewDimens.innerVerticalPadding
            ),
        contentAlignment = if (handedness == Handedness.LEFT) Alignment.BottomStart else Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .size(AppOverviewDimens.buttonSize)
                .clip(shape)
                .background(colors.background, alpha = colors.backgroundAlpha)
                .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.dock_search_hint),
                tint = colors.contentColor
            )
        }
    }
}
