package com.bisc.flauncher.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R

/** The single background shared by every surface — pages, app overview, both search surfaces. */
@Composable
fun SettingsGlobalBackgroundScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val globalBackground by viewModel.globalBackground.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_section_global_background), navController = navController, onBack = onBack) {
        BackgroundConfigSection(
            imageKey = "global",
            config = globalBackground,
            onConfigChange = viewModel::setGlobalBackground
        )
    }
}
