package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppGridBarPosition
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.LocalAppIconLoader
import com.bisc.flauncher.ui.home.LocalSurfaceIsDark
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.search.automaticRailAppearance
import com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark
import kotlin.math.roundToInt

/**
 * The App grid's settings as a card stack: the grid built from its real parts with a number on
 * each, the list of parts, and the options of the chosen one. Everything is a draft until Save.
 */
@Composable
fun SettingsAppGridScreen(
    navController: NavController,
    onBack: () -> Unit,
    startElement: String? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
    home: HomeViewModel = hiltViewModel()
) {
    val draft: StackDraft = viewModel()
    val pageId by home.appGridPageId.collectAsState()
    val id = pageId ?: -1L
    val page = rememberPageDraft(draft, home, id)

    val position = draft.field("bar_position", viewModel.appGridBarPosition.collectAsState().value, viewModel::setAppGridBarPosition)
    val opensTextSearch = draft.field("bar_text_search", viewModel.appGridBarOpensTextSearch.collectAsState().value, viewModel::setAppGridBarOpensTextSearch)
    val keyboard = draft.field("bar_keyboard", viewModel.appGridKeyboardAutoOpen.collectAsState().value, viewModel::setAppGridKeyboardAutoOpen)
    val barLook = draft.field<ElementAppearance?>("bar_look", viewModel.appGridBarAppearance.collectAsState().value) {
        if (it == null) viewModel.resetAppGridBarAppearance() else viewModel.setAppGridBarAppearance(it)
    }
    val defaultCorner by viewModel.defaultCornerRadiusPercent.collectAsState()
    val textFieldCorner by viewModel.searchFieldCornerRadiusPercent.collectAsState()
    val barCorner = draft.field("bar_corner", viewModel.appGridFieldCornerPercent.collectAsState().value ?: textFieldCorner ?: defaultCorner, viewModel::setAppGridFieldCornerPercent)
    val horizontal = draft.field("horizontal", viewModel.appGridHorizontal.collectAsState().value, viewModel::setAppGridHorizontal)
    val rows = draft.field("rows", viewModel.appGridRows.collectAsState().value, viewModel::setAppGridRows)
    val pageDots = draft.field("page_dots", viewModel.appGridPageDots.collectAsState().value, viewModel::setAppGridPageDots)
    val columns = draft.field("columns", viewModel.appGridColumns.collectAsState().value, viewModel::setAppGridColumns)
    val iconSize = draft.field("icon_size", viewModel.appGridIconSizePercent.collectAsState().value, viewModel::setAppGridIconSizePercent)
    val labels = draft.field("labels", viewModel.appGridShowLabels.collectAsState().value, viewModel::setAppGridShowLabels)
    val folderIds = draft.field("folder_ids", viewModel.appGridFolderIds.collectAsState().value, viewModel::setAppGridFolderIds)
    val alphabetical = draft.field("folders_alphabetical", viewModel.appGridFoldersAlphabetical.collectAsState().value, viewModel::setAppGridFoldersAlphabetical)
    val hideFolderApps = draft.field("hide_folder_apps", viewModel.appGridHideFolderApps.collectAsState().value, viewModel::setAppGridHideFolderApps)
    val folders by home.folders.collectAsState()

    // Where the field goes when it is switched on again; also where it is drawn while off.
    var lastPosition by rememberSaveable { mutableStateOf(position.value.takeIf { it != AppGridBarPosition.NONE } ?: AppGridBarPosition.TOP) }
    val barShown = position.value != AppGridBarPosition.NONE
    val barAt = if (barShown) position.value else lastPosition

    val dark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), page.shown)
    val bar = barLook.value ?: automaticRailAppearance(dark)

    val off = stringResource(R.string.stack_summary_off)
    val automatic = stringResource(R.string.settings_element_color_auto_hint)
    val own = stringResource(R.string.settings_look_custom)
    val chosenFolders = folders.filter { it.id in folderIds.value }

    val elements = listOf(
        StackElement(
            id = "page", number = 0,
            title = stringResource(R.string.settings_search_page_label),
            summary = stringResource(R.string.stack_page_summary_plain),
            draftKeys = page.keys
        ) {
            PageStackOptions(page, LauncherPageKind.APP_GRID, navController, home)
        },
        StackElement(
            id = "search_field", number = 1,
            title = stringResource(R.string.settings_app_grid_search_field),
            summary = if (!barShown) off else if (barLook.value != null) own else automatic,
            shown = barShown,
            draftKeys = listOf("bar_position", "bar_text_search", "bar_keyboard", "bar_look", "bar_corner"),
            anchor = Offset(0.08f, 0f),
            ownLook = barLook.value != null,
            onResetLook = { barLook.value = null },
            sample = { LookSample(LookSampleKind.FIELD, bar, barCorner.value, dark) }
        ) {
            StackShowSwitch(stringResource(R.string.stack_show), barShown, { on -> position.value = if (on) lastPosition else AppGridBarPosition.NONE })
            StackDimmed(!barShown) {
                SettingsPreferenceGroup {
                    DraftSwitchRow(
                        stringResource(R.string.settings_app_grid_bar_text_search), opensTextSearch.value, { opensTextSearch.value = it },
                        info = stringResource(R.string.settings_app_grid_bar_text_search_info)
                    )
                    DraftSwitchRow(
                        stringResource(R.string.settings_app_grid_keyboard), keyboard.value && !opensTextSearch.value, { keyboard.value = it },
                        info = stringResource(R.string.settings_app_grid_keyboard_info),
                        enabled = !opensTextSearch.value
                    )
                }
                SettingsPreferenceGroup {
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_search_bar_position_label),
                        options = listOf(
                            AppGridBarPosition.TOP to stringResource(R.string.settings_app_grid_bar_top),
                            AppGridBarPosition.BOTTOM to stringResource(R.string.settings_app_grid_bar_bottom)
                        ),
                        selected = barAt,
                        onSelect = { lastPosition = it; if (barShown) position.value = it }
                    )
                }
                ElementAppearanceSection(bar, { barLook.value = it }, stringResource(R.string.settings_element_fill_label))
                SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
                    CornerPercentPicker(percent = barCorner.value, onChange = { barCorner.value = it })
                }
            }
        },
        StackElement(
            id = "grid", number = 2,
            title = stringResource(R.string.settings_app_grid_grid_label),
            summary = stringResource(if (horizontal.value) R.string.settings_app_grid_horizontal else R.string.settings_app_grid_vertical),
            draftKeys = listOf("horizontal", "rows", "page_dots", "columns", "icon_size", "labels"),
            anchor = Offset(0.5f, 0.5f)
        ) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_app_grid_direction)) {
                RadioPreference(stringResource(R.string.settings_app_grid_vertical), false, horizontal.value, onSelect = { horizontal.value = it })
                RadioPreference(stringResource(R.string.settings_app_grid_horizontal), true, horizontal.value, onSelect = { horizontal.value = it })
                if (horizontal.value) {
                    val r = rows.value
                    // Below 3 comes "as many as fit".
                    DraftStepperRow(
                        title = stringResource(R.string.settings_app_grid_rows),
                        value = if (r == 0) stringResource(R.string.settings_app_grid_rows_auto) else r.toString(),
                        onLess = { rows.value = if (r <= 3) 0 else r - 1 },
                        onMore = { rows.value = if (r == 0) 3 else r + 1 },
                        lessEnabled = r != 0,
                        moreEnabled = r < 8
                    )
                    DraftSwitchRow(stringResource(R.string.settings_app_grid_page_dots), pageDots.value, { pageDots.value = it })
                }
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_app_grid_columns)) {
                val c = columns.value
                DraftStepperRow(
                    title = stringResource(R.string.settings_app_grid_columns),
                    value = c.toString(),
                    onLess = { columns.value = c - 1 },
                    onMore = { columns.value = c + 1 },
                    lessEnabled = c > 3,
                    moreEnabled = c < 6
                )
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_app_grid_icon_size)) {
                ExpandableSlider(
                    value = iconSize.value.toFloat(),
                    onSettled = { iconSize.value = it.roundToInt() },
                    valueRange = 60f..140f,
                    valueLabel = { stringResource(R.string.settings_icon_size_value, it.roundToInt()) }
                )
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_captions_label)) {
                DraftSwitchRow(stringResource(R.string.settings_element_show_labels_label), labels.value, { labels.value = it })
            }
        },
        StackElement(
            id = "folders", number = 3,
            title = stringResource(R.string.settings_app_grid_folders),
            summary = if (chosenFolders.isEmpty()) off else chosenFolders.joinToString { it.name },
            shown = chosenFolders.isNotEmpty(),
            draftKeys = listOf("folder_ids", "folders_alphabetical", "hide_folder_apps"),
            anchor = Offset(0.85f, 0.1f)
        ) {
            SettingsPreferenceGroup(
                title = stringResource(R.string.settings_app_grid_folders),
                info = stringResource(R.string.settings_app_grid_folders_info)
            ) {
                if (folders.isEmpty()) {
                    SettingsPreference(icon = null, title = stringResource(R.string.settings_app_grid_folders_empty), onClick = null, trailing = null, enabled = false)
                }
                folders.forEach { folder ->
                    val on = folder.id in folderIds.value
                    DraftSwitchRow(folder.name, on, { show ->
                        val ids = if (show) folderIds.value + folder.id else folderIds.value - folder.id
                        // Kept in the order of the folders list.
                        folderIds.value = folders.map { it.id }.filter { it in ids }
                    })
                }
            }
            StackDimmed(chosenFolders.isEmpty()) {
                SettingsPreferenceGroup(title = stringResource(R.string.settings_app_grid_folders_placement)) {
                    RadioPreference(stringResource(R.string.settings_app_grid_folders_top), false, alphabetical.value, onSelect = { alphabetical.value = it })
                    RadioPreference(stringResource(R.string.settings_app_grid_folders_alphabetical), true, alphabetical.value, onSelect = { alphabetical.value = it })
                    DraftSwitchRow(stringResource(R.string.settings_app_grid_hide_folder_apps), hideFolderApps.value, { hideFolderApps.value = it })
                }
            }
        }
    )

    val apps by home.gridApps.collectAsState()
    val folderItems by home.folderItemsByFolder.collectAsState()
    val iconScale by home.iconSizeScale.collectAsState()
    // Folders switched off are drawn faint, so the part is there to see.
    val drawnFolders = chosenFolders.ifEmpty { folders.take(2) }
    val drawnApps = remember(apps, drawnFolders, folderItems, hideFolderApps.value) {
        val all = if (!hideFolderApps.value) apps else {
            val inFolders = drawnFolders.flatMap { folderItems[it.id].orEmpty() }.filter { it.type == GridItemType.APP }.map { it.payload }.toSet()
            apps.filter { it.componentName.flattenToString() !in inFolders }
        }
        all.take(40)
    }
    CompositionLocalProvider(LocalSurfaceIsDark provides dark) {
        SettingsCardStackScreen(
            title = stringResource(R.string.settings_pages_app_grid),
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
                    com.bisc.flauncher.ui.home.search.AppGridContent(
                        apps = drawnApps,
                        folders = drawnFolders,
                        foldersAlphabetical = alphabetical.value,
                        folderItemsByFolder = folderItems,
                        query = "",
                        queryResults = emptyList(),
                        columns = columns.value,
                        horizontal = horizontal.value,
                        rowsPerPage = rows.value,
                        pageDots = pageDots.value,
                        globalIconScale = iconScale,
                        gridIconScale = iconSize.value / 100f,
                        showLabels = labels.value,
                        labelColor = ink,
                        barPosition = barAt,
                        barOpensTextSearch = opensTextSearch.value,
                        keyboardAutoOpen = false,
                        barAppearance = bar,
                        cornerRadiusPercent = barCorner.value,
                        keyboardCapColor = null,
                        keyboardCapCornerRadiusPercent = 0,
                        keyboardCapArrow = false,
                        loadIcon = home::loadIcon,
                        loadPackIcon = home::loadPackIcon,
                        onQueryChange = {},
                        onAppClick = {},
                        onFolderClick = {},
                        onOpenTextSearch = {},
                        modifier = Modifier.padding(top = (if (page.hideStatus.value) 0.dp else statusTop) + 16.dp)
                    )
                    if (!page.hideStatus.value) StatusBarSketch(statusTop, ink)
                }
            }
        }
    }
}
