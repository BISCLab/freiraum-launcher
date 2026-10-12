package com.bisc.flauncher.data.repository

import android.content.Context
import com.bisc.flauncher.data.db.FolderItemDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.BackgroundSource
import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.DesignPreset
import com.bisc.flauncher.data.model.DesignPresetValues
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.util.BackgroundImageStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Applies a design preset (light or dark) to all surfaces as a single batch write. */
class PresetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val pageContentRepository: PageContentRepository,
    private val pageRowDao: PageRowDao,
    private val folderItemDao: FolderItemDao
) {
    suspend fun applyPreset(preset: DesignPreset) {
        val values = DesignPresetValues.of(preset)

        settingsRepository.setTheme(values.theme)
        settingsRepository.setAccentColor(values.accentColor)
        settingsRepository.setSidebarPanelAppearance(values.cardAppearance)
        settingsRepository.setFolderCardAppearance(values.cardAppearance)
        settingsRepository.setKeyboardAppearance(values.cardAppearance)
        settingsRepository.setSearchAppearance(values.cardAppearance)
        settingsRepository.setAppListContentAppearance(values.cardAppearance)
        // These three are "Layer 3, null means auto light/dark" overrides — a customization made under
        // one preset (e.g. a dark-mode-tuned dock search button) otherwise survives switching presets
        // untouched, silently breaking contrast against the new preset's background (e.g. a light icon
        // stuck on the new light dock). Applying a preset resets them back to automatic so the preset
        // is actually a coherent, fully-consistent look rather than "everything except whatever you'd
        // customized before".
        settingsRepository.resetSearchAlphabetRailAppearance()
        settingsRepository.resetAppListSearchButtonAppearance()

        // One shared global background now, not a same-color override stamped onto every page — clearing
        // every per-surface override here means they actually go back to tracking the global background
        // going forward (matching what "applying a coherent preset" should mean), instead of each still
        // carrying its own now-redundant copy of the same color.
        settingsRepository.setGlobalBackground(BackgroundConfig(source = BackgroundSource.COLOR, fill = ColorFill(color = values.backgroundColor)))
        settingsRepository.clearQuickOverviewBackground()
        pageContentRepository.allPages.first().forEach { page ->
            BackgroundImageStore.delete(context, "page_${page.id}")
            settingsRepository.clearPageBackgroundConfig(page.id)
        }

        val rows = pageRowDao.observeAll().first().map { row ->
            row.withAppearance(if (row.type == PageRowType.SEARCH_BAR) values.searchBarAppearance else values.rowAppearance)
        }
        pageRowDao.updateAll(rows)

        val items = folderItemDao.observeAll().first().map { it.withAppearance(values.rowAppearance) }
        folderItemDao.updateAll(items)

        settingsRepository.setActiveDesignPreset(preset)
    }
}
