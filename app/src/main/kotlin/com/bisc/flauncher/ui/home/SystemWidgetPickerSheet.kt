package com.bisc.flauncher.ui.home

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.bisc.flauncher.R

/** Lists real third-party AppWidgets available to host. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemWidgetPickerSheet(
    providers: List<AppWidgetProviderInfo>,
    onProviderSelected: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (providers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.system_widget_picker_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(providers, key = { it.provider.flattenToString() }) { provider ->
                    val label = remember(provider) { provider.loadLabel(packageManager) }
                    val bitmap = remember(provider) {
                        runCatching { provider.loadIcon(context, 0)?.toBitmap()?.asImageBitmap() }.getOrNull()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProviderSelected(provider) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (bitmap != null) {
                            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(32.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            )
                        }
                        Text(label, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}
