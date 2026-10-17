package com.bisc.flauncher.ui.settings

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.theme.noRippleClickable
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.AppOverride
import com.bisc.flauncher.data.model.IconPackInfo
import com.bisc.flauncher.data.model.RevealMode
import com.bisc.flauncher.data.settings.IconShape

private val GROUP_CORNER_RADIUS = 12.dp

private fun groupItemShape(index: Int, count: Int): Shape {
    val full = CornerSize(GROUP_CORNER_RADIUS)
    val flat = CornerSize(0.dp)
    return when {
        count <= 1 -> RoundedCornerShape(full, full, full, full)
        index == 0 -> RoundedCornerShape(full, full, flat, flat)
        index == count - 1 -> RoundedCornerShape(flat, flat, full, full)
        else -> RoundedCornerShape(flat, flat, flat, flat)
    }
}

/** Every installed app in one place: icon appearance (shape + pack) up top, then the apps themselves. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppsScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val apps by viewModel.labeledApps.collectAsState()
    val overrides by viewModel.appOverrides.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    val iconSize by viewModel.iconSize.collectAsState()
    val activeIconPack by viewModel.activeIconPack.collectAsState()
    val iconPacks by viewModel.iconPacks.collectAsState()
    var filter by rememberSaveable { mutableStateOf("") }
    var editingApp by remember { mutableStateOf<AppInfo?>(null) }

    val searching = filter.isNotBlank()
    val filteredApps = remember(apps, filter) {
        if (filter.isBlank()) apps else apps.filter { it.label.contains(filter, ignoreCase = true) }
    }
    val customizedApps = remember(filteredApps, overrides, searching) {
        if (searching) emptyList() else filteredApps.filter { overrides[it.componentName.flattenToString()] != null }
    }
    val remainingApps = remember(filteredApps, customizedApps, searching) {
        if (searching) filteredApps else filteredApps - customizedApps.toSet()
    }

    CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalAppIconLoader provides viewModel.iconLoader) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = settingsTopBarInsets(),
                title = { Text(stringResource(R.string.settings_section_manage_apps)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = { SettingsBreadcrumbBar(navController) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item { SettingsGroupLabel(stringResource(R.string.settings_icon_shape_label)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconShapePreference(stringResource(R.string.settings_icon_shape_rounded_square), IconShape.ROUNDED_SQUARE, iconShape, viewModel::setIconShape)
                    IconShapePreference(stringResource(R.string.settings_icon_shape_circle), IconShape.CIRCLE, iconShape, viewModel::setIconShape)
                }
            }
            item { Spacer24() }

            item { SettingsGroupLabel(stringResource(R.string.settings_icon_size_label)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconSizePreference(stringResource(R.string.settings_icon_size_small), com.bisc.flauncher.data.settings.IconSize.SMALL, iconSize, viewModel::setIconSize)
                    IconSizePreference(stringResource(R.string.settings_icon_size_medium), com.bisc.flauncher.data.settings.IconSize.MEDIUM, iconSize, viewModel::setIconSize)
                    IconSizePreference(stringResource(R.string.settings_icon_size_large), com.bisc.flauncher.data.settings.IconSize.LARGE, iconSize, viewModel::setIconSize)
                }
            }
            item { Spacer24() }

            item { SettingsGroupLabel(stringResource(R.string.settings_section_icon_pack)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
            item { Spacer24() }

            item {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    placeholder = { Text(stringResource(R.string.settings_manage_apps_search_hint)) },
                    singleLine = true,
                    shape = SettingsTextFieldShape,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            if (customizedApps.isNotEmpty()) {
                item { SettingsGroupLabel(stringResource(R.string.settings_manage_apps_customized_label)) }
                itemsIndexed(customizedApps, key = { _, app -> app.componentName.flattenToString() }) { index, app ->
                    AppOverrideRow(
                        app = app,
                        override = overrides[app.componentName.flattenToString()],
                        loadIcon = viewModel::loadIcon,
                        shape = groupItemShape(index, customizedApps.size),
                        onClick = { editingApp = app }
                    )
                }
                item { Spacer24() }
            }

            item {
                SettingsGroupLabel(
                    if (searching) {
                        stringResource(R.string.settings_manage_apps_results_label, remainingApps.size)
                    } else {
                        stringResource(R.string.settings_manage_apps_all_label, remainingApps.size)
                    }
                )
            }
            if (remainingApps.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.settings_manage_apps_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                itemsIndexed(remainingApps, key = { _, app -> "all_" + app.componentName.flattenToString() }) { index, app ->
                    AppOverrideRow(
                        app = app,
                        override = overrides[app.componentName.flattenToString()],
                        loadIcon = viewModel::loadIcon,
                        shape = groupItemShape(index, remainingApps.size),
                        onClick = { editingApp = app }
                    )
                }
            }
        }
    }
    }

    editingApp?.let { app ->
        AppOverrideEditSheet(
            app = app,
            viewModel = viewModel,
            override = overrides[app.componentName.flattenToString()] ?: AppOverride(app.componentName.flattenToString()),
            onSave = { customLabel, hidden, revealMode, revealCode ->
                viewModel.saveAppOverride(app.componentName, customLabel, hidden, revealMode, revealCode)
                editingApp = null
            },
            onDismiss = { editingApp = null }
        )
    }
}

@Composable
private fun Spacer24() {
    Box(modifier = Modifier.height(22.dp))
}

@Composable
private fun AppOverrideRow(
    app: AppInfo,
    override: AppOverride?,
    loadIcon: (ComponentName) -> ImageBitmap?,
    shape: Shape,
    onClick: () -> Unit
) {
    val bitmap = com.bisc.flauncher.ui.home.rememberAppIcon(app.componentName, loadIcon)
    val summary = when {
        override?.hidden == true && !override.customLabel.isNullOrBlank() ->
            stringResource(R.string.settings_manage_apps_hidden_summary) + " · " + stringResource(R.string.settings_manage_apps_renamed_summary, override.customLabel)
        override?.hidden == true -> stringResource(R.string.settings_manage_apps_hidden_summary)
        !override?.customLabel.isNullOrBlank() -> stringResource(R.string.settings_manage_apps_renamed_summary, override?.customLabel.orEmpty())
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(32.dp))
        } else {
            Box(modifier = Modifier.size(32.dp))
        }
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.titleMedium)
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AppOverrideEditSheet(
    app: AppInfo,
    viewModel: SettingsViewModel,
    override: AppOverride,
    onSave: (customLabel: String?, hidden: Boolean, revealMode: RevealMode, revealCode: String?) -> Unit,
    onDismiss: () -> Unit
) {
    CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalAppIconLoader provides viewModel.iconLoader) {
        AppOverrideEditSheetContent(app, viewModel, override, onSave, onDismiss)
    }
}

@Composable
private fun AppOverrideEditSheetContent(
    app: AppInfo,
    viewModel: SettingsViewModel,
    override: AppOverride,
    onSave: (customLabel: String?, hidden: Boolean, revealMode: RevealMode, revealCode: String?) -> Unit,
    onDismiss: () -> Unit
) {
    // Keyed on the component, not the whole override.
    var customLabel by remember(override.componentName) { mutableStateOf(override.customLabel.orEmpty()) }
    var hidden by remember(override.componentName) { mutableStateOf(override.hidden) }
    var revealMode by remember(override.componentName) { mutableStateOf(override.revealMode) }
    var revealCode by remember(override.componentName) { mutableStateOf(override.revealCode.orEmpty()) }
    var pickingIcon by remember { mutableStateOf(false) }

    SettingsEditPage(title = stringResource(R.string.settings_manage_apps_edit_title), onDismiss = onDismiss) {
        OutlinedTextField(
            value = customLabel,
            onValueChange = { customLabel = it },
            label = { Text(stringResource(R.string.settings_manage_apps_name_label)) },
            placeholder = { Text(app.label) },
            singleLine = true,
            shape = SettingsTextFieldShape,
            modifier = Modifier.fillMaxWidth()
        )

        SettingsPreferenceGroup {
            val preview = com.bisc.flauncher.ui.home.rememberAppIcon(app.componentName, viewModel::loadIcon)
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_icon_label),
                summary = if (override.iconPackDrawable != null) override.iconPackDrawable.replace('_', ' ')
                else stringResource(R.string.settings_app_icon_default),
                onClick = { pickingIcon = true },
                trailing = { if (preview != null) Image(bitmap = preview, contentDescription = null, modifier = Modifier.size(36.dp)) }
            )
        }

        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_manage_apps_hidden_label),
                info = stringResource(R.string.settings_manage_apps_hidden_description),
                onClick = { hidden = !hidden },
                trailing = { Switch(checked = hidden, onCheckedChange = { hidden = it }) }
            )
        }

        if (hidden) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_manage_apps_reveal_mode_label)) {
                RadioPreference(
                    label = stringResource(R.string.settings_manage_apps_reveal_none),
                    value = RevealMode.NONE,
                    selected = revealMode,
                    onSelect = { revealMode = it }
                )
                RadioPreference(
                    label = stringResource(R.string.settings_manage_apps_reveal_search_code),
                    value = RevealMode.SEARCH_CODE,
                    selected = revealMode,
                    onSelect = { revealMode = it }
                )
            }
            if (revealMode == RevealMode.SEARCH_CODE) {
                OutlinedTextField(
                    value = revealCode,
                    onValueChange = { revealCode = it },
                    label = { Text(stringResource(R.string.settings_manage_apps_reveal_code_hint)) },
                    singleLine = true,
                    shape = SettingsTextFieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        TextButton(
            onClick = { onSave(customLabel, hidden, revealMode, revealCode) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_manage_apps_save))
        }
    }

    if (pickingIcon) {
        SettingsFixedDialog(
            title = stringResource(R.string.settings_folder_icon_pack_choose_icon),
            onDismiss = { pickingIcon = false }
        ) {
            IconPackPickerContent(
                viewModel = viewModel,
                currentPack = override.iconPackPackage,
                currentDrawable = override.iconPackDrawable,
                onPick = { pack, drawable ->
                    viewModel.setAppIcon(app.componentName, pack, drawable)
                    pickingIcon = false
                },
                onReset = {
                    viewModel.setAppIcon(app.componentName, null, null)
                    pickingIcon = false
                }
            )
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
            { Box(modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) }
        },
        trailing = { RadioButton(selected = selected, onClick = onSelect) }
    )
}
