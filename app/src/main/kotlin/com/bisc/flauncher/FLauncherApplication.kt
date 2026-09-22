package com.bisc.flauncher

import android.app.Application
import android.content.Context
import com.bisc.flauncher.data.backup.BackupManager
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.util.ExternalApps
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class FLauncherApplication : Application() {
    @Inject lateinit var settings: SettingsRepository

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        BackupManager.applyStagedRestore(base)
    }

    override fun onCreate() {
        super.onCreate()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch { settings.searchBrowserApp.collect { ExternalApps.browser = it } }
        scope.launch { settings.searchMapsApp.collect { ExternalApps.maps = it } }
    }
}
