package com.bisc.flauncher.ui.home.dock

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.DockPowerButtonAction
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.settings.IconShape

/**
 * Dock: a flat row of uniform squircle buttons — every slot the exact same size and shape whether it's a
 * pinned app or a power button, proportionally scaled from the Figma reference (see [DockDimens]). There
 * is no longer a special "Hub orb" with its own gesture vocabulary (tap/swipe/drag); that whole control
 * was replaced by [DockSlotUi.PowerButton] slots — plain, single-purpose buttons the user places
 * (text search, category search, app drawer, app list, ...) alongside their pinned apps, in whatever
 * order and count they want. The bar's own corner radius comes from [dockDimens]'s `cornerRadius` — see
 * [DockCornerStyle] and [LocalDockCornerRadius], which HomeScreen provides from the user's corner-style
 * setting, the same value every other dock-matched surface (e.g. the text-search field) already reads.
 * [appearance] controls the bar's own fill/border, same per-row customization every other dock-style row
 * already has.
 */
@Composable
fun SearchBar(
    slots: List<DockSlotUi>,
    onAppClick: (ComponentName) -> Unit,
    onUnsetAppClick: (Int) -> Unit,
    onPowerButtonClick: (DockPowerButtonAction) -> Unit,
    modifier: Modifier = Modifier,
    iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    appearance: ElementAppearance = ElementAppearance(),
    loadQuickLaunchIcon: (ComponentName) -> ImageBitmap? = { null }
) {
    val colors = resolveElementColors(appearance)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = DockDimens.horizontalMargin,
                end = DockDimens.horizontalMargin,
                top = DockDimens.topMargin,
                bottom = DockDimens.bottomMargin
            ),
        contentAlignment = Alignment.Center
    ) {
        val d = dockDimens(maxWidth)
        val itemShape = when (iconShape) {
            IconShape.CIRCLE -> CircleShape
            IconShape.ROUNDED_SQUARE -> RoundedCornerShape(d.iconCornerRadius)
        }
        val barShape = remember(d.cornerRadius) { RoundedCornerShape(d.cornerRadius) }

        @Composable
        fun SlotButton(slot: DockSlotUi) {
            when (slot) {
                is DockSlotUi.App -> {
                    val componentName = slot.componentName
                    val bitmap = remember(componentName) { componentName?.let(loadQuickLaunchIcon) }
                    Box(
                        modifier = Modifier
                            .size(d.iconSize)
                            .clip(itemShape)
                            .background(if (bitmap == null) slot.backgroundColor else Color.Transparent)
                            .clickable(onClick = { if (componentName != null) onAppClick(componentName) else onUnsetAppClick(slot.index) }),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(bitmap = bitmap, contentDescription = slot.label, modifier = Modifier.size(d.iconSize))
                        } else {
                            Icon(imageVector = unsetAppSlotIcon, contentDescription = null, tint = slot.iconColor)
                        }
                    }
                }
                is DockSlotUi.PowerButton -> {
                    Box(
                        modifier = Modifier
                            .size(d.iconSize)
                            .clip(itemShape)
                            .background(slot.backgroundColor)
                            .clickable(onClick = { onPowerButtonClick(slot.action) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = slot.action.defaultIcon(), contentDescription = null, tint = slot.iconColor)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .width(d.pillWidth)
                .height(d.barHeight)
                .shadow(elevation = 10.dp, shape = barShape, clip = false)
                .then(
                    if (colors.backgroundAlpha > 0f) Modifier.clip(barShape).background(colors.background, alpha = colors.backgroundAlpha)
                    else Modifier
                )
                .then(
                    if (appearance.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, barShape) else Modifier
                )
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = d.innerHorizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                slots.forEach { slot -> SlotButton(slot) }
            }
        }
    }
}
