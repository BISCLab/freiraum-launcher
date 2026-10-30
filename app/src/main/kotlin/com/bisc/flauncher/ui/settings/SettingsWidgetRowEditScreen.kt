package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.data.model.clockDateAction
import com.bisc.flauncher.data.model.clockTimeAction
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.PageRowDimens
import com.bisc.flauncher.ui.home.WidgetSizeEditor
import com.bisc.flauncher.ui.home.PageGrid
import com.bisc.flauncher.ui.home.dock.PagePickerSheet
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
    val allPages by viewModel.allPages.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val savedRow = rowsByPage[pageId]?.find { it.id == rowId }

    LaunchedEffect(savedRow) { if (savedRow == null) onBack() }
    if (savedRow == null) return
    var row by rememberDraft(savedRow)

    var hostedWidgetView by remember { mutableStateOf<android.view.View?>(null) }
    var confirmingRemove by remember { mutableStateOf(false) }
    var pickingActionTypeFor by remember { mutableStateOf<ClockActionSlot?>(null) }
    var pickingAppFor by remember { mutableStateOf<ClockActionSlot?>(null) }
    var pickingPageFor by remember { mutableStateOf<ClockActionSlot?>(null) }
    fun applyClockAction(slot: ClockActionSlot, action: GestureAction) {
        when (slot) {
            ClockActionSlot.DATE -> row = row.copy(clockDateActionType = action.type.name, clockDateActionPayload = action.payload)
            ClockActionSlot.TIME -> row = row.copy(clockTimeActionType = action.type.name, clockTimeActionPayload = action.payload)
        }
    }

    val isBuiltIn = row.type == PageRowType.WIDGET
    val minSpan = if (isBuiltIn) WidgetKind.minSpan(row.payload) else 1
    // The page's row grid, computed exactly as HomeScreen lays the page out (see PageGrid): how many
    // row units fit - fewer with Home's Dock, which shows on every page - and how tall one is drawn.
    val homePageId = allPages.find { it.isHome }?.id
    val shownDock = rowsByPage[homePageId].orEmpty().find { it.type == PageRowType.SEARCH_BAR }?.takeIf { it.enabled }
    val rowUnitCounts by viewModel.rowUnitCounts.collectAsState()
    val unitCount = rowUnitCounts[pageId] ?: 0
    val hidesStatusBar by remember(pageId) { viewModel.pageHideStatusBar(pageId) }.collectAsState(initial = true)
    val density = LocalDensity.current
    val windowHeight = with(density) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height.toDp() }
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    val statusBarInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val cutoutInset = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val gridUnit = PageRowDimens.stretchedUnit(
        PageGrid.viewport(
            windowHeight = windowHeight,
            topInset = if (hidesStatusBar) cutoutInset else maxOf(statusBarInset, cutoutInset),
            navigationBar = navigationBarInset,
            dock = shownDock
        ),
        unitCount
    )
    val maxSpan = if (isBuiltIn) WidgetKind.maxSpan(row.payload) else unitCount.takeIf { it > 0 } ?: 6

    val showsStyleControls = row.type == PageRowType.SYSTEM_WIDGET ||
        (isBuiltIn && row.payload != WidgetKind.CLOCK && row.payload != WidgetKind.SPACER)

    // A free-height widget can be anything from one plain row up to the whole page; taller can't fit,
    // since a page doesn't scroll.
    val heightPxRange = with(density) {
        val minPx = PageRowDimens.rowUnitHeight.roundToPx()
        minPx..maxOf(PageRowDimens.spanHeight(gridUnit, maxSpan).roundToPx(), minPx)
    }

    SettingsSubScreen(title = rowLabel(row), navController = navController, onBack = onBack, onSave = { viewModel.saveRow(row) }, dirty = row != savedRow) {
        WidgetSizeEditor(
            row = row,
            minSpan = minSpan,
            maxSpan = maxSpan,
            onSpanChange = { row = row.copy(spanY = it) },
            onSystemWidgetViewReady = { hostedWidgetView = it },
            heightPxRange = heightPxRange,
            onHeightPxChange = { row = row.copy(heightPx = it) },
            gridUnit = gridUnit,
            gridUnitCount = maxSpan
        )
        if (showsStyleControls) {
            ElementAppearanceSection(
                appearance = row.appearance(),
                onAppearanceChange = { row = row.withAppearance(it) },
                styleGroupTitle = surfaceLabel(R.string.settings_layer_surface_widget),
                onEyedropperCapture = if (row.type == PageRowType.SYSTEM_WIDGET) {
                    { hostedWidgetView?.let(::captureViewBitmap) }
                } else null
            )
            SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
                ExpandablePercentSlider(
                    percent = row.cornerRadiusPercent,
                    onSettled = { row = row.copy(cornerRadiusPercent = it) }
                )
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_edge_label)) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_widgets_edge_to_edge_label),
                    info = stringResource(R.string.settings_widgets_edge_to_edge_summary),
                    onClick = { row = row.copy(edgeToEdge = !row.edgeToEdge) },
                    trailing = { Switch(checked = row.edgeToEdge, onCheckedChange = { row = row.copy(edgeToEdge = it) }) }
                )
            }
        }
        if (isBuiltIn && row.payload == WidgetKind.MUSIC) {
            val musicSources by viewModel.musicSources.collectAsState()
            val coverRounded by viewModel.musicCoverRounded.collectAsState()
            SettingsPreferenceGroup(title = stringResource(R.string.settings_music_sources)) {
                MusicSourcesRow(sources = musicSources, apps = browsableApps, loadIcon = viewModel::loadIcon, onSave = viewModel::setMusicSources)
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_music_cover_label)) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_music_cover_rounded),
                    info = stringResource(R.string.settings_music_cover_rounded_info),
                    onClick = { viewModel.setMusicCoverRounded(!coverRounded) },
                    trailing = { Switch(checked = coverRounded, onCheckedChange = viewModel::setMusicCoverRounded) }
                )
            }
        }
        if (isBuiltIn && row.payload == WidgetKind.CLOCK) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_clock_style_label)) {
                ClockStyle.entries.forEach { style ->
                    RadioPreference(clockStyleLabel(style), style, row.clockStyle, { row = row.copy(clockStyle = it) })
                }
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_clock_alignment_label)) {
                ClockAlignment.entries.forEach { alignment ->
                    RadioPreference(clockAlignmentLabel(alignment), alignment, row.clockAlignment, { row = row.copy(clockAlignment = it) })
                }
            }
            // The clock isn't a card like the other built-in widgets (no fill/border/corner-shape), so
            // it skips ElementAppearanceSection entirely - but its text still reads row.appearance()
            // .contentColor at render time (ClockWidget's customInk), so this one field is exposed
            // directly instead. "Automatic" still means what ClockWidget actually falls back to when
            // unset: contrast-matched to whatever's behind it, not a fixed color, hence the plain white
            // seed here is only a preview swatch, not the real automatic behavior.
            AutomaticColorPicker(
                title = stringResource(R.string.settings_element_text_color_label),
                custom = row.appearance().contentColor,
                automaticSeed = android.graphics.Color.WHITE,
                onChange = { row = row.withAppearance(row.appearance().copy(contentColor = it)) }
            )
            SettingsPreferenceGroup(title = stringResource(R.string.settings_clock_actions_label)) {
                // Same order as on the widget: the time on top, the date below it.
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_clock_time_action),
                    summary = gestureActionSummary(row.clockTimeAction(), allPages, browsableApps),
                    onClick = { pickingActionTypeFor = ClockActionSlot.TIME }
                )
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_clock_date_action),
                    summary = gestureActionSummary(row.clockDateAction(), allPages, browsableApps),
                    onClick = { pickingActionTypeFor = ClockActionSlot.DATE }
                )
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_clock_time_format_label)) {
                ClockTimeFormat.entries.forEach { format ->
                    RadioPreference(clockTimeFormatLabel(format), format, row.clockTimeFormat, { row = row.copy(clockTimeFormat = it) })
                }
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_clock_date_format_label)) {
                var dateFormatText by remember(row.id) { mutableStateOf(row.clockDateFormat) }
                OutlinedTextField(
                    value = dateFormatText,
                    onValueChange = { dateFormatText = it; row = row.copy(clockDateFormat = it) },
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
                    onClick = { row = row.copy(clockShowNextAlarm = !row.clockShowNextAlarm) },
                    trailing = { Switch(checked = row.clockShowNextAlarm, onCheckedChange = { row = row.copy(clockShowNextAlarm = it) }) }
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
