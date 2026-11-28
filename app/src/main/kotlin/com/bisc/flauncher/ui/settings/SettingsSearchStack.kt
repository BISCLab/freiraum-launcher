package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.LocalAppIconLoader
import com.bisc.flauncher.ui.home.LocalSurfaceIsDark
import com.bisc.flauncher.ui.home.search.automaticRailAppearance
import com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark
import com.bisc.flauncher.ui.stackElement

/**
 * The Text search's settings as a card stack: the search built from its real parts with a number
 * on each, the list of parts, and the options of the chosen one. Everything is a draft until Save.
 * The keyboard is the system's own; the replica draws one in plain shapes.
 */
@Composable
fun SettingsSearchFullScreen(
    navController: NavController,
    onBack: () -> Unit,
    startElement: String? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
    home: HomeViewModel = hiltViewModel()
) {
    val draft: StackDraft = viewModel()

    // ---- ⓪ The page: its own background, unlike the others not tied to a page id ----
    val global by viewModel.globalBackground.collectAsState()
    val page = PageDraft(
        imageKey = SettingsRepository.SEARCH_BACKGROUND_KEY,
        background = draft.field<BackgroundConfig?>("background", viewModel.searchBackgroundOverride.collectAsState().value) {
            if (it == null) viewModel.clearSearchBackgroundConfig() else viewModel.setSearchBackgroundConfig(it)
        },
        shade = draft.field<Float?>("shade", viewModel.searchGlobalShade.collectAsState().value) { it?.let(viewModel::setSearchGlobalShade) },
        hideStatus = draft.field("status", viewModel.searchHideStatusBar.collectAsState().value, viewModel::setSearchHideStatusBar),
        global = global
    )
    val pageLook by viewModel.searchAppearance.collectAsState()
    val textColor = draft.field("text_color", pageLook.contentColor) { viewModel.setSearchAppearance(pageLook.copy(contentColor = it)) }

    // ---- The parts ----
    val atBottom = draft.field("at_bottom", viewModel.searchBarAtBottom.collectAsState().value, viewModel::setSearchBarAtBottom)
    val railShown = draft.field("rail", viewModel.searchBarShowCategories.collectAsState().value, viewModel::setSearchBarShowCategories)
    val railLeft = draft.field("rail_left", viewModel.searchCategoryRailLeft.collectAsState().value, viewModel::setSearchCategoryRailLeft)
    val autoOpen = draft.field("auto_open", viewModel.searchKeyboardAutoOpen.collectAsState().value, viewModel::setSearchKeyboardAutoOpen)
    val noLearning = draft.field("no_learning", viewModel.searchKeyboardNoLearning.collectAsState().value, viewModel::setSearchKeyboardNoLearning)
    val capOn = draft.field("cap", viewModel.searchKeyboardCapEnabled.collectAsState().value, viewModel::setSearchKeyboardCapEnabled)
    val capColor = draft.field("cap_color", viewModel.searchKeyboardCapColor.collectAsState().value, viewModel::setSearchKeyboardCapColor)
    val capCorner = draft.field("cap_corner", viewModel.searchKeyboardCapCornerRadiusPercent.collectAsState().value, viewModel::setSearchKeyboardCapCornerRadiusPercent)
    val capArrow = draft.field("cap_arrow", viewModel.searchKeyboardCapArrow.collectAsState().value, viewModel::setSearchKeyboardCapArrow)
    val edgeToEdge = draft.field("edge", viewModel.searchResultsEdgeToEdge.collectAsState().value, viewModel::setSearchResultsEdgeToEdge)
    val symbols = draft.field("symbols", viewModel.categorySymbolsSearch.collectAsState().value) { viewModel.setCategorySymbols("search", it) }
    val counts = draft.field("counts", viewModel.categoryCountsSearch.collectAsState().value) { viewModel.setCategoryCounts("search", it) }

    val fieldLook = draft.field<ElementAppearance?>("field_look", viewModel.searchFieldAppearance.collectAsState().value) {
        if (it == null) viewModel.resetSearchFieldAppearance() else viewModel.setSearchFieldAppearance(it)
    }
    val railLook = draft.field<ElementAppearance?>("rail_look", viewModel.searchRailAppearance.collectAsState().value) {
        if (it == null) viewModel.resetSearchRailAppearance() else viewModel.setSearchRailAppearance(it)
    }
    val resultsLook = draft.field<ElementAppearance?>("results_look", viewModel.searchResultsAppearance.collectAsState().value) {
        if (it == null) viewModel.resetSearchResultsAppearance() else viewModel.setSearchResultsAppearance(it)
    }
    val bubbleLook = draft.field<ElementAppearance?>("bubble_look", viewModel.searchRailBubbleAppearance.collectAsState().value) {
        if (it == null) viewModel.resetSearchRailBubbleAppearance() else viewModel.setSearchRailBubbleAppearance(it)
    }
    // Unset corners: the field follows the bar (else fully round), the rail the general roundness, the cards start round.
    val barCorner by viewModel.searchBarCornerRadiusPercent.collectAsState()
    val defaultCorner by viewModel.defaultCornerRadiusPercent.collectAsState()
    val fieldCorner = draft.field("field_corner", viewModel.searchFieldCornerRadiusPercent.collectAsState().value ?: barCorner ?: 100, viewModel::setSearchFieldCornerRadiusPercent)
    val railCorner = draft.field("rail_corner", viewModel.searchRailCornerRadiusPercent.collectAsState().value ?: defaultCorner, viewModel::setSearchRailCornerRadiusPercent)
    val resultsCorner = draft.field("results_corner", viewModel.searchResultsCornerRadiusPercent.collectAsState().value ?: 100, viewModel::setSearchResultsCornerRadiusPercent)

    // ---- What follows from it ----
    val dark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), draft.shownBackground(page.shown))
    val defaultBox = com.bisc.flauncher.ui.theme.defaultBoxAppearanceFor(MaterialTheme.colorScheme.background.luminance() < 0.5f)
    val field = fieldLook.value ?: automaticRailAppearance(false)
    val rail = railLook.value ?: automaticRailAppearance(dark)
    val results = resultsLook.value ?: defaultBox
    val bubble = bubbleLook.value ?: resultsLook.value ?: defaultBox

    val off = stringResource(R.string.stack_summary_off)
    val automatic = stringResource(R.string.settings_element_color_auto_hint)
    val own = stringResource(R.string.settings_look_custom)
    fun lookSummary(look: ElementAppearance?) = if (look != null) own else automatic
    val fill = stringResource(R.string.settings_element_fill_label)

    @Composable
    fun Corners(value: Int, onChange: (Int) -> Unit) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
            CornerPercentPicker(percent = value, onChange = onChange)
        }
    }

    @Composable
    fun LinkRow(title: String, summary: String?, route: String) {
        SettingsPreference(
            icon = null,
            title = title,
            summary = summary,
            onClick = { navController.navigate(route) },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        )
    }

    val elements = listOf(
        StackElement(
            id = "page", number = 0,
            title = stringResource(R.string.settings_search_page_label),
            summary = stringResource(R.string.stack_page_summary_search),
            draftKeys = page.keys + "text_color"
        ) {
            PageStackOptions(page, null, navController, home)
            SettingsPreferenceGroup(title = stringResource(R.string.settings_element_text_color_label)) {
                ColorPickerRow(
                    title = stringResource(R.string.settings_element_text_color_label),
                    summary = if (textColor.value == null) automatic else null,
                    color = textColor.value ?: MaterialTheme.colorScheme.onSurface.toArgb(),
                    onColorChange = { textColor.value = it },
                    onReset = if (textColor.value != null) ({ textColor.value = null }) else null
                )
            }
            OpenWithGroup(LauncherPageKind.SEARCH, home, navController)
            SettingsGroupLabel(stringResource(R.string.stack_name_label))
            UtilityPageIdentityGroup(kind = LauncherPageKind.SEARCH, navController = navController, viewModel = home)
        },
        StackElement(
            id = "search_field", number = 1,
            title = stringResource(R.string.settings_app_grid_search_field),
            summary = lookSummary(fieldLook.value),
            draftKeys = listOf("at_bottom", "field_look", "field_corner"),
            anchor = Offset(0.08f, 0f),
            ownLook = fieldLook.value != null,
            onResetLook = { fieldLook.value = null },
            sample = { LookSample(LookSampleKind.FIELD, field, fieldCorner.value, dark) }
        ) {
            SettingsPreferenceGroup {
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_search_bar_position_label),
                    options = listOf(false to stringResource(R.string.settings_app_grid_bar_top), true to stringResource(R.string.settings_app_grid_bar_bottom)),
                    selected = atBottom.value,
                    onSelect = { atBottom.value = it },
                    info = stringResource(R.string.settings_search_field_position_info)
                )
            }
            ElementAppearanceSection(field, { fieldLook.value = it }, fill)
            Corners(fieldCorner.value) { fieldCorner.value = it }
        },
        StackElement(
            id = "rail", number = 2,
            title = stringResource(R.string.settings_search_rail_label),
            summary = if (!railShown.value) off else lookSummary(railLook.value),
            shown = railShown.value,
            draftKeys = listOf("rail", "rail_left", "rail_look", "rail_corner"),
            anchor = Offset(0.5f, 0.5f),
            ownLook = railLook.value != null,
            onResetLook = { railLook.value = null },
            sample = { LookSample(LookSampleKind.RAIL, rail, railCorner.value, dark) }
        ) {
            StackShowSwitch(stringResource(R.string.stack_show), railShown.value, { railShown.value = it }, summary = stringResource(R.string.settings_search_rail_info))
            StackDimmed(!railShown.value) {
                SettingsPreferenceGroup {
                    LinkRow(stringResource(R.string.settings_search_found_title), stringResource(R.string.settings_search_found_summary), "settings/search/behavior")
                }
                SettingsPreferenceGroup {
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_search_rail_side),
                        options = listOf(true to stringResource(R.string.settings_search_rail_left), false to stringResource(R.string.settings_search_rail_right)),
                        selected = railLeft.value,
                        onSelect = { railLeft.value = it }
                    )
                }
                ElementAppearanceSection(rail, { railLook.value = it }, fill)
                Corners(railCorner.value) { railCorner.value = it }
            }
        },
        StackElement(
            id = "results", number = 3,
            title = stringResource(R.string.settings_search_results_label),
            summary = lookSummary(resultsLook.value),
            draftKeys = listOf("edge", "symbols", "counts", "results_look", "results_corner"),
            anchor = Offset(0.92f, 0f),
            ownLook = resultsLook.value != null,
            onResetLook = { resultsLook.value = null },
            sample = { LookSample(LookSampleKind.CARDS, results, resultsCorner.value, dark) }
        ) {
            SettingsPreferenceGroup {
                LinkRow(stringResource(R.string.settings_search_found_title), stringResource(R.string.settings_search_found_summary), "settings/search/behavior")
                DraftSwitchRow(stringResource(R.string.settings_categories_symbols), symbols.value, { symbols.value = it }, summary = stringResource(R.string.settings_category_symbols_search_summary))
                DraftSwitchRow(stringResource(R.string.settings_categories_counts), counts.value, { counts.value = it })
            }
            SettingsPreferenceGroup {
                DraftSwitchRow(stringResource(R.string.settings_widgets_edge_to_edge_label), edgeToEdge.value, { edgeToEdge.value = it }, info = stringResource(R.string.settings_edge_to_edge_summary))
            }
            ElementAppearanceSection(results, { resultsLook.value = it }, fill)
            Corners(resultsCorner.value) { resultsCorner.value = it }
        },
        StackElement(
            id = "bubble", number = 4,
            title = stringResource(R.string.settings_search_bubble_label),
            summary = if (!railShown.value) off else lookSummary(bubbleLook.value),
            shown = railShown.value,
            draftKeys = listOf("bubble_look"),
            ownLook = bubbleLook.value != null,
            onResetLook = { bubbleLook.value = null },
            sample = { LookSample(LookSampleKind.BUBBLE, bubble, 100, dark) }
        ) {
            Text(
                stringResource(R.string.stack_bubble_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StackDimmed(!railShown.value) {
                ElementAppearanceSection(bubble, { bubbleLook.value = it }, fill)
            }
        },
        StackElement(
            id = "keyboard", number = 5,
            title = stringResource(R.string.settings_search_keyboard_label),
            summary = stringResource(R.string.stack_summary_only_keyboard),
            state = KEYBOARD,
            draftKeys = listOf("auto_open", "no_learning", "cap", "cap_color", "cap_corner", "cap_arrow"),
            anchor = Offset(0.92f, 0f)
        ) {
            SettingsPreferenceGroup {
                DraftSwitchRow(stringResource(R.string.settings_search_keyboard_auto_open), autoOpen.value, { autoOpen.value = it }, info = stringResource(R.string.settings_search_keyboard_auto_open_summary))
                DraftSwitchRow(stringResource(R.string.settings_keyboard_no_learning), noLearning.value, { noLearning.value = it }, summary = stringResource(R.string.settings_keyboard_no_learning_summary))
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_search_keyboard_cap_label)) {
                DraftSwitchRow(stringResource(R.string.settings_search_keyboard_cap_label), capOn.value, { capOn.value = it }, info = stringResource(R.string.settings_search_keyboard_cap_info))
            }
            StackDimmed(!capOn.value) {
                SettingsPreferenceGroup {
                    KeyboardCoverColorRow(color = capColor.value, cornerRadiusPercent = capCorner.value, arrow = capArrow.value, onColorChange = { capColor.value = it })
                    DraftSwitchRow(stringResource(R.string.settings_search_keyboard_cap_arrow), capArrow.value, { capArrow.value = it }, info = stringResource(R.string.settings_search_keyboard_cap_arrow_info))
                }
                Corners(capCorner.value) { capCorner.value = it }
            }
        }
    )

    // What the replica shows: the favorites, or a few apps when there are none yet.
    val favorites by home.searchFavoriteApps.collectAsState()
    val favoriteIds by home.searchFavoriteIds.collectAsState()
    val apps by home.browsableApps.collectAsState()
    val shownApps = remember(favorites, apps) { favorites.ifEmpty { apps.take(8) } }
    val order by home.searchCategoryOrder.collectAsState()
    val railHidden by home.searchRailHidden.collectAsState()
    val active by home.activeSearchCategories.collectAsState()
    val colors by home.categoryColors.collectAsState()
    val engines by home.searchEngines.collectAsState()
    val aiApps by home.searchAiApps.collectAsState()
    val webChoices = com.bisc.flauncher.ui.home.search.rememberEngineChoices(engines)
    val aiChoices = com.bisc.flauncher.ui.home.search.rememberAiChoices(aiApps, home::loadIcon)

    CompositionLocalProvider(LocalSurfaceIsDark provides dark) {
        SettingsCardStackScreen(
            title = stringResource(R.string.settings_section_search_full),
            navController = navController,
            onBack = onBack,
            draft = draft,
            elements = elements,
            states = listOf("normal" to stringResource(R.string.stack_state_normal), KEYBOARD to stringResource(R.string.stack_state_keyboard)),
            startElement = startElement
        ) { state, _ ->
            val typing = state == KEYBOARD
            val ink = com.bisc.flauncher.ui.home.dock.resolveElementColors(pageLook.copy(contentColor = textColor.value, fillMode = ElementFillMode.NONE), dark).contentColor
            val statusTop = replicaStatusHeight()
            CompositionLocalProvider(LocalAppIconLoader provides home.iconLoader) {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            com.bisc.flauncher.ui.home.search.SearchOverlay(
                                query = "",
                                appResults = emptyList(),
                                calculatorResult = null,
                                converterResult = null,
                                contactResults = emptyList(),
                                websiteResults = emptyList(),
                                shortcutResults = emptyList(),
                                contactsPermissionGranted = true,
                                appearance = pageLook.copy(contentColor = textColor.value),
                                cornerRadiusPercent = defaultCorner,
                                background = draft.shownBackground(page.shown),
                                barCornerRadiusPercent = barCorner,
                                resultsAppearance = resultsLook.value,
                                resultsCornerRadiusPercent = resultsCorner.value,
                                resultsEdgeToEdge = edgeToEdge.value,
                                barAtBottom = atBottom.value,
                                // Always drawn; switched off it comes out faint.
                                showCategoryRail = true,
                                railHidden = railHidden,
                                fieldAppearance = fieldLook.value,
                                fieldCornerRadiusPercent = fieldCorner.value,
                                statusBarHidden = page.hideStatus.value,
                                categoryOrder = order,
                                activeCategories = active + SearchCategory.FAVORITES,
                                categoryFilter = null,
                                categoryColors = colors,
                                webChoices = webChoices,
                                aiChoices = aiChoices,
                                keyboardAutoOpen = false,
                                railBubbleAppearance = bubbleLook.value,
                                loadIcon = home::loadIcon,
                                onQueryChange = {},
                                onAppClick = {},
                                onContactClick = {},
                                onSettingsShortcutClick = {},
                                onLauncherSettingsClick = {},
                                onWebsiteClick = {},
                                onShortcutClick = {},
                                onRefreshContactsPermission = {},
                                onWebCommand = {},
                                onMapsCommand = {},
                                onAiCommand = {},
                                favoriteApps = shownApps,
                                favoriteIds = favoriteIds,
                                railLeft = railLeft.value,
                                railAppearance = railLook.value,
                                railCornerRadiusPercent = railCorner.value
                            )
                        }
                        if (typing) {
                            Column(Modifier.fillMaxWidth().stackElement("keyboard")) {
                                if (capOn.value) com.bisc.flauncher.ui.home.search.KeyboardCapStrip(capColor.value, capCorner.value, capArrow.value)
                                KeyboardSketch()
                            }
                        }
                    }
                    if (!page.hideStatus.value) StatusBarSketch(statusTop, ink)
                }
            }
        }
    }
}

private const val KEYBOARD = "keyboard"
