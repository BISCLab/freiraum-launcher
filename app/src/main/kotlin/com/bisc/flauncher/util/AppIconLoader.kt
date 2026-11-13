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
import com.bisc.flauncher.data.settings.IconShape
import com.bisc.flauncher.data.settings.SettingsRepository
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
    private val settingsRepository: SettingsRepository,
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

    // Per-app icon-shape overrides - null for a component means "use the global iconShape".
    @Volatile
    private var shapeOverrides: Map<String, IconShape> = emptyMap()

    @Volatile
    private var globalShape: IconShape = IconShape.ROUNDED_SQUARE

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
                setShapeOverrides(
                    list.mapNotNull { override ->
                        override.iconShape?.let { shape -> override.componentName to shape }
                    }.toMap()
                )
            }
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            settingsRepository.iconShape.collect { shape ->
                if (shape != globalShape) {
                    globalShape = shape
                    clearCache()
                }
            }
        }
    }

    private fun setOverrides(newOverrides: Map<String, Pair<String, String>>) {
        if (newOverrides == overrides) return
        overrides = newOverrides
        clearCache()
    }

    private fun setShapeOverrides(newOverrides: Map<String, IconShape>) {
        if (newOverrides == shapeOverrides) return
        shapeOverrides = newOverrides
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
        if (ProfileComponents.isProfile(componentName)) {
            val own = overrides[componentName.flattenToString()]?.let { loadPackIcon(it.first, it.second) }
            return (own ?: loadProfileIcon(componentName))?.also { cache.put(componentName, it) }
        }
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
                val shape = shapeOverrides[componentName.flattenToString()] ?: globalShape
                renderMasked(drawable, shape).asImageBitmap()
            }
        }
        cache.put(componentName, resolved)
        return resolved
    }

    /** One icon pack's appfilter, read once: by exact component, and by package as a fallback. */
    private class PackIndex(val exact: Map<String, String>, val byPackage: Map<String, String>)

    private val packIndexes = java.util.concurrent.ConcurrentHashMap<String, PackIndex>()
    private val renderCache = LruCache<Triple<ComponentName, IconShape, String?>, ImageBitmap>(96)

    private fun packIndex(pack: String): PackIndex = packIndexes.computeIfAbsent(pack) {
        val mappings = runCatching { IconPackAppFilterParser.parse(context, pack) }.getOrDefault(emptyList())
        val byPackage = HashMap<String, String>()
        mappings.forEach { (key, drawable) -> byPackage.putIfAbsent(key.substringBefore('/'), drawable) }
        PackIndex(mappings.toMap(), byPackage)
    }

    /** Every component [pack] has an icon for, read once and kept. */
    fun packMappings(pack: String): Map<String, String> = packIndex(pack).exact

    /** The drawable [pack] has for [componentName], if any; the pack is read only the first time. */
    fun packDrawableFor(pack: String, componentName: ComponentName): String? {
        val index = packIndex(pack)
        return index.exact[componentName.flattenToString()] ?: index.exact[componentName.flattenToShortString()]
            ?: index.byPackage[componentName.packageName]
    }

    /** A rendering made before, without any work; null if there is none yet. */
    fun peekRender(componentName: ComponentName, shape: IconShape, iconPack: String?): ImageBitmap? =
        renderCache.get(Triple(componentName, shape, iconPack))

    /** An app's icon exactly as the launcher draws it with [shape] and [iconPack] (null = the apps'
     *  own icons), for the settings' previews of a choice. Kept, so showing it again is instant. */
    fun render(componentName: ComponentName, shape: IconShape, iconPack: String?): ImageBitmap? {
        val key = Triple(componentName, shape, iconPack)
        renderCache.get(key)?.let { return it }
        val fromPack = iconPack?.let { pack -> packDrawableFor(pack, componentName)?.let { loadPackIcon(pack, it) } }
        val result = fromPack ?: run {
            val drawable = runCatching { context.packageManager.getActivityIcon(componentName) }.getOrNull() ?: return null
            renderMasked(drawable, shape).asImageBitmap()
        }
        renderCache.put(key, result)
        return result
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

    /** An app of another profile: its own icon (or the icon pack's) in the shape, with the
     *  profile's badge on it as the system draws it. */
    private fun loadProfileIcon(marked: ComponentName): ImageBitmap? {
        val user = ProfileComponents.userOf(context, marked) ?: return null
        val real = ProfileComponents.real(marked)
        val launcherApps = context.getSystemService(android.content.pm.LauncherApps::class.java)
        val base: Bitmap = loadIconPackDrawable(real)?.toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX) ?: run {
            val info = runCatching {
                launcherApps.resolveActivity(android.content.Intent(android.content.Intent.ACTION_MAIN).setComponent(real), user)
            }.getOrNull() ?: return null
            val shape = shapeOverrides[marked.flattenToString()] ?: globalShape
            renderMasked(info.getIcon(0), shape)
        }
        val badged = runCatching {
            context.packageManager.getUserBadgedIcon(android.graphics.drawable.BitmapDrawable(context.resources, base), user)
        }.getOrNull() ?: return base.asImageBitmap()
        return badged.toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX).asImageBitmap()
    }

    private fun renderMasked(drawable: Drawable, shape: IconShape): Bitmap {
        val output = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val path = when (shape) {
            IconShape.CIRCLE -> Path().apply {
                val r = ICON_SIZE_PX / 2f
                addCircle(r, r, r, Path.Direction.CW)
            }
            IconShape.ROUNDED_SQUARE -> Path().apply {
                val radius = ICON_SIZE_PX * CORNER_RADIUS_FRACTION
                addRoundRect(RectF(0f, 0f, ICON_SIZE_PX.toFloat(), ICON_SIZE_PX.toFloat()), radius, radius, Path.Direction.CW)
            }
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
