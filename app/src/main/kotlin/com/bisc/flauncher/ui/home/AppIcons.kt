package com.bisc.flauncher.ui.home

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.bisc.flauncher.util.AppIconLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The shared icon loader, provided once by [HomeScreen] so long lists can read the icon cache directly. */
val LocalAppIconLoader = staticCompositionLocalOf<AppIconLoader?> { null }

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
