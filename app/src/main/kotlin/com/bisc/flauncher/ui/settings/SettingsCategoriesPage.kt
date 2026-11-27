package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.ui.home.CategoriesPagePreview
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.LocalAppIconLoader
import com.bisc.flauncher.ui.home.LocalSurfaceIsDark
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark

/**
 * The Categories page's settings as a card stack: the page built from its real parts with a number
 * on each, the list of parts, and the options of the chosen one. Everything is a draft until Save.
 */
@Composable
fun SettingsCategoriesPageScreen(
    navController: NavController,
    onBack: () -> Unit,
    startElement: String? = null,
    home: HomeViewModel = hiltViewModel(),
    settings: SettingsViewModel = hiltViewModel()
) {
    val draft: StackDraft = viewModel()
    val pageId by home.categoriesPageId.collectAsState()
    val page = rememberPageDraft(draft, home, pageId ?: -1L)

    val view = draft.field("view", home.categoriesView.collectAsState().value, home::setCategoriesView)
    val cardRows = draft.field("card_rows", home.categoriesCardRows.collectAsState().value, home::setCategoriesCardRows)
    val showSearch = draft.field("search", home.categoriesShowSearch.collectAsState().value, home::setCategoriesShowSearch)
    val symbols = draft.field("symbols", settings.categorySymbolsPage.collectAsState().value) { settings.setCategorySymbols("page", it) }
    val counts = draft.field("counts", settings.categoryCountsPage.collectAsState().value) { settings.setCategoryCounts("page", it) }

    val dark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), page.shown)
    val off = stringResource(R.string.stack_summary_off)
    val tabs = view.value == "TABS"

    @Composable
    fun ManageLink() {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_categories_manage),
                summary = stringResource(R.string.settings_categories_manage_summary),
                onClick = { navController.navigate("settings/elements/categories") },
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
            )
        }
    }

    val elements = listOf(
        StackElement(
            id = "page", number = 0,
            title = stringResource(R.string.settings_search_page_label),
            summary = stringResource(R.string.stack_page_summary_plain),
            draftKeys = page.keys
        ) {
            PageStackOptions(page, LauncherPageKind.CATEGORIES, navController, home)
        },
        StackElement(
            id = "chips", number = 1,
            title = stringResource(R.string.stack_el_category_labels),
            draftKeys = listOf("symbols", "counts"),
            anchor = Offset(0.5f, 0.5f)
        ) {
            SettingsPreferenceGroup {
                DraftSwitchRow(stringResource(R.string.settings_categories_symbols), symbols.value, { symbols.value = it }, summary = stringResource(R.string.settings_category_symbols_page_summary))
                DraftSwitchRow(stringResource(R.string.settings_categories_counts), counts.value, { counts.value = it })
            }
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.stack_chips_look_link),
                    summary = stringResource(R.string.stack_chips_look_note),
                    onClick = { navController.navigate("settings/elements/categories") },
                    trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                )
            }
        },
        StackElement(
            id = "search_field", number = 2,
            title = stringResource(R.string.settings_categories_search_field),
            summary = if (showSearch.value) null else off,
            shown = showSearch.value,
            draftKeys = listOf("search"),
            anchor = Offset(0.08f, 0f)
        ) {
            StackShowSwitch(stringResource(R.string.stack_show), showSearch.value, { showSearch.value = it }, summary = stringResource(R.string.settings_categories_search_field_summary))
        },
        StackElement(
            id = "cards", number = 3,
            title = stringResource(R.string.stack_el_category_content),
            summary = stringResource(if (tabs) R.string.settings_categories_view_tabs else R.string.settings_categories_view_cards),
            draftKeys = listOf("view", "card_rows"),
            anchor = Offset(0.92f, 0.04f)
        ) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_categories_view), info = stringResource(R.string.settings_categories_view_info)) {
                RadioPreference(stringResource(R.string.settings_categories_view_cards), "CARDS", view.value, onSelect = { view.value = it }, summary = stringResource(R.string.settings_categories_view_cards_summary))
                RadioPreference(stringResource(R.string.settings_categories_view_tabs), "TABS", view.value, onSelect = { view.value = it }, summary = stringResource(R.string.settings_categories_view_tabs_summary))
                if (!tabs) {
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_categories_card_rows),
                        options = listOf(1 to "1", 2 to "2", 3 to "3", 0 to stringResource(R.string.settings_categories_card_rows_all)),
                        selected = cardRows.value,
                        onSelect = { cardRows.value = it },
                        info = stringResource(R.string.settings_categories_card_rows_info)
                    )
                }
            }
            ManageLink()
        }
    )

    val iconScale by home.iconSizeScale.collectAsState()
    CompositionLocalProvider(LocalSurfaceIsDark provides dark) {
        SettingsCardStackScreen(
            title = stringResource(R.string.settings_categories_page),
            navController = navController,
            onBack = onBack,
            draft = draft,
            elements = elements,
            startElement = startElement
        ) { _, _ ->
            val ink = resolveElementColors(ElementAppearance(fillMode = ElementFillMode.NONE), dark).contentColor
            val statusTop = replicaStatusHeight()
            CompositionLocalProvider(LocalAppIconLoader provides home.iconLoader, LocalContentColor provides ink) {
                Box(Modifier.fillMaxSize()) {
                    BackgroundFill(page.shown, Modifier.fillMaxSize())
                    com.bisc.flauncher.ui.home.HomeCategoriesPage(
                        viewModel = home,
                        hideStatusBar = page.hideStatus.value,
                        surfaceDark = dark,
                        iconSizeScale = iconScale,
                        onManage = {},
                        // The field is always drawn; switched off it comes out faint.
                        preview = CategoriesPagePreview(
                            view = view.value,
                            showSearch = true,
                            cardRows = cardRows.value,
                            symbols = symbols.value,
                            counts = counts.value,
                            top = if (page.hideStatus.value) androidx.compose.ui.unit.Dp(0f) else statusTop
                        )
                    )
                    if (!page.hideStatus.value) StatusBarSketch(statusTop, ink)
                }
            }
        }
    }
}
