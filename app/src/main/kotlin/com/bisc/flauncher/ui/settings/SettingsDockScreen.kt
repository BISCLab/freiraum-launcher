package com.bisc.flauncher.ui.settings

import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.DockVisibilityMode
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.data.model.dockRows
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.entryLabel
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/** The Dock: how it looks, with a live preview, and the way to its visibility and elements. */
@Composable
fun SettingsDockScreen(pageId: Long, navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val row = rememberDockRow(pageId, viewModel) ?: return
    val entriesByRow by viewModel.rowEntriesByRow.collectAsState()
    val count = entriesByRow[row.id].orEmpty().size
    SettingsSubScreen(title = stringResource(R.string.settings_home_dock_label), navController = navController, onBack = onBack) {
        LookSample(LookSampleKind.DOCK, row.appearance(), row.cornerRadiusPercent)
        SettingsTileGrid(
            columns = 3,
            tiles = listOf(
                SettingsTile(Icons.Filled.GridView, stringResource(R.string.settings_dock_elements_title),
                    badge = androidx.compose.ui.res.pluralStringResource(R.plurals.settings_hub_elements_count, count, count)) { onNavigate("elements") },
                SettingsTile(Icons.Filled.Visibility, stringResource(R.string.settings_dock_visibility_label),
                    badge = dockVisibilityModeLabel(row.dockVisibilityMode)) { onNavigate("visibility") },
                SettingsTile(Icons.Filled.Palette, stringResource(R.string.settings_home_appearance_title)) { onNavigate("look") }
            )
        )
    }
}

/** The Dock's look: fill, text, opacity, border and corners. */
@Composable
fun SettingsDockLookScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val saved = rememberDockRow(pageId, viewModel) ?: return
    ElementLookScreen(
        title = stringResource(R.string.settings_home_dock_label),
        navController = navController,
        onBack = onBack,
        saved = saved.appearance(),
        savedCorner = saved.cornerRadiusPercent,
        onSave = { look, corner -> viewModel.saveRow(saved.withAppearance(look).copy(cornerRadiusPercent = corner ?: saved.cornerRadiusPercent)) },
        kind = LookSampleKind.DOCK
    )
}

/** Finds the Home page's own pinned dock row, wherever it's needed across the 4 Dock sub-screens - 
 *  always present once seeded (see PageContentRepository.ensureSeeded), so a null here only means
 *  the data hasn't loaded yet; the caller falls back to an empty screen for that one frame. */
@Composable
private fun rememberDockRow(pageId: Long, viewModel: HomeViewModel): PageRow? {
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    return rowsByPage[pageId]?.find { it.type == PageRowType.SEARCH_BAR }
}

@Composable
fun SettingsDockVisibilityScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val row = rememberDockRow(pageId, viewModel) ?: return
    SettingsSubScreen(title = stringResource(R.string.settings_dock_visibility_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            DockVisibilityMode.entries.forEach { mode ->
                RadioPreference(dockVisibilityModeLabel(mode), mode, row.dockVisibilityMode, { viewModel.setRowDockVisibilityMode(row, it) })
            }
        }
    }
}

/** The Dock's own grid editor - the Dock is a mirrored Panel, so this mirrors
 *  [SettingsPanelElementsScreen] exactly (same "+" preview/chooser, same tap-a-filled-slot-to-remove
 *  instead of a separate reorderable list), just capped at 2 sub-rows instead of 6. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDockElementsScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val row = rememberDockRow(pageId, viewModel) ?: return
    val rowEntriesByRow by viewModel.rowEntriesByRow.collectAsState()
    val entries = rowEntriesByRow[row.id].orEmpty()
    val browsableApps by viewModel.labeledApps.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val folderItemsByFolder by viewModel.folderItemsByFolder.collectAsState()
    val savedMapTargets by viewModel.savedMapTargets.collectAsState()
    val pinnedWebsites by viewModel.pinnedWebsites.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val shortcutAppPickerOpen by viewModel.shortcutAppPickerOpen.collectAsState()
    val shortcutPickerForApp by viewModel.shortcutPickerForApp.collectAsState()
    val availableShortcuts by viewModel.availableShortcuts.collectAsState()
    val pinnedContacts by viewModel.pinnedContacts.collectAsState()
    val contactPickerOpen by viewModel.contactPickerOpen.collectAsState()
    val contactPickerList by viewModel.contactPickerList.collectAsState()
    val actionButtons by viewModel.actionButtons.collectAsState()
    val rowShape = row.dockRows()

    var showAddChooser by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showPagePicker by remember { mutableStateOf(false) }
    var showMapTargetPicker by remember { mutableStateOf(false) }
    var showWebsitePicker by remember { mutableStateOf(false) }
    var showAddMapTargetDialog by remember { mutableStateOf(false) }
    var showAddWebsiteDialog by remember { mutableStateOf(false) }
    var showActionButtonPicker by remember { mutableStateOf(false) }
    var pendingShortcut by remember { mutableStateOf(false) }

    val (picked, pick) = rememberPickedEntry(entries)
    val slotCount = rowShape.sum()
    val look = row.appearance()
    SettingsSubScreen(
        title = stringResource(R.string.settings_dock_elements_title),
        navController = navController,
        onBack = onBack,
        // The Dock stays in sight while its settings below scroll.
        header = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RowLiveStage(look, row.cornerRadiusPercent) { fit ->
                    com.bisc.flauncher.ui.home.GridEntries(
                        iconSizeScale = fit(rowShape.maxOrNull() ?: 5),
                        entries = entries,
                        columns = 1,
                        rowColumns = rowShape,
                        fixedSlots = true,
                        folders = folders,
                        folderItemsByFolder = folderItemsByFolder,
                        savedMapTargets = savedMapTargets,
                        pinnedWebsites = pinnedWebsites,
                        pages = allPages,
                        loadIcon = viewModel::loadIcon,
                        loadPackIcon = viewModel::loadPackIcon,
                        resolveLabel = viewModel::resolveLabel,
                        onFolderClick = {},
                        onShortcutClick = {},
                        onOpenPageLink = { _, _ -> },
                        pinnedContacts = pinnedContacts,
                        loadContactPhoto = viewModel::loadContactPhoto,
                        onContactClick = {},
                        actionButtons = actionButtons,
                        onActionButtonClick = {},
                        showLabels = row.showLabels,
                        labelColor = com.bisc.flauncher.ui.home.dock.resolveElementColors(look).contentColor,
                        onAddEmptySlot = { slot -> pick(null); viewModel.reserveRowSlot(row.id, slot); showAddChooser = true },
                        onEditEntry = { entry -> pick(entry) },
                        selectedEntryId = picked?.id,
                        rowGap = 8.dp
                    )
                }
                picked?.let { entry ->
                    RowEntrySelectionBar(
                        title = com.bisc.flauncher.ui.home.entryLabel(entry, folders, savedMapTargets, pinnedWebsites, allPages, pinnedContacts, viewModel::resolveLabel, actionButtons),
                        canMoveBack = entry.orderIndex > 0,
                        canMoveOn = entry.orderIndex < slotCount - 1,
                        onMove = { delta -> viewModel.moveRowEntry(entry, delta, entries, fixedSlots = true, slotCount = slotCount) },
                        onOpenSettings = entrySettingsRoute(entry)?.let { route -> { navController.navigate(route) } },
                        onRemove = { viewModel.removeRowEntry(entry); pick(null) },
                        onClose = { pick(null) }
                    )
                }
            }
        }
    ) {
        if (picked == null) {
            Text(
                stringResource(R.string.settings_live_editor_hint),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_panel_rows_label)) {
            FolderRowsEditor(
                rowShape = rowShape,
                maxColumns = 5,
                onChange = { viewModel.setDockRowColumns(row, it) },
                maxRows = 2,
                newRowColumns = 5
            )
        }
        CaptionsSection(
            showLabels = row.showLabels,
            onShowLabelsChange = { viewModel.setRowShowLabels(row, it) },
            appearance = row.appearance(),
            onAppearanceChange = { viewModel.setRowAppearance(row, it) }
        )
    }

    if (showAddChooser) {
        AddElementChooserSheet(
            onAddAppClick = { showAddChooser = false; showAppPicker = true },
            onAddFolderClick = { showAddChooser = false; showFolderPicker = true },
            onAddShortcutClick = { showAddChooser = false; pendingShortcut = true; viewModel.openShortcutAppPicker() },
            onAddMapTargetClick = { showAddChooser = false; showMapTargetPicker = true },
            onAddWebsiteClick = { showAddChooser = false; showWebsitePicker = true },
            onAddPageLinkClick = { showAddChooser = false; showPagePicker = true },
            onAddContactClick = { showAddChooser = false; viewModel.openContactPicker() },
            onAddActionButtonClick = { showAddChooser = false; showActionButtonPicker = true },
            onDismiss = { showAddChooser = false }
        )
    }
    if (showActionButtonPicker) {
        ActionButtonPickerSheet(
            buttons = actionButtons,
            onSelect = { button -> viewModel.addActionButtonEntryToRow(row.id, button.id); showActionButtonPicker = false },
            onDismiss = { showActionButtonPicker = false }
        )
    }
    if (showAppPicker) {
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app -> viewModel.addAppEntryToRow(row.id, app); showAppPicker = false },
            onDismiss = { showAppPicker = false }
        )
    }
    if (showFolderPicker) {
        val alreadyInRow = remember(entries) {
            entries.filter { it.type == com.bisc.flauncher.data.model.GridItemType.FOLDER }.mapNotNull { it.payload.toLongOrNull() }.toSet()
        }
        FolderPickerDialog(
            folders = folders.filter { it.id !in alreadyInRow },
            folderItemsByFolder = folderItemsByFolder,
            loadIcon = viewModel::loadIcon,
            onPick = { folder -> viewModel.addExistingFolderToRow(row.id, folder.id); showFolderPicker = false },
            onDismiss = { showFolderPicker = false }
        )
    }
    if (showPagePicker) {
        com.bisc.flauncher.ui.home.dock.PagePickerSheet(
            pages = allPages,
            onPageSelected = { page -> viewModel.addPageLinkEntryToRow(row.id, page.id); showPagePicker = false },
            onDismiss = { showPagePicker = false }
        )
    }
    if (showMapTargetPicker) {
        SavedMapTargetPickerSheet(
            targets = savedMapTargets,
            onSelect = { target -> viewModel.addMapTargetEntryToRow(row.id, target.id); showMapTargetPicker = false },
            onAddNew = { showMapTargetPicker = false; showAddMapTargetDialog = true },
            onDismiss = { showMapTargetPicker = false }
        )
    }
    if (showWebsitePicker) {
        SavedWebsitePickerSheet(
            websites = pinnedWebsites,
            onSelect = { website -> viewModel.addWebsiteEntryToRow(row.id, website.id); showWebsitePicker = false },
            onAddNew = { showWebsitePicker = false; showAddWebsiteDialog = true },
            onDismiss = { showWebsitePicker = false }
        )
    }
    if (showAddMapTargetDialog) {
        AddMapTargetDialog(
            onSave = { label, lat, lon, address -> viewModel.addNewMapTargetToRow(row.id, label, lat, lon, address); showAddMapTargetDialog = false },
            onDismiss = { showAddMapTargetDialog = false }
        )
    }
    if (showAddWebsiteDialog) {
        AddWebsiteDialog(
            onSave = { label, url -> viewModel.addNewWebsiteToRow(row.id, label, url); showAddWebsiteDialog = false },
            onDismiss = { showAddWebsiteDialog = false }
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
            onShortcutSelected = { info ->
                if (pendingShortcut) viewModel.addShortcutEntryToRow(row.id, info)
                pendingShortcut = false
                viewModel.dismissShortcutPicker()
            },
            onDismiss = { pendingShortcut = false; viewModel.dismissShortcutPicker() }
        )
    }
    if (contactPickerOpen) {
        com.bisc.flauncher.ui.home.dock.ContactPickerSheet(
            contacts = contactPickerList,
            loadPhoto = viewModel::loadContactPhoto,
            onSelected = { contact -> viewModel.addContactEntryToRow(row.id, contact); viewModel.dismissContactPicker() },
            onDismiss = viewModel::dismissContactPicker
        )
    }
}

/** Shared "what do you want to add" chooser, opened by tapping an empty "+" slot in the Dock's or
 *  Panel's element preview. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddElementChooserSheet(
    onAddAppClick: () -> Unit,
    onAddFolderClick: () -> Unit,
    onAddShortcutClick: () -> Unit,
    onAddMapTargetClick: () -> Unit,
    onAddWebsiteClick: () -> Unit,
    onAddPageLinkClick: (() -> Unit)? = null,
    onAddContactClick: (() -> Unit)? = null,
    onAddActionButtonClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    // Every kind in a tile of the same size, named in a word: the title already says "Add".
    // New folders are made under Folders; here an existing one is put in place.
    val tiles = listOfNotNull(
        SettingsTile(Icons.Filled.Apps, stringResource(R.string.settings_add_kind_app), onClick = onAddAppClick),
        SettingsTile(Icons.Filled.Folder, stringResource(R.string.settings_add_kind_folder), onClick = onAddFolderClick),
        SettingsTile(Icons.AutoMirrored.Filled.Shortcut, stringResource(R.string.settings_add_kind_shortcut), onClick = onAddShortcutClick),
        SettingsTile(Icons.Filled.Place, stringResource(R.string.settings_add_kind_place), onClick = onAddMapTargetClick),
        SettingsTile(Icons.Filled.Language, stringResource(R.string.settings_add_kind_link), onClick = onAddWebsiteClick),
        onAddPageLinkClick?.let { SettingsTile(Icons.Filled.ViewCarousel, stringResource(R.string.settings_add_kind_page), onClick = it) },
        onAddContactClick?.let { SettingsTile(Icons.Filled.Person, stringResource(R.string.settings_add_kind_contact), onClick = it) },
        onAddActionButtonClick?.let { SettingsTile(Icons.Filled.Bolt, stringResource(R.string.settings_add_kind_action), onClick = it) }
    )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.settings_add_element_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp, bottom = 14.dp))
            SettingsTileGrid(tiles = tiles, columns = 3)
        }
    }
}

/** Asks for the name of a new folder. */
@Composable
internal fun NewFolderNameDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_row_entry_new_folder)) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_folder_name_label)) },
                singleLine = true
            )
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onCreate(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.settings_save)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.widget_notes_cancel)) } }
    )
}


/** Where an element placed in a row is set up, or null for kinds with nothing of their own to set. */
internal fun entrySettingsRoute(entry: com.bisc.flauncher.data.model.RowEntry): String? = when (entry.type) {
    com.bisc.flauncher.data.model.GridItemType.APP -> "settings/apps/edit/" + android.net.Uri.encode(entry.payload)
    com.bisc.flauncher.data.model.GridItemType.FOLDER -> "settings/folder/${entry.payload}"
    com.bisc.flauncher.data.model.GridItemType.MAP_TARGET -> "settings/elements/addresses/${entry.payload}"
    com.bisc.flauncher.data.model.GridItemType.WEBSITE -> "settings/elements/links/${entry.payload}"
    com.bisc.flauncher.data.model.GridItemType.ACTION_BUTTON -> "settings/elements/actionbuttons/${entry.payload}"
    else -> null
}

/** A tapped element of the Dock or Panel: its own settings, or out of here. */
@Composable
internal fun EntryActionsDialog(label: String, onOpenSettings: (() -> Unit)?, onRemove: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = { Text(stringResource(R.string.settings_entry_actions_text)) },
        confirmButton = {
            Row {
                androidx.compose.material3.TextButton(onClick = onRemove) { Text(stringResource(R.string.folder_item_remove)) }
                if (onOpenSettings != null) androidx.compose.material3.TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.settings_entry_open_settings)) }
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.widget_notes_cancel)) } }
    )
}
