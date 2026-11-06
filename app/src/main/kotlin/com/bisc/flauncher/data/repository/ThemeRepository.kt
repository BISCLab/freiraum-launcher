package com.bisc.flauncher.data.repository

import android.content.Context
import com.bisc.flauncher.data.db.FolderItemDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.model.BackgroundConfig
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

/** Light/Dark/System is the only appearance control FLauncher has - there is no separate "design
 *  preset" layered on top of it. [reapplyTheme] (re)stamps the background and every box/card/
 *  widget/dock/panel's default fill to match the currently active theme (resolving SYSTEM against
 *  the live device setting), called once at first install, whenever the user switches
 *  Light/Dark/System in Settings, and whenever the device's own dark mode flips while SYSTEM is
 *  active (see MainActivity.onConfigurationChanged). */
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val pageContentRepository: PageContentRepository,
    private val pageRowDao: PageRowDao,
    private val folderItemDao: FolderItemDao
) {
    suspend fun reapplyTheme() {
        val isDark = settingsRepository.theme.first().isDark(context)
        val boxAppearance = defaultBoxAppearanceFor(isDark)

        // Plain backgrounds follow the mode; pictures (from a wallpaper pack or your own) stay.
        // A page with a plain color of its own goes back to the shared global background.
        if (settingsRepository.globalBackground.first().source != BackgroundSource.IMAGE) {
            settingsRepository.setGlobalBackground(BackgroundConfig(source = BackgroundSource.COLOR, fill = ColorFill(color = backgroundColorFor(isDark))))
        }
        pageContentRepository.allPages.first().forEach { page ->
            val own = settingsRepository.pageBackgroundOverride(page.id).first()
            if (own != null && own.source != BackgroundSource.IMAGE) settingsRepository.clearPageBackgroundConfig(page.id)
        }

        // "Layer 3, null means auto light/dark" overrides - a customization made under the old
        // theme otherwise survives the switch untouched, silently breaking contrast against the
        // new one (e.g. a light icon stuck on a now-light dock). Resetting them back to automatic
        // is what makes this a coherent, fully-consistent look rather than "everything except
        // whatever you'd customized before".
        settingsRepository.resetAppListRailAppearance()
        settingsRepository.resetAppListSearchButtonAppearance()
        settingsRepository.resetSearchBarAppearance()
        settingsRepository.resetSearchResultsAppearance()
        settingsRepository.setFolderCardAppearance(boxAppearance)
        settingsRepository.setSearchAppearance(boxAppearance)

        val rows = pageRowDao.observeAll().first().map { it.withAppearance(boxAppearance) }
        pageRowDao.updateAll(rows)
        val items = folderItemDao.observeAll().first().map { it.withAppearance(boxAppearance) }
        folderItemDao.updateAll(items)
    }
}
