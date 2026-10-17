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
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.PagePickerSheet
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/**
 * Full-screen editor for one [PageRowType.APPS] (or, for the dock, [PageRowType.SEARCH_BAR]) row —
 * the masterplan Phase 9 template for converting the remaining ModalBottomSheet row editors to real
 * nav screens: same chrome ([SettingsSubScreen]) and immediate-apply behavior (no Save/Discard) as
 * its sibling [SettingsWidgetRowEditScreen], which already established that pattern for widget rows.
 * A draft+Save/Discard model was deliberately not introduced here — every other settings screen in
 * the app, including the one editing the exact same [com.bisc.flauncher.data.model.PageRow] entity
 * for widget rows, applies changes immediately, and this screen diverging alone would be
 * inconsistent rather than a genuine improvement.
 */
@Composable
fun SettingsAppsRowEditScreen(
    pageId: Long,
    rowId: Long,
    navController: NavController,
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val appEntriesByRow by viewModel.appEntriesByRow.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val folderItemsByFolder by viewModel.folderItemsByFolder.collectAsState()
    val savedMapTargets by viewModel.savedMapTargets.collectAsState()
    val pinnedWebsites by viewModel.pinnedWebsites.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val shortcutAppPickerOpen by viewModel.shortcutAppPickerOpen.collectAsState()
    val shortcutPickerForApp by viewModel.shortcutPickerForApp.collectAsState()
    val availableShortcuts by viewModel.availableShortcuts.collectAsState()

    val row = rowsByPage[pageId]?.find { it.id == rowId }
    // Only auto-navigate back once we've definitely seen this row exist and then watched it
    // disappear (e.g. removed from another screen) — reacting to a transient null on the very
    // first composition, before rowsByPage has had a chance to settle for this exact row, would
    // pop the screen before its real content ever gets a chance to render.
    var everSawRow by remember { mutableStateOf(false) }
    LaunchedEffect(row) {
        if (row != null) everSawRow = true
        else if (everSawRow) onBack()
    }
    if (row == null) return
    val entries = appEntriesByRow[row.id].orEmpty()

    var showAppPicker by remember { mutableStateOf(false) }
    var showFolderCreator by remember { mutableStateOf(false) }
    var showPagePicker by remember { mutableStateOf(false) }
    var showMapTargetPicker by remember { mutableStateOf(false) }
    var showWebsitePicker by remember { mutableStateOf(false) }
    var showAddMapTargetDialog by remember { mutableStateOf(false) }
    var showAddWebsiteDialog by remember { mutableStateOf(false) }
    var pendingShortcut by remember { mutableStateOf(false) }
    var confirmingRemoveRow by remember { mutableStateOf(false) }

    // The dock row (PageRowType.SEARCH_BAR) always floats as a single-line pill regardless of its
    // own pillMode column — that flag only matters for an ordinary APPS row opting in. Showing the
    // toggle (off) and the irrelevant Columns stepper for the dock would be actively misleading.
    val isDock = row.type == PageRowType.SEARCH_BAR
    val isPillLayout = row.pillMode || isDock
    // The home page's own dock row is permanent — same protection the row used to get from the old,
    // now-merged SearchBarRowEditSheet.
    val canRemoveRow = row.type != PageRowType.SEARCH_BAR || pages.find { it.id == pageId }?.isHome != true

    SettingsSubScreen(title = stringResource(R.string.settings_apps_row_edit_title), navController = navController, onBack = onBack) {
        if (isDock) {
            val visibilityMode = row.dockVisibilityMode
            SettingsPreferenceGroup(title = stringResource(R.string.settings_dock_visibility_label)) {
                com.bisc.flauncher.data.model.DockVisibilityMode.entries.forEach { mode ->
                    RadioPreference(dockVisibilityModeLabel(mode), mode, visibilityMode, { viewModel.setRowDockVisibilityMode(row, it) })
                }
            }
        }

        if (!isDock) {
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_apps_row_pill_mode_label),
                    summary = stringResource(R.string.settings_apps_row_pill_mode_summary),
                    onClick = { viewModel.setRowPillMode(row, !row.pillMode) },
                    trailing = { Switch(checked = row.pillMode, onCheckedChange = { viewModel.setRowPillMode(row, it) }) }
                )
            }
        }

        if (!isPillLayout) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_apps_row_columns_label)) {
                SettingsPreference(
                    icon = null,
                    title = row.columns.toString(),
                    onClick = {},
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { viewModel.setRowColumns(row, row.columns - 1) },
                                enabled = row.columns > GridConstants.MIN_APPS_ROW_COLUMNS
                            ) { Icon(Icons.Filled.Remove, contentDescription = null) }
                            IconButton(
                                onClick = { viewModel.setRowColumns(row, row.columns + 1) },
                                enabled = row.columns < GridConstants.MAX_APPS_ROW_COLUMNS
                            ) { Icon(Icons.Filled.Add, contentDescription = null) }
                        }
                    }
                )
            }
        }

        ElementAppearanceSection(
            appearance = row.appearance(),
            onAppearanceChange = { viewModel.setRowAppearance(row, it) },
            styleGroupTitle = stringResource(R.string.settings_dock_background_label)
        )

        SettingsPreferenceGroup(title = stringResource(R.string.settings_apps_row_apps_label)) {
            if (entries.isEmpty()) {
                Text(
                    stringResource(R.string.settings_apps_row_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                ReorderableList(items = entries, key = { it.id }, onReorder = viewModel::reorderAppRowEntries) { entry ->
                    AppRowEntryRow(
                        entry = entry,
                        folders = folders,
                        folderItemsByFolder = folderItemsByFolder,
                        savedMapTargets = savedMapTargets,
                        pinnedWebsites = pinnedWebsites,
                        pages = allPages,
                        loadIcon = viewModel::loadIcon,
                        resolveLabel = viewModel::resolveLabel,
                        onRemove = { viewModel.removeAppRowEntry(entry) }
                    )
                }
            }
        }

        SettingsPreferenceGroup {
            SettingsPreference(icon = Icons.Filled.Add, title = stringResource(R.string.settings_apps_row_add_app), onClick = { showAppPicker = true }, trailing = null)
            SettingsPreference(icon = Icons.Filled.CreateNewFolder, title = stringResource(R.string.settings_apps_row_add_folder), onClick = { showFolderCreator = true }, trailing = null)
            SettingsPreference(
                icon = Icons.AutoMirrored.Filled.Shortcut,
                title = stringResource(R.string.settings_apps_row_add_shortcut),
                onClick = { pendingShortcut = true; viewModel.openShortcutAppPicker() },
                trailing = null
            )
            SettingsPreference(icon = Icons.Filled.Place, title = stringResource(R.string.settings_apps_row_add_map_target), onClick = { showMapTargetPicker = true }, trailing = null)
            SettingsPreference(icon = Icons.Filled.Language, title = stringResource(R.string.settings_apps_row_add_website), onClick = { showWebsitePicker = true }, trailing = null)
            SettingsPreference(icon = Icons.Filled.ViewCarousel, title = stringResource(R.string.settings_apps_row_add_page_link), onClick = { showPagePicker = true }, trailing = null)
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
            onCreate = { name -> viewModel.createFolderInRow(row.id, name); showFolderCreator = false },
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
