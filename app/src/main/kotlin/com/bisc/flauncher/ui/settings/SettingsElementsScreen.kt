package com.bisc.flauncher.ui.settings

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.bisc.flauncher.R

/** Global "Elemente" hub (Settings -> Elements) - Apps/Ordner/Adressen/Internetadressen/Shortcuts/
 *  Action buttons, each configured once here and used everywhere that Element type can be placed
 *  (Panel, Dock, Elementenbox rows, folders). Shortcuts have no entry of their own: they're not
 *  editable beyond add/remove, which happens wherever they're placed. */
@Composable
fun SettingsElementsScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    SettingsSubScreen(title = stringResource(R.string.settings_section_elements), navController = navController, onBack = onBack) {
        val apps by viewModel.labeledApps.collectAsState()
        val folders by viewModel.folders.collectAsState()
        val places by viewModel.savedMapTargets.collectAsState()
        val links by viewModel.pinnedWebsites.collectAsState()
        val buttons by viewModel.actionButtons.collectAsState()
        Text(
            stringResource(R.string.settings_elements_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        SettingsTileGrid(
            tiles = listOf(
                SettingsTile(Icons.Filled.Apps, stringResource(R.string.settings_section_manage_apps), stringResource(R.string.settings_elements_apps_short), apps.size.toString()) { navController.navigate("settings/apps") },
                SettingsTile(Icons.AutoMirrored.Filled.List, stringResource(R.string.settings_section_folder), stringResource(R.string.settings_elements_folders_short), folders.size.toString()) { navController.navigate("settings/folder") },
                SettingsTile(Icons.Filled.Place, stringResource(R.string.settings_elements_addresses_title), stringResource(R.string.settings_elements_places_short), places.size.toString()) { navController.navigate("settings/elements/addresses") },
                SettingsTile(Icons.Filled.Language, stringResource(R.string.settings_elements_links_title), stringResource(R.string.settings_elements_links_short), links.size.toString()) { navController.navigate("settings/elements/links") },
                SettingsTile(Icons.Filled.Bolt, stringResource(R.string.settings_elements_actionbuttons_title), stringResource(R.string.settings_elements_actions_short), buttons.size.toString()) { navController.navigate("settings/elements/actionbuttons") }
            )
        )
    }
}
