package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppListDisplayMode
import com.bisc.flauncher.data.model.AppListLayout
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.home.search.automaticRailAppearance

private enum class OverviewElementSheet { LIST_BACKGROUND, HEADER_LETTERS, ALPHABET_LETTERS, RAIL_BOX, SEARCH_BUTTON }

/** Everything that shapes the app-overview page. */
@Composable
fun ColumnScope.AppOverviewSettingsSection(surfaceIsDark: Boolean, viewModel: SettingsViewModel = hiltViewModel()) {
    val showAzIndex by viewModel.searchShowAzIndex.collectAsState()
    val showRailBar by viewModel.searchIndexShowRailBar.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val displayMode by viewModel.appListDisplayMode.collectAsState()
    val layout by viewModel.appListLayout.collectAsState()
    val searchButtonAppearance by viewModel.appListSearchButtonAppearance.collectAsState()
    val showSearchButton by viewModel.appListShowSearchButton.collectAsState()
    val contentAppearance by viewModel.appListContentAppearance.collectAsState()
    val headerLetterAppearance by viewModel.searchHeaderLetterAppearance.collectAsState()
    val alphabetLetterAppearance by viewModel.searchAlphabetLetterAppearance.collectAsState()
    val railAppearance by viewModel.searchAlphabetRailAppearance.collectAsState()
    val handedness by viewModel.appOverviewHandedness.collectAsState()
    var openSheet by remember { mutableStateOf<OverviewElementSheet?>(null) }

    SettingsPreferenceGroup(title = stringResource(R.string.settings_search_app_list_mode_label)) {
        RadioPreference(
            label = stringResource(R.string.settings_search_app_list_mode_fullscreen),
            value = AppListDisplayMode.FULLSCREEN,
            selected = displayMode,
            onSelect = viewModel::setAppListDisplayMode
        )
        RadioPreference(
            label = stringResource(R.string.settings_search_app_list_mode_window),
            value = AppListDisplayMode.WINDOW,
            selected = displayMode,
            onSelect = viewModel::setAppListDisplayMode
        )
    }

    SettingsPreferenceGroup(title = stringResource(R.string.settings_app_list_style_label)) {
        RadioPreference(
            label = stringResource(R.string.settings_app_list_style_list),
            value = AppListLayout.LIST,
            selected = layout,
            onSelect = viewModel::setAppListLayout
        )
        RadioPreference(
            label = stringResource(R.string.settings_app_list_style_tiles),
            value = AppListLayout.TILES,
            selected = layout,
            onSelect = viewModel::setAppListLayout
        )
    }

    SettingsPreferenceGroup(title = stringResource(R.string.settings_app_overview_alignment_label)) {
        RadioPreference(
            label = stringResource(R.string.settings_app_overview_align_left),
            value = Handedness.LEFT,
            selected = handedness,
            onSelect = viewModel::setAppOverviewHandedness
        )
        RadioPreference(
            label = stringResource(R.string.settings_app_overview_align_right),
            value = Handedness.RIGHT,
            selected = handedness,
            onSelect = viewModel::setAppOverviewHandedness
        )
    }

    SettingsPreferenceGroup(title = stringResource(R.string.settings_app_overview_elements_label)) {
        SettingsPreference(
            icon = null,
            title = surfaceLabel(R.string.settings_layer_surface_app_list),
            onClick = { openSheet = OverviewElementSheet.LIST_BACKGROUND },
            trailing = null
        )
        SettingsPreference(
            icon = null,
            title = surfaceLabel(R.string.settings_layer_surface_section_letter),
            onClick = { openSheet = OverviewElementSheet.HEADER_LETTERS },
            trailing = null
        )
        SettingsPreference(
            icon = null,
            title = surfaceLabel(R.string.settings_layer_surface_alphabet_letter),
            onClick = { openSheet = OverviewElementSheet.ALPHABET_LETTERS },
            trailing = null
        )
        SettingsPreference(
            icon = null,
            title = surfaceLabel(R.string.settings_layer_surface_search_button),
            onClick = { openSheet = OverviewElementSheet.SEARCH_BUTTON },
            trailing = null
        )
        SettingsPreference(
            icon = null,
            title = surfaceLabel(R.string.settings_layer_surface_alphabet_rail),
            onClick = { openSheet = OverviewElementSheet.RAIL_BOX },
            trailing = null
        )
    }

    SettingsPreferenceGroup(
        title = stringResource(R.string.settings_search_full_browse_label),
        info = stringResource(R.string.settings_app_list_search_button_summary)
    ) {
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_search_show_az_index),
            onClick = { viewModel.setSearchShowAzIndex(!showAzIndex) },
            trailing = { Switch(checked = showAzIndex, onCheckedChange = viewModel::setSearchShowAzIndex) }
        )
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_search_index_show_rail_bar),
            onClick = { viewModel.setSearchIndexShowRailBar(!showRailBar) },
            trailing = { Switch(checked = showRailBar, onCheckedChange = viewModel::setSearchIndexShowRailBar) }
        )
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_app_list_search_button),
            onClick = { viewModel.setAppListShowSearchButton(!showSearchButton) },
            trailing = { Switch(checked = showSearchButton, onCheckedChange = viewModel::setAppListShowSearchButton) }
        )
    }

    ColorPickerSection(
        title = stringResource(R.string.settings_accent_color_row),
        info = stringResource(R.string.settings_accent_color_summary),
        color = accentColor,
        onColorChange = viewModel::setAccentColor
    )

    when (openSheet) {
        OverviewElementSheet.LIST_BACKGROUND -> SettingsEditPage(
            title = surfaceLabel(R.string.settings_layer_surface_app_list),
            onDismiss = { openSheet = null }
        ) {
            ElementAppearanceSection(
                appearance = contentAppearance,
                onAppearanceChange = viewModel::setAppListContentAppearance,
                styleGroupTitle = surfaceLabel(R.string.settings_layer_surface_app_list)
            )
        }
        OverviewElementSheet.HEADER_LETTERS -> SettingsEditPage(
            title = surfaceLabel(R.string.settings_layer_surface_section_letter),
            onDismiss = { openSheet = null }
        ) {
            ElementAppearanceSection(
                appearance = headerLetterAppearance,
                onAppearanceChange = viewModel::setSearchHeaderLetterAppearance,
                styleGroupTitle = surfaceLabel(R.string.settings_layer_surface_section_letter)
            )
        }
        OverviewElementSheet.ALPHABET_LETTERS -> SettingsEditPage(
            title = surfaceLabel(R.string.settings_layer_surface_alphabet_letter),
            onDismiss = { openSheet = null }
        ) {
            ElementAppearanceSection(
                appearance = alphabetLetterAppearance,
                onAppearanceChange = viewModel::setSearchAlphabetLetterAppearance,
                styleGroupTitle = surfaceLabel(R.string.settings_layer_surface_alphabet_letter)
            )
        }
        OverviewElementSheet.SEARCH_BUTTON -> SettingsEditPage(
            title = surfaceLabel(R.string.settings_layer_surface_search_button),
            onDismiss = { openSheet = null }
        ) {
            ElementAppearanceSection(
                appearance = searchButtonAppearance ?: railAppearance ?: automaticRailAppearance(surfaceIsDark),
                onAppearanceChange = viewModel::setAppListSearchButtonAppearance,
                styleGroupTitle = surfaceLabel(R.string.settings_layer_surface_search_button)
            )
            if (searchButtonAppearance != null) {
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_search_button_follow_rail),
                        onClick = viewModel::resetAppListSearchButtonAppearance,
                        trailing = null
                    )
                }
            }
        }
        OverviewElementSheet.RAIL_BOX -> SettingsEditPage(
            title = surfaceLabel(R.string.settings_layer_surface_alphabet_rail),
            onDismiss = { openSheet = null }
        ) {
            ElementAppearanceSection(
                appearance = railAppearance ?: automaticRailAppearance(surfaceIsDark),
                onAppearanceChange = viewModel::setSearchAlphabetRailAppearance,
                styleGroupTitle = surfaceLabel(R.string.settings_layer_surface_alphabet_rail)
            )
            if (railAppearance != null) {
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_element_color_reset),
                        onClick = viewModel::resetSearchAlphabetRailAppearance,
                        trailing = null
                    )
                }
            }
        }
        null -> Unit
    }
}
