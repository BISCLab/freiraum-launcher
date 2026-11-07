package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.db.FolderItemDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.home.OnboardingViewModel
import com.bisc.flauncher.ui.home.PartScreensRow
import com.bisc.flauncher.ui.home.ShapePart
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.label
import com.bisc.flauncher.ui.theme.defaultBoxAppearanceFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where each part keeps its look: in its rows (Dock, Panel, widgets, element boxes) or under a name. */
private val NAMED_LOOKS = mapOf(
    ShapePart.FOLDERS to "folder_card",
    ShapePart.SEARCH_BUTTON to "app_list_search_button",
    ShapePart.ALPHABET_RAIL to "search_alphabet_rail",
    ShapePart.SEARCH_BAR to "search_field",
    ShapePart.SEARCH_RESULTS to "search_results",
    ShapePart.CATEGORY_RAIL to "search_category_rail"
)

@HiltViewModel
class PartColorsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val pageRowDao: PageRowDao,
    private val folderItemDao: FolderItemDao
) : ViewModel() {
    private val _looks = MutableStateFlow<Map<ShapePart, ElementAppearance>>(emptyMap())
    val looks: StateFlow<Map<ShapePart, ElementAppearance>> = _looks.asStateFlow()

    /** What each part looks like now; parts still on automatic show the default box of [dark]. */
    fun load(dark: Boolean) = viewModelScope.launch {
        val rows = pageRowDao.observeAll().first()
        val fallback = defaultBoxAppearanceFor(dark)
        fun row(vararg types: PageRowType) = rows.firstOrNull { it.type in types }?.appearance() ?: fallback
        val named = NAMED_LOOKS.mapValues { (_, key) -> settingsRepository.elementAppearanceOrNull(key) ?: fallback }
        _looks.value = mapOf(
            ShapePart.DOCK to row(PageRowType.SEARCH_BAR),
            ShapePart.PANEL to row(PageRowType.PANEL),
            ShapePart.WIDGETS to row(PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET),
            ShapePart.ELEMENT_BOXES to row(PageRowType.ELEMENT_BOX)
        ) + named
    }

    /** Writes the parts that changed. */
    fun save(edited: Map<ShapePart, ElementAppearance>) {
        val changed = edited.filter { (part, look) -> _looks.value[part] != look }
        if (changed.isEmpty()) return
        _looks.value = _looks.value + changed
        viewModelScope.launch {
            changed.forEach { (part, look) -> NAMED_LOOKS[part]?.let { settingsRepository.setElementAppearanceByName(it, look) } }
            pageRowDao.updateAll(pageRowDao.observeAll().first().map { row ->
                val look = when (row.type) {
                    PageRowType.SEARCH_BAR -> changed[ShapePart.DOCK]
                    PageRowType.PANEL -> changed[ShapePart.PANEL]
                    PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> changed[ShapePart.WIDGETS]
                    PageRowType.ELEMENT_BOX -> changed[ShapePart.ELEMENT_BOXES]
                    else -> null
                }
                if (look != null) row.withAppearance(look) else row
            })
            changed[ShapePart.WIDGETS]?.let { look ->
                folderItemDao.updateAll(folderItemDao.observeAll().first().map { item ->
                    if (item.type == GridItemType.WIDGET || item.type == GridItemType.SYSTEM_WIDGET) item.withAppearance(look) else item
                })
            }
        }
    }
}

/** A ready-made color set for every part at once. */
private enum class ColorPreset(val label: Int) { LIGHT(R.string.settings_colors_light), DARK(R.string.settings_colors_dark), GLASS(R.string.settings_colors_glass), ACCENT(R.string.settings_colors_accent) }

private fun ColorPreset.look(accent: Int): ElementAppearance = when (this) {
    ColorPreset.LIGHT -> ElementAppearance(fill = ColorFill(color = android.graphics.Color.WHITE), opacity = 0.85f, borderEnabled = true)
    ColorPreset.DARK -> ElementAppearance(fill = ColorFill(color = android.graphics.Color.BLACK), opacity = 0.8f, borderEnabled = true)
    ColorPreset.GLASS -> ElementAppearance(fill = ColorFill(color = android.graphics.Color.WHITE), opacity = 0.18f, borderEnabled = true, borderColor = 0x55FFFFFF)
    ColorPreset.ACCENT -> ElementAppearance(fill = ColorFill(color = accent), opacity = 0.9f, borderEnabled = false, contentColor = android.graphics.Color.WHITE)
}

/** Colors of every part on one page: the same small screens as for the corners, in the real colors,
 *  a color set for everything, and a table to change each part on its own. Saved together. */
@Composable
fun SettingsColorsScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: PartColorsViewModel = hiltViewModel(),
    cornersViewModel: OnboardingViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    LaunchedEffect(Unit) { viewModel.load(dark); cornersViewModel.loadCorners() }
    val saved by viewModel.looks.collectAsState()
    val corners by cornersViewModel.corners.collectAsState()
    val accent by settingsViewModel.accentColor.collectAsState()
    var draft by rememberDraft(saved)
    var focus by remember { mutableStateOf<ShapePart?>(null) }
    var editing by remember { mutableStateOf<ShapePart?>(null) }

    SettingsSubScreen(
        title = stringResource(R.string.settings_colors_all),
        navController = navController,
        onBack = onBack,
        onSave = { viewModel.save(draft) },
        dirty = draft != saved
    ) {
        Text(
            stringResource(R.string.settings_colors_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        PartScreensRow(corners, focus, draft)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ColorPreset.entries.forEach { preset ->
                val look = preset.look(accent)
                val colors = resolveElementColors(look)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { focus = null; draft = ShapePart.entries.associateWith { look } },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier.size(16.dp).clip(RoundedCornerShape(50))
                                .background(colors.background, alpha = colors.backgroundAlpha.coerceAtLeast(0.3f))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                        )
                        Text(stringResource(preset.label), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Text(stringResource(R.string.welcome_shape_single), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            ShapePart.entries.filter { it in draft }.forEachIndexed { i, part ->
                val look = draft.getValue(part)
                val colors = resolveElementColors(look)
                val focused = focus == part
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { focus = part; editing = part }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(part.label()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    // The part's fill with its text on it, like a small sample of it.
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .background(colors.background, alpha = colors.backgroundAlpha)
                            .then(if (look.borderEnabled) Modifier.border(1.dp, colors.borderColor, RoundedCornerShape(10.dp)) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aa", color = colors.contentColor, style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (i < draft.size - 1) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                }
            }
        }
    }

    editing?.let { part ->
        var partDraft by rememberDraft(draft.getValue(part))
        SettingsEditPage(
            title = stringResource(part.label()),
            onDismiss = { editing = null },
            onSave = { draft = draft + (part to partDraft) },
            dirty = partDraft != draft[part]
        ) {
            LookSample(
                when (part) {
                    ShapePart.DOCK -> LookSampleKind.DOCK
                    ShapePart.PANEL, ShapePart.WIDGETS, ShapePart.ELEMENT_BOXES -> LookSampleKind.PANEL
                    ShapePart.FOLDERS -> LookSampleKind.FOLDER
                    ShapePart.SEARCH_BAR, ShapePart.SEARCH_BUTTON -> LookSampleKind.FIELD
                    ShapePart.ALPHABET_RAIL, ShapePart.CATEGORY_RAIL -> LookSampleKind.RAIL
                    ShapePart.SEARCH_RESULTS -> LookSampleKind.CARDS
                },
                partDraft,
                corners[part] ?: 100
            )
            ElementAppearanceSection(appearance = partDraft, onAppearanceChange = { partDraft = it }, styleGroupTitle = stringResource(R.string.settings_element_fill_label))
        }
    }
}
