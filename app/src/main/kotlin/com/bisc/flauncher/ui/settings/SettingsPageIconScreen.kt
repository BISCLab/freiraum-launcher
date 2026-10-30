package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.PageIconGlyph
import com.bisc.flauncher.ui.home.dock.PageIconGlyphImage
import com.bisc.flauncher.ui.home.dock.defaultColor

/** A page's own icon - which symbol, background color, symbol color - the look it shows when a
 *  dock slot links to it. Free choice among the 4 symbols, not tied to the page's own kind. */
@Composable
fun SettingsPageIconScreen(pageId: Long, navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val pages by viewModel.allPages.collectAsState()
    val page = pages.find { it.id == pageId }

    if (page == null) {
        SettingsSubScreen(title = stringResource(R.string.settings_page_icon_title), navController = navController, onBack = onBack) {}
        return
    }

    val savedGlyph = page.iconGlyph?.let { runCatching { PageIconGlyph.valueOf(it) }.getOrNull() } ?: PageIconGlyph.forPageKind(page.kind)
    val saved = Triple(savedGlyph, page.iconBackgroundColor ?: savedGlyph.defaultColor().toArgb(), page.iconColor ?: android.graphics.Color.WHITE)
    var draft by rememberDraft(saved)
    val (glyph, bgColor, iconColor) = draft

    fun update(newGlyph: PageIconGlyph = glyph, newBg: Int = bgColor, newIcon: Int = iconColor) {
        draft = Triple(newGlyph, newBg, newIcon)
    }

    SettingsSubScreen(
        title = stringResource(R.string.settings_page_icon_title),
        navController = navController,
        onBack = onBack,
        onSave = { viewModel.setPageIcon(pageId, glyph, bgColor, iconColor) },
        dirty = draft != saved
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(bgColor)),
                contentAlignment = Alignment.Center
            ) {
                PageIconGlyphImage(glyph = glyph, tint = Color(iconColor), modifier = Modifier.size(40.dp))
            }
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_page_icon_symbol_label)) {
            PageIconGlyph.entries.forEach { candidate ->
                RadioPreference(pageIconGlyphLabel(candidate), candidate, glyph, { update(newGlyph = it) })
            }
        }

        ColorPickerSection(
            title = stringResource(R.string.settings_page_icon_background_color),
            color = bgColor,
            onColorChange = { update(newBg = it) }
        )

        ColorPickerSection(
            title = stringResource(R.string.settings_page_icon_symbol_color),
            color = iconColor,
            onColorChange = { update(newIcon = it) }
        )
    }
}

@Composable
private fun pageIconGlyphLabel(glyph: PageIconGlyph): String = when (glyph) {
    PageIconGlyph.SEARCH -> stringResource(R.string.settings_page_icon_glyph_search)
    PageIconGlyph.APP_DRAWER -> stringResource(R.string.settings_page_icon_glyph_app_drawer)
    PageIconGlyph.APP_LIST -> stringResource(R.string.settings_page_icon_glyph_app_list)
    PageIconGlyph.WIDGET_PAGE -> stringResource(R.string.settings_page_icon_glyph_widget_page)
}
