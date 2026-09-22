package com.bisc.flauncher.util

import android.view.View
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.view.drawToBitmap

/** Renders [view]'s current content to a bitmap, e.g. so a color can be sampled from it. */
fun captureViewBitmap(view: View): ImageBitmap? {
    if (view.width <= 0 || view.height <= 0) return null
    return runCatching { view.drawToBitmap().asImageBitmap() }.getOrNull()
}
