package com.bisc.flauncher.ui.home

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
import kotlin.math.roundToInt

/** Widget preview with +/- buttons and a drag handle, either of which sets the row height. */
@Composable
fun WidgetSizeEditor(
    row: PageRow,
    minSpan: Int,
    maxSpan: Int,
    onSpanChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSystemWidgetViewReady: ((android.view.View) -> Unit)? = null
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
