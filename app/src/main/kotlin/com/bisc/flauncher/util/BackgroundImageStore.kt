package com.bisc.flauncher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/** Storage of background images in app-private files, with an in-memory cache. */
object BackgroundImageStore {
    // Was 1440 — bigger than most phones actually need (portrait screens are commonly ~1080-1200px
    // wide) and, more importantly, bigger than it needs to be for what this is used for: two of these
    // get alpha-blended over each other for ~220ms during a page-background crossfade. A flat color
    // costs Skia essentially nothing to blend; a large decoded photo bitmap is real per-frame GPU/CPU
    // work, and that gap — invisible for solid colors, real for custom images — matches exactly what
    // was reported (color-to-color transitions stayed smooth, image transitions kept flickering no
    // matter how the crossfade's own timing logic was rewritten). Smaller source bitmaps blend cheaper.
    private const val MAX_DIMENSION = 1080

    fun save(context: Context, uri: Uri, key: String): String? {
        return runCatching {
            val bitmap = decodeSampled(context, uri) ?: return null
            val path = writeToStore(context, bitmap, key)
            // The freshly-picked bitmap is right here in memory — caching it under its final path now
            // means the very first time a page with this background renders, it already has both the
            // image and its luminance (used to decide icon/widget contrast) instead of showing blank
            // until an async decode-from-disk finishes. That blank-then-pop-in was the flicker seen when
            // switching between pages with different, just-picked custom backgrounds.
            cache(path, bitmap)
            path
        }.getOrNull()
    }

    private fun decodeFull(path: String): Bitmap? = BitmapFactory.decodeFile(path)

    /** [path] as stored, or — when that file isn't there — the same file name in this app's own
     *  backgrounds folder. Stored paths are absolute, so ones written under a different app id
     *  (a backup from before the rename) point into a folder this app can't read. */
    fun resolve(context: Context, path: String): String {
        if (File(path).exists()) return path
        val local = File(backgroundsDir(context), File(path).name)
        return if (local.exists()) local.absolutePath else path
    }

    private fun writeToStore(context: Context, bitmap: Bitmap, key: String): String {
        val dir = backgroundsDir(context)
        dir.mkdirs()
        // A new file name per save changes the stored path, which is what makes the UI reload the image.
        val file = File(dir, "${key}_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        filesOf(context, key).filter { it != file }.forEach { it.delete() }
        return file.absolutePath
    }

    private fun cache(path: String, bitmap: Bitmap) {
        val key = cacheKey(path)
        luminanceCache[key] = averageLuminance(bitmap)
        bitmapCache.put(key, bitmap.asImageBitmap())
    }

    private val bitmapCache = android.util.LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 1024L / 6L).toInt().coerceIn(24 * 1024, 96 * 1024)
    ).also { }

    private val luminanceCache = java.util.concurrent.ConcurrentHashMap<String, Float>()

    private fun cacheKey(path: String) = "$path@${File(path).lastModified()}"

    fun peek(path: String): ImageBitmap? = bitmapCache.get(cacheKey(path))

    fun peekLuminance(path: String): Float? = luminanceCache[cacheKey(path)]

    fun load(context: Context, path: String): ImageBitmap? {
        val key = cacheKey(path)
        bitmapCache.get(key)?.let { return it }
        val bitmap = runCatching { decodeFull(path) }.getOrNull() ?: return null
        luminanceCache[key] = averageLuminance(bitmap)
        return bitmap.asImageBitmap().also { bitmapCache.put(key, it) }
    }

    private fun averageLuminance(bitmap: Bitmap): Float {
        val small = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        val pixels = IntArray(16 * 16)
        small.getPixels(pixels, 0, 16, 0, 0, 16, 16)
        if (small !== bitmap) small.recycle()
        return pixels.map { androidx.core.graphics.ColorUtils.calculateLuminance(it).toFloat() }.average().toFloat()
    }

    fun delete(context: Context, key: String) {
        runCatching { filesOf(context, key).forEach { it.delete() } }
    }

    private fun backgroundsDir(context: Context): File = File(context.filesDir, "backgrounds")

    private fun filesOf(context: Context, key: String): List<File> {
        val timestamped = Regex("^" + Regex.escape(key) + "_[0-9]+[.]jpg$")
        return backgroundsDir(context).listFiles()?.filter { it.name == "$key.jpg" || timestamped.matches(it.name) }.orEmpty()
    }

    private fun decodeSampled(context: Context, uri: Uri): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream() always returns null in inJustDecodeBounds mode by design (it only fills
        // outWidth/outHeight) — the `?: return null` below has to key off whether the stream actually
        // opened, not off decodeStream's return value, or this bails out on every single image.
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
            true
        } ?: return null

        var sampleSize = 1
        val (width, height) = boundsOptions.outWidth to boundsOptions.outHeight
        while (width / (sampleSize * 2) >= MAX_DIMENSION || height / (sampleSize * 2) >= MAX_DIMENSION) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }
    }
}
