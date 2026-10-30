package com.bisc.flauncher.data.repository

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.bisc.flauncher.data.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Lists launchable apps via the standard MAIN/LAUNCHER intent query. */
class InstalledAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Emits once per install/uninstall/update. [getLaunchableApps] itself was only ever fetched
     * once at app start - a fresh install (or uninstall) never touched that snapshot again, so a
     * newly installed app simply never appeared in search or the app list until the next cold
     * start of FLauncher. Collectors re-query on each emission to pick the change up live.
     */
    fun packageChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    suspend fun getLaunchableApps(): List<AppInfo> = withContext(Dispatchers.Default) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        resolveInfos
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo ->
                AppInfo(
                    componentName = ComponentName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name
                    ),
                    label = resolveInfo.loadLabel(packageManager).toString()
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun rawLabelFor(componentName: ComponentName): String =
        runCatching {
            context.packageManager.getActivityInfo(componentName, 0).loadLabel(context.packageManager).toString()
        }.getOrDefault("")
}
