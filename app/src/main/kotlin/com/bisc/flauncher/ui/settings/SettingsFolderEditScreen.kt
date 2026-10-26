package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.FolderIconMode
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.IconPackIcon
import com.bisc.flauncher.data.model.RowEntry
import com.bisc.flauncher.data.model.borderAppearance
import com.bisc.flauncher.data.model.contentAppearance
import com.bisc.flauncher.data.model.rows
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import com.bisc.flauncher.ui.home.FolderIcon
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PRESET_COLORS = listOf(
    "#FF4F378B", "#FFB3261E", "#FF386A20", "#FF9A5D00", "#FF00658F", "#FF6B5C00"
)

/** Edit page for a single folder. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFolderEditScreen(
    folderId: Long,
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val folders by viewModel.folders.collectAsState()
    val folderItems by viewModel.folderItems.collectAsState()
    val iconPacks by viewModel.iconPacks.collectAsState()
    val globalCardAppearance by viewModel.folderCardAppearance.collectAsState()
    val folder = folders.find { it.id == folderId }
    var edited by rememberSaveable(folderId) { mutableStateOf<String?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val itemsByFolder = remember(folderItems) { folderItems.groupBy { it.folderId } }
    val items = itemsByFolder[folderId].orEmpty()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val savedMapTargets by viewModel.savedMapTargets.collectAsState()
    val pinnedWebsites by viewModel.pinnedWebsites.collectAsState()
    val shortcutAppPickerOpen by viewModel.shortcutAppPickerOpen.collectAsState()
    val shortcutPickerForApp by viewModel.shortcutPickerForApp.collectAsState()
    val availableShortcuts by viewModel.availableShortcuts.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showMapTargetPicker by remember { mutableStateOf(false) }
    var showAddMapTargetDialog by remember { mutableStateOf(false) }
    var showWebsitePicker by remember { mutableStateOf(false) }
    var showAddWebsiteDialog by remember { mutableStateOf(false) }
    var showAddChooser by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<com.bisc.flauncher.data.model.FolderItem?>(null) }
    var pickingFolderIcon by remember { mutableStateOf(false) }
    var appearanceOpen by remember { mutableStateOf(false) }
    var borderOpen by remember { mutableStateOf(false) }
    var showWidgetKindPicker by remember { mutableStateOf(false) }
    var sizingWidgetKind by remember { mutableStateOf<String?>(null) }
    var sizingWidgetSpanY by remember { mutableStateOf(1) }
    val globalShowLabels by viewModel.folderShowLabels.collectAsState()

    val latestEdited by rememberUpdatedState(edited)
    val latestName by rememberUpdatedState(folder?.name)
    DisposableEffect(folderId) {
        onDispose {
            val name = latestEdited?.trim()
            if (!name.isNullOrBlank() && name != latestName) viewModel.renameFolder(folderId, name)
        }
    }

    SettingsSubScreen(title = stringResource(R.string.settings_folder_edit_title), navController = navController, onBack = onBack) {
        if (folder == null) return@SettingsSubScreen

        OutlinedTextField(
            value = edited ?: folder.name,
            onValueChange = { edited = it },
            label = { Text(stringResource(R.string.settings_folder_name_label)) },
            singleLine = true,
            shape = SettingsTextFieldShape,
            modifier = Modifier.fillMaxWidth()
        )

        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_icon_label),
                summary = if (folder.iconMode == FolderIconMode.ICON_PACK) {
                    iconPacks.find { it.packageName == folder.iconPackPackage }?.label ?: folder.iconPackPackage
                } else {
                    stringResource(R.string.settings_app_icon_default)
                },
                onClick = { pickingFolderIcon = true },
                trailing = {
                    FolderIcon(
                        folder = folder,
                        previewItems = folderItems.filter { it.folderId == folderId },
                        loadIcon = viewModel::loadIcon,
                        loadPackIcon = viewModel::loadPackIcon,
                        modifier = Modifier.size(36.dp)
                    )
                }
            )
        }

        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_folder_appearance_label),
                onClick = { appearanceOpen = true },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_element_border_label),
                onClick = { borderOpen = true },
                trailing = null
            )
        }

        val rowShape = folder.rows()
        val gridEntries = remember(items) {
            items.map { item -> RowEntry(id = item.id, pageRowId = 0L, orderIndex = item.orderIndex, type = item.type, payload = item.payload) }
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_folder_items_label)) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.bisc.flauncher.ui.home.GridEntries(
                    entries = gridEntries,
                    columns = 1,
                    rowColumns = rowShape,
                    folders = folders,
                    folderItemsByFolder = itemsByFolder,
                    savedMapTargets = savedMapTargets,
                    pinnedWebsites = pinnedWebsites,
                    pages = emptyList(),
                    loadIcon = viewModel::loadIcon,
                    loadPackIcon = viewModel::loadPackIcon,
                    resolveLabel = viewModel::resolveLabel,
                    onFolderClick = {},
                    onShortcutClick = {},
                    onOpenPageLink = { _, _ -> },
                    showLabels = true,
                    labelColor = Color.White,
                    onAddEmptySlot = { showAddChooser = true },
                    onEditEntry = { entry -> pendingRemoval = items.find { it.id == entry.id } }
                )
            }
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_panel_rows_label)) {
            rowShape.forEachIndexed { index, cols ->
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_panel_row_number, index + 1),
                    onClick = {},
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(cols.toString(), modifier = Modifier.padding(end = 4.dp))
                            IconButton(
                                onClick = { viewModel.setFolderRowColumns(folderId, rowShape.toMutableList().apply { this[index] = cols - 1 }) },
                                enabled = cols > 1
                            ) { Icon(Icons.Filled.Remove, contentDescription = null) }
                            IconButton(
                                onClick = { viewModel.setFolderRowColumns(folderId, rowShape.toMutableList().apply { this[index] = cols + 1 }) },
                                enabled = cols < 6
                            ) { Icon(Icons.Filled.Add, contentDescription = null) }
                            IconButton(
                                onClick = { viewModel.setFolderRowColumns(folderId, rowShape.filterIndexed { i, _ -> i != index }) },
                                enabled = rowShape.size > 1
                            ) { Icon(Icons.Filled.Close, contentDescription = null) }
                        }
                    }
                )
            }
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_panel_add_row),
                onClick = { viewModel.setFolderRowColumns(folderId, rowShape + 4) },
                trailing = null
            )
        }

        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.DeleteOutline,
                title = stringResource(R.string.settings_folder_delete),
                onClick = { confirmingDelete = true },
                trailing = null
            )
        }
    }

    if (appearanceOpen) {
        val effectiveContentAppearance = if (folder?.appearanceOverrideEnabled == true) folder.contentAppearance() else globalCardAppearance
        val effectiveShowLabels = if (folder?.showLabelsOverrideEnabled == true) folder.showLabels else globalShowLabels
        SettingsEditPage(title = stringResource(R.string.settings_folder_appearance_label), onDismiss = { appearanceOpen = false }) {
            ElementBackgroundSection(appearance = effectiveContentAppearance, onAppearanceChange = { viewModel.setFolderAppearance(folderId, it) })
            if (folder?.appearanceOverrideEnabled == true) {
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_folder_appearance_reset_label),
                        onClick = { viewModel.setFolderAppearanceOverrideEnabled(folderId, false) },
                        trailing = null
                    )
                }
            }
            CaptionsSection(
                showLabels = effectiveShowLabels,
                onShowLabelsChange = { viewModel.setFolderItemsShowLabels(folderId, it) },
                appearance = effectiveContentAppearance,
                onAppearanceChange = { viewModel.setFolderAppearance(folderId, it) },
                title = stringResource(R.string.settings_captions_label)
            )
        }
    }

    if (borderOpen) {
        val effectiveBorderAppearance = if (folder?.borderOverrideEnabled == true) folder.borderAppearance() else globalCardAppearance
        SettingsEditPage(title = stringResource(R.string.settings_element_border_label), onDismiss = { borderOpen = false }) {
            ElementBorderSection(appearance = effectiveBorderAppearance, onAppearanceChange = { viewModel.setFolderBorderAppearance(folderId, it) })
            if (folder?.borderOverrideEnabled == true) {
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_folder_border_reset_label),
                        onClick = { viewModel.setFolderBorderOverrideEnabled(folderId, false) },
                        trailing = null
                    )
                }
            }
        }
    }

    if (pickingFolderIcon) {
        SettingsFixedDialog(title = stringResource(R.string.settings_app_icon_label), onDismiss = { pickingFolderIcon = false }) {
            SettingsPreferenceGroup {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.setFolderIcon(folderId, FolderIconMode.AUTO_STACK); pickingFolderIcon = false }) {
                        Icon(imageVector = Icons.Filled.Apps, contentDescription = stringResource(R.string.folder_icon_auto))
                    }
                    PRESET_COLORS.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                .combinedClickable(onClick = {
                                    viewModel.setFolderIcon(folderId, FolderIconMode.CUSTOM_COLOR, color = hex)
                                    pickingFolderIcon = false
                                })
                        )
                    }
                }
            }
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = Icons.Filled.Extension,
                    title = stringResource(R.string.settings_folder_icon_pack_label),
                    summary = if (folder?.iconMode == FolderIconMode.ICON_PACK) {
                        iconPacks.find { it.packageName == folder.iconPackPackage }?.label ?: folder.iconPackPackage
                    } else null,
                    onClick = { pickingFolderIcon = false; navController.navigate("settings/folder/$folderId/icon") }
                )
            }
        }
    }

    pendingRemoval?.let { item ->
        com.bisc.flauncher.ui.home.ConfirmRemoveDialog(
            label = com.bisc.flauncher.ui.home.entryLabel(
                RowEntry(id = item.id, pageRowId = 0L, orderIndex = item.orderIndex, type = item.type, payload = item.payload),
                folders, savedMapTargets, pinnedWebsites, emptyList(), emptyList(), viewModel::resolveLabel
            ),
            onConfirm = { viewModel.removeFolderItem(item); pendingRemoval = null },
            onDismiss = { pendingRemoval = null }
        )
    }

    if (showAddChooser) {
        ModalBottomSheet(onDismissRequest = { showAddChooser = false }) {
            SettingsPreferenceGroup {
                SettingsPreference(icon = Icons.Filled.Add, title = stringResource(R.string.settings_row_entry_add_app), onClick = { showAddChooser = false; showAppPicker = true }, trailing = null)
                SettingsPreference(icon = Icons.Filled.CreateNewFolder, title = stringResource(R.string.settings_row_entry_add_folder), onClick = { showAddChooser = false; showFolderPicker = true }, trailing = null)
                SettingsPreference(icon = Icons.AutoMirrored.Filled.Shortcut, title = stringResource(R.string.settings_row_entry_add_shortcut), onClick = { showAddChooser = false; viewModel.openFolderShortcutAppPicker(folderId) }, trailing = null)
                SettingsPreference(icon = Icons.Filled.Place, title = stringResource(R.string.settings_row_entry_add_map_target), onClick = { showAddChooser = false; showMapTargetPicker = true }, trailing = null)
                SettingsPreference(icon = Icons.Filled.Language, title = stringResource(R.string.settings_row_entry_add_website), onClick = { showAddChooser = false; showWebsitePicker = true }, trailing = null)
                SettingsPreference(icon = Icons.Filled.Widgets, title = stringResource(R.string.settings_row_entry_add_widget), onClick = { showAddChooser = false; showWidgetKindPicker = true }, trailing = null)
            }
        }
    }

    if (showWidgetKindPicker) {
        FolderWidgetKindSheet(
            onKindSelected = { kind ->
                showWidgetKindPicker = false
                sizingWidgetSpanY = com.bisc.flauncher.data.model.WidgetKind.defaultSpan(kind)
                sizingWidgetKind = kind
            },
            onDismiss = { showWidgetKindPicker = false }
        )
    }

    sizingWidgetKind?.let { kind ->
        FolderWidgetSizeSheet(
            row = com.bisc.flauncher.data.model.PageRow(
                pageId = 0,
                orderIndex = 0,
                type = com.bisc.flauncher.data.model.PageRowType.WIDGET,
                payload = kind,
                spanY = sizingWidgetSpanY
            ),
            spanY = sizingWidgetSpanY,
            minSpan = com.bisc.flauncher.data.model.WidgetKind.minSpan(kind),
            maxSpan = com.bisc.flauncher.data.model.WidgetKind.maxSpan(kind),
            onSpanChange = { sizingWidgetSpanY = it },
            onSave = {
                viewModel.addWidgetToFolder(folderId, kind, sizingWidgetSpanY)
                sizingWidgetKind = null
            },
            onDismiss = { sizingWidgetKind = null }
        )
    }

    if (showAppPicker) {
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app -> viewModel.addAppToFolder(folderId, app); showAppPicker = false },
            onDismiss = { showAppPicker = false }
        )
    }

    if (showFolderPicker) {
        val alreadyInside = remember(items) {
            items.filter { it.type == GridItemType.FOLDER }.mapNotNull { it.payload.toLongOrNull() }.toSet()
        }
        // Only one level of folder nesting is allowed (see the masterplan's "weder direkt noch
        // indirekt" cycle rule) — a folder already nested somewhere else, or one that already has
        // folder children of its own, is excluded here. Together these also rule out every
        // possible cycle, not just the direct one-hop case the old `alreadyInside`-only check
        // caught: a cycle needs at least two levels of nesting to ever close back on itself.
        val nestedFolderIds = remember(itemsByFolder) {
            itemsByFolder.values.flatten().filter { it.type == GridItemType.FOLDER }.mapNotNull { it.payload.toLongOrNull() }.toSet()
        }
        val foldersWithFolderChildren = remember(itemsByFolder) {
            itemsByFolder.filterValues { folderItems -> folderItems.any { it.type == GridItemType.FOLDER } }.keys
        }
        FolderPickerDialog(
            folders = folders.filter {
                it.id != folderId && it.id !in alreadyInside && it.id !in nestedFolderIds && it.id !in foldersWithFolderChildren
            },
            folderItemsByFolder = itemsByFolder,
            loadIcon = viewModel::loadIcon,
            onPick = { picked -> viewModel.addExistingFolderToFolder(folderId, picked.id); showFolderPicker = false },
            onCreate = { name -> viewModel.createFolderInFolder(folderId, name); showFolderPicker = false },
            onDismiss = { showFolderPicker = false }
        )
    }

    if (shortcutAppPickerOpen) {
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = viewModel::selectShortcutSourceApp,
            onDismiss = viewModel::dismissShortcutAppPicker
        )
    }

    shortcutPickerForApp?.let { app ->
        AppShortcutPickerDialog(
            appLabel = app.label,
            shortcuts = availableShortcuts,
            loadIcon = viewModel::loadShortcutIcon,
            onShortcutSelected = { info -> viewModel.addShortcutToFolder(info) },
            onDismiss = viewModel::dismissShortcutPicker
        )
    }

    if (showMapTargetPicker) {
        SavedMapTargetPickerSheet(
            targets = savedMapTargets,
            onSelect = { target -> viewModel.addMapTargetToFolder(folderId, target.id); showMapTargetPicker = false },
            onAddNew = { showMapTargetPicker = false; showAddMapTargetDialog = true },
            onDismiss = { showMapTargetPicker = false }
        )
    }

    if (showAddMapTargetDialog) {
        AddMapTargetDialog(
            onSave = { label, lat, lon, address ->
                viewModel.addNewMapTargetToFolder(folderId, label, lat, lon, address)
                showAddMapTargetDialog = false
            },
            onDismiss = { showAddMapTargetDialog = false }
        )
    }

    if (showWebsitePicker) {
        SavedWebsitePickerSheet(
            websites = pinnedWebsites,
            onSelect = { website -> viewModel.addWebsiteToFolder(folderId, website.id); showWebsitePicker = false },
            onAddNew = { showWebsitePicker = false; showAddWebsiteDialog = true },
            onDismiss = { showWebsitePicker = false }
        )
    }

    if (showAddWebsiteDialog) {
        AddWebsiteDialog(
            onSave = { label, url ->
                viewModel.addNewWebsiteToFolder(folderId, label, url)
                showAddWebsiteDialog = false
            },
            onDismiss = { showAddWebsiteDialog = false }
        )
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.settings_folder_delete)) },
            text = { Text(stringResource(R.string.settings_folder_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    folder?.let(viewModel::deleteFolder)
                    onBack()
                }) { Text(stringResource(R.string.settings_folder_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderWidgetKindSheet(onKindSelected: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                com.bisc.flauncher.data.model.WidgetKind.MUSIC,
                com.bisc.flauncher.data.model.WidgetKind.WEATHER,
                com.bisc.flauncher.data.model.WidgetKind.NOTES
            ).forEach { kind ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onKindSelected(kind) }
                        .padding(horizontal = 4.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = folderWidgetKindIcon(kind), contentDescription = null)
                    Text(folderWidgetKindLabel(kind), modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderWidgetSizeSheet(
    row: com.bisc.flauncher.data.model.PageRow,
    spanY: Int,
    minSpan: Int,
    maxSpan: Int,
    onSpanChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.settings_rows_size_title), style = MaterialTheme.typography.titleLarge)
            com.bisc.flauncher.ui.home.WidgetSizeEditor(row = row.copy(spanY = spanY), minSpan = minSpan, maxSpan = maxSpan, onSpanChange = onSpanChange)
            TextButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_rows_save))
            }
        }
    }
}

private fun folderWidgetKindIcon(kind: String) = when (kind) {
    com.bisc.flauncher.data.model.WidgetKind.MUSIC -> Icons.Filled.MusicNote
    com.bisc.flauncher.data.model.WidgetKind.WEATHER -> Icons.Filled.Cloud
    com.bisc.flauncher.data.model.WidgetKind.NOTES -> Icons.Filled.EditNote
    else -> Icons.Filled.MusicNote
}

@Composable
private fun folderWidgetKindLabel(kind: String): String = when (kind) {
    com.bisc.flauncher.data.model.WidgetKind.MUSIC -> stringResource(R.string.picker_widget_music)
    com.bisc.flauncher.data.model.WidgetKind.WEATHER -> stringResource(R.string.picker_widget_weather)
    com.bisc.flauncher.data.model.WidgetKind.NOTES -> stringResource(R.string.picker_widget_notes)
    else -> ""
}

/** Full-screen icon picker (settings/folder/{folderId}/icon). */
@Composable
fun SettingsFolderIconScreen(
    folderId: Long,
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val folders by viewModel.folders.collectAsState()
    val folder = folders.find { it.id == folderId }
    val current = folder?.takeIf { it.iconMode == FolderIconMode.ICON_PACK }

    SettingsFixedScreen(title = stringResource(R.string.settings_folder_icon_pack_choose_icon), navController = navController, onBack = onBack) {
        IconPackPickerContent(
            viewModel = viewModel,
            currentPack = current?.iconPackPackage,
            currentDrawable = current?.iconPackDrawable,
            onPick = { pack, drawable ->
                viewModel.setFolderIcon(folderId, FolderIconMode.ICON_PACK, packPackage = pack, packDrawable = drawable)
                onBack()
            }
        )
    }
}

/** Icon-pack picker with search, shared by the folder and per-app icon screens. */
@Composable
fun ColumnScope.IconPackPickerContent(
    viewModel: SettingsViewModel,
    currentPack: String?,
    currentDrawable: String?,
    onPick: (pack: String, drawable: String) -> Unit,
    onReset: (() -> Unit)? = null
) {
    val iconPacks by viewModel.iconPacks.collectAsState()

    var selectedPack by rememberSaveable { mutableStateOf<String?>(null) }
    val pack = selectedPack ?: currentPack ?: iconPacks.firstOrNull()?.packageName
    var query by rememberSaveable { mutableStateOf("") }
    var icons by remember(pack) { mutableStateOf<List<IconPackIcon>?>(null) }
    LaunchedEffect(pack) {
        icons = null
        if (pack != null) icons = viewModel.availableIconPackIcons(pack)
    }
    val filtered by remember(icons) {
        derivedStateOf {
            val all = icons.orEmpty()
            val q = query.trim().lowercase()
            if (q.isEmpty()) all else all.filter { q in it.searchText }
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text(stringResource(R.string.settings_folder_icon_pack_search)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = null) }
            }
        },
        singleLine = true,
        shape = SettingsTextFieldShape,
        modifier = Modifier.fillMaxWidth()
    )
    if (onReset != null) {
        TextButton(onClick = onReset) { Text(stringResource(R.string.settings_app_icon_reset)) }
    }
    if (iconPacks.size > 1) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            iconPacks.forEach { info ->
                FilterChip(
                    selected = info.packageName == pack,
                    onClick = { selectedPack = info.packageName },
                    label = { Text(info.label) }
                )
            }
        }
    } else {
        Spacer(Modifier.height(8.dp))
    }

    val resolved = icons
    when {
        pack == null -> Text(stringResource(R.string.settings_folder_icon_pack_none_installed))
        resolved == null -> Text(stringResource(R.string.settings_folder_icon_pack_loading))
        filtered.isEmpty() -> Text(stringResource(R.string.settings_folder_icon_pack_no_results))
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(64.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(filtered, key = { it.drawable }) { icon ->
                val bitmap by produceState<ImageBitmap?>(null, pack, icon.drawable) {
                    value = withContext(Dispatchers.Default) { viewModel.loadPackIcon(pack, icon.drawable) }
                }
                val selected = currentPack == pack && currentDrawable == icon.drawable
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier)
                        .clickable { onPick(pack, icon.drawable) },
                    contentAlignment = Alignment.Center
                ) {
                    bitmap?.let { Image(bitmap = it, contentDescription = icon.drawable, modifier = Modifier.size(44.dp)) }
                }
            }
        }
    }
}

