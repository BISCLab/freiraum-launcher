package com.bisc.flauncher.ui.settings

import com.bisc.flauncher.ui.marqueeWhenCut
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ColumnScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.IconPackInfo
import com.bisc.flauncher.data.settings.IconShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What is set for all apps, as a draft in a stack: shape, size, pack, the icon style, themed icons and dots. */
internal class AllAppsDraft(
    val shape: DraftField<IconShape>,
    val size: DraftField<Int>,
    val pack: DraftField<String?>,
    val style: DraftField<com.bisc.flauncher.data.settings.IconStyle>,
    val themed: DraftField<Boolean>,
    val dots: DraftField<Boolean>
) {
    val keys = listOf("all:shape", "all:size", "all:pack", "all:style", "all:themed", "all:dots")
}

@Composable
internal fun rememberAllAppsDraft(draft: StackDraft, viewModel: SettingsViewModel): AllAppsDraft = AllAppsDraft(
    shape = draft.field("all:shape", viewModel.iconShape.collectAsState().value, viewModel::setIconShape),
    size = draft.field("all:size", viewModel.iconSizePercent.collectAsState().value, viewModel::setIconSizePercent),
    pack = draft.field("all:pack", viewModel.activeIconPack.collectAsState().value, viewModel::selectIconPack),
    style = draft.field("all:style", viewModel.iconStyle.collectAsState().value, viewModel::setIconStyle),
    themed = draft.field("all:themed", viewModel.themedIcons.collectAsState().value, viewModel::setThemedIcons),
    dots = draft.field("all:dots", viewModel.showNotificationDots.collectAsState().value, viewModel::setShowNotificationDots)
)

/** Every shape with its name, in the order the tiles show them. */
@Composable
internal fun iconShapeChoices(): List<Pair<IconShape, String>> = listOf(
    IconShape.ROUNDED_SQUARE to stringResource(R.string.settings_icon_shape_rounded_square),
    IconShape.CIRCLE to stringResource(R.string.settings_icon_shape_circle),
    IconShape.SQUIRCLE to stringResource(R.string.settings_icon_shape_squircle),
    IconShape.SQUARE to stringResource(R.string.settings_icon_shape_square),
    IconShape.TEARDROP to stringResource(R.string.settings_icon_shape_teardrop),
    IconShape.LEAF to stringResource(R.string.settings_icon_shape_leaf)
)

/** A few well-known apps first, so the samples look familiar. */
@Composable
internal fun rememberIconSamples(viewModel: SettingsViewModel): List<AppInfo> {
    val apps by viewModel.labeledApps.collectAsState()
    return remember(apps) {
        val preferred = listOf("com.android.chrome", "com.google.android.dialer", "com.google.android.apps.messaging", "com.google.android.GoogleCamera", "com.google.android.apps.maps", "com.android.settings")
        val own = apps.filter { !com.bisc.flauncher.util.ProfileComponents.isProfile(it.componentName) }
        (own.filter { it.componentName.packageName in preferred } + own.sortedBy { it.label.lowercase() }).distinct().take(5)
    }
}

/** The samples as the stack's replica shows them: real apps in the shape, size, pack and style of the draft. */
@Composable
internal fun AllAppsSample(all: AllAppsDraft, viewModel: SettingsViewModel) {
    IconsPreview(rememberIconSamples(viewModel), all.size.value, all.shape.value, all.pack.value, viewModel, all.style.value)
}

/** ⓪ "All apps" in three tabs: the shape (with how round and how big), the icon pack, and extras
 *  (small icons filled, a border, themed icons, dots). */
@Composable
internal fun ColumnScope.AllAppsOptions(all: AllAppsDraft, viewModel: SettingsViewModel) {
    val iconPacks by viewModel.iconPacks.collectAsState()
    val samples = rememberIconSamples(viewModel)
    val context = LocalContext.current
    val style = all.style.value
    StackTabs(listOf(
        stringResource(R.string.icons_tab_shape) to {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_icon_shape_label)) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    iconShapeChoices().chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { (shape, label) ->
                                ShapeTile(
                                    label = label,
                                    shape = shape,
                                    iconPack = all.pack.value,
                                    viewModel = viewModel,
                                    sample = samples.firstOrNull(),
                                    selected = all.shape.value == shape,
                                    onClick = { all.shape.value = shape },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    style = style
                                )
                            }
                        }
                    }
                }
            }
            // How round: only where the shape has corners to round.
            if (all.shape.value == IconShape.ROUNDED_SQUARE) {
                SettingsPreferenceGroup(title = stringResource(R.string.icons_corner_label)) {
                    CornerPercentPicker(percent = style.cornerPercent, onChange = { all.style.value = style.copy(cornerPercent = it) })
                }
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_icon_size_label)) {
                ExpandableIconSizeSlider(percent = all.size.value, onSettled = { all.size.value = it })
            }
        },
        stringResource(R.string.settings_section_icon_pack) to {
            SettingsPreferenceGroup {
                val cards = listOf<IconPackInfo?>(null) + iconPacks
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    cards.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { pack ->
                                PackCard(
                                    pack = pack,
                                    samples = samples.take(3),
                                    selected = all.pack.value == pack?.packageName,
                                    onClick = { all.pack.value = pack?.packageName },
                                    viewModel = viewModel,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        stringResource(R.string.icons_tab_extras) to {
            SettingsPreferenceGroup(title = stringResource(R.string.icons_fill_title)) {
                DraftSwitchRow(stringResource(R.string.icons_fill_small), style.fillSmall, { all.style.value = style.copy(fillSmall = it) }, summary = stringResource(R.string.icons_fill_small_summary))
            }
            SettingsPreferenceGroup(title = stringResource(R.string.icons_border_title)) {
                DraftSwitchRow(stringResource(R.string.icons_border), style.border, { all.style.value = style.copy(border = it) }, summary = stringResource(R.string.icons_border_summary))
                if (style.border) {
                    SettingsChoiceRow(
                        title = stringResource(R.string.icons_border_width),
                        options = listOf(1 to stringResource(R.string.icons_border_thin), 2 to stringResource(R.string.icons_border_medium), 3 to stringResource(R.string.icons_border_thick)),
                        selected = style.borderWidth,
                        onSelect = { all.style.value = style.copy(borderWidth = it) }
                    )
                    ColorPickerRow(
                        title = stringResource(R.string.icons_border_color),
                        color = style.borderColor,
                        onColorChange = { all.style.value = style.copy(borderColor = it) }
                    )
                }
            }
            // Themed icons exist from Android 13 on.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                SettingsPreferenceGroup(title = stringResource(R.string.settings_themed_icons_label)) {
                    DraftSwitchRow(stringResource(R.string.settings_themed_icons), all.themed.value, { all.themed.value = it }, summary = stringResource(R.string.settings_themed_icons_summary))
                }
            }
            var tick by remember { androidx.compose.runtime.mutableIntStateOf(0) }
            // Checked again whenever the page comes back, e.g. after granting access in the system.
            androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
            val access = tick.let { androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_dots_label), info = stringResource(R.string.settings_dots_info)) {
                DraftSwitchRow(
                    stringResource(R.string.settings_dots_show), all.dots.value, { all.dots.value = it },
                    summary = if (all.dots.value && !access) stringResource(R.string.settings_dots_no_access) else null
                )
                if (all.dots.value && !access) {
                    SettingsPreference(icon = null, title = stringResource(R.string.settings_dots_grant), onClick = { com.bisc.flauncher.util.openNotificationAccessSettings(context) })
                }
            }
        }
    ))
}

/** Real apps as they look on the home screen right now: shape, size and pack. */
@Composable
private fun IconsPreview(samples: List<AppInfo>, sizePercent: Int, shape: IconShape, pack: String?, viewModel: SettingsViewModel, style: com.bisc.flauncher.data.settings.IconStyle) {
    // Instantly what is known (this choice rendered before, or the icon as it is now), then the
    // exact rendering, all icons at once.
    val icons by produceState(
        samples.map { viewModel.iconLoader.peekRender(it.componentName, shape, pack, style) ?: viewModel.iconLoader.peek(it.componentName) },
        samples, shape, pack, style, viewModel.iconLoader.revision
    ) {
        value = withContext(Dispatchers.Default) {
            samples.map { app -> async { runCatching { viewModel.iconLoader.render(app.componentName, shape, pack, style) }.getOrNull() } }.awaitAll()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHighest)))
            .padding(vertical = 22.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            samples.forEachIndexed { i, app ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    val size = 48.dp * (sizePercent / 100f)
                    Box(Modifier.size(size)) {
                        icons.getOrNull(i)?.let { Image(it, contentDescription = null, modifier = Modifier.size(size)) }
                    }
                    Text(
                        app.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp).marqueeWhenCut()
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberSystemIcon(component: ComponentName?): ImageBitmap? {
    val context = LocalContext.current
    return remember(component) {
        component?.let {
            runCatching { context.packageManager.getActivityIcon(it).toBitmap(width = 144, height = 144).asImageBitmap() }.getOrNull()
        }
    }
}

/** One shape as a picture: a sample icon drawn the way the launcher draws it with that shape and
 *  the chosen icon pack. */
@Composable
private fun ShapeTile(label: String, shape: IconShape, iconPack: String?, viewModel: SettingsViewModel, sample: AppInfo?, selected: Boolean, onClick: () -> Unit, modifier: Modifier, style: com.bisc.flauncher.data.settings.IconStyle) {
    val icon by produceState(sample?.let { viewModel.iconLoader.peekRender(it.componentName, shape, iconPack, style) }, sample?.componentName, shape, iconPack, style) {
        value = sample?.let { app -> withContext(Dispatchers.Default) { runCatching { viewModel.iconLoader.render(app.componentName, shape, iconPack, style) }.getOrNull() } }
    }
    SelectableCard(selected, onClick, modifier) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(56.dp)) }
        }
        // Room for two lines on every tile, so they are all the same height.
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, minLines = 2, modifier = Modifier.padding(top = 8.dp))
    }
}

/** An icon pack as a card with three of its icons; null = the apps' own icons. */
@Composable
private fun PackCard(pack: IconPackInfo?, samples: List<AppInfo>, selected: Boolean, onClick: () -> Unit, viewModel: SettingsViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val ownIcons = samples.map { rememberSystemIcon(it.componentName) }
    val packIcons by produceState<List<ImageBitmap?>?>(null, pack?.packageName, samples) {
        val pkg = pack?.packageName ?: return@produceState
        value = withContext(Dispatchers.Default) {
            samples.map { app -> viewModel.iconLoader.packDrawableFor(pkg, app.componentName)?.let { runCatching { viewModel.loadPackIcon(pkg, it) }.getOrNull() } }
        }
    }
    SelectableCard(selected, onClick, modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            samples.forEachIndexed { i, _ ->
                val icon = if (pack == null) ownIcons[i] else packIcons?.getOrNull(i) ?: ownIcons[i]
                Box(Modifier.size(32.dp)) { icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(32.dp)) } }
            }
        }
        Text(
            pack?.label ?: stringResource(R.string.settings_icon_pack_default),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp).marqueeWhenCut()
        )
    }
}

@Composable
private fun SelectableCard(selected: Boolean, onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

/** The dots on app icons: on or off, and the way to notification access while it is missing. */
@Composable
private fun NotificationDotsGroup(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val on by viewModel.showNotificationDots.collectAsState()
    var tick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    // Checked again whenever the page comes back, e.g. after granting access in the system.
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    val access = tick.let { androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }
    SettingsPreferenceGroup(title = stringResource(R.string.settings_dots_label), info = stringResource(R.string.settings_dots_info)) {
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_dots_show),
            summary = if (on && !access) stringResource(R.string.settings_dots_no_access) else null,
            onClick = { viewModel.setShowNotificationDots(!on) },
            trailing = { androidx.compose.material3.Switch(checked = on, onCheckedChange = viewModel::setShowNotificationDots) }
        )
        if (on && !access) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_dots_grant),
                onClick = { com.bisc.flauncher.util.openNotificationAccessSettings(context) }
            )
        }
    }
}
