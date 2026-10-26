package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.search.automaticRailAppearance

/** Hub for the app-list page's own settings — one entry point per element (Appearance, Section
 *  letters, A–Z index), each its own screen, instead of one long flat page where it's impossible
 *  to tell which card belongs to which element while scrolling. Mirrors [SettingsDockScreen]. */
@Composable
fun SettingsAppOverviewScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val showAzIndex by viewModel.appListShowAzIndex.collectAsState()
    val showSearchButton by viewModel.appListShowSearchButton.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val handedness by viewModel.appOverviewHandedness.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_section_app_overview), navController = navController, onBack = onBack) {
        Text(
            stringResource(R.string.settings_page_app_overview_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_page_appearance_title),
                onClick = { onNavigate("appearance") },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_overview_alignment_label),
                onClick = {
                    viewModel.setAppOverviewHandedness(if (handedness == Handedness.LEFT) Handedness.RIGHT else Handedness.LEFT)
                },
                trailing = { HandednessToggle(selected = handedness, onSelect = viewModel::setAppOverviewHandedness) }
            )
            SettingsPreference(
                icon = null,
                title = surfaceLabel(R.string.settings_layer_surface_section_letter),
                onClick = { onNavigate("section_letters") },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_overview_az_index_label),
                onClick = { viewModel.setAppListShowAzIndex(!showAzIndex) },
                trailing = { Switch(checked = showAzIndex, onCheckedChange = viewModel::setAppListShowAzIndex) },
                extra = {
                    IconButton(onClick = { onNavigate("az_index") }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = stringResource(R.string.settings_more_settings_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_list_search_button),
                info = stringResource(R.string.settings_app_list_search_button_summary),
                onClick = { viewModel.setAppListShowSearchButton(!showSearchButton) },
                trailing = { Switch(checked = showSearchButton, onCheckedChange = viewModel::setAppListShowSearchButton) }
            )
            ColorPickerSection(
                title = stringResource(R.string.settings_accent_color_row),
                info = stringResource(R.string.settings_accent_color_summary),
                color = accentColor,
                onColorChange = viewModel::setAccentColor,
                ownGroup = false
            )
        }
    }
}

/** Compact Left | Right switch for a row's trailing slot — replaces the two separate radio rows. */
@Composable
private fun HandednessToggle(selected: Handedness, onSelect: (Handedness) -> Unit) {
    val options = listOf(
        Handedness.LEFT to stringResource(R.string.settings_app_overview_align_left),
        Handedness.RIGHT to stringResource(R.string.settings_app_overview_align_right)
    )
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {}
            ) {
                Text(label)
            }
        }
    }
}

/** The app-list page's own background and status-bar switch — same pairing as
 *  [SettingsPageAppearanceScreen]. */
@Composable
fun SettingsAppOverviewAppearanceScreen(navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val pageId by viewModel.appOverviewPageId.collectAsState()
    val id = pageId ?: return
    val hideStatusBar by remember(id) { viewModel.pageHideStatusBar(id) }.collectAsState(initial = false)
    val override by remember(id) { viewModel.pageBackgroundOverride(id) }.collectAsState(initial = null)
    val globalShade by remember(id) { viewModel.pageGlobalShade(id) }.collectAsState(initial = null)
    val globalBackground by viewModel.globalBackground.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_page_appearance_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_statusbar_hide_page),
                onClick = { viewModel.setPageHideStatusBar(id, !hideStatusBar) },
                trailing = { Switch(checked = hideStatusBar, onCheckedChange = { viewModel.setPageHideStatusBar(id, it) }) }
            )
        }
        BackgroundOverrideSection(
            override = override,
            global = globalBackground,
            onUseGlobal = { viewModel.clearPageBackgroundConfig(id) },
            onCustomColor = { viewModel.setPageBackgroundConfig(id, it) },
            imageKey = "page_$id",
            globalShade = globalShade,
            onGlobalShadeChange = { viewModel.setPageGlobalShade(id, it) },
            groupTitle = stringResource(R.string.settings_section_global_background)
        )
    }
}

/** The letter-group header circles (e.g. "A", "B", "C" above each section). */
@Composable
fun SettingsAppOverviewSectionLettersScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val headerLetterAppearance by viewModel.appListHeaderLetterAppearance.collectAsState()
    SettingsSubScreen(title = surfaceLabel(R.string.settings_layer_surface_section_letter), navController = navController, onBack = onBack) {
        ElementAppearanceSection(
            appearance = headerLetterAppearance,
            onAppearanceChange = viewModel::setAppListHeaderLetterAppearance,
            styleGroupTitle = null
        )
    }
}

/** The right-edge fast-scroll rail — its box fill/border and letter text color are one appearance
 *  (see [com.bisc.flauncher.ui.home.search.AlphabetIndex]'s own doc on why there's no separate
 *  "show the box" switch or per-letter color anymore). */
@Composable
fun SettingsAppOverviewAzIndexScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val railAppearance by viewModel.appListRailAppearance.collectAsState()
    val pageId by homeViewModel.appOverviewPageId.collectAsState()
    val pageBackground by remember(pageId) { homeViewModel.pageBackgroundConfig(pageId ?: -1L) }.collectAsState(initial = BackgroundConfig())
    val surfaceIsDark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), pageBackground)

    SettingsSubScreen(title = stringResource(R.string.settings_app_overview_az_index_label), navController = navController, onBack = onBack) {
        ElementAppearanceSection(
            appearance = railAppearance ?: automaticRailAppearance(surfaceIsDark),
            onAppearanceChange = viewModel::setAppListRailAppearance,
            styleGroupTitle = null
        )
        if (railAppearance != null) {
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_element_color_reset),
                    onClick = viewModel::resetAppListRailAppearance,
                    trailing = null
                )
            }
        }
    }
}
