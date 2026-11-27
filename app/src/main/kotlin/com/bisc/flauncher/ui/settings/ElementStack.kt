package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.stackElement

/**
 * One element (an app, a place, a link, an action button) as a card stack: ⓪ what it is and does,
 * where it is used, and ① its symbol. The replica shows the symbol at its real size with its name,
 * as it lies on a page.
 */
@Composable
internal fun ElementStackScreen(
    title: String,
    navController: NavController,
    onBack: () -> Unit,
    startElement: String?,
    draft: StackDraft,
    home: HomeViewModel,
    /** ⓪'s name in the list, e.g. "Address". */
    partTitle: String,
    partSummary: String?,
    partKeys: List<String>,
    symbolTitle: String,
    symbolKeys: List<String>,
    name: String,
    /** The symbol at [size], drawn on a background of [ink]'s opposite. */
    symbol: @Composable (size: Dp, ink: Color) -> Unit,
    partOptions: @Composable ColumnScope.() -> Unit,
    symbolOptions: @Composable ColumnScope.() -> Unit
) {
    val elements = listOf(
        StackElement(id = "element", number = 0, title = partTitle, summary = partSummary, draftKeys = partKeys, options = partOptions),
        StackElement(
            id = "symbol", number = 1,
            title = symbolTitle,
            draftKeys = symbolKeys,
            anchor = Offset(1f, 0f),
            sample = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { symbol(72.dp, MaterialTheme.colorScheme.onSurface) } },
            options = symbolOptions
        )
    )
    val pages by home.pages.collectAsState()
    val homeId = pages.find { it.isHome }?.id ?: -1L
    val global by home.globalBackground.collectAsState()
    val background by androidx.compose.runtime.remember(homeId) { home.pageBackgroundConfig(homeId) }.collectAsState(initial = global)
    val iconScale by home.iconSizeScale.collectAsState()
    val dark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), background)
    CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalSurfaceIsDark provides dark) {
        SettingsCardStackScreen(
            title = title,
            navController = navController,
            onBack = onBack,
            draft = draft,
            elements = elements,
            startElement = startElement
        ) { _, _ ->
            val ink = if (dark) Color.White else Color.Black
            val statusTop = replicaStatusHeight()
            CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalAppIconLoader provides home.iconLoader) {
                Box(Modifier.fillMaxSize()) {
                    com.bisc.flauncher.ui.home.dock.BackgroundFill(background, Modifier.fillMaxSize())
                    Column(
                        modifier = Modifier.align(Alignment.Center).width(96.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.stackElement("symbol")) { symbol(56.dp * iconScale, ink) }
                        Spacer(Modifier.height(8.dp))
                        Text(name, color = ink, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    StatusBarSketch(statusTop, ink)
                }
            }
        }
    }
}

/** A tile face (place, link, action button) at a size, like the cells on a page draw it. */
@Composable
internal fun ElementTile(face: com.bisc.flauncher.data.model.TileFace, label: String, size: Dp, ink: Color, defaultSymbol: androidx.compose.ui.graphics.vector.ImageVector) {
    com.bisc.flauncher.ui.home.TileFaceView(
        face, com.bisc.flauncher.ui.home.dock.UNSET_APP_SLOT_COLOR, label,
        modifier = Modifier.size(size), shape = androidx.compose.foundation.shape.RoundedCornerShape(size * 0.28f), around = ink, defaultSymbol = defaultSymbol
    )
}
