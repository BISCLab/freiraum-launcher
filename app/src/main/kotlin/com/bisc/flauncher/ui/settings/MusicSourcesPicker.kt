package com.bisc.flauncher.ui.settings

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppInfo

/** The music widget's "Music sources" row: which apps' playback it shows. */
@Composable
fun MusicSourcesRow(sources: Set<String>, apps: List<AppInfo>, loadIcon: (ComponentName) -> ImageBitmap?, onSave: (Set<String>) -> Unit) {
    var open by remember { mutableStateOf(false) }
    SettingsPreference(
        icon = null,
        title = stringResource(R.string.settings_music_sources),
        summary = if (sources.isEmpty()) stringResource(R.string.settings_music_sources_all)
        else pluralStringResource(R.plurals.settings_music_sources_count, sources.size, sources.size),
        info = stringResource(R.string.settings_music_sources_info),
        onClick = { open = true },
        trailing = null
    )
    if (open) {
        MusicSourcesPicker(sources = sources, apps = apps, loadIcon = loadIcon, onSave = { onSave(it); open = false }, onDismiss = { open = false })
    }
}

/** Checks the apps allowed to show up in the music widget; none checked means every app. Edits a
 *  draft with its own Save bar and asks before leaving with unsaved changes, like the other editors. */
@Composable
private fun MusicSourcesPicker(
    sources: Set<String>,
    apps: List<AppInfo>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onSave: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf(sources) }
    var query by remember { mutableStateOf("") }
    var confirmLeave by remember { mutableStateOf(false) }
    val dirty = draft != sources
    val leave = { if (dirty) confirmLeave = true else onDismiss() }
    // One row per app package (an app can have several launcher entries), checked ones first.
    val packages = remember(apps) { apps.distinctBy { it.componentName.packageName }.sortedBy { it.label.lowercase() } }
    val shown = remember(packages, query, sources) {
        packages.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
            .sortedByDescending { it.componentName.packageName in sources }
    }

    if (confirmLeave) {
        UnsavedChangesDialog(
            onSave = { confirmLeave = false; onSave(draft) },
            onDiscard = { confirmLeave = false; onDismiss() },
            onKeepEditing = { confirmLeave = false }
        )
    }
    SettingsFixedDialog(
        title = stringResource(R.string.settings_music_sources),
        onDismiss = leave,
        bottomBar = { SettingsSaveBar(enabled = dirty, onSave = { onSave(draft) }) }
    ) {
        Text(
            stringResource(R.string.settings_music_sources_info),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.settings_manage_apps_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = null) }
                }
            },
            singleLine = true,
            shape = SettingsTextFieldShape,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Text(
            if (draft.isEmpty()) stringResource(R.string.settings_music_sources_all)
            else pluralStringResource(R.plurals.settings_music_sources_count, draft.size, draft.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 8.dp)) {
            items(shown, key = { it.componentName.packageName }) { app ->
                val pkg = app.componentName.packageName
                val checked = pkg in draft
                val toggle = { draft = if (checked) draft - pkg else draft + pkg }
                val bitmap = com.bisc.flauncher.ui.home.rememberAppIcon(app.componentName, loadIcon)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = toggle).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = checked, onCheckedChange = { toggle() })
                    if (bitmap != null) Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(28.dp))
                    else Spacer(Modifier.size(28.dp))
                    Text(app.label, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}
