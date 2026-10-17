package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.bisc.flauncher.data.model.IconPackIcon
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
@Composable
fun SettingsFolderEditScreen(
    folderId: Long,
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val folders by viewModel.folders.collectAsState()
    val folderItems by viewModel.folderItems.collectAsState()
    val iconPacks by viewModel.iconPacks.collectAsState()
    val folder = folders.find { it.id == folderId }
    var edited by rememberSaveable(folderId) { mutableStateOf<String?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val itemsByFolder = remember(folderItems) { folderItems.groupBy { it.folderId } }
    val items = itemsByFolder[folderId].orEmpty()
    val browsableApps by viewModel.browsableApps.collectAsState()
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

    val latestEdited by rememberUpdatedState(edited)
    val latestName by rememberUpdatedState(folder?.name)
    DisposableEffect(folderId) {
        onDispose {
            val name = latestEdited?.trim()
            if (!name.isNullOrBlank() && name != latestName) viewModel.renameFolder(folderId, name)
        }
    }

    SettingsSubScreen(title = stringResource(R.string.settings_folder_edit_title), navController = navController, onBack = onBack) {
        if (folder == null) return@SettingsSubScreen

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            FolderIcon(
                folder = folder,
                previewItems = folderItems.filter { it.folderId == folderId },
                loadIcon = viewModel::loadIcon,
                loadPackIcon = viewModel::loadPackIcon,
                modifier = Modifier.size(72.dp)
            )
        }

        OutlinedTextField(
            value = edited ?: folder.name,
            onValueChange = { edited = it },
            label = { Text(stringResource(R.string.settings_folder_name_label)) },
            singleLine = true,
            shape = SettingsTextFieldShape,
            modifier = Modifier.fillMaxWidth()
        )

        SettingsPreferenceGroup(title = stringResource(R.string.folder_icon_auto)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setFolderIcon(folderId, FolderIconMode.AUTO_STACK) }) {
                    Icon(imageVector = Icons.Filled.Apps, contentDescription = stringResource(R.string.folder_icon_auto))
                }
                PRESET_COLORS.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                            .combinedClickable(onClick = { viewModel.setFolderIcon(folderId, FolderIconMode.CUSTOM_COLOR, color = hex) })
                    )
                }
            }
            SettingsPreference(
                icon = Icons.Filled.Extension,
                title = stringResource(R.string.settings_folder_icon_pack_label),
                summary = if (folder.iconMode == FolderIconMode.ICON_PACK) {
                    iconPacks.find { it.packageName == folder.iconPackPackage }?.label ?: folder.iconPackPackage
                } else null,
                onClick = { navController.navigate("settings/folder/$folderId/icon") }
            )
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_folder_items_label)) {
            if (items.isEmpty()) {
                Text(
                    stringResource(R.string.folder_overview_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                items.forEach { item ->
                    FolderItemRow(
                        item = item,
                        folders = folders,
                        folderItemsByFolder = itemsByFolder,
                        savedMapTargets = savedMapTargets,
                        pinnedWebsites = pinnedWebsites,
                        loadIcon = viewModel::loadIcon,
                        loadPackIcon = viewModel::loadPackIcon,
                        resolveLabel = viewModel::resolveLabel,
                        onRemove = { viewModel.removeFolderItem(item) }
                    )
                }
            }
        }

        val showAddRow by remember(folderId) { viewModel.folderShowAddRow(folderId) }.collectAsState(initial = true)
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_folder_show_add_row_label),
                info = stringResource(R.string.settings_folder_show_add_row_summary),
                onClick = { viewModel.setFolderShowAddRow(folderId, !showAddRow) },
                trailing = { androidx.compose.material3.Switch(checked = showAddRow, onCheckedChange = { viewModel.setFolderShowAddRow(folderId, it) }) }
            )
        }

        SettingsPreferenceGroup {
            SettingsPreference(icon = Icons.Filled.Add, title = stringResource(R.string.settings_apps_row_add_app), onClick = { showAppPicker = true }, trailing = null)
            SettingsPreference(icon = Icons.Filled.CreateNewFolder, title = stringResource(R.string.settings_apps_row_add_folder), onClick = { showFolderPicker = true }, trailing = null)
            SettingsPreference(icon = Icons.AutoMirrored.Filled.Shortcut, title = stringResource(R.string.settings_apps_row_add_shortcut), onClick = { viewModel.openFolderShortcutAppPicker(folderId) }, trailing = null)
            SettingsPreference(icon = Icons.Filled.Place, title = stringResource(R.string.settings_apps_row_add_map_target), onClick = { showMapTargetPicker = true }, trailing = null)
            SettingsPreference(icon = Icons.Filled.Language, title = stringResource(R.string.settings_apps_row_add_website), onClick = { showWebsitePicker = true }, trailing = null)
        }

        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.DeleteOutline,
                title = stringResource(R.string.settings_folder_delete),
                onClick = { confirmingDelete = true },
                trailing = null
            )
        }
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
        // Only one level of folder nesting is allowed (see the masterplan's "weder direkt noch
        // indirekt" cycle rule) — a folder already nested somewhere else, or one that already has
        // folder children of its own, is excluded here. Together these also rule out every
        // possible cycle, not just the direct one-hop case the old `alreadyInside`-only check
        // caught: a cycle needs at least two levels of nesting to ever close back on itself.
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

@Composable
private fun FolderItemRow(
    item: com.bisc.flauncher.data.model.FolderItem,
    folders: List<com.bisc.flauncher.data.model.Folder>,
    folderItemsByFolder: Map<Long, List<com.bisc.flauncher.data.model.FolderItem>>,
    savedMapTargets: List<com.bisc.flauncher.data.model.SavedMapTarget>,
    pinnedWebsites: List<com.bisc.flauncher.data.model.PinnedWebsite>,
    loadIcon: (android.content.ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap?,
    resolveLabel: (android.content.ComponentName) -> String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (item.type) {
            GridItemType.APP -> {
                val componentName = remember(item.payload) { android.content.ComponentName.unflattenFromString(item.payload) }
                val bitmap = com.bisc.flauncher.ui.home.rememberAppIcon(componentName, loadIcon)
                val label = componentName?.let(resolveLabel).orEmpty()
                if (bitmap != null) Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(label, modifier = Modifier.weight(1f))
            }
            GridItemType.FOLDER -> {
                val childId = remember(item.payload) { item.payload.toLongOrNull() }
                val child = folders.find { it.id == childId }
                FolderIcon(folder = child, previewItems = childId?.let { folderItemsByFolder[it] }.orEmpty(), loadIcon = loadIcon, loadPackIcon = loadPackIcon, modifier = Modifier.size(28.dp))
                Text(child?.name.orEmpty(), modifier = Modifier.weight(1f))
            }
            GridItemType.SHORTCUT -> {
                val label = remember(item.payload) { item.payload.split("|").getOrNull(2).orEmpty() }
                Icon(Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(label, modifier = Modifier.weight(1f))
            }
            GridItemType.MAP_TARGET -> {
                val targetId = remember(item.payload) { item.payload.toLongOrNull() }
                val target = savedMapTargets.find { it.id == targetId }
                Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(target?.label.orEmpty(), modifier = Modifier.weight(1f))
            }
            GridItemType.WEBSITE -> {
                val websiteId = remember(item.payload) { item.payload.toLongOrNull() }
                val website = pinnedWebsites.find { it.id == websiteId }
                Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(website?.label.orEmpty(), modifier = Modifier.weight(1f))
            }
            GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET, GridItemType.PAGE, GridItemType.CONTACT, GridItemType.ACTION_BUTTON -> Text("", modifier = Modifier.weight(1f))
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
