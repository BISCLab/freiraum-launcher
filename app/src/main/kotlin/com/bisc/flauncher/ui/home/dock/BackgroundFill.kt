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
import androidx.compose.ui.graphics.graphicsLayer
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
                // Opaque even while the image is loading (or if it's missing), so whatever lies
                // below — the home screen and its Dock under the app list or search — never shows.
                Box(Modifier.matchParentSize().background(config.fill.asBrush()))
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

/** Two neighboring backgrounds blended by [fraction] (0 = [lower] only). When both show the same
 *  image, only their shades differ — the image is drawn once and just the shade is blended, instead
 *  of alpha-blending two full-screen photos every frame (expensive, and it flickers). */
@Composable
fun BackgroundCrossfade(lower: BackgroundConfig, upper: BackgroundConfig?, fraction: Float, modifier: Modifier = Modifier) {
    if (upper == null || fraction <= 0f) {
        BackgroundFill(lower, modifier)
        return
    }
    val sameImage = lower.source == BackgroundSource.IMAGE && upper.source == BackgroundSource.IMAGE &&
        lower.imagePath == upper.imagePath
    if (sameImage) {
        BackgroundFill(lower.copy(shade = lower.shade + (upper.shade - lower.shade) * fraction), modifier)
    } else {
        Box(modifier = modifier) {
            BackgroundFill(lower, Modifier.matchParentSize())
            BackgroundFill(upper, Modifier.matchParentSize().graphicsLayer { alpha = fraction })
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
