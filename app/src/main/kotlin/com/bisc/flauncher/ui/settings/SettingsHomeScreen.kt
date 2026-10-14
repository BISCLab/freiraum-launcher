package com.bisc.flauncher.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Palette
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
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.WidgetKind
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

/** The Home page's own background (Layer 1) and status-bar switch — same two controls every other
 *  page's row screen has at its top, just promoted to their own screen here. */
@Composable
fun SettingsHomeAppearanceScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val pageHideStatusBar by remember(pageId) { viewModel.pageHideStatusBar(pageId) }.collectAsState(initial = false)
    var showBackgroundSheet by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_home_appearance_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = layerBackgroundLabel(1, R.string.settings_layer_surface_screen),
                onClick = { showBackgroundSheet = true },
                trailing = null
            )
        }
        SettingsPreferenceGroup {
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
        SettingsEditPage(title = layerBackgroundLabel(1, R.string.settings_layer_surface_screen), onDismiss = { showBackgroundSheet = false }) {
            BackgroundOverrideSection(
                override = pageBackgroundOverride,
                global = globalBackground,
                onUseGlobal = { viewModel.clearPageBackgroundConfig(pageId) },
                onCustomColor = { viewModel.setPageBackgroundConfig(pageId, it) }
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
    val homeScreenWidgetsEnabled by viewModel.homeScreenWidgetsEnabled.collectAsState()
    val homePanelEnabled by viewModel.homePanelEnabled.collectAsState()

    val rows = rowsByPage[pageId].orEmpty()
    val dockRow = rows.find { it.type == PageRowType.SEARCH_BAR }
    val otherRows = rows.filter { it.type != PageRowType.SEARCH_BAR }

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
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_homescreen_widgets_enabled),
                info = stringResource(R.string.settings_homescreen_widgets_enabled_summary),
                onClick = { viewModel.setHomeScreenWidgetsEnabled(!homeScreenWidgetsEnabled) },
                trailing = { Switch(checked = homeScreenWidgetsEnabled, onCheckedChange = { viewModel.setHomeScreenWidgetsEnabled(it) }) }
            )
        }
        SettingsPreferenceGroup {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenPanel() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.settings_home_panel_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = homePanelEnabled, onCheckedChange = { viewModel.setHomePanelEnabled(it) })
            }
            if (otherRows.isEmpty()) {
                Text(
                    stringResource(R.string.settings_rows_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                ReorderableList(
                    items = otherRows,
                    key = { it.id },
                    // The Dock row is excluded from this list (fixed as its own bookend below) but
                    // must still be re-persisted alongside it — reorderRows renumbers orderIndex
                    // 0..n-1 over whatever list it's handed, and HomeScreen's own pager rendering
                    // assumes the dock row is always the *last* one by orderIndex (see its
                    // pinnedSearchBarRow lookup) — appending it here keeps that true.
                    onReorder = { reordered -> viewModel.reorderRows(reordered + listOfNotNull(dockRow)) }
                ) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            when (row.type) {
                                PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> onOpenRow(row.id)
                                PageRowType.APPS, PageRowType.SEARCH_BAR -> editingRowId = row.id
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = rowIcon(row), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(rowLabel(row), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            if (dockRow != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { editingRowId = dockRow.id }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(imageVector = rowIcon(dockRow), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(rowLabel(dockRow), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(checked = dockRow.enabled, onCheckedChange = { viewModel.setRowEnabled(dockRow, it) })
                }
            }
        }
        SettingsPreferenceGroup {
            SettingsPreference(icon = Icons.Filled.Add, title = stringResource(R.string.settings_rows_add), onClick = { showAddSheet = true }, trailing = null)
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

    editingRow?.let { row ->
        when (row.type) {
            PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> Unit
            PageRowType.APPS, PageRowType.SEARCH_BAR -> AppsRowEditSheet(
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
                onDockVisibilityModeChange = { mode -> viewModel.setRowDockVisibilityMode(row, mode) },
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
                // The dock bookend is never removable from here — switched off instead (see the
                // inline Switch next to it above).
                canRemoveRow = row.type != PageRowType.SEARCH_BAR,
                onRemoveRow = { viewModel.removeRow(row); editingRowId = null },
                onDismiss = { editingRowId = null }
            )
        }
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

/** The Favorites "Panel"'s own settings — border + dividers moved here from the old flat Global
 *  settings screen (see [SettingsAppearanceScreen]); the enable switch itself lives inline in
 *  [SettingsHomeLayoutScreen]'s list, not here, matching the Dock bookend's own split. */
@Composable
fun SettingsHomePanelScreen(navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val favoritesBottomBorderEnabled by viewModel.favoritesBottomBorderEnabled.collectAsState()
    val favoritesGroupDividersEnabled by viewModel.favoritesGroupDividersEnabled.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_home_panel_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_favorites_bottom_border),
                info = stringResource(R.string.settings_favorites_bottom_border_summary),
                onClick = { viewModel.setFavoritesBottomBorderEnabled(!favoritesBottomBorderEnabled) },
                trailing = { Switch(checked = favoritesBottomBorderEnabled, onCheckedChange = { viewModel.setFavoritesBottomBorderEnabled(it) }) }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_favorites_group_dividers),
                info = stringResource(R.string.settings_favorites_group_dividers_summary),
                onClick = { viewModel.setFavoritesGroupDividersEnabled(!favoritesGroupDividersEnabled) },
                trailing = { Switch(checked = favoritesGroupDividersEnabled, onCheckedChange = { viewModel.setFavoritesGroupDividersEnabled(it) }) }
            )
        }
    }
}
