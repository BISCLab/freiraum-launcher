package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
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
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.dockRows
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.entryLabel
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/** The Dock's own settings — a hub (mirrors the Home page's own Appearance/Layout split) instead
 *  of the old single cluttered "Edit apps row" sheet, which mixed a Visibility picker, background
 *  fill/opacity, border controls and a flat entries list with no clear grouping. */
@Composable
fun SettingsDockScreen(pageId: Long, navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    SettingsSubScreen(title = stringResource(R.string.settings_home_dock_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Visibility,
                title = stringResource(R.string.settings_dock_visibility_label),
                onClick = { onNavigate("visibility") },
                trailing = null
            )
            SettingsPreference(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.settings_section_global_background),
                onClick = { onNavigate("background") },
                trailing = null
            )
            SettingsPreference(
                icon = Icons.Filled.BorderAll,
                title = stringResource(R.string.settings_element_border_label),
                onClick = { onNavigate("border") },
                trailing = null
            )
            SettingsPreference(
                icon = Icons.Filled.GridView,
                title = stringResource(R.string.settings_dock_elements_title),
                onClick = { onNavigate("elements") },
                trailing = null
            )
        }
    }
}

/** Finds the Home page's own pinned dock row, wherever it's needed across the 4 Dock sub-screens —
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

@Composable
fun SettingsDockBackgroundScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val row = rememberDockRow(pageId, viewModel) ?: return
    SettingsSubScreen(title = stringResource(R.string.settings_section_global_background), navController = navController, onBack = onBack) {
        ElementBackgroundSection(appearance = row.appearance(), onAppearanceChange = { viewModel.setRowAppearance(row, it) })
    }
}

@Composable
fun SettingsDockBorderScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val row = rememberDockRow(pageId, viewModel) ?: return
    SettingsSubScreen(title = stringResource(R.string.settings_element_border_label), navController = navController, onBack = onBack) {
        ElementBorderSection(appearance = row.appearance(), onAppearanceChange = { viewModel.setRowAppearance(row, it) })
    }
}

/** The Dock's own grid editor — the Dock is a mirrored Panel, so this mirrors
 *  [SettingsPanelElementsScreen] exactly (same "+" preview/chooser, same tap-a-filled-slot-to-remove
 *  instead of a separate reorderable list), just capped at 2 sub-rows instead of 6. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDockElementsScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val row = rememberDockRow(pageId, viewModel) ?: return
    val appEntriesByRow by viewModel.appEntriesByRow.collectAsState()
    val entries = appEntriesByRow[row.id].orEmpty()
    val browsableApps by viewModel.browsableApps.collectAsState()
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
    val rowShape = row.dockRows()

    var showAddChooser by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showPagePicker by remember { mutableStateOf(false) }
    var showMapTargetPicker by remember { mutableStateOf(false) }
    var showWebsitePicker by remember { mutableStateOf(false) }
    var showAddMapTargetDialog by remember { mutableStateOf(false) }
    var showAddWebsiteDialog by remember { mutableStateOf(false) }
    var pendingShortcut by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<com.bisc.flauncher.data.model.AppRowEntry?>(null) }

    SettingsSubScreen(title = stringResource(R.string.settings_dock_elements_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.bisc.flauncher.ui.home.AppsGridEntries(
                    entries = entries,
                    columns = 1,
                    rowColumns = rowShape,
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
                    showLabels = row.showLabels,
                    labelColor = row.appearance().contentColor?.let { androidx.compose.ui.graphics.Color(it) } ?: androidx.compose.ui.graphics.Color.White,
                    onAddEmptySlot = { showAddChooser = true },
                    onEditEntry = { entry -> pendingRemoval = entry }
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
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(cols.toString(), modifier = Modifier.padding(end = 4.dp))
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.setDockRowColumns(row, rowShape.toMutableList().apply { this[index] = cols - 1 }) },
                                enabled = cols > 1
                            ) { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Remove, contentDescription = null) }
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.setDockRowColumns(row, rowShape.toMutableList().apply { this[index] = cols + 1 }) },
                                enabled = cols < 6
                            ) { androidx.compose.material3.Icon(Icons.Filled.Add, contentDescription = null) }
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.setDockRowColumns(row, rowShape.filterIndexed { i, _ -> i != index }) },
                                enabled = rowShape.size > 1
                            ) { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Close, contentDescription = null) }
                        }
                    }
                )
            }
            if (rowShape.size < 2) {
                SettingsPreference(
                    icon = Icons.Filled.Add,
                    title = stringResource(R.string.settings_panel_add_row),
                    onClick = { viewModel.setDockRowColumns(row, rowShape + 5) },
                    trailing = null
                )
            }
        }
        CaptionsSection(
            showLabels = row.showLabels,
            onShowLabelsChange = { viewModel.setRowShowLabels(row, it) },
            appearance = row.appearance(),
            onAppearanceChange = { viewModel.setRowAppearance(row, it) }
        )
    }

    pendingRemoval?.let { entry ->
        ConfirmRemoveDialog(
            label = com.bisc.flauncher.ui.home.entryLabel(entry, folders, savedMapTargets, pinnedWebsites, allPages, pinnedContacts, viewModel::resolveLabel),
            onConfirm = { viewModel.removeAppRowEntry(entry); pendingRemoval = null },
            onDismiss = { pendingRemoval = null }
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
            onDismiss = { showAddChooser = false }
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
            onCreate = { name -> viewModel.createFolderInRow(row.id, name); showFolderPicker = false },
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

/** Shared "what do you want to add" chooser — the entry point for tapping an empty preview slot
 *  in [SettingsDockElementsScreen] (and, eventually, the same for the Favorites Panel), replacing
 *  what used to be a permanently-visible row of 6 "Add X" buttons with one picker reached by
 *  tapping a "+" the same way the dock's own live empty slots already work. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddElementChooserSheet(
    onAddAppClick: () -> Unit,
    onAddFolderClick: () -> Unit,
    onAddShortcutClick: () -> Unit,
    onAddMapTargetClick: () -> Unit,
    onAddWebsiteClick: () -> Unit,
    onAddPageLinkClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SettingsPreferenceGroup {
            SettingsPreference(icon = Icons.Filled.Add, title = stringResource(R.string.settings_apps_row_add_app), onClick = onAddAppClick, trailing = null)
            SettingsPreference(icon = Icons.Filled.CreateNewFolder, title = stringResource(R.string.settings_apps_row_add_folder), onClick = onAddFolderClick, trailing = null)
            SettingsPreference(icon = Icons.AutoMirrored.Filled.Shortcut, title = stringResource(R.string.settings_apps_row_add_shortcut), onClick = onAddShortcutClick, trailing = null)
            SettingsPreference(icon = Icons.Filled.Place, title = stringResource(R.string.settings_apps_row_add_map_target), onClick = onAddMapTargetClick, trailing = null)
            SettingsPreference(icon = Icons.Filled.Language, title = stringResource(R.string.settings_apps_row_add_website), onClick = onAddWebsiteClick, trailing = null)
            SettingsPreference(icon = Icons.Filled.ViewCarousel, title = stringResource(R.string.settings_apps_row_add_page_link), onClick = onAddPageLinkClick, trailing = null)
            SettingsPreference(icon = Icons.Filled.Person, title = stringResource(R.string.settings_apps_row_add_contact), onClick = onAddContactClick, trailing = null)
        }
    }
}
