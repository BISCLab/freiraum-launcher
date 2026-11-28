package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.face
import com.bisc.flauncher.ui.home.FolderIcon
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.stackElement

/** What all folders share, as a draft: the window's look, corners and captions, and what goes
 *  away while one is open. */
internal class FolderTemplateDraft(
    val look: DraftField<ElementAppearance>,
    val corner: DraftField<Int>,
    val labels: DraftField<Boolean>,
    val hideStatus: DraftField<Boolean>,
    val hideDock: DraftField<Boolean>
) {
    val keys = listOf("all:look", "all:corner", "all:labels", "all:hide_status", "all:hide_dock")
}

@Composable
internal fun rememberFolderTemplateDraft(draft: StackDraft, viewModel: SettingsViewModel) = FolderTemplateDraft(
    look = draft.field("all:look", viewModel.folderCardAppearance.collectAsState().value, viewModel::setFolderCardAppearance),
    corner = draft.field("all:corner", viewModel.folderCornerPercent.collectAsState().value, viewModel::setFolderCornerPercent),
    labels = draft.field("all:labels", viewModel.folderShowLabels.collectAsState().value, viewModel::setFolderShowLabels),
    hideStatus = draft.field("all:hide_status", viewModel.folderHideStatusBar.collectAsState().value, viewModel::setFolderHideStatusBar),
    hideDock = draft.field("all:hide_dock", viewModel.folderHideDock.collectAsState().value, viewModel::setFolderHideDock)
)

private const val OPEN = "open"

/**
 * Every folder in one card stack, shown closed and opened: ⓪ "All folders" (the window every folder
 * without its own follows, and what goes away while one is open), then each folder with its four
 * tabs. Several folders can be changed and saved at once.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsFolderScreen(
    navController: NavController,
    onBack: () -> Unit,
    startElement: String? = null,
    startTab: Int = 0,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val home: HomeViewModel = hiltViewModel()
    val stack: StackDraft = viewModel()
    val folders by viewModel.folders.collectAsState()
    val folderItems by viewModel.folderItems.collectAsState()
    val browsableApps by viewModel.labeledApps.collectAsState()
    val all = rememberFolderTemplateDraft(stack, viewModel)
    var creating by remember { mutableStateOf(false) }
    val itemsByFolder = remember(folderItems) { folderItems.groupBy { it.folderId } }
    // Folders inside other folders show inside those; from outside they still open here.
    val nested = remember(folderItems) {
        folderItems.filter { it.type == GridItemType.FOLDER }.mapNotNull { it.payload.toLongOrNull() }.toSet()
    }
    val following = folders.count { !it.appearanceOverrideEnabled && !it.borderOverrideEnabled }
    val storedLook by viewModel.folderCardAppearance.collectAsState()
    val storedLabels by viewModel.folderShowLabels.collectAsState()
    fun name(f: Folder) = DraftField(stack, folderKey(f.id) + ":name", f.name).value
    fun face(f: Folder) = DraftField(stack, folderKey(f.id) + ":face", f.face()).value
    fun window(f: Folder) = DraftField(stack, folderKey(f.id) + ":window", f.windowEdit(storedLook, storedLabels)).value

    val elements = buildList {
        add(StackElement(
            id = "all", number = 0,
            title = stringResource(R.string.stack_el_all_folders),
            summary = pluralStringResource(R.plurals.settings_folder_template_following, following, following, folders.size),
            draftKeys = all.keys,
            state = OPEN,
            sample = { LookSample(LookSampleKind.FOLDER, all.look.value, all.corner.value) }
        ) {
            PartIntro(stringResource(R.string.settings_folder_global_intro))
            StackTabs(listOf(
                stringResource(R.string.stack_el_folder_window) to {
                    ElementAppearanceSection(appearance = all.look.value, onAppearanceChange = { all.look.value = it }, styleGroupTitle = stringResource(R.string.settings_element_fill_label))
                    SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
                        CornerPercentPicker(percent = all.corner.value, onChange = { all.corner.value = it })
                    }
                    CaptionsSection(
                        showLabels = all.labels.value,
                        onShowLabelsChange = { all.labels.value = it },
                        appearance = all.look.value,
                        onAppearanceChange = { all.look.value = it },
                        title = stringResource(R.string.settings_captions_label)
                    )
                },
                stringResource(R.string.settings_folder_while_open) to {
                    SettingsPreferenceGroup {
                        DraftSwitchRow(stringResource(R.string.settings_folder_hide_status_bar), all.hideStatus.value, { all.hideStatus.value = it })
                        DraftSwitchRow(stringResource(R.string.settings_folder_hide_dock), all.hideDock.value, { all.hideDock.value = it }, info = stringResource(R.string.settings_folder_hide_dock_summary))
                    }
                }
            ))
        })
        folders.forEachIndexed { i, f ->
            val items = itemsByFolder[f.id].orEmpty()
            add(StackElement(
                id = folderKey(f.id), number = i + 1,
                title = name(f).ifBlank { f.name },
                summary = pluralStringResource(R.plurals.hub_content_count, items.size, items.size),
                inList = f.id !in nested,
                draftKeys = listOf(folderKey(f.id) + ":name", folderKey(f.id) + ":face", folderKey(f.id) + ":window"),
                anchor = androidx.compose.ui.geometry.Offset(1f, 0f),
                leading = { FolderIcon(folder = f, previewItems = items, modifier = Modifier.size(32.dp), face = face(f), around = MaterialTheme.colorScheme.onSurface) }
            ) { FolderEditor(f, stack, all, if (folderKey(f.id) == startElement) startTab else 0, navController, viewModel, home) })
        }
    }

    val global by home.globalBackground.collectAsState()
    val homeItems by home.folderItemsByFolder.collectAsState()
    val mapTargets by home.savedMapTargets.collectAsState()
    val websites by home.pinnedWebsites.collectAsState()
    val iconScale by home.iconSizeScale.collectAsState()
    val dark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), stack.shownBackground(global))
    CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalSurfaceIsDark provides dark) {
        SettingsCardStackScreen(
            title = stringResource(R.string.settings_folder_title),
            navController = navController,
            onBack = onBack,
            draft = stack,
            elements = elements,
            states = listOf("closed" to stringResource(R.string.stack_state_closed), OPEN to stringResource(R.string.stack_state_open)),
            startElement = startElement,
            firstCard = StackCard.ELEMENTS,
            listHeader = { StackAddRow(stringResource(R.string.settings_folder_create)) { creating = true } }
        ) { state, _ ->
            val ink = if (dark) Color.White else Color.Black
            val statusTop = replicaStatusHeight()
            val selected = folders.find { folderKey(it.id) == LocalStackSelected.current }
            CompositionLocalProvider(
                com.bisc.flauncher.ui.home.LocalAppIconLoader provides home.iconLoader,
                com.bisc.flauncher.ui.home.widgets.LocalHostAppWidgets provides false
            ) {
                Box(Modifier.fillMaxSize()) {
                    com.bisc.flauncher.ui.home.dock.BackgroundFill(stack.shownBackground(global), Modifier.fillMaxSize())
                    @Composable
                    fun Closed(f: Folder) {
                        Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            FolderIcon(folder = f, previewItems = itemsByFolder[f.id].orEmpty(), modifier = Modifier.size(56.dp * iconScale).stackElement(folderKey(f.id)), face = face(f), around = ink)
                            Spacer(Modifier.height(8.dp))
                            Text(name(f), color = ink, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    // Opened: the chosen folder, or for all folders the first, in their look.
                    val shown = selected ?: folders.firstOrNull { it.id !in nested } ?: folders.firstOrNull()
                    if (state == OPEN && shown != null) {
                        val w = window(shown)
                        val own = selected != null && w.own
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
                        com.bisc.flauncher.ui.home.FolderView(
                            folder = shown,
                            items = homeItems[shown.id] ?: itemsByFolder[shown.id].orEmpty(),
                            folders = folders,
                            folderItemsByFolder = homeItems,
                            savedMapTargets = mapTargets,
                            pinnedWebsites = websites,
                            showLabels = if (selected != null) w.labels else all.labels.value,
                            loadIcon = home::loadIcon,
                            loadPackIcon = home::loadPackIcon,
                            resolveLabel = home::resolveLabel,
                            onAppClick = {},
                            onFolderClick = {},
                            onRemoveItem = {},
                            cornerPercent = if (own) w.corner ?: all.corner.value else all.corner.value,
                            onBack = {},
                            cardAppearance = if (own) w.look else all.look.value,
                            handlesBack = false
                        )
                    } else if (selected != null) {
                        Box(Modifier.align(Alignment.Center)) { Closed(selected) }
                    } else {
                        // All folders, as they would lie on a page.
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = statusTop + 48.dp, start = 12.dp, end = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            maxItemsInEachRow = 4
                        ) { folders.filter { it.id !in nested }.take(16).forEach { Closed(it) } }
                    }
                    if (!(state == OPEN && all.hideStatus.value)) StatusBarSketch(statusTop, ink)
                }
            }
        }
    }

    if (creating) {
        FolderCreatorSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onCreate = { n, apps ->
                creating = false
                // Straight on to the new folder, to arrange it.
                viewModel.createFolderWithApps(n, apps) { id -> navController.navigate("settings/folder/$id") }
            },
            onDismiss = { creating = false }
        )
    }
}

/** The first tile of an overview of tiles: a plus, to make a new one. */
@Composable
internal fun NewItemTile(label: String, modifier: Modifier, lines: Int = 1, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(10.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        // As tall as the tiles beside it.
        repeat(lines) { Text(" ", style = MaterialTheme.typography.labelSmall) }
    }
}

/** One folder, the way in from outside: the stack, opened on it and on the tab asked for. */
@Composable
fun SettingsFolderEditScreen(folderId: Long, navController: NavController, onBack: () -> Unit, startElement: String? = null) {
    val tab = when (startElement) {
        "icon" -> 1
        "window" -> 2
        "content" -> 3
        else -> 0
    }
    SettingsFolderScreen(navController, onBack, startElement = folderKey(folderId), startTab = tab)
}
