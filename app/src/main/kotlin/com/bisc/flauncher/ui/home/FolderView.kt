package com.bisc.flauncher.ui.home

import android.content.ComponentName
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.model.rows
import com.bisc.flauncher.data.model.clockTimeAction
import com.bisc.flauncher.data.model.clockDateAction
import com.bisc.flauncher.data.model.asWidgetRow
import com.bisc.flauncher.ui.home.dock.DockDimens
import com.bisc.flauncher.ui.home.dock.DockPanelSurface
import com.bisc.flauncher.ui.home.widgets.ClockWidget
import com.bisc.flauncher.ui.home.widgets.MusicWidget
import com.bisc.flauncher.ui.home.widgets.NotesWidget
import com.bisc.flauncher.ui.home.widgets.SystemWidgetView
import com.bisc.flauncher.ui.home.widgets.WeatherWidget

private val FolderCornerRadius = 28.dp
private val FOLDER_TOP_GAP = 24.dp
private val FOLDER_CARD_PADDING = 16.dp
private val FOLDER_WIDGET_GAP = 8.dp
private val FOLDER_CELL_HEIGHT = 72.dp
private val FOLDER_CELL_LABEL_HEIGHT = 22.dp

@Composable
private fun folderDockFootprint(): androidx.compose.ui.unit.Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    return DockDimens.scaled((screenWidth - DockDimens.horizontalMargin * 2).coerceAtLeast(0.dp))
        .let { it.barHeight + DockDimens.topMargin + DockDimens.bottomMargin }
}

/** How many rows a widget in [folder] may take so the folder still fits on the screen, given its
 *  other widgets and app rows. [editing] is the widget being sized (left out of the others). */
@Composable
internal fun folderWidgetMaxSpan(folder: Folder, items: List<FolderItem>, editing: FolderItem?, showLabels: Boolean): Int {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenHeight = with(density) { androidx.compose.ui.platform.LocalView.current.rootView.height.toDp() }
    val topInset = maxOf(
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
        WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    )
    val available = screenHeight - topInset - FOLDER_TOP_GAP - folderDockFootprint() - FOLDER_CARD_PADDING * 2
    val (widgets, grid) = items.filter { it.id != editing?.id }
        .partition { it.type == GridItemType.WIDGET || it.type == GridItemType.SYSTEM_WIDGET }
    val othersHeight = widgets.fold(0.dp) { sum, w -> sum + PageRowDimens.spanHeight(PageRowDimens.rowUnitHeight, w.spanY) + FOLDER_WIDGET_GAP }
    val cell = FOLDER_CELL_HEIGHT + if (showLabels) FOLDER_CELL_LABEL_HEIGHT else 0.dp
    val gridHeight = cell * folderSlotRows(grid, folder.rows(), keepEmptyRows = false).size
    val left = available - othersHeight - gridHeight - FOLDER_WIDGET_GAP
    var span = 1
    while (PageRowDimens.spanHeight(PageRowDimens.rowUnitHeight, span + 1) <= left) span++
    return span
}

/**
 * The apps and elements of a folder as rows of cells. Each sits at its own place (its orderIndex
 * counts the cells across the rows), so cells can stay empty in between. Rows follow [rowShape];
 * further rows reuse its last width when there is more to place. [keepEmptyRows] also gives the
 * configured rows that are still empty (for the editor); otherwise empty rows at the end are left out.
 */
internal fun folderSlotRows(gridItems: List<FolderItem>, rowShape: List<Int>, keepEmptyRows: Boolean): List<List<FolderItem?>> {
    val bySlot = HashMap<Int, FolderItem>()
    gridItems.sortedWith(compareBy({ it.orderIndex }, { it.id })).forEach { item ->
        var slot = item.orderIndex.coerceAtLeast(0)
        while (slot in bySlot) slot++
        bySlot[slot] = item
    }
    val widths = rowShape.ifEmpty { listOf(4) }.map { it.coerceAtLeast(1) }
    val needed = maxOf((bySlot.keys.maxOrNull() ?: -1) + 1, if (keepEmptyRows) widths.sum() else 0)
    val rows = mutableListOf<List<FolderItem?>>()
    var start = 0
    while (start < needed) {
        val width = widths.getOrElse(rows.size) { widths.last() }
        val first = start
        rows += List(width) { bySlot[first + it] }
        start += width
    }
    return rows
}

/** Folder card with its items, shown above the home screen. */
@Composable
fun FolderView(
    folder: Folder,
    items: List<FolderItem>,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    savedMapTargets: List<SavedMapTarget> = emptyList(),
    pinnedWebsites: List<PinnedWebsite> = emptyList(),
    showLabels: Boolean = false,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    resolveLabel: (ComponentName) -> String = { "" },
    onAppClick: (FolderItem) -> Unit,
    onFolderClick: (FolderItem) -> Unit,
    onRemoveItem: (FolderItem) -> Unit,
    onMoveItemToSlot: (FolderItem, Int) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    cardAppearance: ElementAppearance,
    /** Runs a clock widget's tap action. */
    onExecuteAction: (com.bisc.flauncher.data.model.GestureAction) -> Unit = {},
    /** Opens a widget's own settings (long press on it, then Edit). */
    onEditWidget: (FolderItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // A pending removal asks first; picking "move" instead switches into reorder mode below.
    var pendingRemoval by remember(folder.id) { mutableStateOf<FolderItem?>(null) }
    var menuForItemId by remember(folder.id) { mutableStateOf<Long?>(null) }
    var movingItemId by remember(folder.id) { mutableStateOf<Long?>(null) }
    BackHandler(onBack = if (movingItemId != null) ({ movingItemId = null }) else onBack)

    // Status bar *or* camera cutout, whichever is taller.
    val topInset = maxOf(
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
        WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    )
    val (widgetItems, gridItems) = items.partition { it.type == GridItemType.WIDGET || it.type == GridItemType.SYSTEM_WIDGET }
    val gridRows = folderSlotRows(gridItems, folder.rows(), keepEmptyRows = false)
    val movingItem = gridItems.find { it.id == movingItemId }
    // Moves step by step through the cells, empty ones too.
    fun moveBy(delta: Int) {
        val item = movingItem ?: return
        val cells = folderSlotRows(gridItems, folder.rows(), keepEmptyRows = true).sumOf { it.size }
        val to = (item.orderIndex + delta).coerceIn(0, cells - 1)
        if (to != item.orderIndex) onMoveItemToSlot(item, to)
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val dockPillWidth = DockDimens.scaled((screenWidth - DockDimens.horizontalMargin * 2).coerceAtLeast(0.dp)).pillWidth

    // The card never scrolls: it grows upward up to just below the status bar. It normally keeps one
    // row unit of space above the Dock; a tall folder uses that space too before it is full.
    androidx.compose.ui.layout.Layout(
        modifier = modifier.fillMaxSize().padding(top = topInset + FOLDER_TOP_GAP, bottom = folderDockFootprint()),
        content = {
        DockPanelSurface(
            appearance = cardAppearance,
            shape = RoundedCornerShape(FolderCornerRadius),
            effectTarget = "folders",
            modifier = Modifier
                .width(dockPillWidth)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                for (widgetItem in widgetItems) {
                    key("widget_${widgetItem.id}") {
                        FolderWidgetRow(
                            item = widgetItem,
                            onExecuteAction = onExecuteAction,
                            menuExpanded = menuForItemId == widgetItem.id,
                            onLongPress = { if (movingItemId == null) menuForItemId = widgetItem.id },
                            onDismissMenu = { menuForItemId = null },
                            onEdit = { menuForItemId = null; onEditWidget(widgetItem) },
                            onRemove = { menuForItemId = null; pendingRemoval = widgetItem }
                        )
                    }
                }
                gridRows.forEachIndexed { rowIndex, row ->
                    key("grid_$rowIndex") {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            repeat(row.size) { slot ->
                                Box(modifier = Modifier.weight(1f)) {
                                    val entry = row[slot]
                                    if (entry != null) {
                                        key(entry.id) {
                                            FolderContentCell(
                                                item = entry,
                                                folders = folders,
                                                folderItemsByFolder = folderItemsByFolder,
                                                savedMapTargets = savedMapTargets,
                                                pinnedWebsites = pinnedWebsites,
                                                loadIcon = loadIcon,
                                                loadPackIcon = loadPackIcon,
                                                resolveLabel = resolveLabel,
                                                selected = entry.id == movingItemId,
                                                enabled = movingItemId == null,
                                                menuExpanded = menuForItemId == entry.id,
                                                onClick = { if (entry.type == GridItemType.FOLDER) onFolderClick(entry) else onAppClick(entry) },
                                                onLongClick = { menuForItemId = entry.id },
                                                onDismissMenu = { menuForItemId = null },
                                                onRequestMove = { menuForItemId = null; movingItemId = entry.id },
                                                onRequestRemove = { menuForItemId = null; pendingRemoval = entry },
                                                showLabel = showLabels
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (movingItem != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { moveBy(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.folder_move_left))
                        }
                        Text(
                            stringResource(R.string.folder_move_hint),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        TextButton(onClick = { movingItemId = null }) { Text(stringResource(R.string.folder_move_done)) }
                        IconButton(onClick = { moveBy(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.folder_move_right))
                        }
                    }
                }
            }
        }
        }
    ) { measurables, constraints ->
        val card = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val gap = minOf(PageRowDimens.rowUnitHeight.roundToPx(), (constraints.maxHeight - card.height).coerceAtLeast(0))
        layout(constraints.maxWidth, constraints.maxHeight) {
            card.place((constraints.maxWidth - card.width) / 2, constraints.maxHeight - gap - card.height)
        }
    }

    pendingRemoval?.let { entry ->
        ConfirmRemoveDialog(
            label = folderItemLabel(entry, folders, savedMapTargets, pinnedWebsites, resolveLabel)
                .ifBlank { stringResource(R.string.settings_layer_surface_widget) },
            onConfirm = { onRemoveItem(entry); pendingRemoval = null },
            onDismiss = { pendingRemoval = null }
        )
    }
}

/** Best-effort display name of a folder item, for the removal confirmation. */
private fun folderItemLabel(
    item: FolderItem,
    folders: List<Folder>,
    savedMapTargets: List<SavedMapTarget>,
    pinnedWebsites: List<PinnedWebsite>,
    resolveLabel: (ComponentName) -> String
): String = when (item.type) {
    GridItemType.APP -> ComponentName.unflattenFromString(item.payload)?.let(resolveLabel).orEmpty()
    GridItemType.FOLDER -> folders.find { it.id == item.payload.toLongOrNull() }?.name.orEmpty()
    GridItemType.SHORTCUT -> item.payload.split("|").getOrNull(2)?.takeIf { it.isNotBlank() } ?: item.payload.substringBefore('|')
    GridItemType.MAP_TARGET -> savedMapTargets.find { it.id == item.payload.toLongOrNull() }?.label.orEmpty()
    GridItemType.WEBSITE -> pinnedWebsites.find { it.id == item.payload.toLongOrNull() }?.label.orEmpty()
    GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET, GridItemType.PAGE, GridItemType.CONTACT, GridItemType.ACTION_BUTTON -> ""
}

/** Fires [onLongPress] when a finger rests on the widget, watching before the widget itself sees the
 *  touch (an Android widget would otherwise keep it), without taking taps or swipes away from it. */
private fun Modifier.watchLongPress(onLongPress: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
            }
        }
        if (released == null) onLongPress()
    }
}

/** A widget in a folder; a long press opens Edit and Remove. */
@Composable
private fun FolderWidgetRow(
    item: FolderItem,
    onExecuteAction: (com.bisc.flauncher.data.model.GestureAction) -> Unit,
    menuExpanded: Boolean,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PageRowDimens.spanHeight(PageRowDimens.rowUnitHeight, item.spanY))
            .padding(bottom = 8.dp)
            .watchLongPress(onLongPress)
    ) {
        androidx.compose.material3.DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.folder_widget_edit)) }, onClick = onEdit)
            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.folder_widget_remove)) }, onClick = onRemove)
        }
        when (item.type) {
            GridItemType.WIDGET -> when (item.payload) {
                WidgetKind.CLOCK -> {
                    val row = item.asWidgetRow()
                    ClockWidget(
                        style = row.clockStyle,
                        timeFormat = row.clockTimeFormat,
                        dateFormat = row.clockDateFormat,
                        showNextAlarm = row.clockShowNextAlarm,
                        alignment = row.clockAlignment,
                        customInk = row.appearance().contentColor?.let { androidx.compose.ui.graphics.Color(it) },
                        onDateClick = row.clockDateAction().takeIf { it.type != com.bisc.flauncher.data.model.GestureActionType.NONE }?.let { action -> { onExecuteAction(action) } },
                        onTimeClick = row.clockTimeAction().takeIf { it.type != com.bisc.flauncher.data.model.GestureActionType.NONE }?.let { action -> { onExecuteAction(action) } }
                    )
                }
                WidgetKind.MUSIC -> MusicWidget(appearance = item.appearance(), cornerRadiusPercent = item.cornerRadiusPercent)
                WidgetKind.WEATHER -> WeatherWidget(appearance = item.appearance(), cornerRadiusPercent = item.cornerRadiusPercent, pagingButtons = item.pagingButtons)
                WidgetKind.NOTES -> NotesWidget(appearance = item.appearance(), cornerRadiusPercent = item.cornerRadiusPercent, spanY = item.spanY, pagingButtons = item.pagingButtons)
            }
            GridItemType.SYSTEM_WIDGET -> item.payload.toIntOrNull()?.let { appWidgetId ->
                SystemWidgetView(
                    appWidgetId = appWidgetId,
                    modifier = Modifier.fillMaxSize(),
                    appearance = item.appearance(),
                    cornerRadiusPercent = item.cornerRadiusPercent
                )
            }
            GridItemType.APP, GridItemType.SHORTCUT, GridItemType.MAP_TARGET, GridItemType.WEBSITE, GridItemType.FOLDER, GridItemType.PAGE, GridItemType.CONTACT, GridItemType.ACTION_BUTTON -> Unit
        }
    }
}

@Composable
private fun FolderContentCell(
    item: FolderItem,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    savedMapTargets: List<SavedMapTarget>,
    pinnedWebsites: List<PinnedWebsite>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap?,
    resolveLabel: (ComponentName) -> String,
    selected: Boolean,
    enabled: Boolean,
    menuExpanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onRequestMove: () -> Unit,
    onRequestRemove: () -> Unit,
    showLabel: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else Modifier
            )
            .combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(stringResource(R.string.folder_item_move)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null) },
                onClick = onRequestMove
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(stringResource(R.string.folder_item_remove)) },
                leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                onClick = onRequestRemove
            )
        }
        when (item.type) {
            GridItemType.APP -> {
                val componentName = remember(item.payload) { ComponentName.unflattenFromString(item.payload) }
                val bitmap = rememberAppIcon(componentName, loadIcon)
                val label = componentName?.let(resolveLabel).orEmpty()
                if (bitmap != null) Image(bitmap = bitmap, contentDescription = label, modifier = Modifier.size(56.dp))
                if (showLabel) {
                    Spacer(Modifier.height(6.dp))
                    GridLabel(label)
                }
            }
            GridItemType.SHORTCUT -> {
                Icon(imageVector = Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, modifier = Modifier.size(56.dp))
                if (showLabel) {
                    Spacer(Modifier.height(6.dp))
                    GridLabel(item.payload.split("|").getOrNull(2)?.takeIf { it.isNotBlank() } ?: item.payload.substringBefore('|'))
                }
            }
            GridItemType.MAP_TARGET -> {
                val targetId = remember(item.payload) { item.payload.toLongOrNull() }
                val target = savedMapTargets.find { it.id == targetId }
                Icon(imageVector = Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(56.dp))
                if (showLabel) {
                    Spacer(Modifier.height(6.dp))
                    GridLabel(target?.label.orEmpty())
                }
            }
            GridItemType.WEBSITE -> {
                val websiteId = remember(item.payload) { item.payload.toLongOrNull() }
                val website = pinnedWebsites.find { it.id == websiteId }
                Icon(imageVector = Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(56.dp))
                if (showLabel) {
                    Spacer(Modifier.height(6.dp))
                    GridLabel(website?.label.orEmpty())
                }
            }
            GridItemType.FOLDER -> {
                val folderId = remember(item.payload) { item.payload.toLongOrNull() }
                val childFolder = folders.find { it.id == folderId }
                FolderIcon(
                    folder = childFolder,
                    previewItems = folderId?.let { folderItemsByFolder[it] }.orEmpty(),
                    loadIcon = loadIcon,
                    loadPackIcon = loadPackIcon,
                    modifier = Modifier.size(56.dp)
                )
                if (showLabel) {
                    Spacer(Modifier.height(6.dp))
                    GridLabel(childFolder?.name.orEmpty())
                }
            }
            GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET, GridItemType.PAGE, GridItemType.CONTACT, GridItemType.ACTION_BUTTON -> Unit
        }
    }
}

@Composable
private fun GridLabel(text: String) {
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
}
