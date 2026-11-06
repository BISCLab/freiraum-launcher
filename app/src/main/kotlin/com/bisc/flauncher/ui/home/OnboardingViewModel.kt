package com.bisc.flauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisc.flauncher.data.db.FolderItemDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.design.DesignRepository
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.theme.SettingsThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
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

/** The parts the welcome tour can shape one by one. */
enum class ShapePart { DOCK, WIDGETS, SEARCH_BUTTON, TEXT_SEARCH, FOLDERS }

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val designRepository: DesignRepository,
    private val pageRowDao: PageRowDao,
    private val folderItemDao: FolderItemDao
) : ViewModel() {

    /** The tour's light/dark switch: the settings and the tour follow it. */
    fun setTourDark(dark: Boolean) = viewModelScope.launch {
        settingsRepository.setSettingsThemeMode(if (dark) SettingsThemeMode.DARK else SettingsThemeMode.LIGHT)
    }

    /** Everything recommended in one go: the bundled Modern design in the chosen mode. */
    fun quickSetup(dark: Boolean, onDone: () -> Unit) = viewModelScope.launch {
        designRepository.refresh()
        designRepository.designs.value.firstOrNull { it.id == if (dark) "modern-dark" else "modern-light" }?.let { designRepository.apply(it) }
        onDone()
    }

    /** Sets the corners of every part to its preset; parts not in [shape] keep theirs. */
    fun applyShape(shape: Map<ShapePart, ShapePreset>) = viewModelScope.launch {
        shape[ShapePart.SEARCH_BUTTON]?.let { settingsRepository.setDefaultCornerRadiusPercent(it.elements) }
        shape[ShapePart.TEXT_SEARCH]?.let {
            settingsRepository.setSearchBarCornerRadiusPercent(it.elements)
            settingsRepository.setSearchFieldCornerRadiusPercent(it.elements)
            settingsRepository.setSearchResultsCornerRadiusPercent(it.elements)
        }
        pageRowDao.updateAll(pageRowDao.observeAll().first().map { row ->
            val corner = when (row.type) {
                PageRowType.SEARCH_BAR, PageRowType.PANEL -> shape[ShapePart.DOCK]?.elements
                PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> shape[ShapePart.WIDGETS]?.widgets
                else -> shape[ShapePart.FOLDERS]?.elements
            }
            if (corner != null) row.copy(cornerRadiusPercent = corner) else row
        })
        folderItemDao.updateAll(folderItemDao.observeAll().first().map { item ->
            val widget = item.type == GridItemType.WIDGET || item.type == GridItemType.SYSTEM_WIDGET
            val corner = if (widget) shape[ShapePart.WIDGETS]?.widgets else shape[ShapePart.FOLDERS]?.elements
            if (corner != null) item.copy(cornerRadiusPercent = corner) else item
        })
    }
}
