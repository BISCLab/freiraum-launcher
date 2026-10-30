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

/** Editor for the single global background shared by every surface: image, color, darken/lighten shade. */
@Composable
fun BackgroundConfigSection(
    imageKey: String,
    config: BackgroundConfig,
    onConfigChange: (BackgroundConfig) -> Unit
) {
    var colorExpanded by remember { mutableStateOf(false) }
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

    SettingsPreferenceGroup(title = surfaceLabel(R.string.settings_layer_surface_screen)) {
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
            // A large, bordered/checkmarked preview card - feedback that a pick actually worked,
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

    SettingsPreferenceGroup(title = stringResource(R.string.settings_background_shade_label)) {
        ExpandableShadePercentSlider(
            percent = (config.shade * 100).roundToInt(),
            onSettled = { onConfigChange(config.copy(shade = it / 100f)) }
        )
    }

}

/**
 * Editor for one surface's background: the shared global background (optionally darkened/lightened
 * just for this surface), its own image, or its own color - laid out like [BackgroundConfigSection].
 * Selecting "Color" opens its own window directly, same as every other color picker in the app,
 * with that color's Darken/Lighten inside it; the global and image options show theirs inline.
 */
@Composable
fun BackgroundOverrideSection(
    override: BackgroundConfig?,
    global: BackgroundConfig,
    onUseGlobal: () -> Unit,
    onCustomColor: (BackgroundConfig) -> Unit,
    /** Storage key for this surface's own picked image (see [BackgroundImageStore]). */
    imageKey: String,
    /** This surface's own shade over the global background; null = the global one's own. */
    globalShade: Float?,
    onGlobalShadeChange: (Float) -> Unit,
    // Null for callers whose own page is already titled "Background" (Home's own Appearance screen,
    // for one) - repeating the title right underneath would just be redundant there. Defaults to
    // "Screen" for every other caller (a page's row editor, a folder, etc.) where this section sits
    // alongside other content on a differently-titled screen and the caption is the only thing
    // saying what it's for.
    groupTitle: String? = surfaceLabel(R.string.settings_layer_surface_screen)
) {
    val usesGlobal = override == null
    val current = override ?: BackgroundConfig(source = BackgroundSource.COLOR)
    val ownImagePath = override?.imagePath
    val usesOwnImage = override?.source == BackgroundSource.IMAGE && ownImagePath != null
    var colorEditorOpen by remember { mutableStateOf(false) }
    val openColorEditor = { onCustomColor(current.copy(source = BackgroundSource.COLOR)); colorEditorOpen = true }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) { BackgroundImageStore.save(context, uri, imageKey) }
                if (path != null) onCustomColor(current.withImage(path))
            }
        }
    }
    val launchImagePicker = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    // Re-selecting keeps an already-picked image; only the first time (or via the big preview card
    // below) opens the system picker.
    val selectOwnImage = {
        if (ownImagePath != null) onCustomColor(current.withImage(ownImagePath)) else launchImagePicker()
    }

    SettingsPreferenceGroup(title = groupTitle) {
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
            title = stringResource(R.string.settings_background_source_custom_image),
            leading = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    if (ownImagePath != null) {
                        val bitmap = rememberBackgroundBitmap(ownImagePath)
                        if (bitmap != null) Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                    }
                }
            },
            onClick = selectOwnImage,
            trailing = { RadioButton(selected = usesOwnImage, onClick = selectOwnImage) }
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
            onClick = openColorEditor,
            trailing = { RadioButton(selected = !usesGlobal && !usesOwnImage, onClick = openColorEditor) }
        )
    }

    if (usesGlobal) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_background_shade_label)) {
            ExpandableShadePercentSlider(
                percent = ((globalShade ?: global.shade) * 100).roundToInt(),
                onSettled = { onGlobalShadeChange(it / 100f) }
            )
        }
    }

    if (usesOwnImage) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_background_source_custom_image)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tapping the big preview picks a different image.
                WallpaperCard(
                    selected = true,
                    label = stringResource(R.string.settings_background_change_image),
                    onClick = launchImagePicker
                ) {
                    val bitmap = rememberBackgroundBitmap(ownImagePath)
                    if (bitmap != null) Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                }
            }
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_background_shade_label)) {
            ExpandableShadePercentSlider(
                percent = (current.shade * 100).roundToInt(),
                onSettled = { onCustomColor(current.copy(shade = it / 100f)) }
            )
        }
    }

    if (colorEditorOpen) {
        var draft by rememberDraft(current)
        SettingsEditPage(title = stringResource(R.string.settings_background_source_color), onDismiss = { colorEditorOpen = false }, onSave = { onCustomColor(draft) }, dirty = draft != current) {
            ColorFillPickerContent(
                fill = draft.fill,
                onFillChange = { fill -> draft = draft.copy(fill = fill) },
                modifier = Modifier.fillMaxWidth()
            )
            SettingsPreferenceGroup(title = stringResource(R.string.settings_background_shade_label)) {
                ExpandableShadePercentSlider(
                    percent = (draft.shade * 100).roundToInt(),
                    onSettled = { draft = draft.copy(shade = it / 100f) }
                )
            }
        }
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

private fun BackgroundConfig.withImage(path: String) =
    copy(source = BackgroundSource.IMAGE, imagePath = path)
