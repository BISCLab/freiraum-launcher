package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.home.HomeViewModel

/** Every page, drag-reorderable. */
@Composable
fun SettingsPagesScreen(navController: NavController, onBack: () -> Unit, onOpenPage: (Long) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val pages by viewModel.pages.collectAsState()
    val context = LocalContext.current

    SettingsSubScreen(title = stringResource(R.string.settings_section_pages), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            if (pages.isNotEmpty()) {
                ReorderableList(
                    items = pages,
                    key = { it.id },
                    onReorder = { reordered -> viewModel.reorderPages(reordered) }
                ) { page ->
                    val label = pageLabel(page, pages)
                    val icon = when {
                        page.isHome -> Icons.Filled.Home
                        page.isAppOverview -> Icons.Filled.Apps
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    }
                    val removable = !page.isHome && !page.isAppOverview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPage(page.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (removable) {
                            IconButton(onClick = { viewModel.removePage(context, page) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.settings_pages_remove))
                            }
                        }
                    }
                }
            }
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_pages_add),
                onClick = viewModel::addPage,
                trailing = null
            )
        }
    }
}
