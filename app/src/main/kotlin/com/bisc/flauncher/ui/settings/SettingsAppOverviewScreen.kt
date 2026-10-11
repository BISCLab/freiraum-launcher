package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.ui.home.HomeViewModel

/** Settings for the app overview: background, layout, appearance and search button. */
@Composable
fun SettingsAppOverviewScreen(navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val pageId by viewModel.appOverviewPageId.collectAsState()
    var showBackgroundSheet by remember { mutableStateOf(false) }
    val listBackground by remember(pageId) { viewModel.pageBackgroundConfig(pageId ?: -1L) }.collectAsState(initial = BackgroundConfig())
    val listAppearance by viewModel.appListContentAppearance.collectAsState()
    val surfaceIsDark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(listAppearance, listBackground)
    val hideStatusBar by remember(pageId) { viewModel.pageHideStatusBar(pageId ?: -1L) }.collectAsState(initial = false)

    SettingsSubScreen(title = stringResource(R.string.settings_section_app_overview), navController = navController, onBack = onBack) {
        Text(
            stringResource(R.string.settings_page_app_overview_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        pageId?.let { id ->
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = layerBackgroundLabel(1, R.string.settings_layer_surface_screen),
                    onClick = { showBackgroundSheet = true },
                    trailing = null
                )
            }
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_statusbar_hide_page),
                    onClick = { viewModel.setPageHideStatusBar(id, !hideStatusBar) },
                    trailing = { Switch(checked = hideStatusBar, onCheckedChange = { viewModel.setPageHideStatusBar(id, it) }) }
                )
            }
        }
        AppOverviewSettingsSection(surfaceIsDark = surfaceIsDark)
    }

    val id = pageId
    if (showBackgroundSheet && id != null) {
        val override by remember(id) { viewModel.pageBackgroundOverride(id) }.collectAsState(initial = null)
        val globalBackground by viewModel.globalBackground.collectAsState()
        SettingsEditPage(title = layerBackgroundLabel(1, R.string.settings_layer_surface_screen), onDismiss = { showBackgroundSheet = false }) {
            BackgroundOverrideSection(
                override = override,
                global = globalBackground,
                onUseGlobal = { viewModel.clearPageBackgroundConfig(id) },
                onCustomColor = { viewModel.setPageBackgroundConfig(id, it) }
            )
        }
    }
}
