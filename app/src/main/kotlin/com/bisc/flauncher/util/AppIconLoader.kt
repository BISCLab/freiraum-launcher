package com.bisc.flauncher.util

import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.createBitmap
import com.bisc.flauncher.ui.theme.isDark
import androidx.compose.ui.graphics.asAndroidBitmap
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
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
import com.bisc.flauncher.data.settings.IconStyle
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

    // Declared before init: the collectors started there may clear it right away.
    private data class RenderKey(val component: ComponentName, val shape: IconShape, val pack: String?, val style: IconStyle)

    private val renderCache = LruCache<RenderKey, ImageBitmap>(96)

    // Bounded (and thread-safe).
    private val packIconCache = LruCache<Pair<String, String>, ImageBitmap>(400)

    @Volatile
    private var overrides: Map<String, Pair<String, String>> = emptyMap()

    // Per-app icon-shape overrides - null for a component means "use the global iconShape".
    @Volatile
    private var shapeOverrides: Map<String, IconShape> = emptyMap()

    @Volatile
    private var globalShape: IconShape = IconShape.ROUNDED_SQUARE

    @Volatile
    private var globalStyle: IconStyle = IconStyle()

    /** Themed icons in light (false) or dark (true) colors; null while they are off. */
    @Volatile
    private var themedDark: Boolean? = null

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
            kotlinx.coroutines.flow.combine(settingsRepository.themedIcons, settingsRepository.theme) { on, theme ->
                if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) theme.isDark(context) else null
            }.collect { dark ->
                if (dark != themedDark) {
                    themedDark = dark
                    renderCache.evictAll()
                    clearCache()
                }
            }
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            settingsRepository.iconStyle.collect { style ->
                if (style != globalStyle) {
                    globalStyle = style
                    clearCache()
                }
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
    fun peekRender(componentName: ComponentName, shape: IconShape, iconPack: String?, style: IconStyle = globalStyle): ImageBitmap? =
        renderCache.get(RenderKey(componentName, shape, iconPack, style))

    /** An app's icon exactly as the launcher draws it with [shape] and [iconPack] (null = the apps'
     *  own icons), for the settings' previews of a choice. Kept, so showing it again is instant. */
    fun render(componentName: ComponentName, shape: IconShape, iconPack: String?, style: IconStyle = globalStyle): ImageBitmap? {
        val key = RenderKey(componentName, shape, iconPack, style)
        renderCache.get(key)?.let { return it }
        if (ProfileComponents.isProfile(componentName)) {
            return loadProfileIcon(componentName, shape, iconPack, forPreview = true, style = style)?.also { renderCache.put(key, it) }
        }
        val fromPack = iconPack?.let { pack -> packDrawableFor(pack, componentName)?.let { loadPackIcon(pack, it) } }
        val result = fromPack ?: run {
            val drawable = runCatching { context.packageManager.getActivityIcon(componentName) }.getOrNull() ?: return null
            renderMasked(drawable, shape, style).asImageBitmap()
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
    /** [shape] and [pack] for a preview of a choice; by default the ones in use. */
    private fun loadProfileIcon(marked: ComponentName, shape: IconShape? = null, pack: String? = activeIconPackState.activePackage, forPreview: Boolean = false, style: IconStyle = globalStyle): ImageBitmap? {
        val user = ProfileComponents.userOf(context, marked) ?: return null
        val real = ProfileComponents.real(marked)
        val launcherApps = context.getSystemService(android.content.pm.LauncherApps::class.java)
        val fromPack = if (forPreview) pack?.let { p -> packDrawableFor(p, real)?.let { loadPackIcon(p, it)?.asAndroidBitmap() } }
            else loadIconPackDrawable(real)?.toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
        val base: Bitmap = fromPack ?: run {
            val info = runCatching {
                launcherApps.resolveActivity(android.content.Intent(android.content.Intent.ACTION_MAIN).setComponent(real), user)
            }.getOrNull() ?: return null
            renderMasked(info.getIcon(0), shape ?: shapeOverrides[marked.flattenToString()] ?: globalShape, style)
        }
        val badged = runCatching {
            context.packageManager.getUserBadgedIcon(base.toDrawable(context.resources), user)
        }.getOrNull() ?: return base.asImageBitmap()
        return badged.toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX).asImageBitmap()
    }

    /** The outline of [shape] at the icon's size; [style] says how round a rounded square is. */
    private fun shapePath(shape: IconShape, style: IconStyle): Path {
        val size = ICON_SIZE_PX.toFloat()
        val rect = RectF(0f, 0f, size, size)
        val half = size / 2f
        return when (shape) {
            IconShape.CIRCLE -> Path().apply { addCircle(half, half, half, Path.Direction.CW) }
            IconShape.ROUNDED_SQUARE -> Path().apply {
                val radius = half * (style.cornerPercent.coerceIn(0, 100) / 100f)
                addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            IconShape.SQUARE -> Path().apply { addRect(rect, Path.Direction.CW) }
            // A superellipse: sides that bulge a little, corners that round softly.
            IconShape.SQUIRCLE -> Path().apply {
                val steps = 96
                for (i in 0..steps) {
                    val t = (i.toFloat() / steps) * 2f * Math.PI.toFloat()
                    val c = kotlin.math.cos(t)
                    val s = kotlin.math.sin(t)
                    val x = half + half * Math.signum(c) * Math.pow(kotlin.math.abs(c).toDouble(), 0.5).toFloat()
                    val y = half + half * Math.signum(s) * Math.pow(kotlin.math.abs(s).toDouble(), 0.5).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            // Three corners round, the lower right one nearly sharp.
            IconShape.TEARDROP -> Path().apply {
                val r = half
                val sharp = size * 0.08f
                addRoundRect(rect, floatArrayOf(r, r, r, r, sharp, sharp, r, r), Path.Direction.CW)
            }
            // Two opposite corners round, the other two nearly sharp.
            IconShape.LEAF -> Path().apply {
                val r = half
                val sharp = size * 0.08f
                addRoundRect(rect, floatArrayOf(r, r, sharp, sharp, r, r, sharp, sharp), Path.Direction.CW)
            }
        }
    }

    /** The mean color of an icon's visible pixels, for the fill behind a small one. */
    private fun meanColor(drawable: Drawable): Int {
        val small = createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 16, 16)
        drawable.draw(Canvas(small))
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        for (x in 0 until 16) for (y in 0 until 16) {
            val p = small.getPixel(x, y)
            if (android.graphics.Color.alpha(p) > 200) {
                r += android.graphics.Color.red(p); g += android.graphics.Color.green(p); b += android.graphics.Color.blue(p); n++
            }
        }
        return if (n == 0L) android.graphics.Color.WHITE else android.graphics.Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }

    private fun renderMasked(drawable: Drawable, shape: IconShape, style: IconStyle = globalStyle): Bitmap {
        val output = createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val path = shapePath(shape, style)
        canvas.clipPath(path)
        val dark = themedDark
        val monochrome = if (dark != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) (drawable as? AdaptiveIconDrawable)?.monochrome else null
        if (dark != null && monochrome != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Themed icon: the app's one-color icon on a system-tinted background, like the system launcher.
            val bg = context.getColor(if (dark) android.R.color.system_neutral1_800 else android.R.color.system_accent1_100)
            val fg = context.getColor(if (dark) android.R.color.system_accent1_100 else android.R.color.system_accent1_700)
            canvas.drawColor(bg)
            monochrome.mutate().apply {
                setTint(fg)
                setBounds(0, 0, ICON_SIZE_PX, ICON_SIZE_PX)
                draw(canvas)
            }
        } else if (drawable is AdaptiveIconDrawable) {
            drawable.background?.let { bg -> bg.setBounds(0, 0, ICON_SIZE_PX, ICON_SIZE_PX); bg.draw(canvas) }
            drawable.foreground?.let { fg -> fg.setBounds(0, 0, ICON_SIZE_PX, ICON_SIZE_PX); fg.draw(canvas) }
        } else if (style.fillSmall) {
            // An icon that does not fill the shape: a fill in its own color behind it, the icon a little smaller.
            canvas.drawColor(meanColor(drawable))
            val inset = (ICON_SIZE_PX * 0.16f).toInt()
            drawable.setBounds(inset, inset, ICON_SIZE_PX - inset, ICON_SIZE_PX - inset)
            drawable.draw(canvas)
        } else {
            drawable.setBounds(0, 0, ICON_SIZE_PX, ICON_SIZE_PX)
            drawable.draw(canvas)
        }
        if (style.border) {
            // Half the stroke falls outside the shape and is cut off: the width is the visible part.
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.style = android.graphics.Paint.Style.STROKE
                color = style.borderColor
                strokeWidth = ICON_SIZE_PX * 0.035f * style.borderWidth.coerceIn(1, 3)
            }
            canvas.drawPath(path, paint)
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
        const val WARM_CHUNK = 16

        fun cacheSizeKb(): Int = (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt().coerceIn(8 * 1024, 48 * 1024)
    }
}
