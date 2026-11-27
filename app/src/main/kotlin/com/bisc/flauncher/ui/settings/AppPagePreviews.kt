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

