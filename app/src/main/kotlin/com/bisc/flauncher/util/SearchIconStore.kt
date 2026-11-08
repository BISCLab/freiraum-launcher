package com.bisc.flauncher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Own pictures for search engines and AI apps, kept as small square PNGs in the app's files
 * (inside a folder the backup takes along). [stamps] changes whenever one is set or removed.
 */
object SearchIconStore {
    private const val SIZE = 192
    private val state = MutableStateFlow<Map<String, Long>>(emptyMap())
    @Volatile private var loaded = false

    fun engineId(engineId: String) = safe("engine_$engineId")
    fun aiId(packageName: String) = safe("ai_$packageName")

    fun stamps(context: Context): StateFlow<Map<String, Long>> {
        if (!loaded) {
            loaded = true
            state.value = dir(context).listFiles()?.filter { it.isFile }?.associate { it.nameWithoutExtension to it.lastModified() }.orEmpty()
        }
        return state
    }

    fun load(context: Context, id: String): ImageBitmap? =
        file(context, id).takeIf { it.exists() }?.let { runCatching { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }.getOrNull() }

    suspend fun save(context: Context, id: String, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2
            val source = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching false
            val side = minOf(source.width, source.height)
            val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
            val scaled = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
            stamps(context)
            dir(context).mkdirs()
            file(context, id).outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
            state.value = state.value + (id to System.currentTimeMillis())
            true
        }.getOrDefault(false)
    }

    fun remove(context: Context, id: String) {
        stamps(context)
        file(context, id).delete()
        state.value = state.value - id
    }

    private fun safe(raw: String) = raw.replace(Regex("[^A-Za-z0-9_.-]"), "_")
    private fun dir(context: Context) = File(context.filesDir, "search_icons/own")
    private fun file(context: Context, id: String) = File(dir(context), "$id.png")
}
