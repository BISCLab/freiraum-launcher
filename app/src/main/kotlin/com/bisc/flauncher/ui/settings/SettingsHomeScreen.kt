package com.bisc.flauncher.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.bisc.flauncher.data.model.AppRowEntry
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.panelRows
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.SystemWidgetPickerSheet
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/** Entry point for the Home page's own settings — split into [SettingsHomeAppearanceScreen] (the
 *  page's background/status-bar) and [SettingsHomeLayoutScreen] (what's actually placed on it),
 *  instead of one long flat screen like every other page still uses (see [SettingsPageRowsScreen]). */
@Composable
fun SettingsHomeScreenHub(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    SettingsSubScreen(title = stringResource(R.string.settings_pages_home), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.settings_home_appearance_title),
                summary = stringResource(R.string.settings_home_appearance_subtitle),
                onClick = { onNavigate("appearance") },
                trailing = null
            )
            SettingsPreference(
                icon = Icons.Filled.ViewAgenda,
                title = stringResource(R.string.settings_home_layout_title),
                summary = stringResource(R.string.settings_home_layout_subtitle),
                onClick = { onNavigate("layout") },
                trailing = null
            )
        }
    }
}

/** The Home page's own background and status-bar switch — just "Background" now (not the old
 *  "Layer 1 · Screen" wording, which leaked internal Layer-model terminology into the UI). "Hide
 *  status bar" sits as its own row right below Background on this hub page — not nested inside
 *  the Background detail page, which is one level further in than it belongs. */
@Composable
fun SettingsHomeAppearanceScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val pageHideStatusBar by remember(pageId) { viewModel.pageHideStatusBar(pageId) }.collectAsState(initial = false)
    var showBackgroundSheet by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_home_appearance_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_section_global_background),
                onClick = { showBackgroundSheet = true },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_statusbar_hide_page),
                onClick = { viewModel.setPageHideStatusBar(pageId, !pageHideStatusBar) },
                trailing = { Switch(checked = pageHideStatusBar, onCheckedChange = { viewModel.setPageHideStatusBar(pageId, it) }) }
            )
        }
    }

    if (showBackgroundSheet) {
        val pageBackgroundOverride by remember(pageId) { viewModel.pageBackgroundOverride(pageId) }.collectAsState(initial = null)
        val globalBackground by viewModel.globalBackground.collectAsState()
        SettingsEditPage(title = stringResource(R.string.settings_section_global_background), onDismiss = { showBackgroundSheet = false }) {
            BackgroundOverrideSection(
                override = pageBackgroundOverride,
                global = globalBackground,
                onUseGlobal = { viewModel.clearPageBackgroundConfig(pageId) },
                onCustomColor = { viewModel.setPageBackgroundConfig(pageId, it) },
                // The page's own title already says "Background" — repeating a "Layer 1 · Screen"
                // caption (plus a help icon explaining the old Layer 1/2/3 model) inside it too is
                // redundant clutter, not extra clarity.
                showLayerHeader = false
            )
        }
    }
}

/** What's actually placed on Home: the Favorites "Panel" and the "Dock" are drawn as fixed
 *  bookends — always first/last, never draggable, but switchable off without losing their own
 *  configuration (see [PageRow.enabled] for the Dock's own case; the Panel has no row of its own,
 *  so it's a plain [SettingsRepository] flag instead) — with ordinary widget/app rows reorderable
 *  between them exactly like [SettingsPageRowsScreen]'s own flat list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeLayoutScreen(
    pageId: Long,
    navController: NavController,
    onBack: () -> Unit,
    onOpenRow: (Long) -> Unit,
    onOpenPanel: () -> Unit,
    onOpenDock: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val appEntriesByRow by viewModel.appEntriesByRow.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val folderItemsByFolder by viewModel.folderItemsByFolder.collectAsState()
    val savedMapTargets by viewModel.savedMapTargets.collectAsState()
    val pinnedWebsites by viewModel.pinnedWebsites.collectAsState()
    val shortcutAppPickerOpen by viewModel.shortcutAppPickerOpen.collectAsState()
    val shortcutPickerForApp by viewModel.shortcutPickerForApp.collectAsState()
    val availableShortcuts by viewModel.availableShortcuts.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val systemWidgetPickerOpenForPage by viewModel.systemWidgetPickerOpenForPage.collectAsState()
    val pendingBindIntent by viewModel.pendingBindIntent.collectAsState()
    val pendingConfigureIntent by viewModel.pendingConfigureIntent.collectAsState()
    val widgetRelocationWarningCount by viewModel.widgetRelocationWarningCount.collectAsState()
    val rows = rowsByPage[pageId].orEmpty()
    val dockRow = rows.find { it.type == PageRowType.SEARCH_BAR }
    val panelRow = rows.find { it.type == PageRowType.PANEL }
    val otherRows = rows.filter { it.type != PageRowType.SEARCH_BAR && it.type != PageRowType.PANEL }

    var editingRowId by remember { mutableStateOf<Long?>(null) }
    val editingRow = editingRowId?.let { id -> rows.find { it.id == id } }
    var showAddSheet by remember { mutableStateOf(false) }

    var pendingShortcutRowId by remember { mutableStateOf<Long?>(null) }
    var showMapTargetPickerForRow by remember { mutableStateOf<Long?>(null) }
    var showWebsitePickerForRow by remember { mutableStateOf<Long?>(null) }
    var showAddMapTargetDialogForRow by remember { mutableStateOf<Long?>(null) }
    var showAddWebsiteDialogForRow by remember { mutableStateOf<Long?>(null) }
    var sizingKind by remember { mutableStateOf<String?>(null) }
    var sizingSpan by remember { mutableStateOf(1) }

    val bindWidgetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.onSystemWidgetBindResult(result.resultCode == Activity.RESULT_OK) }
    val configureWidgetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.onSystemWidgetConfigureResult(result.resultCode == Activity.RESULT_OK) }
    LaunchedEffect(pendingBindIntent) { pendingBindIntent?.let { bindWidgetLauncher.launch(it) } }
    LaunchedEffect(pendingConfigureIntent) { pendingConfigureIntent?.let { configureWidgetLauncher.launch(it) } }

    SettingsSubScreen(title = stringResource(R.string.settings_home_layout_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { onOpenPanel() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.settings_home_panel_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (panelRow != null) {
                    Switch(checked = panelRow.enabled, onCheckedChange = { viewModel.setRowEnabled(panelRow, it) })
                }
            }
            if (otherRows.isNotEmpty()) {
                ReorderableList(
                    items = otherRows,
                    key = { it.id },
                    // The Dock and Panel rows are excluded from this list (fixed as their own bookends
                    // above/below) but must still be re-persisted alongside it — reorderRows renumbers
                    // orderIndex 0..n-1 over whatever list it's handed.
                    onReorder = { reordered -> viewModel.reorderRows(reordered + listOfNotNull(dockRow, panelRow)) }
                ) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable {
                                when (row.type) {
                                    PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> onOpenRow(row.id)
                                    PageRowType.APPS, PageRowType.SEARCH_BAR -> editingRowId = row.id
                                    PageRowType.PANEL -> Unit
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = rowIcon(row), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(rowLabel(row), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            // Replaces the old standalone "Add row" card below the whole group (and the old
            // "No rows on this page" empty-state text) — one connected list from Panel to Dock,
            // with the add action sitting right where new rows actually land.
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { showAddSheet = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.settings_rows_add), style = MaterialTheme.typography.titleMedium)
            }
            if (dockRow != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { onOpenDock() }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.settings_home_dock_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(checked = dockRow.enabled, onCheckedChange = { viewModel.setRowEnabled(dockRow, it) })
                }
            }
        }
    }

    if (showAddSheet) {
        AddRowSheet(
            // The dock always exists on Home now (see PageContentRepository.ensureSeeded) and is
            // switched off instead of removed/re-added — never offered here.
            showSearchBarOption = false,
            onWidgetKindSelected = { kind ->
                showAddSheet = false
                sizingSpan = WidgetKind.defaultSpan(kind)
                sizingKind = kind
            },
            onMoreWidgetsSelected = {
                showAddSheet = false
                viewModel.openSystemWidgetPicker(pageId)
            },
            onAppsRowSelected = {
                showAddSheet = false
                viewModel.addAppsRow(pageId)
            },
            onSearchBarSelected = {},
            onDismiss = { showAddSheet = false }
        )
    }

    if (widgetRelocationWarningCount > 0) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissWidgetRelocationWarning() },
            title = { Text(stringResource(R.string.settings_widget_relocated_title)) },
            text = { Text(stringResource(R.string.settings_widget_relocated_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissWidgetRelocationWarning() }) { Text(stringResource(R.string.settings_widget_relocated_confirm)) }
            }
        )
    }

    sizingKind?.let { kind ->
        WidgetSizeSheet(
            row = PageRow(pageId = pageId, orderIndex = 0, type = PageRowType.WIDGET, payload = kind, spanY = sizingSpan),
            spanY = sizingSpan,
            minSpan = WidgetKind.minSpan(kind),
            maxSpan = WidgetKind.maxSpan(kind),
            onSpanChange = { sizingSpan = it },
            onSave = {
                viewModel.addWidgetRow(pageId, kind, sizingSpan)
                sizingKind = null
            },
            onDismiss = { sizingKind = null }
        )
    }

    systemWidgetPickerOpenForPage?.let { targetPageId ->
        val providers = remember(targetPageId) { viewModel.installedSystemWidgetProviders() }
        SystemWidgetPickerSheet(
            providers = providers,
            onProviderSelected = { provider -> viewModel.startAddSystemWidget(targetPageId, provider) },
            onDismiss = viewModel::dismissSystemWidgetPicker
        )
    }

    // Only ordinary Apps rows reach this sheet now — the Dock bookend opens its own full-page hub
    // instead (see onOpenDock/SettingsDockScreen).
    editingRow?.takeIf { it.type == PageRowType.APPS }?.let { row ->
        AppsRowEditSheet(
            row = row,
            entries = appEntriesByRow[row.id].orEmpty(),
            installedApps = browsableApps,
            folders = folders,
            folderItemsByFolder = folderItemsByFolder,
            savedMapTargets = savedMapTargets,
            pinnedWebsites = pinnedWebsites,
            pages = allPages,
            loadIcon = viewModel::loadIcon,
            resolveLabel = viewModel::resolveLabel,
            onColumnsChange = { viewModel.setRowColumns(row, it) },
            onAppearanceChange = { appearance -> viewModel.setRowAppearance(row, appearance) },
            onPillModeChange = { enabled -> viewModel.setRowPillMode(row, enabled) },
            onAddApp = { app -> viewModel.addAppEntryToRow(row.id, app) },
            onAddExistingFolder = { folderId -> viewModel.addExistingFolderToRow(row.id, folderId) },
            onCreateFolder = { name -> viewModel.createFolderInRow(row.id, name) },
            onAddShortcutClick = {
                pendingShortcutRowId = row.id
                viewModel.openShortcutAppPicker()
            },
            onAddMapTargetClick = { showMapTargetPickerForRow = row.id },
            onAddWebsiteClick = { showWebsitePickerForRow = row.id },
            onAddPageLink = { page -> viewModel.addPageLinkEntryToRow(row.id, page.id) },
            onRemoveEntry = viewModel::removeAppRowEntry,
            onReorderEntries = viewModel::reorderAppRowEntries,
            onRemoveRow = { viewModel.removeRow(row); editingRowId = null },
            onDismiss = { editingRowId = null }
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
                pendingShortcutRowId?.let { rowId -> viewModel.addShortcutEntryToRow(rowId, info) }
                pendingShortcutRowId = null
            },
            onDismiss = {
                pendingShortcutRowId = null
                viewModel.dismissShortcutPicker()
            }
        )
    }

    showMapTargetPickerForRow?.let { rowId ->
        SavedMapTargetPickerSheet(
            targets = savedMapTargets,
            onSelect = { target ->
                viewModel.addMapTargetEntryToRow(rowId, target.id)
                showMapTargetPickerForRow = null
            },
            onAddNew = {
                showMapTargetPickerForRow = null
                showAddMapTargetDialogForRow = rowId
            },
            onDismiss = { showMapTargetPickerForRow = null }
        )
    }

    showWebsitePickerForRow?.let { rowId ->
        SavedWebsitePickerSheet(
            websites = pinnedWebsites,
            onSelect = { website ->
                viewModel.addWebsiteEntryToRow(rowId, website.id)
                showWebsitePickerForRow = null
            },
            onAddNew = {
                showWebsitePickerForRow = null
                showAddWebsiteDialogForRow = rowId
            },
            onDismiss = { showWebsitePickerForRow = null }
        )
    }

    showAddMapTargetDialogForRow?.let { rowId ->
        AddMapTargetDialog(
            onSave = { label, lat, lon, address ->
                viewModel.addNewMapTargetToRow(rowId, label, lat, lon, address)
                showAddMapTargetDialogForRow = null
            },
            onDismiss = { showAddMapTargetDialogForRow = null }
        )
    }

    showAddWebsiteDialogForRow?.let { rowId ->
        AddWebsiteDialog(
            onSave = { label, url ->
                viewModel.addNewWebsiteToRow(rowId, label, url)
                showAddWebsiteDialogForRow = null
            },
            onDismiss = { showAddWebsiteDialogForRow = null }
        )
    }
}

/** The Favorites "Panel"'s own settings hub — Background/Border/Elements, same shape as the Dock's
 *  own hub; the enable switch itself lives inline in [SettingsHomeLayoutScreen]'s list, not here,
 *  matching the Dock bookend's own split. */
@Composable
fun SettingsHomePanelScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    SettingsSubScreen(title = stringResource(R.string.settings_home_panel_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
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

/** Finds the Home page's own pinned Panel row — mirrors [SettingsDockScreen.kt]'s rememberDockRow,
 *  same "always present once seeded, null only means not loaded yet" contract. */
@Composable
private fun rememberPanelRow(pageId: Long, viewModel: HomeViewModel): PageRow? {
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    return rowsByPage[pageId]?.find { it.type == PageRowType.PANEL }
}

@Composable
fun SettingsPanelBackgroundScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val row = rememberPanelRow(pageId, viewModel) ?: return
    SettingsSubScreen(title = stringResource(R.string.settings_section_global_background), navController = navController, onBack = onBack) {
        ElementBackgroundSection(appearance = row.appearance(), onAppearanceChange = { viewModel.setRowAppearance(row, it) })
    }
}

@Composable
fun SettingsPanelBorderScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val row = rememberPanelRow(pageId, viewModel) ?: return
    SettingsSubScreen(title = stringResource(R.string.settings_element_border_label), navController = navController, onBack = onBack) {
        ElementBorderSection(appearance = row.appearance(), onAppearanceChange = { viewModel.setRowAppearance(row, it) })
    }
}

/** The Panel's own grid editor — same "+" preview/chooser idea as [SettingsDockElementsScreen], but
 *  shaped row by row (each sub-row picks its own column count, 1-6) instead of one shared column
 *  count for the whole grid, since the Panel wraps to multiple rows rather than growing/shrinking
 *  width like the dock does — see [com.bisc.flauncher.ui.home.AppsGridEntries]'s rowColumns param
 *  for the centered layout this produces. No separate reorderable entries list underneath either:
 *  tapping a filled slot in the preview itself removes it (see onEditEntry there), since every slot
 *  is already individually picked through the "+" chooser — a second list showing the same content
 *  again underneath was redundant. Adds contacts as a 7th content type (via the same
 *  [AddElementChooserSheet] the Dock now shares). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanelElementsScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val row = rememberPanelRow(pageId, viewModel) ?: return
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
    val rowShape = row.panelRows()

    var showAddChooser by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showPagePicker by remember { mutableStateOf(false) }
    var showMapTargetPicker by remember { mutableStateOf(false) }
    var showWebsitePicker by remember { mutableStateOf(false) }
    var showAddMapTargetDialog by remember { mutableStateOf(false) }
    var showAddWebsiteDialog by remember { mutableStateOf(false) }
    var pendingShortcut by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<AppRowEntry?>(null) }

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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(cols.toString(), modifier = Modifier.padding(end = 4.dp))
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.setPanelRowColumns(row, rowShape.toMutableList().apply { this[index] = cols - 1 }) },
                                enabled = cols > 1
                            ) { Icon(androidx.compose.material.icons.Icons.Filled.Remove, contentDescription = null) }
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.setPanelRowColumns(row, rowShape.toMutableList().apply { this[index] = cols + 1 }) },
                                enabled = cols < 6
                            ) { Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = null) }
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.setPanelRowColumns(row, rowShape.filterIndexed { i, _ -> i != index }) },
                                enabled = rowShape.size > 1
                            ) { Icon(androidx.compose.material.icons.Icons.Filled.Close, contentDescription = null) }
                        }
                    }
                )
            }
            SettingsPreference(
                icon = androidx.compose.material.icons.Icons.Filled.Add,
                title = stringResource(R.string.settings_panel_add_row),
                onClick = { viewModel.setPanelRowColumns(row, rowShape + 4) },
                trailing = null
            )
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
