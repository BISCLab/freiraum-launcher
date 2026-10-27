package com.bisc.flauncher.ui.settings

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

/** One page's rows — widget rows and element boxes mixed freely in one drag-reorder list (see [PageRow]'s doc comment). */
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
    val widgetRelocationWarningCount by viewModel.widgetRelocationWarningCount.collectAsState()
    val pendingOverflowAdd by viewModel.pendingOverflowAdd.collectAsState()

    val rows = rowsByPage[pageId].orEmpty()
    val page = pages.find { it.id == pageId }
    val pageTitle = page?.let { pageLabel(it, pages) } ?: stringResource(R.string.settings_rows_title)

    var showAddSheet by remember { mutableStateOf(false) }
    var sizingKind by remember { mutableStateOf<String?>(null) }
    var sizingSpan by remember { mutableStateOf(1) }

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
        PagePreview(pageId = pageId, isHome = false, navController = navController, viewModel = viewModel)
        // Icon, name, background and status bar all live in the page's own Appearance screen, same
        // split as Home's own hub.
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_home_appearance_title),
                onClick = { navController.navigate("settings/pages/$pageId/appearance") },
                trailing = null
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
                } else {
                    ReorderableList(
                        items = rows,
                        key = { it.id },
                        onReorder = { reordered -> viewModel.reorderRows(reordered) }
                    ) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                when (row.type) {
                                    PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> onOpenRow(row.id)
                                    PageRowType.ELEMENT_BOX, PageRowType.SEARCH_BAR -> navController.navigate("settings/pages/$pageId/rows/${row.id}/elementbox")
                                    PageRowType.PANEL -> Unit
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = rowIcon(row), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(rowLabel(row), style = MaterialTheme.typography.titleMedium)
                        }
                    }
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
            showSearchBarOption = rows.none { it.type == PageRowType.SEARCH_BAR },
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
            onSearchBarSelected = {
                showAddSheet = false
                viewModel.addSearchBarRow(pageId)
            },
            onDismiss = { showAddSheet = false }
        )
    }

    if (widgetRelocationWarningCount > 0) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissWidgetRelocationWarning() },
            title = { Text(stringResource(R.string.settings_widget_relocated_title)) },
            text = { Text(stringResource(R.string.settings_widget_relocated_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissWidgetRelocationWarning() }) { Text(stringResource(R.string.settings_widget_relocated_confirm)) }
            }
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
        WidgetSizeSheet(
            row = PageRow(pageId = pageId, orderIndex = 0, type = PageRowType.WIDGET, payload = kind, spanY = sizingSpan),
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
    showSearchBarOption: Boolean,
    onWidgetKindSelected: (String) -> Unit,
    onMoreWidgetsSelected: () -> Unit,
    onElementBoxSelected: () -> Unit,
    onSearchBarSelected: () -> Unit,
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
            if (showSearchBarOption) {
                SettingsPreference(icon = Icons.Filled.Search, title = stringResource(R.string.settings_rows_add_search_bar), onClick = onSearchBarSelected, trailing = null)
            }
        }
    }
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
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.settings_rows_size_title), style = MaterialTheme.typography.titleLarge)
            WidgetSizeEditor(row = row.copy(spanY = spanY), minSpan = minSpan, maxSpan = maxSpan, onSpanChange = onSpanChange)
            TextButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_rows_save))
            }
        }
    }
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

private fun widgetKindIcon(kind: String): ImageVector = when (kind) {
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
private fun widgetKindLabel(kind: String): String = when (kind) {
    WidgetKind.CLOCK -> stringResource(R.string.picker_widget_clock)
    WidgetKind.MUSIC -> stringResource(R.string.picker_widget_music)
    WidgetKind.WEATHER -> stringResource(R.string.picker_widget_weather)
    WidgetKind.NOTES -> stringResource(R.string.picker_widget_notes)
    WidgetKind.SPACER -> stringResource(R.string.picker_widget_spacer)
    else -> kind
}
