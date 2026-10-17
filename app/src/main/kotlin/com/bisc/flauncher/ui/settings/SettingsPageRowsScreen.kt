package com.bisc.flauncher.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.ClockAlignment
import com.bisc.flauncher.data.model.ClockStyle
import com.bisc.flauncher.data.model.ClockTimeFormat
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.WidgetCornerStyle
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.SystemWidgetPickerSheet
import com.bisc.flauncher.ui.home.WidgetSizeEditor
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/** One page's rows — widget rows and apps rows mixed freely in one drag-reorder list (see [PageRow]'s doc comment). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageRowsScreen(
    pageId: Long,
    navController: NavController,
    onBack: () -> Unit,
    onOpenRow: (Long) -> Unit,
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
    val pages by viewModel.pages.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val systemWidgetPickerOpenForPage by viewModel.systemWidgetPickerOpenForPage.collectAsState()
    val pendingBindIntent by viewModel.pendingBindIntent.collectAsState()
    val pendingConfigureIntent by viewModel.pendingConfigureIntent.collectAsState()
    val widgetRelocationWarningCount by viewModel.widgetRelocationWarningCount.collectAsState()
    val pendingOverflowAdd by viewModel.pendingOverflowAdd.collectAsState()

    val rows = rowsByPage[pageId].orEmpty()
    val page = pages.find { it.id == pageId }
    val pageTitle = page?.let { pageLabel(it, pages) } ?: stringResource(R.string.settings_rows_title)

    var editingRowId by remember { mutableStateOf<Long?>(null) }
    val editingRow = editingRowId?.let { id -> rows.find { it.id == id } }
    var showAddSheet by remember { mutableStateOf(false) }
    var showBackgroundSheet by remember { mutableStateOf(false) }

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

    val pageHideStatusBar by remember(pageId) { viewModel.pageHideStatusBar(pageId) }.collectAsState(initial = false)

    SettingsSubScreen(title = pageTitle, navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = surfaceLabel(R.string.settings_layer_surface_screen),
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
        run {
            SettingsPreferenceGroup {
                if (rows.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_rows_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    ReorderableList(
                        items = rows,
                        key = { it.id },
                        onReorder = { reordered -> viewModel.reorderRows(reordered) }
                    ) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                when (row.type) {
                                    PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> onOpenRow(row.id)
                                    PageRowType.APPS, PageRowType.SEARCH_BAR -> editingRowId = row.id
                                    PageRowType.PANEL -> Unit
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
            }
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = Icons.Filled.Add,
                    title = stringResource(R.string.settings_rows_add),
                    onClick = { showAddSheet = true },
                    trailing = null
                )
            }
        }
    }

    if (showBackgroundSheet) {
        val pageBackgroundOverride by remember(pageId) { viewModel.pageBackgroundOverride(pageId) }.collectAsState(initial = null)
        val globalBackground by viewModel.globalBackground.collectAsState()
        SettingsEditPage(title = surfaceLabel(R.string.settings_layer_surface_screen), onDismiss = { showBackgroundSheet = false }) {
            BackgroundOverrideSection(
                override = pageBackgroundOverride,
                global = globalBackground,
                onUseGlobal = { viewModel.clearPageBackgroundConfig(pageId) },
                onCustomColor = { viewModel.setPageBackgroundConfig(pageId, it) }
            )
        }
    }

    if (showAddSheet) {
        AddRowSheet(
            showSearchBarOption = rows.none { it.type == PageRowType.SEARCH_BAR },
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
            onSearchBarSelected = {
                showAddSheet = false
                viewModel.addSearchBarRow(pageId)
            },
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

    if (pendingOverflowAdd != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelPendingOverflowAdd,
            title = { Text(stringResource(R.string.settings_page_full_title)) },
            text = { Text(stringResource(R.string.settings_page_full_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmPendingOverflowAdd) { Text(stringResource(R.string.settings_page_full_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelPendingOverflowAdd) { Text(stringResource(R.string.widget_notes_cancel)) }
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
            PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET, PageRowType.PANEL -> Unit
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
                // The home page's own dock row is permanent — same protection the row used to get
                // from the old, now-merged SearchBarRowEditSheet.
                canRemoveRow = row.type != PageRowType.SEARCH_BAR || pages.find { it.id == pageId }?.isHome != true,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddRowSheet(
    showSearchBarOption: Boolean,
    onWidgetKindSelected: (String) -> Unit,
    onMoreWidgetsSelected: () -> Unit,
    onAppsRowSelected: () -> Unit,
    onSearchBarSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(WidgetKind.MUSIC, WidgetKind.WEATHER, WidgetKind.NOTES, WidgetKind.SPACER).forEach { kind ->
                SettingsPreference(icon = widgetKindIcon(kind), title = widgetKindLabel(kind), onClick = { onWidgetKindSelected(kind) }, trailing = null)
            }
            SettingsPreference(icon = Icons.Filled.Widgets, title = stringResource(R.string.picker_widget_more), onClick = onMoreWidgetsSelected, trailing = null)
            SettingsPreference(icon = Icons.Filled.Apps, title = stringResource(R.string.settings_rows_add_apps_row), onClick = onAppsRowSelected, trailing = null)
            if (showSearchBarOption) {
                SettingsPreference(icon = Icons.Filled.Search, title = stringResource(R.string.settings_rows_add_search_bar), onClick = onSearchBarSelected, trailing = null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetSizeSheet(
    row: PageRow,
    spanY: Int,
    minSpan: Int,
    maxSpan: Int,
    onSpanChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.settings_rows_size_title), style = MaterialTheme.typography.titleLarge)
            WidgetSizeEditor(row = row.copy(spanY = spanY), minSpan = minSpan, maxSpan = maxSpan, onSpanChange = onSpanChange)
            TextButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_rows_save))
            }
        }
    }
}

/** One [WidgetCornerStyle] radio row, with a small swatch previewing the shape itself. */
@Composable
fun CornerStylePreference(value: WidgetCornerStyle, selected: WidgetCornerStyle, onSelect: (WidgetCornerStyle) -> Unit) {
    val label = when (value) {
        WidgetCornerStyle.SQUARE -> stringResource(R.string.settings_widgets_corner_shape_square)
        WidgetCornerStyle.ROUNDED -> stringResource(R.string.settings_widgets_corner_shape_rounded)
    }
    val shape = when (value) {
        WidgetCornerStyle.SQUARE -> RoundedCornerShape(0.dp)
        WidgetCornerStyle.ROUNDED -> RoundedCornerShape(6.dp)
    }
    SettingsPreference(
        icon = null,
        title = label,
        onClick = { onSelect(value) },
        leading = { Box(modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.primary, shape)) },
        trailing = { RadioButton(selected = value == selected, onClick = { onSelect(value) }) }
    )
}

internal fun rowIcon(row: PageRow): ImageVector = when (row.type) {
    PageRowType.WIDGET -> widgetKindIcon(row.payload)
    PageRowType.SYSTEM_WIDGET -> Icons.Filled.Widgets
    PageRowType.APPS -> Icons.Filled.Apps
    PageRowType.SEARCH_BAR -> Icons.Filled.Search
    PageRowType.PANEL -> Icons.Filled.Dashboard
}

@Composable
internal fun rowLabel(row: PageRow): String = when (row.type) {
    PageRowType.WIDGET -> widgetKindLabel(row.payload)
    PageRowType.SYSTEM_WIDGET -> stringResource(R.string.picker_widget_more)
    PageRowType.APPS -> stringResource(R.string.settings_rows_add_apps_row)
    PageRowType.SEARCH_BAR -> stringResource(R.string.settings_rows_add_search_bar)
    PageRowType.PANEL -> stringResource(R.string.settings_home_panel_label)
}

private fun widgetKindIcon(kind: String): ImageVector = when (kind) {
    WidgetKind.CLOCK -> Icons.Filled.Schedule
    WidgetKind.MUSIC -> Icons.Filled.MusicNote
    WidgetKind.WEATHER -> Icons.Filled.Cloud
    WidgetKind.NOTES -> Icons.Filled.EditNote
    WidgetKind.SPACER -> Icons.Filled.HorizontalRule
    else -> Icons.Filled.Schedule
}

@Composable
internal fun clockAlignmentLabel(alignment: String): String = when (alignment) {
    ClockAlignment.START -> stringResource(R.string.settings_clock_alignment_start)
    ClockAlignment.END -> stringResource(R.string.settings_clock_alignment_end)
    else -> stringResource(R.string.settings_clock_alignment_center)
}

@Composable
internal fun clockStyleLabel(style: String): String = when (style) {
    ClockStyle.DIGITAL -> stringResource(R.string.settings_clock_style_digital)
    ClockStyle.DIGITAL_LARGE -> stringResource(R.string.settings_clock_style_digital_large)
    ClockStyle.STACKED -> stringResource(R.string.settings_clock_style_stacked)
    ClockStyle.ANALOG -> stringResource(R.string.settings_clock_style_analog)
    ClockStyle.ORBIT -> stringResource(R.string.settings_clock_style_orbit)
    ClockStyle.BINARY -> stringResource(R.string.settings_clock_style_binary)
    else -> style
}

@Composable
internal fun clockTimeFormatLabel(format: String): String = when (format) {
    ClockTimeFormat.SYSTEM -> stringResource(R.string.settings_clock_time_format_system)
    ClockTimeFormat.H24 -> stringResource(R.string.settings_clock_time_format_24h)
    ClockTimeFormat.H12 -> stringResource(R.string.settings_clock_time_format_12h)
    else -> format
}

@Composable
private fun widgetKindLabel(kind: String): String = when (kind) {
    WidgetKind.CLOCK -> stringResource(R.string.picker_widget_clock)
    WidgetKind.MUSIC -> stringResource(R.string.picker_widget_music)
    WidgetKind.WEATHER -> stringResource(R.string.picker_widget_weather)
    WidgetKind.NOTES -> stringResource(R.string.picker_widget_notes)
    WidgetKind.SPACER -> stringResource(R.string.picker_widget_spacer)
    else -> kind
}
