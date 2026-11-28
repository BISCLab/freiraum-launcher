package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.graphics.Color
import com.bisc.flauncher.ui.stackElement
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

/** What the stack changes on one app; stored together as its override. */
private data class AppDraft(
    val label: String,
    val hiddenFromAppList: Boolean,
    val hiddenFromSearch: Boolean,
    val hiddenFromAppGrid: Boolean,
    val revealMode: RevealMode,
    val revealCode: String,
    val shape: IconShape?,
    val size: Int?
)

private fun AppOverride.draft() = AppDraft(customLabel.orEmpty(), hiddenFromAppList, hiddenFromSearch, hiddenFromAppGrid, revealMode, revealCode.orEmpty(), iconShape, iconSizePercent)

private fun appKey(app: AppInfo) = "app:" + app.componentName.flattenToString()

/**
 * Every app in one card stack: ⓪ "All apps" (shape, size, pack for all), then the apps, the ones
 * with something of their own first, with a search on top. A tap on an app shows its options in
 * two tabs, the app and its icon. Several apps can be changed and saved at once.
 */
@Composable
fun SettingsAppsScreen(
    navController: NavController,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigate: (String) -> Unit = {},
    startElement: String? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val home: com.bisc.flauncher.ui.home.HomeViewModel = hiltViewModel()
    val stack: StackDraft = androidx.lifecycle.viewmodel.compose.viewModel()
    val apps by viewModel.labeledApps.collectAsState()
    val overrides by viewModel.appOverrides.collectAsState()
    var filter by rememberSaveable { mutableStateOf("") }
    val all = rememberAllAppsDraft(stack, viewModel)
    val customized = stringResource(R.string.settings_manage_apps_customized_label)
    val allLabel = stringResource(R.string.stack_all_apps)
    val ordered = remember(apps, overrides) {
        val (own, rest) = apps.partition { overrides[it.componentName.flattenToString()] != null }
        own.map { it to true } + rest.map { it to false }
    }

    val elements = buildList {
        add(StackElement(
            id = "all", number = 0,
            title = stringResource(R.string.stack_all_apps),
            summary = stringResource(R.string.stack_all_apps_summary),
            draftKeys = all.keys,
            sample = { AllAppsSample(all, viewModel) }
        ) { AllAppsOptions(all, viewModel) })
        ordered.forEachIndexed { i, (app, own) ->
            val key = app.componentName.flattenToString()
            val override = overrides[key]
            add(StackElement(
                id = appKey(app), number = i + 1,
                title = override?.customLabel?.takeIf { it.isNotBlank() } ?: app.label,
                summary = appSummary(override),
                section = if (filter.isBlank()) (if (own) customized else allLabel) else null,
                inList = filter.isBlank() || app.label.contains(filter, ignoreCase = true) || override?.customLabel?.contains(filter, ignoreCase = true) == true,
                draftKeys = listOf(appKey(app)),
                anchor = androidx.compose.ui.geometry.Offset(1f, 0f),
                leading = {
                    val bitmap = com.bisc.flauncher.ui.home.rememberAppIcon(app.componentName, viewModel::loadIcon)
                    if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.size(32.dp))
                }
            ) { AppEditor(app, override ?: AppOverride(key), stack, all, navController, home, viewModel) })
        }
    }

    val pages by home.pages.collectAsState()
    val homeId = pages.find { it.isHome }?.id ?: -1L
    val global by home.globalBackground.collectAsState()
    val background by remember(homeId) { home.pageBackgroundConfig(homeId) }.collectAsState(initial = global)
    val dark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(com.bisc.flauncher.data.model.ElementAppearance(fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE), stack.shownBackground(background))
    CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalAppIconLoader provides viewModel.iconLoader, com.bisc.flauncher.ui.home.LocalSurfaceIsDark provides dark) {
        SettingsCardStackScreen(
            title = stringResource(R.string.settings_section_manage_apps),
            navController = navController,
            onBack = onBack,
            draft = stack,
            elements = elements,
            startElement = startElement,
            firstCard = StackCard.ELEMENTS,
            listHeader = { StackSearchField(filter, { filter = it }, stringResource(R.string.settings_manage_apps_search_hint)) }
        ) { _, _ ->
            val selected = LocalStackSelected.current
            val ink = if (dark) Color.White else Color.Black
            val statusTop = replicaStatusHeight()
            Box(Modifier.fillMaxSize()) {
                com.bisc.flauncher.ui.home.dock.BackgroundFill(stack.shownBackground(background), Modifier.fillMaxSize())
                val app = apps.find { appKey(it) == selected }
                Column(
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (app == null) {
                        // All apps: a row of real ones in the drafted shape, size and pack.
                        Box(Modifier.fillMaxWidth().stackElement("all")) { AllAppsSample(all, viewModel) }
                    } else {
                        val key = app.componentName.flattenToString()
                        val d = DraftField(stack, appKey(app), (overrides[key] ?: AppOverride(key)).draft()).value
                        val iconScale by home.iconSizeScale.collectAsState()
                        Box(Modifier.stackElement(appKey(app))) {
                            AppShapeIcon(app, d.shape ?: all.shape.value, overrides[key]?.iconPackDrawable != null, all.pack.value, viewModel, Modifier.size(56.dp * iconScale * ((d.size ?: all.size.value) / 100f)), all.style.value)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(d.label.ifBlank { app.label }, color = ink, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                StatusBarSketch(statusTop, ink)
            }
        }
    }
}

/** What the list says under an app: hidden where, renamed. */
@Composable
private fun appSummary(override: AppOverride?): String? {
    val hidden = when {
        override?.hiddenFromAppList == true && override.hiddenFromSearch -> stringResource(R.string.settings_manage_apps_hidden_summary)
        override?.hiddenFromAppList == true -> stringResource(R.string.settings_manage_apps_hidden_from_list_summary)
        override?.hiddenFromSearch == true -> stringResource(R.string.settings_manage_apps_hidden_from_search_summary)
        else -> null
    }
    val renamed = override?.customLabel?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.settings_manage_apps_renamed_summary, it) }
    return listOfNotNull(hidden, renamed).joinToString(" · ").ifBlank { null }
}

/** This app's icon in [shape], drawn as the launcher draws it; its own pack icon stays as it is. */
@Composable
private fun AppShapeIcon(app: AppInfo, shape: IconShape, ownIcon: Boolean, pack: String?, viewModel: SettingsViewModel, modifier: Modifier, style: com.bisc.flauncher.data.settings.IconStyle? = null) {
    val own = com.bisc.flauncher.ui.home.rememberAppIcon(app.componentName, viewModel::loadIcon)
    val bitmap by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, shape, pack, ownIcon, style) {
        value = if (ownIcon) null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { if (style != null) viewModel.iconLoader.render(app.componentName, shape, pack, style) else viewModel.iconLoader.render(app.componentName, shape, pack) }.getOrNull()
        }
    }
    (if (ownIcon) own else bitmap ?: own)?.let { Image(it, contentDescription = null, modifier = modifier) }
}

/** One app's options in two tabs: the app (name, categories, where it shows, a search code,
 *  where it is placed) and its icon (picture, shape, size). */
@Composable
private fun ColumnScope.AppEditor(
    app: AppInfo,
    override: AppOverride,
    stack: StackDraft,
    all: AllAppsDraft,
    navController: NavController,
    home: com.bisc.flauncher.ui.home.HomeViewModel,
    viewModel: SettingsViewModel
) {
    val field = stack.field(appKey(app), override.draft()) { d ->
        viewModel.saveAppOverride(
            app.componentName,
            d.label,
            d.hiddenFromAppList,
            d.hiddenFromSearch,
            // A code that is switched on but empty would leave the app unfindable: then it stays findable by name.
            if (d.revealCode.isBlank()) RevealMode.NONE else d.revealMode,
            d.revealCode.trim(),
            d.shape,
            d.size,
            d.hiddenFromAppGrid
        )
    }
    val e = field.value
    var pickingIcon by remember { mutableStateOf(false) }
    val usages = rememberUsages(com.bisc.flauncher.data.model.GridItemType.APP, app.componentName.flattenToString(), home)
    val ownIcon = override.iconPackDrawable != null
    StackTabs(listOf(
        stringResource(R.string.stack_el_app) to {
            OutlinedTextField(
                value = e.label,
                onValueChange = { field.value = e.copy(label = it) },
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
                    PictureChoice(stringResource(R.string.settings_manage_apps_visibility_app_list_label), !e.hiddenFromAppList, { field.value = e.copy(hiddenFromAppList = !e.hiddenFromAppList) }, dimWhenOff = true) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                    },
                    PictureChoice(stringResource(R.string.settings_manage_apps_visibility_app_grid_label), !e.hiddenFromAppGrid, { field.value = e.copy(hiddenFromAppGrid = !e.hiddenFromAppGrid) }, dimWhenOff = true) {
                        Icon(Icons.Filled.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                    },
                    PictureChoice(stringResource(R.string.settings_manage_apps_visibility_search_label), !e.hiddenFromSearch, { field.value = e.copy(hiddenFromSearch = !e.hiddenFromSearch) }, dimWhenOff = true) {
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
                    onEnabledChange = { field.value = e.copy(revealMode = if (it) RevealMode.SEARCH_CODE else RevealMode.NONE) },
                    code = e.revealCode,
                    onCodeChange = { field.value = e.copy(revealCode = it) }
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
        stringResource(R.string.stack_el_app_symbol) to {
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
            PictureChoiceGrid(
                title = stringResource(R.string.settings_icon_shape_label),
                choices = listOf(
                    PictureChoice(stringResource(R.string.settings_app_shape_like_all), e.shape == null, { field.value = e.copy(shape = null) }) {
                        AppShapeIcon(app, all.shape.value, ownIcon, all.pack.value, viewModel, Modifier.fillMaxSize(0.62f))
                    },
                ) + iconShapeChoices().map { (shape, label) ->
                    PictureChoice(label, e.shape == shape, { field.value = e.copy(shape = shape) }) {
                        AppShapeIcon(app, shape, ownIcon, all.pack.value, viewModel, Modifier.fillMaxSize(0.62f), all.style.value)
                    }
                }
            )
            SettingsPreferenceGroup(title = stringResource(R.string.settings_icon_size_label)) {
                ExpandableOptionalIconSizeSlider(percent = e.size, globalDefault = all.size.value, onSettled = { field.value = e.copy(size = it) })
            }
            Text(
                stringResource(R.string.settings_icon_size_dock_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    ))

    if (pickingIcon) {
        SettingsFixedDialog(
            title = stringResource(R.string.settings_folder_icon_pack_choose_icon),
            onDismiss = { pickingIcon = false }
        ) {
            IconPackPickerContent(
                viewModel = viewModel,
                currentPack = override.iconPackPackage,
                currentDrawable = override.iconPackDrawable,
                onPick = { pack, drawable -> viewModel.setAppIcon(app.componentName, pack, drawable); pickingIcon = false },
                onReset = { viewModel.setAppIcon(app.componentName, null, null); pickingIcon = false }
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
    SettingsAppsScreen(navController = navController, onBack = onBack, startElement = "app:$component", viewModel = viewModel)
}
