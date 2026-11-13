package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.GridConstants
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.PagePickerSheet
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/**
 * Full-screen editor for one [PageRowType.ELEMENT_BOX] (or, for the dock, [PageRowType.SEARCH_BAR])
 * row. Like [SettingsWidgetRowEditScreen], the row itself is edited as a draft and written on Save;
 * its entries are added and removed immediately.
 */
@Composable
fun SettingsElementBoxEditScreen(
    pageId: Long,
    rowId: Long,
    navController: NavController,
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val rowEntriesByRow by viewModel.rowEntriesByRow.collectAsState()
    val browsableApps by viewModel.labeledApps.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val folderItemsByFolder by viewModel.folderItemsByFolder.collectAsState()
    val savedMapTargets by viewModel.savedMapTargets.collectAsState()
    val pinnedWebsites by viewModel.pinnedWebsites.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val shortcutAppPickerOpen by viewModel.shortcutAppPickerOpen.collectAsState()
    val shortcutPickerForApp by viewModel.shortcutPickerForApp.collectAsState()
    val availableShortcuts by viewModel.availableShortcuts.collectAsState()
    val pinnedContacts by viewModel.pinnedContacts.collectAsState()
    val actionButtons by viewModel.actionButtons.collectAsState()

    val savedRow = rowsByPage[pageId]?.find { it.id == rowId }
    // Only auto-navigate back once we've definitely seen this row exist and then watched it
    // disappear (e.g. removed from another screen) - reacting to a transient null on the very
    // first composition, before rowsByPage has had a chance to settle for this exact row, would
    // pop the screen before its real content ever gets a chance to render.
    var everSawRow by remember { mutableStateOf(false) }
    LaunchedEffect(savedRow) {
        if (savedRow != null) everSawRow = true
        else if (everSawRow) onBack()
    }
    if (savedRow == null) return
    var row by rememberDraft(savedRow)
    val entries = rowEntriesByRow[row.id].orEmpty()

    var showAppPicker by remember { mutableStateOf(false) }
    var addingElement by remember { mutableStateOf(false) }
    var showFolderCreator by remember { mutableStateOf(false) }
    var showPagePicker by remember { mutableStateOf(false) }
    var showMapTargetPicker by remember { mutableStateOf(false) }
    var showWebsitePicker by remember { mutableStateOf(false) }
    var showAddMapTargetDialog by remember { mutableStateOf(false) }
    var showAddWebsiteDialog by remember { mutableStateOf(false) }
    var pendingShortcut by remember { mutableStateOf(false) }
    var confirmingRemoveRow by remember { mutableStateOf(false) }

    // The dock row (PageRowType.SEARCH_BAR) always floats as a single-line pill regardless of its
    // own pillMode column - that flag only matters for an ordinary element box opting in. Showing the
    // toggle (off) and the irrelevant Columns stepper for the dock would be actively misleading.
    val isDock = row.type == PageRowType.SEARCH_BAR
    val isPillLayout = row.pillMode || isDock
    // The home page's own dock row is permanent.
    val canRemoveRow = row.type != PageRowType.SEARCH_BAR || pages.find { it.id == pageId }?.isHome != true

    val (picked, pick) = rememberPickedEntry(entries)
    val ordered = remember(entries) { entries.sortedBy { it.orderIndex } }
    val look = row.appearance()
    SettingsSubScreen(
        title = stringResource(R.string.settings_element_box_edit_title),
        navController = navController,
        onBack = onBack,
        onSave = { viewModel.saveRow(row) },
        dirty = row != savedRow,
        // The box as it looks, live with the draft below, and always in sight.
        header = {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RowLiveStage(look, row.cornerRadiusPercent) { _ ->
                    com.bisc.flauncher.ui.home.GridEntries(
                        entries = ordered,
                        columns = if (isPillLayout) (ordered.size + 1).coerceIn(1, 6) else row.columns,
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
                        showLabels = row.showLabels && !isPillLayout,
                        labelColor = com.bisc.flauncher.ui.home.dock.resolveElementColors(look).contentColor,
                        totalSlots = ordered.size + 1,
                        onAddEmptySlot = { pick(null); addingElement = true },
                        onEditEntry = { entry -> pick(entry) },
                        selectedEntryId = picked?.id
                    )
                }
                picked?.let { entry ->
                    val index = ordered.indexOfFirst { it.id == entry.id }
                    RowEntrySelectionBar(
                        title = com.bisc.flauncher.ui.home.entryLabel(entry, folders, savedMapTargets, pinnedWebsites, allPages, pinnedContacts, viewModel::resolveLabel, actionButtons),
                        canMoveBack = index > 0,
                        canMoveOn = index in 0 until ordered.lastIndex,
                        onMove = { delta -> viewModel.moveRowEntry(entry, delta, ordered, fixedSlots = false, slotCount = ordered.size) },
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        if (isDock) {
            val visibilityMode = row.dockVisibilityMode
            SettingsPreferenceGroup(title = stringResource(R.string.settings_dock_visibility_label)) {
                com.bisc.flauncher.data.model.DockVisibilityMode.entries.forEach { mode ->
                    RadioPreference(dockVisibilityModeLabel(mode), mode, visibilityMode, { row = row.copy(dockVisibilityMode = it) })
                }
            }
        }

        if (!isDock) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_element_box_form_label)) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_element_box_pill_mode_label),
                    summary = stringResource(R.string.settings_element_box_pill_mode_summary),
                    onClick = { row = row.copy(pillMode = !row.pillMode) },
                    trailing = { Switch(checked = row.pillMode, onCheckedChange = { row = row.copy(pillMode = it) }) }
                )
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_edge_label)) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_widgets_edge_to_edge_label),
                    info = stringResource(R.string.settings_edge_to_edge_summary),
                    onClick = { row = row.copy(edgeToEdge = !row.edgeToEdge) },
                    trailing = { Switch(checked = row.edgeToEdge, onCheckedChange = { row = row.copy(edgeToEdge = it) }) }
                )
            }
        }

        if (!isPillLayout) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_element_box_columns_label)) {
                SettingsPreference(
                    icon = null,
                    title = row.columns.toString(),
                    onClick = null,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { row = row.copy(columns = row.columns - 1) },
                                enabled = row.columns > GridConstants.MIN_ELEMENT_BOX_COLUMNS
                            ) { Icon(Icons.Filled.Remove, contentDescription = androidx.compose.ui.res.stringResource(com.bisc.flauncher.R.string.a11y_less)) }
                            IconButton(
                                onClick = { row = row.copy(columns = row.columns + 1) },
                                enabled = row.columns < GridConstants.MAX_ELEMENT_BOX_COLUMNS
                            ) { Icon(Icons.Filled.Add, contentDescription = androidx.compose.ui.res.stringResource(com.bisc.flauncher.R.string.a11y_more)) }
                        }
                    }
                )
            }
        }

        ElementAppearanceSection(
            appearance = row.appearance(),
            onAppearanceChange = { row = row.withAppearance(it) },
            styleGroupTitle = stringResource(R.string.settings_dock_background_label)
        )
        SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
            ExpandablePercentSlider(percent = row.cornerRadiusPercent, onSettled = { row = row.copy(cornerRadiusPercent = it) })
        }

        if (canRemoveRow) {
            SettingsPreferenceGroup {
                SettingsPreference(icon = Icons.Filled.DeleteOutline, title = stringResource(R.string.settings_rows_remove), onClick = { confirmingRemoveRow = true }, trailing = null)
            }
        }
    }

    if (showPagePicker) {
        PagePickerSheet(
            pages = allPages,
            onPageSelected = { page -> viewModel.addPageLinkEntryToRow(row.id, page.id); showPagePicker = false },
            onDismiss = { showPagePicker = false }
        )
    }

    if (addingElement) {
        AddElementChooserSheet(
            onAddAppClick = { addingElement = false; showAppPicker = true },
            onAddFolderClick = { addingElement = false; showFolderCreator = true },
            onAddShortcutClick = { addingElement = false; pendingShortcut = true; viewModel.openShortcutAppPicker() },
            onAddMapTargetClick = { addingElement = false; showMapTargetPicker = true },
            onAddWebsiteClick = { addingElement = false; showWebsitePicker = true },
            onAddPageLinkClick = { addingElement = false; showPagePicker = true },
            onDismiss = { addingElement = false }
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

    if (showFolderCreator) {
        val alreadyInRow = remember(entries) {
            entries.filter { it.type == GridItemType.FOLDER }.mapNotNull { it.payload.toLongOrNull() }.toSet()
        }
        FolderPickerDialog(
            folders = folders.filter { it.id !in alreadyInRow },
            folderItemsByFolder = folderItemsByFolder,
            loadIcon = viewModel::loadIcon,
            onPick = { folder -> viewModel.addExistingFolderToRow(row.id, folder.id); showFolderCreator = false },
            onDismiss = { showFolderCreator = false }
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
            },
            onDismiss = {
                pendingShortcut = false
                viewModel.dismissShortcutPicker()
            }
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
            onSave = { label, lat, lon, address ->
                viewModel.addNewMapTargetToRow(row.id, label, lat, lon, address)
                showAddMapTargetDialog = false
            },
            onDismiss = { showAddMapTargetDialog = false }
        )
    }

    if (showAddWebsiteDialog) {
        AddWebsiteDialog(
            onSave = { label, url ->
                viewModel.addNewWebsiteToRow(row.id, label, url)
                showAddWebsiteDialog = false
            },
            onDismiss = { showAddWebsiteDialog = false }
        )
    }

    if (confirmingRemoveRow) {
        AlertDialog(
            onDismissRequest = { confirmingRemoveRow = false },
            title = { Text(stringResource(R.string.settings_rows_remove)) },
            confirmButton = {
                TextButton(onClick = { confirmingRemoveRow = false; viewModel.removeRow(row); onBack() }) { Text(stringResource(R.string.settings_rows_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingRemoveRow = false }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }
}
