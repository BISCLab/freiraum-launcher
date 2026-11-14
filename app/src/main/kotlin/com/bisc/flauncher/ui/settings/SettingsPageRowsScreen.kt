package com.bisc.flauncher.ui.settings

import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import com.bisc.flauncher.data.model.withAppearance
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ClockAlignment
import com.bisc.flauncher.data.model.ClockStyle
import com.bisc.flauncher.data.model.ClockTimeFormat
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.SystemWidgetPickerSheet
import com.bisc.flauncher.ui.home.WidgetSizeEditor

/** One page's rows - widget rows and element boxes mixed freely in one drag-reorder list (see [PageRow]'s doc comment). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageRowsScreen(
    pageId: Long,
    navController: NavController,
    onBack: () -> Unit,
    onOpenRow: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val systemWidgetPickerOpenForPage by viewModel.systemWidgetPickerOpenForPage.collectAsState()
    val pendingBindIntent by viewModel.pendingBindIntent.collectAsState()
    val pendingConfigureIntent by viewModel.pendingConfigureIntent.collectAsState()
    val pendingOverflowAdd by viewModel.pendingOverflowAdd.collectAsState()

    // The Dock and the Panel are Home's (shown on every page), never edited from another page.
    val rows = rowsByPage[pageId].orEmpty().filter { it.type != PageRowType.SEARCH_BAR && it.type != PageRowType.PANEL }
    val page = pages.find { it.id == pageId }
    val pageTitle = page?.let { pageLabel(it, pages) } ?: stringResource(R.string.settings_rows_title)

    var showAddSheet by remember { mutableStateOf(false) }
    var sizingKind by remember { mutableStateOf<String?>(null) }
    var sizingSpan by remember { androidx.compose.runtime.mutableIntStateOf(1) }

    val bindWidgetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.onSystemWidgetBindResult(result.resultCode == Activity.RESULT_OK) }
    val configureWidgetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.onSystemWidgetConfigureResult(result.resultCode == Activity.RESULT_OK) }
    LaunchedEffect(pendingBindIntent) { pendingBindIntent?.let { bindWidgetLauncher.launch(it) } }
    LaunchedEffect(pendingConfigureIntent) { pendingConfigureIntent?.let { configureWidgetLauncher.launch(it) } }


    SettingsSubScreen(title = pageTitle, navController = navController, onBack = onBack) {
        // Tap a part of the miniature to edit it; the list below stays as the second way in.
        var selectedRowId by remember { mutableStateOf<Long?>(null) }
        PagePreview(
            pageId = pageId,
            isHome = false,
            navController = navController,
            viewModel = viewModel,
            selectedRowId = selectedRowId,
            onRowTap = { selectedRowId = if (selectedRowId == it.id) null else it.id }
        )
        // The picked row: move it up or down right here, or open it.
        val picked = rows.find { it.id == selectedRowId }
        if (picked != null) {
            val at = rows.indexOf(picked)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SettingsCardShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(rowIcon(picked), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(rowLabel(picked), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f).padding(start = 8.dp))
                androidx.compose.material3.IconButton(onClick = { viewModel.reorderRows(rows.toMutableList().apply { add(at - 1, removeAt(at)) }) }, enabled = at > 0) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.settings_move_up), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                androidx.compose.material3.IconButton(onClick = { viewModel.reorderRows(rows.toMutableList().apply { add(at + 1, removeAt(at)) }) }, enabled = at < rows.lastIndex) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.settings_move_down), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                androidx.compose.material3.FilledTonalButton(onClick = {
                    selectedRowId = null
                    when (picked.type) {
                        PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> onOpenRow(picked.id)
                        else -> navController.navigate("settings/pages/$pageId/rows/${picked.id}/elementbox")
                    }
                }) { Text(stringResource(R.string.settings_row_open)) }
            }
        } else if (rows.isNotEmpty()) {
            Text(
                stringResource(R.string.settings_rows_pick_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        // Icon, name, background and status bar all live in the page's own Appearance screen, same
        // split as Home's own hub.
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_home_appearance_title),
                onClick = { navController.navigate("settings/pages/$pageId/appearance") }
            )
        }

        // Home's Dock and Panel come along to every page; each can be left out here.
        val withoutDockPanel by viewModel.pagesWithoutDockPanel.collectAsState()
        SettingsPreferenceGroup(title = stringResource(R.string.settings_page_dock_panel_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_page_shows_dock),
                onClick = { viewModel.setPageShowsDock(pageId, pageId in withoutDockPanel.first) },
                trailing = { androidx.compose.material3.Switch(checked = pageId !in withoutDockPanel.first, onCheckedChange = { viewModel.setPageShowsDock(pageId, it) }) }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_page_shows_panel),
                onClick = { viewModel.setPageShowsPanel(pageId, pageId in withoutDockPanel.second) },
                trailing = { androidx.compose.material3.Switch(checked = pageId !in withoutDockPanel.second, onCheckedChange = { viewModel.setPageShowsPanel(pageId, it) }) }
            )
        }
        run {
            SettingsPreferenceGroup {
                if (rows.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_rows_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = Icons.Filled.Add,
                    title = stringResource(R.string.settings_rows_add),
                    onClick = { showAddSheet = true },
                    trailing = null
                )
            }
        }
        if (page != null && !page.isHome) {
            val context = androidx.compose.ui.platform.LocalContext.current
            RemovePageGroup(label = pageTitle) {
                viewModel.removePage(context, page)
                onBack()
            }
        }
    }

    if (showAddSheet) {
        AddRowSheet(
            onWidgetKindSelected = { kind ->
                showAddSheet = false
                sizingSpan = WidgetKind.defaultSpan(kind)
                sizingKind = kind
            },
            onMoreWidgetsSelected = {
                showAddSheet = false
                viewModel.openSystemWidgetPicker(pageId)
            },
            onElementBoxSelected = {
                showAddSheet = false
                viewModel.addElementBox(pageId)
            },
            onDismiss = { showAddSheet = false }
        )
    }

    if (pendingOverflowAdd != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelPendingOverflowAdd,
            title = { Text(stringResource(R.string.settings_page_full_title)) },
            text = { Text(stringResource(R.string.settings_page_full_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmPendingOverflowAdd) { Text(stringResource(R.string.settings_page_full_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelPendingOverflowAdd) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }

    sizingKind?.let { kind ->
        val freeUnits by androidx.compose.runtime.produceState<Int?>(null, pageId) { value = viewModel.freeRowUnits(pageId) }
        WidgetSizeSheet(
            freeUnits = freeUnits,
            row = newWidgetPreviewRow(pageId, kind, sizingSpan),
            spanY = sizingSpan,
            minSpan = WidgetKind.minSpan(kind),
            maxSpan = WidgetKind.maxSpan(kind),
            onSpanChange = { sizingSpan = it },
            onSave = {
                viewModel.addWidgetRow(pageId, kind, sizingSpan)
                sizingKind = null
            },
            onDismiss = { sizingKind = null }
        )
    }

    systemWidgetPickerOpenForPage?.let { targetPageId ->
        val providers = remember(targetPageId) { viewModel.installedSystemWidgetProviders() }
        SystemWidgetPickerSheet(
            providers = providers,
            onProviderSelected = { provider -> viewModel.startAddSystemWidget(targetPageId, provider) },
            onDismiss = viewModel::dismissSystemWidgetPicker
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddRowSheet(
    onWidgetKindSelected: (String) -> Unit,
    onMoreWidgetsSelected: () -> Unit,
    onElementBoxSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(WidgetKind.CLOCK, WidgetKind.MUSIC, WidgetKind.WEATHER, WidgetKind.NOTES, WidgetKind.SPACER).forEach { kind ->
                SettingsPreference(icon = widgetKindIcon(kind), title = widgetKindLabel(kind), onClick = { onWidgetKindSelected(kind) }, trailing = null)
            }
            SettingsPreference(icon = Icons.Filled.Widgets, title = stringResource(R.string.picker_widget_more), onClick = onMoreWidgetsSelected, trailing = null)
            SettingsPreference(icon = Icons.Filled.Apps, title = stringResource(R.string.settings_rows_add_element_box), onClick = onElementBoxSelected, trailing = null)
        }
    }
}

/** A not-yet-added widget row as the sizing step previews it - with the same corner radius and
 *  fill a new widget actually gets (see PageContentRepository.addWidgetRow), not PageRow's defaults. */
@Composable
internal fun newWidgetPreviewRow(pageId: Long, kind: String, spanY: Int): PageRow {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return PageRow(
        pageId = pageId, orderIndex = 0, type = PageRowType.WIDGET, payload = kind, spanY = spanY,
        cornerRadiusPercent = com.bisc.flauncher.ui.theme.DEFAULT_WIDGET_CORNER_RADIUS_PERCENT
    ).withAppearance(com.bisc.flauncher.ui.theme.defaultBoxAppearanceFor(isDark))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetSizeSheet(
    row: PageRow,
    spanY: Int,
    minSpan: Int,
    maxSpan: Int,
    onSpanChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    /** Row units still free on the page; null = not known. */
    freeUnits: Int? = null
) {
    // Opened in full at once: half open on a tall screen, Save would lie below the edge.
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.85f).dp
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // The preview scrolls; the room left and Save stay in view below it, however tall it is.
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = minOf(640.dp, maxHeight)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.settings_rows_size_title), style = MaterialTheme.typography.titleLarge)
                WidgetSizeEditor(row = row.copy(spanY = spanY), minSpan = minSpan, maxSpan = maxSpan, onSpanChange = onSpanChange)
            }
            if (freeUnits != null) {
                val fits = spanY <= freeUnits
                Text(
                    if (fits) androidx.compose.ui.res.pluralStringResource(R.plurals.settings_page_room_left, freeUnits, freeUnits)
                    else stringResource(R.string.settings_page_room_too_small, freeUnits),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (fits) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }
            androidx.compose.material3.Button(onClick = onSave, shape = SettingsCardShape, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.settings_rows_save))
            }
        }
    }
}

/** Asks when something does not fit on its page any more: a new page for it, or not. */
@Composable
internal fun PageFullDialog(viewModel: com.bisc.flauncher.ui.home.HomeViewModel) {
    val pending by viewModel.pendingOverflowAdd.collectAsState()
    if (pending == null) return
    androidx.compose.material3.AlertDialog(
        onDismissRequest = viewModel::cancelPendingOverflowAdd,
        title = { Text(stringResource(R.string.settings_page_full_title)) },
        text = { Text(stringResource(R.string.settings_page_full_message)) },
        confirmButton = { TextButton(onClick = viewModel::confirmPendingOverflowAdd) { Text(stringResource(R.string.settings_page_full_confirm)) } },
        dismissButton = { TextButton(onClick = viewModel::cancelPendingOverflowAdd) { Text(stringResource(R.string.widget_notes_cancel)) } }
    )
}

internal fun rowIcon(row: PageRow): ImageVector = when (row.type) {
    PageRowType.WIDGET -> widgetKindIcon(row.payload)
    PageRowType.SYSTEM_WIDGET -> Icons.Filled.Widgets
    PageRowType.ELEMENT_BOX -> Icons.Filled.Apps
    PageRowType.SEARCH_BAR -> Icons.Filled.Search
    PageRowType.PANEL -> Icons.Filled.Dashboard
}

@Composable
internal fun rowLabel(row: PageRow): String = when (row.type) {
    PageRowType.WIDGET -> widgetKindLabel(row.payload)
    PageRowType.SYSTEM_WIDGET -> stringResource(R.string.picker_widget_more)
    PageRowType.ELEMENT_BOX -> stringResource(R.string.settings_rows_add_element_box)
    PageRowType.SEARCH_BAR -> stringResource(R.string.settings_rows_add_search_bar)
    PageRowType.PANEL -> stringResource(R.string.settings_home_panel_label)
}

internal fun widgetKindIcon(kind: String): ImageVector = when (kind) {
    WidgetKind.CLOCK -> Icons.Filled.Schedule
    WidgetKind.MUSIC -> Icons.Filled.MusicNote
    WidgetKind.WEATHER -> Icons.Filled.Cloud
    WidgetKind.NOTES -> Icons.Filled.EditNote
    WidgetKind.SPACER -> Icons.Filled.HorizontalRule
    else -> Icons.Filled.Schedule
}

@Composable
internal fun clockAlignmentLabel(alignment: String): String = when (alignment) {
    ClockAlignment.START -> stringResource(R.string.settings_clock_alignment_start)
    ClockAlignment.END -> stringResource(R.string.settings_clock_alignment_end)
    else -> stringResource(R.string.settings_clock_alignment_center)
}

@Composable
internal fun clockStyleLabel(style: String): String = when (style) {
    ClockStyle.DIGITAL -> stringResource(R.string.settings_clock_style_digital)
    ClockStyle.DIGITAL_LARGE -> stringResource(R.string.settings_clock_style_digital_large)
    ClockStyle.STACKED -> stringResource(R.string.settings_clock_style_stacked)
    ClockStyle.ANALOG -> stringResource(R.string.settings_clock_style_analog)
    ClockStyle.ORBIT -> stringResource(R.string.settings_clock_style_orbit)
    ClockStyle.BINARY -> stringResource(R.string.settings_clock_style_binary)
    else -> style
}

@Composable
internal fun clockTimeFormatLabel(format: String): String = when (format) {
    ClockTimeFormat.SYSTEM -> stringResource(R.string.settings_clock_time_format_system)
    ClockTimeFormat.H24 -> stringResource(R.string.settings_clock_time_format_24h)
    ClockTimeFormat.H12 -> stringResource(R.string.settings_clock_time_format_12h)
    else -> format
}

@Composable
internal fun widgetKindLabel(kind: String): String = when (kind) {
    WidgetKind.CLOCK -> stringResource(R.string.picker_widget_clock)
    WidgetKind.MUSIC -> stringResource(R.string.picker_widget_music)
    WidgetKind.WEATHER -> stringResource(R.string.picker_widget_weather)
    WidgetKind.NOTES -> stringResource(R.string.picker_widget_notes)
    WidgetKind.SPACER -> stringResource(R.string.picker_widget_spacer)
    else -> kind
}
