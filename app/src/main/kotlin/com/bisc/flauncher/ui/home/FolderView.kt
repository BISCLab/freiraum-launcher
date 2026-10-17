package com.bisc.flauncher.ui.home

import android.content.ComponentName
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.bisc.flauncher.ui.home.dock.DockDimens
import com.bisc.flauncher.ui.home.dock.DockPanelSurface
import com.bisc.flauncher.ui.home.widgets.ClockWidget
import com.bisc.flauncher.ui.home.widgets.MusicWidget
import com.bisc.flauncher.ui.home.widgets.NotesWidget
import com.bisc.flauncher.ui.home.widgets.SystemWidgetView
import com.bisc.flauncher.ui.home.widgets.WeatherWidget

private const val GRID_COLUMNS = 4

private val FolderCornerRadius = 28.dp

/** Folder card with its items, shown above the home screen. */
@Composable
fun FolderView(
    folder: Folder,
    items: List<FolderItem>,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    savedMapTargets: List<SavedMapTarget> = emptyList(),
    pinnedWebsites: List<PinnedWebsite> = emptyList(),
    showAddRow: Boolean = true,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    resolveLabel: (ComponentName) -> String = { "" },
    onAppClick: (FolderItem) -> Unit,
    onFolderClick: (FolderItem) -> Unit,
    onRemoveItem: (FolderItem) -> Unit,
    onReorderItems: (List<FolderItem>) -> Unit = {},
    onAddAppClick: () -> Unit,
    onAddShortcutClick: () -> Unit,
    onAddWidgetClick: () -> Unit,
    onAddSubfolderClick: () -> Unit,
    onAddMapTargetClick: () -> Unit = {},
    onAddWebsiteClick: () -> Unit = {},
    onBack: () -> Unit,
    cardAppearance: ElementAppearance,
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
    val gridRows = gridItems.chunked(GRID_COLUMNS)
    val movingItem = gridItems.find { it.id == movingItemId }
    fun moveBy(delta: Int) {
        val from = gridItems.indexOfFirst { it.id == movingItemId }
        val to = (from + delta).coerceIn(0, gridItems.lastIndex)
        if (from < 0 || from == to) return
        val reordered = gridItems.toMutableList().apply { add(to, removeAt(from)) }
        onReorderItems(reordered)
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val dockFootprint = DockDimens.scaled((screenWidth - DockDimens.horizontalMargin * 2).coerceAtLeast(0.dp))
        .let { it.barHeight + DockDimens.topMargin + DockDimens.bottomMargin }
    val bottomAnchorGap = dockFootprint + PageRowDimens.rowUnitHeight
    val dockPillWidth = DockDimens.scaled((screenWidth - DockDimens.horizontalMargin * 2).coerceAtLeast(0.dp)).pillWidth

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().padding(top = topInset + 24.dp, bottom = bottomAnchorGap),
        contentAlignment = Alignment.BottomCenter
    ) {
        DockPanelSurface(
            appearance = cardAppearance,
            shape = RoundedCornerShape(FolderCornerRadius),
            modifier = Modifier
                .width(dockPillWidth)
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                for (widgetItem in widgetItems) {
                    key("widget_${widgetItem.id}") { FolderWidgetRow(item = widgetItem) }
                }
                for (row in gridRows) {
                    key("grid_${row.first().id}") {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            repeat(GRID_COLUMNS) { slot ->
                                Box(modifier = Modifier.weight(1f)) {
                                    val entry = row.getOrNull(slot)
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
                                                onRequestRemove = { menuForItemId = null; pendingRemoval = entry }
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

                if (showAddRow && movingItemId == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AddButton(Icons.Filled.Add, stringResource(R.string.folder_add_app), onAddAppClick, Modifier.weight(1f))
                        AddButton(Icons.AutoMirrored.Filled.Shortcut, stringResource(R.string.folder_add_shortcut), onAddShortcutClick, Modifier.weight(1f))
                        AddButton(Icons.Filled.Place, stringResource(R.string.settings_row_entry_add_map_target), onAddMapTargetClick, Modifier.weight(1f))
                        AddButton(Icons.Filled.Language, stringResource(R.string.settings_row_entry_add_website), onAddWebsiteClick, Modifier.weight(1f))
                        AddButton(Icons.Filled.Widgets, stringResource(R.string.folder_add_widget), onAddWidgetClick, Modifier.weight(1f))
                        AddButton(Icons.Filled.CreateNewFolder, stringResource(R.string.folder_add_subfolder), onAddSubfolderClick, Modifier.weight(1f))
                    }
                }
            }
        }
    }

    pendingRemoval?.let { entry ->
        ConfirmRemoveDialog(
            label = folderItemLabel(entry, folders, savedMapTargets, pinnedWebsites, resolveLabel),
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

@Composable
private fun AddButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.combinedClickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FolderWidgetRow(item: FolderItem) {
    Box(modifier = Modifier.fillMaxWidth().height(PageRowDimens.rowUnitHeight * item.spanY).padding(bottom = 8.dp)) {
        when (item.type) {
            GridItemType.WIDGET -> when (item.payload) {
                WidgetKind.CLOCK -> ClockWidget()
                WidgetKind.MUSIC -> MusicWidget(appearance = item.appearance(), cornerStyle = item.cornerStyle)
                WidgetKind.WEATHER -> WeatherWidget(appearance = item.appearance(), cornerStyle = item.cornerStyle)
                WidgetKind.NOTES -> NotesWidget(appearance = item.appearance(), cornerStyle = item.cornerStyle, spanY = item.spanY)
            }
            GridItemType.SYSTEM_WIDGET -> item.payload.toIntOrNull()?.let { appWidgetId ->
                SystemWidgetView(
                    appWidgetId = appWidgetId,
                    modifier = Modifier.fillMaxSize(),
                    appearance = item.appearance(),
                    cornerStyle = item.cornerStyle
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
    onRequestRemove: () -> Unit
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
                Spacer(Modifier.height(6.dp))
                GridLabel(label)
            }
            GridItemType.SHORTCUT -> {
                Icon(imageVector = Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(6.dp))
                GridLabel(item.payload.split("|").getOrNull(2)?.takeIf { it.isNotBlank() } ?: item.payload.substringBefore('|'))
            }
            GridItemType.MAP_TARGET -> {
                val targetId = remember(item.payload) { item.payload.toLongOrNull() }
                val target = savedMapTargets.find { it.id == targetId }
                Icon(imageVector = Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(6.dp))
                GridLabel(target?.label.orEmpty())
            }
            GridItemType.WEBSITE -> {
                val websiteId = remember(item.payload) { item.payload.toLongOrNull() }
                val website = pinnedWebsites.find { it.id == websiteId }
                Icon(imageVector = Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(6.dp))
                GridLabel(website?.label.orEmpty())
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
                Spacer(Modifier.height(6.dp))
                GridLabel(childFolder?.name.orEmpty())
            }
            GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET, GridItemType.PAGE, GridItemType.CONTACT, GridItemType.ACTION_BUTTON -> Unit
        }
    }
}

@Composable
private fun GridLabel(text: String) {
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
}
