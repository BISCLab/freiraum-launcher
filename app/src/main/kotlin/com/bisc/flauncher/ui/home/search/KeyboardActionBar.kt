package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape

/** The keyboard's own bottom action row. */
@Composable
fun KeyboardActionBar(
    onHome: () -> Unit,
    onTogglePage: () -> Unit,
    onSpace: () -> Unit,
    showingNumbersPage: Boolean,
    handedness: Handedness,
    iconShape: IconShape,
    d: ScaledSearch,
    background: Color,
    border: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    val itemShape = when (iconShape) {
        IconShape.CIRCLE -> CircleShape
        IconShape.ROUNDED_SQUARE -> RoundedCornerShape(d.iconCornerRadius)
    }
    val spaceWidth = d.iconSize * 3 + d.itemSpacing * 2

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(d.itemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val page = @Composable {
            ActionSlot(
                icon = if (showingNumbersPage) Icons.Filled.Abc else Icons.Filled.Dialpad,
                contentDescription = stringResource(
                    if (showingNumbersPage) R.string.search_action_letters else R.string.search_action_numbers
                ),
                size = d.iconSize,
                shape = itemShape,
                onClick = onTogglePage
            )
        }
        val space = @Composable {
            Box(
                modifier = Modifier
                    .height(d.iconSize)
                    .width(spaceWidth)
                    .clip(itemShape)
                    .background(LocalContentColor.current.copy(alpha = 0.08f))
                    .clickable(onClick = onSpace),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.SpaceBar,
                    contentDescription = stringResource(R.string.search_action_space),
                    tint = LocalContentColor.current.copy(alpha = 0.7f)
                )
            }
        }
        val actionSlots = @Composable {
            Row(
                horizontalArrangement = Arrangement.spacedBy(d.itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (handedness == Handedness.LEFT) {
                    space(); page()
                } else {
                    page(); space()
                }
            }
        }
        val divider = @Composable {
            Box(modifier = Modifier.height(d.buttonSize).width(1.dp).background(dividerColor))
        }
        val homeButton = @Composable {
            Box(
                modifier = Modifier
                    .size(d.buttonSize)
                    .clip(itemShape)
                    .background(background)
                    .border(1.dp, border, itemShape)
                    .clickable(onClick = onHome),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = stringResource(R.string.search_cluster_home),
                    tint = LocalContentColor.current.copy(alpha = 0.8f)
                )
            }
        }

        if (handedness == Handedness.LEFT) {
            homeButton(); divider(); actionSlots()
        } else {
            actionSlots(); divider(); homeButton()
        }
    }
}

@Composable
private fun ActionSlot(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(LocalContentColor.current.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = LocalContentColor.current.copy(alpha = 0.7f))
    }
}
