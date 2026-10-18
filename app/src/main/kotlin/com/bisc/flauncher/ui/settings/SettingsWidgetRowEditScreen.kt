package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.clockDateAction
import com.bisc.flauncher.data.model.clockTimeAction
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.PageRowDimens
import com.bisc.flauncher.ui.home.WidgetSizeEditor
import com.bisc.flauncher.ui.home.safeSystemWidgetMaxHeightPx
import com.bisc.flauncher.ui.home.dock.PagePickerSheet
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet
import com.bisc.flauncher.util.captureViewBitmap
import kotlin.math.roundToInt

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
    val allPages by viewModel.allPages.collectAsState()
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
    // fits roughly one fewer row before page-overflow rebalancing would move the rest to a new page.
    val pageHasDock = rowsByPage[pageId].orEmpty().any { it.type == PageRowType.SEARCH_BAR }
    val maxSpan = if (isBuiltIn) WidgetKind.maxSpan(row.payload) else if (pageHasDock) 6 else 8

    val showsStyleControls = row.type == PageRowType.SYSTEM_WIDGET ||
        (isBuiltIn && row.payload != WidgetKind.CLOCK && row.payload != WidgetKind.SPACER)

    // Same dock-aware rule as the coarse maxSpan above, but in exact pixels for the fine stepper.
    // This is a page-level approximation (it doesn't know what other rows sit above this one, which
    // page-overflow rebalancing accounts for separately when this row no longer fits alongside them).
    // Mirrors HomeScreen's actual reserved chrome exactly (real status/nav bar insets, not flat
    // guesses) plus a safety margin, so this cap is never more generous than what genuinely fits — a
    // too-generous cap here previously let widgets be sized taller than the screen, which is what
    // caused real overflow/crashes now that a page never scrolls to compensate.
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    // A display cutout (punch-hole camera) can be taller than the plain status bar inset alone on some
    // devices, which under-reserved this cap's top chrome and is exactly what let a widget be sized
    // taller than what actually fits there — same fix FolderView already applies for the same reason.
    val statusBarInset = maxOf(
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
        WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    )
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val heightPxRange = remember(pageHasDock, configuration.screenWidthDp, configuration.screenHeightDp, density, statusBarInset, navigationBarInset) {
        with(density) {
            val minPx = PageRowDimens.rowUnitHeight.toPx().roundToInt()
            val maxPx = safeSystemWidgetMaxHeightPx(
                pageHasDock = pageHasDock,
                screenWidthPx = configuration.screenWidthDp.dp.toPx().roundToInt(),
                screenHeightPx = configuration.screenHeightDp.dp.toPx().roundToInt(),
                densityValue = density.density,
                statusBarPx = statusBarInset.toPx().roundToInt(),
                navigationBarPx = navigationBarInset.toPx().roundToInt()
            )
            minPx..maxOf(maxPx, minPx)
        }
    }

    SettingsSubScreen(title = rowLabel(row), navController = navController, onBack = onBack) {
        WidgetSizeEditor(
            row = row,
            minSpan = minSpan,
            maxSpan = maxSpan,
            onSpanChange = { viewModel.setRowSpanY(row, it) },
            onSystemWidgetViewReady = { hostedWidgetView = it },
            heightPxRange = heightPxRange,
            onHeightPxChange = { viewModel.setRowHeightPx(row, it) }
        )
        if (showsStyleControls) {
            ElementAppearanceSection(
                appearance = row.appearance(),
                onAppearanceChange = { viewModel.setRowAppearance(row, it) },
                styleGroupTitle = surfaceLabel(R.string.settings_layer_surface_widget),
                onEyedropperCapture = if (row.type == PageRowType.SYSTEM_WIDGET) {
                    { hostedWidgetView?.let(::captureViewBitmap) }
                } else null
            )
            SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
                SettledSlider(
                    value = row.cornerRadiusPercent.toFloat(),
                    onSettled = { viewModel.setRowCornerRadiusPercent(row, it.roundToInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    label = { live ->
                        Text(
                            stringResource(R.string.settings_corner_radius_value, live.roundToInt()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
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
            // The clock isn't a card like the other built-in widgets (no fill/border/corner-shape), so
            // it skips ElementAppearanceSection entirely — but its text still reads row.appearance()
            // .contentColor at render time (ClockWidget's customInk), so this one field is exposed
            // directly instead. "Automatic" still means what ClockWidget actually falls back to when
            // unset: contrast-matched to whatever's behind it, not a fixed color, hence the plain white
            // seed here is only a preview swatch, not the real automatic behavior.
            AutomaticColorPicker(
                title = stringResource(R.string.settings_element_text_color_label),
                custom = row.appearance().contentColor,
                automaticSeed = android.graphics.Color.WHITE,
                onChange = { viewModel.setRowAppearance(row, row.appearance().copy(contentColor = it)) }
            )
            SettingsPreferenceGroup(title = stringResource(R.string.settings_clock_actions_label)) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_clock_date_action),
                    summary = gestureActionSummary(row.clockDateAction(), allPages, browsableApps),
                    onClick = { pickingActionTypeFor = ClockActionSlot.DATE }
                )
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_clock_time_action),
                    summary = gestureActionSummary(row.clockTimeAction(), allPages, browsableApps),
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
            pages = allPages,
            onPageSelected = { page ->
                applyClockAction(slot, GestureAction(GestureActionType.PAGE, page.id.toString()))
                pickingPageFor = null
            },
            onDismiss = { pickingPageFor = null }
        )
    }
}
