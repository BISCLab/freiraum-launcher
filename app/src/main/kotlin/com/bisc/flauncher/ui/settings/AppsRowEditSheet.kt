package com.bisc.flauncher.ui.settings

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.AppRowEntry
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GridConstants
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.ui.home.FolderIcon
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/** Editor for one [com.bisc.flauncher.data.model.PageRowType.APPS] row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsRowEditSheet(
    row: PageRow,
    entries: List<AppRowEntry>,
    installedApps: List<AppInfo>,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    savedMapTargets: List<SavedMapTarget>,
    pinnedWebsites: List<PinnedWebsite>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    resolveLabel: (ComponentName) -> String = { "" },
    onColumnsChange: (Int) -> Unit,
    onAppearanceChange: (ElementAppearance) -> Unit,
    onAddApp: (AppInfo) -> Unit,
    onAddExistingFolder: (folderId: Long) -> Unit,
    onCreateFolder: (name: String) -> Unit,
    onAddShortcutClick: () -> Unit,
    onAddMapTargetClick: () -> Unit,
    onAddWebsiteClick: () -> Unit,
    onRemoveEntry: (AppRowEntry) -> Unit,
    onReorderEntries: (List<AppRowEntry>) -> Unit,
    onRemoveRow: () -> Unit,
    onDismiss: () -> Unit
) {
    var showAppPicker by remember { mutableStateOf(false) }
    var showFolderCreator by remember { mutableStateOf(false) }
    var confirmingRemoveRow by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.settings_apps_row_edit_title), style = MaterialTheme.typography.titleLarge)

            SettingsPreferenceGroup(title = stringResource(R.string.settings_apps_row_columns_label)) {
                SettingsPreference(
                    icon = null,
                    title = row.columns.toString(),
                    onClick = {},
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onColumnsChange(row.columns - 1) },
                                enabled = row.columns > GridConstants.MIN_APPS_ROW_COLUMNS
                            ) { Icon(Icons.Filled.Remove, contentDescription = null) }
                            IconButton(
                                onClick = { onColumnsChange(row.columns + 1) },
                                enabled = row.columns < GridConstants.MAX_APPS_ROW_COLUMNS
                            ) { Icon(Icons.Filled.Add, contentDescription = null) }
                        }
                    }
                )
            }

            ElementAppearanceSection(
                appearance = row.appearance(),
                onAppearanceChange = onAppearanceChange,
                styleGroupTitle = layerBackgroundLabel(2, R.string.settings_layer_surface_apps_row)
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
                    ReorderableList(items = entries, key = { it.id }, onReorder = onReorderEntries) { entry ->
                        AppRowEntryRow(
                            entry = entry,
                            folders = folders,
                            folderItemsByFolder = folderItemsByFolder,
                            savedMapTargets = savedMapTargets,
                            pinnedWebsites = pinnedWebsites,
                            loadIcon = loadIcon,
                            resolveLabel = resolveLabel,
                            onRemove = { onRemoveEntry(entry) }
                        )
                    }
                }
            }

            SettingsPreferenceGroup {
                SettingsPreference(icon = Icons.Filled.Add, title = stringResource(R.string.settings_apps_row_add_app), onClick = { showAppPicker = true }, trailing = null)
                SettingsPreference(icon = Icons.Filled.CreateNewFolder, title = stringResource(R.string.settings_apps_row_add_folder), onClick = { showFolderCreator = true }, trailing = null)
                SettingsPreference(icon = Icons.AutoMirrored.Filled.Shortcut, title = stringResource(R.string.settings_apps_row_add_shortcut), onClick = onAddShortcutClick, trailing = null)
                SettingsPreference(icon = Icons.Filled.Place, title = stringResource(R.string.settings_apps_row_add_map_target), onClick = onAddMapTargetClick, trailing = null)
                SettingsPreference(icon = Icons.Filled.Language, title = stringResource(R.string.settings_apps_row_add_website), onClick = onAddWebsiteClick, trailing = null)
            }

            SettingsPreferenceGroup {
                SettingsPreference(icon = Icons.Filled.DeleteOutline, title = stringResource(R.string.settings_rows_remove), onClick = { confirmingRemoveRow = true }, trailing = null)
            }
        }
    }

    if (showAppPicker) {
        ShortcutSourceAppSheet(
            apps = installedApps,
            loadIcon = loadIcon,
            onAppSelected = { app -> onAddApp(app); showAppPicker = false },
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
            loadIcon = loadIcon,
            onPick = { folder -> onAddExistingFolder(folder.id); showFolderCreator = false },
            onCreate = { name -> onCreateFolder(name); showFolderCreator = false },
            onDismiss = { showFolderCreator = false }
        )
    }

    if (confirmingRemoveRow) {
        AlertDialog(
            onDismissRequest = { confirmingRemoveRow = false },
            title = { Text(stringResource(R.string.settings_rows_remove)) },
            confirmButton = {
                TextButton(onClick = { confirmingRemoveRow = false; onRemoveRow() }) { Text(stringResource(R.string.settings_rows_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingRemoveRow = false }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }
}

@Composable
private fun AppRowEntryRow(
    entry: AppRowEntry,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    savedMapTargets: List<SavedMapTarget>,
    pinnedWebsites: List<PinnedWebsite>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    resolveLabel: (ComponentName) -> String,
    onRemove: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (entry.type) {
            GridItemType.APP -> {
                val componentName = remember(entry.payload) { ComponentName.unflattenFromString(entry.payload) }
                val bitmap = com.bisc.flauncher.ui.home.rememberAppIcon(componentName, loadIcon)
                val label = componentName?.let(resolveLabel).orEmpty()
                if (bitmap != null) Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(label, modifier = Modifier.weight(1f))
            }
            GridItemType.FOLDER -> {
                val folderId = remember(entry.payload) { entry.payload.toLongOrNull() }
                val folder = folders.find { it.id == folderId }
                FolderIcon(folder = folder, previewItems = folderId?.let { folderItemsByFolder[it] }.orEmpty(), loadIcon = loadIcon, modifier = Modifier.size(28.dp))
                Text(folder?.name.orEmpty(), modifier = Modifier.weight(1f))
            }
            GridItemType.SHORTCUT -> {
                val label = remember(entry.payload) { entry.payload.split("|").getOrNull(2).orEmpty() }
                Icon(Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(label, modifier = Modifier.weight(1f))
            }
            GridItemType.MAP_TARGET -> {
                val targetId = remember(entry.payload) { entry.payload.toLongOrNull() }
                val target = savedMapTargets.find { it.id == targetId }
                Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(target?.label.orEmpty(), modifier = Modifier.weight(1f))
            }
            GridItemType.WEBSITE -> {
                val websiteId = remember(entry.payload) { entry.payload.toLongOrNull() }
                val website = pinnedWebsites.find { it.id == websiteId }
                Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(website?.label.orEmpty(), modifier = Modifier.weight(1f))
            }
            GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET -> Text("", modifier = Modifier.weight(1f))
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Selection of an existing folder, or creation of a new one, for an apps row. */
@Composable
fun FolderPickerDialog(
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onPick: (Folder) -> Unit,
    onCreate: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    SettingsFixedDialog(title = stringResource(R.string.settings_apps_row_add_folder), onDismiss = onDismiss) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.CreateNewFolder,
                title = stringResource(R.string.settings_apps_row_new_folder),
                onClick = { creating = true },
                trailing = null
            )
        }
        if (folders.isEmpty()) {
            Text(
                stringResource(R.string.settings_apps_row_no_folders),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(folders, key = { it.id }) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(folder) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FolderIcon(folder = folder, previewItems = folderItemsByFolder[folder.id].orEmpty(), loadIcon = loadIcon, modifier = Modifier.size(36.dp))
                    Text(folder.name, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    if (creating) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text(stringResource(R.string.settings_apps_row_new_folder)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.settings_folder_creator_name_hint)) },
                    singleLine = true,
                    shape = SettingsTextFieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = { creating = false; onCreate(name.trim()) }) {
                    Text(stringResource(R.string.settings_folder_creator_create))
                }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text(stringResource(R.string.widget_notes_cancel)) } }
        )
    }
}

/** Names a new folder and multi-selects its starting apps. */
@Composable
fun FolderCreatorSheet(
    apps: List<AppInfo>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onCreate: (name: String, apps: List<AppInfo>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val selected = remember { androidx.compose.runtime.mutableStateListOf<AppInfo>() }
    val filtered = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }
    val canCreate = name.isNotBlank() && selected.isNotEmpty()

    SettingsFixedDialog(
        title = stringResource(R.string.settings_folder_creator_title),
        onDismiss = onDismiss,
        actions = {
            TextButton(onClick = { onCreate(name, selected.toList()) }, enabled = canCreate) {
                Text(stringResource(R.string.settings_folder_creator_create))
            }
        },
        bottomBar = {
            androidx.compose.material3.Button(
                onClick = { onCreate(name, selected.toList()) },
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(stringResource(R.string.settings_folder_creator_create))
            }
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.settings_folder_creator_name_hint)) },
            singleLine = true,
            shape = SettingsTextFieldShape,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.settings_folder_creator_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = null) }
                }
            },
            singleLine = true,
            shape = SettingsTextFieldShape,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Text(
            text = stringResource(R.string.settings_folder_creator_selected, selected.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 8.dp)) {
            items(filtered, key = { it.componentName.flattenToString() }) { app ->
                val bitmap = com.bisc.flauncher.ui.home.rememberAppIcon(app.componentName, loadIcon)
                val isSelected = selected.contains(app)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (isSelected) selected.remove(app) else selected.add(app) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isSelected, onCheckedChange = { checked -> if (checked) selected.add(app) else selected.remove(app) })
                    if (bitmap != null) Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(28.dp))
                    else androidx.compose.foundation.layout.Spacer(Modifier.size(28.dp))
                    Text(app.label, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

/** Pick one already-[targets] saved place, or start a brand new one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedMapTargetPickerSheet(
    targets: List<SavedMapTarget>,
    onSelect: (SavedMapTarget) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (targets.isEmpty()) {
                Text(
                    stringResource(R.string.sidebar_maps_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                targets.forEach { target ->
                    SettingsPreference(icon = Icons.Filled.Place, title = target.label, onClick = { onSelect(target) }, trailing = null)
                }
            }
            SettingsPreference(icon = Icons.Filled.Add, title = stringResource(R.string.sidebar_maps_add), onClick = onAddNew, trailing = null)
        }
    }
}

/** Selection of a saved website, or creation of a new one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedWebsitePickerSheet(
    websites: List<PinnedWebsite>,
    onSelect: (PinnedWebsite) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (websites.isEmpty()) {
                Text(
                    stringResource(R.string.sidebar_websites_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                websites.forEach { website ->
                    SettingsPreference(icon = Icons.Filled.Language, title = website.label, onClick = { onSelect(website) }, trailing = null)
                }
            }
            SettingsPreference(icon = Icons.Filled.Add, title = stringResource(R.string.sidebar_website_add), onClick = onAddNew, trailing = null)
        }
    }
}
