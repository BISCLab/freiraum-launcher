package com.bisc.flauncher.ui.home.dock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.BackgroundSource
import com.bisc.flauncher.data.model.asBrush
import com.bisc.flauncher.util.BackgroundImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Draws a layer 1 background (image or color), fully opaque, with an optional black/white shade
 *  overlay on top (negative [BackgroundConfig.shade] darkens, positive lightens). */
@Composable
fun BackgroundFill(config: BackgroundConfig, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        when (config.source) {
            BackgroundSource.COLOR -> {
                Box(Modifier.matchParentSize().background(config.fill.asBrush()))
            }
            BackgroundSource.IMAGE -> {
                val bitmap = rememberBackgroundBitmap(config.imagePath)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        val shade = config.shade.coerceIn(-1f, 1f)
        if (shade < 0f) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = -shade)))
        } else if (shade > 0f) {
            Box(Modifier.matchParentSize().background(Color.White.copy(alpha = shade)))
        }
    }
}

/** Stored background image for a path, served from the in-memory cache when available. */
@Composable
fun rememberBackgroundBitmap(path: String?): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = path?.let { BackgroundImageStore.peek(it) }, path) {
        // Re-resolved whenever the path changes; keeping the old value would show the previous image.
        value = path?.let { BackgroundImageStore.peek(it) ?: withContext(Dispatchers.Default) { BackgroundImageStore.load(context, it) } }
    }
    return bitmap
}

/** Average luminance of the stored image at [path]. */
@Composable
fun rememberBackgroundLuminance(path: String?): Float? {
    val context = LocalContext.current
    val luminance by produceState<Float?>(initialValue = path?.let { BackgroundImageStore.peekLuminance(it) }, path) {
        value = path?.let {
            BackgroundImageStore.peekLuminance(it) ?: withContext(Dispatchers.Default) {
                BackgroundImageStore.load(context, it)
                BackgroundImageStore.peekLuminance(it)
            }
        }
    }
    return luminance
}
