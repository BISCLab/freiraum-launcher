package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.ui.home.HomeViewModel

/** What every page has as ⓪: its own background (or the shared one with its shade) and the status bar. */
class PageDraft(
    val id: Long,
    val background: DraftField<BackgroundConfig?>,
    val shade: DraftField<Float?>,
    val hideStatus: DraftField<Boolean>,
    val global: BackgroundConfig
) {
    /** The background as the page shows it, from the draft. */
    val shown: BackgroundConfig get() = background.value ?: global.copy(shade = shade.value ?: global.shade)
    val keys = listOf("background", "shade", "status")
}

@Composable
fun rememberPageDraft(draft: StackDraft, home: HomeViewModel, id: Long): PageDraft {
    val hideStatus = draft.field("status", remember(id) { home.pageHideStatusBar(id) }.collectAsState(initial = false).value) { home.setPageHideStatusBar(id, it) }
    val background = draft.field<BackgroundConfig?>("background", remember(id) { home.pageBackgroundOverride(id) }.collectAsState(initial = null).value) {
        if (it == null) home.clearPageBackgroundConfig(id) else home.setPageBackgroundConfig(id, it)
    }
    val shade = draft.field<Float?>("shade", remember(id) { home.pageGlobalShade(id) }.collectAsState(initial = null).value) { it?.let { s -> home.setPageGlobalShade(id, s) } }
    val global by home.globalBackground.collectAsState()
    return PageDraft(id, background, shade, hideStatus, global)
}

/**
 * The options of ⓪ on every page, in the same order: what the page does ([before]), its background,
 * the status bar, then name and symbol. [kind] is null for pages whose name is set elsewhere.
 */
@Composable
internal fun ColumnScope.PageStackOptions(
    page: PageDraft,
    kind: LauncherPageKind?,
    navController: NavController,
    home: HomeViewModel,
    before: @Composable ColumnScope.() -> Unit = {}
) {
    before()
    BackgroundOverrideSection(
        override = page.background.value,
        global = page.global,
        onUseGlobal = { page.background.value = null },
        onCustomColor = { page.background.value = it },
        imageKey = "page_${page.id}",
        globalShade = page.shade.value,
        onGlobalShadeChange = { page.shade.value = it },
        groupTitle = stringResource(R.string.settings_section_global_background),
        onEditGlobal = { navController.navigate("settings/appearance/background") }
    )
    SettingsPreferenceGroup(title = stringResource(R.string.settings_statusbar_label)) {
        DraftSwitchRow(stringResource(R.string.settings_statusbar_hide_page), page.hideStatus.value, { page.hideStatus.value = it })
    }
    if (kind != null) {
        SettingsGroupLabel(stringResource(R.string.stack_name_label))
        UtilityPageIdentityGroup(kind = kind, navController = navController, viewModel = home)
    }
}

/** A switch row bound to a draft value. */
@Composable
internal fun DraftSwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, info: String? = null, enabled: Boolean = true, summary: String? = null) {
    SettingsPreference(
        icon = null,
        title = title,
        summary = summary,
        info = info,
        enabled = enabled,
        onClick = { onChange(!checked) },
        trailing = { Switch(checked = checked, enabled = enabled, onCheckedChange = onChange) }
    )
}

/** A number with minus and plus. */
@Composable
internal fun DraftStepperRow(title: String, value: String, onLess: () -> Unit, onMore: () -> Unit, lessEnabled: Boolean, moreEnabled: Boolean, summary: String? = null) {
    SettingsPreference(
        icon = null,
        title = title,
        summary = summary ?: value,
        onClick = null,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onLess, enabled = lessEnabled) { Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.a11y_less)) }
                IconButton(onClick = onMore, enabled = moreEnabled) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.a11y_more)) }
            }
        }
    )
}

/** The status bar's height on this device, at least a little. */
@Composable
internal fun replicaStatusHeight(): Dp = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }.coerceAtLeast(24.dp)

/** The status bar in plain shapes: the time on the left, signal and battery on the right. */
@Composable
internal fun StatusBarSketch(height: Dp, ink: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().height(height).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("12:30", color = ink, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(ink.copy(alpha = 0.8f)))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.width(22.dp).height(11.dp).clip(RoundedCornerShape(3.dp)).background(ink.copy(alpha = 0.8f)))
    }
}

/** A keyboard in plain shapes, where the real one opens while searching. */
@Composable
internal fun KeyboardSketch(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val key = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(
        modifier = modifier.fillMaxWidth().background(base).padding(horizontal = 6.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(10, 9, 9, 5).forEach { count ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                repeat(count) { i ->
                    val wide = count == 5 && i == 2
                    Box(Modifier.width(if (wide) 150.dp else 30.dp).height(44.dp).clip(RoundedCornerShape(8.dp)).background(key))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
