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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.DockTapAction
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.DockOrbDesign
import com.bisc.flauncher.ui.home.dock.DockStyle
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.SearchButtonBorderColor
import com.bisc.flauncher.ui.theme.SearchButtonSurfaceColor

/** Everything about the dock pill, consolidated in one place. */
@Composable
fun SettingsDockScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val quickLaunchSlots by viewModel.quickLaunchSlots.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val singleTapAction by viewModel.dockSingleTapAction.collectAsState()
    val doubleTapAction by viewModel.dockDoubleTapAction.collectAsState()
    val matchWidgetCorners by viewModel.dockMatchWidgetCorners.collectAsState()
    val searchButtonAppearance by viewModel.dockSearchButtonAppearance.collectAsState()
    val dockStyle by viewModel.dockStyle.collectAsState()
    val dockOrbDesign by viewModel.dockOrbDesign.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val homePage = pages.find { it.isHome }
    val dockRow = homePage?.let { rowsByPage[it.id] }?.find { it.type == PageRowType.SEARCH_BAR }

    var pickingSlotIndex by remember { mutableStateOf<Int?>(null) }
    var showBackgroundSheet by remember { mutableStateOf(false) }
    var showSearchButtonSheet by remember { mutableStateOf(false) }
    var pickingTapAction by remember { mutableStateOf<DockTap?>(null) }
    var pickingDockStyle by remember { mutableStateOf(false) }
    var pickingOrbDesign by remember { mutableStateOf(false) }

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
                SettingsPreference(
                    icon = null,
                    title = layerBackgroundLabel(3, R.string.settings_layer_surface_search_button),
                    onClick = { showSearchButtonSheet = true },
                    trailing = null
                )
            }
        }

        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_dock_style_label),
                summary = dockStyleLabel(dockStyle),
                onClick = { pickingDockStyle = true },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_dock_orb_design_label),
                summary = dockOrbDesignLabel(dockOrbDesign),
                onClick = { pickingOrbDesign = true },
                trailing = null
            )
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

    if (showSearchButtonSheet && dockRow != null) {
        val layerLabel = layerBackgroundLabel(3, R.string.settings_layer_surface_search_button)
        SettingsEditPage(title = layerLabel, onDismiss = { showSearchButtonSheet = false }) {
            ElementAppearanceSection(
                appearance = searchButtonAppearance ?: automaticSearchButtonAppearance(dockRow.appearance()),
                onAppearanceChange = viewModel::setDockSearchButtonAppearance,
                styleGroupTitle = layerLabel
            )
            if (searchButtonAppearance != null) {
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_element_color_reset),
                        onClick = viewModel::resetDockSearchButtonAppearance,
                        trailing = null
                    )
                }
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

    if (pickingDockStyle) {
        ModalBottomSheetPicker(onDismiss = { pickingDockStyle = false }) {
            DockStyle.entries.forEach { style ->
                RadioPreference(dockStyleLabel(style), style, dockStyle, { viewModel.setDockStyle(it); pickingDockStyle = false })
            }
        }
    }

    if (pickingOrbDesign) {
        ModalBottomSheetPicker(onDismiss = { pickingOrbDesign = false }) {
            DockOrbDesign.entries.forEach { design ->
                RadioPreference(dockOrbDesignLabel(design), design, dockOrbDesign, { viewModel.setDockOrbDesign(it); pickingOrbDesign = false })
            }
        }
    }
}

@Composable
private fun dockStyleLabel(style: DockStyle): String = when (style) {
    DockStyle.PLAIN -> stringResource(R.string.settings_dock_style_plain)
    DockStyle.AURORA -> stringResource(R.string.settings_dock_style_aurora)
}

@Composable
private fun dockOrbDesignLabel(design: DockOrbDesign): String = when (design) {
    DockOrbDesign.DEFAULT -> stringResource(R.string.settings_dock_orb_design_default)
    DockOrbDesign.LEAF -> stringResource(R.string.settings_dock_orb_design_leaf)
    DockOrbDesign.DEW -> stringResource(R.string.settings_dock_orb_design_dew)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalBottomSheetPicker(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
    }
}

/** A starting point for the search button editor that matches its current automatic light/dark look. */
@Composable
private fun automaticSearchButtonAppearance(dockAppearance: ElementAppearance): ElementAppearance {
    val surfaceDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dockColors = resolveElementColors(dockAppearance, surfaceDark)
    val usesLightDetailing = dockAppearance.fillMode != ElementFillMode.NONE && dockColors.solidBackground.luminance() > 0.5f
    return if (usesLightDetailing) {
        ElementAppearance(
            fill = ColorFill(color = SearchButtonSurfaceColor.toArgb()),
            borderEnabled = true,
            borderColor = SearchButtonBorderColor.toArgb(),
            contentColor = SearchButtonBorderColor.copy(alpha = 0.8f).toArgb()
        )
    } else {
        ElementAppearance(
            fill = ColorFill(color = dockColors.contentColor.toArgb()),
            opacity = 0.12f,
            contentColor = dockColors.contentColor.toArgb()
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
