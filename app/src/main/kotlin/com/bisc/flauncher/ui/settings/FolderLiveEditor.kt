package com.bisc.flauncher.ui.settings

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.ui.home.FolderIcon
import com.bisc.flauncher.ui.home.PageRowDimens
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.rememberAppIcon

/** How much smaller than on the home screen the live folder is drawn. */
private const val LIVE_SCALE = 0.55f
private val LIVE_ICON = 34.dp

internal fun FolderItem.isWidget() = type == GridItemType.WIDGET || type == GridItemType.SYSTEM_WIDGET

/**
 * The folder as it looks when opened, live: its card in its own look, the widgets on top at their
 * real height, then its apps and elements in their rows. Every part can be tapped to pick it; the
 * picked one is outlined in the accent. Every empty cell adds something right there.
 */
@Composable
internal fun FolderLivePreview(
    look: ElementAppearance,
    widgets: List<FolderItem>,
    gridItems: List<FolderItem>,
    rowShape: List<Int>,
    showLabels: Boolean,
    selectedId: Long?,
    onSelect: (FolderItem) -> Unit,
    /** An empty cell was tapped: add something exactly there. */
    onAdd: (slot: Int) -> Unit,
    widgetIcon: (FolderItem) -> ImageVector,
    widgetLabel: @Composable (FolderItem) -> String,
    itemLabel: @Composable (FolderItem) -> String,
    itemIcon: @Composable (FolderItem, Dp) -> Unit
) {
    val colors = resolveElementColors(look)
    val shape = RoundedCornerShape(22.dp)
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHighest)))
            .padding(horizontal = 18.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.background, alpha = colors.backgroundAlpha.coerceAtLeast(0.25f))
                .then(if (look.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            @Composable
            fun WidgetTile(widget: FolderItem) {
                val picked = widget.id == selectedId
                val height = PageRowDimens.spanHeight(PageRowDimens.rowUnitHeight, widget.spanY) * LIVE_SCALE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.contentColor.copy(alpha = 0.10f))
                        .border(if (picked) 2.dp else 1.dp, if (picked) accent else colors.contentColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .clickable { onSelect(widget) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(widgetIcon(widget), contentDescription = null, tint = colors.contentColor.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    Text(widgetLabel(widget), color = colors.contentColor, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(
                        pluralStringResource(R.plurals.settings_widget_size_rows, widget.spanY, widget.spanY),
                        color = colors.contentColor.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            // The apps and elements at their places; every empty cell adds something there, and
            // when all are taken a further row offers more.
            val rows = com.bisc.flauncher.ui.home.folderSlotRows(gridItems, rowShape, keepEmptyRows = true).toMutableList()
            if (rows.none { line -> line.any { it == null } }) {
                val widths = rowShape.ifEmpty { listOf(4) }
                rows += List(widths.getOrElse(rows.size) { widths.last() }.coerceAtLeast(1)) { null }
            }
            var cellIndex = 0
            val realRows = com.bisc.flauncher.ui.home.folderSlotRows(gridItems, rowShape, keepEmptyRows = true).size
            val widgetsByRow = com.bisc.flauncher.ui.home.widgetsAbove(widgets, realRows)
            rows.forEachIndexed { rowIndex, line ->
                if (rowIndex < realRows) widgetsByRow[rowIndex].forEach { WidgetTile(it) }
                Row(modifier = Modifier.fillMaxWidth()) {
                    line.forEach { item ->
                        val slot = cellIndex++
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                            when {
                                item != null -> {
                                    val picked = item.id == selectedId
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(if (picked) 2.dp else 0.dp, if (picked) accent else Color.Transparent, RoundedCornerShape(12.dp))
                                            .clickable { onSelect(item) }
                                            .padding(4.dp)
                                    ) {
                                        itemIcon(item, LIVE_ICON)
                                        if (showLabels) {
                                            Text(
                                                itemLabel(item),
                                                color = colors.contentColor,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.width(LIVE_ICON + 16.dp).padding(top = 3.dp)
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(LIVE_ICON)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.5.dp, colors.contentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .clickable { onAdd(slot) },
                                        contentAlignment = Alignment.Center
                                    ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.settings_folder_add_content), tint = colors.contentColor.copy(alpha = 0.7f)) }
                                }
                            }
                        }
                    }
                }
            }
            widgetsByRow[realRows].forEach { WidgetTile(it) }
        }
    }
}

/** The icon of one app or element of a folder, at [size]. */
@Composable
internal fun FolderItemIcon(
    item: FolderItem,
    size: Dp,
    folders: List<Folder>,
    itemsByFolder: Map<Long, List<FolderItem>>,
    loadIcon: (ComponentName) -> androidx.compose.ui.graphics.ImageBitmap?,
    loadPackIcon: (String, String) -> androidx.compose.ui.graphics.ImageBitmap?
) {
    when (item.type) {
        GridItemType.APP -> {
            val icon = rememberAppIcon(ComponentName.unflattenFromString(item.payload), loadIcon)
            if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(size))
            else Box(Modifier.size(size).clip(RoundedCornerShape(size / 3)).background(MaterialTheme.colorScheme.surfaceVariant))
        }
        GridItemType.FOLDER -> {
            val sub = folders.find { it.id == item.payload.toLongOrNull() }
            FolderIcon(folder = sub, previewItems = sub?.let { itemsByFolder[it.id] }.orEmpty(), loadIcon = loadIcon, loadPackIcon = loadPackIcon, modifier = Modifier.size(size))
        }
        else -> Box(
            modifier = Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when (item.type) {
                    GridItemType.SHORTCUT -> Icons.AutoMirrored.Filled.Shortcut
                    GridItemType.MAP_TARGET -> Icons.Filled.Place
                    GridItemType.WEBSITE -> Icons.Filled.Language
                    else -> Icons.Filled.Apps
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

/** What can be done with the picked part, right under the live folder. */
@Composable
internal fun FolderSelectionBar(
    title: String,
    isWidget: Boolean,
    span: Int,
    canGrow: Boolean,
    canShrink: Boolean,
    onSpan: (Int) -> Unit,
    canMoveBack: Boolean,
    canMoveOn: Boolean,
    onMove: (Int) -> Unit,
    onOpenSettings: (() -> Unit)?,
    onRemove: () -> Unit,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.primary.copy(alpha = 0.08f))
            .border(1.dp, scheme.primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text(stringResource(R.string.folder_move_done)) }
        }
        if (isWidget) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_folder_widget_height), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                StepButton(Icons.Filled.Remove, canShrink) { onSpan(span - 1) }
                Text(
                    pluralStringResource(R.plurals.settings_widget_size_rows, span, span),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(76.dp)
                )
                StepButton(Icons.Filled.Add, canGrow) { onSpan(span + 1) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_folder_position), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            StepButton(if (isWidget) Icons.Filled.KeyboardArrowUp else Icons.AutoMirrored.Filled.ArrowBack, canMoveBack) { onMove(-1) }
            StepButton(if (isWidget) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.ArrowForward, canMoveOn) { onMove(1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (onOpenSettings != null) {
                androidx.compose.material3.Button(
                    onClick = onOpenSettings,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = scheme.surfaceContainerHighest, contentColor = scheme.onSurface),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_entry_settings), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            androidx.compose.material3.Button(
                onClick = onRemove,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = scheme.errorContainer, contentColor = scheme.onErrorContainer),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.folder_widget_remove), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (enabled) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.3f), modifier = Modifier.size(20.dp))
    }
}

/** The two ways to add something, as two large buttons. */
@Composable
internal fun FolderAddButtons(onAddContent: () -> Unit, onAddWidget: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(onClick = onAddContent, modifier = Modifier.weight(1f).height(52.dp)) {
            Icon(Icons.Filled.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_folder_add_content), maxLines = 1)
        }
        FilledTonalButton(onClick = onAddWidget, modifier = Modifier.weight(1f).height(52.dp)) {
            Icon(Icons.Filled.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_folder_add_widget), maxLines = 1)
        }
    }
}

/** The rows of a folder: each as a line of small boxes, as many as fit side by side, with - and +. */
@Composable
internal fun FolderRowsEditor(rowShape: List<Int>, maxColumns: Int, maxRows: Int = Int.MAX_VALUE, newRowColumns: Int = 4, onChange: (List<Int>) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rowShape.forEachIndexed { index, cols ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_panel_row_number, index + 1), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(64.dp))
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(cols) { Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(scheme.primary.copy(alpha = 0.35f))) }
                    repeat(maxColumns - cols) { Spacer(Modifier.weight(1f)) }
                }
                StepButton(Icons.Filled.Remove, cols > 1) { onChange(rowShape.toMutableList().apply { this[index] = cols - 1 }) }
                StepButton(Icons.Filled.Add, cols < maxColumns) { onChange(rowShape.toMutableList().apply { this[index] = cols + 1 }) }
                IconButton(onClick = { onChange(rowShape.filterIndexed { i, _ -> i != index }) }, enabled = rowShape.size > 1, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = scheme.onSurfaceVariant.copy(alpha = if (rowShape.size > 1) 1f else 0.3f), modifier = Modifier.size(18.dp))
                }
            }
        }
        if (rowShape.size < maxRows) TextButton(onClick = { onChange(rowShape + newRowColumns.coerceAtMost(maxColumns)) }) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.settings_panel_add_row))
        }
    }
}
