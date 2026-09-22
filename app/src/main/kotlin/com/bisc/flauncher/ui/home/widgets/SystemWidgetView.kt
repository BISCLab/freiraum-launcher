package com.bisc.flauncher.ui.home.widgets

import android.appwidget.AppWidgetHostView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.WidgetCornerStyle
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.LocalSurfaceIsDark
import com.bisc.flauncher.ui.home.LocalSurfaceIsDarkLoading

/** Embeds a real, hosted third-party AppWidget (as opposed to our own Compose widgets). */
@Composable
fun SystemWidgetView(
    appWidgetId: Int,
    modifier: Modifier = Modifier,
    appearance: ElementAppearance = ElementAppearance(),
    cornerStyle: WidgetCornerStyle = WidgetCornerStyle.ROUNDED,
    horizontalMargin: Dp = 0.dp,
    onHostedViewReady: ((android.view.View) -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val info = remember(appWidgetId) { viewModel.getSystemWidgetInfo(appWidgetId) }
    // While the real page background is still loading, LocalSurfaceIsDark would only be a guess; mounting
    // the native widget view with a guessed dark/light setting can leave it stuck wrong even once the real
    // value arrives (many widgets don't repaint themselves just because our host view gets recreated).
    val surfaceLoading = LocalSurfaceIsDarkLoading.current
    if (info == null || surfaceLoading) {
        WidgetCard(modifier = modifier, appearance = appearance, cornerStyle = cornerStyle, horizontalMargin = horizontalMargin) {
            Text("···", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    WidgetCard(
        modifier = modifier,
        appearance = appearance,
        cornerStyle = cornerStyle,
        horizontalMargin = horizontalMargin,
        contentPadding = 0.dp
    ) {
        // Follows the surface the widget sits on (our own dark/light decision), not the system's
        // separate day/night setting, so it doesn't clash with the rest of the launcher.
        val dark = LocalSurfaceIsDark.current ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)
        // No custom pointer handling here: a hosted widget's own scrollable content (e.g. a list)
        // calls requestDisallowInterceptTouchEvent on its own when it starts scrolling, and Compose's
        // View interop already honors that against every ancestor gesture (page scroll, page-swipe,
        // ...). Adding our own drag interception here previously *prevented* the widget from ever
        // seeing the continued drag, which is why it couldn't scroll and could leave the page's own
        // swipe gesture in a half-consumed, stuck state.
        val density = LocalDensity.current
        androidx.compose.runtime.key(dark) {
            var hostView by remember { mutableStateOf<AppWidgetHostView?>(null) }
            AndroidView(
                modifier = Modifier.fillMaxSize().onSizeChanged { size ->
                    if (size.width <= 0 || size.height <= 0) return@onSizeChanged
                    val view = hostView ?: return@onSizeChanged
                    // Some widgets only bind their scrollable content (RemoteViewsAdapter lists, etc.)
                    // once they learn their real allotted size; without this they can silently stay in
                    // a default/collapsed layout with nothing to scroll, even though our card is big.
                    with(density) {
                        val widthDp = size.width.toDp().value.toInt()
                        val heightDp = size.height.toDp().value.toInt()
                        viewModel.updateSystemWidgetSize(appWidgetId, view, widthDp, heightDp)
                    }
                },
                factory = {
                    viewModel.createSystemWidgetView(appWidgetId, info, dark).also {
                        hostView = it
                        onHostedViewReady?.invoke(it)
                    }
                }
            )
        }
    }
}
