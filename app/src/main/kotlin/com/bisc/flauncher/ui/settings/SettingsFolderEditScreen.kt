package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.asWidgetRow
import com.bisc.flauncher.data.model.withWidgetRow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import com.bisc.flauncher.data.model.appearance
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
    viewModel: SettingsViewModel = hiltViewModel(),
    // Adding app widgets (picker, bind permission, configuration) runs through the same flow as on pages.
    homeViewModel: com.bisc.flauncher.ui.home.HomeViewModel = hiltViewModel()
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
    val browsableApps by viewModel.labeledApps.collectAsState()
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
    var showWidgetKindPicker by remember { mutableStateOf(false) }
    var sizingWidgetKind by remember { mutableStateOf<String?>(null) }
    var sizingWidgetSpanY by remember { androidx.compose.runtime.mutableIntStateOf(1) }
    var selectedId by remember(folderId) { mutableStateOf<Long?>(null) }
    val systemWidgetPickerForFolder by homeViewModel.systemWidgetPickerOpenForFolder.collectAsState()
    val pendingBindIntent by homeViewModel.pendingBindIntent.collectAsState()
    val pendingConfigureIntent by homeViewModel.pendingConfigureIntent.collectAsState()
    val bindWidgetLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result -> homeViewModel.onSystemWidgetBindResult(result.resultCode == android.app.Activity.RESULT_OK) }
    val configureWidgetLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result -> homeViewModel.onSystemWidgetConfigureResult(result.resultCode == android.app.Activity.RESULT_OK) }
    LaunchedEffect(pendingBindIntent) { pendingBindIntent?.let { bindWidgetLauncher.launch(it) } }
    LaunchedEffect(pendingConfigureIntent) { pendingConfigureIntent?.let { configureWidgetLauncher.launch(it) } }
    val globalShowLabels by viewModel.folderShowLabels.collectAsState()

    val newName = edited?.trim()
    val nameChanged = folder != null && !newName.isNullOrBlank() && newName != folder.name
    SettingsSubScreen(
        title = stringResource(R.string.settings_folder_edit_title),
        navController = navController,
        onBack = onBack,
        onSave = { newName?.takeIf { nameChanged }?.let { viewModel.renameFolder(folderId, it) } },
        dirty = nameChanged
    ) {
        if (folder == null) return@SettingsSubScreen

        val (widgetItems, iconItems) = items.partition { it.isWidget() }
        val content = if (folder.appearanceOverrideEnabled) folder.contentAppearance() else globalCardAppearance
        val border = if (folder.borderOverrideEnabled) folder.borderAppearance() else globalCardAppearance
        val look = content.copy(borderEnabled = border.borderEnabled, borderColor = border.borderColor, borderWidth = border.borderWidth)
        val showLabels = if (folder.showLabelsOverrideEnabled) folder.showLabels else globalShowLabels
        val rowShape = folder.rows()

        // Its icon and name side by side, like the folder sits on a page.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .clickable { pickingFolderIcon = true },
                contentAlignment = Alignment.Center
            ) {
                FolderIcon(folder = folder, previewItems = items, loadIcon = viewModel::loadIcon, loadPackIcon = viewModel::loadPackIcon, modifier = Modifier.size(56.dp))
            }
            OutlinedTextField(
                value = edited ?: folder.name,
                onValueChange = { edited = it },
                label = { Text(stringResource(R.string.settings_folder_name_label)) },
                supportingText = { Text(stringResource(R.string.settings_folder_icon_tap_hint)) },
                singleLine = true,
                shape = SettingsTextFieldShape,
                modifier = Modifier.weight(1f)
            )
        }

        // Where the folder sits: the Dock and the Panel are where most go, one tap away.
        val homeRowsByPage by homeViewModel.rowsByPage.collectAsState()
        val homePages by homeViewModel.pages.collectAsState()
        val entriesByRow by homeViewModel.rowEntriesByRow.collectAsState()
        val homeRows = homeRowsByPage[homePages.find { it.isHome }?.id].orEmpty()
        SettingsPreferenceGroup(title = stringResource(R.string.settings_folder_place_label)) {
            listOf(
                homeRows.find { it.type == com.bisc.flauncher.data.model.PageRowType.SEARCH_BAR } to R.string.settings_folder_in_dock,
                homeRows.find { it.type == com.bisc.flauncher.data.model.PageRowType.PANEL } to R.string.settings_folder_in_panel
            ).forEach { (row, label) ->
                if (row != null) {
                    val entry = entriesByRow[row.id].orEmpty().find { it.type == GridItemType.FOLDER && it.payload == folderId.toString() }
                    val toggle = { if (entry != null) homeViewModel.removeRowEntry(entry) else homeViewModel.addExistingFolderToRow(row.id, folderId) }
                    SettingsPreference(
                        icon = null,
                        title = stringResource(label),
                        onClick = toggle,
                        trailing = { androidx.compose.material3.Switch(checked = entry != null, onCheckedChange = { toggle() }) }
                    )
                }
            }
        }

        // The folder itself, live: tap a part to work on it, the empty tile adds something.
        SettingsGroupLabel(stringResource(R.string.settings_folder_content_label))
        FolderLivePreview(
            look = look,
            widgets = widgetItems,
            gridItems = iconItems,
            rowShape = rowShape,
            showLabels = showLabels,
            selectedId = selectedId,
            onSelect = { selectedId = if (selectedId == it.id) null else it.id },
            onAdd = { slot -> viewModel.reserveFolderSlot(folderId, slot); showAddChooser = true },
            widgetIcon = ::folderWidgetIcon,
            widgetLabel = { folderWidgetLabel(it, homeViewModel) },
            itemLabel = { item ->
                com.bisc.flauncher.ui.home.entryLabel(
                    RowEntry(id = item.id, pageRowId = 0L, orderIndex = item.orderIndex, type = item.type, payload = item.payload),
                    folders, savedMapTargets, pinnedWebsites, emptyList(), emptyList(), viewModel::resolveLabel
                )
            },
            itemIcon = { item, size -> FolderItemIcon(item, size, folders, itemsByFolder, viewModel::loadIcon, viewModel::loadPackIcon) }
        )
        Text(
            stringResource(R.string.settings_folder_live_hint),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        val selected = items.find { it.id == selectedId }
        if (selected != null) {
            val isWidget = selected.isWidget()
            val siblings = if (isWidget) widgetItems else iconItems
            val position = siblings.indexOfFirst { it.id == selected.id }
            val cellCount = com.bisc.flauncher.ui.home.folderSlotRows(iconItems, rowShape, keepEmptyRows = true).sumOf { it.size } + rowShape.ifEmpty { listOf(4) }.last()
            val widgetRowCount = com.bisc.flauncher.ui.home.folderSlotRows(iconItems, rowShape, keepEmptyRows = true).size
            val isBuiltIn = selected.type == GridItemType.WIDGET
            val minSpan = if (isBuiltIn) com.bisc.flauncher.data.model.WidgetKind.minSpan(selected.payload) else 1
            val maxSpan = if (isWidget) maxOf(
                minOf(
                    if (isBuiltIn) com.bisc.flauncher.data.model.WidgetKind.maxSpan(selected.payload) else MAX_FOLDER_APP_WIDGET_SPAN,
                    com.bisc.flauncher.ui.home.folderWidgetMaxSpan(folder, items, selected, showLabels)
                ),
                selected.spanY,
                minSpan
            ) else 1
            FolderSelectionBar(
                title = if (isWidget) folderWidgetLabel(selected, homeViewModel) else com.bisc.flauncher.ui.home.entryLabel(
                    RowEntry(id = selected.id, pageRowId = 0L, orderIndex = selected.orderIndex, type = selected.type, payload = selected.payload),
                    folders, savedMapTargets, pinnedWebsites, emptyList(), emptyList(), viewModel::resolveLabel
                ),
                isWidget = isWidget,
                span = selected.spanY,
                canGrow = selected.spanY < maxSpan,
                canShrink = selected.spanY > minSpan,
                onSpan = { viewModel.updateFolderItem(selected.copy(spanY = it)) },
                // Widgets swap with their neighbour; apps and elements step through the cells, empty ones too.
                canMoveBack = if (isWidget) com.bisc.flauncher.ui.home.canMoveWidget(widgetItems, selected, -1, widgetRowCount) else selected.orderIndex > 0,
                canMoveOn = if (isWidget) com.bisc.flauncher.ui.home.canMoveWidget(widgetItems, selected, 1, widgetRowCount) else selected.orderIndex < cellCount - 1,
                onMove = { delta ->
                    if (isWidget) {
                        homeViewModel.saveFolderItems(com.bisc.flauncher.ui.home.moveWidgetStep(widgetItems, selected, delta, widgetRowCount))
                    } else {
                        viewModel.moveFolderItemToSlot(selected, selected.orderIndex + delta)
                    }
                },
                onOpenSettings = if (isWidget) ({ navController.navigate("settings/folder/$folderId/widget/${selected.id}") }) else null,
                onRemove = { pendingRemoval = selected },
                onClose = { selectedId = null }
            )
        }

        FolderAddButtons(onAddContent = { viewModel.reserveFolderSlot(folderId, null); showAddChooser = true }, onAddWidget = { showWidgetKindPicker = true })

        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_folder_rows_label),
            info = stringResource(R.string.settings_folder_rows_info)
        ) {
            FolderRowsEditor(rowShape, com.bisc.flauncher.data.model.MAX_FOLDER_ROW_COLUMNS) { viewModel.setFolderRowColumns(folderId, it) }
        }

        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_home_appearance_title),
                summary = stringResource(
                    if (folder.appearanceOverrideEnabled || folder.borderOverrideEnabled) R.string.settings_folder_look_own
                    else R.string.settings_folder_look_like_all
                ),
                onClick = { appearanceOpen = true },
                trailing = null
            )
        }
        // Captions belong with the items, like the Dock's and Panel's own.
        CaptionsSection(
            showLabels = showLabels,
            onShowLabelsChange = { viewModel.setFolderItemsShowLabels(folderId, it) },
            appearance = content,
            onAppearanceChange = { viewModel.setFolderAppearance(folderId, it) },
            title = stringResource(R.string.settings_captions_label)
        )

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
        // Fill and border are kept apart (each can follow all folders on its own), but edited together.
        val content = if (folder?.appearanceOverrideEnabled == true) folder.contentAppearance() else globalCardAppearance
        val border = if (folder?.borderOverrideEnabled == true) folder.borderAppearance() else globalCardAppearance
        val saved = content.copy(borderEnabled = border.borderEnabled, borderColor = border.borderColor, borderWidth = border.borderWidth)
        var draft by rememberDraft(saved)
        SettingsEditPage(
            title = stringResource(R.string.settings_home_appearance_title),
            onDismiss = { appearanceOpen = false },
            onSave = {
                val fillChanged = draft.fillMode != saved.fillMode || draft.fill != saved.fill || draft.opacity != saved.opacity || draft.contentColor != saved.contentColor
                val borderChanged = draft.borderEnabled != saved.borderEnabled || draft.borderColor != saved.borderColor || draft.borderWidth != saved.borderWidth
                if (fillChanged) viewModel.setFolderAppearance(folderId, draft)
                if (borderChanged) viewModel.setFolderBorderAppearance(folderId, draft)
            },
            dirty = draft != saved
        ) {
            LookSample(LookSampleKind.FOLDER, draft, 100)
            // Its state right under the preview, like on every look page; the way back is beside it.
            val own = folder?.appearanceOverrideEnabled == true || folder?.borderOverrideEnabled == true
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (own) R.string.settings_look_custom else R.string.settings_folder_look_like_all),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (own) AutoResetChip(
                    onClick = {
                        viewModel.setFolderAppearanceOverrideEnabled(folderId, false)
                        viewModel.setFolderBorderOverrideEnabled(folderId, false)
                        appearanceOpen = false
                    },
                    label = stringResource(R.string.settings_folder_look_reset)
                )
            }
            ElementAppearanceSection(appearance = draft, onAppearanceChange = { draft = it }, styleGroupTitle = stringResource(R.string.settings_element_fill_label))
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
            label = if (item.isWidget()) folderWidgetLabel(item, homeViewModel) else com.bisc.flauncher.ui.home.entryLabel(
                RowEntry(id = item.id, pageRowId = 0L, orderIndex = item.orderIndex, type = item.type, payload = item.payload),
                folders, savedMapTargets, pinnedWebsites, emptyList(), emptyList(), viewModel::resolveLabel
            ),
            onConfirm = { viewModel.removeFolderItem(item); pendingRemoval = null; if (selectedId == item.id) selectedId = null },
            onDismiss = { pendingRemoval = null }
        )
    }

    if (showAddChooser) {
        AddElementChooserSheet(
            onAddAppClick = { showAddChooser = false; showAppPicker = true },
            onAddFolderClick = { showAddChooser = false; showFolderPicker = true },
            onAddShortcutClick = { showAddChooser = false; viewModel.openFolderShortcutAppPicker(folderId) },
            onAddMapTargetClick = { showAddChooser = false; showMapTargetPicker = true },
            onAddWebsiteClick = { showAddChooser = false; showWebsitePicker = true },
            onDismiss = { showAddChooser = false }
        )
    }

    if (showWidgetKindPicker) {
        FolderWidgetKindSheet(
            onKindSelected = { kind ->
                showWidgetKindPicker = false
                sizingWidgetSpanY = com.bisc.flauncher.data.model.WidgetKind.defaultSpan(kind)
                sizingWidgetKind = kind
            },
            onMoreSelected = {
                showWidgetKindPicker = false
                homeViewModel.openSystemWidgetPickerForFolder(folderId)
            },
            onDismiss = { showWidgetKindPicker = false }
        )
    }

    systemWidgetPickerForFolder?.let { targetFolderId ->
        val providers = remember(targetFolderId) { homeViewModel.installedSystemWidgetProviders() }
        com.bisc.flauncher.ui.home.SystemWidgetPickerSheet(
            providers = providers,
            onProviderSelected = { provider -> homeViewModel.startAddSystemWidgetToFolder(targetFolderId, provider) },
            onDismiss = homeViewModel::dismissSystemWidgetPicker
        )
    }

    sizingWidgetKind?.let { kind ->
        FolderWidgetSizeSheet(
            row = newWidgetPreviewRow(0, kind, sizingWidgetSpanY),
            spanY = sizingWidgetSpanY,
            minSpan = com.bisc.flauncher.data.model.WidgetKind.minSpan(kind),
            maxSpan = minOf(
                com.bisc.flauncher.data.model.WidgetKind.maxSpan(kind),
                folder?.let { com.bisc.flauncher.ui.home.folderWidgetMaxSpan(it, items, null, globalShowLabels) } ?: MAX_FOLDER_APP_WIDGET_SPAN
            ).coerceAtLeast(com.bisc.flauncher.data.model.WidgetKind.minSpan(kind)),
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
        // Only one level of folder nesting is allowed - a folder already nested somewhere else, or
        // one that already has folder children of its own, is excluded here. That also rules out
        // every possible cycle: a cycle needs at least two levels of nesting to close on itself.
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
internal fun FolderWidgetKindSheet(onKindSelected: (String) -> Unit, onMoreSelected: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                com.bisc.flauncher.data.model.WidgetKind.CLOCK,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onMoreSelected)
                    .padding(horizontal = 4.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Filled.Widgets, contentDescription = null)
                Text(stringResource(R.string.picker_widget_more), modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderWidgetSizeSheet(
    row: com.bisc.flauncher.data.model.PageRow,
    spanY: Int,
    minSpan: Int,
    maxSpan: Int,
    onSpanChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // The preview scrolls when it's taller than the sheet; the buttons below always stay reachable.
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.settings_rows_size_title), style = MaterialTheme.typography.titleLarge)
            com.bisc.flauncher.ui.home.WidgetSizeEditor(row = row.copy(spanY = spanY), minSpan = minSpan, maxSpan = maxSpan, onSpanChange = onSpanChange)
        }
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_rows_save))
            }
            if (onRemove != null) {
                TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_rows_remove), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/** Largest an app widget in a folder can be, in rows - less if the folder would no longer fit the screen. */
private const val MAX_FOLDER_APP_WIDGET_SPAN = 6

internal fun folderWidgetIcon(item: com.bisc.flauncher.data.model.FolderItem) =
    if (item.type == GridItemType.WIDGET) folderWidgetKindIcon(item.payload) else Icons.Filled.Widgets

@Composable
internal fun folderWidgetLabel(item: com.bisc.flauncher.data.model.FolderItem, homeViewModel: com.bisc.flauncher.ui.home.HomeViewModel): String {
    if (item.type == GridItemType.WIDGET) return folderWidgetKindLabel(item.payload)
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(item.payload) {
        item.payload.toIntOrNull()?.let(homeViewModel::getSystemWidgetInfo)?.loadLabel(context.packageManager)
    } ?: stringResource(R.string.settings_folder_widget_app_widget)
}

private fun folderWidgetKindIcon(kind: String) = when (kind) {
    com.bisc.flauncher.data.model.WidgetKind.CLOCK -> Icons.Filled.Schedule
    com.bisc.flauncher.data.model.WidgetKind.MUSIC -> Icons.Filled.MusicNote
    com.bisc.flauncher.data.model.WidgetKind.WEATHER -> Icons.Filled.Cloud
    com.bisc.flauncher.data.model.WidgetKind.NOTES -> Icons.Filled.EditNote
    else -> Icons.Filled.MusicNote
}

@Composable
private fun folderWidgetKindLabel(kind: String): String = when (kind) {
    com.bisc.flauncher.data.model.WidgetKind.CLOCK -> stringResource(R.string.picker_widget_clock)
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


/** One widget in a folder, with the same editor as widgets on pages (minus what only pages have:
 *  free pixel height and edge to edge). Its height is limited so the folder still fits. */
@Composable
fun SettingsFolderWidgetEditScreen(
    folderId: Long,
    itemId: Long,
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    homeViewModel: com.bisc.flauncher.ui.home.HomeViewModel = hiltViewModel()
) {
    val folders by viewModel.folders.collectAsState()
    val folderItems by viewModel.folderItems.collectAsState()
    val showLabels by viewModel.folderShowLabels.collectAsState()
    val folder = folders.find { it.id == folderId }
    val items = folderItems.filter { it.folderId == folderId }
    val item = items.find { it.id == itemId }
    LaunchedEffect(item == null) { if (item == null) onBack() }
    if (item == null || folder == null) return
    val isBuiltIn = item.type == GridItemType.WIDGET
    val minSpan = if (isBuiltIn) com.bisc.flauncher.data.model.WidgetKind.minSpan(item.payload) else 1
    // Its own height always counts as possible, so it can always be made smaller again.
    val maxSpan = maxOf(
        minOf(
            if (isBuiltIn) com.bisc.flauncher.data.model.WidgetKind.maxSpan(item.payload) else MAX_FOLDER_APP_WIDGET_SPAN,
            com.bisc.flauncher.ui.home.folderWidgetMaxSpan(folder, items, item, showLabels)
        ),
        item.spanY,
        minSpan
    )
    WidgetEditor(
        savedRow = remember(item) { item.asWidgetRow() },
        navController = navController,
        onBack = onBack,
        onSave = { row -> viewModel.updateFolderItem(item.withWidgetRow(row)) },
        onRemove = { viewModel.removeFolderItem(item) },
        minSpan = minSpan,
        maxSpan = maxSpan,
        heightPxRange = null,
        gridUnit = com.bisc.flauncher.ui.home.PageRowDimens.rowUnitHeight,
        showEdgeToEdge = false,
        viewModel = homeViewModel
    )
}

