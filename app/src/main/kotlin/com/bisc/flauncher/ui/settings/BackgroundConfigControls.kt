package com.bisc.flauncher.ui.settings

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import com.bisc.flauncher.ui.home.dock.rememberBackgroundBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.bisc.flauncher.data.model.BundledWallpaper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.BackgroundSource
import com.bisc.flauncher.data.model.asBrush
import com.bisc.flauncher.util.BackgroundImageStore
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Editor for a layer 1 background: bundled wallpaper, custom image, color, opacity and darkening. */
@Composable
fun BackgroundConfigSection(
    imageKey: String,
    config: BackgroundConfig,
    onConfigChange: (BackgroundConfig) -> Unit
) {
    var colorExpanded by remember { mutableStateOf(false) }
    var showLayerHelp by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var offerSystemPath by remember { mutableStateOf<String?>(null) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) { BackgroundImageStore.save(context, uri, imageKey) }
                if (path != null) {
                    onConfigChange(config.withImage(path))
                    offerSystemPath = path
                }
            }
        }
    }
    val pickBundled = { wallpaper: BundledWallpaper ->
        BackgroundImageStore.delete(context, imageKey)
        onConfigChange(config.withImage(wallpaper.path))
        offerSystemPath = wallpaper.path
    }
    val launchImagePicker = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val customImagePath = config.imagePath?.takeIf { BundledWallpaper.fromPath(it) == null }
    val imageSelected = config.source == BackgroundSource.IMAGE

    SettingsPreferenceGroup(
        title = layerBackgroundLabel(1, R.string.settings_layer_surface_screen),
        onInfoClick = { showLayerHelp = true }
    ) {
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_background_source_custom_image),
            leading = customImagePath?.let { path ->
                {
                    val bitmap = rememberBackgroundBitmap(path)
                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 36.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    ) {
                        if (bitmap != null) Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                    }
                }
            },
            onClick = launchImagePicker,
            trailing = {
                RadioButton(
                    selected = imageSelected && customImagePath != null,
                    onClick = launchImagePicker
                )
            }
        )
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_background_source_color),
            leading = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(config.fill.asBrush())
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            },
            onClick = {
                if (config.source != BackgroundSource.COLOR) onConfigChange(config.copy(source = BackgroundSource.COLOR))
                colorExpanded = !colorExpanded
            },
            trailing = {
                RadioButton(
                    selected = config.source == BackgroundSource.COLOR,
                    onClick = {
                        if (config.source != BackgroundSource.COLOR) onConfigChange(config.copy(source = BackgroundSource.COLOR))
                        colorExpanded = true
                    }
                )
            }
        )
        if (colorExpanded && config.source == BackgroundSource.COLOR) {
            ColorFillPickerContent(
                fill = config.fill,
                onFillChange = { fill -> onConfigChange(config.copy(fill = fill)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    SettingsPreferenceGroup(title = stringResource(R.string.settings_background_bundled_label)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BundledWallpaper.entries.forEach { wallpaper ->
                val selected = imageSelected && config.imagePath == wallpaper.path
                val shape = RoundedCornerShape(12.dp)
                Column(
                    modifier = Modifier.width(96.dp).clip(shape).clickable { pickBundled(wallpaper) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        Image(
                            painter = painterResource(wallpaper.drawable),
                            contentDescription = stringResource(wallpaper.label),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 96.dp, height = 170.dp)
                                .clip(shape)
                                .border(
                                    if (selected) 3.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape
                                )
                        )
                        if (selected) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                    }
                    Text(
                        stringResource(wallpaper.label),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        if (imageSelected && config.imagePath != null) {
            SettingsPreference(
                icon = Icons.Outlined.Wallpaper,
                title = stringResource(R.string.settings_background_system_row),
                onClick = { launchSystemWallpaper(context, config.imagePath) }
            )
        }
    }

    SettingsPreferenceGroup(title = stringResource(R.string.settings_background_opacity_label)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.settings_background_opacity_value, (config.opacity * 100).roundToInt()),
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = config.opacity,
                onValueChange = { onConfigChange(config.copy(opacity = it)) },
                valueRange = 0f..1f
            )
        }
    }

    SettingsPreferenceGroup(title = stringResource(R.string.settings_background_darken_label)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.settings_background_darken_value, (config.darken * 100).roundToInt()),
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = config.darken,
                onValueChange = { onConfigChange(config.copy(darken = it)) },
                valueRange = 0f..1f
            )
        }
    }

    offerSystemPath?.let { path ->
        AlertDialog(
            onDismissRequest = { offerSystemPath = null },
            title = { Text(stringResource(R.string.settings_background_system_title)) },
            text = { Text(stringResource(R.string.settings_background_system_text)) },
            confirmButton = {
                TextButton(onClick = { offerSystemPath = null; launchSystemWallpaper(context, path) }) {
                    Text(stringResource(R.string.settings_background_system_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { offerSystemPath = null }) { Text(stringResource(R.string.settings_background_system_dismiss)) }
            }
        )
    }

    if (showLayerHelp) {
        LayerExplainerSheet(onDismiss = { showLayerHelp = false })
    }
}

/** Opens Android's own wallpaper screen, which asks whether to use the image for home screen, lock screen or both. */
private fun launchSystemWallpaper(context: Context, path: String) {
    val uri = BackgroundImageStore.exportForSystem(context, path) ?: return
    val crop = WallpaperManager.getInstance(context).getCropAndSetWallpaperIntent(uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(crop)
    } catch (_: ActivityNotFoundException) {
        val attach = Intent(Intent.ACTION_ATTACH_DATA).setDataAndType(uri, "image/jpeg")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { context.startActivity(Intent.createChooser(attach, null)) }
    }
}

/** Selecting an image also raises a fully transparent background, since it would otherwise stay invisible. */
private fun BackgroundConfig.withImage(path: String) =
    copy(source = BackgroundSource.IMAGE, imagePath = path, opacity = if (opacity <= 0f) 1f else opacity)
