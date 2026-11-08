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

/** The App list in small, from the live settings: section letters, A-Z rail and search button.
 *  Each is tappable and opens its own settings (the same routes as the list below). */
@Composable
fun AppListPreview(onNavigate: (String) -> Unit, viewModel: SettingsViewModel = hiltViewModel(), home: HomeViewModel = hiltViewModel()) {
    val pageId by home.appOverviewPageId.collectAsState()
    val background = rememberPageBackground(pageId, home)
    val dark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), background)
    val handedness by viewModel.appOverviewHandedness.collectAsState()
    val showAz by viewModel.appListShowAzIndex.collectAsState()
    val showButton by viewModel.appListShowSearchButton.collectAsState()
    val railOwn by viewModel.appListRailAppearance.collectAsState()
    val buttonOwn by viewModel.appListSearchButtonAppearance.collectAsState()
    val letter by viewModel.appListHeaderLetterAppearance.collectAsState()
    val rail = railOwn ?: automaticRailAppearance(dark)
    val button = buttonOwn ?: automaticRailAppearance(dark)
    val left = handedness == Handedness.LEFT
    val ink = resolveElementColors(ElementAppearance(fillMode = ElementFillMode.NONE), dark).contentColor

    PreviewPhone(background, onPage = { onNavigate("page") }) {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = if (left) 24.dp else 10.dp, end = if (left) 10.dp else 24.dp, top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("A", "B").forEach { initial ->
                val letterColors = resolveElementColors(letter, dark)
                Box(
                    modifier = Modifier.size(18.dp).look(letter, dark, CircleShape).clickable { onNavigate("section_letters") },
                    contentAlignment = Alignment.Center
                ) { Text(initial, fontSize = 9.sp, color = letterColors.contentColor) }
                repeat(3) { i ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(ink.copy(alpha = 0.35f)))
                        Box(Modifier.width(if (i % 2 == 0) 50.dp else 36.dp).height(5.dp).clip(CircleShape).background(ink.copy(alpha = 0.25f)))
                    }
                }
            }
        }
        if (showAz) {
            Box(
                modifier = Modifier
                    .align(if (left) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 5.dp)
                    .width(12.dp)
                    .fillMaxHeight(0.7f)
                    .look(rail, dark, RoundedCornerShape(6.dp))
                    .clickable { onNavigate("az_index") },
                contentAlignment = Alignment.Center
            ) {
                Text("A\n·\n·\nZ", fontSize = 6.sp, lineHeight = 7.sp, color = resolveElementColors(rail, dark).contentColor)
            }
        }
        if (showButton) {
            Box(
                modifier = Modifier
                    .align(if (left) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(28.dp)
                    .look(button, dark, CircleShape)
                    .clickable { onNavigate("search_button") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = resolveElementColors(button, dark).contentColor, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** The App grid in small: its icons in the set columns, the search field where it sits, and the
 *  page dots when it pages sideways. Tap a part to open its settings. */
@Composable
fun AppGridPreview(onNavigate: (String) -> Unit, viewModel: SettingsViewModel = hiltViewModel(), home: HomeViewModel = hiltViewModel()) {
    val pageId by home.appGridPageId.collectAsState()
    val background = rememberPageBackground(pageId, home)
    val dark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), background)
    val columns by viewModel.appGridColumns.collectAsState()
    val position by viewModel.appGridBarPosition.collectAsState()
    val barOwn by viewModel.appGridBarAppearance.collectAsState()
    val horizontal by viewModel.appGridHorizontal.collectAsState()
    val pageDots by viewModel.appGridPageDots.collectAsState()
    val showLabels by viewModel.appGridShowLabels.collectAsState()
    val bar = barOwn ?: automaticRailAppearance(dark)
    val ink = resolveElementColors(ElementAppearance(fillMode = ElementFillMode.NONE), dark).contentColor

    PreviewPhone(background, onPage = { onNavigate("page") }) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            @Composable
            fun Field() {
                Box(
                    modifier = Modifier.fillMaxWidth().height(16.dp).look(bar, dark, CircleShape).clickable { onNavigate("search_field") },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = resolveElementColors(bar, dark).contentColor, modifier = Modifier.padding(start = 5.dp).size(9.dp))
                }
            }
            if (position == AppGridBarPosition.TOP) Field()
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().clickable { onNavigate("grid") },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(columns) {
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(ink.copy(alpha = 0.35f)))
                                if (showLabels) Box(Modifier.padding(top = 3.dp).width(12.dp).height(3.dp).clip(CircleShape).background(ink.copy(alpha = 0.2f)))
                            }
                        }
                    }
                }
            }
            if (horizontal && pageDots) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)) {
                    repeat(3) { i -> Box(Modifier.size(width = if (i == 0) 9.dp else 4.dp, height = 4.dp).clip(CircleShape).background(ink.copy(alpha = if (i == 0) 0.8f else 0.35f))) }
                }
            }
            if (position == AppGridBarPosition.BOTTOM) Field()
            Spacer(Modifier.height(2.dp))
        }
    }
}
