package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.LocalAppIconLoader
import com.bisc.flauncher.ui.home.LocalSurfaceIsDark
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.search.automaticRailAppearance
import com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark

/**
 * The App list's settings as a card stack: the list built from its real parts with a number on
 * each, the list of parts, and the options of the chosen one. Everything is a draft until Save.
 * [startElement] opens one part's options first (from the settings search or a shortcut).
 */
@Composable
fun SettingsAppOverviewScreen(
    navController: NavController,
    onBack: () -> Unit,
    startElement: String? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
    home: HomeViewModel = hiltViewModel()
) {
    val draft: StackDraft = viewModel()
    val pageId by home.appOverviewPageId.collectAsState()
    val id = pageId ?: -1L

    // ---- What is stored, and the draft over it ----
    val handedness = draft.field("handedness", viewModel.appOverviewHandedness.collectAsState().value, viewModel::setAppOverviewHandedness)
    val showAz = draft.field("az", viewModel.appListShowAzIndex.collectAsState().value, viewModel::setAppListShowAzIndex)
    val showButton = draft.field("button", viewModel.appListShowSearchButton.collectAsState().value, viewModel::setAppListShowSearchButton)
    val ownSearch = draft.field("own_search", viewModel.appListOwnSearch.collectAsState().value, viewModel::setAppListOwnSearch)
    val chips = draft.field("chips", viewModel.appListCategoryChips.collectAsState().value, viewModel::setAppListCategoryChips)
    val chipSymbols = draft.field("chip_symbols", viewModel.categorySymbolsAppList.collectAsState().value) { viewModel.setCategorySymbols("app_list", it) }
    val chipCounts = draft.field("chip_counts", viewModel.categoryCountsAppList.collectAsState().value) { viewModel.setCategoryCounts("app_list", it) }
    val letterLook = draft.field("letter_look", viewModel.appListHeaderLetterAppearance.collectAsState().value, viewModel::setAppListHeaderLetterAppearance)
    val defaultCorner by viewModel.defaultCornerRadiusPercent.collectAsState()
    val railLook = draft.field<ElementAppearance?>("rail_look", viewModel.appListRailAppearance.collectAsState().value) {
        if (it == null) viewModel.resetAppListRailAppearance() else viewModel.setAppListRailAppearance(it)
    }
    val railCorner = draft.field("rail_corner", viewModel.alphabetRailCornerPercent.collectAsState().value ?: defaultCorner, viewModel::setAlphabetRailCornerPercent)
    val buttonLook = draft.field<ElementAppearance?>("button_look", viewModel.appListSearchButtonAppearance.collectAsState().value) {
        if (it == null) viewModel.resetAppListSearchButtonAppearance() else viewModel.setAppListSearchButtonAppearance(it)
    }
    val buttonCorner = draft.field("button_corner", viewModel.appListSearchButtonCornerPercent.collectAsState().value ?: defaultCorner, viewModel::setAppListSearchButtonCornerPercent)
    val fieldLook = draft.field<ElementAppearance?>("field_look", viewModel.appListSearchFieldAppearance.collectAsState().value) {
        if (it == null) viewModel.resetAppListSearchFieldAppearance() else viewModel.setAppListSearchFieldAppearance(it)
    }
    val textFieldCorner by viewModel.searchFieldCornerRadiusPercent.collectAsState()
    val fieldCorner = draft.field("field_corner", viewModel.appListFieldCornerPercent.collectAsState().value ?: textFieldCorner ?: defaultCorner, viewModel::setAppListFieldCornerPercent)
    val page = rememberPageDraft(draft, home, id)

    // ---- What follows from it ----
    val pageBackground = page.shown
    val dark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), pageBackground)
    val left = handedness.value == Handedness.LEFT
    val rail = railLook.value ?: automaticRailAppearance(dark)
    val button = buttonLook.value ?: automaticRailAppearance(dark)
    val field = fieldLook.value ?: automaticRailAppearance(dark)
    val fieldShown = showButton.value && ownSearch.value

    val off = stringResource(R.string.stack_summary_off)
    val automatic = stringResource(R.string.settings_element_color_auto_hint)
    val own = stringResource(R.string.settings_look_custom)
    fun lookSummary(shown: Boolean, look: ElementAppearance?) = if (!shown) off else if (look != null) own else automatic

    val elements = listOf(
        StackElement(
            id = "page", number = 0,
            title = stringResource(R.string.settings_search_page_label),
            summary = stringResource(R.string.stack_page_summary),
            draftKeys = listOf("handedness") + page.keys
        ) {
            PageStackOptions(page, LauncherPageKind.APP_LIST, navController, home) {
                SettingsPreferenceGroup(title = stringResource(R.string.settings_app_overview_alignment_label)) {
                    HandednessPreviews(selected = handedness.value, onSelect = { handedness.value = it })
                }
            }
        },
        StackElement(
            id = "category_labels", number = 1,
            title = stringResource(R.string.stack_el_category_labels),
            summary = if (chips.value) null else off,
            shown = chips.value,
            draftKeys = listOf("chips", "chip_symbols", "chip_counts"),
            anchor = Offset(0.5f, 0.5f)
        ) {
            StackShowSwitch(stringResource(R.string.stack_show), chips.value, { chips.value = it }, summary = stringResource(R.string.settings_app_list_category_chips_info))
            StackDimmed(!chips.value) {
                SettingsPreferenceGroup {
                    DraftSwitchRow(stringResource(R.string.settings_categories_symbols), chipSymbols.value, { chipSymbols.value = it })
                    DraftSwitchRow(stringResource(R.string.settings_categories_counts), chipCounts.value, { chipCounts.value = it })
                }
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.stack_chips_look_link),
                        summary = stringResource(R.string.stack_chips_look_note),
                        onClick = { navController.navigate("settings/elements/categories") },
                        trailing = { androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                    )
                }
            }
        },
        StackElement(
            id = "letters", number = 2,
            title = stringResource(R.string.settings_part_letters_short),
            draftKeys = listOf("letter_look"),
            anchor = if (left) Offset(-0.7f, 0.5f) else Offset(1.7f, 0.5f),
            sample = { LookSample(LookSampleKind.LETTER, letterLook.value, 100, dark) }
        ) {
            ElementAppearanceSection(letterLook.value, { letterLook.value = it }, stringResource(R.string.settings_element_fill_label))
        },
        StackElement(
            id = "az_index", number = 3,
            title = stringResource(R.string.settings_app_overview_az_index_label),
            summary = lookSummary(showAz.value, railLook.value),
            shown = showAz.value,
            draftKeys = listOf("az", "rail_look", "rail_corner"),
            anchor = if (left) Offset(1f, 0.5f) else Offset(0f, 0.5f),
            ownLook = railLook.value != null,
            onResetLook = { railLook.value = null },
            sample = { LookSample(LookSampleKind.AZ_INDEX, rail, railCorner.value, dark) }
        ) {
            StackShowSwitch(stringResource(R.string.stack_show), showAz.value, { showAz.value = it })
            StackDimmed(!showAz.value) {
                ElementAppearanceSection(rail, { railLook.value = it }, stringResource(R.string.settings_element_fill_label))
                SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
                    CornerPercentPicker(percent = railCorner.value, onChange = { railCorner.value = it })
                }
            }
        },
        StackElement(
            id = "search_button", number = 4,
            title = stringResource(R.string.settings_app_list_search_button),
            summary = lookSummary(showButton.value, buttonLook.value),
            shown = showButton.value,
            draftKeys = listOf("button", "own_search", "button_look", "button_corner"),
            anchor = if (left) Offset(1f, 0f) else Offset(0f, 0f),
            ownLook = buttonLook.value != null,
            onResetLook = { buttonLook.value = null },
            sample = { LookSample(LookSampleKind.SEARCH_BUTTON, button, buttonCorner.value, dark) }
        ) {
            StackShowSwitch(stringResource(R.string.stack_show), showButton.value, { showButton.value = it }, summary = stringResource(R.string.settings_app_list_search_button_summary))
            StackDimmed(!showButton.value) {
                SettingsPreferenceGroup {
                    DraftSwitchRow(stringResource(R.string.settings_app_list_own_search), ownSearch.value, { ownSearch.value = it }, info = stringResource(R.string.settings_app_list_own_search_info))
                }
                ElementAppearanceSection(button, { buttonLook.value = it }, stringResource(R.string.settings_element_fill_label))
                SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
                    CornerPercentPicker(percent = buttonCorner.value, onChange = { buttonCorner.value = it })
                }
            }
        },
        StackElement(
            id = "search_field", number = 5,
            title = stringResource(R.string.settings_app_grid_search_field),
            summary = if (fieldShown) stringResource(R.string.stack_summary_only_searching) else off,
            shown = fieldShown,
            state = SEARCHING,
            draftKeys = listOf("own_search", "field_look", "field_corner"),
            anchor = Offset(0.08f, 0f),
            ownLook = fieldLook.value != null,
            onResetLook = { fieldLook.value = null },
            sample = { LookSample(LookSampleKind.FIELD, field, fieldCorner.value, dark) }
        ) {
            StackShowSwitch(
                stringResource(R.string.settings_app_list_own_search), ownSearch.value, { ownSearch.value = it },
                summary = stringResource(R.string.stack_field_needs_own_search),
                enabled = showButton.value
            )
            StackDimmed(!fieldShown) {
                ElementAppearanceSection(field, { fieldLook.value = it }, stringResource(R.string.settings_element_fill_label))
                SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
                    CornerPercentPicker(percent = fieldCorner.value, onChange = { fieldCorner.value = it })
                }
            }
        }
    )

    val apps by home.browsableApps.collectAsState()
    val accent by home.appListAccentColor.collectAsState()
    CompositionLocalProvider(LocalSurfaceIsDark provides dark) {
        SettingsCardStackScreen(
            title = stringResource(R.string.settings_section_app_overview),
            navController = navController,
            onBack = onBack,
            draft = draft,
            elements = elements,
            states = listOf("normal" to stringResource(R.string.stack_state_normal), SEARCHING to stringResource(R.string.stack_state_searching)),
            startElement = startElement
        ) { state, _ ->
            CompositionLocalProvider(LocalAppIconLoader provides home.iconLoader) {
                AppListReplica(
                    searching = state == SEARCHING,
                    apps = remember(apps) { apps.take(18) },
                    background = pageBackground,
                    dark = dark,
                    hideStatus = page.hideStatus.value,
                    handedness = handedness.value,
                    chipSymbols = chipSymbols.value,
                    chipCounts = chipCounts.value,
                    letterLook = letterLook.value,
                    railLook = railLook.value,
                    railCorner = railCorner.value,
                    button = button,
                    buttonCorner = buttonCorner.value,
                    field = field,
                    fieldCorner = fieldCorner.value,
                    accent = accent,
                    loadIcon = home::loadIcon
                )
            }
        }
    }
}

private const val SEARCHING = "search"

/**
 * The App list as on the home screen, from its own parts, at full screen size: the status bar, the
 * category labels, the list with its letters and rail, and the search button; while searching the
 * field and a keyboard instead. Every part is drawn, also the ones switched off (they come out faint).
 */
@Composable
private fun AppListReplica(
    searching: Boolean,
    apps: List<AppInfo>,
    background: BackgroundConfig,
    dark: Boolean,
    hideStatus: Boolean,
    handedness: Handedness,
    chipSymbols: Boolean,
    chipCounts: Boolean,
    letterLook: ElementAppearance,
    railLook: ElementAppearance?,
    railCorner: Int,
    button: ElementAppearance,
    buttonCorner: Int,
    field: ElementAppearance,
    fieldCorner: Int,
    accent: Int,
    loadIcon: (android.content.ComponentName) -> androidx.compose.ui.graphics.ImageBitmap?
) {
    val statusTop = replicaStatusHeight()
    val ink = resolveElementColors(ElementAppearance(fillMode = ElementFillMode.NONE), dark).contentColor
    val hasCategories = com.bisc.flauncher.ui.home.LauncherCategories.resolved.isNotEmpty()
    Box(Modifier.fillMaxSize()) {
        BackgroundFill(background, Modifier.fillMaxSize())
        CompositionLocalProvider(LocalContentColor provides ink) {
            Column(Modifier.fillMaxSize()) {
                if (!searching && hasCategories) {
                    com.bisc.flauncher.ui.home.AppListCategoryChips(
                        selected = null,
                        onSelect = {},
                        surfaceDark = dark,
                        showSymbols = chipSymbols,
                        showCounts = chipCounts,
                        modifier = Modifier.padding(top = statusTop + 12.dp)
                    )
                }
                com.bisc.flauncher.ui.home.search.AppOverviewContent(
                    showSearchButton = !searching,
                    allApps = apps,
                    showAzIndex = true,
                    headerLetterAppearance = letterLook,
                    pageBackground = background,
                    railAppearance = railLook,
                    accentColor = accent,
                    handedness = handedness,
                    cornerRadiusPercent = railCorner,
                    loadIcon = loadIcon,
                    onAppClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = if (!searching && hasCategories) 4.dp else statusTop + 16.dp)
                )
                if (searching) {
                    com.bisc.flauncher.ui.home.search.AppSearchField(
                        query = "",
                        onQueryChange = {},
                        onSubmit = {},
                        appearance = field,
                        cornerRadiusPercent = fieldCorner,
                        autoOpen = false,
                        modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = com.bisc.flauncher.ui.MaxContentWidth).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                    )
                    KeyboardSketch()
                }
            }
        }
        if (!searching) {
            com.bisc.flauncher.ui.home.search.AppOverviewSearchButton(
                onClick = {},
                handedness = handedness,
                cornerRadiusPercent = buttonCorner,
                appearance = button
            )
        }
        if (!hideStatus) StatusBarSketch(statusTop, ink)
    }
}
