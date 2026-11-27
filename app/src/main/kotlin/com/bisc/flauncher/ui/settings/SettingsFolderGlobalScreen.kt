package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.ui.home.HomeViewModel

/**
 * The look every folder follows unless it has its own, as a card stack: ⓪ what happens while a
 * folder is open, ① the folder window. The replica opens the first folder in this look.
 */
@Composable
fun SettingsFolderGlobalScreen(
    navController: NavController,
    onBack: () -> Unit,
    startElement: String? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
    home: HomeViewModel = hiltViewModel()
) {
    val draft: StackDraft = viewModel()
    val look = draft.field("look", viewModel.folderCardAppearance.collectAsState().value, viewModel::setFolderCardAppearance)
    val corner = draft.field("corner", viewModel.folderCornerPercent.collectAsState().value, viewModel::setFolderCornerPercent)
    val labels = draft.field("labels", viewModel.folderShowLabels.collectAsState().value, viewModel::setFolderShowLabels)
    val hideStatus = draft.field("hide_status", viewModel.folderHideStatusBar.collectAsState().value, viewModel::setFolderHideStatusBar)
    val hideDock = draft.field("hide_dock", viewModel.folderHideDock.collectAsState().value, viewModel::setFolderHideDock)
    val folders by viewModel.folders.collectAsState()
    val following = folders.count { !it.appearanceOverrideEnabled && !it.borderOverrideEnabled }

    val elements = listOf(
        StackElement(
            id = "page", number = 0,
            title = stringResource(R.string.stack_el_all_folders),
            summary = pluralStringResource(R.plurals.settings_folder_template_following, following, following, folders.size),
            draftKeys = listOf("hide_status", "hide_dock")
        ) {
            PartIntro(stringResource(R.string.settings_folder_global_intro))
            SettingsPreferenceGroup(title = stringResource(R.string.settings_folder_while_open)) {
                DraftSwitchRow(stringResource(R.string.settings_folder_hide_status_bar), hideStatus.value, { hideStatus.value = it })
                DraftSwitchRow(stringResource(R.string.settings_folder_hide_dock), hideDock.value, { hideDock.value = it }, info = stringResource(R.string.settings_folder_hide_dock_summary))
            }
        },
        StackElement(
            id = "window", number = 1,
            title = stringResource(R.string.stack_el_folder_window),
            draftKeys = listOf("look", "corner", "labels"),
            anchor = Offset(0f, 0f),
            sample = { LookSample(LookSampleKind.FOLDER, look.value, corner.value) }
        ) {
            ElementAppearanceSection(appearance = look.value, onAppearanceChange = { look.value = it }, styleGroupTitle = stringResource(R.string.settings_element_fill_label))
            SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
                CornerPercentPicker(percent = corner.value, onChange = { corner.value = it })
            }
            CaptionsSection(
                showLabels = labels.value,
                onShowLabelsChange = { labels.value = it },
                appearance = look.value,
                onAppearanceChange = { look.value = it },
                title = stringResource(R.string.settings_captions_label)
            )
        }
    )

    val global by home.globalBackground.collectAsState()
    val items by home.folderItemsByFolder.collectAsState()
    val mapTargets by home.savedMapTargets.collectAsState()
    val websites by home.pinnedWebsites.collectAsState()
    // A folder at the top level, shown in this look.
    val nested = items.values.flatten().filter { it.type == GridItemType.FOLDER }.mapNotNull { it.payload.toLongOrNull() }.toSet()
    val sample = folders.firstOrNull { it.id !in nested } ?: folders.firstOrNull()
    val dark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), global)
    CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalSurfaceIsDark provides dark) {
        SettingsCardStackScreen(
            title = stringResource(R.string.settings_folder_global_label),
            navController = navController,
            onBack = onBack,
            draft = draft,
            elements = elements,
            startElement = startElement
        ) { _, _ ->
            val statusTop = replicaStatusHeight()
            CompositionLocalProvider(
                com.bisc.flauncher.ui.home.LocalAppIconLoader provides home.iconLoader,
                com.bisc.flauncher.ui.home.widgets.LocalHostAppWidgets provides false
            ) {
                Box(Modifier.fillMaxSize()) {
                    com.bisc.flauncher.ui.home.dock.BackgroundFill(global, Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
                    if (sample != null) {
                        com.bisc.flauncher.ui.home.FolderView(
                            folder = sample,
                            items = items[sample.id].orEmpty(),
                            folders = folders,
                            folderItemsByFolder = items,
                            savedMapTargets = mapTargets,
                            pinnedWebsites = websites,
                            showLabels = labels.value,
                            loadIcon = home::loadIcon,
                            loadPackIcon = home::loadPackIcon,
                            resolveLabel = home::resolveLabel,
                            onAppClick = {},
                            onFolderClick = {},
                            onRemoveItem = {},
                            cornerPercent = corner.value,
                            onBack = {},
                            cardAppearance = look.value,
                            handlesBack = false
                        )
                    } else {
                        Text(
                            stringResource(R.string.stack_all_folders_no_sample),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.Center).padding(32.dp)
                        )
                    }
                    if (!hideStatus.value) StatusBarSketch(statusTop, Color.White)
                }
            }
        }
    }
}
