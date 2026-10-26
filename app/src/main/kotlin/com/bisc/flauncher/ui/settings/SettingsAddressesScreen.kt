package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.OutlinedTextField
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
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/** Global "Adressen" Element list (Settings -> Elements -> Addresses) — every [SavedMapTarget] in
 *  one place, independent of which rows/folders currently pin it. Mirrors [SettingsFolderScreen]'s
 *  list+create-dialog+navigate-to-detail pattern. */
@Composable
fun SettingsAddressesScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val targets by viewModel.savedMapTargets.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_elements_addresses_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_addresses_title)) {
            if (targets.isEmpty()) {
                SettingsPreference(icon = null, title = stringResource(R.string.settings_elements_addresses_empty), onClick = {}, trailing = null)
            }
            targets.sortedBy { it.label }.forEach { target ->
                SettingsPreference(
                    icon = Icons.Filled.Place,
                    title = target.label,
                    summary = target.address.takeIf { it.isNotBlank() },
                    onClick = { navController.navigate("settings/elements/addresses/${target.id}") }
                )
            }
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_elements_add_address),
                onClick = { showCreateDialog = true },
                trailing = null
            )
        }
    }

    if (showCreateDialog) {
        AddMapTargetDialog(
            onSave = { label, lat, lon, address -> viewModel.addMapTarget(label, lat, lon, address); showCreateDialog = false },
            onDismiss = { showCreateDialog = false }
        )
    }
}

/** Detail/edit screen for one [SavedMapTarget], reached by id from [SettingsAddressesScreen] — same
 *  "hold only the id, re-derive the live item every recomposition" convention as the rest of
 *  Settings (e.g. [SettingsPageRowsScreen]). */
@Composable
fun SettingsAddressDetailScreen(targetId: Long, navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val targets by viewModel.savedMapTargets.collectAsState()
    val target = targets.find { it.id == targetId } ?: return
    val browsableApps by viewModel.browsableApps.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    SettingsSubScreen(title = target.label, navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            OutlinedTextField(
                value = target.label,
                onValueChange = { viewModel.updateMapTarget(target.copy(label = it)) },
                label = { Text(stringResource(R.string.sidebar_maps_label_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            OutlinedTextField(
                value = target.address,
                onValueChange = { viewModel.updateMapTarget(target.copy(address = it)) },
                label = { Text(stringResource(R.string.settings_elements_address_hint)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_navigation_app_label)) {
            val appLabel = target.navigationApp?.let { pkg -> browsableApps.find { it.componentName.packageName == pkg }?.label }
            SettingsPreference(
                icon = null,
                title = appLabel ?: stringResource(R.string.settings_elements_app_default),
                onClick = { showAppPicker = true },
                trailing = if (target.navigationApp != null) {
                    { TextButton(onClick = { viewModel.updateMapTarget(target.copy(navigationApp = null)) }) { Text(stringResource(R.string.settings_elements_use_default)) } }
                } else null
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_colors_label)) {
            SettingsPreference(
                icon = com.bisc.flauncher.ui.home.PictogramCatalog.iconFor(target.iconKey) ?: Icons.Filled.Place,
                title = stringResource(R.string.settings_element_icon_label),
                onClick = { showIconPicker = true },
                trailing = null
            )
            AutomaticColorPicker(
                title = stringResource(R.string.settings_section_global_background),
                custom = target.backgroundColor,
                automaticSeed = android.graphics.Color.DKGRAY,
                onChange = { viewModel.updateMapTarget(target.copy(backgroundColor = it)) },
                ownGroup = false
            )
            AutomaticColorPicker(
                title = stringResource(R.string.settings_element_text_color_label),
                custom = target.iconColor,
                automaticSeed = android.graphics.Color.WHITE,
                onChange = { viewModel.updateMapTarget(target.copy(iconColor = it)) },
                ownGroup = false
            )
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.DeleteOutline,
                title = stringResource(R.string.settings_elements_delete_address),
                onClick = { confirmingDelete = true },
                trailing = null
            )
        }
    }

    if (showIconPicker) {
        PictogramPickerSheet(
            selected = target.iconKey,
            onSelect = { key -> viewModel.updateMapTarget(target.copy(iconKey = key)); showIconPicker = false },
            onDismiss = { showIconPicker = false }
        )
    }

    if (showAppPicker) {
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app -> viewModel.updateMapTarget(target.copy(navigationApp = app.componentName.packageName)); showAppPicker = false },
            onDismiss = { showAppPicker = false }
        )
    }

    if (confirmingDelete) {
        ConfirmRemoveDialog(
            label = target.label,
            onConfirm = { viewModel.removeMapTarget(target); confirmingDelete = false; onBack() },
            onDismiss = { confirmingDelete = false }
        )
    }
}
