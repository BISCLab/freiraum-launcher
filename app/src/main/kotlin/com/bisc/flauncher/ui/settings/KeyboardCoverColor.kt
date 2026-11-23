package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R

/** The keyboard cover's color: opens an editor where the color is tried on the real keyboard. A tap
 *  into the try-out line opens the keyboard, and the cover sits on it in the color being chosen. */
@Composable
internal fun KeyboardCoverColorRow(color: Int, cornerRadiusPercent: Int, arrow: Boolean, onColorChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    SettingsPreference(
        icon = null,
        title = stringResource(R.string.settings_search_keyboard_cap_color),
        info = stringResource(R.string.settings_search_keyboard_cap_color_info),
        leading = null,
        onClick = { open = true },
        trailing = {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(color)).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
    )
    if (open) KeyboardCoverColorEditor(color, cornerRadiusPercent, arrow, onSave = onColorChange, onDismiss = { open = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyboardCoverColorEditor(color: Int, cornerRadiusPercent: Int, arrow: Boolean, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableIntStateOf(color) }
    // A tapped system color restarts the picker from it; otherwise it keeps its own hue and brightness.
    var pickerGeneration by remember { mutableIntStateOf(0) }
    var tryText by remember { mutableStateOf("") }
    val systemColors = rememberSystemKeyboardColors()
    var screenshot by remember { mutableStateOf<android.net.Uri?>(null) }
    val pickScreenshot = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) screenshot = uri }
    SettingsEditPage(
        title = stringResource(R.string.settings_search_keyboard_cap_label),
        onDismiss = onDismiss,
        onSave = { onSave(draft) },
        dirty = draft != color,
        footer = {
            if (WindowInsets.isImeVisible) com.bisc.flauncher.ui.home.search.KeyboardCapStrip(draft, cornerRadiusPercent, arrow)
        }
    ) {
        OutlinedTextField(
            value = tryText,
            onValueChange = { tryText = it },
            placeholder = { Text(stringResource(R.string.settings_keyboard_cover_try)) },
            singleLine = true,
            shape = SettingsCardShape,
            modifier = Modifier.fillMaxWidth()
        )
        Text(stringResource(R.string.settings_keyboard_cover_pipette), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.settings_keyboard_cover_pipette_info), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.material3.FilledTonalButton(
            onClick = {
                pickScreenshot.launch(
                    androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        ) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Colorize, contentDescription = null, modifier = Modifier.size(18.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.settings_keyboard_cover_pipette_pick))
        }
        screenshot?.let { uri ->
            ScreenshotColorPicker(uri = uri, onPick = { draft = it; pickerGeneration++ }, onDismiss = { screenshot = null })
        }
        if (systemColors.isNotEmpty()) {
            Text(stringResource(R.string.settings_keyboard_cover_system_colors), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.settings_keyboard_cover_system_colors_info), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                systemColors.forEach { swatch ->
                    val picked = swatch == draft
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(swatch))
                            .border(if (picked) 3.dp else 1.dp, if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { draft = swatch; pickerGeneration++ }
                    )
                }
            }
        }
        Text(stringResource(R.string.settings_keyboard_cover_own_color), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        key(pickerGeneration) {
            ColorPickerControls(color = Color(draft), onColorChange = { draft = it.toArgb() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** The system palette's neutral tones that keyboards following the system colors use for their
 *  background, light ones first; empty before Android 12. */
@Composable
private fun rememberSystemKeyboardColors(): List<Int> {
    val context = LocalContext.current
    return remember {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) emptyList()
        else listOf(
            android.R.color.system_neutral1_10, android.R.color.system_neutral1_50, android.R.color.system_neutral1_100,
            android.R.color.system_neutral2_50, android.R.color.system_neutral2_100, android.R.color.system_neutral1_200,
            android.R.color.system_neutral1_700, android.R.color.system_neutral1_800, android.R.color.system_neutral2_800,
            android.R.color.system_neutral1_900, android.R.color.system_neutral2_900, android.R.color.system_neutral1_1000
        ).map { context.getColor(it) }.distinct()
    }
}
