package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.bisc.flauncher.ui.stackElement
import androidx.compose.foundation.layout.fillMaxSize
import com.bisc.flauncher.data.model.labelFace
import com.bisc.flauncher.data.model.face
import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.Category
import com.bisc.flauncher.data.model.CategoryMember
import com.bisc.flauncher.data.model.CategoryRule
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.PinnedContact
import com.bisc.flauncher.data.model.PinnedShortcut
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.repository.CategoryRepository
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.categoryColor
import com.bisc.flauncher.ui.home.categoryIcon
import com.bisc.flauncher.ui.home.inkOn
import com.bisc.flauncher.ui.home.CATEGORY_NO_COLOR
import com.bisc.flauncher.ui.home.hasFill
import com.bisc.flauncher.ui.home.hasSymbol

/** A category's symbol on its color, as a small round badge. */
@Composable
internal fun CategoryBadge(category: Category?, size: androidx.compose.ui.unit.Dp = 36.dp) {
    com.bisc.flauncher.ui.home.CategoryMark(category, modifier = Modifier.size(size))
}

/**
 * Every category in one card stack: ⓪ all of them (the way to the Categories page, their order,
 * a new one), then each category with its four tabs. The replica shows the chosen one as the
 * Categories page shows it, with its icon below. Several can be changed and saved at once.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsCategoriesScreen(
    navController: NavController,
    onBack: () -> Unit,
    startElement: String? = null,
    startTab: Int = 0,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val home: HomeViewModel = hiltViewModel()
    val stack: StackDraft = androidx.lifecycle.viewmodel.compose.viewModel()
    val categories by viewModel.categories.collectAsState()
    val resolved by viewModel.resolvedCategories.collectAsState()
    val counts = remember(resolved) { resolved.associate { it.category.id to it.members.size } }
    var choosingNew by remember { mutableStateOf(false) }
    // A category as drafted: what the replica and the list show.
    fun shown(c: Category): Category {
        val base = c.withLookFields()
        val k = categoryKey(c.id)
        return base.copy(
            name = DraftField(stack, "$k:name", base.name).value,
            color = DraftField(stack, "$k:color", base.color).value,
            tileFace = DraftField(stack, "$k:tile", base.tileFace).value,
            nameStyle = DraftField(stack, "$k:label", base.nameStyle).value
        )
    }

    val elements = buildList {
        add(StackElement(
            id = "all", number = 0,
            title = stringResource(R.string.settings_pages_categories),
            summary = pluralStringResource(R.plurals.hub_content_count, categories.size, categories.size)
        ) {
            PartIntro(stringResource(R.string.settings_categories_intro))
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = Icons.Filled.ViewModule,
                    title = stringResource(R.string.settings_categories_page),
                    summary = stringResource(R.string.settings_categories_page_summary),
                    onClick = { navController.navigate("settings/categories") },
                    trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                )
            }
            StackAddRow(stringResource(R.string.stack_new_category_title)) { choosingNew = true }
            if (categories.size > 1) {
                SettingsGroupLabel(stringResource(R.string.settings_categories_order), subtitle = stringResource(R.string.stack_categories_drag_info))
                ReorderableList(items = categories, key = { it.id }, onReorder = viewModel::reorderCategories, itemHeight = 64.dp, dividers = true) { category ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CategoryBadge(category)
                        Text(category.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        })
        categories.forEachIndexed { i, c ->
            val d = shown(c)
            val count = counts[c.id] ?: 0
            add(StackElement(
                id = categoryKey(c.id), number = i + 1,
                title = d.name.ifBlank { c.name },
                summary = if (c.smartRule != null) stringResource(R.string.settings_categories_tile_smart, count) else pluralStringResource(R.plurals.hub_content_count, count, count),
                draftKeys = listOf("name", "color", "tile", "label", "rule_param", "manual", "members").map { "${categoryKey(c.id)}:$it" },
                anchor = androidx.compose.ui.geometry.Offset(1f, 0f),
                leading = { CategoryBadge(d, 32.dp) }
            ) { CategoryEditor(c, stack, if (categoryKey(c.id) == startElement) startTab else 0, navController, viewModel, home) })
        }
    }

    val pageId by home.categoriesPageId.collectAsState()
    val background by remember(pageId) { home.pageBackgroundConfig(pageId ?: -1L) }.collectAsState(initial = com.bisc.flauncher.data.model.BackgroundConfig())
    val dark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(com.bisc.flauncher.data.model.ElementAppearance(fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE), background)
    val boxLook by home.categoryBoxLook.collectAsState()
    val iconScale by home.iconSizeScale.collectAsState()
    androidx.compose.runtime.CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalSurfaceIsDark provides dark) {
        SettingsCardStackScreen(
            title = stringResource(R.string.settings_pages_categories),
            navController = navController,
            onBack = onBack,
            draft = stack,
            elements = elements,
            startElement = startElement,
            firstCard = StackCard.ELEMENTS,
            listHeader = { StackAddRow(stringResource(R.string.stack_new_category_title)) { choosingNew = true } }
        ) { _, _ ->
            val ink = if (dark) Color.White else Color.Black
            val statusTop = replicaStatusHeight()
            val (look, corner) = boxLook
            val chosen = categories.find { categoryKey(it.id) == LocalStackSelected.current }
            val symbols = com.bisc.flauncher.ui.home.LauncherCategories.symbolsOnPage
            androidx.compose.runtime.CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalAppIconLoader provides home.iconLoader) {
                Box(Modifier.fillMaxSize()) {
                    com.bisc.flauncher.ui.home.dock.BackgroundFill(background, Modifier.fillMaxSize())
                    Column(Modifier.fillMaxSize().padding(top = statusTop + 12.dp)) {
                        // The labels on top, the chosen one filled.
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.bisc.flauncher.ui.home.CategoryChip(
                                category = null, label = stringResource(R.string.categories_all), count = null, selected = chosen == null,
                                look = look, onClick = {}, showSymbol = symbols
                            )
                            categories.forEach { c ->
                                val d = shown(c)
                                com.bisc.flauncher.ui.home.CategoryChip(
                                    category = d, label = d.name, count = (counts[c.id] ?: 0).takeIf { com.bisc.flauncher.ui.home.LauncherCategories.countsOnPage },
                                    selected = c == chosen, look = look, onClick = {}, showSymbol = symbols,
                                    modifier = if (c == chosen) Modifier.stackElement(categoryKey(c.id)) else Modifier
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (chosen != null) {
                            val d = shown(chosen)
                            val members = resolved.find { it.category.id == chosen.id }?.members.orEmpty()
                            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                val columns = com.bisc.flauncher.ui.home.categoryColumns(maxWidth - 24.dp)
                                com.bisc.flauncher.ui.home.CategoryCard(look, corner) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                        Box(Modifier.weight(1f)) { com.bisc.flauncher.ui.home.CategoryNamePill(d, MaterialTheme.typography.titleMedium, showSymbol = symbols) }
                                    }
                                    val labelColor = com.bisc.flauncher.ui.home.dock.resolveElementColors(look).contentColor
                                    if (members.isEmpty()) Text(stringResource(R.string.categories_nothing), modifier = Modifier.padding(12.dp), color = labelColor)
                                    else com.bisc.flauncher.ui.home.CategoryMemberGrid(members.take(columns * 3), columns, labelColor, iconScale, viewModel = home)
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            // As it lies in a Dock or a box: the icon at its real size with its name.
                            Column(
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 48.dp).width(96.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                com.bisc.flauncher.ui.home.CategoryMark(d, modifier = Modifier.size(56.dp * iconScale), shape = RoundedCornerShape(16.dp), ink = ink)
                                Spacer(Modifier.height(8.dp))
                                Text(d.name, color = ink, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else {
                            // All of them as icons, as they would lie in a Dock or a box.
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(top = 36.dp, start = 12.dp, end = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                maxItemsInEachRow = 4
                            ) {
                                categories.take(16).forEach { c ->
                                    val d = shown(c)
                                    Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        com.bisc.flauncher.ui.home.CategoryMark(d, modifier = Modifier.size(56.dp * iconScale).stackElement(categoryKey(c.id)), shape = RoundedCornerShape(16.dp), ink = ink)
                                        Spacer(Modifier.height(8.dp))
                                        Text(d.name, color = ink, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                    StatusBarSketch(statusTop, ink)
                }
            }
        }
    }

    if (choosingNew) {
        NewCategorySheet(
            viewModel = viewModel,
            onCreated = { id -> choosingNew = false; navController.navigate("settings/elements/categories/$id") },
            onDismiss = { choosingNew = false }
        )
    }
}

/** One category, the way in from outside: the stack, opened on it and on the tab asked for. */
@Composable
fun SettingsCategoryDetailScreen(categoryId: Long, navController: NavController, onBack: () -> Unit, startElement: String? = null) {
    val tab = when (startElement) {
        "icon" -> 1
        "label" -> 2
        "card" -> 3
        else -> 0
    }
    SettingsCategoriesScreen(navController, onBack, startElement = categoryKey(categoryId), startTab = tab)
}

/** The three ways to a new category, in one sheet: empty, a kind Android already knows, or a rule. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NewCategorySheet(viewModel: SettingsViewModel, onCreated: (Long) -> Unit, onDismiss: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val suggestions by viewModel.androidCategorySuggestions.collectAsState()
    val usedRules = remember(categories) { categories.mapNotNull { it.smartRule }.toSet() }
    val freeRules = CategoryRepository.READY_RULES.filter { it !in usedRules }
    // null = the three ways; "android" or "rule" = that list.
    var step by remember { mutableStateOf<String?>(null) }
    var naming by remember { mutableStateOf(false) }
    var makingRule by remember { mutableStateOf(false) }
    androidx.compose.material3.ModalBottomSheet(
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.stack_new_category_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 4.dp))
            when (step) {
                null -> SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = Icons.Filled.Add,
                        title = stringResource(R.string.stack_new_empty),
                        summary = stringResource(R.string.stack_new_empty_summary),
                        onClick = { naming = true }
                    )
                    if (suggestions.isNotEmpty()) {
                        SettingsPreference(
                            icon = Icons.Filled.Apps,
                            title = stringResource(R.string.stack_new_android),
                            summary = stringResource(R.string.stack_new_android_summary),
                            onClick = { step = "android" },
                            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                        )
                    }
                    SettingsPreference(
                        icon = Icons.Filled.Tune,
                        title = stringResource(R.string.stack_new_rule),
                        summary = stringResource(R.string.stack_new_rule_summary),
                        onClick = { step = "rule" },
                        trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                    )
                }
                "android" -> SettingsPreferenceGroup(title = stringResource(R.string.settings_categories_android), info = stringResource(R.string.settings_categories_android_info)) {
                    suggestions.forEach { suggestion ->
                        SettingsPreference(
                            icon = com.bisc.flauncher.ui.home.PictogramCatalog.iconFor(suggestion.kind.iconKey),
                            title = stringResource(suggestion.kind.nameRes),
                            summary = stringResource(R.string.settings_categories_count, suggestion.apps.size),
                            onClick = { viewModel.addCategoryFromSuggestion(suggestion); onDismiss() },
                            trailing = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.settings_categories_add_one), tint = MaterialTheme.colorScheme.primary) }
                        )
                    }
                }
                else -> SettingsPreferenceGroup(title = stringResource(R.string.settings_categories_smart), info = stringResource(R.string.settings_categories_smart_info)) {
                    freeRules.forEach { rule ->
                        val name = stringResource(CategoryRepository.ruleNameRes(rule))
                        SettingsPreference(
                            icon = com.bisc.flauncher.ui.home.PictogramCatalog.iconFor(CategoryRepository.ruleIconKey(rule)),
                            title = name,
                            summary = stringResource(ruleSummaryRes(rule)),
                            onClick = { viewModel.addCategory(name, rule = rule); onDismiss() },
                            trailing = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.settings_categories_add_one), tint = MaterialTheme.colorScheme.primary) }
                        )
                    }
                    SettingsPreference(
                        icon = Icons.Filled.Tune,
                        title = stringResource(R.string.settings_categories_own_rule),
                        summary = stringResource(R.string.settings_categories_own_rule_summary),
                        onClick = { makingRule = true }
                    )
                }
            }
            if (step != null) {
                TextButton(onClick = { step = null }) { Text(stringResource(R.string.settings_back)) }
            }
        }
    }
    if (naming) {
        NewCategoryDialog(
            onCreate = { name -> naming = false; viewModel.addCategory(name) { id -> onCreated(id) } },
            onDismiss = { naming = false }
        )
    }
    if (makingRule) {
        NewRuleDialog(
            onCreate = { name, rule ->
                makingRule = false
                val param = if (rule == CategoryRule.ANDROID_KIND) com.bisc.flauncher.data.repository.AndroidAppKind.GAME.name else null
                viewModel.addCategory(name, rule = rule, iconKey = CategoryRepository.ruleIconKey(rule), ruleParam = param) { id -> onCreated(id) }
            },
            onDismiss = { makingRule = false }
        )
    }
}

/** The category as a card, the way the Categories page shows it, from what is set right now. A tap
 *  on an app here does not open it; the dashed field below the apps adds more. */
@Composable
private fun CategoryLiveCard(
    category: Category,
    members: List<CategoryMember>,
    home: HomeViewModel,
    onAdd: (() -> Unit)?,
    onPick: ((CategoryMember) -> Unit)? = null,
    picked: CategoryMember? = null
) {
    val boxLook by home.categoryBoxLook.collectAsState()
    val (look, corner) = boxLook
    val labelColor = com.bisc.flauncher.ui.home.dock.resolveElementColors(look).contentColor
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = com.bisc.flauncher.ui.home.categoryColumns(maxWidth - 56.dp)
        com.bisc.flauncher.ui.home.CategoryCard(look, corner) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Box(modifier = Modifier.weight(1f)) { com.bisc.flauncher.ui.home.CategoryNamePill(category, MaterialTheme.typography.titleMedium) }
                Text(members.size.toString(), style = MaterialTheme.typography.labelLarge, color = labelColor.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 8.dp))
            }
            Box {
                if (members.isEmpty()) Text(stringResource(R.string.categories_nothing), modifier = Modifier.padding(12.dp), color = labelColor)
                else com.bisc.flauncher.ui.home.CategoryMemberGrid(members, columns, labelColor, viewModel = home, onPick = onPick, picked = picked)
                // Without picking only a picture: nothing opens from it.
                if (onPick == null) Box(Modifier.matchParentSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {})
            }
            if (onAdd != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .dashedOutline(labelColor.copy(alpha = 0.5f), 14.dp)
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.settings_category_pick_apps), tint = labelColor.copy(alpha = 0.7f))
                }
            }
        }
    }
}

/** One category in the overview: its tile, its name, how much is in it. */
@Composable
private fun CategoryTile(category: Category, count: Int, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        com.bisc.flauncher.ui.home.CategoryMark(category, modifier = Modifier.size(52.dp), shape = RoundedCornerShape(16.dp), ink = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        Text(category.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(
            if (category.smartRule != null) stringResource(R.string.settings_categories_tile_smart, count)
            else androidx.compose.ui.res.pluralStringResource(R.plurals.hub_content_count, count, count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

internal fun ruleSummaryRes(rule: CategoryRule): Int = when (rule) {
    CategoryRule.NEW -> R.string.settings_category_rule_new_summary
    CategoryRule.UNUSED -> R.string.settings_category_rule_unused_summary
    CategoryRule.FREQUENT -> R.string.settings_category_rule_frequent_summary
    CategoryRule.UNCATEGORIZED -> R.string.settings_category_rule_uncategorized_summary
    CategoryRule.NAME -> R.string.settings_category_rule_name_summary
    CategoryRule.ANDROID_KIND -> R.string.settings_category_rule_android_kind_summary
    CategoryRule.PROFILE -> R.string.settings_category_rule_profile_summary
}

/** Makes a smart category with a rule of your own: a name and what fills it. */
@Composable
private fun NewRuleDialog(onCreate: (String, CategoryRule) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var rule by remember { mutableStateOf(CategoryRule.NAME) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_categories_own_rule)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.settings_category_name)) },
                    singleLine = true,
                    shape = SettingsTextFieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(12.dp))
                Text(stringResource(R.string.settings_category_rule_pick), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                CategoryRule.entries.forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { rule = r }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(selected = rule == r, onClick = { rule = r })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(CategoryRepository.ruleNameRes(r)), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(ruleSummaryRes(r)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name.trim(), rule) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.settings_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.widget_notes_cancel)) } }
    )
}

/** A number with a minus and a plus beside it. */
@Composable
private fun NumberStepper(title: String, value: Int, range: IntRange, step: Int, onChange: (Int) -> Unit) {
    SettingsPreference(
        icon = null,
        title = title,
        summary = value.toString(),
        onClick = null,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onChange((value - step).coerceIn(range)) }, enabled = value > range.first) {
                    Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.a11y_less))
                }
                IconButton(onClick = { onChange((value + step).coerceIn(range)) }, enabled = value < range.last) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.a11y_more))
                }
            }
        }
    )
}

/** The setting of a smart category's rule: days, how many, the words, or Android's kind. */
@Composable
private fun RuleSettings(draft: Category, onChange: (Category) -> Unit) {
    val rule = draft.smartRule ?: return
    SettingsPreferenceGroup(title = stringResource(R.string.settings_category_smart_label), info = stringResource(ruleSummaryRes(rule))) {
        when (rule) {
            CategoryRule.NEW, CategoryRule.UNUSED -> {
                val default = if (rule == CategoryRule.NEW) CategoryRepository.NEW_DAYS.toInt() else CategoryRepository.UNUSED_DAYS.toInt()
                NumberStepper(
                    title = stringResource(if (rule == CategoryRule.NEW) R.string.settings_category_rule_days_new else R.string.settings_category_rule_days_unused),
                    value = draft.ruleParam?.toIntOrNull() ?: default,
                    range = 1..365,
                    step = if ((draft.ruleParam?.toIntOrNull() ?: default) >= 30) 5 else 1,
                    onChange = { onChange(draft.copy(ruleParam = it.toString())) }
                )
            }
            CategoryRule.FREQUENT -> NumberStepper(
                title = stringResource(R.string.settings_category_rule_most),
                value = draft.ruleParam?.toIntOrNull() ?: 16,
                range = 1..60,
                step = 1,
                onChange = { onChange(draft.copy(ruleParam = it.toString())) }
            )
            CategoryRule.NAME -> Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)) {
                OutlinedTextField(
                    value = draft.ruleParam.orEmpty(),
                    onValueChange = { onChange(draft.copy(ruleParam = it)) },
                    label = { Text(stringResource(R.string.settings_category_rule_words)) },
                    supportingText = { Text(stringResource(R.string.settings_category_rule_words_hint)) },
                    singleLine = true,
                    shape = SettingsTextFieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            CategoryRule.ANDROID_KIND -> com.bisc.flauncher.data.repository.AndroidAppKind.entries.forEach { kind ->
                RadioPreference(stringResource(kind.nameRes), kind.name, draft.ruleParam ?: "", { onChange(draft.copy(ruleParam = it)) })
            }
            CategoryRule.UNCATEGORIZED, CategoryRule.PROFILE -> SettingsPreference(icon = null, title = stringResource(ruleSummaryRes(rule)), onClick = null, trailing = null)
        }
    }
}

/** Asks for the name of a new category. */
@Composable
internal fun NewCategoryDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_categories_new)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_category_name)) },
                singleLine = true,
                shape = SettingsTextFieldShape,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onCreate(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.settings_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.widget_notes_cancel)) } }
    )
}

internal fun categoryKey(id: Long) = "cat:$id"

/**
 * One category's options in a stack, in four tabs: the category (name, color, where it is used),
 * its icon, its label and its card with what it holds. Stored together as one category at Save.
 */
@Composable
internal fun androidx.compose.foundation.layout.ColumnScope.CategoryEditor(
    saved: Category,
    stack: StackDraft,
    startTab: Int,
    navController: NavController,
    viewModel: SettingsViewModel,
    homeViewModel: HomeViewModel
) {
    val categoryId = saved.id
    val key = categoryKey(categoryId)
    val resolved by viewModel.resolvedCategories.collectAsState()
    // Categories from before the name look and the tile had their own fields: read once into them,
    // so the page starts from what is shown, and only a change makes it ask to save.
    val base = remember(saved) { saved.withLookFields() }
    val members = resolved.find { it.category.id == categoryId }?.members.orEmpty()
    val savedMembers = members.map { it.type to it.payload }

    // One value per part, so each shows its own dot; stored together, as one category.
    val name = stack.field("$key:name", base.name) {}
    val color = stack.field("$key:color", base.color) {}
    val tile = stack.field("$key:tile", base.tileFace) {}
    val label = stack.field("$key:label", base.nameStyle) {}
    val ruleParam = stack.field("$key:rule_param", base.ruleParam) {}
    val manual = stack.field("$key:manual", base.manualOrder) {}
    val draftMembers = stack.field("$key:members", savedMembers) {}
    val draft = base.copy(name = name.value, color = color.value, tileFace = tile.value, nameStyle = label.value, ruleParam = ruleParam.value, manualOrder = manual.value)

    val apps by viewModel.labeledApps.collectAsState()
    val websites by viewModel.pinnedWebsites.collectAsState()
    val places by viewModel.savedMapTargets.collectAsState()
    val shortcuts by viewModel.pinnedShortcuts.collectAsState()
    val contacts by homeViewModel.pinnedContacts.collectAsState()
    var pickingApps by remember { mutableStateOf(false) }
    var pickingElements by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var picked by remember(categoryId) { mutableStateOf<CategoryMember?>(null) }
    val rule = saved.smartRule
    // Shown like the category will show them: in the own order, else apps by name and the rest after.
    val shownMembers: List<CategoryMember> = if (rule != null) members else {
        val ordered = if (draft.manualOrder) draftMembers.value else {
            val (appMembers, others) = draftMembers.value.partition { it.first == GridItemType.APP }
            appMembers.sortedBy { (_, key) -> apps.find { it.componentName.flattenToString() == key }?.label?.lowercase() } + others
        }
        ordered.mapIndexed { index, (type, payload) -> CategoryMember(categoryId = categoryId, type = type, payload = payload, orderIndex = index) }
    }
    val membersChanged = rule == null && if (draft.manualOrder) draftMembers.value != savedMembers else draftMembers.value.toSet() != savedMembers.toSet()
    stack.saveTogether(key) {
        // From the category as stored now (its place may have moved meanwhile), with only what changed here.
        val now = viewModel.categories.value.find { it.id == categoryId }?.withLookFields() ?: return@saveTogether
        fun <T> pick(field: DraftField<T>, stored: T): T = if (stack.changed(listOf(field.key))) field.value else stored
        val written = now.copy(
            name = pick(name, now.name),
            color = pick(color, now.color),
            tileFace = pick(tile, now.tileFace),
            nameStyle = pick(label, now.nameStyle),
            ruleParam = pick(ruleParam, now.ruleParam),
            manualOrder = pick(manual, now.manualOrder)
        )
        if (written.name.isNotBlank() && written != now) viewModel.updateCategory(written)
        if (membersChanged || draft.manualOrder != base.manualOrder) viewModel.replaceCategoryMembers(categoryId, shownMembers.map { it.type to it.payload })
    }

    val countText = if (rule != null) stringResource(R.string.hub_content_smart, pluralStringResource(R.plurals.hub_content_count, shownMembers.size, shownMembers.size))
        else pluralStringResource(R.plurals.hub_content_count, shownMembers.size, shownMembers.size)
    val chipsInAppList by viewModel.appListCategoryChips.collectAsState()
    val usages = rememberUsages(
        GridItemType.CATEGORY, categoryId.toString(), homeViewModel,
        extra = listOfNotNull(
            Usage(stringResource(R.string.stack_category_page_place), "settings/categories"),
            if (chipsInAppList) Usage(stringResource(R.string.stack_app_list_place), "settings/app_overview") else null
        )
    )

    /** Moves the picked member by [delta]; the order becomes the own one, as it is shown now. */
    fun move(member: CategoryMember, delta: Int) {
        val list = shownMembers.map { it.type to it.payload }.toMutableList()
        val from = list.indexOf(member.type to member.payload)
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        list.add(to, list.removeAt(from))
        draftMembers.value = list
        manual.value = true
    }

    StackTabs(listOf(
        stringResource(R.string.stack_el_category) to {
            SettingsPreferenceGroup {
                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)) {
                    OutlinedTextField(
                        value = name.value,
                        onValueChange = { name.value = it },
                        label = { Text(stringResource(R.string.settings_category_name)) },
                        singleLine = true,
                        shape = SettingsTextFieldShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            SettingsPreferenceGroup(title = stringResource(R.string.category_color_title), info = stringResource(R.string.category_color_info)) {
                AutomaticColorPicker(
                    title = stringResource(R.string.settings_category_color),
                    custom = draft.color,
                    automaticSeed = categoryColor(draft.copy(color = null)).toArgb(),
                    onChange = { color.value = it },
                    ownGroup = false
                )
            }
            UsagesGroup(usages, navController)
            SettingsPreferenceGroup {
                SettingsPreference(icon = Icons.Filled.DeleteOutline, title = stringResource(R.string.settings_category_delete), onClick = { confirmingDelete = true }, trailing = null)
            }
        },
        stringResource(R.string.stack_el_category_icon) to {
            PartIntro(stringResource(R.string.settings_category_tile_info))
            TileFaceEditor(
                face = draft.face(),
                onChange = { tile.value = it.toJson() },
                autoSurface = categoryColor(draft),
                letter = draft.name,
                content = shownMembers.take(4).map { member ->
                    if (member.type == GridItemType.APP) com.bisc.flauncher.ui.home.TileContent.App(ComponentName.unflattenFromString(member.payload))
                    else com.bisc.flauncher.ui.home.TileContent.Element(memberTypeIcon(member.type))
                },
                contentSummary = stringResource(R.string.tile_content_category),
                viewModel = viewModel
            )
        },
        stringResource(R.string.stack_el_category_label) to {
            PartIntro(stringResource(R.string.category_name_look_info))
            SurfaceLookSection(
                face = draft.labelFace(),
                onChange = { label.value = it.toJson() },
                autoSurface = categoryColor(draft),
                inkLabel = stringResource(R.string.settings_element_text_color_label),
                cornersFollowApps = false
            )
        },
        stringResource(R.string.stack_el_category_card) to {
            PartIntro(stringResource(if (rule != null) R.string.section_content_smart else R.string.section_content_own))
            CategoryLiveCard(
                draft, shownMembers, homeViewModel,
                onAdd = if (rule == null) ({ pickingApps = true }) else null,
                onPick = if (rule == null) ({ m -> picked = if (picked?.let { it.type == m.type && it.payload == m.payload } == true) null else m }) else null,
                picked = picked
            )
            val current = picked?.let { p -> shownMembers.find { it.type == p.type && it.payload == p.payload } }
            if (current != null) {
                val index = shownMembers.indexOf(current)
                RowEntrySelectionBar(
                    title = categoryMemberLabel(current, apps, websites, places, contacts),
                    canMoveBack = index > 0,
                    canMoveOn = index < shownMembers.lastIndex,
                    onMove = { delta -> move(current, delta) },
                    onOpenSettings = null,
                    onRemove = { draftMembers.value = draftMembers.value - (current.type to current.payload); picked = null },
                    onClose = { picked = null }
                )
            } else if (rule == null && shownMembers.isNotEmpty()) {
                Text(
                    stringResource(R.string.stack_content_pick_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            if (rule == null) {
                AddButtons(
                    Icons.Filled.Apps to stringResource(R.string.settings_category_pick_apps), { pickingApps = true },
                    Icons.Filled.Language to stringResource(R.string.settings_category_pick_elements), { pickingElements = true }
                )
            }
            if (rule != null) RuleSettings(draft) { ruleParam.value = it.ruleParam }
            if (rule == null && shownMembers.size > 1) {
                // Switching on keeps the order as it is shown now.
                val setManual = { on: Boolean ->
                    if (on) draftMembers.value = shownMembers.map { it.type to it.payload }
                    manual.value = on
                }
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_category_manual_order),
                        summary = stringResource(if (draft.manualOrder) R.string.settings_category_manual_order_on else R.string.settings_category_manual_order_off),
                        onClick = { setManual(!draft.manualOrder) },
                        trailing = { androidx.compose.material3.Switch(checked = draft.manualOrder, onCheckedChange = { setManual(it) }) }
                    )
                }
            }
        }
    ), start = startTab)

    if (pickingApps) {
        val chosen = draftMembers.value.filter { it.first == GridItemType.APP }.map { it.second }.toSet()
        CategoryAppsPicker(
            title = saved.name,
            apps = apps,
            initial = chosen,
            loadIcon = viewModel::loadIcon,
            onDone = { pickedApps ->
                // Everything kept stays where it was (an own order too); new apps come at the end, by name.
                val kept = draftMembers.value.filter { it.first != GridItemType.APP || it.second in pickedApps }
                val added = pickedApps.filter { it !in chosen }
                    .sortedBy { key -> apps.find { it.componentName.flattenToString() == key }?.label?.lowercase() }
                    .map { GridItemType.APP to it }
                draftMembers.value = kept + added
                pickingApps = false
            },
            onDismiss = { pickingApps = false }
        )
    }
    if (pickingElements) {
        CategoryElementsSheet(
            members = shownMembers,
            websites = websites,
            places = places,
            contacts = contacts,
            shortcuts = shortcuts,
            onToggle = { type, payload, on ->
                val list = draftMembers.value
                draftMembers.value = if (on) (list - (type to payload)) + (type to payload) else list - (type to payload)
            },
            onDismiss = { pickingElements = false }
        )
    }
    if (confirmingDelete) {
        ConfirmRemoveDialog(
            label = saved.name,
            onConfirm = { viewModel.deleteCategory(saved); confirmingDelete = false },
            onDismiss = { confirmingDelete = false }
        )
    }
}

/** The name of any member, for lists. */
internal fun categoryMemberLabel(
    member: CategoryMember,
    apps: List<AppInfo>,
    websites: List<PinnedWebsite>,
    places: List<SavedMapTarget>,
    contacts: List<PinnedContact>
): String = when (member.type) {
    GridItemType.APP -> apps.find { it.componentName.flattenToString() == member.payload }?.label.orEmpty()
    GridItemType.WEBSITE -> websites.find { it.id == member.payload.toLongOrNull() }?.label.orEmpty()
    GridItemType.MAP_TARGET -> places.find { it.id == member.payload.toLongOrNull() }?.label.orEmpty()
    GridItemType.CONTACT -> contacts.find { it.contactId == member.payload.toLongOrNull() }?.displayName.orEmpty()
    GridItemType.SHORTCUT -> member.payload.split("|").getOrNull(2)?.takeIf { it.isNotBlank() } ?: member.payload.substringBefore('|')
    else -> ""
}

@Composable
private fun CategoryMemberRow(
    member: CategoryMember,
    apps: List<AppInfo>,
    websites: List<PinnedWebsite>,
    places: List<SavedMapTarget>,
    contacts: List<PinnedContact>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onRemove: (() -> Unit)?,
    // Inside a reorderable list, which brings its own background and margins.
    inList: Boolean = false
) {
    Row(
        modifier = if (inList) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().heightIn(min = 64.dp).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            when (member.type) {
                GridItemType.APP -> {
                    val component = remember(member.payload) { ComponentName.unflattenFromString(member.payload) }
                    val bitmap = com.bisc.flauncher.ui.home.rememberAppIcon(component, loadIcon)
                    if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.size(36.dp))
                }
                else -> Icon(memberTypeIcon(member.type), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(categoryMemberLabel(member, apps, websites, places, contacts), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.a11y_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

internal fun memberTypeIcon(type: GridItemType) = when (type) {
    GridItemType.WEBSITE -> Icons.Filled.Language
    GridItemType.MAP_TARGET -> Icons.Filled.Place
    GridItemType.CONTACT -> Icons.Filled.Person
    GridItemType.SHORTCUT -> Icons.AutoMirrored.Filled.Shortcut
    else -> Icons.Filled.Apps
}

/** Picks the apps of a category: all of them in a grid, the chosen ones marked. */
@Composable
internal fun CategoryAppsPicker(
    title: String,
    apps: List<AppInfo>,
    initial: Set<String>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onDone: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(initial) } }
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }
    SettingsFixedDialog(
        title = title,
        onDismiss = onDismiss,
        actions = { TextButton(onClick = { onDone(selected.toSet()) }) { Text(stringResource(R.string.settings_categories_done)) } },
        bottomBar = {
            Button(
                onClick = { onDone(selected.toSet()) },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text(stringResource(R.string.settings_category_apps_done, selected.size)) }
        }
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.settings_folder_creator_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = SettingsTextFieldShape,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(84.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filtered.size, key = { filtered[it].componentName.flattenToString() }) { index ->
                val app = filtered[index]
                val key = app.componentName.flattenToString()
                val bitmap = com.bisc.flauncher.ui.home.rememberAppIcon(app.componentName, loadIcon)
                val isSelected = key in selected
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { if (isSelected) selected.remove(key) else selected.add(key) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        if (bitmap != null) Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(44.dp))
                        else Spacer(Modifier.size(44.dp))
                        if (isSelected) {
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp)) }
                        }
                    }
                    Text(
                        app.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/** Links, places, contacts and shortcuts you already have, each with a box to put it in or out. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryElementsSheet(
    members: List<CategoryMember>,
    websites: List<PinnedWebsite>,
    places: List<SavedMapTarget>,
    contacts: List<PinnedContact>,
    shortcuts: List<PinnedShortcut>,
    onToggle: (GridItemType, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val inCategory = remember(members) { members.map { it.type to it.payload }.toSet() }
    ModalBottomSheet(sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = com.bisc.flauncher.ui.sheetMaxHeight()).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.settings_category_pick_elements_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            Text(stringResource(R.string.settings_category_pick_elements_summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            val sections = listOf(
                Triple(R.string.settings_add_kind_link, GridItemType.WEBSITE, websites.map { it.id.toString() to it.label }),
                Triple(R.string.settings_add_kind_place, GridItemType.MAP_TARGET, places.map { it.id.toString() to it.label }),
                Triple(R.string.settings_add_kind_contact, GridItemType.CONTACT, contacts.map { it.contactId.toString() to it.displayName }),
                Triple(R.string.settings_add_kind_shortcut, GridItemType.SHORTCUT, shortcuts.map { "${it.packageName}|${it.shortcutId}|${it.label}" to it.label })
            )
            if (sections.all { it.third.isEmpty() }) {
                Text(stringResource(R.string.settings_category_no_elements), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(4.dp))
            }
            sections.forEach { (titleRes, type, items) ->
                if (items.isEmpty()) return@forEach
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp))
                items.forEach { (payload, label) ->
                    val checked = (type to payload) in inCategory
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onToggle(type, payload, !checked) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(memberTypeIcon(type), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Checkbox(checked = checked, onCheckedChange = { onToggle(type, payload, it) })
                    }
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text(stringResource(R.string.settings_categories_done)) }
        }
    }
}

/** Picks a category: the current one checked, and a new one can be made right here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<Category>,
    onSelect: (Category) -> Unit,
    onDismiss: () -> Unit,
    selectedId: Long? = null,
    onCreate: ((String) -> Unit)? = null
) {
    var creating by remember { mutableStateOf(false) }
    ModalBottomSheet(sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = com.bisc.flauncher.ui.sheetMaxHeight()).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.settings_categories_pick_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            if (categories.isEmpty() && onCreate == null) {
                Text(stringResource(R.string.settings_categories_picker_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SettingsPreferenceGroup {
                categories.forEach { category ->
                    SettingsPreference(
                        icon = null,
                        leading = { CategoryBadge(category) },
                        title = category.name,
                        onClick = { onSelect(category) },
                        trailing = if (category.id == selectedId) ({ Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }) else null
                    )
                }
                if (onCreate != null) {
                    SettingsPreference(icon = Icons.Filled.Add, title = stringResource(R.string.settings_categories_new), onClick = { creating = true }, trailing = null)
                }
            }
        }
    }
    if (creating && onCreate != null) {
        NewCategoryDialog(onCreate = { name -> creating = false; onCreate(name) }, onDismiss = { creating = false })
    }
}

/** The categories of one element with a check each, and a way to make a new one on the spot. */
@Composable
fun ElementCategoriesDialog(
    title: String,
    categories: List<Category>,
    members: Set<Long>,
    onToggle: (Long, Boolean) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    val own = categories.filter { it.smartRule == null }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(modifier = Modifier.heightIn(max = com.bisc.flauncher.ui.sheetMaxHeight()).verticalScroll(rememberScrollState())) {
                if (own.isEmpty()) {
                    Text(stringResource(R.string.settings_categories_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                own.forEach { category ->
                    val checked = category.id in members
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onToggle(category.id, !checked) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryBadge(category, 28.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Checkbox(checked = checked, onCheckedChange = { onToggle(category.id, it) })
                    }
                }
                TextButton(onClick = { creating = true }, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_categories_new))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_categories_done)) } }
    )
    if (creating) {
        NewCategoryDialog(onCreate = { name -> creating = false; onCreate(name) }, onDismiss = { creating = false })
    }
}

/** A Categories widget's own choice: how it shows, which categories, and the chips. Part of the
 *  widget screen's draft, written with its Save. */
@Composable
internal fun CategoryWidgetSettings(
    config: com.bisc.flauncher.data.model.CategoryWidgetConfig,
    onChange: (com.bisc.flauncher.data.model.CategoryWidgetConfig) -> Unit,
    navController: NavController,
    viewModel: HomeViewModel
) {
    val resolved by viewModel.resolvedCategories.collectAsState()
    fun set(new: com.bisc.flauncher.data.model.CategoryWidgetConfig) = onChange(new)
    val single = config.mode == com.bisc.flauncher.data.model.CategoryWidgetMode.SINGLE

    SettingsPreferenceGroup(title = stringResource(R.string.widget_categories_mode)) {
        RadioPreference(stringResource(R.string.widget_categories_mode_tabs), com.bisc.flauncher.data.model.CategoryWidgetMode.TABS, config.mode, { set(config.copy(mode = it)) }, summary = stringResource(R.string.widget_categories_mode_tabs_summary))
        RadioPreference(stringResource(R.string.widget_categories_mode_rows), com.bisc.flauncher.data.model.CategoryWidgetMode.ROWS, config.mode, { set(config.copy(mode = it)) }, summary = stringResource(R.string.widget_categories_mode_rows_summary))
        RadioPreference(stringResource(R.string.widget_categories_mode_single), com.bisc.flauncher.data.model.CategoryWidgetMode.SINGLE, config.mode, { set(config.copy(mode = it, ids = config.ids.take(1))) }, summary = stringResource(R.string.widget_categories_mode_single_summary))
        RadioPreference(stringResource(R.string.widget_categories_mode_tiles), com.bisc.flauncher.data.model.CategoryWidgetMode.TILES, config.mode, { set(config.copy(mode = it)) }, summary = stringResource(R.string.widget_categories_mode_tiles_summary))
        if (config.mode != com.bisc.flauncher.data.model.CategoryWidgetMode.TILES) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_categories_symbols),
                summary = stringResource(R.string.settings_category_symbols_widget_summary),
                onClick = { set(config.copy(symbols = !config.symbols)) },
                trailing = { androidx.compose.material3.Switch(checked = config.symbols, onCheckedChange = { set(config.copy(symbols = it)) }) }
            )
        }
        // Stacked and compact show how much is in a category.
        if (config.mode == com.bisc.flauncher.data.model.CategoryWidgetMode.ROWS || config.mode == com.bisc.flauncher.data.model.CategoryWidgetMode.TILES) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_categories_counts),
                onClick = { set(config.copy(counts = !config.counts)) },
                trailing = { androidx.compose.material3.Switch(checked = config.counts, onCheckedChange = { set(config.copy(counts = it)) }) }
            )
        }
        if (config.mode == com.bisc.flauncher.data.model.CategoryWidgetMode.TABS) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.widget_categories_chips),
                summary = stringResource(R.string.widget_categories_chips_summary),
                onClick = { set(config.copy(chips = !config.chips)) },
                trailing = { androidx.compose.material3.Switch(checked = config.chips, onCheckedChange = { set(config.copy(chips = it)) }) }
            )
        }
    }
    SettingsPreferenceGroup(
        title = stringResource(if (single) R.string.widget_categories_which_one else R.string.widget_categories_which),
        info = if (single) null else stringResource(R.string.widget_categories_which_info)
    ) {
        if (resolved.isEmpty()) {
            SettingsPreference(icon = null, title = stringResource(R.string.settings_categories_empty), onClick = { navController.navigate("settings/elements/categories") })
        }
        if (!single) {
            RadioPreference(stringResource(R.string.widget_categories_all), true, config.ids.isEmpty(), { if (it) set(config.copy(ids = emptyList())) })
        }
        resolved.forEach { r ->
            val id = r.category.id
            val checked = if (single) (config.ids.firstOrNull() ?: resolved.first().category.id) == id else config.ids.isEmpty() || id in config.ids
            SettingsPreference(
                icon = null,
                leading = { CategoryBadge(r.category, 32.dp) },
                title = r.category.name,
                onClick = {
                    if (single) set(config.copy(ids = listOf(id)))
                    else {
                        // Leaving "all" starts from all of them, so unticking one keeps the rest.
                        val current = config.ids.ifEmpty { resolved.map { it.category.id } }
                        val next = if (id in current) current - id else resolved.map { it.category.id }.filter { it in current || it == id }
                        // At least one stays chosen.
                        if (next.isNotEmpty()) set(config.copy(ids = if (next.size == resolved.size) emptyList() else next))
                    }
                },
                trailing = {
                    if (single) androidx.compose.material3.RadioButton(selected = checked, onClick = null)
                    else Checkbox(checked = checked, onCheckedChange = null)
                }
            )
        }
    }
}

/** The categories of one app as chips: a tap puts it in or takes it out right away. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AppCategoryChips(app: AppInfo, viewModel: SettingsViewModel) {
    val resolved by viewModel.resolvedCategories.collectAsState()
    val payload = app.componentName.flattenToString()
    val own = resolved.filter { it.category.smartRule == null }
    var creating by remember { mutableStateOf(false) }
    Column {
        SettingsGroupLabel(stringResource(R.string.app_menu_categories), info = stringResource(R.string.settings_app_categories_info))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            own.forEach { r ->
                val inIt = r.members.any { it.type == GridItemType.APP && it.payload == payload }
                androidx.compose.material3.FilterChip(
                    selected = inIt,
                    onClick = { viewModel.setCategoryMember(r.category.id, GridItemType.APP, payload, !inIt) },
                    label = { Text(r.category.name) },
                    leadingIcon = null
                )
            }
            androidx.compose.material3.AssistChip(
                onClick = { creating = true },
                label = { Text(stringResource(R.string.settings_categories_new)) },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
    }
    if (creating) {
        NewCategoryDialog(
            onCreate = { name ->
                creating = false
                viewModel.addCategory(name) { id -> viewModel.setCategoryMember(id, GridItemType.APP, payload, true) }
            },
            onDismiss = { creating = false }
        )
    }
}

/** "Show how many" for one place that shows categories by name. */
@Composable
internal fun CategoryCountsSwitch(place: String, viewModel: SettingsViewModel) {
    val on by when (place) {
        "page" -> viewModel.categoryCountsPage
        "search" -> viewModel.categoryCountsSearch
        else -> viewModel.categoryCountsAppList
    }.collectAsState()
    SettingsPreference(
        icon = null,
        title = stringResource(R.string.settings_categories_counts),
        onClick = { viewModel.setCategoryCounts(place, !on) },
        trailing = { androidx.compose.material3.Switch(checked = on, onCheckedChange = { viewModel.setCategoryCounts(place, it) }) }
    )
}

/** "Show symbols" for one place that shows categories by name. */
@Composable
internal fun CategorySymbolsSwitch(place: String, summary: String, viewModel: SettingsViewModel) {
    val on by when (place) {
        "page" -> viewModel.categorySymbolsPage
        "search" -> viewModel.categorySymbolsSearch
        else -> viewModel.categorySymbolsAppList
    }.collectAsState()
    SettingsPreference(
        icon = null,
        title = stringResource(R.string.settings_categories_symbols),
        summary = summary,
        onClick = { viewModel.setCategorySymbols(place, !on) },
        trailing = { androidx.compose.material3.Switch(checked = on, onCheckedChange = { viewModel.setCategorySymbols(place, it) }) }
    )
}

/** This category with its look in the fields made for it: older ones kept it in the symbol and the
 *  color (0 = an outline). Shown the same, so nothing changes until something is chosen. */
private fun Category.withLookFields(): Category =
    if (tileFace != null && nameStyle?.startsWith("{") == true && color != CATEGORY_NO_COLOR) this
    else copy(
        tileFace = face().toJson(),
        nameStyle = labelFace().toJson(),
        color = color.takeIf { it != CATEGORY_NO_COLOR }
    )
