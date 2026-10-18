package com.bisc.flauncher.ui.settings

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R

/** The folder-wide appearance (fill/opacity/border/text color) and visibility presets shared by
 *  every folder — mirrors [SettingsAppsGlobalScreen]'s role for apps. An individual folder can still
 *  override this appearance for itself (see SettingsFolderEditScreen). */
@Composable
fun SettingsFolderGlobalScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val cardAppearance by viewModel.folderCardAppearance.collectAsState()
    val hideStatusBar by viewModel.folderHideStatusBar.collectAsState()
    val hideDock by viewModel.folderHideDock.collectAsState()
    val layerLabel = surfaceLabel(R.string.settings_layer_surface_folder)

    SettingsSubScreen(title = stringResource(R.string.settings_folder_global_label), navController = navController, onBack = onBack) {
        ElementAppearanceSection(appearance = cardAppearance, onAppearanceChange = viewModel::setFolderCardAppearance, styleGroupTitle = layerLabel)

        SettingsPreferenceGroup(title = stringResource(R.string.settings_folder_visibility_label)) {
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
