package com.bisc.flauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.bisc.flauncher.data.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Lists launchable apps via the standard MAIN/LAUNCHER intent query. */
class InstalledAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
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
