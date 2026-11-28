package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.stackElement

/** One element in a [ElementCollectionScreen]: its id in the stack, its name and its symbol. */
internal class CollectionItem(
    val id: String,
    val name: String,
    val symbol: @Composable (size: Dp, ink: Color) -> Unit
)

/**
 * A kind of element (places, links, action buttons) in one card stack: ⓪ all of them, then each
 * with its options. The replica shows the chosen one's symbol at its real size with its name, or
 * for ⓪ all symbols side by side, each with its number.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ElementCollectionScreen(
    title: String,
    navController: NavController,
    onBack: () -> Unit,
    startElement: String?,
    draft: StackDraft,
    home: HomeViewModel,
    elements: List<StackElement>,
    items: List<CollectionItem>,
    listHeader: (@Composable () -> Unit)? = null
) {
    val pages by home.pages.collectAsState()
    val homeId = pages.find { it.isHome }?.id ?: -1L
    val global by home.globalBackground.collectAsState()
    val background by androidx.compose.runtime.remember(homeId) { home.pageBackgroundConfig(homeId) }.collectAsState(initial = global)
    val iconScale by home.iconSizeScale.collectAsState()
    val dark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), draft.shownBackground(background))
    CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalSurfaceIsDark provides dark) {
        SettingsCardStackScreen(
            title = title,
            navController = navController,
            onBack = onBack,
            draft = draft,
            elements = elements,
            startElement = startElement,
            firstCard = StackCard.ELEMENTS,
            listHeader = listHeader
        ) { _, _ ->
            val ink = if (dark) Color.White else Color.Black
            val statusTop = replicaStatusHeight()
            val selected = LocalStackSelected.current
            val chosen = items.find { it.id == selected }
            CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalAppIconLoader provides home.iconLoader) {
                Box(Modifier.fillMaxSize()) {
                    com.bisc.flauncher.ui.home.dock.BackgroundFill(draft.shownBackground(background), Modifier.fillMaxSize())
                    @Composable
                    fun Cell(item: CollectionItem) {
                        Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.stackElement(item.id)) { item.symbol(56.dp * iconScale, ink) }
                            Spacer(Modifier.height(8.dp))
                            Text(item.name, color = ink, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (chosen != null) {
                        Box(Modifier.align(Alignment.Center)) { Cell(chosen) }
                    } else {
                        // All of them, as they would lie on a page.
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = statusTop + 48.dp, start = 12.dp, end = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            maxItemsInEachRow = 4
                        ) { items.take(16).forEach { Cell(it) } }
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
