package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.ui.home.FolderIcon

/** All folders as tiles to tap, with a tile to make a new one, and the look of all folders above. */
@Composable
fun SettingsFolderScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val folders by viewModel.folders.collectAsState()
    val folderItems by viewModel.folderItems.collectAsState()
    val browsableApps by viewModel.labeledApps.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    val itemsByFolder = remember(folderItems) { folderItems.groupBy { it.folderId } }
    // Folders inside other folders show inside those, not here among the top level.
    val nested = remember(folderItems) {
        folderItems.filter { it.type == com.bisc.flauncher.data.model.GridItemType.FOLDER }.mapNotNull { it.payload.toLongOrNull() }.toSet()
    }

    SettingsSubScreen(title = stringResource(R.string.settings_folder_title), navController = navController, onBack = onBack) {
        Text(
            stringResource(R.string.settings_folder_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.settings_folder_global_label),
                // How many folders follow it: a folder with a fill or border of its own does not.
                summary = stringResource(
                    R.string.settings_folder_template_following,
                    folders.count { !it.appearanceOverrideEnabled && !it.borderOverrideEnabled },
                    folders.size
                ),
                onClick = { navController.navigate("settings/folder/global") }
            )
        }
        val tiles: List<Folder?> = listOf<Folder?>(null) + folders.filter { it.id !in nested }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 12.dp
            val tileWidth = (maxWidth - gap * 2) / 3
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                tiles.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { folder ->
                            if (folder == null) {
                                NewFolderTile(Modifier.width(tileWidth)) { showCreateDialog = true }
                            } else {
                                FolderTile(
                                    folder = folder,
                                    items = itemsByFolder[folder.id].orEmpty(),
                                    viewModel = viewModel,
                                    modifier = Modifier.width(tileWidth)
                                ) { navController.navigate("settings/folder/${folder.id}") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        FolderCreatorSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onCreate = { name, apps ->
                showCreateDialog = false
                // Straight on to the new folder, to arrange it.
                viewModel.createFolderWithApps(name, apps) { id -> navController.navigate("settings/folder/$id") }
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun FolderTile(folder: Folder, items: List<FolderItem>, viewModel: SettingsViewModel, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val widgets = items.count { it.isWidget() }
    val others = items.size - widgets
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FolderIcon(folder = folder, previewItems = items, loadIcon = viewModel::loadIcon, loadPackIcon = viewModel::loadPackIcon, modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(10.dp))
        Text(folder.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(
            if (widgets > 0) stringResource(R.string.settings_folder_tile_count_widgets, others, widgets) else stringResource(R.string.settings_folder_items_count, others),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NewFolderTile(modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.settings_folder_create), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1, textAlign = TextAlign.Center)
        Text(" ", style = MaterialTheme.typography.labelSmall)
    }
}
