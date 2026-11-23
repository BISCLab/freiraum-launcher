package com.bisc.flauncher

import android.content.pm.LauncherApps
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.bisc.flauncher.data.repository.FolderRepository
import com.bisc.flauncher.data.repository.PageContentRepository
import com.bisc.flauncher.data.repository.ShortcutRepository
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.theme.FLauncherTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** "Add to home screen" from another app (a website from Chrome, a contact, a playlist): asks where
 *  the shortcut goes - Home, a folder or only the search - and pins it there. */
@AndroidEntryPoint
class PinShortcutActivity : ComponentActivity() {
    @Inject lateinit var shortcutRepository: ShortcutRepository
    @Inject lateinit var pageContentRepository: PageContentRepository
    @Inject lateinit var folderRepository: FolderRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcherApps = getSystemService(LauncherApps::class.java)
        val request = runCatching { launcherApps?.getPinItemRequest(intent) }.getOrNull()
        val info = request?.takeIf { it.isValid && it.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT }?.shortcutInfo
        if (request == null || info == null) {
            finish()
            return
        }
        val label = (info.shortLabel ?: info.longLabel ?: info.id).toString()
        val icon = runCatching { launcherApps?.getShortcutIconDrawable(info, resources.displayMetrics.densityDpi)?.toBitmap(192, 192)?.asImageBitmap() }.getOrNull()
        setContent {
            val theme by settingsRepository.theme.collectAsState(initial = com.bisc.flauncher.ui.theme.ThemePreference.SYSTEM)
            val folders by folderRepository.folders.collectAsState(initial = emptyList())
            FLauncherTheme(themePreference = theme) {
                var target by remember { mutableStateOf<Target>(Target.Home) }
                AlertDialog(
                    onDismissRequest = { finish() },
                    icon = {
                        if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(56.dp))
                        else Icon(Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, modifier = Modifier.size(40.dp))
                    },
                    title = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    text = {
                        Column(modifier = Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                            Text(stringResource(R.string.pin_shortcut_where), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                            TargetRow(stringResource(R.string.pin_shortcut_home), target == Target.Home) { target = Target.Home }
                            folders.forEach { folder ->
                                TargetRow(stringResource(R.string.pin_shortcut_folder, folder.name), target == Target.InFolder(folder.id)) { target = Target.InFolder(folder.id) }
                            }
                            TargetRow(stringResource(R.string.pin_shortcut_search_only), target == Target.SearchOnly) { target = Target.SearchOnly }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val chosen = target
                            lifecycleScope.launch {
                                // Accepting pins it for this launcher, so it can be started later.
                                if (runCatching { request.accept() }.getOrDefault(false)) {
                                    shortcutRepository.pin(info)
                                    when (chosen) {
                                        Target.Home -> pageContentRepository.addShortcutToHome(info.`package`, info.id, label)
                                        is Target.InFolder -> folderRepository.addShortcutToFolder(chosen.folderId, info.`package`, info.id, label)
                                        Target.SearchOnly -> Unit
                                    }
                                    android.widget.Toast.makeText(this@PinShortcutActivity, getString(R.string.pin_shortcut_added, label), android.widget.Toast.LENGTH_SHORT).show()
                                }
                                finish()
                            }
                        }) { Text(stringResource(R.string.pin_shortcut_add)) }
                    },
                    dismissButton = { TextButton(onClick = { finish() }) { Text(stringResource(R.string.widget_notes_cancel)) } }
                )
            }
        }
    }

    private sealed interface Target {
        data object Home : Target
        data class InFolder(val folderId: Long) : Target
        data object SearchOnly : Target
    }
}

@Composable
private fun TargetRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
