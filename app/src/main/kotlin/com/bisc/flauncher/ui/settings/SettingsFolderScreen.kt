package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.bisc.flauncher.ui.home.FolderIcon

/** Folder overlay's own settings screen. */
@Composable
fun SettingsFolderScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val cardAppearance by viewModel.folderCardAppearance.collectAsState()
    val hideStatusBar by viewModel.folderHideStatusBar.collectAsState()
    val hideDock by viewModel.folderHideDock.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val folderItems by viewModel.folderItems.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    var showCardSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val layerLabel = surfaceLabel(R.string.settings_layer_surface_folder)
    val itemsByFolder = remember(folderItems) { folderItems.groupBy { it.folderId } }

    SettingsSubScreen(title = stringResource(R.string.settings_folder_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(icon = null, title = layerLabel, onClick = { showCardSheet = true }, trailing = null)
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_folder_show_add_row_everywhere_off),
                info = stringResource(R.string.settings_folder_show_add_row_everywhere_off_summary),
                onClick = { viewModel.hideFolderAddRowEverywhere() },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_folder_hide_status_bar),
                onClick = { viewModel.setFolderHideStatusBar(!hideStatusBar) },
                trailing = { Switch(checked = hideStatusBar, onCheckedChange = viewModel::setFolderHideStatusBar) }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_folder_hide_dock),
                info = stringResource(R.string.settings_folder_hide_dock_summary),
                onClick = { viewModel.setFolderHideDock(!hideDock) },
                trailing = { Switch(checked = hideDock, onCheckedChange = viewModel::setFolderHideDock) }
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_folder_list_label)) {
            if (folders.isEmpty()) {
                SettingsPreference(icon = null, title = stringResource(R.string.folder_overview_empty), onClick = {}, trailing = null)
            }
            folders.forEach { folder ->
                val count = itemsByFolder[folder.id].orEmpty().size
                SettingsPreference(
                    icon = null,
                    leading = {
                        FolderIcon(
                            folder = folder,
                            previewItems = itemsByFolder[folder.id].orEmpty(),
                            loadIcon = viewModel::loadIcon,
                            loadPackIcon = viewModel::loadPackIcon,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    title = folder.name,
                    summary = stringResource(R.string.settings_folder_items_count, count),
                    onClick = { navController.navigate("settings/folder/${folder.id}") }
                )
            }
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_folder_create),
                onClick = { showCreateDialog = true },
                trailing = null
            )
        }
    }

    if (showCardSheet) {
        SettingsEditPage(title = layerLabel, onDismiss = { showCardSheet = false }) {
            ElementAppearanceSection(appearance = cardAppearance, onAppearanceChange = viewModel::setFolderCardAppearance, styleGroupTitle = layerLabel)
        }
    }

    if (showCreateDialog) {
        FolderCreatorSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onCreate = { name, apps -> viewModel.createFolderWithApps(name, apps); showCreateDialog = false },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
internal fun CreateFolderDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    SettingsEditSheet(title = stringResource(R.string.settings_folder_creator_title), onDismiss = onDismiss) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.settings_folder_creator_name_hint)) },
            singleLine = true,
            shape = SettingsTextFieldShape,
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(
            onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_folder_creator_create))
        }
    }
}
