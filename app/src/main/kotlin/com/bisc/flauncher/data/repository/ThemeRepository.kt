package com.bisc.flauncher.data.repository

import android.content.Context
import com.bisc.flauncher.data.db.FolderItemDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.model.BackgroundSource
import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.theme.backgroundColorFor
import com.bisc.flauncher.ui.theme.defaultBoxAppearanceFor
import com.bisc.flauncher.ui.theme.isDark
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** [reapplyTheme] (re)stamps every box/card/widget/dock/panel's default fill to match the currently
 *  active theme; backgrounds keep their pictures and colors (resolving SYSTEM against
 *  the live device setting), called once at first install, whenever the user switches
 *  Light/Dark/System in Settings, and whenever the device's own dark mode flips while SYSTEM is
 *  active (see MainActivity.onConfigurationChanged). */
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val pageContentRepository: PageContentRepository,
    private val pageRowDao: PageRowDao,
    private val folderItemDao: FolderItemDao,
    private val designRepository: com.bisc.flauncher.data.design.DesignRepository
) {
    suspend fun reapplyTheme() {
        val isDark = settingsRepository.theme.first().isDark(context)
        // A design with a light and a dark version (like Modern) switches to the other version
        // instead of falling back to plain defaults; the chosen mode (System too) stays.
        designRepository.counterpartFor(isDark)?.let { design ->
            designRepository.apply(design, keepTheme = true)
            return
        }
        val boxAppearance = defaultBoxAppearanceFor(isDark)

        // Backgrounds stay as they are; only the plain default color of the old mode swaps over.
        val global = settingsRepository.globalBackground.first()
        if (global.source == BackgroundSource.COLOR && global.fill.color in PLAIN_DEFAULTS) {
            settingsRepository.setGlobalBackground(global.copy(fill = ColorFill(color = backgroundColorFor(isDark))))
        }

        // "Layer 3, null means auto light/dark" overrides - a customization made under the old
        // theme otherwise survives the switch untouched, silently breaking contrast against the
        // new one (e.g. a light icon stuck on a now-light dock). Resetting them back to automatic
        // is what makes this a coherent, fully-consistent look rather than "everything except
        // whatever you'd customized before".
        settingsRepository.resetAppListRailAppearance()
        settingsRepository.resetAppListSearchButtonAppearance()
        settingsRepository.resetSearchResultsAppearance()
        settingsRepository.setFolderCardAppearance(boxAppearance)
        settingsRepository.setSearchAppearance(boxAppearance)

        val rows = pageRowDao.observeAll().first().map { it.withAppearance(boxAppearance) }
        pageRowDao.updateAll(rows)
        val items = folderItemDao.observeAll().first().map { it.withAppearance(boxAppearance) }
        folderItemDao.updateAll(items)
    }

    private companion object {
        val PLAIN_DEFAULTS = setOf(
            com.bisc.flauncher.ui.theme.MATTE_DARK_COLOR,
            com.bisc.flauncher.ui.theme.MATTE_LIGHT_COLOR,
            android.graphics.Color.WHITE
        )
    }
}
