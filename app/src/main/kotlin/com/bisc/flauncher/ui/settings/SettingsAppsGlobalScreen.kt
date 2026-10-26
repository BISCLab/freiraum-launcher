package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.IconPackInfo
import com.bisc.flauncher.data.settings.IconShape

/** The app-icon presets shared by every app (shape, size, pack) — each one can still be overridden
 *  individually on a per-app basis in that app's own edit screen (see AppOverrideEditSheetContent). */
@Composable
fun SettingsAppsGlobalScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val iconShape by viewModel.iconShape.collectAsState()
    val iconSizePercent by viewModel.iconSizePercent.collectAsState()
    val activeIconPack by viewModel.activeIconPack.collectAsState()
    val iconPacks by viewModel.iconPacks.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_icons_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_icon_shape_label)) {
            IconShapePreference(stringResource(R.string.settings_icon_shape_rounded_square), IconShape.ROUNDED_SQUARE, iconShape, viewModel::setIconShape)
            IconShapePreference(stringResource(R.string.settings_icon_shape_circle), IconShape.CIRCLE, iconShape, viewModel::setIconShape)
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_icon_size_label)) {
            ExpandableIconSizeSlider(percent = iconSizePercent, onSettled = viewModel::setIconSizePercent)
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_section_icon_pack)) {
            IconPackPreference(
                label = stringResource(R.string.settings_icon_pack_default),
                icon = null,
                selected = activeIconPack == null,
                onSelect = { viewModel.selectIconPack(null) }
            )
            iconPacks.forEach { pack ->
                IconPackPreference(
                    label = pack.label,
                    icon = pack,
                    selected = activeIconPack == pack.packageName,
                    onSelect = { viewModel.selectIconPack(pack.packageName) }
                )
            }
        }
    }
}

@Composable
private fun IconPackPreference(
    label: String,
    icon: IconPackInfo?,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(icon?.packageName) {
        icon?.let {
            runCatching {
                context.packageManager.getApplicationIcon(it.packageName)
                    .toBitmap(width = 96, height = 96)
                    .asImageBitmap()
            }.getOrNull()
        }
    }
    SettingsPreference(
        icon = null,
        title = label,
        onClick = onSelect,
        leading = if (bitmap != null) {
            { Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(24.dp)) }
        } else {
            { androidx.compose.foundation.layout.Box(modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) }
        },
        trailing = { RadioButton(selected = selected, onClick = onSelect) }
    )
}
