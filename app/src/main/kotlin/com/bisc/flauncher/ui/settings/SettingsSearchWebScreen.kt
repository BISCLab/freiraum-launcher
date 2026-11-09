package com.bisc.flauncher.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.model.WebEngine
import com.bisc.flauncher.ui.home.search.CommandBadge
import com.bisc.flauncher.ui.home.search.CommandChoice
import com.bisc.flauncher.ui.home.search.CommandChoicesContent
import com.bisc.flauncher.ui.home.search.defaultColor
import com.bisc.flauncher.ui.home.search.rememberAiChoices
import com.bisc.flauncher.ui.home.search.rememberEngineChoices
import com.bisc.flauncher.util.SearchIconStore
import kotlinx.coroutines.launch

/** Which of the two command results a page is about. */
enum class CommandPart { WEB, AI }

/** The search engines (or the AI apps) of the Text search: several, in order, each with its own
 *  picture if wanted, and their result card as a preview that stays on top. */
@Composable
fun SettingsSearchWebScreen(navController: NavController, onBack: () -> Unit, part: CommandPart = CommandPart.WEB, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engines by viewModel.searchEngines.collectAsState()
    val aiApps by viewModel.searchAiApps.collectAsState()
    val categoryColors by viewModel.categoryColors.collectAsState()
    val installedApps by viewModel.browsableApps.collectAsState()
    val engineChoices = rememberEngineChoices(engines)
    val aiChoices = rememberAiChoices(aiApps, viewModel::loadIcon)
    val iconPacks by viewModel.iconPacks.collectAsState()
    var packIconFor by remember { mutableStateOf<CommandChoice?>(null) }
    var addingEngine by remember { mutableStateOf(false) }
    var editingEngine by remember { mutableStateOf<WebEngine?>(null) }
    var addingAi by remember { mutableStateOf(false) }
    var iconFor by remember { mutableStateOf<CommandChoice?>(null) }
    var pendingIconId by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = pendingIconId
        pendingIconId = null
        if (uri != null && id != null) scope.launch { SearchIconStore.save(context, id, uri) }
    }
    fun tint(category: SearchCategory) = categoryColors[category]?.let { Color(it) } ?: category.defaultColor

    val query = stringResource(R.string.settings_search_web_preview_query)
    val withEngine = stringResource(R.string.search_command_web_with)
    val askApp = stringResource(R.string.search_command_ai)
    val previewChoices = if (part == CommandPart.WEB) engineChoices else aiChoices
    SettingsSubScreen(
        title = stringResource(if (part == CommandPart.WEB) R.string.settings_search_web_engines else R.string.settings_search_web_ai),
        navController = navController,
        onBack = onBack,
        // The result card as the Text search shows it, on top while the list below scrolls.
        header = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHighest)))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (previewChoices.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_search_web_no_ai),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                } else {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
                        CommandChoicesContent(
                            previewChoices,
                            text = { c -> if (part == CommandPart.WEB) withEngine.format(query, c.label) else askApp.format(c.label, query) },
                            tint = tint(if (part == CommandPart.WEB) SearchCategory.WEB else SearchCategory.AI),
                            prominent = true,
                            onRun = {}
                        )
                    }
                }
            }
        }
    ) {
        Text(
            stringResource(if (part == CommandPart.WEB) R.string.settings_search_web_intro_web else R.string.settings_search_web_intro_ai),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        if (part == CommandPart.WEB) {
        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_search_web_engines),
            info = stringResource(R.string.settings_search_web_engines_info)
        ) {
            ReorderableList(
                items = engines,
                key = { it.id },
                onReorder = viewModel::setSearchEngines,
                itemHeight = 68.dp,
                shape = RectangleShape
            ) { engine ->
                val choice = engineChoices.firstOrNull { it.key == engine.id }
                ChoiceRow(
                    choice = choice,
                    subtitle = if (engine == engines.firstOrNull()) stringResource(R.string.settings_search_web_default) + " · " + engine.host else engine.host,
                    accent = engine == engines.firstOrNull(),
                    onIcon = { iconFor = choice },
                    onEdit = if (engine.isPreset) null else ({ editingEngine = engine }),
                    onRemove = if (engines.size > 1) ({
                        SearchIconStore.remove(context, SearchIconStore.engineId(engine.id))
                        viewModel.setSearchEngines(engines - engine)
                    }) else null
                )
            }
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_search_web_add_engine),
                onClick = { addingEngine = true }
            )
        }

        }
        if (part == CommandPart.AI) {
        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_search_web_ai),
            info = stringResource(R.string.settings_search_web_ai_info)
        ) {
            if (aiChoices.isEmpty()) {
                Text(
                    stringResource(R.string.settings_search_web_no_ai),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                ReorderableList(
                    items = aiChoices,
                    key = { it.key },
                    onReorder = { viewModel.setSearchAiApps(it.map { c -> c.key }) },
                    itemHeight = 68.dp,
                    shape = RectangleShape
                ) { choice ->
                    val first = choice == aiChoices.first()
                    ChoiceRow(
                        choice = choice,
                        subtitle = stringResource(if (first) R.string.settings_search_web_default else R.string.settings_search_web_choice),
                        accent = first,
                        onIcon = { iconFor = choice },
                        onEdit = null,
                        onRemove = {
                            SearchIconStore.remove(context, choice.iconId)
                            viewModel.setSearchAiApps(aiApps - choice.key)
                        }
                    )
                }
            }
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_search_web_add_ai),
                onClick = { addingAi = true }
            )
        }
        }
    }

    if (addingEngine) {
        val missing = WebEngine.presets.filter { p -> engines.none { it.id == p.id } }
        val missingChoices = rememberEngineChoices(missing)
        AlertDialog(
            onDismissRequest = { addingEngine = false },
            title = { Text(stringResource(R.string.settings_search_web_add_engine)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                    items(missingChoices, key = { it.key }) { choice ->
                        val engine = missing.first { it.id == choice.key }
                        PickRow(choice, engine.host) {
                            viewModel.setSearchEngines(engines + engine)
                            addingEngine = false
                        }
                    }
                    if (missing.isEmpty()) item {
                        Text(
                            stringResource(R.string.settings_search_web_all_added),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { addingEngine = false; editingEngine = WebEngine("", "", "") }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            Text(
                                stringResource(R.string.settings_search_web_custom_engine),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 14.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { addingEngine = false }) { Text(stringResource(R.string.widget_notes_cancel)) } }
        )
    }

    editingEngine?.let { engine ->
        var name by remember(engine) { mutableStateOf(engine.name) }
        var url by remember(engine) { mutableStateOf(engine.template) }
        AlertDialog(
            onDismissRequest = { editingEngine = null },
            title = { Text(stringResource(R.string.settings_search_web_custom_engine)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.settings_search_web_name)) },
                        singleLine = true,
                        shape = SettingsTextFieldShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.settings_search_web_url)) },
                        singleLine = true,
                        shape = SettingsTextFieldShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.settings_search_web_url_info), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank() && url.isNotBlank(),
                    onClick = {
                        val saved = engine.copy(
                            id = engine.id.ifBlank { "custom_" + System.currentTimeMillis() },
                            name = name.trim(),
                            template = url.trim()
                        )
                        viewModel.setSearchEngines(
                            if (engines.any { it.id == saved.id }) engines.map { if (it.id == saved.id) saved else it } else engines + saved
                        )
                        editingEngine = null
                    }
                ) { Text(stringResource(R.string.widget_notes_save)) }
            },
            dismissButton = { TextButton(onClick = { editingEngine = null }) { Text(stringResource(R.string.widget_notes_cancel)) } }
        )
    }

    if (addingAi) {
        val apps = remember(installedApps, aiApps) {
            installedApps.filter { it.componentName.packageName !in aiApps }.distinctBy { it.componentName.packageName }.sortedBy { it.label.lowercase() }
        }
        AlertDialog(
            onDismissRequest = { addingAi = false },
            title = { Text(stringResource(R.string.settings_search_web_add_ai)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                    items(apps, key = { it.componentName.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.setSearchAiApps(aiApps + app.componentName.packageName); addingAi = false }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = remember(app.componentName) { viewModel.loadIcon(app.componentName) }
                            if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(36.dp))
                            else Box(Modifier.size(36.dp))
                            Text(app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 14.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { addingAi = false }) { Text(stringResource(R.string.widget_notes_cancel)) } }
        )
    }

    iconFor?.let { choice ->
        AlertDialog(
            onDismissRequest = { iconFor = null },
            title = { Text(choice.label) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CommandBadge(choice, 76.dp)
                    Text(
                        stringResource(R.string.settings_search_web_icon_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                    )
                    IconSourceRow(Icons.Filled.Image, stringResource(R.string.settings_search_web_icon_pick)) {
                        pendingIconId = choice.iconId
                        iconFor = null
                        imagePicker.launch(arrayOf("image/*"))
                    }
                    if (iconPacks.isNotEmpty()) {
                        IconSourceRow(Icons.Filled.Apps, stringResource(R.string.settings_search_web_icon_pack)) {
                            iconFor = null
                            packIconFor = choice
                        }
                    }
                    if (choice.ownIcon) {
                        IconSourceRow(Icons.Filled.Restore, stringResource(R.string.settings_search_web_icon_reset)) {
                            SearchIconStore.remove(context, choice.iconId)
                            iconFor = null
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { iconFor = null }) { Text(stringResource(R.string.widget_notes_cancel)) } }
        )
    }

    packIconFor?.let { choice ->
        SettingsFixedDialog(
            title = choice.label,
            onDismiss = { packIconFor = null }
        ) {
            IconPackPickerContent(
                viewModel = viewModel,
                currentPack = null,
                currentDrawable = null,
                onPick = { pack, drawable ->
                    packIconFor = null
                    viewModel.loadPackIcon(pack, drawable)?.let { image -> scope.launch { SearchIconStore.save(context, choice.iconId, image) } }
                }
            )
        }
    }
}

/** One way to get a symbol: photo, icon pack, or back to the standard one. */
@Composable
private fun IconSourceRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 14.dp))
    }
}

/** An engine or app in its list: the symbol to tap for a picture, name, a line below, edit and remove. */
@Composable
private fun ChoiceRow(choice: CommandChoice?, subtitle: String, accent: Boolean, onIcon: () -> Unit, onEdit: (() -> Unit)?, onRemove: (() -> Unit)?) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.clip(CircleShape).clickable(onClick = onIcon).padding(4.dp)) {
            if (choice != null) CommandBadge(choice, 38.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(choice?.label.orEmpty(), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        onEdit?.let {
            IconButton(onClick = it, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
        onRemove?.let {
            IconButton(onClick = it, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PickRow(choice: CommandChoice, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CommandBadge(choice, 36.dp)
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(choice.label, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
