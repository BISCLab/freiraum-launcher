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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.theme.noRippleClickable
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.AppOverride
import com.bisc.flauncher.data.model.RevealMode
import com.bisc.flauncher.data.settings.IconShape

private fun groupItemShape(index: Int, count: Int): Shape {
    val full = SettingsCardShape.topStart
    val flat = CornerSize(0.dp)
    return when {
        count <= 1 -> RoundedCornerShape(full, full, full, full)
        index == 0 -> RoundedCornerShape(full, full, flat, flat)
        index == count - 1 -> RoundedCornerShape(flat, flat, full, full)
        else -> RoundedCornerShape(flat, flat, flat, flat)
    }
}

/** Every installed app in one place: a link to the shared icon presets up top, then the apps themselves. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppsScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val apps by viewModel.labeledApps.collectAsState()
    val overrides by viewModel.appOverrides.collectAsState()
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
            // The icon presets every app shares (shape, size, pack) live under Design → Icons.
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
            onSave = { draft ->
                viewModel.saveAppOverride(app.componentName, draft.customLabel, draft.hiddenFromAppList, draft.hiddenFromSearch, draft.revealMode, draft.revealCode, draft.iconShape, draft.iconSizePercent)
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
    val hiddenSummary = when {
        override?.hiddenFromAppList == true && override.hiddenFromSearch == true -> stringResource(R.string.settings_manage_apps_hidden_summary)
        override?.hiddenFromAppList == true -> stringResource(R.string.settings_manage_apps_hidden_from_list_summary)
        override?.hiddenFromSearch == true -> stringResource(R.string.settings_manage_apps_hidden_from_search_summary)
        else -> null
    }
    val summary = when {
        hiddenSummary != null && !override?.customLabel.isNullOrBlank() ->
            hiddenSummary + " · " + stringResource(R.string.settings_manage_apps_renamed_summary, override?.customLabel.orEmpty())
        hiddenSummary != null -> hiddenSummary
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
    onSave: (AppOverride) -> Unit,
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
    onSave: (AppOverride) -> Unit,
    onDismiss: () -> Unit
) {
    val globalIconSizePercent by viewModel.iconSizePercent.collectAsState()
    // Keyed on the component, not the whole override.
    var customLabel by remember(override.componentName) { mutableStateOf(override.customLabel.orEmpty()) }
    var hiddenFromAppList by remember(override.componentName) { mutableStateOf(override.hiddenFromAppList) }
    var hiddenFromSearch by remember(override.componentName) { mutableStateOf(override.hiddenFromSearch) }
    var revealMode by remember(override.componentName) { mutableStateOf(override.revealMode) }
    var revealCode by remember(override.componentName) { mutableStateOf(override.revealCode.orEmpty()) }
    var iconShape by remember(override.componentName) { mutableStateOf(override.iconShape) }
    var iconSizePercent by remember(override.componentName) { mutableStateOf(override.iconSizePercent) }
    var pickingIcon by remember { mutableStateOf(false) }
    var revealSettingsOpen by remember { mutableStateOf(false) }

    val draft = override.copy(
        customLabel = customLabel,
        hiddenFromAppList = hiddenFromAppList,
        hiddenFromSearch = hiddenFromSearch,
        revealMode = revealMode,
        revealCode = revealCode,
        iconShape = iconShape,
        iconSizePercent = iconSizePercent
    )
    val dirty = customLabel != override.customLabel.orEmpty() ||
        hiddenFromAppList != override.hiddenFromAppList ||
        hiddenFromSearch != override.hiddenFromSearch ||
        revealMode != override.revealMode ||
        revealCode != override.revealCode.orEmpty() ||
        iconShape != override.iconShape ||
        iconSizePercent != override.iconSizePercent

    SettingsEditPage(title = stringResource(R.string.settings_manage_apps_edit_title), onDismiss = onDismiss, onSave = { onSave(draft) }, dirty = dirty) {
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

        SettingsPreferenceGroup(title = stringResource(R.string.settings_icon_shape_label)) {
            RadioPreference(
                label = stringResource(R.string.settings_element_color_auto_hint),
                value = null,
                selected = iconShape,
                onSelect = { iconShape = it }
            )
            IconShapePreference(
                stringResource(R.string.settings_icon_shape_rounded_square),
                IconShape.ROUNDED_SQUARE,
                iconShape,
                { iconShape = it }
            )
            IconShapePreference(
                stringResource(R.string.settings_icon_shape_circle),
                IconShape.CIRCLE,
                iconShape,
                { iconShape = it }
            )
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_icon_size_label)) {
            ExpandableOptionalIconSizeSlider(
                percent = iconSizePercent,
                globalDefault = globalIconSizePercent,
                onSettled = { iconSizePercent = it }
            )
        }

        // Independent per-surface visibility - an app can stay out of the app list while still
        // being searchable, or vice versa; placements on pages, in the dock and in folders are
        // unaffected either way. Once other page types exist, a "Pages" row belongs in this same
        // group alongside App list/Text search. Reveal-with-search-code only makes sense once Text
        // search is ON - if the app is already out of Text search entirely, there's no code that
        // could bypass that - so its own settings button only shows then; turning it on *replaces*
        // name matching rather than adding to it, the app then only turns up for the exact code.
        SettingsPreferenceGroup(title = stringResource(R.string.settings_manage_apps_visibility_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_manage_apps_visibility_app_list_label),
                info = stringResource(R.string.settings_manage_apps_visibility_app_list_description),
                onClick = { hiddenFromAppList = !hiddenFromAppList },
                trailing = { Switch(checked = !hiddenFromAppList, onCheckedChange = { hiddenFromAppList = !it }) }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_manage_apps_visibility_search_label),
                info = stringResource(R.string.settings_manage_apps_visibility_search_description),
                onClick = { hiddenFromSearch = !hiddenFromSearch },
                trailing = { Switch(checked = !hiddenFromSearch, onCheckedChange = { hiddenFromSearch = !it }) },
                extra = if (!hiddenFromSearch) {
                    {
                        IconButton(onClick = { revealSettingsOpen = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = stringResource(R.string.settings_more_settings_button),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else null
            )
        }

        if (revealSettingsOpen) {
            SettingsEditPage(title = stringResource(R.string.settings_manage_apps_reveal_search_code), onDismiss = { revealSettingsOpen = false }) {
                Text(
                    stringResource(R.string.settings_manage_apps_reveal_search_code_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RevealSearchCodeCard(
                    enabled = revealMode == RevealMode.SEARCH_CODE,
                    onEnabledChange = { revealMode = if (it) RevealMode.SEARCH_CODE else RevealMode.NONE },
                    code = revealCode,
                    onCodeChange = { revealCode = it }
                )
            }
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

/** The on/off toggle and its search-code input as one unbroken card, instead of a separate
 *  indented row plus a detached text field below it. */
@Composable
private fun RevealSearchCodeCard(enabled: Boolean, onEnabledChange: (Boolean) -> Unit, code: String, onCodeChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SettingsCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .noRippleClickable(onClick = { onEnabledChange(!enabled) })
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.settings_manage_apps_reveal_search_code),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        if (enabled) {
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                label = { Text(stringResource(R.string.settings_manage_apps_reveal_code_hint)) },
                singleLine = true,
                shape = SettingsTextFieldShape,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)
            )
        }
    }
}

