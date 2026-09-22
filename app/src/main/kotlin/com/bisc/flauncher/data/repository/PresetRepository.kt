package com.bisc.flauncher.data.repository

import android.content.Context
import com.bisc.flauncher.data.db.FolderItemDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.BackgroundSource
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
        settingsRepository.setSearchResultCardAppearance(values.cardAppearance)
        settingsRepository.setAppListContentAppearance(values.cardAppearance)

        pageContentRepository.allPages.first().forEach { page ->
            BackgroundImageStore.delete(context, "page_${page.id}")
            settingsRepository.setPageBackgroundConfig(page.id, BackgroundConfig(source = BackgroundSource.IMAGE, imagePath = values.wallpaper.path))
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
