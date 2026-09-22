package com.bisc.flauncher.ui.home.search

import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.AppInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.AppListDisplayMode
import com.bisc.flauncher.data.model.AppListLayout
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.home.dock.DockDimens
import com.bisc.flauncher.ui.home.dock.DockPanelSurface
import com.bisc.flauncher.ui.home.dock.resolveElementColors

private val WINDOW_CONTENT_INSET = 20.dp

/** App overview content: app list with A-Z rail, as fullscreen or windowed card. */
@Composable
fun AppOverviewContent(
    allApps: List<AppInfo>,
    showAzIndex: Boolean,
    indexShowRailBar: Boolean,
    headerLetterAppearance: ElementAppearance,
    alphabetLetterAppearance: ElementAppearance,
    displayMode: AppListDisplayMode,
    layout: AppListLayout = AppListLayout.LIST,
    contentAppearance: ElementAppearance,
    pageBackground: BackgroundConfig,
    railAppearance: ElementAppearance?,
    accentColor: Int,
    handedness: Handedness,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    showSearchButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    val grouping = remember(allApps) { BrowseGrouping.from(allApps) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val surfaceIsDark = rememberSurfaceIsDark(contentAppearance, pageBackground)
    // Same footprint the dock has.
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val bottomInset = if (showSearchButton) {
        DockDimens.scaled((screenWidth - DockDimens.horizontalMargin * 2).coerceAtLeast(0.dp)).barHeight +
            DockDimens.bottomMargin + DockDimens.contentGap
    } else DockDimens.bottomMargin
    val surfaceTextColor = com.bisc.flauncher.ui.home.dock.resolveElementColors(contentAppearance, surfaceIsDark).contentColor

    val browseList = @Composable { edgeInset: Dp, bottomInset: Dp ->
        BrowseAppList(
            allApps = allApps,
            grouping = grouping,
            showAzIndex = showAzIndex,
            indexShowRailBar = indexShowRailBar,
            headerLetterAppearance = headerLetterAppearance,
            alphabetLetterAppearance = alphabetLetterAppearance,
            listState = listState,
            loadIcon = loadIcon,
            onAppClick = onAppClick,
            coroutineScope = coroutineScope,
            reservedZoneHeight = bottomInset,
            handedness = handedness,
            accentColor = accentColor,
            edgeInset = edgeInset,
            layout = layout
        )
    }

    CompositionLocalProvider(LocalRailAppearance provides railAppearance, LocalContentColor provides surfaceTextColor) {
    val fullscreenFill = if (displayMode == AppListDisplayMode.FULLSCREEN) {
        val colors = resolveElementColors(contentAppearance)
        Modifier.background(colors.background, alpha = colors.backgroundAlpha)
    } else Modifier
    Box(modifier = Modifier.fillMaxSize().then(fullscreenFill)) {
    Box(modifier = modifier) {
        if (displayMode == AppListDisplayMode.WINDOW) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .padding(horizontal = DockDimens.horizontalMargin)
                    .padding(bottom = bottomInset)
            ) {
                val cornerRadius = com.bisc.flauncher.ui.home.dock.dockDimens(maxWidth).cornerRadius
                DockPanelSurface(
                    appearance = contentAppearance,
                    shape = RoundedCornerShape(cornerRadius),
                    modifier = Modifier.fillMaxSize()
                ) {
                    CompositionLocalProvider(LocalContentColor provides surfaceTextColor) {
                        Box(modifier = Modifier.fillMaxSize().padding(top = cornerRadius)) {
                            browseList(WINDOW_CONTENT_INSET, cornerRadius)
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                browseList(DockDimens.horizontalMargin, bottomInset)
            }
        }
    }
    }
    }
}
