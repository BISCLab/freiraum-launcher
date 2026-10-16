package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.bisc.flauncher.R

/** Global "Elemente" hub (Settings -> Elements) — Apps/Ordner/Adressen/Internetadressen/Shortcuts/
 *  Powerbuttons, each configured once here and used everywhere that Element type can be placed
 *  (Panel, Dock, Elementenbox rows, folders). Shortcuts has no entry of its own per the masterplan
 *  (2.5: "kein eigenes Untermenü nötig" — they're not meaningfully editable beyond add/remove,
 *  which already happens wherever they're placed). */
@Composable
fun SettingsElementsScreen(navController: NavController, onBack: () -> Unit) {
    SettingsSubScreen(title = stringResource(R.string.settings_section_elements), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Apps,
                title = stringResource(R.string.settings_section_manage_apps),
                summary = stringResource(R.string.settings_card_manage_apps_subtitle),
                onClick = { navController.navigate("settings/apps") }
            )
            SettingsPreference(
                icon = Icons.AutoMirrored.Filled.List,
                title = stringResource(R.string.settings_section_folder),
                summary = stringResource(R.string.settings_card_folder_subtitle),
                onClick = { navController.navigate("settings/folder") }
            )
            SettingsPreference(
                icon = Icons.Filled.Place,
                title = stringResource(R.string.settings_elements_addresses_title),
                onClick = { navController.navigate("settings/elements/addresses") }
            )
            SettingsPreference(
                icon = Icons.Filled.Language,
                title = stringResource(R.string.settings_elements_links_title),
                onClick = { navController.navigate("settings/elements/links") }
            )
            // Powerbuttons row is added once its screen exists (follow-up within this phase).
        }
    }
}
