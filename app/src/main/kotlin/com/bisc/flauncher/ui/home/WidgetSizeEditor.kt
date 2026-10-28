package com.bisc.flauncher.ui.home

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import kotlin.math.roundToInt

/**
 * Widget preview with size controls below it: a coarse row-count stepper for our own widgets, or —
 * for a hosted external widget — a fine, per-digit pixel-height stepper (see [PixelHeightStepper]).
 */
@Composable
fun WidgetSizeEditor(
    row: PageRow,
    minSpan: Int,
    maxSpan: Int,
    onSpanChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSystemWidgetViewReady: ((android.view.View) -> Unit)? = null,
    heightPxRange: IntRange? = null,
    onHeightPxChange: ((Int) -> Unit)? = null,
    /** The page's row unit as it's drawn there (stretched), and how many fit — the grid a hosted
     *  widget's free pixel height is matched against. */
    gridUnit: androidx.compose.ui.unit.Dp = PageRowDimens.rowUnitHeight,
    gridUnitCount: Int = 0
) {
    val density = LocalDensity.current
    val unitPx = remember(density) { with(density) { PageRowDimens.rowUnitHeight.toPx() } }
    var dragOffsetPx by remember { mutableStateOf(0f) }

    Column(modifier = modifier.fillMaxWidth()) {
        PageRowContent(
            row = row,
            entries = emptyList(),
            folders = emptyList(),
            folderItemsByFolder = emptyMap(),
            loadIcon = { null },
            onFolderClick = {},
            onSystemWidgetViewReady = onSystemWidgetViewReady
        )
        if (row.type == PageRowType.SYSTEM_WIDGET && heightPxRange != null && onHeightPxChange != null) {
            val gridHeightsPx = with(density) {
                (1..gridUnitCount).map { PageRowDimens.spanHeight(gridUnit, it).roundToPx() }
            }
            val currentHeightPx = row.heightPx ?: with(density) { PageRowDimens.spanHeight(gridUnit, row.spanY).roundToPx() }
            PixelHeightStepper(
                valuePx = currentHeightPx.coerceIn(heightPxRange),
                range = heightPxRange,
                onValueChange = onHeightPxChange,
                gridHeightsPx = gridHeightsPx,
                modifier = Modifier.padding(top = 8.dp)
            )
            return@Column
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The +/- buttons are the reliable way to resize; the handle is for quick, coarse dragging.
            IconButton(onClick = { onSpanChange((row.spanY - 1).coerceIn(minSpan, maxSpan)) }, enabled = row.spanY > minSpan) {
                Icon(Icons.Filled.Remove, contentDescription = null)
            }
            Text(
                pluralStringResource(R.plurals.settings_widget_size_rows, row.spanY, row.spanY),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(onClick = { onSpanChange((row.spanY + 1).coerceIn(minSpan, maxSpan)) }, enabled = row.spanY < maxSpan) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
            Box(
                modifier = Modifier.padding(start = 12.dp).size(72.dp, 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp, 28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .pointerInput(row.id, row.type, minSpan, maxSpan) {
                            var startSpan = row.spanY
                            detectDragGestures(
                                onDragStart = {
                                    startSpan = row.spanY
                                    dragOffsetPx = 0f
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    dragOffsetPx += drag.y
                                    val candidate = (startSpan + (dragOffsetPx / unitPx).roundToInt())
                                        .coerceIn(minSpan, maxSpan)
                                    if (candidate != row.spanY) onSpanChange(candidate)
                                },
                                onDragEnd = { dragOffsetPx = 0f },
                                onDragCancel = { dragOffsetPx = 0f }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private val PLACE_VALUES = listOf(1000, 100, 10, 1)

/**
 * Four small up/down steppers, one per decimal place (thousands/hundreds/tens/ones), for dialling in
 * an exact pixel height. Each nudges [valuePx] by its own place value and clamps to [range].
 */
@Composable
fun PixelHeightStepper(
    valuePx: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** Exact heights of 1, 2, 3, … page rows: hitting one is marked (and felt), and each can be
     *  jumped to directly, so a free-height widget can sit on the page's row grid. */
    gridHeightsPx: List<Int> = emptyList()
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val setValue: (Int) -> Unit = { px ->
        if (px in gridHeightsPx && px != valuePx) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        onValueChange(px)
    }
    val matchedRows = gridHeightsPx.indexOf(valuePx) + 1
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_widget_size_px, valuePx), style = MaterialTheme.typography.bodyMedium)
            if (matchedRows > 0) {
                Text(
                    " = " + pluralStringResource(R.plurals.settings_widget_size_rows, matchedRows, matchedRows),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PLACE_VALUES.forEach { place ->
                val digit = (valuePx / place) % 10
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { setValue((valuePx + place).coerceIn(range)) },
                        enabled = valuePx + place <= range.last
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.settings_widget_size_increase, place))
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(digit.toString(), style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(
                        onClick = { setValue((valuePx - place).coerceIn(range)) },
                        enabled = valuePx - place >= range.first
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.settings_widget_size_decrease, place))
                    }
                }
            }
        }
        val reachable = gridHeightsPx.withIndex().filter { it.value in range }
        if (reachable.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reachable.forEach { (index, px) ->
                    FilterChip(
                        selected = px == valuePx,
                        onClick = { setValue(px) },
                        label = { Text(pluralStringResource(R.plurals.settings_widget_size_rows, index + 1, index + 1)) }
                    )
                }
            }
        }
    }
}
