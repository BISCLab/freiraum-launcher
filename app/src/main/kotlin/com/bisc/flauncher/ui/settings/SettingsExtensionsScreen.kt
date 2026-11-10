package com.bisc.flauncher.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.home.HomeViewModel

/** The page in the browser that shows every design, wallpaper pack and widget source there is. The
 *  launcher has no internet access, so finding and downloading happens there. */
internal const val EXTENSIONS_URL = "https://bisclab.github.io/freiraum-launcher/"

internal fun openExtensionsPage(context: Context, section: String = "") {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(EXTENSIONS_URL + section)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Extensions: what can be added to the launcher - designs, wallpaper packs and widgets - with what
 *  is installed of each, and the way to the page that shows everything there is. */
@Composable
fun SettingsExtensionsScreen(
    navController: NavController,
    onBack: () -> Unit,
    designsViewModel: DesignsViewModel = hiltViewModel(),
    packsViewModel: WallpaperPacksViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val designs by designsViewModel.designs.collectAsState()
    val packs by packsViewModel.packs.collectAsState()
    SettingsSubScreen(title = stringResource(R.string.settings_group_extensions), navController = navController, onBack = onBack) {
        Text(
            stringResource(R.string.extensions_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HubCard(
                icon = Icons.Filled.AutoAwesome,
                title = stringResource(R.string.designs_title),
                what = pluralStringResource(R.plurals.extensions_available, designs.size, designs.size),
                contents = stringResource(R.string.designs_entry_summary),
                onClick = { navController.navigate("settings/appearance/designs") }
            )
            HubCard(
                icon = Icons.Filled.Wallpaper,
                title = stringResource(R.string.packs_title),
                what = pluralStringResource(R.plurals.extensions_available, packs.size, packs.size),
                contents = stringResource(R.string.packs_entry_summary),
                onClick = { navController.navigate("settings/wallpaper_packs") }
            )
            HubCard(
                icon = Icons.Filled.Widgets,
                title = stringResource(R.string.extensions_widgets_title),
                what = stringResource(R.string.extensions_widgets_what),
                contents = stringResource(R.string.extensions_widgets_contents),
                onClick = { navController.navigate("settings/extensions/widgets") }
            )
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.extensions_browse),
                summary = stringResource(R.string.extensions_browse_summary),
                onClick = { openExtensionsPage(context) },
                trailing = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
            )
        }
    }
}

/** Widgets: the ones that come with the launcher, and Weather, which needs an app that brings the
 *  data (the launcher itself has no internet access). */
@Composable
fun SettingsExtensionWidgetsScreen(navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val weather by viewModel.weather.collectAsState()
    SettingsSubScreen(title = stringResource(R.string.extensions_widgets_title), navController = navController, onBack = onBack) {
        Text(
            stringResource(R.string.extensions_widgets_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        SettingsPreferenceGroup(title = stringResource(R.string.extensions_widgets_included)) {
            SettingsPreference(icon = Icons.Filled.Schedule, title = stringResource(R.string.picker_widget_clock), onClick = null, trailing = null)
            SettingsPreference(icon = Icons.Filled.EditNote, title = stringResource(R.string.picker_widget_notes), onClick = null, trailing = null)
            SettingsPreference(icon = Icons.Filled.MusicNote, title = stringResource(R.string.picker_widget_music), onClick = null, trailing = null)
        }
        SettingsPreferenceGroup(
            title = stringResource(R.string.extensions_widgets_with_source),
            info = stringResource(R.string.extensions_weather_info)
        ) {
            val snapshot = weather
            SettingsPreference(
                icon = Icons.Filled.Cloud,
                title = stringResource(R.string.picker_widget_weather),
                summary = if (snapshot == null) stringResource(R.string.extensions_weather_no_source)
                else stringResource(
                    R.string.extensions_weather_receiving,
                    android.text.format.DateUtils.getRelativeTimeSpanString(snapshot.updatedAtMillis).toString()
                ),
                onClick = null,
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.extensions_weather_find),
                summary = stringResource(R.string.extensions_weather_find_summary),
                onClick = { openExtensionsPage(context, "#widgets") },
                trailing = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
            )
        }
    }
}
