package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.bisc.flauncher.data.model.RowEntry
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.data.model.panelRows
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.SystemWidgetPickerSheet
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/** Entry point for the Home page's own settings - split into [SettingsPageAppearanceScreen] (the
 *  page's background/status-bar) and [SettingsHomeLayoutScreen] (what's actually placed on it),
 *  instead of one long flat screen like every other page still uses (see [SettingsPageRowsScreen]). */
@Composable
fun SettingsHomeScreenHub(pageId: Long, navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    SettingsSubScreen(title = stringResource(R.string.settings_pages_home), navController = navController, onBack = onBack) {
        // Tap a part of the miniature (background, Panel, a row, the Dock) to edit it.
        PagePreview(pageId = pageId, isHome = true, navController = navController)
        SettingsTileGrid(
            tiles = listOf(
                SettingsTile(Icons.Filled.Palette, stringResource(R.string.settings_home_appearance_title), stringResource(R.string.settings_home_appearance_subtitle)) { onNavigate("appearance") },
                SettingsTile(Icons.Filled.ViewAgenda, stringResource(R.string.settings_home_layout_title), stringResource(R.string.settings_home_layout_subtitle)) { onNavigate("layout") }
            )
        )
    }
}

/** A carousel page's own Appearance screen (Home and every other page alike): its icon and name,
 *  then its background and status-bar switch - all one card. "Hide status bar" sits right below
 *  Background here, not nested inside the Background detail page, which is one level further in
 *  than it belongs. */
@Composable
fun SettingsPageAppearanceScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val pageHideStatusBar by remember(pageId) { viewModel.pageHideStatusBar(pageId) }.collectAsState(initial = false)
    val pages by viewModel.pages.collectAsState()
    val pageBackgroundOverride by remember(pageId) { viewModel.pageBackgroundOverride(pageId) }.collectAsState(initial = null)
    val pageGlobalShade by remember(pageId) { viewModel.pageGlobalShade(pageId) }.collectAsState(initial = null)
    val globalBackground by viewModel.globalBackground.collectAsState()

    // The same three groups as on the App list, App grid and Text search pages.
    SettingsSubScreen(title = stringResource(R.string.settings_home_appearance_title), navController = navController, onBack = onBack) {
        pages.find { it.id == pageId }?.let { page ->
            SettingsPreferenceGroup {
                PageIdentityRows(
                    page = page,
                    pages = pages,
                    onOpenIcon = { navController.navigate("settings/pages/$pageId/icon") },
                    onRename = { name -> viewModel.renamePage(pageId, name) }
                )
            }
        }
        BackgroundOverrideSection(
            override = pageBackgroundOverride,
            global = globalBackground,
            onUseGlobal = { viewModel.clearPageBackgroundConfig(pageId) },
            onCustomColor = { viewModel.setPageBackgroundConfig(pageId, it) },
            imageKey = "page_$pageId",
            globalShade = pageGlobalShade,
            onGlobalShadeChange = { viewModel.setPageGlobalShade(pageId, it) },
            groupTitle = stringResource(R.string.settings_section_global_background),
            onEditGlobal = { navController.navigate("settings/appearance/background") }
        )
        SettingsPreferenceGroup(title = stringResource(R.string.settings_statusbar_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_statusbar_hide_page),
                onClick = { viewModel.setPageHideStatusBar(pageId, !pageHideStatusBar) },
                trailing = { Switch(checked = pageHideStatusBar, onCheckedChange = { viewModel.setPageHideStatusBar(pageId, it) }) }
            )
        }
    }
}

/** What's actually placed on Home: the Favorites "Panel" and the "Dock" are drawn as fixed
 *  bookends - always first/last, never draggable, but switchable off without losing their own
 *  configuration (see [PageRow.enabled] for the Dock's own case; the Panel has no row of its own,
 *  so it's a plain [SettingsRepository] flag instead) - with ordinary widget/app rows reorderable
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
    val systemWidgetPickerOpenForPage by viewModel.systemWidgetPickerOpenForPage.collectAsState()
    val pendingBindIntent by viewModel.pendingBindIntent.collectAsState()
    val pendingConfigureIntent by viewModel.pendingConfigureIntent.collectAsState()
    val rows = rowsByPage[pageId].orEmpty()
    val dockRow = rows.find { it.type == PageRowType.SEARCH_BAR }
    val panelRow = rows.find { it.type == PageRowType.PANEL }
    val otherRows = rows.filter { it.type != PageRowType.SEARCH_BAR && it.type != PageRowType.PANEL }

    var showAddSheet by remember { mutableStateOf(false) }
    var sizingKind by remember { mutableStateOf<String?>(null) }
    var sizingSpan by remember { androidx.compose.runtime.mutableIntStateOf(1) }

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
                Icon(imageVector = Icons.Filled.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
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
                    // above/below) but must still be re-persisted alongside it - reorderRows renumbers
                    // orderIndex 0..n-1 over whatever list it's handed.
                    onReorder = { reordered -> viewModel.reorderRows(reordered + listOfNotNull(dockRow, panelRow)) },
                    // Inside the Panel…Dock card: square, so it joins the block instead of rounding itself.
                    shape = androidx.compose.ui.graphics.RectangleShape
                ) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable {
                                when (row.type) {
                                    PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> onOpenRow(row.id)
                                    PageRowType.ELEMENT_BOX, PageRowType.SEARCH_BAR -> navController.navigate("settings/pages/$pageId/rows/${row.id}/elementbox")
                                    PageRowType.PANEL -> Unit
                                }
                            }
                            // Horizontal inset comes from the list row itself - lined up with Panel and Dock.
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = rowIcon(row), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Text(rowLabel(row), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            // One connected list from Panel to Dock, with the add action right where new rows land.
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
                    Icon(imageVector = Icons.Filled.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.settings_home_dock_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(checked = dockRow.enabled, onCheckedChange = { viewModel.setRowEnabled(dockRow, it) })
                }
            }
        }
    }

    if (showAddSheet) {
        AddRowSheet(
            onWidgetKindSelected = { kind ->
                showAddSheet = false
                sizingSpan = WidgetKind.defaultSpan(kind)
                sizingKind = kind
            },
            onMoreWidgetsSelected = {
                showAddSheet = false
                viewModel.openSystemWidgetPicker(pageId)
            },
            onElementBoxSelected = {
                showAddSheet = false
                viewModel.addElementBox(pageId)
            },
            onDismiss = { showAddSheet = false }
        )
    }

    sizingKind?.let { kind ->
        WidgetSizeSheet(
            row = newWidgetPreviewRow(pageId, kind, sizingSpan),
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

}

/** The Favorites "Panel"'s own settings hub - Background/Border/Elements, same shape as the Dock's
 *  own hub; the enable switch itself lives inline in [SettingsHomeLayoutScreen]'s list, not here,
 *  matching the Dock bookend's own split. */
@Composable
fun SettingsHomePanelScreen(pageId: Long, navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val row = rememberPanelRow(pageId, viewModel) ?: return
    val entriesByRow by viewModel.rowEntriesByRow.collectAsState()
    val widgets by viewModel.panelWidgets.collectAsState()
    val count = entriesByRow[row.id].orEmpty().size
    SettingsSubScreen(title = stringResource(R.string.settings_home_panel_label), navController = navController, onBack = onBack) {
        LookSample(LookSampleKind.PANEL, row.appearance(), row.cornerRadiusPercent)
        SettingsTileGrid(
            tiles = listOf(
                SettingsTile(
                    Icons.Filled.GridView,
                    stringResource(R.string.settings_panel_content_title),
                    stringResource(R.string.settings_hub_panel_content_what),
                    badge = androidx.compose.ui.res.pluralStringResource(R.plurals.settings_hub_elements_count, count, count) + " · " +
                        androidx.compose.ui.res.pluralStringResource(R.plurals.settings_hub_widgets_count, widgets.size, widgets.size)
                ) { onNavigate("elements") },
                SettingsTile(Icons.Filled.Palette, stringResource(R.string.settings_home_appearance_title), stringResource(R.string.settings_hub_bar_look_contents)) { onNavigate("look") }
            )
        )
    }
}

/** The Panel's look: fill, text, opacity, border and corners. */
@Composable
fun SettingsHomePanelLookScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val saved = rememberPanelRow(pageId, viewModel) ?: return
    ElementLookScreen(
        title = stringResource(R.string.settings_home_panel_label),
        navController = navController,
        onBack = onBack,
        saved = saved.appearance(),
        savedCorner = saved.cornerRadiusPercent,
        onSave = { look, corner -> viewModel.saveRow(saved.withAppearance(look).copy(cornerRadiusPercent = corner ?: saved.cornerRadiusPercent)) },
        kind = LookSampleKind.PANEL
    )
}

/** Finds the Home page's own pinned Panel row - mirrors [SettingsDockScreen.kt]'s rememberDockRow,
 *  same "always present once seeded, null only means not loaded yet" contract. */
@Composable
private fun rememberPanelRow(pageId: Long, viewModel: HomeViewModel): PageRow? {
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    return rowsByPage[pageId]?.find { it.type == PageRowType.PANEL }
}


/** The Panel's own grid editor - same "+" preview/chooser idea as [SettingsDockElementsScreen], but
 *  shaped row by row (each sub-row picks its own column count, 1-6) instead of one shared column
 *  count for the whole grid, since the Panel wraps to multiple rows rather than growing/shrinking
 *  width like the dock does - see [com.bisc.flauncher.ui.home.GridEntries]'s rowColumns param
 *  for the centered layout this produces. No separate reorderable entries list underneath either:
 *  tapping a filled slot in the preview itself removes it (see onEditEntry there), since every slot
 *  is already individually picked through the "+" chooser - a second list showing the same content
 *  again underneath was redundant. Adds contacts as a 7th content type (via the same
 *  [AddElementChooserSheet] the Dock now shares). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanelElementsScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val row = rememberPanelRow(pageId, viewModel) ?: return
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
    val rowShape = row.panelRows()

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

    val panelWidgets by viewModel.panelWidgets.collectAsState()
    val panelFolderId by androidx.compose.runtime.produceState<Long?>(null) { value = viewModel.panelWidgetFolderId() }
    var choosingWidget by remember { mutableStateOf(false) }
    var sizingWidget by remember { mutableStateOf<String?>(null) }
    var sizingSpan by remember { androidx.compose.runtime.mutableIntStateOf(1) }
    val systemWidgetPickerFor by viewModel.systemWidgetPickerOpenForFolder.collectAsState()

    val (picked, pick) = rememberPickedEntry(entries)
    val slotCount = rowShape.sum()
    val look = row.appearance()
    SettingsSubScreen(
        title = stringResource(R.string.settings_panel_content_title),
        navController = navController,
        onBack = onBack,
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
                        selectedEntryId = picked?.id
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        // Widgets sit on top of the Panel, its elements below them; the Panel scrolls when it is full.
        SettingsPreferenceGroup(title = stringResource(R.string.settings_panel_widgets_label), info = stringResource(R.string.settings_panel_widgets_info)) {
            val rowCount = rowShape.size
            com.bisc.flauncher.ui.home.widgetsAbove(panelWidgets, rowCount).flatten().forEach { item ->
                val place = minOf(item.orderIndex / com.bisc.flauncher.ui.home.WIDGET_ROW_STEP, rowCount)
                SettingsPreference(
                    icon = if (item.type == com.bisc.flauncher.data.model.GridItemType.SYSTEM_WIDGET) androidx.compose.material.icons.Icons.Filled.GridView else widgetKindIcon(item.payload),
                    title = if (item.type == com.bisc.flauncher.data.model.GridItemType.SYSTEM_WIDGET) stringResource(R.string.picker_widget_more) else widgetKindLabel(item.payload),
                    summary = if (place >= rowCount) stringResource(R.string.settings_widget_place_bottom) else stringResource(R.string.settings_widget_place_above_row, place + 1),
                    onClick = { panelFolderId?.let { navController.navigate("settings/folder/$it/widget/${item.id}") } },
                    trailing = {
                        Row {
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.saveFolderItems(com.bisc.flauncher.ui.home.moveWidgetStep(panelWidgets, item, -1, rowCount)) },
                                enabled = com.bisc.flauncher.ui.home.canMoveWidget(panelWidgets, item, -1, rowCount)
                            ) { Icon(androidx.compose.material.icons.Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.settings_move_up)) }
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.saveFolderItems(com.bisc.flauncher.ui.home.moveWidgetStep(panelWidgets, item, 1, rowCount)) },
                                enabled = com.bisc.flauncher.ui.home.canMoveWidget(panelWidgets, item, 1, rowCount)
                            ) { Icon(androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.settings_move_down)) }
                        }
                    }
                )
            }
            SettingsPreference(
                icon = androidx.compose.material.icons.Icons.Filled.Add,
                title = stringResource(R.string.settings_panel_add_widget),
                onClick = { choosingWidget = true },
                trailing = null
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_panel_rows_label)) {
            FolderRowsEditor(rowShape = rowShape, maxColumns = 5) { viewModel.setPanelRowColumns(row, it) }
        }
        CaptionsSection(
            showLabels = row.showLabels,
            onShowLabelsChange = { viewModel.setRowShowLabels(row, it) },
            appearance = row.appearance(),
            onAppearanceChange = { viewModel.setRowAppearance(row, it) }
        )
    }

    if (choosingWidget) {
        FolderWidgetKindSheet(
            onKindSelected = { kind ->
                choosingWidget = false
                sizingSpan = com.bisc.flauncher.data.model.WidgetKind.defaultSpan(kind)
                sizingWidget = kind
            },
            onMoreSelected = {
                choosingWidget = false
                panelFolderId?.let(viewModel::openSystemWidgetPickerForFolder)
            },
            onDismiss = { choosingWidget = false }
        )
    }
    sizingWidget?.let { kind ->
        FolderWidgetSizeSheet(
            row = newWidgetPreviewRow(0, kind, sizingSpan),
            spanY = sizingSpan,
            minSpan = com.bisc.flauncher.data.model.WidgetKind.minSpan(kind),
            maxSpan = com.bisc.flauncher.data.model.WidgetKind.maxSpan(kind),
            onSpanChange = { sizingSpan = it },
            onSave = { viewModel.addPanelWidget(kind, sizingSpan); sizingWidget = null },
            onDismiss = { sizingWidget = null }
        )
    }
    systemWidgetPickerFor?.let { targetFolderId ->
        val providers = remember(targetFolderId) { viewModel.installedSystemWidgetProviders() }
        com.bisc.flauncher.ui.home.SystemWidgetPickerSheet(
            providers = providers,
            onProviderSelected = { provider -> viewModel.startAddSystemWidgetToFolder(targetFolderId, provider) },
            onDismiss = viewModel::dismissSystemWidgetPicker
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
            apps = com.bisc.flauncher.util.ProfileComponents.ownOnly(browsableApps),
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
