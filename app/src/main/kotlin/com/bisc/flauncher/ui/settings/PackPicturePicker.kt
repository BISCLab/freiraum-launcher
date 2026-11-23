package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.R
import java.io.File

/** Every picture of the installed wallpapers - the ones for each screen, the extra ones meant for
 *  further pages and the lock screen picture - to take one as a page's own background. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PackPicturePicker(onPick: (File) -> Unit, onDismiss: () -> Unit, viewModel: WallpaperPacksViewModel = hiltViewModel()) {
    val packs by viewModel.packs.collectAsState()
    val german = java.util.Locale.getDefault().language == "de"
    SettingsEditPage(title = stringResource(R.string.settings_background_from_packs), onDismiss = onDismiss) {
        if (packs.isEmpty()) {
            Text(stringResource(R.string.settings_background_from_packs_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        packs.forEach { pack ->
            val pictures = (listOfNotNull(pack.images["home"], pack.images["appList"], pack.images["search"], pack.images["appGrid"]) + pack.extras + listOfNotNull(pack.lock))
                .distinct()
            if (pictures.isEmpty()) return@forEach
            SettingsGroupLabel(pack.displayName(german))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pictures.forEach { file ->
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onPick(file); onDismiss() }) {
                        PackThumb(file, 140.dp)
                    }
                }
            }
        }
    }
}
