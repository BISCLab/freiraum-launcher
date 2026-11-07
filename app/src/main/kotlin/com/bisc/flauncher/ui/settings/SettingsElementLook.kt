package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.designGlow

/** Which part the preview on top of an [ElementLookScreen] draws. */
enum class LookSampleKind { DOCK, PANEL, FOLDER, FIELD, RAIL, CARDS }

/**
 * One page for everything about how a part looks: a live preview on top, then fill, text color,
 * opacity and border, then its corners, and a single Save for all of it. [links] sit right under
 * the preview (other pages of the part, like its elements), [extras] at the end.
 */
@Composable
internal fun ElementLookScreen(
    title: String,
    navController: NavController,
    onBack: () -> Unit,
    saved: ElementAppearance,
    /** Null = the part has no corners to set here. */
    savedCorner: Int?,
    onSave: (ElementAppearance, Int?) -> Unit,
    kind: LookSampleKind,
    cornerMax: Int = 100,
    allowNone: Boolean = true,
    links: @Composable ColumnScope.() -> Unit = {},
    extras: @Composable ColumnScope.() -> Unit = {}
) {
    var look by rememberDraft(saved)
    var corner by rememberDraft(savedCorner)
    SettingsSubScreen(
        title = title,
        navController = navController,
        onBack = onBack,
        onSave = { onSave(look, corner) },
        dirty = look != saved || corner != savedCorner
    ) {
        LookSample(kind, look, corner ?: 100)
        links()
        ElementAppearanceSection(
            appearance = look,
            onAppearanceChange = { look = it },
            styleGroupTitle = stringResource(R.string.settings_element_fill_label),
            allowNone = allowNone
        )
        corner?.let { percent ->
            SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
                ExpandablePercentSlider(percent = percent, onSettled = { corner = it }, max = cornerMax)
            }
        }
        extras()
    }
}

/** The part in small on a neutral backdrop, drawn from the draft so every change shows at once. */
@Composable
internal fun LookSample(kind: LookSampleKind, look: ElementAppearance, cornerPercent: Int) {
    val colors = resolveElementColors(look)
    val pct = cornerPercent.coerceIn(0, 100) / 100f
    val backdrop = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHighest))
    val ink = colors.contentColor
    @Composable
    fun Surface(modifier: Modifier, shape: Shape, target: String, content: @Composable () -> Unit) {
        Box(
            modifier = modifier
                .designGlow(shape, target)
                .clip(shape)
                .background(colors.background, alpha = colors.backgroundAlpha)
                .then(if (look.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier),
            contentAlignment = Alignment.Center
        ) { content() }
    }
    @Composable
    fun Dots(count: Int, size: Dp = 22.dp, round: Dp = 7.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(count) { Box(Modifier.size(size).clip(RoundedCornerShape(round)).background(ink.copy(alpha = 0.35f))) }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backdrop)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        when (kind) {
            LookSampleKind.DOCK -> Surface(Modifier.fillMaxWidth().height(64.dp), RoundedCornerShape(28.dp * pct), "dock") { Dots(5) }
            LookSampleKind.PANEL -> Surface(Modifier.fillMaxWidth().height(104.dp), RoundedCornerShape(28.dp * pct), "panel") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Dots(5); Dots(5) }
            }
            LookSampleKind.FOLDER -> Surface(Modifier.width(200.dp).height(104.dp), RoundedCornerShape(24.dp), "folders") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Dots(4); Dots(4) }
            }
            LookSampleKind.FIELD -> Surface(Modifier.fillMaxWidth().height(52.dp), RoundedCornerShape(26.dp * pct), "search") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = ink)
                    Text(stringResource(R.string.search_field_hint), color = ink.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyLarge)
                }
            }
            LookSampleKind.RAIL -> Surface(Modifier.width(44.dp).height(110.dp), RoundedCornerShape(22.dp * pct), "rail") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    listOf(Color(0xFFFFB300), Color(0xFF4F86F7), Color(0xFF34A853), Color(0xFF00ACC1)).forEach {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(it))
                    }
                }
            }
            LookSampleKind.CARDS -> Surface(Modifier.fillMaxWidth().height(96.dp), RoundedCornerShape(28.dp * pct), "cards") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.search_section_apps), color = ink.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Dots(4, size = 30.dp, round = 9.dp)
                }
            }
        }
    }
}
