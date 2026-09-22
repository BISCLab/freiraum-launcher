package com.bisc.flauncher.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Loads and in-memory-caches contact photo thumbnails. */
@Singleton
class ContactPhotoLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = LruCache<Long, ImageBitmap>(CACHE_SIZE)

    suspend fun load(contactId: Long, photoUri: String?): ImageBitmap? {
        cache.get(contactId)?.let { return it }
        if (photoUri == null) return null
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }?.asImageBitmap() ?: return null
        cache.put(contactId, bitmap)
        return bitmap
    }

    private companion object {
        const val CACHE_SIZE = 80
    }
}
