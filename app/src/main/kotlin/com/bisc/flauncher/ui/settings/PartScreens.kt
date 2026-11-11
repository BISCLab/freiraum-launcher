package com.bisc.flauncher.ui.settings

import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisc.flauncher.R
import com.bisc.flauncher.data.db.FolderItemDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.theme.DEFAULT_WIDGET_CORNER_RADIUS_PERCENT
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Corner presets for every part: one value for elements, a smaller one for widgets, since
 *  round corners cut into a widget's content. */
enum class ShapePreset(val elements: Int, val widgets: Int) {
    ROUND(100, 80),
    SOFT(45, 45),
    SQUARE(12, 12)
}

/** Every part that can get its own corners and colors. */
enum class ShapePart(val isWidget: Boolean = false) {
    DOCK, PANEL, WIDGETS(isWidget = true), WIDGET_ARROWS, ELEMENT_BOXES, FOLDERS, SEARCH_BAR, SEARCH_BUTTON, ALPHABET_RAIL, SEARCH_RESULTS, CATEGORY_RAIL, KEYBOARD_STRIP, MENUS;

    fun percentOf(preset: ShapePreset) = if (isWidget) preset.widgets else preset.elements

    /** The preset closest to [percent]. */
    fun presetOf(percent: Int): ShapePreset = ShapePreset.entries.minBy { kotlin.math.abs(percentOf(it) - percent) }
}

/** The corners of every part: what each has now, one preset for all, or one part at a time. Used
 *  by the welcome tour (applies at once) and by Appearance → Corners of all parts (saves on demand). */
@HiltViewModel
class PartCornersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val pageRowDao: PageRowDao,
    private val folderItemDao: FolderItemDao
) : ViewModel() {

    private val _corners = MutableStateFlow<Map<ShapePart, Int>>(emptyMap())
    /** What each part has now, so the previews start from the chosen design. */
    val corners: StateFlow<Map<ShapePart, Int>> = _corners.asStateFlow()

    fun loadCorners() = viewModelScope.launch {
        val general = settingsRepository.defaultCornerRadiusPercent.first()
        val rows = pageRowDao.observeAll().first()
        val items = folderItemDao.observeAll().first()
        fun row(vararg types: PageRowType) = rows.firstOrNull { it.type in types }?.cornerRadiusPercent
        _corners.value = mapOf(
            ShapePart.DOCK to (row(PageRowType.SEARCH_BAR) ?: general),
            ShapePart.PANEL to (row(PageRowType.PANEL) ?: general),
            ShapePart.WIDGETS to (row(PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET) ?: DEFAULT_WIDGET_CORNER_RADIUS_PERCENT),
            ShapePart.ELEMENT_BOXES to (row(PageRowType.ELEMENT_BOX) ?: general),
            ShapePart.FOLDERS to settingsRepository.folderCornerPercent.first(),
            ShapePart.SEARCH_BUTTON to (settingsRepository.appListSearchButtonCornerPercent.first() ?: general),
            ShapePart.ALPHABET_RAIL to (settingsRepository.alphabetRailCornerPercent.first() ?: general),
            ShapePart.SEARCH_BAR to (settingsRepository.searchFieldCornerRadiusPercent.first() ?: settingsRepository.searchBarCornerRadiusPercent.first() ?: 100),
            ShapePart.SEARCH_RESULTS to (settingsRepository.searchResultsCornerRadiusPercent.first() ?: general),
            ShapePart.CATEGORY_RAIL to (settingsRepository.searchRailCornerRadiusPercent.first()
                ?: settingsRepository.alphabetRailCornerPercent.first() ?: general),
            ShapePart.WIDGET_ARROWS to settingsRepository.widgetPaging.first().cornerRadiusPercent,
            ShapePart.KEYBOARD_STRIP to settingsRepository.searchKeyboardCapCornerRadiusPercent.first(),
            ShapePart.MENUS to (settingsRepository.menuCornerPercent.first() ?: com.bisc.flauncher.ui.home.DEFAULT_MENU_CORNER)
        )
    }

    /** One preset for every part. */
    fun applyAll(preset: ShapePreset) = apply(ShapePart.entries.associateWith { it.percentOf(preset) })

    fun applyPart(part: ShapePart, preset: ShapePreset) = apply(mapOf(part to part.percentOf(preset)))

    /** Saves what was edited in the settings; only parts that changed are written. */
    fun saveCorners(edited: Map<ShapePart, Int>) = apply(edited.filter { (part, percent) -> _corners.value[part] != percent })

    private fun apply(shape: Map<ShapePart, Int>) {
        _corners.value = _corners.value + shape
        viewModelScope.launch {
            shape[ShapePart.SEARCH_BUTTON]?.let { settingsRepository.setAppListSearchButtonCornerPercent(it) }
            shape[ShapePart.ALPHABET_RAIL]?.let { settingsRepository.setAlphabetRailCornerPercent(it) }
            shape[ShapePart.SEARCH_BAR]?.let {
                // Every search field: Text search, App list and App grid.
                settingsRepository.setAppListFieldCornerPercent(it)
                settingsRepository.setAppGridFieldCornerPercent(it)
                settingsRepository.setSearchBarCornerRadiusPercent(it)
                settingsRepository.setSearchFieldCornerRadiusPercent(it)
            }
            shape[ShapePart.SEARCH_RESULTS]?.let { settingsRepository.setSearchResultsCornerRadiusPercent(it) }
            shape[ShapePart.CATEGORY_RAIL]?.let { settingsRepository.setSearchRailCornerRadiusPercent(it) }
            shape[ShapePart.WIDGET_ARROWS]?.let { corner ->
                settingsRepository.setWidgetPaging(settingsRepository.widgetPaging.first().copy(cornerRadiusPercent = corner))
            }
            shape[ShapePart.KEYBOARD_STRIP]?.let { settingsRepository.setSearchKeyboardCapCornerRadiusPercent(it) }
            shape[ShapePart.MENUS]?.let { settingsRepository.setMenuCornerPercent(it) }
            pageRowDao.updateAll(pageRowDao.observeAll().first().map { row ->
                val corner = when (row.type) {
                    PageRowType.SEARCH_BAR -> shape[ShapePart.DOCK]
                    PageRowType.PANEL -> shape[ShapePart.PANEL]
                    PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> shape[ShapePart.WIDGETS]
                    else -> shape[ShapePart.ELEMENT_BOXES]
                }
                if (corner != null) row.copy(cornerRadiusPercent = corner) else row
            })
            shape[ShapePart.FOLDERS]?.let { settingsRepository.setFolderCornerPercent(it) }
            shape[ShapePart.WIDGETS]?.let { corner ->
                folderItemDao.updateAll(folderItemDao.observeAll().first().filter { it.isWidget() }.map { it.copy(cornerRadiusPercent = corner) })
            }
        }
    }

    private fun com.bisc.flauncher.data.model.FolderItem.isWidget() = type == GridItemType.WIDGET || type == GridItemType.SYSTEM_WIDGET
}

private val SHAPE_SCREEN_GAP = 10.dp

/** The small screens side by side; exactly three fit, like the page tiles, the rest are a swipe away. */
@Composable
internal fun PartScreensRow(
    corners: Map<ShapePart, Int>,
    focus: ShapePart?,
    looks: Map<ShapePart, com.bisc.flauncher.data.model.ElementAppearance>? = null
) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val screenWidth = (maxWidth - SHAPE_SCREEN_GAP * 2) / 3
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SHAPE_SCREEN_GAP)
        ) {
            ShapeScreen.entries.forEach { screen ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(screenWidth)) {
                    MockScreen(screen, corners, focus, screenWidth, looks)
                    Text(stringResource(screen.label), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

/** The screens the shape previews show, each with the parts that live on it. */
internal enum class ShapeScreen(val label: Int) {
    HOME(R.string.welcome_screen_home),
    PANEL(R.string.welcome_screen_panel),
    APP_LIST(R.string.welcome_screen_app_list),
    SEARCH(R.string.welcome_screen_search),
    FOLDER(R.string.welcome_screen_folder)
}

/** The shape previews, the choice for all and the table; [onAll] and [onPart] decide whether a
 *  change applies at once (welcome tour) or waits for Save (settings). */
@Composable
internal fun ShapeEditor(corners: Map<ShapePart, Int>, onAll: (ShapePreset) -> Unit, onPart: (ShapePart, ShapePreset) -> Unit, showScreens: Boolean = true) {
    var focus by remember { mutableStateOf<ShapePart?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        if (showScreens) PartScreensRow(corners, focus)
        val all = ShapePreset.entries.firstOrNull { preset ->
            corners.isNotEmpty() && ShapePart.entries.all { corners[it] == it.percentOf(preset) }
        }
        Segmented(
            options = ShapePreset.entries,
            label = { stringResource(it.label()) },
            selected = all,
            onSelect = { focus = null; onAll(it) },
            modifier = Modifier.fillMaxWidth(),
            large = true
        )
        Text(
            stringResource(R.string.welcome_shape_single),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            ShapePart.entries.forEachIndexed { i, part ->
                val focused = focus == part
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { focus = part }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(part.label()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Segmented(
                        options = ShapePreset.entries,
                        label = { stringResource(it.label()) },
                        selected = corners[part]?.let(part::presetOf),
                        onSelect = { focus = part; onPart(part, it) }
                    )
                }
                if (i < ShapePart.entries.lastIndex) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                }
            }
        }
    }
}

/** A pill of choices, the chosen one filled with the accent. */
@Composable
private fun <T> Segmented(
    options: List<T>,
    label: @Composable (T) -> String,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(3.dp)
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                modifier = Modifier
                    .then(if (large) Modifier.weight(1f) else Modifier)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = if (large) 8.dp else 10.dp, vertical = if (large) 10.dp else 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option),
                    style = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun ShapePreset.label() = when (this) {
    ShapePreset.ROUND -> R.string.welcome_shape_round
    ShapePreset.SOFT -> R.string.welcome_shape_soft
    ShapePreset.SQUARE -> R.string.welcome_shape_square
}

internal fun ShapePart.label() = when (this) {
    ShapePart.DOCK -> R.string.welcome_shape_part_dock
    ShapePart.PANEL -> R.string.welcome_shape_part_panel
    ShapePart.WIDGETS -> R.string.welcome_shape_part_widgets
    ShapePart.ELEMENT_BOXES -> R.string.welcome_shape_part_element_boxes
    ShapePart.FOLDERS -> R.string.welcome_shape_part_folders
    ShapePart.SEARCH_BUTTON -> R.string.welcome_shape_part_search_button
    ShapePart.ALPHABET_RAIL -> R.string.welcome_shape_part_alphabet_rail
    ShapePart.SEARCH_BAR -> R.string.welcome_shape_part_search_bar
    ShapePart.SEARCH_RESULTS -> R.string.welcome_shape_part_search_results
    ShapePart.CATEGORY_RAIL -> R.string.welcome_shape_part_category_rail
    ShapePart.WIDGET_ARROWS -> R.string.welcome_shape_part_widget_arrows
    ShapePart.KEYBOARD_STRIP -> R.string.welcome_shape_part_keyboard_strip
    ShapePart.MENUS -> R.string.settings_menus_title
}

/** One screen in small with its parts drawn at their corners; the part picked in the table glows
 *  in the accent so it's clear which one it is. */
@Composable
internal fun MockScreen(
    screen: ShapeScreen,
    corners: Map<ShapePart, Int>,
    focus: ShapePart?,
    width: androidx.compose.ui.unit.Dp,
    /** The parts' real looks; null = plain shapes, as in the corners editor. */
    looks: Map<ShapePart, com.bisc.flauncher.data.model.ElementAppearance>? = null
) {
    val frame = RoundedCornerShape(18.dp)
    val neutral = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val accent = MaterialTheme.colorScheme.primary
    @Composable
    fun Part(part: ShapePart, modifier: Modifier, text: String? = null, content: @Composable BoxScope.() -> Unit = {}) {
        val shape = com.bisc.flauncher.ui.theme.PartCornerShape(corners[part] ?: 50, full = 9.dp)
        val look = looks?.get(part)
        val colors = look?.let { com.bisc.flauncher.ui.home.dock.resolveElementColors(it) }
        Box(
            modifier = modifier
                .clip(shape)
                .then(
                    if (colors != null) Modifier.background(colors.background, alpha = colors.backgroundAlpha)
                    else Modifier.background(if (focus == part) accent.copy(alpha = 0.55f) else neutral)
                )
                .then(if (colors != null && look.borderEnabled) Modifier.border(1.dp, colors.borderColor, shape) else Modifier)
                .then(if (focus == part) Modifier.border(2.dp, accent, shape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            content()
            if (text != null) Text(text, fontSize = 8.sp, maxLines = 1, color = colors?.contentColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        }
    }
    @Composable
    fun Dots(count: Int, size: androidx.compose.ui.unit.Dp = 9.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(count) { Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))) }
        }
    }
    Box(
        modifier = Modifier
            .width(width)
            .aspectRatio(0.5f)
            .clip(frame)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(2.dp, MaterialTheme.colorScheme.outline, frame)
            .padding(8.dp)
    ) {
        when (screen) {
            ShapeScreen.HOME -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(Modifier.height(10.dp))
                Part(ShapePart.WIDGETS, Modifier.fillMaxWidth().height(52.dp), stringResource(R.string.welcome_shape_part_widgets))
                Part(ShapePart.ELEMENT_BOXES, Modifier.fillMaxWidth().height(34.dp)) { Dots(4) }
                Spacer(Modifier.weight(1f))
                Part(ShapePart.DOCK, Modifier.fillMaxWidth().height(28.dp), stringResource(R.string.welcome_shape_part_dock))
            }
            ShapeScreen.PANEL -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Part(ShapePart.PANEL, Modifier.fillMaxWidth().height(96.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.welcome_shape_part_panel), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                        Dots(4)
                        Dots(4)
                    }
                }
                Spacer(Modifier.weight(1f))
                Part(ShapePart.DOCK, Modifier.fillMaxWidth().height(28.dp), stringResource(R.string.welcome_shape_part_dock))
            }
            ShapeScreen.APP_LIST -> {
                Column(Modifier.fillMaxSize().padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Part(ShapePart.SEARCH_BAR, Modifier.fillMaxWidth().height(22.dp), stringResource(R.string.welcome_mock_search))
                    repeat(7) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)))
                            Box(Modifier.width(44.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)))
                        }
                    }
                }
                Part(ShapePart.SEARCH_BUTTON, Modifier.align(Alignment.BottomEnd).size(26.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                Part(ShapePart.ALPHABET_RAIL, Modifier.align(Alignment.CenterEnd).width(12.dp).fillMaxHeight(0.72f)) {
                    Text("A\nZ", fontSize = 7.sp, lineHeight = 8.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            ShapeScreen.SEARCH -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Part(ShapePart.SEARCH_RESULTS, Modifier.weight(1f).height(110.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.welcome_shape_part_search_results), fontSize = 8.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                        Dots(3)
                        Dots(3)
                    }
                }
                Part(ShapePart.CATEGORY_RAIL, Modifier.width(12.dp).height(110.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(4) { Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))) }
                    }
                }
                }
                Spacer(Modifier.weight(1f))
                Part(ShapePart.SEARCH_BAR, Modifier.fillMaxWidth().height(26.dp), stringResource(R.string.welcome_mock_search))
            }
            ShapeScreen.FOLDER -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // The folder card itself is the part; apps inside are plain icons.
                Part(ShapePart.FOLDERS, Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Part(ShapePart.WIDGETS, Modifier.fillMaxWidth().height(40.dp), stringResource(R.string.welcome_shape_part_widgets))
                        repeat(2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                repeat(3) {
                                    Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

