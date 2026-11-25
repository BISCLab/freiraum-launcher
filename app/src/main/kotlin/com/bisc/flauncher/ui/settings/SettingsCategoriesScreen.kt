package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import com.bisc.flauncher.data.model.nameLook
import com.bisc.flauncher.data.model.face
import com.bisc.flauncher.data.model.CategoryNameStyle
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
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

/** Settings > Elements > Categories: every category in its order, new ones, Android's own kinds as
 *  ready-made offers and the smart categories that fill themselves. */
@Composable
fun SettingsCategoriesScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsState()
    val resolved by viewModel.resolvedCategories.collectAsState()
    val suggestions by viewModel.androidCategorySuggestions.collectAsState()
    val counts = remember(resolved) { resolved.associate { it.category.id to it.members.size } }
    var creating by remember { mutableStateOf(false) }
    val usedRules = remember(categories) { categories.mapNotNull { it.smartRule }.toSet() }
    val freeRules = CategoryRepository.READY_RULES.filter { it !in usedRules }
    var makingRule by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_pages_categories), navController = navController, onBack = onBack) {
        Text(
            stringResource(R.string.settings_categories_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Column {
            SettingsGroupLabel(stringResource(R.string.settings_categories_yours), info = stringResource(R.string.settings_categories_yours_info))
            if (categories.isEmpty()) {
                SettingsPreferenceGroup {
                    SettingsPreference(icon = null, title = stringResource(R.string.settings_categories_empty), onClick = null, trailing = null)
                }
            } else {
                ReorderableList(
                    items = categories,
                    key = { it.id },
                    onReorder = viewModel::reorderCategories,
                    itemHeight = 72.dp,
                    dividers = true
                ) { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("settings/elements/categories/${category.id}") },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CategoryBadge(category)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(category.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (category.smartRule != null) stringResource(R.string.settings_categories_smart_count, counts[category.id] ?: 0)
                                else stringResource(R.string.settings_categories_count, counts[category.id] ?: 0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        SettingsPreferenceGroup {
            SettingsPreference(icon = Icons.Filled.Add, title = stringResource(R.string.settings_categories_new), onClick = { creating = true }, trailing = null)
            SettingsPreference(
                icon = Icons.Filled.ViewModule,
                title = stringResource(R.string.settings_categories_page),
                summary = stringResource(R.string.settings_categories_page_summary),
                onClick = { navController.navigate("settings/categories") }
            )
        }
        if (suggestions.isNotEmpty()) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_categories_android), info = stringResource(R.string.settings_categories_android_info)) {
                suggestions.forEach { suggestion ->
                    SettingsPreference(
                        icon = com.bisc.flauncher.ui.home.PictogramCatalog.iconFor(suggestion.kind.iconKey),
                        title = stringResource(suggestion.kind.nameRes),
                        summary = stringResource(R.string.settings_categories_count, suggestion.apps.size),
                        onClick = { viewModel.addCategoryFromSuggestion(suggestion) },
                        trailing = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.settings_categories_add_one), tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_categories_smart), info = stringResource(R.string.settings_categories_smart_info)) {
                freeRules.forEach { rule ->
                    val name = stringResource(CategoryRepository.ruleNameRes(rule))
                    SettingsPreference(
                        icon = com.bisc.flauncher.ui.home.PictogramCatalog.iconFor(CategoryRepository.ruleIconKey(rule)),
                        title = name,
                        summary = stringResource(ruleSummaryRes(rule)),
                        onClick = { viewModel.addCategory(name, rule = rule) },
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

    if (makingRule) {
        NewRuleDialog(
            onCreate = { name, rule ->
                makingRule = false
                val param = if (rule == CategoryRule.ANDROID_KIND) com.bisc.flauncher.data.repository.AndroidAppKind.GAME.name else null
                viewModel.addCategory(name, rule = rule, iconKey = CategoryRepository.ruleIconKey(rule), ruleParam = param) { id -> navController.navigate("settings/elements/categories/$id") }
            },
            onDismiss = { makingRule = false }
        )
    }

    if (creating) {
        NewCategoryDialog(
            onCreate = { name ->
                creating = false
                viewModel.addCategory(name) { id -> navController.navigate("settings/elements/categories/$id") }
            },
            onDismiss = { creating = false }
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

/** The pages of one category: an overview with name and color, then the name's look, the tile's look
 *  and what it holds each on their own. Every page keeps a draft, written with Save. */
enum class CategoryPart { HUB, NAME, TILE, CONTENT }

/** One category, or one part of it, see [CategoryPart]. */
@Composable
fun SettingsCategoryDetailScreen(categoryId: Long, navController: NavController, onBack: () -> Unit, part: CategoryPart = CategoryPart.HUB, viewModel: SettingsViewModel = hiltViewModel()) {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val categories by viewModel.categories.collectAsState()
    val resolved by viewModel.resolvedCategories.collectAsState()
    val saved = categories.find { it.id == categoryId } ?: return
    // Categories from before the name look and the tile had their own fields: read once into them,
    // so the page starts from what is shown, and only a change makes it ask to save.
    val base = remember(saved) { saved.withLookFields() }
    var draft by rememberDraft(base)
    val members = resolved.find { it.category.id == categoryId }?.members.orEmpty()
    // What an own category holds, as a draft: kind and payload in their stored order.
    val savedMembers = members.map { it.type to it.payload }
    var draftMembers by rememberDraft(savedMembers)
    val apps by viewModel.labeledApps.collectAsState()
    val websites by viewModel.pinnedWebsites.collectAsState()
    val places by viewModel.savedMapTargets.collectAsState()
    val shortcuts by viewModel.pinnedShortcuts.collectAsState()
    val contacts by homeViewModel.pinnedContacts.collectAsState()
    var pickingApps by remember { mutableStateOf(false) }
    var pickingElements by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val rule = saved.smartRule
    // Shown like the category will show them: in the own order, else apps by name and the rest after.
    val shownMembers: List<CategoryMember> = if (rule != null) members else {
        val ordered = if (draft.manualOrder) draftMembers else {
            val (appMembers, others) = draftMembers.partition { it.first == GridItemType.APP }
            appMembers.sortedBy { (_, key) -> apps.find { it.componentName.flattenToString() == key }?.label?.lowercase() } + others
        }
        ordered.mapIndexed { index, (type, payload) -> CategoryMember(categoryId = categoryId, type = type, payload = payload, orderIndex = index) }
    }
    val membersChanged = rule == null && if (draft.manualOrder) draftMembers != savedMembers else draftMembers.toSet() != savedMembers.toSet()

    val partTitle = when (part) {
        CategoryPart.NAME -> stringResource(R.string.section_as_name)
        CategoryPart.TILE -> stringResource(R.string.settings_category_tile_label)
        CategoryPart.CONTENT -> stringResource(R.string.section_content)
        CategoryPart.HUB -> saved.name
    }
    val tilePreview: @Composable (Color) -> Unit = { around -> com.bisc.flauncher.ui.home.CategoryMark(draft, modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp), ink = around) }
    val namePreview: @Composable (Color) -> Unit = { around -> com.bisc.flauncher.ui.home.CategoryNamePill(draft, MaterialTheme.typography.labelLarge, ink = around) }
    SettingsSubScreen(
        title = partTitle,
        navController = navController,
        onBack = onBack,
        onSave = {
            viewModel.updateCategory(draft)
            if (membersChanged || draft.manualOrder != base.manualOrder) viewModel.replaceCategoryMembers(categoryId, shownMembers.map { it.type to it.payload })
        },
        dirty = draft != base || membersChanged,
        // What this page changes, as it will look, always in sight while choosing.
        header = if (part == CategoryPart.CONTENT) null else ({
            TilePreviewHeader(
                name = draft.name,
                tile = if (part == CategoryPart.NAME) null else tilePreview,
                extra = if (part == CategoryPart.TILE) null else namePreview,
                tileCaption = stringResource(R.string.preview_caption_tile),
                extraCaption = stringResource(R.string.preview_caption_name)
            )
        })
    ) {
        when (part) {
            CategoryPart.HUB -> {
                SettingsPreferenceGroup {
                    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)) {
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { draft = draft.copy(name = it) },
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
                        onChange = { draft = draft.copy(color = it) },
                        ownGroup = false
                    )
                }
                // Look and content each on a page of their own, so it is always clear what is changed.
                val open = { sub: String -> navController.navigate("settings/elements/categories/$categoryId/$sub") }
                SettingsTileGrid(
                    columns = 3,
                    title = stringResource(R.string.settings_parts_label),
                    tiles = listOf(
                        SettingsTile(Icons.AutoMirrored.Filled.Label, stringResource(R.string.section_as_name)) { open("name") },
                        SettingsTile(Icons.Filled.Apps, stringResource(R.string.settings_category_tile_label)) { open("tile") },
                        SettingsTile(Icons.Filled.Inventory2, stringResource(R.string.section_content), badge = shownMembers.size.toString()) { open("content") }
                    )
                )
                SettingsPreferenceGroup {
                    SettingsPreference(icon = Icons.Filled.DeleteOutline, title = stringResource(R.string.settings_category_delete), onClick = { confirmingDelete = true }, trailing = null)
                }
            }
            CategoryPart.NAME -> {
                PartIntro(stringResource(R.string.category_name_look_info))
                // The same two choices as for every other element: a fill, and a border.
                val look = draft.nameLook()
                val setLook = { fill: Boolean, border: Boolean -> draft = draft.copy(nameStyle = CategoryNameStyle.of(fill, border).name) }
                SettingsPreferenceGroup(title = stringResource(R.string.settings_element_fill_label), info = stringResource(R.string.category_name_fill_info)) {
                    val swatch = androidx.compose.ui.graphics.SolidColor(categoryColor(draft))
                    ElementFillMode.entries.forEach { mode ->
                        FillModePreference(
                            value = mode,
                            selected = if (look.hasFill) ElementFillMode.COLOR else ElementFillMode.NONE,
                            onSelect = { setLook(it == ElementFillMode.COLOR, look.hasBorder) },
                            customFill = swatch
                        )
                    }
                }
                SettingsPreferenceGroup(title = stringResource(R.string.settings_element_border_label)) {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_element_border_label),
                        onClick = { setLook(look.hasFill, !look.hasBorder) },
                        trailing = { androidx.compose.material3.Switch(checked = look.hasBorder, onCheckedChange = { setLook(look.hasFill, it) }) }
                    )
                }
            }
            CategoryPart.TILE -> {
                // How it looks as a tile, like a folder's icon: only in the Dock, the Panel, boxes and folders.
                PartIntro(stringResource(R.string.settings_category_tile_info))
                TileFaceEditor(
                    face = draft.face(),
                    onChange = { draft = draft.copy(tileFace = it.toJson()) },
                    autoSurface = categoryColor(draft),
                    letter = draft.name,
                    content = shownMembers.take(4).map { member ->
                        if (member.type == GridItemType.APP) com.bisc.flauncher.ui.home.TileContent.App(ComponentName.unflattenFromString(member.payload))
                        else com.bisc.flauncher.ui.home.TileContent.Element(memberTypeIcon(member.type))
                    },
                    contentSummary = stringResource(R.string.tile_content_category),
                    viewModel = viewModel
                )
            }
            CategoryPart.CONTENT -> {
                // What it holds: by a rule, or picked by hand, and in which order.
                PartIntro(stringResource(if (rule != null) R.string.section_content_smart else R.string.section_content_own))
                if (rule != null) RuleSettings(draft) { draft = it }
                if (rule == null && shownMembers.size > 1) {
                    // Switching on keeps the order as it is shown now, ready to be dragged.
                    val setManual = { on: Boolean ->
                        if (on) draftMembers = shownMembers.map { it.type to it.payload }
                        draft = draft.copy(manualOrder = on)
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
                Column {
                    SettingsGroupLabel(stringResource(R.string.settings_category_members, shownMembers.size))
                    val removeMember = { member: CategoryMember -> draftMembers = draftMembers - (member.type to member.payload) }
                    if (rule == null && draft.manualOrder && shownMembers.size > 1) {
                        ReorderableList(
                            items = shownMembers,
                            key = { "${it.type}|${it.payload}" },
                            onReorder = { ordered -> draftMembers = ordered.map { it.type to it.payload } },
                            itemHeight = 64.dp,
                            dividers = true
                        ) { member ->
                            CategoryMemberRow(
                                member = member,
                                apps = apps,
                                websites = websites,
                                places = places,
                                contacts = contacts,
                                loadIcon = viewModel::loadIcon,
                                onRemove = { removeMember(member) },
                                inList = true
                            )
                        }
                    } else if (shownMembers.isEmpty()) {
                        SettingsPreferenceGroup {
                            SettingsPreference(
                                icon = null,
                                title = stringResource(if (rule != null) R.string.settings_category_smart_empty else R.string.settings_category_members_empty),
                                onClick = null,
                                trailing = null
                            )
                        }
                    } else {
                        SettingsPreferenceGroup {
                            shownMembers.forEach { member ->
                                CategoryMemberRow(
                                    member = member,
                                    apps = apps,
                                    websites = websites,
                                    places = places,
                                    contacts = contacts,
                                    loadIcon = viewModel::loadIcon,
                                    onRemove = if (rule == null) ({ removeMember(member) }) else null
                                )
                            }
                        }
                    }
                }
                if (rule == null) {
                    SettingsPreferenceGroup {
                        SettingsPreference(icon = Icons.Filled.Apps, title = stringResource(R.string.settings_category_pick_apps), onClick = { pickingApps = true })
                        SettingsPreference(
                            icon = Icons.Filled.Language,
                            title = stringResource(R.string.settings_category_pick_elements),
                            summary = stringResource(R.string.settings_category_pick_elements_summary),
                            onClick = { pickingElements = true }
                        )
                    }
                }
            }
        }
    }

    if (pickingApps) {
        val chosen = draftMembers.filter { it.first == GridItemType.APP }.map { it.second }.toSet()
        CategoryAppsPicker(
            title = saved.name,
            apps = apps,
            initial = chosen,
            loadIcon = viewModel::loadIcon,
            onDone = { picked ->
                // Everything kept stays where it was (an own order too); new apps come at the end, by name.
                val kept = draftMembers.filter { it.first != GridItemType.APP || it.second in picked }
                val added = picked.filter { it !in chosen }
                    .sortedBy { key -> apps.find { it.componentName.flattenToString() == key }?.label?.lowercase() }
                    .map { GridItemType.APP to it }
                draftMembers = kept + added
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
                draftMembers = if (on) (draftMembers - (type to payload)) + (type to payload) else draftMembers - (type to payload)
            },
            onDismiss = { pickingElements = false }
        )
    }
    if (confirmingDelete) {
        ConfirmRemoveDialog(
            label = saved.name,
            onConfirm = { viewModel.deleteCategory(saved); confirmingDelete = false; onBack() },
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
            Text(stringResource(R.string.settings_category_pick_elements), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
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
    if (tileFace != null && nameStyle != null && color != CATEGORY_NO_COLOR) this
    else copy(
        tileFace = face().toJson(),
        nameStyle = nameLook().name,
        color = color.takeIf { it != CATEGORY_NO_COLOR }
    )
