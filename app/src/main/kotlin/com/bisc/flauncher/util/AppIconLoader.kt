package com.bisc.flauncher.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.bisc.flauncher.data.db.AppOverrideDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Loads and caches launcher icons. */
@Singleton
class AppIconLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activeIconPackState: ActiveIconPackState,
    appOverrideDao: AppOverrideDao
) {
    private val cache = object : LruCache<ComponentName, ImageBitmap>(cacheSizeKb()) {
        override fun sizeOf(key: ComponentName, value: ImageBitmap): Int =
            ((value.width.toLong() * value.height.toLong() * 4L) / 1024L).toInt().coerceAtLeast(1)
    }

    // Bounded (and thread-safe).
    private val packIconCache = LruCache<Pair<String, String>, ImageBitmap>(400)

    @Volatile
    private var overrides: Map<String, Pair<String, String>> = emptyMap()

    var revision by mutableIntStateOf(0)
        private set

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            appOverrideDao.observeAll().collect { list ->
                setOverrides(
                    list.mapNotNull { override ->
                        val pack = override.iconPackPackage
                        val drawable = override.iconPackDrawable
                        if (pack != null && drawable != null) override.componentName to (pack to drawable) else null
                    }.toMap()
                )
            }
        }
    }

    private fun setOverrides(newOverrides: Map<String, Pair<String, String>>) {
        if (newOverrides == overrides) return
        overrides = newOverrides
        clearCache()
    }

    fun loadPackIcon(iconPackPackage: String, drawableName: String): ImageBitmap? {
        val key = iconPackPackage to drawableName
        packIconCache.get(key)?.let { return it }
        val drawable = runCatching {
            val resources = context.packageManager.getResourcesForApplication(iconPackPackage)
            val resId = resources.getIdentifier(drawableName, "drawable", iconPackPackage)
            if (resId == 0) null else ResourcesCompat.getDrawable(resources, resId, null)
        }.getOrNull() ?: return null
        val bitmap = drawable.toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX).asImageBitmap()
        packIconCache.put(key, bitmap)
        return bitmap
    }

    fun peek(componentName: ComponentName): ImageBitmap? = cache.get(componentName)

    fun load(componentName: ComponentName): ImageBitmap? {
        cache.get(componentName)?.let { return it }
        val override = overrides[componentName.flattenToString()]
        val bitmap = if (override != null) {
            loadPackIcon(override.first, override.second)
        } else null
        val resolved = bitmap ?: run {
            val iconPackDrawable = loadIconPackDrawable(componentName)
            if (iconPackDrawable != null) {
                iconPackDrawable.toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX).asImageBitmap()
            } else {
                val drawable = try {
                    context.packageManager.getActivityIcon(componentName)
                } catch (e: PackageManager.NameNotFoundException) {
                    return null
                }
                renderMasked(drawable).asImageBitmap()
            }
        }
        cache.put(componentName, resolved)
        return resolved
    }

    suspend fun warm(components: List<ComponentName>) = coroutineScope {
        components.chunked(WARM_CHUNK).map { chunk ->
            async(Dispatchers.Default) {
                for (component in chunk) {
                    ensureActive()
                    if (cache.get(component) == null) runCatching { load(component) }
                }
            }
        }.awaitAll()
    }

    fun clearCache() {
        cache.evictAll()
        revision++
    }

    private fun renderMasked(drawable: Drawable): Bitmap {
        val output = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val radius = ICON_SIZE_PX * CORNER_RADIUS_FRACTION
        val path = Path().apply {
            addRoundRect(RectF(0f, 0f, ICON_SIZE_PX.toFloat(), ICON_SIZE_PX.toFloat()), radius, radius, Path.Direction.CW)
        }
        canvas.clipPath(path)
        if (drawable is AdaptiveIconDrawable) {
            drawable.background?.let { bg -> bg.setBounds(0, 0, ICON_SIZE_PX, ICON_SIZE_PX); bg.draw(canvas) }
            drawable.foreground?.let { fg -> fg.setBounds(0, 0, ICON_SIZE_PX, ICON_SIZE_PX); fg.draw(canvas) }
        } else {
            drawable.setBounds(0, 0, ICON_SIZE_PX, ICON_SIZE_PX)
            drawable.draw(canvas)
        }
        return output
    }

    private fun loadIconPackDrawable(componentName: ComponentName): Drawable? {
        val pack = activeIconPackState.activePackage ?: return null
        val drawableName = activeIconPackState.drawableNameFor(componentName) ?: return null
        return runCatching {
            val resources = context.packageManager.getResourcesForApplication(pack)
            val resId = resources.getIdentifier(drawableName, "drawable", pack)
            if (resId == 0) null else ResourcesCompat.getDrawable(resources, resId, null)
        }.getOrNull()
    }

    private companion object {
        const val ICON_SIZE_PX = 128
        const val CORNER_RADIUS_FRACTION = 0.22f
        const val WARM_CHUNK = 16

        fun cacheSizeKb(): Int = (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt().coerceIn(8 * 1024, 48 * 1024)
    }
}
