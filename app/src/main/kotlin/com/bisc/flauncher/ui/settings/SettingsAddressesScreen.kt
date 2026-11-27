package com.bisc.flauncher.ui.settings

import com.bisc.flauncher.data.model.face
import androidx.compose.material.icons.filled.Apps
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
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

/** Global "Adressen" Element list (Settings -> Elements -> Addresses) - every [SavedMapTarget] in
 *  one place, independent of which rows/folders currently pin it. Mirrors [SettingsFolderScreen]'s
 *  list+create-dialog+navigate-to-detail pattern. */
@Composable
fun SettingsAddressesScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val targets by viewModel.savedMapTargets.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_elements_addresses_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_addresses_title)) {
            if (targets.isEmpty()) {
                SettingsPreference(icon = null, title = stringResource(R.string.settings_elements_addresses_empty), onClick = null, trailing = null)
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

/** Detail/edit screen for one [SavedMapTarget], reached by id from [SettingsAddressesScreen] - same
 *  "hold only the id, re-derive the live item every recomposition" convention as the rest of
 *  Settings (e.g. [SettingsPageRowsScreen]). */
@Composable
fun SettingsAddressDetailScreen(targetId: Long, navController: NavController, onBack: () -> Unit, startElement: String? = null, viewModel: SettingsViewModel = hiltViewModel()) {
    val home: com.bisc.flauncher.ui.home.HomeViewModel = hiltViewModel()
    val stack: StackDraft = androidx.lifecycle.viewmodel.compose.viewModel()
    val targets by viewModel.savedMapTargets.collectAsState()
    val saved = targets.find { it.id == targetId } ?: return
    // What it is, and its symbol, each a value of its own; written together as one place.
    val item = stack.field("item", saved.copy(tileFace = null)) {}
    val face = stack.field("face", saved.tileFace) {}
    val target = item.value.copy(tileFace = face.value)
    stack.saveTogether { if (target != saved) viewModel.updateMapTarget(target) }
    val browsableApps by viewModel.labeledApps.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val usages = rememberUsages(com.bisc.flauncher.data.model.GridItemType.MAP_TARGET, targetId.toString(), home)

    ElementStackScreen(
        title = target.label.ifBlank { saved.label },
        navController = navController,
        onBack = onBack,
        startElement = startElement,
        draft = stack,
        home = home,
        partTitle = stringResource(R.string.stack_el_address),
        partSummary = target.address.takeIf { it.isNotBlank() },
        partKeys = listOf("item"),
        symbolTitle = stringResource(R.string.stack_el_symbol),
        symbolKeys = listOf("face"),
        name = target.label,
        symbol = { size, ink -> ElementTile(target.face(), target.label, size, ink, Icons.Filled.Place) },
        partOptions = {
            SettingsPreferenceGroup {
                OutlinedTextField(
                    value = target.label,
                    onValueChange = { item.value = item.value.copy(label = it) },
                    label = { Text(stringResource(R.string.sidebar_maps_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutlinedTextField(
                    value = target.address,
                    onValueChange = { item.value = item.value.copy(address = it) },
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
                        { TextButton(onClick = { item.value = item.value.copy(navigationApp = null) }) { Text(stringResource(R.string.settings_elements_use_default)) } }
                    } else null
                )
            }
            UsagesGroup(usages, navController)
            SettingsPreferenceGroup {
                SettingsPreference(icon = Icons.Filled.DeleteOutline, title = stringResource(R.string.settings_elements_delete_address), onClick = { confirmingDelete = true }, trailing = null)
            }
        },
        symbolOptions = {
            PartIntro(stringResource(R.string.element_tile_info))
            TileFaceEditor(
                face = target.face(),
                onChange = { face.value = it.toJson() },
                autoSurface = com.bisc.flauncher.ui.home.dock.UNSET_APP_SLOT_COLOR,
                letter = target.label,
                content = emptyList(),
                contentSummary = "",
                viewModel = viewModel,
                kinds = listOf(com.bisc.flauncher.data.model.TileKind.SYMBOL, com.bisc.flauncher.data.model.TileKind.LETTER),
                defaultSymbol = Icons.Filled.Place
            )
        }
    )

    if (showAppPicker) {
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app -> item.value = item.value.copy(navigationApp = app.componentName.packageName); showAppPicker = false },
            onDismiss = { showAppPicker = false }
        )
    }

    if (confirmingDelete) {
        ConfirmRemoveDialog(
            label = target.label,
            onConfirm = { viewModel.removeMapTarget(saved); confirmingDelete = false; onBack() },
            onDismiss = { confirmingDelete = false }
        )
    }
}
