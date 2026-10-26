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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
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
import com.bisc.flauncher.data.model.ActionButton
import com.bisc.flauncher.data.model.ActionButtonActionType
import com.bisc.flauncher.data.model.SettingsShortcut
import com.bisc.flauncher.data.model.SettingsShortcuts
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.dock.PagePickerSheet
import com.bisc.flauncher.ui.pageLabel

/** Global "Action button" Element list (Settings -> Elements -> Action buttons) — an [ActionButton]
 *  opens a page, the Panel/Dock directly, or a curated Android settings screen, and is placeable
 *  anywhere a [com.bisc.flauncher.data.model.GridItemType.ACTION_BUTTON] entry is allowed. Mirrors
 *  [SettingsAddressesScreen]'s list+create-dialog+navigate-to-detail pattern; "create" here is a
 *  name-only dialog (matching [SettingsFolderScreen]'s create flow) since the action/colors are
 *  more naturally picked on the detail screen right after. */
@Composable
fun SettingsActionButtonsScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val buttons by viewModel.actionButtons.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_elements_actionbuttons_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_actionbuttons_title)) {
            if (buttons.isEmpty()) {
                SettingsPreference(icon = null, title = stringResource(R.string.settings_elements_actionbuttons_empty), onClick = {}, trailing = null)
            }
            buttons.sortedBy { it.label }.forEach { button ->
                SettingsPreference(
                    icon = actionButtonActionIcon(button.actionType),
                    title = button.label,
                    onClick = { navController.navigate("settings/elements/actionbuttons/${button.id}") }
                )
            }
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_elements_add_actionbutton),
                onClick = { showCreateDialog = true },
                trailing = null
            )
        }
    }

    if (showCreateDialog) {
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.settings_elements_add_actionbutton)) },
            text = {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_elements_actionbutton_name_hint)) }
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = label.isNotBlank(),
                    onClick = {
                        viewModel.addActionButton(label.trim()) { id -> navController.navigate("settings/elements/actionbuttons/$id") }
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

internal fun actionButtonActionIcon(type: ActionButtonActionType) = when (type) {
    ActionButtonActionType.PANEL -> Icons.Filled.Dashboard
    ActionButtonActionType.DOCK -> Icons.Filled.Apps
    ActionButtonActionType.PAGE -> Icons.Filled.ViewCarousel
    ActionButtonActionType.ANDROID_SETTING -> Icons.Filled.Settings
    ActionButtonActionType.LAUNCHER_SETTINGS -> Icons.Filled.Tune
}

/** Picks an *existing* [ActionButton] for a row's "+" slot — unlike the Addresses/Links pickers,
 *  there's no "create new" shortcut here (an action button's action/colors are involved enough
 *  that it's picked up front via Settings -> Elements -> Action buttons instead, same reasoning as
 *  why apps are picked from the device's existing list rather than created on the spot). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionButtonPickerSheet(buttons: List<ActionButton>, onSelect: (ActionButton) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (buttons.isEmpty()) {
                Text(
                    stringResource(R.string.settings_elements_actionbuttons_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            buttons.forEach { button ->
                SettingsPreference(
                    icon = actionButtonActionIcon(button.actionType),
                    title = button.label,
                    onClick = { onSelect(button) },
                    trailing = null
                )
            }
        }
    }
}

/** Detail/edit screen for one [ActionButton], reached by id — see [SettingsAddressDetailScreen] for
 *  the same pattern applied to addresses. */
@Composable
fun SettingsActionButtonDetailScreen(buttonId: Long, navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val buttons by viewModel.actionButtons.collectAsState()
    val button = buttons.find { it.id == buttonId } ?: return
    val allPages by viewModel.allPages.collectAsState()
    var showPagePicker by remember { mutableStateOf(false) }
    var showSettingPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    SettingsSubScreen(title = button.label, navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            OutlinedTextField(
                value = button.label,
                onValueChange = { viewModel.updateActionButton(button.copy(label = it)) },
                label = { Text(stringResource(R.string.settings_elements_actionbutton_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_actionbutton_action_label)) {
            RadioPreference(
                stringResource(R.string.settings_elements_actionbutton_action_panel),
                ActionButtonActionType.PANEL,
                button.actionType,
                { viewModel.updateActionButton(button.copy(actionType = it, actionPageId = null)) }
            )
            RadioPreference(
                stringResource(R.string.settings_elements_actionbutton_action_dock),
                ActionButtonActionType.DOCK,
                button.actionType,
                { viewModel.updateActionButton(button.copy(actionType = it, actionPageId = null)) }
            )
            RadioPreference(
                stringResource(R.string.settings_elements_actionbutton_action_page),
                ActionButtonActionType.PAGE,
                button.actionType,
                { viewModel.updateActionButton(button.copy(actionType = it)) }
            )
            if (button.actionType == ActionButtonActionType.PAGE) {
                val targetPage = allPages.find { it.id == button.actionPageId }
                SettingsPreference(
                    icon = null,
                    title = targetPage?.let { pageLabel(it, allPages) } ?: stringResource(R.string.settings_elements_actionbutton_pick_page),
                    onClick = { showPagePicker = true },
                    trailing = null
                )
            }
            RadioPreference(
                stringResource(R.string.settings_elements_actionbutton_action_setting),
                ActionButtonActionType.ANDROID_SETTING,
                button.actionType,
                { viewModel.updateActionButton(button.copy(actionType = it, actionPageId = null)) }
            )
            if (button.actionType == ActionButtonActionType.ANDROID_SETTING) {
                val targetShortcut = SettingsShortcuts.all.find { it.action == button.androidSettingAction }
                SettingsPreference(
                    icon = null,
                    title = targetShortcut?.let { stringResource(it.labelRes) } ?: stringResource(R.string.settings_elements_actionbutton_pick_setting),
                    onClick = { showSettingPicker = true },
                    trailing = null
                )
            }
            RadioPreference(
                stringResource(R.string.settings_elements_actionbutton_action_launcher_settings),
                ActionButtonActionType.LAUNCHER_SETTINGS,
                button.actionType,
                { viewModel.updateActionButton(button.copy(actionType = it, actionPageId = null)) }
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_colors_label)) {
            SettingsPreference(
                icon = com.bisc.flauncher.ui.home.PictogramCatalog.iconFor(button.iconKey) ?: actionButtonActionIcon(button.actionType),
                title = stringResource(R.string.settings_element_icon_label),
                onClick = { showIconPicker = true },
                trailing = null
            )
            AutomaticColorPicker(
                title = stringResource(R.string.settings_section_global_background),
                custom = button.backgroundColor,
                automaticSeed = android.graphics.Color.DKGRAY,
                onChange = { viewModel.updateActionButton(button.copy(backgroundColor = it)) },
                ownGroup = false
            )
            AutomaticColorPicker(
                title = stringResource(R.string.settings_element_text_color_label),
                custom = button.iconColor,
                automaticSeed = android.graphics.Color.WHITE,
                onChange = { viewModel.updateActionButton(button.copy(iconColor = it)) },
                ownGroup = false
            )
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.DeleteOutline,
                title = stringResource(R.string.settings_elements_delete_actionbutton),
                onClick = { confirmingDelete = true },
                trailing = null
            )
        }
    }

    if (showPagePicker) {
        PagePickerSheet(
            pages = allPages,
            onPageSelected = { page -> viewModel.updateActionButton(button.copy(actionPageId = page.id)); showPagePicker = false },
            onDismiss = { showPagePicker = false }
        )
    }

    if (showSettingPicker) {
        AndroidSettingPickerSheet(
            onSelect = { shortcut -> viewModel.updateActionButton(button.copy(androidSettingAction = shortcut.action)); showSettingPicker = false },
            onDismiss = { showSettingPicker = false }
        )
    }

    if (showIconPicker) {
        PictogramPickerSheet(
            selected = button.iconKey,
            onSelect = { key -> viewModel.updateActionButton(button.copy(iconKey = key)); showIconPicker = false },
            onDismiss = { showIconPicker = false }
        )
    }

    if (confirmingDelete) {
        ConfirmRemoveDialog(
            label = button.label,
            onConfirm = { viewModel.removeActionButton(button); confirmingDelete = false; onBack() },
            onDismiss = { confirmingDelete = false }
        )
    }
}

/** Picks one of the curated [SettingsShortcuts] — the same preset list the search feature already
 *  surfaces its own settings results from, reused here rather than duplicated. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AndroidSettingPickerSheet(onSelect: (SettingsShortcut) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsShortcuts.all.forEach { shortcut ->
                SettingsPreference(
                    icon = Icons.Filled.Settings,
                    title = stringResource(shortcut.labelRes),
                    onClick = { onSelect(shortcut) },
                    trailing = null
                )
            }
        }
    }
}
