package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R

/** The folder-wide appearance/border/visibility presets shared by every folder — a hub mirroring
 *  the Dock's own Appearance/Border/Visibility split (see SettingsDockScreen.kt), rather than one
 *  long flat screen. An individual folder can still override Appearance and Border independently
 *  for itself (see SettingsFolderEditScreen). */
@Composable
fun SettingsFolderGlobalScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    SettingsSubScreen(title = stringResource(R.string.settings_folder_global_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.settings_folder_appearance_label),
                onClick = { onNavigate("appearance") },
                trailing = null
            )
            SettingsPreference(
                icon = Icons.Filled.BorderAll,
                title = stringResource(R.string.settings_element_border_label),
                onClick = { onNavigate("border") },
                trailing = null
            )
            SettingsPreference(
                icon = Icons.Filled.Visibility,
                title = stringResource(R.string.settings_folder_visibility_label),
                onClick = { onNavigate("visibility") },
                trailing = null
            )
        }
    }
}

@Composable
fun SettingsFolderGlobalAppearanceScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val cardAppearance by viewModel.folderCardAppearance.collectAsState()
    val showLabels by viewModel.folderShowLabels.collectAsState()
    SettingsSubScreen(title = stringResource(R.string.settings_folder_appearance_label), navController = navController, onBack = onBack) {
        ElementBackgroundSection(appearance = cardAppearance, onAppearanceChange = viewModel::setFolderCardAppearance)
        CaptionsSection(
            showLabels = showLabels,
            onShowLabelsChange = viewModel::setFolderShowLabels,
            appearance = cardAppearance,
            onAppearanceChange = viewModel::setFolderCardAppearance,
            title = stringResource(R.string.settings_captions_label)
        )
    }
}

@Composable
fun SettingsFolderGlobalBorderScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val cardAppearance by viewModel.folderCardAppearance.collectAsState()
    SettingsSubScreen(title = stringResource(R.string.settings_element_border_label), navController = navController, onBack = onBack) {
        ElementBorderSection(appearance = cardAppearance, onAppearanceChange = viewModel::setFolderCardAppearance)
    }
}

@Composable
fun SettingsFolderGlobalVisibilityScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val hideStatusBar by viewModel.folderHideStatusBar.collectAsState()
    val hideDock by viewModel.folderHideDock.collectAsState()
    val showAddRowDefault by viewModel.folderShowAddRowDefault.collectAsState()
    SettingsSubScreen(title = stringResource(R.string.settings_folder_visibility_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_folder_show_add_row_default_label),
                info = stringResource(R.string.settings_folder_show_add_row_summary),
                onClick = { viewModel.setFolderShowAddRowDefault(!showAddRowDefault) },
                trailing = { Switch(checked = showAddRowDefault, onCheckedChange = viewModel::setFolderShowAddRowDefault) }
            )
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
    }
}
