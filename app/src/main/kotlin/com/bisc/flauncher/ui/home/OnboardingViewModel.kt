package com.bisc.flauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisc.flauncher.data.db.FolderItemDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.design.DesignRepository
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.theme.DEFAULT_WIDGET_CORNER_RADIUS_PERCENT
import com.bisc.flauncher.ui.theme.SettingsThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Corner presets of the welcome tour: one value for elements, a smaller one for widgets, since
 *  round corners cut into a widget's content. */
enum class ShapePreset(val elements: Int, val widgets: Int) {
    ROUND(100, 20),
    SOFT(40, 12),
    SQUARE(10, 4)
}

/** Every part the welcome tour can shape on its own. */
enum class ShapePart(val isWidget: Boolean = false) {
    DOCK, PANEL, WIDGETS(isWidget = true), ELEMENT_BOXES, FOLDERS, SEARCH_BUTTON, ALPHABET_RAIL, SEARCH_BAR, SEARCH_RESULTS;

    fun percentOf(preset: ShapePreset) = if (isWidget) preset.widgets else preset.elements

    /** The preset closest to [percent]. */
    fun presetOf(percent: Int): ShapePreset = ShapePreset.entries.minBy { kotlin.math.abs(percentOf(it) - percent) }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val designRepository: DesignRepository,
    private val pageRowDao: PageRowDao,
    private val folderItemDao: FolderItemDao
) : ViewModel() {

    private val _corners = MutableStateFlow<Map<ShapePart, Int>>(emptyMap())
    /** What each part has now, so the previews start from the chosen design. */
    val corners: StateFlow<Map<ShapePart, Int>> = _corners.asStateFlow()

    /** The tour's light/dark switch: the settings and the tour follow it. */
    fun setTourDark(dark: Boolean) = viewModelScope.launch {
        settingsRepository.setSettingsThemeMode(if (dark) SettingsThemeMode.DARK else SettingsThemeMode.LIGHT)
    }

    /** Everything recommended in one go: the bundled Modern design in the chosen mode. */
    fun quickSetup(dark: Boolean, onDone: () -> Unit) = viewModelScope.launch {
        applyModern(dark)
        onDone()
    }

    /** A design is always chosen: Modern in the tour's mode until another one is picked. */
    fun ensureDesign(dark: Boolean) = viewModelScope.launch {
        if (settingsRepository.activeDesignId.first() == null) applyModern(dark)
    }

    private suspend fun applyModern(dark: Boolean) {
        designRepository.refresh()
        designRepository.designs.value.firstOrNull { it.id == if (dark) "modern-dark" else "modern-light" }?.let { designRepository.apply(it) }
    }

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
            ShapePart.FOLDERS to (items.firstOrNull { !it.isWidget() }?.cornerRadiusPercent ?: general),
            ShapePart.SEARCH_BUTTON to (settingsRepository.appListSearchButtonCornerPercent.first() ?: general),
            ShapePart.ALPHABET_RAIL to (settingsRepository.alphabetRailCornerPercent.first() ?: general),
            ShapePart.SEARCH_BAR to (settingsRepository.searchBarCornerRadiusPercent.first() ?: general),
            ShapePart.SEARCH_RESULTS to (settingsRepository.searchResultsCornerRadiusPercent.first() ?: general)
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
                // The other search fields (App list, App grid) follow the general roundness.
                settingsRepository.setDefaultCornerRadiusPercent(it)
                settingsRepository.setSearchBarCornerRadiusPercent(it)
                settingsRepository.setSearchFieldCornerRadiusPercent(it)
            }
            shape[ShapePart.SEARCH_RESULTS]?.let { settingsRepository.setSearchResultsCornerRadiusPercent(it) }
            pageRowDao.updateAll(pageRowDao.observeAll().first().map { row ->
                val corner = when (row.type) {
                    PageRowType.SEARCH_BAR -> shape[ShapePart.DOCK]
                    PageRowType.PANEL -> shape[ShapePart.PANEL]
                    PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> shape[ShapePart.WIDGETS]
                    else -> shape[ShapePart.ELEMENT_BOXES]
                }
                if (corner != null) row.copy(cornerRadiusPercent = corner) else row
            })
            folderItemDao.updateAll(folderItemDao.observeAll().first().map { item ->
                val corner = if (item.isWidget()) shape[ShapePart.WIDGETS] else shape[ShapePart.FOLDERS]
                if (corner != null) item.copy(cornerRadiusPercent = corner) else item
            })
        }
    }

    private fun com.bisc.flauncher.data.model.FolderItem.isWidget() = type == GridItemType.WIDGET || type == GridItemType.SYSTEM_WIDGET
}
