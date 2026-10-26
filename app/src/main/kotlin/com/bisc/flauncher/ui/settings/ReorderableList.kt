package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/** A vertical drag-to-reorder list. */
@Composable
fun <T> ReorderableList(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 64.dp,
    itemContent: @Composable (T) -> Unit
) {
    val density = LocalDensity.current
    val itemHeightPx = remember(itemHeight, density) { with(density) { itemHeight.toPx() } }
    val itemsState = rememberUpdatedState(items)
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(items) {
        if (draggingKey != null && items.none { key(it) == draggingKey }) {
            draggingKey = null
            dragOffsetPx = 0f
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SettingsCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        items.forEach { item ->
            val itemKey = key(item)
            androidx.compose.runtime.key(itemKey) {
            val isDragging = itemKey == draggingKey
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset { IntOffset(0, if (isDragging) dragOffsetPx.roundToInt() else 0) }
                    .background(
                        if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) { itemContent(item) }
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .pointerInput(itemKey) {
                            var workingOrder = itemsState.value
                            try {
                            detectDragGestures(
                                onDragStart = {
                                    workingOrder = itemsState.value
                                    draggingKey = itemKey
                                    dragOffsetPx = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetPx += dragAmount.y
                                    val currentIndex = workingOrder.indexOfFirst { key(it) == itemKey }
                                    if (currentIndex >= 0) {
                                        val targetIndex = (currentIndex + (dragOffsetPx / itemHeightPx).roundToInt())
                                            .coerceIn(0, workingOrder.lastIndex)
                                        if (targetIndex != currentIndex) {
                                            val reordered = workingOrder.toMutableList()
                                            val moved = reordered.removeAt(currentIndex)
                                            reordered.add(targetIndex, moved)
                                            dragOffsetPx -= (targetIndex - currentIndex) * itemHeightPx
                                            workingOrder = reordered
                                            onReorder(reordered)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggingKey = null
                                    dragOffsetPx = 0f
                                },
                                onDragCancel = {
                                    draggingKey = null
                                    dragOffsetPx = 0f
                                }
                            )
                            } finally {
                                if (draggingKey == itemKey) {
                                    draggingKey = null
                                    dragOffsetPx = 0f
                                }
                            }
                        }
                )
            }
            }
        }
    }
}
