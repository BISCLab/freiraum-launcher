package com.bisc.flauncher.ui.home

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The icons of shortcuts ("pkg|id|label"), as the app that made them draws them: a website added
 *  from Chrome shows its own icon instead of a generic symbol. Kept once loaded. */
object ShortcutIcons {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()

    fun load(context: Context, packageName: String, shortcutId: String, sizePx: Int): ImageBitmap? {
        val key = "$packageName|$shortcutId"
        cache[key]?.let { return it }
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return null
        val info = runCatching {
            if (!launcherApps.hasShortcutHostPermission()) return null
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setShortcutIds(listOf(shortcutId))
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                )
            }
            launcherApps.getShortcuts(query, Process.myUserHandle())?.firstOrNull()
        }.getOrNull() ?: return null
        val drawable = runCatching { launcherApps.getShortcutIconDrawable(info, context.resources.displayMetrics.densityDpi) }.getOrNull() ?: return null
        val bitmap = runCatching { drawable.toBitmap(sizePx, sizePx).asImageBitmap() }.getOrNull() ?: return null
        cache[key] = bitmap
        return bitmap
    }
}

/** A shortcut's own icon for [payload] ("pkg|id|label"), or null while loading or when it has none. */
@Composable
fun rememberShortcutIcon(payload: String): ImageBitmap? {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, payload) {
        val parts = payload.split("|")
        if (parts.size >= 2) value = withContext(Dispatchers.IO) { ShortcutIcons.load(context, parts[0], parts[1], 192) }
    }
    return icon
}
