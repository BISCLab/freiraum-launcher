package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.theme.noRippleClickable
import com.bisc.flauncher.data.model.SidebarIconOption
import com.bisc.flauncher.data.model.SidebarCategoryViewMode
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.ResolvedSidebarItem
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet
import com.bisc.flauncher.ui.home.dock.SidebarEntryType

/** The sidebar's own dedicated settings screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSidebarScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val items by viewModel.sidebarItems.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val panelAppearance by viewModel.sidebarPanelAppearance.collectAsState()
    val shortcutAppPickerOpen by viewModel.shortcutAppPickerOpen.collectAsState()
    val shortcutPickerForApp by viewModel.shortcutPickerForApp.collectAsState()
    val availableShortcuts by viewModel.availableShortcuts.collectAsState()

    var editingItemId by remember { mutableStateOf<Long?>(null) }
    val editingItem = editingItemId?.let { id -> items.find { it.raw.id == id } }
    var showAddChooser by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showMapTargetDialog by remember { mutableStateOf(false) }
    var showPanelSheet by remember { mutableStateOf(false) }
    var showBackdropSheet by remember { mutableStateOf(false) }
    val quickOverviewBackgroundOverride by viewModel.quickOverviewBackgroundOverride.collectAsState()
    val globalBackground by viewModel.globalBackground.collectAsState()
    val quickHideStatusBar by viewModel.quickOverviewHideStatusBar.collectAsState()
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_section_search_overview), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            if (items.isEmpty()) {
                Text(
                    stringResource(R.string.settings_sidebar_items_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                ReorderableList(
                    items = items,
                    key = { it.raw.id },
                    onReorder = { reordered -> viewModel.reorderSidebarItems(reordered) }
                ) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .noRippleClickable { editingItemId = item.raw.id },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SidebarItemRowIcon(item = item, loadIcon = viewModel::loadIcon)
                        Text(sidebarItemLabel(item), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_sidebar_add),
                onClick = { showAddChooser = true },
                trailing = null
            )
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = layerBackgroundLabel(1, R.string.settings_layer_surface_quick_overview),
                info = stringResource(R.string.settings_quick_overview_backdrop_summary),
                onClick = { showBackdropSheet = true },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = layerBackgroundLabel(2, R.string.settings_layer_surface_sidebar),
                onClick = { showPanelSheet = true },
                trailing = null
            )
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_statusbar_hide_quick_search),
                onClick = { viewModel.setQuickOverviewHideStatusBar(!quickHideStatusBar) },
                trailing = { Switch(checked = quickHideStatusBar, onCheckedChange = viewModel::setQuickOverviewHideStatusBar) }
            )
        }
    }

    if (showBackdropSheet) {
        SettingsEditPage(
            title = layerBackgroundLabel(1, R.string.settings_layer_surface_quick_overview),
            onDismiss = { showBackdropSheet = false }
        ) {
            Text(
                stringResource(R.string.settings_overlay_backdrop_explainer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BackgroundOverrideSection(
                override = quickOverviewBackgroundOverride,
                global = globalBackground,
                onUseGlobal = viewModel::clearQuickOverviewBackground,
                onCustomColor = viewModel::setQuickOverviewBackground
            )
        }
    }

    if (showPanelSheet) {
        SettingsEditPage(title = layerBackgroundLabel(2, R.string.settings_layer_surface_sidebar), onDismiss = { showPanelSheet = false }) {
            ElementAppearanceSection(
                appearance = panelAppearance,
                onAppearanceChange = viewModel::setSidebarPanelAppearance,
                styleGroupTitle = layerBackgroundLabel(2, R.string.settings_layer_surface_sidebar)
            )
        }
    }

    if (showAddChooser) {
        AddSidebarItemChooserSheet(
            onCategory = { showAddChooser = false; showCategoryPicker = true },
            onApp = { showAddChooser = false; showAppPicker = true },
            onShortcut = { showAddChooser = false; viewModel.openShortcutAppPicker() },
            onPlace = { showAddChooser = false; showMapTargetDialog = true },
            onFolder = { showAddChooser = false; showCreateFolderDialog = true },
            onDismiss = { showAddChooser = false }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onCreate = { name -> viewModel.createFolder(name); showCreateFolderDialog = false },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    if (showCategoryPicker) {
        val alreadyAdded = remember(items) {
            items.filterIsInstance<ResolvedSidebarItem.Category>().map { it.entryType }.toSet()
        }
        val remaining = remember(alreadyAdded) { SidebarEntryType.entries.filter { it !in alreadyAdded } }
        AddCategorySheet(
            options = remaining,
            onSelected = { entry -> viewModel.addSidebarCategory(entry); showCategoryPicker = false },
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (showAppPicker) {
        val alreadyAdded = remember(items) {
            items.filterIsInstance<ResolvedSidebarItem.App>().map { it.app.componentName }.toSet()
        }
        val pickableApps = remember(browsableApps, alreadyAdded) {
            browsableApps.filter { it.componentName !in alreadyAdded }
        }
        AddAppSheet(
            apps = pickableApps,
            loadIcon = viewModel::loadIcon,
            onSelected = { app -> viewModel.addSidebarApp(app); showAppPicker = false },
            onDismiss = { showAppPicker = false }
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
            onShortcutSelected = viewModel::addSidebarShortcut,
            onDismiss = viewModel::dismissShortcutPicker
        )
    }

    if (showMapTargetDialog) {
        AddMapTargetDialog(
            onSave = { label, lat, lon, address ->
                viewModel.addSidebarMapTarget(label, lat, lon, address)
                showMapTargetDialog = false
            },
            onDismiss = { showMapTargetDialog = false }
        )
    }

    editingItem?.let { item ->
        val configurableEntry = (item as? ResolvedSidebarItem.Category)?.entryType?.takeIf { it in SIDEBAR_CATEGORY_DISPLAY_CONFIGURABLE }
        val viewMode = configurableEntry?.let { entry ->
            remember(entry) { viewModel.sidebarCategoryViewMode(entry) }.collectAsState(initial = SidebarCategoryViewMode.LIST).value
        }
        val showAddRow = configurableEntry?.let { entry ->
            remember(entry) { viewModel.sidebarCategoryShowAddRow(entry) }.collectAsState(initial = true).value
        }
        SidebarItemEditSheet(
            item = item,
            onIconSelected = { key -> viewModel.setSidebarItemIcon(item, key) },
            onRemove = {
                viewModel.removeSidebarItem(item)
                editingItemId = null
            },
            onDismiss = { editingItemId = null },
            viewMode = viewMode,
            onViewModeSelected = { mode -> configurableEntry?.let { viewModel.setSidebarCategoryViewMode(it, mode) } },
            showAddRow = showAddRow,
            onShowAddRowChange = { show -> configurableEntry?.let { viewModel.setSidebarCategoryShowAddRow(it, show) } }
        )
    }
}

private val SIDEBAR_CATEGORY_DISPLAY_CONFIGURABLE = setOf(
    SidebarEntryType.FOLDERS, SidebarEntryType.MAPS, SidebarEntryType.WEBSITES, SidebarEntryType.SHORTCUTS, SidebarEntryType.APPS,
    SidebarEntryType.CONTACTS
)

@Composable
private fun sidebarItemLabel(item: ResolvedSidebarItem): String = when (item) {
    is ResolvedSidebarItem.Category -> stringResource(item.entryType.labelRes)
    is ResolvedSidebarItem.App -> item.app.label
    is ResolvedSidebarItem.Shortcut -> item.shortcut.label
    is ResolvedSidebarItem.MapTarget -> item.target.label
}

@Composable
private fun SidebarItemRowIcon(item: ResolvedSidebarItem, loadIcon: (android.content.ComponentName) -> androidx.compose.ui.graphics.ImageBitmap?) {
    val customIcon = item.raw.customIconKey?.let { key -> runCatching { SidebarIconOption.valueOf(key) }.getOrNull() }
    when {
        customIcon != null -> Icon(customIcon.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        item is ResolvedSidebarItem.Category -> Icon(item.entryType.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        item is ResolvedSidebarItem.App -> {
            val bitmap = com.bisc.flauncher.ui.home.rememberAppIcon(item.app.componentName, loadIcon)
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(28.dp))
            } else {
                Icon(Icons.Filled.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        item is ResolvedSidebarItem.Shortcut -> Icon(Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        item is ResolvedSidebarItem.MapTarget -> Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSidebarItemChooserSheet(
    onCategory: () -> Unit,
    onApp: () -> Unit,
    onShortcut: () -> Unit,
    onPlace: () -> Unit,
    onFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            SettingsPreference(icon = null, title = stringResource(R.string.settings_sidebar_add_category), onClick = onCategory, trailing = null)
            SettingsPreference(icon = Icons.Filled.Apps, title = stringResource(R.string.settings_sidebar_add_app), onClick = onApp, trailing = null)
            SettingsPreference(icon = Icons.AutoMirrored.Filled.Shortcut, title = stringResource(R.string.settings_sidebar_add_shortcut), onClick = onShortcut, trailing = null)
            SettingsPreference(icon = Icons.Filled.Place, title = stringResource(R.string.settings_sidebar_add_place), onClick = onPlace, trailing = null)
            SettingsPreference(icon = Icons.Filled.CreateNewFolder, title = stringResource(R.string.settings_folder_create), onClick = onFolder, trailing = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategorySheet(options: List<SidebarEntryType>, onSelected: (SidebarEntryType) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if (options.isEmpty()) {
                Text(
                    stringResource(R.string.settings_sidebar_add_category_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                options.forEach { entry ->
                    SettingsPreference(
                        icon = entry.icon,
                        title = stringResource(entry.labelRes),
                        onClick = { onSelected(entry) },
                        trailing = null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAppSheet(
    apps: List<com.bisc.flauncher.data.model.AppInfo>,
    loadIcon: (android.content.ComponentName) -> androidx.compose.ui.graphics.ImageBitmap?,
    onSelected: (com.bisc.flauncher.data.model.AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(apps, key = { it.componentName.flattenToString() }) { app ->
                    val bitmap = com.bisc.flauncher.ui.home.rememberAppIcon(app.componentName, loadIcon)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .noRippleClickable { onSelected(app) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (bitmap != null) {
                            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(32.dp))
                        }
                        Text(app.label, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarItemEditSheet(
    item: ResolvedSidebarItem,
    onIconSelected: (String?) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    viewMode: SidebarCategoryViewMode? = null,
    onViewModeSelected: (SidebarCategoryViewMode) -> Unit = {},
    showAddRow: Boolean? = null,
    onShowAddRowChange: (Boolean) -> Unit = {}
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.settings_sidebar_edit_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.settings_sidebar_icon_label), style = MaterialTheme.typography.labelMedium)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    IconOptionSwatch(
                        icon = null,
                        selected = item.raw.customIconKey == null,
                        onClick = { onIconSelected(null) }
                    )
                }
                items(SidebarIconOption.entries.toList()) { option ->
                    IconOptionSwatch(
                        icon = option.icon,
                        selected = item.raw.customIconKey == option.name,
                        onClick = { onIconSelected(option.name) }
                    )
                }
            }
            if (viewMode != null) {
                SettingsPreferenceGroup(title = stringResource(R.string.settings_sidebar_category_view_mode_label)) {
                    RadioPreference(
                        label = stringResource(R.string.sidebar_view_list),
                        value = SidebarCategoryViewMode.LIST,
                        selected = viewMode,
                        onSelect = onViewModeSelected
                    )
                    RadioPreference(
                        label = stringResource(R.string.sidebar_view_grid),
                        value = SidebarCategoryViewMode.GRID,
                        selected = viewMode,
                        onSelect = onViewModeSelected
                    )
                }
            }
            if (showAddRow != null) {
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_sidebar_category_show_add_row_label),
                        onClick = { onShowAddRowChange(!showAddRow) },
                        trailing = { Switch(checked = showAddRow, onCheckedChange = onShowAddRowChange) }
                    )
                }
            }
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = Icons.Filled.DeleteOutline,
                    title = stringResource(R.string.settings_sidebar_remove),
                    onClick = onRemove,
                    trailing = null
                )
            }
        }
    }
}

@Composable
private fun IconOptionSwatch(icon: androidx.compose.ui.graphics.vector.ImageVector?, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        } else {
            Text(stringResource(R.string.settings_sidebar_icon_default).take(1), style = MaterialTheme.typography.labelSmall)
        }
    }
}
