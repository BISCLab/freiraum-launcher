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

/** App list hub - same split as Text search: everything about how it looks under Appearance,
 *  everything about how it works under Behavior. */
@Composable
fun SettingsAppOverviewScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    AppearanceBehaviorHub(
        title = stringResource(R.string.settings_section_app_overview),
        intro = stringResource(R.string.settings_page_app_overview_description),
        appearanceContents = stringResource(R.string.settings_hub_app_list_appearance),
        behaviorContents = stringResource(R.string.settings_hub_app_list_behavior),
        navController = navController,
        onBack = onBack,
        onNavigate = onNavigate
    )
}

/** Appearance hub: the page itself, then each of its elements. Its accents follow the global one. */
@Composable
fun SettingsAppOverviewAppearanceHubScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val showSearchButton by viewModel.appListShowSearchButton.collectAsState()
    val ownSearch by viewModel.appListOwnSearch.collectAsState()
    SettingsSubScreen(title = stringResource(R.string.settings_home_appearance_title), navController = navController, onBack = onBack) {
        // Tap a part of the miniature to edit it; the list below stays as the second way in.
        AppListPreview(onNavigate = onNavigate)
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_page_label),
                onClick = { onNavigate("page") }
            )
            SettingsPreference(
                icon = null,
                title = surfaceLabel(R.string.settings_layer_surface_section_letter),
                onClick = { onNavigate("section_letters") }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_overview_az_index_label),
                onClick = { onNavigate("az_index") }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_list_search_button),
                onClick = { onNavigate("search_button") }
            )
            if (showSearchButton && ownSearch) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_app_grid_search_field),
                    onClick = { onNavigate("search_field") }
                )
            }
        }
    }
}

/** Behavior: which side the list's controls sit on, and which of them are shown at all. */
@Composable
fun SettingsAppOverviewBehaviorScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val showAzIndex by viewModel.appListShowAzIndex.collectAsState()
    val showSearchButton by viewModel.appListShowSearchButton.collectAsState()
    val ownSearch by viewModel.appListOwnSearch.collectAsState()
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
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_list_own_search),
                info = stringResource(R.string.settings_app_list_own_search_info),
                enabled = showSearchButton,
                onClick = { viewModel.setAppListOwnSearch(!ownSearch) },
                trailing = { Switch(checked = ownSearch, enabled = showSearchButton, onCheckedChange = viewModel::setAppListOwnSearch) }
            )
        }
    }
}

/** The floating search button's own look. Independent of the A-Z rail: until set here it has the
 *  automatic look for the page's brightness. */
@Composable
fun SettingsAppOverviewSearchButtonScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val own by viewModel.appListSearchButtonAppearance.collectAsState()
    val corner by viewModel.appListSearchButtonCornerPercent.collectAsState()
    val surfaceIsDark = appListPageDark(homeViewModel)
    // Keyed on the stored value only: the automatic look is re-derived once the page background
    // has loaded, and that must not wipe what is being edited.
    val saved = remember(own) { own ?: automaticRailAppearance(surfaceIsDark) }
    val defaultCorner by viewModel.defaultCornerRadiusPercent.collectAsState()
    ElementLookScreen(
        title = stringResource(R.string.settings_app_list_search_button),
        navController = navController,
        onBack = onBack,
        saved = saved,
        savedCorner = corner ?: defaultCorner,
        onSave = { look, newCorner -> viewModel.setAppListSearchButtonAppearance(look); newCorner?.let(viewModel::setAppListSearchButtonCornerPercent) },
        kind = LookSampleKind.SEARCH_BUTTON,
        backdropDark = surfaceIsDark,
        custom = own != null,
        onResetLook = viewModel::resetAppListSearchButtonAppearance,
    )
}

/** Whether the App list page is dark, from its background. */
@Composable
private fun appListPageDark(homeViewModel: HomeViewModel): Boolean {
    val pageId by homeViewModel.appOverviewPageId.collectAsState()
    val pageBackground by remember(pageId) { homeViewModel.pageBackgroundConfig(pageId ?: -1L) }.collectAsState(initial = BackgroundConfig())
    return com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), pageBackground)
}

/** The app-list page itself: its own background and status-bar switch - same pairing as
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
fun SettingsAppOverviewSectionLettersScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val saved by viewModel.appListHeaderLetterAppearance.collectAsState()
    ElementLookScreen(
        title = surfaceLabel(R.string.settings_layer_surface_section_letter),
        navController = navController,
        onBack = onBack,
        saved = saved,
        savedCorner = null,
        onSave = { look, _ -> viewModel.setAppListHeaderLetterAppearance(look) },
        kind = LookSampleKind.LETTER,
        backdropDark = appListPageDark(homeViewModel)
    )
}

/** The fast-scroll rail; its box and its letters are one look (see
 *  [com.bisc.flauncher.ui.home.search.AlphabetIndex]). Only the rail follows it, nothing else. */
@Composable
fun SettingsAppOverviewAzIndexScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val railAppearance by viewModel.appListRailAppearance.collectAsState()
    val corner by viewModel.alphabetRailCornerPercent.collectAsState()
    val surfaceIsDark = appListPageDark(homeViewModel)
    val saved = remember(railAppearance) { railAppearance ?: automaticRailAppearance(surfaceIsDark) }
    val defaultCorner by viewModel.defaultCornerRadiusPercent.collectAsState()
    ElementLookScreen(
        title = stringResource(R.string.settings_app_overview_az_index_label),
        navController = navController,
        onBack = onBack,
        saved = saved,
        savedCorner = corner ?: defaultCorner,
        onSave = { look, newCorner -> viewModel.setAppListRailAppearance(look); newCorner?.let(viewModel::setAlphabetRailCornerPercent) },
        kind = LookSampleKind.AZ_INDEX,
        backdropDark = surfaceIsDark,
        custom = railAppearance != null,
        onResetLook = viewModel::resetAppListRailAppearance
    )
}

/** The app list's own search field. Independent of the button and the rail: until set here it has
 *  the automatic look for the page's brightness. */
@Composable
fun SettingsAppOverviewSearchFieldScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val own by viewModel.appListSearchFieldAppearance.collectAsState()
    val corner by viewModel.appListFieldCornerPercent.collectAsState()
    val fieldCorner by viewModel.searchFieldCornerRadiusPercent.collectAsState()
    val surfaceIsDark = appListPageDark(homeViewModel)
    val saved = remember(own) { own ?: automaticRailAppearance(surfaceIsDark) }
    val defaultCorner by viewModel.defaultCornerRadiusPercent.collectAsState()
    ElementLookScreen(
        title = stringResource(R.string.settings_app_grid_search_field),
        navController = navController,
        onBack = onBack,
        saved = saved,
        savedCorner = corner ?: fieldCorner ?: defaultCorner,
        onSave = { look, newCorner -> viewModel.setAppListSearchFieldAppearance(look); newCorner?.let(viewModel::setAppListFieldCornerPercent) },
        kind = LookSampleKind.FIELD,
        backdropDark = surfaceIsDark,
        custom = own != null,
        onResetLook = viewModel::resetAppListSearchFieldAppearance
    )
}
