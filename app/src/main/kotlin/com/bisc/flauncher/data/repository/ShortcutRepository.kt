package com.bisc.flauncher.data.repository

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Process
import com.bisc.flauncher.data.db.PinnedShortcutDao
import com.bisc.flauncher.data.model.PinnedShortcut
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** App shortcuts via the LauncherApps API. */
class ShortcutRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PinnedShortcutDao
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    val pinnedShortcuts: Flow<List<PinnedShortcut>> = dao.observeAll()

    fun hasPermission(): Boolean = runCatching { launcherApps?.hasShortcutHostPermission() }.getOrNull() ?: false

    suspend fun shortcutsFor(packageName: String): List<ShortcutInfo> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)
        }
        runCatching { launcherApps?.getShortcuts(query, Process.myUserHandle()) }.getOrNull().orEmpty()
    }

    /** Every installed app's own shortcuts (what long-pressing its icon shows), for the text search. */
    suspend fun allShortcuts(): List<ShortcutInfo> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val query = LauncherApps.ShortcutQuery().apply {
            setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)
        }
        runCatching { launcherApps?.getShortcuts(query, Process.myUserHandle()) }.getOrNull().orEmpty()
            .filter { it.isEnabled }
    }

    fun loadIcon(info: ShortcutInfo): Drawable? =
        runCatching { launcherApps?.getShortcutIconDrawable(info, context.resources.displayMetrics.densityDpi) }.getOrNull()

    suspend fun pin(info: ShortcutInfo): Long {
        val label = (info.shortLabel ?: info.longLabel ?: info.id).toString()
        return dao.insert(PinnedShortcut(packageName = info.`package`, shortcutId = info.id, label = label))
    }

    fun launch(shortcut: PinnedShortcut) {
        launch(shortcut.packageName, shortcut.shortcutId)
    }

    fun launch(packageName: String, shortcutId: String) {
        if (!hasPermission()) return
        runCatching {
            launcherApps?.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
        }
    }
}
