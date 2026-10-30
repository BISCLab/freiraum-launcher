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
import androidx.compose.foundation.layout.width
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
import com.bisc.flauncher.data.settings.Handedness
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

internal val FLUSH_INSET = AppOverviewDimens.horizontalMargin

internal val RAIL_BAR_WIDTH = 28.dp

/** Right-edge fast-scroll rail for the browse app list. Box fill/border/letter-text color all come
 *  from [LocalRailColors] (resolved from the single A–Z index appearance setting) - a fill mode of
 *  None already makes the box itself invisible, so there's no separate "show the box" switch to
 *  keep in sync with it. */
@Composable
fun AlphabetIndex(
    letters: List<Char>,
    hasOtherGroup: Boolean,
    handedness: Handedness,
    cornerRadiusPercent: Int,
    onSelect: (IndexEntry) -> Unit,
    onScrub: (active: Boolean, label: String, verticalFraction: Float) -> Unit,
    modifier: Modifier = Modifier,
    edgeInset: Dp = FLUSH_INSET
) {
    val railBarCorner = (RAIL_BAR_WIDTH / 2) * (cornerRadiusPercent.coerceIn(0, 100) / 100f)
    val entries = remember(letters, hasOtherGroup) {
        buildList {
            letters.forEach { add(IndexEntry.Letter(it)) }
            if (hasOtherGroup) add(IndexEntry.Other)
        }
    }
    val railColors = LocalRailColors.current

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
                    .clip(RoundedCornerShape(railBarCorner))
                    .background(railColors.box, alpha = railColors.boxAlpha)
                    .then(
                        if (railColors.showBorder) Modifier.border(railColors.borderWidth, railColors.border, RoundedCornerShape(railBarCorner))
                        else Modifier
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
                        Text(entry.label(), style = MaterialTheme.typography.labelSmall, color = railColors.text)
                    }
                }
            }
        }
    }
}
