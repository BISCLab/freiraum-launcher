package com.bisc.flauncher.ui.settings

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R

/**
 * How every folder without a look of its own looks once opened, in the same order as a single
 * folder's "Opened folder": fill, opacity, border, corner rounding, captions; then what happens
 * around a folder while it is open. Everything is a draft, written with Save.
 */
@Composable
fun SettingsFolderGlobalScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val savedLook by viewModel.folderCardAppearance.collectAsState()
    val savedCorner by viewModel.folderCornerPercent.collectAsState()
    val savedLabels by viewModel.folderShowLabels.collectAsState()
    val savedHideStatusBar by viewModel.folderHideStatusBar.collectAsState()
    val savedHideDock by viewModel.folderHideDock.collectAsState()
    var look by rememberDraft(savedLook)
    var corner by rememberDraft(savedCorner)
    var labels by rememberDraft(savedLabels)
    var hideStatusBar by rememberDraft(savedHideStatusBar)
    var hideDock by rememberDraft(savedHideDock)

    SettingsSubScreen(
        title = stringResource(R.string.settings_folder_global_label),
        navController = navController,
        onBack = onBack,
        onSave = {
            if (look != savedLook) viewModel.setFolderCardAppearance(look)
            if (corner != savedCorner) viewModel.setFolderCornerPercent(corner)
            if (labels != savedLabels) viewModel.setFolderShowLabels(labels)
            if (hideStatusBar != savedHideStatusBar) viewModel.setFolderHideStatusBar(hideStatusBar)
            if (hideDock != savedHideDock) viewModel.setFolderHideDock(hideDock)
        },
        dirty = look != savedLook || corner != savedCorner || labels != savedLabels || hideStatusBar != savedHideStatusBar || hideDock != savedHideDock,
        header = { LookSample(LookSampleKind.FOLDER, look, corner) }
    ) {
        PartIntro(stringResource(R.string.settings_folder_global_intro))
        ElementAppearanceSection(appearance = look, onAppearanceChange = { look = it }, styleGroupTitle = stringResource(R.string.settings_element_fill_label))
        SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
            ExpandablePercentSlider(percent = corner, onSettled = { corner = it })
        }
        CaptionsSection(
            showLabels = labels,
            onShowLabelsChange = { labels = it },
            appearance = look,
            onAppearanceChange = { look = it },
            title = stringResource(R.string.settings_captions_label)
        )
        SettingsPreferenceGroup(title = stringResource(R.string.settings_folder_while_open)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_folder_hide_status_bar),
                onClick = { hideStatusBar = !hideStatusBar },
                trailing = { Switch(checked = hideStatusBar, onCheckedChange = { hideStatusBar = it }) }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_folder_hide_dock),
                info = stringResource(R.string.settings_folder_hide_dock_summary),
                onClick = { hideDock = !hideDock },
                trailing = { Switch(checked = hideDock, onCheckedChange = { hideDock = it }) }
            )
        }
    }
}
