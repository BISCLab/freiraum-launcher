package com.bisc.flauncher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.bisc.flauncher.data.model.BundledWallpaper
import java.io.File

/** Storage of background images in app-private files, with an in-memory cache. */
object BackgroundImageStore {
    private const val MAX_DIMENSION = 1440

    fun save(context: Context, uri: Uri, key: String): String? {
        return runCatching {
            val bitmap = decodeSampled(context, uri) ?: return null
            writeToStore(context, bitmap, key)
        }.getOrNull()
    }

    /** Writes the image behind [path] to a cache file the system wallpaper picker can read through the FileProvider. */
    fun exportForSystem(context: Context, path: String): Uri? = runCatching {
        val bitmap = decodeFull(context, path)!!
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "wallpaper.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }.getOrNull()

    private fun decodeFull(context: Context, path: String): Bitmap? =
        BundledWallpaper.fromPath(path)?.let { BitmapFactory.decodeResource(context.resources, it.drawable) }
            ?: BitmapFactory.decodeFile(path)

    private fun writeToStore(context: Context, bitmap: Bitmap, key: String): String {
        val dir = backgroundsDir(context)
        dir.mkdirs()
        // A new file name per save changes the stored path, which is what makes the UI reload the image.
        val file = File(dir, "${key}_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()
        filesOf(context, key).filter { it != file }.forEach { it.delete() }
        return file.absolutePath
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
        val bitmap = runCatching { decodeFull(context, path) }.getOrNull() ?: return null
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
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
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
