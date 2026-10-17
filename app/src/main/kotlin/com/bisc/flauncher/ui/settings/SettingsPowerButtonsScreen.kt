package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.bisc.flauncher.data.model.PowerButton
import com.bisc.flauncher.data.model.PowerButtonActionType
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.dock.PagePickerSheet
import com.bisc.flauncher.ui.pageLabel

/** Global "Powerbuttons" Element list (Settings -> Elements -> Power buttons) — a [PowerButton]
 *  opens a page, or the Panel/Dock directly, and is placeable anywhere a
 *  [com.bisc.flauncher.data.model.GridItemType.POWER_BUTTON] entry is allowed. Mirrors
 *  [SettingsAddressesScreen]'s list+create-dialog+navigate-to-detail pattern; "create" here is a
 *  name-only dialog (matching [SettingsFolderScreen]'s create flow) since the action/colors are
 *  more naturally picked on the detail screen right after. */
@Composable
fun SettingsPowerButtonsScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val buttons by viewModel.powerButtons.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_elements_powerbuttons_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_powerbuttons_title)) {
            if (buttons.isEmpty()) {
                SettingsPreference(icon = null, title = stringResource(R.string.settings_elements_powerbuttons_empty), onClick = {}, trailing = null)
            }
            buttons.sortedBy { it.label }.forEach { button ->
                SettingsPreference(
                    icon = powerButtonActionIcon(button.actionType),
                    title = button.label,
                    onClick = { navController.navigate("settings/elements/powerbuttons/${button.id}") }
                )
            }
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_elements_add_powerbutton),
                onClick = { showCreateDialog = true },
                trailing = null
            )
        }
    }

    if (showCreateDialog) {
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.settings_elements_add_powerbutton)) },
            text = {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_elements_powerbutton_name_hint)) }
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = label.isNotBlank(),
                    onClick = {
                        viewModel.addPowerButton(label.trim()) { id -> navController.navigate("settings/elements/powerbuttons/$id") }
                        showCreateDialog = false
                    }
                ) { Text(stringResource(R.string.widget_notes_save)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }
}

internal fun powerButtonActionIcon(type: PowerButtonActionType) = when (type) {
    PowerButtonActionType.PANEL -> Icons.Filled.Dashboard
    PowerButtonActionType.DOCK -> Icons.Filled.Apps
    PowerButtonActionType.PAGE -> Icons.Filled.ViewCarousel
}

/** Picks an *existing* [PowerButton] for a row's "+" slot — unlike the Addresses/Links pickers,
 *  there's no "create new" shortcut here (a power button's action/colors are involved enough that
 *  it's picked up front via Settings -> Elements -> Power buttons instead, same reasoning as why
 *  apps are picked from the device's existing list rather than created on the spot). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerButtonPickerSheet(buttons: List<PowerButton>, onSelect: (PowerButton) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (buttons.isEmpty()) {
                Text(
                    stringResource(R.string.settings_elements_powerbuttons_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            buttons.forEach { button ->
                SettingsPreference(
                    icon = powerButtonActionIcon(button.actionType),
                    title = button.label,
                    onClick = { onSelect(button) },
                    trailing = null
                )
            }
        }
    }
}

/** Detail/edit screen for one [PowerButton], reached by id — see [SettingsAddressDetailScreen] for
 *  the same pattern applied to addresses. */
@Composable
fun SettingsPowerButtonDetailScreen(buttonId: Long, navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val buttons by viewModel.powerButtons.collectAsState()
    val button = buttons.find { it.id == buttonId } ?: return
    val allPages by viewModel.allPages.collectAsState()
    var showPagePicker by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    SettingsSubScreen(title = button.label, navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            OutlinedTextField(
                value = button.label,
                onValueChange = { viewModel.updatePowerButton(button.copy(label = it)) },
                label = { Text(stringResource(R.string.settings_elements_powerbutton_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_powerbutton_action_label)) {
            RadioPreference(
                stringResource(R.string.settings_elements_powerbutton_action_panel),
                PowerButtonActionType.PANEL,
                button.actionType,
                { viewModel.updatePowerButton(button.copy(actionType = it, actionPageId = null)) }
            )
            RadioPreference(
                stringResource(R.string.settings_elements_powerbutton_action_dock),
                PowerButtonActionType.DOCK,
                button.actionType,
                { viewModel.updatePowerButton(button.copy(actionType = it, actionPageId = null)) }
            )
            RadioPreference(
                stringResource(R.string.settings_elements_powerbutton_action_page),
                PowerButtonActionType.PAGE,
                button.actionType,
                { viewModel.updatePowerButton(button.copy(actionType = it)) }
            )
            if (button.actionType == PowerButtonActionType.PAGE) {
                val targetPage = allPages.find { it.id == button.actionPageId }
                SettingsPreference(
                    icon = null,
                    title = targetPage?.let { pageLabel(it, allPages) } ?: stringResource(R.string.settings_elements_powerbutton_pick_page),
                    onClick = { showPagePicker = true },
                    trailing = null
                )
            }
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_powerbutton_colors_label)) {
            AutomaticColorPicker(
                title = stringResource(R.string.settings_section_global_background),
                custom = button.backgroundColor,
                automaticSeed = android.graphics.Color.DKGRAY,
                onChange = { viewModel.updatePowerButton(button.copy(backgroundColor = it)) }
            )
            AutomaticColorPicker(
                title = stringResource(R.string.settings_element_text_color_label),
                custom = button.iconColor,
                automaticSeed = android.graphics.Color.WHITE,
                onChange = { viewModel.updatePowerButton(button.copy(iconColor = it)) }
            )
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.DeleteOutline,
                title = stringResource(R.string.settings_elements_delete_powerbutton),
                onClick = { confirmingDelete = true },
                trailing = null
            )
        }
    }

    if (showPagePicker) {
        PagePickerSheet(
            pages = allPages,
            onPageSelected = { page -> viewModel.updatePowerButton(button.copy(actionPageId = page.id)); showPagePicker = false },
            onDismiss = { showPagePicker = false }
        )
    }

    if (confirmingDelete) {
        ConfirmRemoveDialog(
            label = button.label,
            onConfirm = { viewModel.removePowerButton(button); confirmingDelete = false; onBack() },
            onDismiss = { confirmingDelete = false }
        )
    }
}
