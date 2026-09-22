package com.bisc.flauncher.ui.home.dock

import com.bisc.flauncher.ui.home.rememberAppIcon
import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.AppInfo

/** App selection for browsing shortcuts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutSourceAppSheet(
    apps: List<AppInfo>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onAppSelected: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(apps, key = { it.componentName.flattenToString() }) { app ->
                val bitmap = rememberAppIcon(app.componentName, loadIcon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppSelected(app) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (bitmap != null) {
                        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                    Text(app.label, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}
