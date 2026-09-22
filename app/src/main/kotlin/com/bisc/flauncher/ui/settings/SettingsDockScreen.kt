package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
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
import com.bisc.flauncher.data.model.DockTapAction
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/** Everything about the dock pill, consolidated in one place. */
@Composable
fun SettingsDockScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val quickLaunchSlots by viewModel.quickLaunchSlots.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val singleTapAction by viewModel.dockSingleTapAction.collectAsState()
    val doubleTapAction by viewModel.dockDoubleTapAction.collectAsState()
    val matchWidgetCorners by viewModel.dockMatchWidgetCorners.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val homePage = pages.find { it.isHome }
    val dockRow = homePage?.let { rowsByPage[it.id] }?.find { it.type == PageRowType.SEARCH_BAR }

    var pickingSlotIndex by remember { mutableStateOf<Int?>(null) }
    var showBackgroundSheet by remember { mutableStateOf(false) }
    var pickingTapAction by remember { mutableStateOf<DockTap?>(null) }

    SettingsSubScreen(title = stringResource(R.string.settings_section_dock), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_dock_slots_label)) {
            quickLaunchSlots.forEach { slot ->
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_dock_slot_title, slot.index + 1),
                    summary = slot.label ?: stringResource(R.string.settings_dock_slot_not_set),
                    onClick = { pickingSlotIndex = slot.index },
                    trailing = if (slot.componentName != null) {
                        {
                            IconButton(onClick = { viewModel.setDockSlot(slot.index, null) }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.settings_dock_slot_clear))
                            }
                        }
                    } else {
                        null
                    }
                )
            }
        }

        if (dockRow != null) {
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = layerBackgroundLabel(2, R.string.settings_layer_surface_dock),
                    onClick = { showBackgroundSheet = true },
                    trailing = null
                )
            }
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_dock_tap_actions_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_dock_single_tap_label),
                summary = dockTapActionLabel(singleTapAction),
                onClick = { pickingTapAction = DockTap.SINGLE }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_dock_double_tap_label),
                summary = dockTapActionLabel(doubleTapAction),
                onClick = { pickingTapAction = DockTap.DOUBLE }
            )
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

    pickingSlotIndex?.let { index ->
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app ->
                viewModel.setDockSlot(index, app.componentName)
                pickingSlotIndex = null
            },
            onDismiss = { pickingSlotIndex = null }
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
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_dock_widget_corners),
                    info = stringResource(R.string.settings_dock_widget_corners_summary),
                    onClick = { viewModel.setDockMatchWidgetCorners(!matchWidgetCorners) },
                    trailing = { Switch(checked = matchWidgetCorners, onCheckedChange = viewModel::setDockMatchWidgetCorners) }
                )
            }
        }
    }

    pickingTapAction?.let { which ->
        val current = if (which == DockTap.SINGLE) singleTapAction else doubleTapAction
        DockTapActionSheet(
            current = current,
            onSelected = { action ->
                if (which == DockTap.SINGLE) viewModel.setDockSingleTapAction(action) else viewModel.setDockDoubleTapAction(action)
                pickingTapAction = null
            },
            onDismiss = { pickingTapAction = null }
        )
    }
}

private enum class DockTap { SINGLE, DOUBLE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DockTapActionSheet(current: DockTapAction, onSelected: (DockTapAction) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            DockTapAction.entries.forEach { action ->
                RadioPreference(dockTapActionLabel(action), action, current, onSelected)
            }
        }
    }
}

@Composable
private fun dockTapActionLabel(action: DockTapAction): String = when (action) {
    DockTapAction.NONE -> stringResource(R.string.settings_dock_tap_action_none)
    DockTapAction.QUICK_OVERVIEW -> stringResource(R.string.settings_dock_tap_action_overview)
    DockTapAction.FULL_SEARCH -> stringResource(R.string.settings_dock_tap_action_search)
}
