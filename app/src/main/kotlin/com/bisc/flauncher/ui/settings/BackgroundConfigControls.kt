package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
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

/** One way to fill a screen, as a small phone with its real look. */
private class BackgroundChoice(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val preview: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
)

/**
 * The choices side by side as equal small phones, three to a line (an empty place keeps the size
 * when there are two): each shows what it would look like, the chosen one with an accent edge and a
 * check, its name in the accent.
 */
@Composable
private fun BackgroundChoiceRow(title: String?, choices: List<BackgroundChoice>) {
    Column {
        if (title != null) SettingsGroupLabel(title)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            choices.forEach { choice ->
                val shape = RoundedCornerShape(16.dp)
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable(onClick = choice.onClick),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.56f)) {
                        Box(
                            modifier = Modifier.fillMaxSize().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            content = { choice.preview(this) }
                        )
                        // The frame on top and outside the clip, so its corners stay whole.
                        Box(
                            Modifier.matchParentSize().border(
                                if (choice.selected) 3.dp else 1.dp,
                                if (choice.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape
                            )
                        )
                        if (choice.selected) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) { SelectedMark(true) }
                        }
                    }
                    Text(
                        choice.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (choice.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
            }
            repeat(3 - choices.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ImagePreview(path: String?) {
    if (path != null) {
        val bitmap = rememberBackgroundBitmap(path)
        if (bitmap != null) Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Editor for the single background shared by every surface: an image or a color, then what fits
 *  the choice - the image to change, or the color - and how much it is darkened or lightened. */
@Composable
fun BackgroundConfigSection(
    imageKey: String,
    config: BackgroundConfig,
    onConfigChange: (BackgroundConfig) -> Unit
) {
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
    val launchImagePicker = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val imagePath = config.imagePath
    val usesImage = config.source == BackgroundSource.IMAGE && imagePath != null

    BackgroundChoiceRow(
        title = null,
        choices = listOf(
            BackgroundChoice(stringResource(R.string.settings_background_source_custom_image), usesImage, {
                if (imagePath != null) onConfigChange(config.withImage(imagePath)) else launchImagePicker()
            }) { ImagePreview(imagePath) },
            BackgroundChoice(stringResource(R.string.settings_background_own_color), !usesImage, {
                if (config.source != BackgroundSource.COLOR) onConfigChange(config.copy(source = BackgroundSource.COLOR))
            }) { Box(Modifier.fillMaxSize().background(config.fill.asBrush())) }
        )
    )

    SettingsPreferenceGroup {
        if (usesImage) {
            SettingsPreference(icon = null, title = stringResource(R.string.settings_background_change_image), onClick = launchImagePicker)
        } else {
            ColorFillPickerContent(
                fill = config.fill,
                onFillChange = { fill -> onConfigChange(config.copy(fill = fill)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
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
 * Editor for one screen's background, three ways side by side: like all pages (the shared one),
 * an own image, an own color. Below, only what fits the choice: for the shared one how much it is
 * darkened here and the way to change it for all pages; for an own image the image and its shade;
 * for an own color the color, in its own window like every color.
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
    groupTitle: String? = surfaceLabel(R.string.settings_layer_surface_screen),
    /** Opens the background for all pages; null where there is no way there. */
    onEditGlobal: (() -> Unit)? = null
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
    // Picking it again keeps an image picked before; only the first time opens the system picker.
    val selectOwnImage = {
        if (ownImagePath != null) onCustomColor(current.withImage(ownImagePath)) else launchImagePicker()
    }

    BackgroundChoiceRow(
        title = groupTitle,
        choices = listOf(
            BackgroundChoice(stringResource(R.string.settings_background_like_all), usesGlobal, onUseGlobal) {
                com.bisc.flauncher.ui.home.dock.BackgroundFill(global, Modifier.fillMaxSize())
            },
            BackgroundChoice(stringResource(R.string.settings_background_source_custom_image), usesOwnImage, selectOwnImage) { ImagePreview(ownImagePath) },
            BackgroundChoice(stringResource(R.string.settings_background_own_color), !usesGlobal && !usesOwnImage, {
                if (usesGlobal || usesOwnImage) onCustomColor(current.copy(source = BackgroundSource.COLOR)) else colorEditorOpen = true
            }) { Box(Modifier.fillMaxSize().background(current.fill.asBrush())) }
        )
    )

    SettingsPreferenceGroup {
        when {
            usesGlobal -> {
                ExpandableShadePercentSlider(
                    percent = ((globalShade ?: global.shade) * 100).roundToInt(),
                    onSettled = { onGlobalShadeChange(it / 100f) }
                )
                if (onEditGlobal != null) {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_background_for_all_edit),
                        summary = stringResource(R.string.settings_background_for_all_summary),
                        onClick = onEditGlobal
                    )
                }
            }
            usesOwnImage -> {
                SettingsPreference(icon = null, title = stringResource(R.string.settings_background_change_image), onClick = launchImagePicker)
                ExpandableShadePercentSlider(
                    percent = (current.shade * 100).roundToInt(),
                    onSettled = { onCustomColor(current.copy(shade = it / 100f)) }
                )
            }
            else -> SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_background_change_color),
                leading = {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(current.fill.asBrush()).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                },
                onClick = openColorEditor
            )
        }
    }

    if (colorEditorOpen) {
        var draft by rememberDraft(current)
        SettingsEditPage(title = stringResource(R.string.settings_background_own_color), onDismiss = { colorEditorOpen = false }, onSave = { onCustomColor(draft) }, dirty = draft != current) {
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
