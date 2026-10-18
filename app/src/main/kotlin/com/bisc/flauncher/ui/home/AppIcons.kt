package com.bisc.flauncher.ui.home

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.bisc.flauncher.data.settings.IconShape
import com.bisc.flauncher.util.AppIconLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The shared icon loader, provided once by [HomeScreen] so long lists can read the icon cache directly. */
val LocalAppIconLoader = staticCompositionLocalOf<AppIconLoader?> { null }

/** The global icon shape, provided once by [MainActivity][com.bisc.flauncher.MainActivity] — for the
 *  cell types whose background box is drawn directly in Compose (map target/website/page link/action
 *  button/empty-slot cells) rather than baked into a cached bitmap the way app icons are (see
 *  [com.bisc.flauncher.util.AppIconLoader.renderMasked]), so those cells can still react to the shape
 *  setting without threading it as an explicit parameter through every grid/row call site. */
val LocalIconShape = staticCompositionLocalOf { IconShape.ROUNDED_SQUARE }

/** Resolves the effective icon-size scale for one app (its own override if it has one, otherwise
 *  the global default) — provided once by [HomeScreen] so [com.bisc.flauncher.ui.home.AppIconCell]
 *  doesn't need this threaded as an explicit parameter through every grid/row/folder call site that
 *  merely forwards the global scale unchanged (see HomeViewModel.iconSizeScaleFor). Null when not
 *  provided (e.g. Settings' own icon previews), which callers treat as "use the flat scale passed in". */
val LocalIconSizeResolver = staticCompositionLocalOf<((String) -> Float)?> { null }

/** An app's icon for a scrolling list. */
@Composable
fun rememberAppIcon(componentName: ComponentName?, load: (ComponentName) -> ImageBitmap?): ImageBitmap? {
    if (componentName == null) return null
    val loader = LocalAppIconLoader.current
    val revision = loader?.revision ?: 0
    val cached = remember(componentName, revision) { loader?.peek(componentName) }
    if (cached != null) return cached
    val loaded by produceState<ImageBitmap?>(initialValue = null, componentName, revision) {
        value = withContext(Dispatchers.Default) { load(componentName) }
    }
    return loaded
}
