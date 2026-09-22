package com.bisc.flauncher.ui.home.dock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.settings.SettingsPreference

/** Choice between adding a single app and adding all installed apps to the sidebar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSidebarAppsChooserSheet(
    onPickOne: () -> Unit,
    onAddAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            SettingsPreference(icon = Icons.Filled.Add, title = stringResource(R.string.sidebar_apps_add_single), onClick = onPickOne, trailing = null)
            SettingsPreference(icon = Icons.Filled.DoneAll, title = stringResource(R.string.sidebar_apps_add_all), onClick = onAddAll, trailing = null)
        }
    }
}
