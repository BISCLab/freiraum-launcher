package com.bisc.flauncher.ui.settings

import com.bisc.flauncher.ui.marqueeWhenCut
import androidx.core.net.toUri
import androidx.core.content.edit
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.ui.home.search.defaultColor
import com.bisc.flauncher.ui.home.search.icon
import com.bisc.flauncher.ui.home.search.shownIcon
import com.bisc.flauncher.ui.home.search.settingsLabelRes

/** Behavior: where web queries go, which AI app gets them, and which categories are searched in
 *  which order (with per-category color). */
@Composable
fun SettingsSearchBehaviorScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val categoryOrder by viewModel.searchCategoryOrder.collectAsState()
    val modes by viewModel.searchCategoryModes.collectAsState()
    val railShown by viewModel.searchBarShowCategories.collectAsState()
    val categoryColors by viewModel.categoryColors.collectAsState()
    var pickingColorFor by remember { mutableStateOf<SearchCategory?>(null) }
    SettingsSubScreen(title = stringResource(R.string.settings_search_found_title), navController = navController, onBack = onBack) {
        // The first time: what the table below does, until it is understood.
        val hints = remember { context.getSharedPreferences("settings_hints", android.content.Context.MODE_PRIVATE) }
        var tableUnderstood by remember { mutableStateOf(hints.getBoolean("search_table", false)) }
        if (!tableUnderstood) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SettingsCardShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.settings_search_first_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(stringResource(R.string.settings_search_first_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                androidx.compose.material3.FilledTonalButton(onClick = { tableUnderstood = true; hints.edit { putBoolean("search_table", true) } }, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.settings_search_first_ok))
                }
            }
        }
        Column {
            SettingsGroupLabel(stringResource(R.string.settings_search_categories_label), info = stringResource(R.string.settings_search_categories_info))
            // What the two columns do, on its own above the table.
            CategoryLegend(railShown)
            androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
            Column(modifier = Modifier.fillMaxWidth().clip(SettingsCardShape)) {
                CategoryTableHeader(railShown)
                ReorderableList(
                    items = categoryOrder,
                    key = { it },
                    onReorder = viewModel::setSearchCategoryOrder,
                    itemHeight = 76.dp,
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    dividers = true
                ) { category ->
                    CategoryModeRow(
                        category = category,
                        color = categoryColors[category]?.let { Color(it) } ?: category.defaultColor,
                        mode = modes[category] ?: com.bisc.flauncher.data.model.CategoryMode(search = true, rail = true),
                        railShown = railShown,
                        onDesign = { pickingColorFor = category },
                        onChange = { viewModel.setSearchCategoryModes(mapOf(category to it)) },
                        // Categories with settings of their own open them with a tap on their name.
                        onOpen = when (category) {
                            SearchCategory.FAVORITES -> ({ navController.navigate("settings/search/favorites") })
                            SearchCategory.WEB -> ({ navController.navigate("settings/search/web") })
                            SearchCategory.AI -> ({ navController.navigate("settings/search/ai") })
                            else -> null
                        }
                    )
                }
            }
            Text(
                stringResource(R.string.settings_search_table_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp)
            )
        }
    }

    pickingColorFor?.let { category ->
        val categoryIcons by viewModel.categoryIcons.collectAsState()
        val savedColor = categoryColors[category]?.let { Color(it) } ?: category.defaultColor
        val savedIcon = categoryIcons[category]
        var color by rememberDraft(savedColor)
        var iconKey by rememberDraft(savedIcon)
        SettingsEditPage(
            title = stringResource(category.settingsLabelRes),
            onDismiss = { pickingColorFor = null },
            onSave = {
                if (color != savedColor) viewModel.setCategoryColor(category, color.toArgb())
                if (iconKey != savedIcon) viewModel.setCategoryIcon(category, iconKey)
            },
            dirty = color != savedColor || iconKey != savedIcon
        ) {
            CategoryDesigner(category, color, iconKey, onIcon = { iconKey = it }, onColor = { color = it })
        }
    }
}

private val MODE_COLUMN = 60.dp
/** The drag handle's column at the end of each row (see ReorderableList). */
private val HANDLE_COLUMN = 32.dp

/** What the two columns do: two small cards side by side, apart from the table. */
@Composable
private fun CategoryLegend(railShown: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            Triple(Icons.Filled.Search, R.string.settings_search_col_search to R.string.settings_search_legend_search_body, true),
            Triple(Icons.AutoMirrored.Filled.ViewSidebar, R.string.settings_search_col_rail to R.string.settings_search_legend_rail_body, railShown)
        ).forEach { (icon, texts, active) ->
            val shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .alpha(if (active) 1f else 0.45f)
                    .clip(shape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp)) }
                    Text(
                        stringResource(texts.first),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(stringResource(texts.second), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The table's head: column names on a tinted band, the same column lines as the rows below. */
@Composable
private fun CategoryTableHeader(railShown: Boolean) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .drawBehind {
                val col = MODE_COLUMN.toPx()
                val end = size.width - 16.dp.toPx() - HANDLE_COLUMN.toPx()
                listOf(end - col * 2, end - col, end).forEach { x -> drawLine(line, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1.dp.toPx()) }
                drawLine(line, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), 1.dp.toPx())
            }
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.settings_search_col_category),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        listOf(R.string.settings_search_col_search to true, R.string.settings_search_col_rail to railShown).forEach { (label, active) ->
            Text(
                stringResource(label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 1f else 0.4f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.width(MODE_COLUMN)
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.width(HANDLE_COLUMN))
    }
}

/** One category: symbol (tap to design it), name with what it does right now, and the two toggles. */
@Composable
private fun CategoryModeRow(
    category: SearchCategory,
    color: Color,
    mode: com.bisc.flauncher.data.model.CategoryMode,
    railShown: Boolean,
    onDesign: () -> Unit,
    onChange: (com.bisc.flauncher.data.model.CategoryMode) -> Unit,
    /** Opens the category's own settings; null = it has none. */
    onOpen: (() -> Unit)? = null
) {
    val inRail = mode.rail && railShown
    val (status, statusColor) = when {
        mode.search && inRail -> R.string.settings_search_mode_always to color
        mode.search -> R.string.settings_search_mode_results to MaterialTheme.colorScheme.onSurfaceVariant
        inRail -> R.string.settings_search_mode_demand to MaterialTheme.colorScheme.onSurfaceVariant
        else -> R.string.settings_search_mode_off to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val off = !mode.search && !inRail
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .drawBehind {
                val col = MODE_COLUMN.toPx()
                listOf(size.width - col * 2, size.width - col, size.width).forEach { x ->
                    drawLine(line, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1.dp.toPx())
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            category.shownIcon(),
            contentDescription = null,
            tint = color.copy(alpha = if (off) 0.4f else 1f),
            modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDesign).padding(4.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
                .padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // One line; a longer name runs through now and then, so it can be read in full.
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .weight(1f, fill = false)
                        .clip(androidx.compose.ui.graphics.RectangleShape)
                        .marqueeWhenCut()
                ) {
                    Text(
                        stringResource(category.settingsLabelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (off) 0.5f else 1f),
                        maxLines = 1,
                        softWrap = false
                    )
                }
                if (onOpen != null) Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(stringResource(status), style = MaterialTheme.typography.labelSmall, color = statusColor, maxLines = 2)
        }
        val name = stringResource(category.settingsLabelRes)
        ModeToggle(Icons.Filled.Search, on = mode.search, enabled = true, color = color, label = stringResource(R.string.a11y_category_in_results, name)) { onChange(mode.copy(search = it)) }
        ModeToggle(Icons.AutoMirrored.Filled.ViewSidebar, on = mode.rail, enabled = railShown, color = color, label = stringResource(R.string.a11y_category_in_rail, name)) { onChange(mode.copy(rail = it)) }
    }
}

@Composable
private fun ModeToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, on: Boolean, enabled: Boolean, color: Color, label: String, onToggle: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = Modifier.width(MODE_COLUMN), contentAlignment = Alignment.Center) {
        val shown = on && enabled
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (shown) color.copy(alpha = 0.2f) else Color.Transparent)
                .then(if (shown) Modifier else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                // A switch for screen readers, named by its category and column.
                .toggleable(value = on, enabled = enabled, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onToggle)
                .semantics { contentDescription = label }
                .alpha(if (enabled) 1f else 0.4f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (shown) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** A category's symbol and color: a large preview, the symbols to pick (its own first), the color. */
@Composable
private fun CategoryDesigner(category: SearchCategory, color: Color, iconKey: String?, onIcon: (String?) -> Unit, onColor: (Color) -> Unit) {
    val shown = iconKey?.let { key -> com.bisc.flauncher.ui.home.search.CategoryIconChoices.firstOrNull { it.first == key }?.second } ?: category.icon
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) { Icon(shown, contentDescription = null, tint = color, modifier = Modifier.size(36.dp)) }
    }
    SettingsPreferenceGroup(title = stringResource(R.string.settings_category_symbol)) {
        val choices: List<Pair<String?, androidx.compose.ui.graphics.vector.ImageVector>> =
            // Always 6 x 6: its own first, then the others without a twin of its own.
            listOf<Pair<String?, androidx.compose.ui.graphics.vector.ImageVector>>(null to category.icon) +
                com.bisc.flauncher.ui.home.search.CategoryIconChoices.filter { it.second != category.icon }.take(35)
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            choices.chunked(6).forEach { line ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    line.forEach { (key, vector) ->
                        val picked = key == iconKey
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                .background(if (picked) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .then(if (picked) Modifier.border(2.dp, color, androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) else Modifier)
                                .clickable { onIcon(key) },
                            contentAlignment = Alignment.Center
                        ) { Icon(vector, contentDescription = null, tint = if (picked) color else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    repeat(6 - line.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    SettingsPreferenceGroup(title = stringResource(R.string.settings_category_color)) {
        ColorPickerControls(color = color, onColorChange = onColor, modifier = Modifier.padding(12.dp))
    }
}

internal data class PickableApp(val packageName: String, val label: String)

@Composable
internal fun AppPickerDialog(
    title: String,
    clearLabel: String,
    apps: List<PickableApp>,
    selectedPackage: String?,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    ChoicePanel(
        title = title,
        options = apps.map { app ->
            ChoiceOption(
                label = app.label,
                selected = app.packageName == selectedPackage,
                onClick = { onPick(app.packageName) },
                leading = {
                    val icon = androidx.compose.runtime.remember(app.packageName) {
                        runCatching { context.packageManager.getApplicationIcon(app.packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
                    }
                    if (icon != null) androidx.compose.foundation.Image(icon, contentDescription = null, modifier = Modifier.size(32.dp))
                }
            )
        },
        onDismiss = onDismiss,
        footer = if (selectedPackage != null) ({ ChoicePanelAction(clearLabel, onClear) }) else null
    )
}

/** Apps that handle [uri] as a view intent, sorted by name. */
internal fun appsHandling(context: android.content.Context, uri: String, browsable: Boolean): List<PickableApp> {
    val pm = context.packageManager
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri.toUri())
    if (browsable) intent.addCategory(android.content.Intent.CATEGORY_BROWSABLE)
    return pm.queryIntentActivities(intent, 0)
        .map { PickableApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

/** Favorites of the Text search: which apps it shows, opened from the Favorites category. */
@Composable
fun SettingsSearchFavoritesScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val favorites by viewModel.searchFavorites.collectAsState()
    val favoritesMode by viewModel.searchFavoritesMode.collectAsState()
    val labeledApps by viewModel.labeledApps.collectAsState()
    val overrides by viewModel.appOverrides.collectAsState()
    var pickerOpen by remember { mutableStateOf(false) }
    SettingsSubScreen(title = stringResource(R.string.search_section_favorites), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(info = stringResource(R.string.settings_search_favorites_info)) {
            RadioPreference(stringResource(R.string.settings_search_favorites_mode_both), com.bisc.flauncher.data.model.SearchFavoritesMode.BOTH, favoritesMode, viewModel::setSearchFavoritesMode)
            RadioPreference(stringResource(R.string.settings_search_favorites_mode_manual), com.bisc.flauncher.data.model.SearchFavoritesMode.MANUAL, favoritesMode, viewModel::setSearchFavoritesMode)
            RadioPreference(stringResource(R.string.settings_search_favorites_mode_auto), com.bisc.flauncher.data.model.SearchFavoritesMode.AUTO, favoritesMode, viewModel::setSearchFavoritesMode)
        }
        if (favoritesMode != com.bisc.flauncher.data.model.SearchFavoritesMode.AUTO) {
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_search_favorites_pick),
                    summary = androidx.compose.ui.res.pluralStringResource(R.plurals.settings_search_favorites_count, favorites.size, favorites.size),
                    onClick = { pickerOpen = true }
                )
            }
        }
    }
    if (pickerOpen) {
        var picked by remember { mutableStateOf(favorites) }
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            title = { Text(stringResource(R.string.settings_search_favorites_pick)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    // Only apps the text search shows can be favorites.
                    val apps = labeledApps.filter { app ->
                        overrides[app.componentName.flattenToString()]?.let { it.hiddenFromSearch || it.revealMode == com.bisc.flauncher.data.model.RevealMode.SEARCH_CODE } != true
                    }.sortedBy { it.label.lowercase() }
                    items(apps, key = { it.componentName.flattenToString() }) { app ->
                        val id = app.componentName.flattenToString()
                        val on = id in picked
                        val full = !on && picked.size >= com.bisc.flauncher.data.model.SEARCH_FAVORITES_MAX
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !full) { picked = if (on) picked - id else picked + id }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(checked = on, enabled = !full, onCheckedChange = { picked = if (on) picked - id else picked + id })
                            Text(app.label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setSearchFavorites(picked); pickerOpen = false }) { Text(stringResource(R.string.widget_notes_save)) }
            },
            dismissButton = { TextButton(onClick = { pickerOpen = false }) { Text(stringResource(R.string.widget_notes_cancel)) } }
        )
    }
}
