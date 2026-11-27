package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material.icons.filled.Palette
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
    val iconPacks by viewModel.iconPacks.collectAsState()
    val activeIconPack by viewModel.activeIconPack.collectAsState()

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.bisc.flauncher.R.string.settings_back))
                    }
                }
            )
        },
        bottomBar = { SettingsBreadcrumbBar(navController) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = com.bisc.flauncher.ui.MaxContentWidth).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Icon pack, shape and size for all apps live under Appearance, but many look for them
            // here first, so a way there sits on top.
            item {
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.settings_icons_label),
                        summary = stringResource(
                            R.string.settings_apps_icons_link_summary,
                            iconPacks.firstOrNull { it.packageName == activeIconPack }?.label ?: stringResource(R.string.settings_icon_pack_default)
                        ),
                        onClick = { navController.navigate("settings/appearance/icons") }
                    )
                }
            }
            item { Spacer24() }
            if (customizedApps.isNotEmpty()) {
                item { SettingsGroupLabel(stringResource(R.string.settings_manage_apps_customized_label)) }
                itemsIndexed(customizedApps, key = { _, app -> app.componentName.flattenToString() }) { index, app ->
                    AppOverrideRow(
                        app = app,
                        override = overrides[app.componentName.flattenToString()],
                        loadIcon = viewModel::loadIcon,
                        shape = groupItemShape(index, customizedApps.size),
                        onClick = { navController.navigate("settings/apps/edit/" + android.net.Uri.encode(app.componentName.flattenToString())) }
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
                        onClick = { navController.navigate("settings/apps/edit/" + android.net.Uri.encode(app.componentName.flattenToString())) }
                    )
                }
            }
        }
    }
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

/** What the app stack changes besides the symbol: name, where it shows and the search code. */
private data class AppEdit(
    val label: String,
    val hiddenFromAppList: Boolean,
    val hiddenFromSearch: Boolean,
    val hiddenFromAppGrid: Boolean,
    val revealMode: RevealMode,
    val revealCode: String
)

/**
 * One app as a card stack: ⓪ the app (name, categories, where it shows, a search code, where it is
 * placed) and ① its icon (picture, shape, size). The replica shows the icon at its real size.
 * Everything but a picture from an icon pack is a draft until Save.
 */
@Composable
private fun AppStack(app: AppInfo, navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel) {
    val home: com.bisc.flauncher.ui.home.HomeViewModel = hiltViewModel()
    val stack: StackDraft = androidx.lifecycle.viewmodel.compose.viewModel()
    val key = app.componentName.flattenToString()
    val overrides by viewModel.appOverrides.collectAsState()
    val override = overrides[key] ?: AppOverride(key)
    val globalIconSizePercent by viewModel.iconSizePercent.collectAsState()
    val edit = stack.field(
        "app",
        AppEdit(override.customLabel.orEmpty(), override.hiddenFromAppList, override.hiddenFromSearch, override.hiddenFromAppGrid, override.revealMode, override.revealCode.orEmpty())
    ) {}
    val shape = stack.field("shape", override.iconShape) {}
    val size = stack.field("size", override.iconSizePercent) {}
    val e = edit.value
    stack.saveTogether {
        viewModel.saveAppOverride(
            app.componentName,
            e.label,
            e.hiddenFromAppList,
            e.hiddenFromSearch,
            // A code that is switched on but empty would leave the app unfindable: then it stays findable by name.
            if (e.revealCode.isBlank()) RevealMode.NONE else e.revealMode,
            e.revealCode.trim(),
            shape.value,
            size.value,
            e.hiddenFromAppGrid
        )
    }
    var pickingIcon by remember { mutableStateOf(false) }
    val usages = rememberUsages(com.bisc.flauncher.data.model.GridItemType.APP, key, home)
    val globalShape by viewModel.iconShape.collectAsState()
    val activePack by viewModel.activeIconPack.collectAsState()
    val ownIcon = override.iconPackDrawable != null
    val ownPreview = com.bisc.flauncher.ui.home.rememberAppIcon(app.componentName, viewModel::loadIcon)

    /** This app's icon in [iconShape], drawn as the launcher draws it. */
    @Composable
    fun ShapePicture(iconShape: IconShape, modifier: Modifier) {
        val bitmap by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, iconShape, activePack, ownIcon) {
            value = if (ownIcon) null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { viewModel.iconLoader.render(app.componentName, iconShape, activePack) }.getOrNull()
            }
        }
        (if (ownIcon) ownPreview else bitmap)?.let { Image(it, contentDescription = null, modifier = modifier) }
    }

    ElementStackScreen(
        title = e.label.ifBlank { app.label },
        navController = navController,
        onBack = onBack,
        startElement = null,
        draft = stack,
        home = home,
        partTitle = stringResource(R.string.stack_el_app),
        partSummary = app.label,
        partKeys = listOf("app"),
        symbolTitle = stringResource(R.string.stack_el_app_symbol),
        symbolKeys = listOf("shape", "size"),
        name = e.label.ifBlank { app.label },
        symbol = { s, _ ->
            val scale = (size.value ?: globalIconSizePercent) / 100f
            Box(Modifier.size(s * scale)) { ShapePicture(shape.value ?: globalShape, Modifier.fillMaxSize()) }
        },
        partOptions = {
            OutlinedTextField(
                value = e.label,
                onValueChange = { edit.value = e.copy(label = it) },
                label = { Text(stringResource(R.string.settings_manage_apps_name_label)) },
                placeholder = { Text(app.label) },
                singleLine = true,
                shape = SettingsTextFieldShape,
                modifier = Modifier.fillMaxWidth()
            )
            AppCategoryChips(app, viewModel)
            // Where the app shows, each place on its own. Places on pages, in the Dock and in folders stay either way.
            PictureChoiceGrid(
                title = stringResource(R.string.settings_app_visible_in),
                info = stringResource(R.string.settings_manage_apps_visibility_app_list_description),
                toggles = true,
                choices = listOf(
                    PictureChoice(stringResource(R.string.settings_manage_apps_visibility_app_list_label), !e.hiddenFromAppList, { edit.value = e.copy(hiddenFromAppList = !e.hiddenFromAppList) }, dimWhenOff = true) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                    },
                    PictureChoice(stringResource(R.string.settings_manage_apps_visibility_app_grid_label), !e.hiddenFromAppGrid, { edit.value = e.copy(hiddenFromAppGrid = !e.hiddenFromAppGrid) }, dimWhenOff = true) {
                        Icon(Icons.Filled.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                    },
                    PictureChoice(stringResource(R.string.settings_manage_apps_visibility_search_label), !e.hiddenFromSearch, { edit.value = e.copy(hiddenFromSearch = !e.hiddenFromSearch) }, dimWhenOff = true) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                    }
                )
            )
            // Only while the text search shows it: found only by a code instead of its name.
            if (!e.hiddenFromSearch) {
                Text(
                    stringResource(R.string.settings_manage_apps_reveal_search_code_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                RevealSearchCodeCard(
                    enabled = e.revealMode == RevealMode.SEARCH_CODE,
                    onEnabledChange = { edit.value = e.copy(revealMode = if (it) RevealMode.SEARCH_CODE else RevealMode.NONE) },
                    code = e.revealCode,
                    onCodeChange = { edit.value = e.copy(revealCode = it) }
                )
            } else {
                Text(
                    stringResource(R.string.settings_app_hidden_search_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            UsagesGroup(usages, navController)
        },
        symbolOptions = {
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_app_icon_label),
                    summary = if (override.iconPackDrawable != null) override.iconPackDrawable.replace('_', ' ')
                    else stringResource(R.string.settings_app_icon_default),
                    onClick = { pickingIcon = true },
                    trailing = { if (ownPreview != null) Image(bitmap = ownPreview, contentDescription = null, modifier = Modifier.size(36.dp)) }
                )
            }
            PictureChoiceGrid(
                title = stringResource(R.string.settings_icon_shape_label),
                choices = listOf(
                    PictureChoice(stringResource(R.string.settings_app_shape_like_all), shape.value == null, { shape.value = null }) { ShapePicture(globalShape, Modifier.fillMaxSize(0.62f)) },
                    PictureChoice(stringResource(R.string.settings_icon_shape_rounded_square), shape.value == IconShape.ROUNDED_SQUARE, { shape.value = IconShape.ROUNDED_SQUARE }) { ShapePicture(IconShape.ROUNDED_SQUARE, Modifier.fillMaxSize(0.62f)) },
                    PictureChoice(stringResource(R.string.settings_icon_shape_circle), shape.value == IconShape.CIRCLE, { shape.value = IconShape.CIRCLE }) { ShapePicture(IconShape.CIRCLE, Modifier.fillMaxSize(0.62f)) }
                )
            )
            SettingsPreferenceGroup(title = stringResource(R.string.settings_icon_size_label)) {
                ExpandableOptionalIconSizeSlider(
                    percent = size.value,
                    globalDefault = globalIconSizePercent,
                    onSettled = { size.value = it }
                )
            }
            Text(
                stringResource(R.string.settings_icon_size_dock_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    )

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
                supportingText = { Text(stringResource(R.string.settings_manage_apps_reveal_code_note)) },
                singleLine = true,
                shape = SettingsTextFieldShape,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)
            )
        }
    }
}


/** One app's page on its own, opened from the app's long-press menu; leaving it goes back there. */
@Composable
fun SettingsAppEditRoute(component: String, navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val apps by viewModel.labeledApps.collectAsState()
    val app = apps.firstOrNull { it.componentName.flattenToString() == component } ?: return
    CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalAppIconLoader provides viewModel.iconLoader) {
        AppStack(app, navController, onBack, viewModel)
    }
}
