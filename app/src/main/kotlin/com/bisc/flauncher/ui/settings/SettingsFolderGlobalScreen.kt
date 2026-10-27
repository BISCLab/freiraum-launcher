package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
                title = stringResource(R.string.settings_element_fill_label),
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
    val saved by viewModel.folderCardAppearance.collectAsState()
    var cardAppearance by rememberDraft(saved)
    SettingsSubScreen(title = stringResource(R.string.settings_element_fill_label), navController = navController, onBack = onBack, onSave = { viewModel.setFolderCardAppearance(cardAppearance) }, dirty = cardAppearance != saved) {
        ElementBackgroundSection(appearance = cardAppearance, onAppearanceChange = { cardAppearance = it })
    }
}

@Composable
fun SettingsFolderGlobalBorderScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val saved by viewModel.folderCardAppearance.collectAsState()
    var cardAppearance by rememberDraft(saved)
    SettingsSubScreen(title = stringResource(R.string.settings_element_border_label), navController = navController, onBack = onBack, onSave = { viewModel.setFolderCardAppearance(cardAppearance) }, dirty = cardAppearance != saved) {
        ElementBorderSection(appearance = cardAppearance, onAppearanceChange = { cardAppearance = it })
    }
}

@Composable
fun SettingsFolderGlobalVisibilityScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val hideStatusBar by viewModel.folderHideStatusBar.collectAsState()
    val hideDock by viewModel.folderHideDock.collectAsState()
    val showLabels by viewModel.folderShowLabels.collectAsState()
    val cardAppearance by viewModel.folderCardAppearance.collectAsState()
    SettingsSubScreen(title = stringResource(R.string.settings_folder_visibility_label), navController = navController, onBack = onBack) {
        // Everything about what's shown while a folder is open, in one card: its items' captions
        // (each folder can override this right under its own items), status bar and dock.
        SettingsPreferenceGroup {
            CaptionsRows(
                showLabels = showLabels,
                onShowLabelsChange = viewModel::setFolderShowLabels,
                appearance = cardAppearance,
                onAppearanceChange = viewModel::setFolderCardAppearance
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
