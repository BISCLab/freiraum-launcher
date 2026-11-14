package com.bisc.flauncher.ui.home.search

import android.content.ComponentName
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.bisc.flauncher.data.model.AppInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.settings.Handedness

/** App overview content: app list with A-Z rail, as a fullscreen fill over the page's own
 *  background (drawn separately by the caller - see [pageBackground], used only to pick a
 *  readable text color against it). */
@Composable
fun AppOverviewContent(
    allApps: List<AppInfo>,
    showAzIndex: Boolean,
    headerLetterAppearance: ElementAppearance,
    pageBackground: BackgroundConfig,
    railAppearance: ElementAppearance?,
    accentColor: Int,
    handedness: Handedness,
    cornerRadiusPercent: Int,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    showSearchButton: Boolean = false,
    /** Work profile and private space, listed after the own apps. */
    profiles: ProfileListHost? = null,
    /** While searching: only the profiles' apps that match. */
    profileQuery: String = "",
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val surfaceIsDark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), pageBackground)
    val bottomInset = if (showSearchButton) {
        AppOverviewDimens.barHeight + AppOverviewDimens.bottomMargin + AppOverviewDimens.contentGap
    } else AppOverviewDimens.bottomMargin
    val surfaceTextColor = com.bisc.flauncher.ui.home.dock.resolveElementColors(
        ElementAppearance(fillMode = ElementFillMode.NONE), surfaceIsDark
    ).contentColor

    CompositionLocalProvider(LocalRailAppearance provides railAppearance, LocalContentColor provides surfaceTextColor) {
        androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val columns = browseColumns(maxWidth, showAzIndex)
            val grouping = remember(allApps, columns) { BrowseGrouping.from(allApps, columns) }
            BrowseAppList(
                columns = columns,
                allApps = allApps,
                grouping = grouping,
                showAzIndex = showAzIndex,
                headerLetterAppearance = headerLetterAppearance,
                listState = listState,
                loadIcon = loadIcon,
                onAppClick = onAppClick,
                coroutineScope = coroutineScope,
                reservedZoneHeight = bottomInset,
                handedness = handedness,
                accentColor = accentColor,
                edgeInset = AppOverviewDimens.horizontalMargin,
                cornerRadiusPercent = cornerRadiusPercent,
                profiles = profiles,
                profileQuery = profileQuery
            )
        }
    }
}
