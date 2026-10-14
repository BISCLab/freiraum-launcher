package com.bisc.flauncher.ui.settings

import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
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

/** Editor for the single global layer 1 background shared by every surface: image, color, opacity, darkening. */
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
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) { BackgroundImageStore.save(context, uri, imageKey) }
                if (path != null) onConfigChange(config.withImage(path))
            }
        }
    }
    val launchImagePicker = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val customImagePath = config.imagePath
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

    SettingsPreferenceGroup(title = stringResource(R.string.settings_background_source_custom_image)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A large, bordered/checkmarked preview card — feedback that a pick actually worked,
            // instead of just the tiny 24x36 thumbnail in the row above.
            val customSelected = imageSelected && customImagePath != null
            WallpaperCard(
                selected = customSelected,
                label = stringResource(R.string.settings_background_source_custom_image),
                onClick = {
                    if (customImagePath != null && !customSelected) onConfigChange(config.withImage(customImagePath))
                    else launchImagePicker()
                }
            ) {
                if (customImagePath != null) {
                    val bitmap = rememberBackgroundBitmap(customImagePath)
                    if (bitmap != null) Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    SettingsPreferenceGroup(title = stringResource(R.string.settings_background_opacity_label)) {
        SettledSlider(
            value = config.opacity,
            onSettled = { onConfigChange(config.copy(opacity = it)) },
            valueRange = 0f..1f,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            label = { live ->
                Text(
                    stringResource(R.string.settings_background_opacity_value, (live * 100).roundToInt()),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }

    SettingsPreferenceGroup(title = stringResource(R.string.settings_background_darken_label)) {
        SettledSlider(
            value = config.darken,
            onSettled = { onConfigChange(config.copy(darken = it)) },
            valueRange = 0f..1f,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            label = { live ->
                Text(
                    stringResource(R.string.settings_background_darken_value, (live * 100).roundToInt()),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }

    if (showLayerHelp) {
        LayerExplainerSheet(onDismiss = { showLayerHelp = false })
    }
}

/**
 * Editor for one surface's layer 1 when it can only ever be "the shared global background" or "my own
 * color" — no per-surface image anymore (see SettingsRepository.backgroundOverrideFor for why: a custom
 * image only ever flickered against *another* custom image, never against a color or the already-
 * GPU-resident global image, so images only live at the global level now).
 */
@Composable
fun BackgroundOverrideSection(
    override: BackgroundConfig?,
    global: BackgroundConfig,
    onUseGlobal: () -> Unit,
    onCustomColor: (BackgroundConfig) -> Unit,
    // Off for callers whose own page is already titled "Background" (Home's own Appearance screen,
    // for one) — repeating "Layer 1 · Screen" plus a help icon explaining the old Layer 1/2/3 model
    // right underneath is redundant there. Still on by default for every other caller (a page's row
    // editor, a folder, etc.) where this section sits alongside other content on a differently-
    // titled screen and the caption is the only thing saying what it's for.
    showLayerHeader: Boolean = true
) {
    var showLayerHelp by remember { mutableStateOf(false) }
    val usesGlobal = override == null
    val current = override ?: BackgroundConfig(source = BackgroundSource.COLOR)

    SettingsPreferenceGroup(
        title = if (showLayerHeader) layerBackgroundLabel(1, R.string.settings_layer_surface_screen) else null,
        onInfoClick = if (showLayerHeader) ({ showLayerHelp = true }) else null
    ) {
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_background_use_global),
            summary = stringResource(R.string.settings_background_use_global_summary),
            leading = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (global.source == BackgroundSource.COLOR) global.fill.asBrush() else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    if (global.source == BackgroundSource.IMAGE) {
                        val bitmap = rememberBackgroundBitmap(global.imagePath)
                        if (bitmap != null) Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                    }
                }
            },
            onClick = onUseGlobal,
            trailing = { RadioButton(selected = usesGlobal, onClick = onUseGlobal) }
        )
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_background_source_color),
            leading = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(current.fill.asBrush())
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            },
            onClick = { onCustomColor(current.copy(source = BackgroundSource.COLOR)) },
            trailing = {
                RadioButton(
                    selected = !usesGlobal,
                    onClick = { onCustomColor(current.copy(source = BackgroundSource.COLOR)) }
                )
            }
        )
        if (!usesGlobal) {
            ColorFillPickerContent(
                fill = current.fill,
                onFillChange = { fill -> onCustomColor(current.copy(fill = fill)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    if (!usesGlobal) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_background_opacity_label)) {
            SettledSlider(
                value = current.opacity,
                onSettled = { onCustomColor(current.copy(opacity = it)) },
                valueRange = 0f..1f,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                label = { live ->
                    Text(
                        stringResource(R.string.settings_background_opacity_value, (live * 100).roundToInt()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_background_darken_label)) {
            SettledSlider(
                value = current.darken,
                onSettled = { onCustomColor(current.copy(darken = it)) },
                valueRange = 0f..1f,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                label = { live ->
                    Text(
                        stringResource(R.string.settings_background_darken_value, (live * 100).roundToInt()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }

    if (showLayerHelp) {
        LayerExplainerSheet(onDismiss = { showLayerHelp = false })
    }
}

/** One selectable wallpaper option: a 96x170 preview, bordered and checkmarked when [selected]. */
@Composable
private fun WallpaperCard(selected: Boolean, label: String, onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier.width(96.dp).clip(shape).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 170.dp)
                .clip(shape)
                .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape)
        ) {
            content()
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
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Selecting an image also raises a fully transparent background, since it would otherwise stay invisible. */
private fun BackgroundConfig.withImage(path: String) =
    copy(source = BackgroundSource.IMAGE, imagePath = path, opacity = if (opacity <= 0f) 1f else opacity)
