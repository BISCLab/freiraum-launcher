package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.DockVisibilityMode
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.dockRows
import com.bisc.flauncher.data.model.panelRows
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.LocalAppIconLoader
import com.bisc.flauncher.ui.home.LocalRowUnitHeight
import com.bisc.flauncher.ui.home.LocalSurfaceIsDark
import com.bisc.flauncher.ui.home.PageRowDimens
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.dock.DockDimens
import com.bisc.flauncher.ui.home.dock.DockPanelSurface
import com.bisc.flauncher.ui.home.dock.FreeEdge
import com.bisc.flauncher.ui.home.dock.freeEdgeBorder
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark
import com.bisc.flauncher.ui.stackElement

/**
 * The Home page's settings as a card stack: the page built from its real rows, Panel and Dock
 * with a number on each, the list of parts, and the options of the chosen one. Order, Panel and
 * Dock are a draft until Save; contents, adding rows and the name have pages of their own.
 */
@Composable
fun SettingsHomeStackScreen(
    pageId: Long,
    navController: NavController,
    onBack: () -> Unit,
    startElement: String? = null,
    home: HomeViewModel = hiltViewModel()
) {
    val draft: StackDraft = viewModel()
    val page = rememberPageDraft(draft, home, pageId)
    val rowsByPage by home.rowsByPage.collectAsState()
    val rows = rowsByPage[pageId].orEmpty()
    val savedDock = rows.find { it.type == PageRowType.SEARCH_BAR }
    val savedPanel = rows.find { it.type == PageRowType.PANEL }
    val savedOthers = rows.filter { it.type != PageRowType.SEARCH_BAR && it.type != PageRowType.PANEL }

    // Only what the stack itself changes is kept, and put onto the row as it is stored at Save, so
    // a change made meanwhile on a page of its own (the Panel's rows, the Dock's elements) stays.
    val panelEdit = draft.field("panel", savedPanel?.edit()) { e -> if (e != null && savedPanel != null) home.saveRow(savedPanel.with(e)) }
    val dockEdit = draft.field("dock", savedDock?.edit()) { e -> if (e != null && savedDock != null) home.saveRow(savedDock.with(e)) }
    val panel = savedPanel?.let { r -> panelEdit.value?.let { r.with(it) } ?: r }
    val dock = savedDock?.let { r -> dockEdit.value?.let { r.with(it) } ?: r }
    val order = draft.field("order", savedOthers.map { it.id }) { ids ->
        // Panel and Dock go along with their changes, so this write keeps them.
        val ordered = ids.mapNotNull { id -> savedOthers.find { it.id == id } } + savedOthers.filter { it.id !in ids }
        home.reorderRows(ordered + listOfNotNull(dock, panel))
    }
    // Rows added elsewhere meanwhile come last.
    val others = order.value.mapNotNull { id -> savedOthers.find { it.id == id } } + savedOthers.filter { it.id !in order.value }

    val dark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), draft.shownBackground(page.shown))
    val off = stringResource(R.string.stack_summary_off)
    val pages by home.pages.collectAsState()

    fun move(row: PageRow, delta: Int) {
        val ids = others.map { it.id }.toMutableList()
        val from = ids.indexOf(row.id)
        val to = from + delta
        if (from < 0 || to !in ids.indices) return
        ids.add(to, ids.removeAt(from))
        order.value = ids
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

    @Composable
    fun RowLook(row: PageRow, onChange: (PageRow) -> Unit) {
        ElementAppearanceSection(row.appearance(), { onChange(row.withAppearance(it)) }, stringResource(R.string.settings_element_fill_label))
        SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
            CornerPercentPicker(percent = row.cornerRadiusPercent, onChange = { onChange(row.copy(cornerRadiusPercent = it)) })
        }
    }

    val elements = buildList {
        add(StackElement(
            id = "page", number = 0,
            title = stringResource(R.string.settings_search_page_label),
            summary = stringResource(R.string.stack_page_summary_home),
            draftKeys = page.keys
        ) {
            PageStackOptions(page, null, navController, home) {
                SettingsPreferenceGroup(title = stringResource(R.string.stack_rows_label)) {
                    LinkRow(stringResource(R.string.stack_rows_manage), null, "settings/pages/$pageId/home/layout")
                }
            }
            SettingsGroupLabel(stringResource(R.string.stack_name_label))
            pages.find { it.id == pageId }?.let { p ->
                SettingsPreferenceGroup {
                    PageIdentityRows(
                        page = p,
                        pages = pages,
                        onOpenIcon = { navController.navigate("settings/pages/$pageId/icon") },
                        onRename = { name -> home.renamePage(pageId, name) }
                    )
                }
            }
        })
        panel?.let { row ->
            add(StackElement(
                id = "panel", number = 1,
                title = stringResource(R.string.settings_home_panel_label),
                summary = if (row.enabled) stringResource(R.string.stack_summary_only_panel) else off,
                shown = row.enabled,
                state = PANEL,
                draftKeys = listOf("panel"),
                anchor = Offset(0.92f, 1f),
                sample = { LookSample(LookSampleKind.PANEL, row.appearance(), row.cornerRadiusPercent) }
            ) {
                StackShowSwitch(stringResource(R.string.stack_show), row.enabled, { panelEdit.value = row.copy(enabled = it).edit() })
                StackDimmed(!row.enabled) {
                    SettingsPreferenceGroup {
                        LinkRow(stringResource(R.string.settings_panel_content_title), stringResource(R.string.settings_hub_panel_content_what), "settings/pages/$pageId/home/panel/elements")
                    }
                    RowLook(row) { panelEdit.value = it.edit() }
                }
            })
        }
        others.forEachIndexed { index, row ->
            val label = rowLabel(row)
            val twin = others.count { rowLabel(it) == label } > 1
            add(StackElement(
                id = "row_${row.id}", number = index + 2,
                title = if (twin) "$label ${others.filter { rowLabel(it) == label }.indexOf(row) + 1}" else label,
                summary = stringResource(R.string.stack_row_position_value, index + 1, others.size),
                draftKeys = listOf("order"),
                anchor = Offset(0.92f, 0.5f)
            ) {
                SettingsPreferenceGroup {
                    LinkRow(
                        stringResource(R.string.stack_row_edit),
                        stringResource(R.string.stack_row_edit_summary),
                        if (row.type == PageRowType.ELEMENT_BOX) "settings/pages/$pageId/rows/${row.id}/elementbox" else "settings/pages/$pageId/rows/${row.id}"
                    )
                }
                SettingsPreferenceGroup(title = stringResource(R.string.settings_search_bar_position_label)) {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.stack_row_position_value, index + 1, others.size),
                        onClick = null,
                        trailing = {
                            Row {
                                IconButton(onClick = { move(row, -1) }, enabled = index > 0) {
                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.settings_move_up))
                                }
                                IconButton(onClick = { move(row, 1) }, enabled = index < others.lastIndex) {
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.settings_move_down))
                                }
                            }
                        }
                    )
                }
            })
        }
        dock?.let { row ->
            add(StackElement(
                id = "dock", number = others.size + 2,
                title = stringResource(R.string.settings_home_dock_label),
                summary = if (row.enabled) dockVisibilityModeLabel(row.dockVisibilityMode) else off,
                shown = row.enabled,
                draftKeys = listOf("dock"),
                anchor = Offset(0.92f, 0f),
                sample = { LookSample(LookSampleKind.DOCK, row.appearance(), row.cornerRadiusPercent) }
            ) {
                StackShowSwitch(stringResource(R.string.stack_show), row.enabled, { dockEdit.value = row.copy(enabled = it).edit() })
                StackDimmed(!row.enabled) {
                    SettingsPreferenceGroup {
                        LinkRow(stringResource(R.string.settings_dock_elements_title), null, "settings/pages/$pageId/home/layout/dock/elements")
                    }
                    SettingsPreferenceGroup(title = stringResource(R.string.settings_dock_visibility_label)) {
                        DockVisibilityMode.entries.forEach { mode ->
                            RadioPreference(dockVisibilityModeLabel(mode), mode, row.dockVisibilityMode, onSelect = { dockEdit.value = row.copy(dockVisibilityMode = it).edit() })
                        }
                    }
                    RowLook(row) { dockEdit.value = it.edit() }
                }
            })
        }
    }

    CompositionLocalProvider(LocalSurfaceIsDark provides dark) {
        SettingsCardStackScreen(
            title = stringResource(R.string.settings_pages_home),
            navController = navController,
            onBack = onBack,
            draft = draft,
            elements = elements,
            states = if (panel != null) listOf("normal" to stringResource(R.string.stack_state_normal), PANEL to stringResource(R.string.stack_state_panel)) else emptyList(),
            startElement = startElement
        ) { state, _ ->
            HomeReplica(
                background = draft.shownBackground(page.shown),
                home = home,
                pageId = pageId,
                page = page,
                dark = dark,
                rows = others,
                panel = panel?.takeIf { state == PANEL },
                dock = dock
            )
        }
    }
}

private const val PANEL = "panel"

/** What the stack changes on the Panel and Dock rows. */
private data class RowEdit(val enabled: Boolean, val look: ElementAppearance, val corner: Int, val visibility: DockVisibilityMode)

private fun PageRow.edit() = RowEdit(enabled, appearance(), cornerRadiusPercent, dockVisibilityMode)

private fun PageRow.with(e: RowEdit): PageRow = withAppearance(e.look).copy(enabled = e.enabled, cornerRadiusPercent = e.corner, dockVisibilityMode = e.visibility)

/** The Home page as on the home screen, from its own parts: the rows, the Dock at the bottom and,
 *  while open, the Panel from the top. Every part is drawn, also the ones switched off. */
@Composable
private fun HomeReplica(
    background: com.bisc.flauncher.data.model.BackgroundConfig,
    home: HomeViewModel,
    pageId: Long,
    page: PageDraft,
    dark: Boolean,
    rows: List<PageRow>,
    panel: PageRow?,
    dock: PageRow?
) {
    val entriesByRow by home.rowEntriesByRow.collectAsState()
    val folders by home.folders.collectAsState()
    val folderItems by home.folderItemsByFolder.collectAsState()
    val mapTargets by home.savedMapTargets.collectAsState()
    val websites by home.pinnedWebsites.collectAsState()
    val allPages by home.allPages.collectAsState()
    val contacts by home.pinnedContacts.collectAsState()
    val buttons by home.actionButtons.collectAsState()
    val iconScale by home.iconSizeScale.collectAsState()
    val unitCounts by home.rowUnitCounts.collectAsState()
    val panelWidgets by home.panelWidgets.collectAsState()
    val ink = resolveElementColors(ElementAppearance(fillMode = ElementFillMode.NONE), dark).contentColor
    val statusTop = replicaStatusHeight()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    @Composable
    fun Entries(row: PageRow, entries: List<com.bisc.flauncher.data.model.RowEntry>, shape: List<Int>, dockLike: Boolean) {
        com.bisc.flauncher.ui.home.GridEntries(
            entries = entries,
            columns = 1,
            rowColumns = shape,
            fixedSlots = true,
            referenceColumnCount = if (dockLike) DockDimens.REFERENCE_SLOT_COUNT else null,
            rowGap = if (dockLike) DockDimens.rowGap else 0.dp,
            folders = folders,
            folderItemsByFolder = folderItems,
            savedMapTargets = mapTargets,
            pinnedWebsites = websites,
            pages = allPages,
            loadIcon = home::loadIcon,
            loadPackIcon = home::loadPackIcon,
            resolveLabel = home::resolveLabel,
            onFolderClick = {},
            onShortcutClick = {},
            onOpenPageLink = { _, _ -> },
            pinnedContacts = contacts,
            loadContactPhoto = home::loadContactPhoto,
            actionButtons = buttons,
            showLabels = row.showLabels,
            labelColor = row.appearance().contentColor?.let { Color(it) } ?: Color.White,
            iconSizeScale = iconScale
        )
    }

    CompositionLocalProvider(
        LocalAppIconLoader provides home.iconLoader,
        // The real widgets keep their one view on the home screen; here they show as empty cards.
        com.bisc.flauncher.ui.home.widgets.LocalHostAppWidgets provides false
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            BackgroundFill(background, Modifier.fillMaxSize())
            val top = (if (page.hideStatus.value) 0.dp else statusTop) + 16.dp
            val reserve = dock?.let { DockDimens.height(it) + PageRowDimens.rowSpacing + navBottom } ?: (navBottom + 16.dp)
            val unit = PageRowDimens.stretchedUnit(maxHeight - top - reserve, unitCounts[pageId] ?: 0)
            CompositionLocalProvider(LocalRowUnitHeight provides unit) {
                com.bisc.flauncher.ui.home.PageRowsColumn(
                    rows = rows,
                    entriesByRow = entriesByRow,
                    folders = folders,
                    folderItemsByFolder = folderItems,
                    savedMapTargets = mapTargets,
                    pinnedWebsites = websites,
                    pages = allPages,
                    loadIcon = home::loadIcon,
                    loadPackIcon = home::loadPackIcon,
                    resolveLabel = home::resolveLabel,
                    onFolderClick = {},
                    iconSizeScale = iconScale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = com.bisc.flauncher.ui.MaxContentWidth)
                        .padding(top = top, bottom = reserve)
                )
            }
            if (dock != null) {
                val look = dock.appearance()
                val radius = 28.dp * (dock.cornerRadiusPercent.coerceIn(0, 100) / 100f)
                DockPanelSurface(
                    appearance = look.copy(borderEnabled = false),
                    shape = RoundedCornerShape(topStart = radius, topEnd = radius),
                    effectTarget = "dock",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = com.bisc.flauncher.ui.MaxContentWidth)
                        .fillMaxWidth()
                        .height(DockDimens.shownHeight(dock) + navBottom)
                        .stackElement("dock")
                        .then(if (look.borderEnabled) Modifier.freeEdgeBorder(1.dp, resolveElementColors(look).borderColor, radius, FreeEdge.TOP) else Modifier)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = DockDimens.innerHorizontalPadding, end = DockDimens.innerHorizontalPadding, top = DockDimens.topPadding, bottom = DockDimens.innerVerticalPadding + navBottom)
                    ) { Entries(dock, entriesByRow[dock.id].orEmpty(), dock.dockRows(), dockLike = true) }
                }
            }
            if (panel != null) {
                val look = panel.appearance()
                val radius = 28.dp * (panel.cornerRadiusPercent.coerceIn(0, 100) / 100f)
                val shape = panel.panelRows()
                val widgetsHeight = panelWidgets.fold(0.dp) { sum, item -> sum + PageRowDimens.spanHeight(PageRowDimens.rowUnitHeight, item.spanY) + 8.dp }
                DockPanelSurface(
                    appearance = look.copy(borderEnabled = false),
                    shape = RoundedCornerShape(bottomStart = radius, bottomEnd = radius),
                    effectTarget = "panel",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = com.bisc.flauncher.ui.MaxContentWidth)
                        .fillMaxWidth()
                        .height((96.dp * shape.size + 64.dp + widgetsHeight).coerceIn(140.dp, maxHeight))
                        .stackElement("panel")
                        .then(if (look.borderEnabled) Modifier.freeEdgeBorder(1.dp, resolveElementColors(look).borderColor, radius, FreeEdge.BOTTOM) else Modifier)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(top = statusTop).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val entries = entriesByRow[panel.id].orEmpty()
                        val widgetsByRow = com.bisc.flauncher.ui.home.widgetsAbove(panelWidgets, shape.size)
                        var from = 0
                        shape.forEachIndexed { i, cols ->
                            com.bisc.flauncher.ui.home.PanelWidgets(widgetsByRow[i], onExecuteAction = {})
                            Entries(panel, entries.filter { it.orderIndex in from until from + cols }.map { it.copy(orderIndex = it.orderIndex - from) }, listOf(cols), dockLike = false)
                            from += cols
                        }
                        com.bisc.flauncher.ui.home.PanelWidgets(widgetsByRow[shape.size], onExecuteAction = {})
                    }
                }
            }
            if (!page.hideStatus.value) StatusBarSketch(statusTop, if (panel != null) resolveElementColors(panel.appearance()).contentColor else ink)
        }
    }
}
