package com.bisc.flauncher.ui.settings

import com.bisc.flauncher.data.model.face
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
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

private fun buttonKey(id: Long) = "button:$id"

/**
 * Every action button in one card stack: ⓪ all of them with a way to add one, then each button
 * with its options in two tabs, the button and its symbol. Several can be changed and saved at once.
 */
@Composable
fun SettingsActionButtonsScreen(navController: NavController, onBack: () -> Unit, startElement: String? = null, viewModel: SettingsViewModel = hiltViewModel()) {
    val home: com.bisc.flauncher.ui.home.HomeViewModel = hiltViewModel()
    val stack: StackDraft = androidx.lifecycle.viewmodel.compose.viewModel()
    val buttons by viewModel.actionButtons.collectAsState()
    var adding by remember { mutableStateOf(false) }
    var openAfterAdd by remember { mutableStateOf<String?>(null) }
    val sorted = buttons.sortedBy { it.label.lowercase() }
    fun shown(b: ActionButton): ActionButton {
        val item = DraftField(stack, buttonKey(b.id) + ":item", b.copy(tileFace = null)).value
        return item.copy(tileFace = DraftField(stack, buttonKey(b.id) + ":face", b.tileFace).value)
    }

    val elements = buildList {
        add(StackElement(
            id = "all", number = 0,
            title = stringResource(R.string.settings_elements_actionbuttons_title),
            summary = androidx.compose.ui.res.pluralStringResource(R.plurals.hub_content_count, buttons.size, buttons.size)
        ) {
            PartIntro(stringResource(R.string.stack_buttons_intro))
            StackAddRow(stringResource(R.string.settings_elements_add_actionbutton)) { adding = true }
        })
        sorted.forEachIndexed { i, b ->
            val s = shown(b)
            add(StackElement(
                id = buttonKey(b.id), number = i + 1,
                title = s.label,
                draftKeys = listOf(buttonKey(b.id) + ":item", buttonKey(b.id) + ":face"),
                anchor = androidx.compose.ui.geometry.Offset(1f, 0f),
                leading = { ElementTile(s.face(), s.label, 32.dp, MaterialTheme.colorScheme.onSurface, actionButtonActionIcon(s.actionType)) }
            ) { ActionButtonEditor(b, stack, navController, home, viewModel) })
        }
    }

    ElementCollectionScreen(
        title = stringResource(R.string.settings_elements_actionbuttons_title),
        navController = navController,
        onBack = onBack,
        startElement = startElement ?: openAfterAdd,
        draft = stack,
        home = home,
        elements = elements,
        items = sorted.map { b -> val s = shown(b); CollectionItem(buttonKey(b.id), s.label) { size, ink -> ElementTile(s.face(), s.label, size, ink, actionButtonActionIcon(s.actionType)) } },
        listHeader = { StackAddRow(stringResource(R.string.settings_elements_add_actionbutton)) { adding = true } }
    )

    if (adding) {
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
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
                        // Straight on to the new one, to pick what it does.
                        viewModel.addActionButton(label.trim()) { id -> navController.navigate("settings/elements/actionbuttons/$id") }
                        adding = false
                    }
                ) { Text(stringResource(R.string.widget_notes_save)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { adding = false }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }
}

/** One action button, the way in from outside: the stack, opened on it. */
@Composable
fun SettingsActionButtonDetailScreen(buttonId: Long, navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    SettingsActionButtonsScreen(navController, onBack, startElement = buttonKey(buttonId), viewModel = viewModel)
}

/** A button's options: name and what it does, where it is used; and its symbol. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ActionButtonEditor(
    saved: ActionButton,
    stack: StackDraft,
    navController: NavController,
    home: com.bisc.flauncher.ui.home.HomeViewModel,
    viewModel: SettingsViewModel
) {
    val key = buttonKey(saved.id)
    val item = stack.field("$key:item", saved.copy(tileFace = null)) {}
    val face = stack.field("$key:face", saved.tileFace) {}
    val button = item.value.copy(tileFace = face.value)
    stack.saveTogether(key) { if (button != saved) viewModel.updateActionButton(button) }
    val allPages by viewModel.allPages.collectAsState()
    var showPagePicker by remember { mutableStateOf(false) }
    var showSettingPicker by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val usages = rememberUsages(com.bisc.flauncher.data.model.GridItemType.ACTION_BUTTON, saved.id.toString(), home)
    fun set(change: (ActionButton) -> ActionButton) { item.value = change(item.value) }

    StackTabs(listOf(
        stringResource(R.string.stack_el_action_button) to {
            SettingsPreferenceGroup {
                OutlinedTextField(
                    value = button.label,
                    onValueChange = { v -> set { it.copy(label = v) } },
                    label = { Text(stringResource(R.string.settings_elements_actionbutton_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_actionbutton_action_label)) {
                RadioPreference(stringResource(R.string.settings_elements_actionbutton_action_panel), ActionButtonActionType.PANEL, button.actionType, { t -> set { it.copy(actionType = t, actionPageId = null) } })
                RadioPreference(stringResource(R.string.settings_elements_actionbutton_action_dock), ActionButtonActionType.DOCK, button.actionType, { t -> set { it.copy(actionType = t, actionPageId = null) } })
                RadioPreference(stringResource(R.string.settings_elements_actionbutton_action_page), ActionButtonActionType.PAGE, button.actionType, { t -> set { it.copy(actionType = t) } })
                if (button.actionType == ActionButtonActionType.PAGE) {
                    val targetPage = allPages.find { it.id == button.actionPageId }
                    SettingsPreference(
                        icon = null,
                        title = targetPage?.let { pageLabel(it, allPages) } ?: stringResource(R.string.settings_elements_actionbutton_pick_page),
                        onClick = { showPagePicker = true },
                        trailing = null
                    )
                }
                RadioPreference(stringResource(R.string.settings_elements_actionbutton_action_setting), ActionButtonActionType.ANDROID_SETTING, button.actionType, { t -> set { it.copy(actionType = t, actionPageId = null) } })
                if (button.actionType == ActionButtonActionType.ANDROID_SETTING) {
                    val targetShortcut = SettingsShortcuts.all.find { it.action == button.androidSettingAction }
                    SettingsPreference(
                        icon = null,
                        title = targetShortcut?.let { stringResource(it.labelRes) } ?: stringResource(R.string.settings_elements_actionbutton_pick_setting),
                        onClick = { showSettingPicker = true },
                        trailing = null
                    )
                }
                RadioPreference(stringResource(R.string.settings_elements_actionbutton_action_launcher_settings), ActionButtonActionType.LAUNCHER_SETTINGS, button.actionType, { t -> set { it.copy(actionType = t, actionPageId = null) } })
            }
            UsagesGroup(usages, navController)
            SettingsPreferenceGroup {
                SettingsPreference(icon = Icons.Filled.DeleteOutline, title = stringResource(R.string.settings_elements_delete_actionbutton), onClick = { confirmingDelete = true }, trailing = null)
            }
        },
        stringResource(R.string.stack_el_symbol) to {
            PartIntro(stringResource(R.string.element_tile_info))
            TileFaceEditor(
                face = button.face(),
                onChange = { face.value = it.toJson() },
                autoSurface = com.bisc.flauncher.ui.home.dock.UNSET_APP_SLOT_COLOR,
                letter = button.label,
                content = emptyList(),
                contentSummary = "",
                viewModel = viewModel,
                kinds = listOf(com.bisc.flauncher.data.model.TileKind.SYMBOL, com.bisc.flauncher.data.model.TileKind.LETTER),
                defaultSymbol = actionButtonActionIcon(button.actionType)
            )
        }
    ))

    if (showPagePicker) {
        PagePickerSheet(
            pages = allPages,
            onPageSelected = { page -> set { it.copy(actionPageId = page.id) }; showPagePicker = false },
            onDismiss = { showPagePicker = false }
        )
    }
    if (showSettingPicker) {
        AndroidSettingPickerSheet(
            onSelect = { shortcut -> set { it.copy(androidSettingAction = shortcut.action) }; showSettingPicker = false },
            onDismiss = { showSettingPicker = false }
        )
    }
    if (confirmingDelete) {
        ConfirmRemoveDialog(
            label = button.label,
            onConfirm = { viewModel.removeActionButton(saved); confirmingDelete = false },
            onDismiss = { confirmingDelete = false }
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

/** Picks an *existing* [ActionButton] for a row's "+" slot - unlike the Addresses/Links pickers,
 *  there's no "create new" shortcut here (an action button's action/colors are involved enough
 *  that it's picked up front via Settings -> Elements -> Action buttons instead, same reasoning as
 *  why apps are picked from the device's existing list rather than created on the spot). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionButtonPickerSheet(buttons: List<ActionButton>, onSelect: (ActionButton) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = com.bisc.flauncher.ui.sheetMaxHeight())
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

/** Picks one of the curated [SettingsShortcuts] - the same preset list the search feature already
 *  surfaces its own settings results from, reused here rather than duplicated. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AndroidSettingPickerSheet(onSelect: (SettingsShortcut) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = com.bisc.flauncher.ui.sheetMaxHeight())
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
