package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.bisc.flauncher.data.model.DockPowerButtonAction
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.DockCornerStyle
import com.bisc.flauncher.ui.home.dock.DockSlotUi
import com.bisc.flauncher.ui.home.dock.DockWidthMode
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet
import com.bisc.flauncher.ui.home.dock.defaultIcon

/**
 * Everything about the dock, consolidated in one place. The dock is a flexible-length list of slots now
 * (no more fixed 4) — each either a pinned app or a power button (a destination like text search, replacing
 * what the old single Hub orb used to dispatch by gesture). Per-slot background/icon color overrides exist
 * in the data model ([HomeViewModel.setDockSlotColors]) but aren't exposed here yet — this screen covers
 * add/remove/reorder/assign, the bar's own background (Layer 2, same per-row appearance every dock-style
 * row has) and its corner style.
 */
@Composable
fun SettingsDockScreen(
    navController: NavController,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    initialEditSlot: Int? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val dockSlots by viewModel.dockSlots.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val cornerStyle by viewModel.dockCornerStyle.collectAsState()
    val widthMode by viewModel.dockWidthMode.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val homePage = pages.find { it.isHome }
    val dockRow = homePage?.let { rowsByPage[it.id] }?.find { it.type == PageRowType.SEARCH_BAR }

    var pickingAppForSlot by remember { mutableStateOf<Int?>(null) }
    var pickingPowerButtonForSlot by remember { mutableStateOf<Int?>(null) }
    var addingPowerButton by remember { mutableStateOf(false) }
    var showBackgroundSheet by remember { mutableStateOf(false) }
    var pickingCornerStyle by remember { mutableStateOf(false) }
    var pickingWidthMode by remember { mutableStateOf(false) }

    // Deep link from the home-screen dock's "+" (see HomeScreen's onUnsetAppClick): jump straight into
    // this slot's picker instead of just landing on the dock screen. Guarded so it only fires once — the
    // slot list keeps changing after the pick (the whole point), which would otherwise reopen it forever.
    var consumedInitialEditSlot by remember { mutableStateOf(false) }
    LaunchedEffect(dockSlots) {
        if (!consumedInitialEditSlot && initialEditSlot != null) {
            dockSlots.find { it.index == initialEditSlot }?.let { slot ->
                consumedInitialEditSlot = true
                when (slot) {
                    is DockSlotUi.App -> pickingAppForSlot = slot.index
                    is DockSlotUi.PowerButton -> pickingPowerButtonForSlot = slot.index
                }
            }
        }
    }

    SettingsSubScreen(title = stringResource(R.string.settings_section_dock), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_dock_slots_label)) {
            dockSlots.forEach { slot ->
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_dock_slot_title, slot.index + 1),
                    summary = dockSlotSummary(slot),
                    onClick = {
                        when (slot) {
                            is DockSlotUi.App -> pickingAppForSlot = slot.index
                            is DockSlotUi.PowerButton -> pickingPowerButtonForSlot = slot.index
                        }
                    },
                    trailing = {
                        Row {
                            IconButton(onClick = { viewModel.moveDockSlot(slot.index, -1) }) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.settings_dock_slot_move_up))
                            }
                            IconButton(onClick = { viewModel.moveDockSlot(slot.index, 1) }) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.settings_dock_slot_move_down))
                            }
                            IconButton(onClick = { viewModel.removeDockSlot(slot.index) }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.settings_dock_slot_remove))
                            }
                        }
                    }
                )
            }
        }

        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_dock_add_app_slot),
                onClick = viewModel::addDockAppSlot,
                trailing = null
            )
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_dock_add_power_button_slot),
                onClick = { addingPowerButton = true },
                trailing = null
            )
        }

        if (dockRow != null) {
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = layerBackgroundLabel(2, R.string.settings_layer_surface_dock),
                    onClick = { showBackgroundSheet = true },
                    trailing = null
                )
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_dock_corner_style_label),
                    summary = dockCornerStyleLabel(cornerStyle),
                    onClick = { pickingCornerStyle = true },
                    trailing = null
                )
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_dock_width_mode_label),
                    summary = dockWidthModeLabel(widthMode),
                    onClick = { pickingWidthMode = true },
                    trailing = null
                )
            }
        }

        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.AutoMirrored.Filled.ViewList,
                title = stringResource(R.string.settings_section_search_overview),
                summary = stringResource(R.string.settings_card_search_overview_subtitle),
                onClick = { onNavigate("dock/overview") }
            )
            SettingsPreference(
                icon = Icons.Filled.Search,
                title = stringResource(R.string.settings_section_search_full),
                summary = stringResource(R.string.settings_card_search_full_subtitle),
                onClick = { onNavigate("dock/full") }
            )
        }
    }

    pickingAppForSlot?.let { index ->
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app ->
                viewModel.setDockAppSlot(index, app.componentName)
                pickingAppForSlot = null
            },
            onDismiss = { pickingAppForSlot = null }
        )
    }

    pickingPowerButtonForSlot?.let { index ->
        PowerButtonActionSheet(
            onSelected = { action ->
                viewModel.setDockPowerButtonSlot(index, action)
                pickingPowerButtonForSlot = null
            },
            onDismiss = { pickingPowerButtonForSlot = null }
        )
    }

    if (addingPowerButton) {
        PowerButtonActionSheet(
            onSelected = { action ->
                viewModel.addDockPowerButtonSlot(action)
                addingPowerButton = false
            },
            onDismiss = { addingPowerButton = false }
        )
    }

    if (showBackgroundSheet && dockRow != null) {
        val layerLabel = layerBackgroundLabel(2, R.string.settings_layer_surface_dock)
        SettingsEditPage(title = layerLabel, onDismiss = { showBackgroundSheet = false }) {
            ElementAppearanceSection(
                appearance = dockRow.appearance(),
                onAppearanceChange = { appearance -> viewModel.setRowAppearance(dockRow, appearance) },
                styleGroupTitle = layerLabel
            )
        }
    }

    if (pickingCornerStyle) {
        DockCornerStyleSheet(
            current = cornerStyle,
            onSelected = { style -> viewModel.setDockCornerStyle(style); pickingCornerStyle = false },
            onDismiss = { pickingCornerStyle = false }
        )
    }

    if (pickingWidthMode) {
        DockWidthModeSheet(
            current = widthMode,
            onSelected = { mode -> viewModel.setDockWidthMode(mode); pickingWidthMode = false },
            onDismiss = { pickingWidthMode = false }
        )
    }
}

@Composable
private fun dockSlotSummary(slot: DockSlotUi): String = when (slot) {
    is DockSlotUi.App -> slot.label ?: stringResource(R.string.settings_dock_slot_not_set)
    is DockSlotUi.PowerButton -> dockPowerButtonActionLabel(slot.action)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PowerButtonActionSheet(onSelected: (DockPowerButtonAction) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            DockPowerButtonAction.entries.forEach { action ->
                SettingsPreference(
                    icon = action.defaultIcon(),
                    title = dockPowerButtonActionLabel(action),
                    onClick = { onSelected(action) },
                    trailing = null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DockCornerStyleSheet(current: DockCornerStyle, onSelected: (DockCornerStyle) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            DockCornerStyle.entries.forEach { style ->
                RadioPreference(dockCornerStyleLabel(style), style, current, onSelected)
            }
        }
    }
}

@Composable
private fun dockPowerButtonActionLabel(action: DockPowerButtonAction): String = when (action) {
    DockPowerButtonAction.TEXT_SEARCH -> stringResource(R.string.settings_dock_power_button_text_search)
    DockPowerButtonAction.CATEGORY_SEARCH -> stringResource(R.string.settings_dock_power_button_category_search)
    DockPowerButtonAction.APP_DRAWER -> stringResource(R.string.settings_dock_power_button_app_drawer)
    DockPowerButtonAction.APP_LIST -> stringResource(R.string.settings_dock_power_button_app_list)
}

@Composable
private fun dockCornerStyleLabel(style: DockCornerStyle): String = when (style) {
    DockCornerStyle.SHARP -> stringResource(R.string.settings_dock_corner_style_sharp)
    DockCornerStyle.ROUNDED -> stringResource(R.string.settings_dock_corner_style_rounded)
    DockCornerStyle.FULLY_ROUND -> stringResource(R.string.settings_dock_corner_style_fully_round)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DockWidthModeSheet(current: DockWidthMode, onSelected: (DockWidthMode) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            DockWidthMode.entries.forEach { mode ->
                RadioPreference(dockWidthModeLabel(mode), mode, current, onSelected)
            }
        }
    }
}

@Composable
private fun dockWidthModeLabel(mode: DockWidthMode): String = when (mode) {
    DockWidthMode.FIXED -> stringResource(R.string.settings_dock_width_mode_fixed)
    DockWidthMode.GROW -> stringResource(R.string.settings_dock_width_mode_grow)
}
