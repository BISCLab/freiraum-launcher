package com.bisc.flauncher.data.repository

import android.content.Context
import android.content.Intent
import com.bisc.flauncher.data.db.IconPackMappingDao
import com.bisc.flauncher.data.model.IconPackIcon
import com.bisc.flauncher.data.model.IconPackInfo
import com.bisc.flauncher.data.model.IconPackMapping
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.util.ActiveIconPackState
import com.bisc.flauncher.util.AppIconLoader
import com.bisc.flauncher.util.IconPackAppFilterParser
import com.bisc.flauncher.util.IconPackDrawableXmlParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Discovers installed icon packs and applies a selected one. */
class IconPackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val mappingDao: IconPackMappingDao,
    private val appIconLoader: AppIconLoader,
    private val activeIconPackState: ActiveIconPackState
) {
    suspend fun installedIconPacks(): List<IconPackInfo> = withContext(Dispatchers.Default) {
        val packageManager = context.packageManager
        ICON_PACK_ACTIONS.asSequence()
            .flatMap { action ->
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(Intent(action), 0).asSequence()
            }
            .distinctBy { it.activityInfo.packageName }
            .map { resolveInfo ->
                IconPackInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = resolveInfo.loadLabel(packageManager).toString()
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    suspend fun availableIcons(packageName: String): List<IconPackIcon> = withContext(Dispatchers.Default) {
        iconListCache[packageName]?.let { return@withContext it }
        val pm = context.packageManager
        val labels = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .associate { "${it.activityInfo.packageName}/${it.activityInfo.name}" to it.loadLabel(pm).toString() }
        // appfilter.xml maps icons to apps; drawable.xml additionally lists icons meant for manual
        // picking (e.g. themed folder icons) that appfilter never mentions, so both are merged.
        val byDrawable = IconPackAppFilterParser.parse(context, packageName)
            .groupBy({ it.second }, { it.first })
            .toMutableMap()
        IconPackDrawableXmlParser.parse(context, packageName).forEach { drawable -> byDrawable.getOrPut(drawable) { emptyList() } }
        val icons = byDrawable
            .map { (drawable, components) ->
                val text = buildString {
                    append(drawable.replace('_', ' '))
                    components.forEach { component ->
                        append(' ').append(component)
                        labels[component]?.let { append(' ').append(it) }
                    }
                }.lowercase()
                IconPackIcon(drawable, text)
            }
            .sortedBy { it.drawable }
        iconListCache[packageName] = icons
        icons
    }

    private val iconListCache = java.util.concurrent.ConcurrentHashMap<String, List<IconPackIcon>>()

    suspend fun selectPack(packageName: String?) = withContext(Dispatchers.IO) {
        if (packageName == null) {
            activeIconPackState.update(null, emptyMap())
            settingsRepository.setActiveIconPack(null)
            appIconLoader.clearCache()
            return@withContext
        }

        val parsed = IconPackAppFilterParser.parse(context, packageName)
        mappingDao.clearForPack(packageName)
        if (parsed.isNotEmpty()) {
            mappingDao.insertAll(
                parsed.map { (component, drawable) ->
                    IconPackMapping(iconPackPackage = packageName, componentName = component, drawableName = drawable)
                }
            )
        }
        activeIconPackState.update(packageName, parsed.toMap())
        settingsRepository.setActiveIconPack(packageName)
        appIconLoader.clearCache()
    }

    private companion object {
        val ICON_PACK_ACTIONS = listOf("com.novalauncher.THEME", "org.adw.launcher.THEMES")
    }
}
