package com.bisc.flauncher.ui.home.dock

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.bisc.flauncher.R

/** Lists the shortcuts published by the selected app. */
@Composable
fun AppShortcutPickerDialog(
    appLabel: String,
    shortcuts: List<ShortcutInfo>,
    loadIcon: (ShortcutInfo) -> android.graphics.drawable.Drawable?,
    onShortcutSelected: (ShortcutInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(appLabel, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                if (shortcuts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.sidebar_shortcuts_none_for_app))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(shortcuts, key = { it.id }) { shortcut ->
                            val bitmap = remember(shortcut.id) {
                                runCatching { loadIcon(shortcut)?.toBitmap()?.asImageBitmap() }.getOrNull()
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onShortcutSelected(shortcut) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (bitmap != null) {
                                    Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(28.dp))
                                }
                                Text(
                                    (shortcut.shortLabel ?: shortcut.longLabel ?: shortcut.id).toString(),
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.widget_notes_cancel)) }
                }
            }
        }
    }
}
