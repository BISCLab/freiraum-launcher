package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.luminance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.home.dock.DockDimens
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.noRippleClickable

/** One entry in the [AlphabetIndex] rail. */
sealed class IndexEntry {
    data class Letter(val char: Char) : IndexEntry()
    data object Other : IndexEntry()
}

/** Short label shown both on the rail itself and (enlarged) in the scrub magnifier bubble. */
fun IndexEntry.label(): String = when (this) {
    is IndexEntry.Letter -> char.toString()
    IndexEntry.Other -> "#"
}

internal val FLUSH_INSET = DockDimens.horizontalMargin

internal val RAIL_BAR_WIDTH = 28.dp
private val RAIL_BAR_CORNER = RAIL_BAR_WIDTH / 2

/** Right-edge fast-scroll rail for the browse app list. */
@Composable
fun AlphabetIndex(
    letters: List<Char>,
    hasOtherGroup: Boolean,
    showRailBar: Boolean,
    letterAppearance: ElementAppearance,
    handedness: Handedness,
    onSelect: (IndexEntry) -> Unit,
    onScrub: (active: Boolean, label: String, verticalFraction: Float) -> Unit,
    modifier: Modifier = Modifier,
    edgeInset: Dp = FLUSH_INSET
) {
    val entries = remember(letters, hasOtherGroup) {
        buildList {
            letters.forEach { add(IndexEntry.Letter(it)) }
            if (hasOtherGroup) add(IndexEntry.Other)
        }
    }
    val letterColors = resolveElementColors(letterAppearance)
    val railColors = LocalRailColors.current
    val railTextColor = railColors.text
    val ownLetterFill = letterAppearance.fillMode == ElementFillMode.COLOR
    val letterCircleSize = 20.dp

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val heightPx = with(density) { maxHeight.toPx() }

        fun selectFromY(y: Float) {
            if (entries.isEmpty()) return
            val fraction = (y / heightPx).coerceIn(0f, 1f)
            val entry = entries[(fraction * entries.size).toInt().coerceIn(0, entries.size - 1)]
            onSelect(entry)
            onScrub(true, entry.label(), fraction)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(entries) {
                    detectDragGestures(
                        onDragStart = { offset -> selectFromY(offset.y) },
                        onDragEnd = { onScrub(false, "", 0f) },
                        onDragCancel = { onScrub(false, "", 0f) },
                        onDrag = { change, _ -> selectFromY(change.position.y) }
                    )
                },
            contentAlignment = if (handedness == Handedness.LEFT) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            Column(
                modifier = Modifier
                    .then(
                        if (handedness == Handedness.LEFT) Modifier.padding(start = edgeInset) else Modifier.padding(end = edgeInset)
                    )
                    .width(RAIL_BAR_WIDTH)
                    .fillMaxHeight()
                    .then(
                        if (showRailBar) {
                            Modifier
                                .clip(RoundedCornerShape(RAIL_BAR_CORNER))
                                .background(railColors.box, alpha = railColors.boxAlpha)
                                .then(
                                    if (railColors.showBorder) Modifier.border(railColors.borderWidth, railColors.border, RoundedCornerShape(RAIL_BAR_CORNER))
                                    else Modifier
                                )
                        } else {
                            Modifier
                        }
                    ),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                entries.forEachIndexed { _, entry ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .noRippleClickable { onSelect(entry) }
                            .padding(vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(letterCircleSize)
                                .clip(CircleShape)
                                .background(letterColors.background, alpha = letterColors.backgroundAlpha)
                                .then(
                                    if (letterAppearance.borderEnabled) Modifier.border(letterColors.borderWidth, if (ownLetterFill || letterAppearance.borderColor != null) letterColors.borderColor else railTextColor.copy(alpha = 0.3f), CircleShape) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                entry.label(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ownLetterFill) letterColors.contentColor else if (showRailBar) railTextColor else LocalContentColor.current
                            )
                        }
                    }
                }
            }
        }
    }
}
