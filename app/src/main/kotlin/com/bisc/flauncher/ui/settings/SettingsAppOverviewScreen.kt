package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.search.automaticRailAppearance

/** App list hub — same split as Text search: everything about how it looks under Appearance,
 *  everything about how it works under Behavior. */
@Composable
fun SettingsAppOverviewScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
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
                title = stringResource(R.string.settings_home_appearance_title),
                onClick = { onNavigate("appearance") },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_behavior_label),
                onClick = { onNavigate("behavior") },
                trailing = null
            )
        }
    }
}

/** Appearance hub: the page itself, then each of its elements. Its accents follow the global one. */
@Composable
fun SettingsAppOverviewAppearanceHubScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    SettingsSubScreen(title = stringResource(R.string.settings_home_appearance_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_page_label),
                onClick = { onNavigate("page") },
                trailing = null
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
                onClick = { onNavigate("az_index") },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_list_search_button),
                onClick = { onNavigate("search_button") },
                trailing = null
            )
        }
    }
}

/** Behavior: which side the list's controls sit on, and which of them are shown at all. */
@Composable
fun SettingsAppOverviewBehaviorScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val showAzIndex by viewModel.appListShowAzIndex.collectAsState()
    val showSearchButton by viewModel.appListShowSearchButton.collectAsState()
    val handedness by viewModel.appOverviewHandedness.collectAsState()
    SettingsSubScreen(title = stringResource(R.string.settings_search_behavior_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_app_overview_alignment_label)) {
            HandednessPreviews(selected = handedness, onSelect = viewModel::setAppOverviewHandedness)
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_overview_az_index_label),
                onClick = { viewModel.setAppListShowAzIndex(!showAzIndex) },
                trailing = { Switch(checked = showAzIndex, onCheckedChange = viewModel::setAppListShowAzIndex) }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_list_search_button),
                info = stringResource(R.string.settings_app_list_search_button_summary),
                onClick = { viewModel.setAppListShowSearchButton(!showSearchButton) },
                trailing = { Switch(checked = showSearchButton, onCheckedChange = viewModel::setAppListShowSearchButton) }
            )
        }
    }
}

/** The floating search button's own look — follows the A–Z index's until customized here. */
@Composable
fun SettingsAppOverviewSearchButtonScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val own by viewModel.appListSearchButtonAppearance.collectAsState()
    val railAppearance by viewModel.appListRailAppearance.collectAsState()
    val pageId by homeViewModel.appOverviewPageId.collectAsState()
    val pageBackground by remember(pageId) { homeViewModel.pageBackgroundConfig(pageId ?: -1L) }.collectAsState(initial = BackgroundConfig())
    val surfaceIsDark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), pageBackground)

    // Keyed on the stored values only: the automatic look is re-derived once the page background
    // has loaded, and that must not wipe what is being edited.
    val saved = remember(own, railAppearance) { own ?: railAppearance ?: automaticRailAppearance(surfaceIsDark) }
    var draft by rememberDraft(saved)
    SettingsSubScreen(title = stringResource(R.string.settings_app_list_search_button), navController = navController, onBack = onBack, onSave = { viewModel.setAppListSearchButtonAppearance(draft) }, dirty = draft != saved) {
        ElementAppearanceSection(
            appearance = draft,
            onAppearanceChange = { draft = it },
            styleGroupTitle = null
        )
    }
}

/** The app-list page itself: its own background and status-bar switch — same pairing as
 *  [SettingsPageAppearanceScreen]. */
@Composable
fun SettingsAppOverviewPageScreen(navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val pageId by viewModel.appOverviewPageId.collectAsState()
    val id = pageId ?: return
    val hideStatusBar by remember(id) { viewModel.pageHideStatusBar(id) }.collectAsState(initial = false)
    val override by remember(id) { viewModel.pageBackgroundOverride(id) }.collectAsState(initial = null)
    val globalShade by remember(id) { viewModel.pageGlobalShade(id) }.collectAsState(initial = null)
    val globalBackground by viewModel.globalBackground.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_search_page_label), navController = navController, onBack = onBack) {
        UtilityPageIdentityGroup(kind = LauncherPageKind.APP_LIST, navController = navController, viewModel = viewModel)
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
        SettingsPreferenceGroup(title = stringResource(R.string.settings_statusbar_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_statusbar_hide_page),
                onClick = { viewModel.setPageHideStatusBar(id, !hideStatusBar) },
                trailing = { Switch(checked = hideStatusBar, onCheckedChange = { viewModel.setPageHideStatusBar(id, it) }) }
            )
        }
    }
}

/** The letter-group header circles (e.g. "A", "B", "C" above each section). */
@Composable
fun SettingsAppOverviewSectionLettersScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val saved by viewModel.appListHeaderLetterAppearance.collectAsState()
    var draft by rememberDraft(saved)
    SettingsSubScreen(title = surfaceLabel(R.string.settings_layer_surface_section_letter), navController = navController, onBack = onBack, onSave = { viewModel.setAppListHeaderLetterAppearance(draft) }, dirty = draft != saved) {
        ElementAppearanceSection(
            appearance = draft,
            onAppearanceChange = { draft = it },
            styleGroupTitle = null
        )
    }
}

/** The right-edge fast-scroll rail — its box fill/border and letter text color are one appearance
 *  (see [com.bisc.flauncher.ui.home.search.AlphabetIndex]). */
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

    val saved = remember(railAppearance) { railAppearance ?: automaticRailAppearance(surfaceIsDark) }
    var draft by rememberDraft(saved)
    SettingsSubScreen(title = stringResource(R.string.settings_app_overview_az_index_label), navController = navController, onBack = onBack, onSave = { viewModel.setAppListRailAppearance(draft) }, dirty = draft != saved) {
        ElementAppearanceSection(
            appearance = draft,
            onAppearanceChange = { draft = it },
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
