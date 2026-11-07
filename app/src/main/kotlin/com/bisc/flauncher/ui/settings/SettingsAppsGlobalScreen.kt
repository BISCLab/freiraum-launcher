package com.bisc.flauncher.ui.settings

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

/** App icons for every app at once: a live preview of real apps on top, then the shape as picture
 *  tiles, the size, and the icon packs as cards with three of their icons. Each app can still get
 *  its own in its own settings. */
@Composable
fun SettingsAppsGlobalScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val iconShape by viewModel.iconShape.collectAsState()
    val iconSizePercent by viewModel.iconSizePercent.collectAsState()
    val activeIconPack by viewModel.activeIconPack.collectAsState()
    val iconPacks by viewModel.iconPacks.collectAsState()
    val apps by viewModel.labeledApps.collectAsState()
    // Well-known apps first, so the samples look familiar.
    val samples = remember(apps) {
        val preferred = listOf("com.android.chrome", "com.google.android.dialer", "com.google.android.apps.messaging", "com.google.android.GoogleCamera", "com.google.android.apps.maps", "com.android.settings")
        (apps.filter { it.componentName.packageName in preferred } + apps.sortedBy { it.label.lowercase() }).distinct().take(5)
    }

    SettingsSubScreen(title = stringResource(R.string.settings_icons_label), navController = navController, onBack = onBack) {
        IconsPreview(samples, iconSizePercent, iconShape, activeIconPack, viewModel)

        SettingsPreferenceGroup(title = stringResource(R.string.settings_icon_shape_label)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    IconShape.ROUNDED_SQUARE to R.string.settings_icon_shape_rounded_square,
                    IconShape.CIRCLE to R.string.settings_icon_shape_circle
                ).forEach { (shape, label) ->
                    ShapeTile(
                        label = stringResource(label),
                        shape = if (shape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(26),
                        sample = samples.firstOrNull(),
                        selected = iconShape == shape,
                        onClick = { viewModel.setIconShape(shape) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_icon_size_label)) {
            ExpandableIconSizeSlider(percent = iconSizePercent, onSettled = viewModel::setIconSizePercent)
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_section_icon_pack)) {
            val cards = listOf<IconPackInfo?>(null) + iconPacks
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                cards.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { pack ->
                            PackCard(
                                pack = pack,
                                samples = samples.take(3),
                                selected = activeIconPack == pack?.packageName,
                                onClick = { viewModel.selectIconPack(pack?.packageName) },
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Real apps as they look on the home screen right now: shape, size and pack. */
@Composable
private fun IconsPreview(samples: List<AppInfo>, sizePercent: Int, shape: IconShape, pack: String?, viewModel: SettingsViewModel) {
    val icons by produceState<List<ImageBitmap?>>(emptyList(), samples, shape, pack) {
        value = withContext(Dispatchers.IO) { samples.map { runCatching { viewModel.loadIcon(it.componentName) }.getOrNull() } }
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
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)
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

/** One shape as a picture: a sample icon cut to it. */
@Composable
private fun ShapeTile(label: String, shape: Shape, sample: AppInfo?, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val icon = rememberSystemIcon(sample?.componentName)
    SelectableCard(selected, onClick, modifier) {
        Box(Modifier.size(56.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(56.dp)) }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}

/** An icon pack as a card with three of its icons; null = the apps' own icons. */
@Composable
private fun PackCard(pack: IconPackInfo?, samples: List<AppInfo>, selected: Boolean, onClick: () -> Unit, viewModel: SettingsViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val ownIcons = samples.map { rememberSystemIcon(it.componentName) }
    val packIcons by produceState<List<ImageBitmap?>?>(null, pack?.packageName, samples) {
        val pkg = pack?.packageName ?: return@produceState
        value = withContext(Dispatchers.IO) {
            val mappings = runCatching { com.bisc.flauncher.util.IconPackAppFilterParser.parse(context, pkg) }.getOrDefault(emptyList())
            samples.map { app ->
                val exact = app.componentName.flattenToString()
                val drawable = mappings.firstOrNull { it.first == exact || it.first == app.componentName.flattenToShortString() }?.second
                    ?: mappings.firstOrNull { it.first.startsWith(app.componentName.packageName + "/") }?.second
                drawable?.let { runCatching { viewModel.loadPackIcon(pkg, it) }.getOrNull() }
            }
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
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
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
