package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.ui.MotionLevel
import com.bisc.flauncher.ui.MotionPrefs
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.systemAnimationsOff
import com.bisc.flauncher.ui.theme.noRippleClickable

/** Carousel pages in their order (Home among them), then the utility pages. */
internal fun statusBarPages(allPages: List<LauncherPage>): List<LauncherPage> =
    allPages.filter { it.kind == LauncherPageKind.ROWS }.sortedBy { it.orderIndex } +
        allPages.filter { it.kind == LauncherPageKind.APP_LIST } +
        allPages.filter { it.kind == LauncherPageKind.SEARCH } +
        allPages.filter { it.kind == LauncherPageKind.APP_GRID }

/** "Hidden on 5 of 8" - pages plus folders, for the one-line entry under System. */
@Composable
internal fun statusBarSummary(viewModel: SettingsViewModel): String {
    val allPages by viewModel.allPages.collectAsState()
    val searchHidden by viewModel.searchHideStatusBar.collectAsState()
    val folderHidden by viewModel.folderHideStatusBar.collectAsState()
    val pages = statusBarPages(allPages)
    var hidden = if (folderHidden) 1 else 0
    pages.forEach { page ->
        val pageHidden = if (page.kind == LauncherPageKind.SEARCH) searchHidden
            else remember(page.id) { viewModel.pageHideStatusBar(page.id) }.collectAsState(initial = false).value
        if (pageHidden) hidden++
    }
    return stringResource(R.string.settings_statusbar_summary, hidden, pages.size + 1)
}

/** "On, 6 of 7" / "Off" for the one-line entry under System. */
@Composable
internal fun motionSummary(viewModel: SettingsViewModel): String {
    val level by viewModel.motionLevel.collectAsState()
    val prefs by viewModel.motionPrefs.collectAsState()
    val context = LocalContext.current
    if (systemAnimationsOff(context)) return stringResource(R.string.settings_motion_summary_system_off)
    if (level == MotionLevel.OFF) return stringResource(R.string.settings_motion_summary_off)
    val each = listOf(prefs.appZoom, prefs.overlayZoom, prefs.folderZoom, prefs.pageDepth, prefs.backGesture, prefs.settingsSlide, prefs.gestureRipples)
    return stringResource(R.string.settings_motion_summary_on, each.count { it }, each.size)
}

/** Status bar: one switch for every place at once, then each page and folders on its own. */
@Composable
fun SettingsStatusBarScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val everywhere by viewModel.statusBarHiddenEverywhere.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val searchHidden by viewModel.searchHideStatusBar.collectAsState()
    val folderHidden by viewModel.folderHideStatusBar.collectAsState()
    var confirmingHide by remember { mutableStateOf(false) }
    // Hiding everywhere asks first; showing it again just does it.
    val setEverywhere = { hide: Boolean -> if (hide) confirmingHide = true else viewModel.setStatusBarHiddenEverywhere(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_statusbar_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(info = stringResource(R.string.settings_statusbar_hide_all_summary)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_statusbar_hide_all),
                onClick = { setEverywhere(!everywhere) },
                trailing = { Switch(checked = everywhere, onCheckedChange = setEverywhere) }
            )
            statusBarPages(allPages).forEach { page ->
                if (page.kind == LauncherPageKind.SEARCH) {
                    SlimSwitchRow(pageLabel(page, allPages), searchHidden, true, viewModel::setSearchHideStatusBar)
                } else {
                    val hidden by remember(page.id) { viewModel.pageHideStatusBar(page.id) }.collectAsState(initial = false)
                    SlimSwitchRow(pageLabel(page, allPages), hidden, true) { viewModel.setPageHideStatusBar(page.id, it) }
                }
            }
            SlimSwitchRow(stringResource(R.string.settings_section_folder), folderHidden, true, viewModel::setFolderHideStatusBar)
        }
    }

    if (confirmingHide) {
        AlertDialog(
            onDismissRequest = { confirmingHide = false },
            title = { Text(stringResource(R.string.settings_statusbar_hide_all)) },
            text = { Text(stringResource(R.string.settings_statusbar_confirm_hide_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.setStatusBarHiddenEverywhere(true); confirmingHide = false }) {
                    Text(stringResource(R.string.widget_notes_save))
                }
            },
            dismissButton = { TextButton(onClick = { confirmingHide = false }) { Text(stringResource(R.string.widget_notes_cancel)) } }
        )
    }
}

/** Animations: one switch for all, then each animation on its own, and the charging animation. */
@Composable
fun SettingsAnimationsScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val level by viewModel.motionLevel.collectAsState()
    val prefs by viewModel.motionPrefs.collectAsState()
    val chargingAnimation by viewModel.chargingAnimationEnabled.collectAsState()
    val chargingColor by viewModel.chargingAnimationColor.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    var systemOff by remember { mutableStateOf(systemAnimationsOff(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        systemOff = systemAnimationsOff(context)
        onPauseOrDispose { }
    }
    val on = level == MotionLevel.FULL && !systemOff
    val setOn = { enabled: Boolean -> viewModel.setMotionLevel(if (enabled) MotionLevel.FULL else MotionLevel.OFF) }
    fun set(p: MotionPrefs) = viewModel.setMotionPrefs(p)

    SettingsSubScreen(title = stringResource(R.string.settings_motion_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(info = stringResource(R.string.settings_motion_info)) {
            if (systemOff) {
                // Android's own switch overrides everything here - say so, with a way there.
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_motion_system_off),
                    summary = stringResource(R.string.settings_motion_system_off_summary),
                    onClick = { runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) } },
                    trailing = null
                )
            }
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_motion_all),
                enabled = !systemOff,
                onClick = { setOn(!on) },
                trailing = { Switch(checked = on, enabled = !systemOff, onCheckedChange = setOn) }
            )
            SlimSwitchRow(stringResource(R.string.settings_motion_app_zoom), prefs.appZoom, on) { set(prefs.copy(appZoom = it)) }
            SlimSwitchRow(stringResource(R.string.settings_motion_overlay_zoom), prefs.overlayZoom, on) { set(prefs.copy(overlayZoom = it)) }
            SlimSwitchRow(stringResource(R.string.settings_motion_folder_zoom), prefs.folderZoom, on) { set(prefs.copy(folderZoom = it)) }
            SlimSwitchRow(stringResource(R.string.settings_motion_page_depth), prefs.pageDepth, on) { set(prefs.copy(pageDepth = it)) }
            SlimSwitchRow(stringResource(R.string.settings_motion_back_gesture), prefs.backGesture, on) { set(prefs.copy(backGesture = it)) }
            SlimSwitchRow(stringResource(R.string.settings_motion_settings_slide), prefs.settingsSlide, on) { set(prefs.copy(settingsSlide = it)) }
            SlimSwitchRow(stringResource(R.string.settings_motion_ripples), prefs.gestureRipples, on) { set(prefs.copy(gestureRipples = it)) }
        }
        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_charging_label),
            info = stringResource(R.string.settings_charging_animation_summary)
        ) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_charging_animation),
                onClick = { viewModel.setChargingAnimationEnabled(!chargingAnimation) },
                trailing = { Switch(checked = chargingAnimation, onCheckedChange = viewModel::setChargingAnimationEnabled) }
            )
            if (chargingAnimation) {
                AccentOverrideRows(
                    title = stringResource(R.string.settings_charging_color),
                    own = chargingColor,
                    global = accentColor,
                    onChange = viewModel::setChargingAnimationColor
                )
            }
        }
    }
}

/** A slimmer switch row under a main switch, so it reads as that one's detail; greyed out while
 *  [enabled] is false. */
@Composable
internal fun SlimSwitchRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .noRippleClickable(enabled = enabled) { onChange(!checked) }
            .padding(start = 16.dp, end = 8.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, enabled = enabled, onCheckedChange = onChange, modifier = Modifier.scale(0.75f))
    }
}
