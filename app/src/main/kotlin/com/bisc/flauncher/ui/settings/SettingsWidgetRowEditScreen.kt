package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ClockAlignment
import com.bisc.flauncher.data.model.ClockStyle
import com.bisc.flauncher.data.model.ClockTimeFormat
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.WidgetCornerStyle
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.clockDateAction
import com.bisc.flauncher.data.model.clockTimeAction
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.WidgetSizeEditor
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet
import com.bisc.flauncher.util.captureViewBitmap

private enum class ClockActionSlot { DATE, TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsWidgetRowEditScreen(
    pageId: Long,
    rowId: Long,
    navController: NavController,
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val row = rowsByPage[pageId]?.find { it.id == rowId }

    LaunchedEffect(row) { if (row == null) onBack() }
    if (row == null) return

    var hostedWidgetView by remember { mutableStateOf<android.view.View?>(null) }
    var confirmingRemove by remember { mutableStateOf(false) }
    var pickingActionTypeFor by remember { mutableStateOf<ClockActionSlot?>(null) }
    var pickingAppFor by remember { mutableStateOf<ClockActionSlot?>(null) }
    var pickingPageFor by remember { mutableStateOf<ClockActionSlot?>(null) }
    fun applyClockAction(slot: ClockActionSlot, action: GestureAction) {
        when (slot) {
            ClockActionSlot.DATE -> viewModel.setRowClockDateAction(row, action)
            ClockActionSlot.TIME -> viewModel.setRowClockTimeAction(row, action)
        }
    }

    val isBuiltIn = row.type == PageRowType.WIDGET
    val minSpan = if (isBuiltIn) WidgetKind.minSpan(row.payload) else 1
    // A page with its own dock reserves noticeably more space at the bottom than one without, so it
    // fits roughly one fewer row before the exact, position-aware clip in PagedRows would kick in anyway.
    val pageHasDock = rowsByPage[pageId].orEmpty().any { it.type == PageRowType.SEARCH_BAR }
    val maxSpan = if (isBuiltIn) WidgetKind.maxSpan(row.payload) else if (pageHasDock) 6 else 8

    val showsStyleControls = row.type == PageRowType.SYSTEM_WIDGET ||
        (isBuiltIn && row.payload != WidgetKind.CLOCK && row.payload != WidgetKind.SPACER)

    SettingsSubScreen(title = rowLabel(row), navController = navController, onBack = onBack) {
        WidgetSizeEditor(
            row = row,
            minSpan = minSpan,
            maxSpan = maxSpan,
            onSpanChange = { viewModel.setRowSpanY(row, it) },
            onSystemWidgetViewReady = { hostedWidgetView = it }
        )
        if (showsStyleControls) {
            ElementAppearanceSection(
                appearance = row.appearance(),
                onAppearanceChange = { viewModel.setRowAppearance(row, it) },
                styleGroupTitle = layerBackgroundLabel(2, R.string.settings_layer_surface_widget),
                onEyedropperCapture = if (row.type == PageRowType.SYSTEM_WIDGET) {
                    { hostedWidgetView?.let(::captureViewBitmap) }
                } else null
            )
            SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
                CornerStylePreference(value = WidgetCornerStyle.ROUNDED, selected = row.cornerStyle, onSelect = { viewModel.setRowCornerStyle(row, it) })
                CornerStylePreference(value = WidgetCornerStyle.SQUARE, selected = row.cornerStyle, onSelect = { viewModel.setRowCornerStyle(row, it) })
            }
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_widgets_edge_to_edge_label),
                    info = stringResource(R.string.settings_widgets_edge_to_edge_summary),
                    onClick = { viewModel.setRowEdgeToEdge(row, !row.edgeToEdge) },
                    trailing = { Switch(checked = row.edgeToEdge, onCheckedChange = { viewModel.setRowEdgeToEdge(row, it) }) }
                )
            }
        }
        if (isBuiltIn && row.payload == WidgetKind.CLOCK) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_clock_style_label)) {
                ClockStyle.entries.forEach { style ->
                    RadioPreference(clockStyleLabel(style), style, row.clockStyle, { viewModel.setRowClockStyle(row, it) })
                }
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_clock_alignment_label)) {
                ClockAlignment.entries.forEach { alignment ->
                    RadioPreference(clockAlignmentLabel(alignment), alignment, row.clockAlignment, { viewModel.setRowClockAlignment(row, it) })
                }
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_clock_actions_label)) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_clock_date_action),
                    summary = gestureActionSummary(row.clockDateAction(), pages, browsableApps),
                    onClick = { pickingActionTypeFor = ClockActionSlot.DATE }
                )
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_clock_time_action),
                    summary = gestureActionSummary(row.clockTimeAction(), pages, browsableApps),
                    onClick = { pickingActionTypeFor = ClockActionSlot.TIME }
                )
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_clock_time_format_label)) {
                ClockTimeFormat.entries.forEach { format ->
                    RadioPreference(clockTimeFormatLabel(format), format, row.clockTimeFormat, { viewModel.setRowClockTimeFormat(row, it) })
                }
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_clock_date_format_label)) {
                var dateFormatText by remember(row.id) { mutableStateOf(row.clockDateFormat) }
                OutlinedTextField(
                    value = dateFormatText,
                    onValueChange = { dateFormatText = it; viewModel.setRowClockDateFormat(row, it) },
                    placeholder = { Text(stringResource(R.string.settings_clock_date_format_placeholder)) },
                    supportingText = { Text(stringResource(R.string.settings_clock_date_format_summary)) },
                    singleLine = true,
                    shape = SettingsTextFieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_clock_show_next_alarm),
                    info = stringResource(R.string.settings_clock_show_next_alarm_summary),
                    onClick = { viewModel.setRowClockShowNextAlarm(row, !row.clockShowNextAlarm) },
                    trailing = { Switch(checked = row.clockShowNextAlarm, onCheckedChange = { viewModel.setRowClockShowNextAlarm(row, it) }) }
                )
            }
        }
        SettingsPreferenceGroup {
            SettingsPreference(icon = Icons.Filled.DeleteOutline, title = stringResource(R.string.settings_rows_remove), onClick = { confirmingRemove = true }, trailing = null)
        }
    }

    if (confirmingRemove) {
        AlertDialog(
            onDismissRequest = { confirmingRemove = false },
            title = { Text(stringResource(R.string.settings_rows_remove)) },
            confirmButton = {
                TextButton(onClick = { confirmingRemove = false; viewModel.removeRow(row); onBack() }) { Text(stringResource(R.string.settings_rows_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingRemove = false }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }

    pickingActionTypeFor?.let { slot ->
        val current = when (slot) {
            ClockActionSlot.DATE -> row.clockDateAction()
            ClockActionSlot.TIME -> row.clockTimeAction()
        }
        GestureActionTypeSheet(
            current = current.type,
            types = GestureActionType.entries.filterNot { it == GestureActionType.SHORTCUT },
            onSelected = { type ->
                pickingActionTypeFor = null
                when (type) {
                    GestureActionType.APP -> pickingAppFor = slot
                    GestureActionType.PAGE -> pickingPageFor = slot
                    else -> applyClockAction(slot, GestureAction(type))
                }
            },
            onDismiss = { pickingActionTypeFor = null }
        )
    }

    pickingAppFor?.let { slot ->
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app ->
                applyClockAction(slot, GestureAction(GestureActionType.APP, app.componentName.flattenToString()))
                pickingAppFor = null
            },
            onDismiss = { pickingAppFor = null }
        )
    }

    pickingPageFor?.let { slot ->
        PagePickerSheet(
            pages = pages,
            onPageSelected = { page ->
                applyClockAction(slot, GestureAction(GestureActionType.PAGE, page.id.toString()))
                pickingPageFor = null
            },
            onDismiss = { pickingPageFor = null }
        )
    }
}
