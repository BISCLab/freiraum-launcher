package com.bisc.flauncher.ui.settings

import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.designGlow

/** Which part the preview on top of an [ElementLookScreen] draws. */
enum class LookSampleKind { DOCK, PANEL, FOLDER, FIELD, RAIL, CARDS, AZ_INDEX, SEARCH_BUTTON, LETTER, BUBBLE }

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
    /** Whether the page the part sits on is dark; the preview's backdrop and every "automatic"
     *  color follow it. Null = like the settings themselves. */
    backdropDark: Boolean? = null,
    /** Roundness the preview shows when the part has no corners to set here. */
    previewCorner: Int = 100,
    /** Whether the part has a look of its own (true) or the automatic one (false); null = the
     *  part has no automatic look to go back to. */
    custom: Boolean? = null,
    onResetLook: () -> Unit = {},
    links: @Composable ColumnScope.() -> Unit = {},
    extras: @Composable ColumnScope.() -> Unit = {}
) {
    var look by rememberDraft(saved)
    var corner by rememberDraft(savedCorner)
    androidx.compose.runtime.CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalSurfaceIsDark provides backdropDark) {
    SettingsSubScreen(
        title = title,
        navController = navController,
        onBack = onBack,
        onSave = { onSave(look, corner) },
        dirty = look != saved || corner != savedCorner,
        // The preview stays in view while the settings below are scrolled through.
        header = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LookSample(kind, look, corner ?: previewCorner, backdropDark)
                if (custom != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(if (custom) R.string.settings_look_custom else R.string.settings_look_automatic),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (custom) AutoResetChip(onClick = onResetLook, label = stringResource(R.string.settings_look_reset))
                    }
                }
            }
        }
    ) {
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
}

/** The part in small on a neutral backdrop, drawn from the draft so every change shows at once. */
@Composable
internal fun LookSample(kind: LookSampleKind, look: ElementAppearance, cornerPercent: Int, backdropDark: Boolean? = null) {
    val colors = if (backdropDark != null) resolveElementColors(look, backdropDark) else resolveElementColors(look)
    val pct = cornerPercent.coerceIn(0, 100) / 100f
    val backdrop = when (backdropDark) {
        true -> Brush.verticalGradient(listOf(Color(0xFF2E2E33), Color(0xFF16161A)))
        false -> Brush.verticalGradient(listOf(Color(0xFFF6F6F8), Color(0xFFDDDDE2)))
        null -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHighest))
    }
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
            .height(136.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backdrop)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        when (kind) {
            LookSampleKind.DOCK -> Surface(Modifier.fillMaxWidth().height(64.dp), com.bisc.flauncher.ui.theme.PartCornerShape(cornerPercent), "dock") { Dots(5) }
            LookSampleKind.PANEL -> Surface(Modifier.fillMaxWidth().height(104.dp), com.bisc.flauncher.ui.theme.PartCornerShape(cornerPercent), "panel") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Dots(5); Dots(5) }
            }
            LookSampleKind.FOLDER -> Surface(Modifier.width(200.dp).height(104.dp), com.bisc.flauncher.ui.theme.PartCornerShape(cornerPercent), "folders") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Dots(4); Dots(4) }
            }
            LookSampleKind.FIELD -> Surface(Modifier.fillMaxWidth().height(52.dp), com.bisc.flauncher.ui.theme.PartCornerShape(cornerPercent), "search") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = ink)
                    Text(stringResource(R.string.search_field_hint), color = ink.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyLarge)
                }
            }
            LookSampleKind.RAIL -> Surface(Modifier.width(28.dp).height(120.dp), com.bisc.flauncher.ui.theme.PartCornerShape(cornerPercent), "rail") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    listOf(Color(0xFFFFB300), Color(0xFF4F86F7), Color(0xFF34A853), Color(0xFF00ACC1)).forEach {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(it))
                    }
                }
            }
            LookSampleKind.AZ_INDEX -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                AppRowsHint(backdropDark)
                Surface(Modifier.width(28.dp).height(104.dp), com.bisc.flauncher.ui.theme.PartCornerShape(cornerPercent), "rail") {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        listOf("A", "B", "C", "D", "E").forEach { Text(it, color = ink, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
            LookSampleKind.SEARCH_BUTTON -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                AppRowsHint(backdropDark)
                Surface(Modifier.size(56.dp), com.bisc.flauncher.ui.theme.PartCornerShape(cornerPercent), "buttons") {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = ink)
                }
            }
            LookSampleKind.LETTER -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(Modifier.size(40.dp), CircleShape, "buttons") {
                    Text("A", color = ink, style = MaterialTheme.typography.titleMedium)
                }
                AppRowsHint(backdropDark)
            }
            LookSampleKind.BUBBLE -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                Surface(Modifier.height(48.dp), CircleShape, "cards") {
                    Row(Modifier.padding(start = 8.dp, end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF00ACC1).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF00ACC1), modifier = Modifier.size(20.dp))
                        }
                        Text(stringResource(R.string.search_filter_web), color = ink, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 10.dp))
                    }
                }
                // The magnified symbol on the rail, on the same ground.
                Surface(Modifier.size(38.dp), CircleShape, "rail") {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF00ACC1), modifier = Modifier.size(22.dp))
                }
            }
            LookSampleKind.CARDS -> Surface(Modifier.fillMaxWidth().height(96.dp), com.bisc.flauncher.ui.theme.PartCornerShape(cornerPercent), "cards") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.search_section_apps), color = ink.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Dots(4, size = 30.dp, round = 9.dp)
                }
            }
        }
    }
}

/** A few faint app rows beside the part, so it reads as sitting on the list. */
@Composable
private fun AppRowsHint(dark: Boolean?) {
    val ink = (if (dark ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)) Color.White else Color.Black).copy(alpha = 0.22f)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { i ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(ink))
                Box(Modifier.width(if (i == 1) 70.dp else 96.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(ink))
            }
        }
    }
}

/** The launcher's popup menus: the long press on an app, on an entry in a folder, on a note. */
@Composable
fun SettingsMenuLookScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val own by viewModel.menuAppearance.collectAsState()
    val automaticFill = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
    val saved = remember(own) { own ?: ElementAppearance(fill = com.bisc.flauncher.data.model.ColorFill(color = automaticFill)) }
    ElementLookScreen(
        title = stringResource(R.string.settings_menus_title),
        navController = navController,
        onBack = onBack,
        saved = saved,
        savedCorner = null,
        onSave = { look, _ -> viewModel.setMenuAppearance(look) },
        kind = LookSampleKind.CARDS,
        allowNone = false,
        previewCorner = 85,
        custom = own != null,
        onResetLook = viewModel::resetMenuAppearance
    )
}
