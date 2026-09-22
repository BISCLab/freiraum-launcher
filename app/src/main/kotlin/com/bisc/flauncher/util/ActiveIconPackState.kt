package com.bisc.flauncher.util

import android.content.ComponentName
import com.bisc.flauncher.data.db.IconPackMappingDao
import com.bisc.flauncher.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** Holds the currently-active icon pack's parsed appfilter.xml mappings in memory. */
@Singleton
class ActiveIconPackState @Inject constructor(
    settingsRepository: SettingsRepository,
    mappingDao: IconPackMappingDao
) {
    @Volatile
    var activePackage: String? = null
        private set

    @Volatile
    private var mappings: Map<String, String> = emptyMap()

    init {
        val pkg = runBlocking { settingsRepository.activeIconPack.first() }
        activePackage = pkg
        if (pkg != null) {
            mappings = runBlocking { mappingDao.getAllForPack(pkg) }
                .associate { it.componentName to it.drawableName }
        }
    }

    fun drawableNameFor(componentName: ComponentName): String? =
        mappings[componentName.flattenToString()]

    fun update(packageName: String?, newMappings: Map<String, String>) {
        activePackage = packageName
        mappings = newMappings
    }
}
