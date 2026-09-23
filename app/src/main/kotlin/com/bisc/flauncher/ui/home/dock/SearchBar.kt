package com.bisc.flauncher.ui.home.dock

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape
import com.bisc.flauncher.ui.theme.SearchButtonBorderColor
import com.bisc.flauncher.ui.theme.SearchButtonSurfaceColor

/** Dock: quick-launch icons and the search button. */
@Composable
fun SearchBar(
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
    handedness: Handedness = Handedness.RIGHT,
    iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    appearance: ElementAppearance = ElementAppearance(),
    // Layer 3: null keeps the automatic light/dark look derived from the dock's own (Layer 2) colors.
    searchButtonAppearance: ElementAppearance? = null,
    quickLaunchSlots: List<QuickLaunchSlot> = defaultQuickLaunchSlots,
    loadQuickLaunchIcon: (ComponentName) -> ImageBitmap? = { null },
    onQuickLaunchClick: (ComponentName) -> Unit = {}
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
        val pillShape = RoundedCornerShape(d.cornerRadius)
        val itemShape = when (iconShape) {
            IconShape.CIRCLE -> CircleShape
            IconShape.ROUNDED_SQUARE -> RoundedCornerShape(d.iconCornerRadius)
        }
        val usesLightPillDetailing = appearance.fillMode != ElementFillMode.NONE && colors.solidBackground.luminance() > 0.5f
        val contentColor = colors.contentColor
        Row(
            modifier = Modifier
                .width(d.pillWidth)
                .height(d.barHeight)
                .clip(pillShape)
                .background(colors.background, alpha = colors.backgroundAlpha)
                .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, pillShape) else Modifier)
                .padding(horizontal = d.innerHorizontalPadding, vertical = d.innerVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(d.itemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val quickLaunchRow = @Composable {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d.itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickLaunchSlots.forEach { slot ->
                        val bitmap = remember(slot.componentName) { slot.componentName?.let(loadQuickLaunchIcon) }
                        Box(
                            modifier = Modifier
                                .size(d.iconSize)
                                .clip(itemShape)
                                .background(if (bitmap == null) slot.placeholderTint else Color.Transparent)
                                .clickable(onClick = { slot.componentName?.let(onQuickLaunchClick) }),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(bitmap = bitmap, contentDescription = slot.label, modifier = Modifier.size(d.iconSize))
                            } else {
                                Icon(imageVector = slot.placeholderIcon, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            val divider = @Composable {
                Box(
                    modifier = Modifier
                        .height(d.buttonSize)
                        .width(1.dp)
                        .background(colors.dividerColor)
                )
            }

            val customSearchColors = searchButtonAppearance?.let { resolveElementColors(it) }
            val searchButton = @Composable {
                Box(
                    modifier = Modifier
                        .size(d.buttonSize)
                        .clip(itemShape)
                        .then(
                            when {
                                customSearchColors != null -> Modifier
                                    .background(customSearchColors.background, alpha = customSearchColors.backgroundAlpha)
                                    .then(
                                        if (searchButtonAppearance?.borderEnabled == true) {
                                            Modifier.border(customSearchColors.borderWidth, customSearchColors.borderColor, itemShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                usesLightPillDetailing -> Modifier
                                    .background(SearchButtonSurfaceColor)
                                    .border(1.dp, SearchButtonBorderColor, itemShape)
                                else -> Modifier.background(contentColor.copy(alpha = 0.12f))
                            }
                        )
                        .quickTapGestures(onTap = onSingleTap, onDoubleTap = onDoubleTap),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.dock_search_hint),
                        tint = when {
                            customSearchColors != null -> customSearchColors.contentColor
                            usesLightPillDetailing -> Color.Black.copy(alpha = 0.8f)
                            else -> contentColor
                        }
                    )
                }
            }

            if (handedness == Handedness.LEFT) {
                searchButton(); divider(); quickLaunchRow()
            } else {
                quickLaunchRow(); divider(); searchButton()
            }
        }
    }
}

private const val QUICK_DOUBLE_TAP_WINDOW_MS = 150L

/**
 * Same single-vs-double-tap distinction Compose's own `combinedClickable` does, but with a much
 * shorter window to decide "no second tap is coming" (150ms here vs. the system's ~300ms
 * doubleTapTimeoutMillis that combinedClickable uses internally). That system default is what made a
 * plain single tap feel noticeably delayed before doing anything, since it always had to wait it out
 * first even when no double tap was going to follow.
 */
@Composable
private fun Modifier.quickTapGestures(onTap: () -> Unit, onDoubleTap: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .indication(interactionSource, LocalIndication.current)
        .pointerInput(onTap, onDoubleTap) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val press = PressInteraction.Press(down.position)
                interactionSource.tryEmit(press)
                val up = waitForUpOrCancellation()
                interactionSource.tryEmit(if (up != null) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                if (up == null) return@awaitEachGesture

                val secondDown = withTimeoutOrNull(QUICK_DOUBLE_TAP_WINDOW_MS) { awaitFirstDown() }
                if (secondDown == null) {
                    onTap()
                    return@awaitEachGesture
                }
                val secondPress = PressInteraction.Press(secondDown.position)
                interactionSource.tryEmit(secondPress)
                val secondUp = waitForUpOrCancellation()
                interactionSource.tryEmit(if (secondUp != null) PressInteraction.Release(secondPress) else PressInteraction.Cancel(secondPress))
                if (secondUp != null) onDoubleTap()
            }
        }
}
