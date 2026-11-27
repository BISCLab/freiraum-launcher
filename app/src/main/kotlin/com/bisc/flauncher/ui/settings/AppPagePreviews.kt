package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.data.model.AppGridBarPosition
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.search.automaticRailAppearance
import com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark

/** The background a utility page really shows: its own, or the global one with its own shade. */
@Composable
private fun rememberPageBackground(pageId: Long?, home: HomeViewModel): BackgroundConfig {
    val global by home.globalBackground.collectAsState()
    val id = pageId ?: return global
    val override by remember(id) { home.pageBackgroundOverride(id) }.collectAsState(initial = null)
    val shade by remember(id) { home.pageGlobalShade(id) }.collectAsState(initial = null)
    return override ?: global.copy(shade = shade ?: global.shade)
}

/** A phone outline sized like the Text search preview, with the page background inside; a tap on
 *  free space opens the page's own settings. */
@Composable
private fun PreviewPhone(background: BackgroundConfig, onPage: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val phoneShape = RoundedCornerShape(24.dp)
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(0.5f)
                .clip(phoneShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(3.dp, MaterialTheme.colorScheme.outline, phoneShape)
        ) {
            BackgroundFill(background, Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().clickable(onClick = onPage))
            content()
        }
    }
}

@Composable
private fun Modifier.look(look: ElementAppearance, dark: Boolean, shape: androidx.compose.ui.graphics.Shape): Modifier {
    val colors = resolveElementColors(look, dark)
    return this
        .clip(shape)
        .background(colors.background, alpha = colors.backgroundAlpha)
        .then(if (look.borderEnabled) Modifier.border(colors.borderWidth / 2, colors.borderColor, shape) else Modifier)
}

/** The Categories page in small, from the live settings: the chips of the real categories, the
 *  search field when it is shown, and the cards, tabs or list below. Tap a part to open its settings. */
@Composable
fun CategoriesPreview(onNavigate: (String) -> Unit, home: HomeViewModel = hiltViewModel()) {
    val pageId by home.categoriesPageId.collectAsState()
    val background = rememberPageBackground(pageId, home)
    val dark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), background)
    val resolved by home.resolvedCategories.collectAsState()
    val view by home.categoriesView.collectAsState()
    val showSearch by home.categoriesShowSearch.collectAsState()
    val cardRows by home.categoriesCardRows.collectAsState()
    val boxLook by home.categoryBoxLook.collectAsState()
    val look = boxLook.first
    val ink = resolveElementColors(ElementAppearance(fillMode = ElementFillMode.NONE), dark).contentColor
    val shown = resolved.take(3)
    val accent = MaterialTheme.colorScheme.primary

    PreviewPhone(background, onPage = { onNavigate("page") }) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            // The chips: the categories themselves.
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onNavigate("manage") },
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val chips = shown.ifEmpty { listOf(null, null, null) }
                chips.forEachIndexed { i, r ->
                    val color = r?.let { com.bisc.flauncher.ui.home.categoryColor(it.category) } ?: accent
                    Box(
                        Modifier.width(if (i == 0) 30.dp else 24.dp).height(10.dp).clip(CircleShape)
                            .then(if (i == 0) Modifier.background(color) else Modifier.border(1.dp, color, CircleShape))
                    )
                }
            }
            if (showSearch) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(14.dp).look(automaticRailAppearance(dark), dark, CircleShape).clickable { onNavigate("layout") },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = ink.copy(alpha = 0.6f), modifier = Modifier.padding(start = 5.dp).size(8.dp))
                }
            }
            // Below each other: cards with their first rows (all of them at 0); tabs: one card.
            val cards = if (view == "TABS") 1 else 2
            val rowsPerCard = when { view == "TABS" -> 5; cardRows == 0 -> 3; else -> cardRows }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().clickable { onNavigate("layout") },
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(cards) { c ->
                    val color = shown.getOrNull(c)?.let { com.bisc.flauncher.ui.home.categoryColor(it.category) } ?: accent
                    Column(
                        modifier = Modifier.fillMaxWidth().look(look, dark, RoundedCornerShape(8.dp)).padding(5.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(Modifier.width(26.dp).height(7.dp).clip(CircleShape).background(color))
                        repeat(rowsPerCard) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                repeat(4) { Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(ink.copy(alpha = 0.35f))) }
                            }
                        }
                    }
                }
            }
        }
    }
}
