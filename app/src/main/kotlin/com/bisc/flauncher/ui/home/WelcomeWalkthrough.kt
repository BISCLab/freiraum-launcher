package com.bisc.flauncher.ui.home

import androidx.compose.foundation.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.FilterChip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.bisc.flauncher.data.wallpaper.WallpaperPackRepository
import com.bisc.flauncher.ui.settings.ChoiceDialog
import com.bisc.flauncher.ui.settings.DesignPreview
import com.bisc.flauncher.ui.settings.DesignsViewModel
import com.bisc.flauncher.ui.settings.WallpaperPacksViewModel
import com.bisc.flauncher.ui.theme.LocalSettingsThemeMode
import com.bisc.flauncher.ui.theme.settingsDark
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.ui.MaxContentWidth
import com.bisc.flauncher.ui.settings.GestureMarker
import com.bisc.flauncher.ui.settings.GesturePhone
import com.bisc.flauncher.ui.settings.PermissionGroups
import com.bisc.flauncher.ui.settings.SettingsViewModel
import com.bisc.flauncher.ui.settings.gestureActionSummary
import kotlinx.coroutines.launch

private enum class WelcomeStep { HELLO, GESTURES, SETTINGS, DESIGN, WALLPAPER, SHAPE, PERMISSIONS, LOCK_SCREEN }

/** The first-start tour: welcome with a quick setup, the gestures, where the settings are, then
 *  design, wallpaper pack and shape to choose, the permissions, and the lock screen when the pack
 *  has a picture for it. Skippable at any point; shown once. */
@Composable
fun WelcomeWalkthrough(
    onDone: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onboarding: OnboardingViewModel = hiltViewModel(),
    designsViewModel: DesignsViewModel = hiltViewModel(),
    packsViewModel: WallpaperPacksViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val packs by packsViewModel.packs.collectAsState()
    val activePackId by packsViewModel.activeId.collectAsState()
    val lockPicture = packs.firstOrNull { it.id == activePackId }?.lock
    val steps = WelcomeStep.entries.filter { it != WelcomeStep.LOCK_SCREEN || lockPicture != null }
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    val dark = settingsDark(LocalSettingsThemeMode.current, isSystemInDarkTheme())
    var lockAnswered by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
                val step = steps.getOrElse(pagerState.currentPage) { steps.last() }
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step == WelcomeStep.HELLO) {
                        TextButton(onClick = { onboarding.setTourDark(!dark) }) {
                            Icon(if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(if (dark) R.string.settings_theme_light else R.string.settings_theme_dark))
                        }
                        Spacer(Modifier.weight(1f))
                        LanguageButton()
                    } else {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDone) { Text(stringResource(R.string.welcome_skip)) }
                    }
                }
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
                    when (steps[index]) {
                        WelcomeStep.HELLO -> HelloPage(
                            onQuick = { onboarding.quickSetup(dark, onDone) },
                            onOwn = { scope.launch { pagerState.animateScrollToPage(1) } }
                        )
                        WelcomeStep.GESTURES -> WelcomePage(R.string.welcome_gestures_title, R.string.welcome_gestures_text) {
                            GesturesPreview(settingsViewModel)
                        }
                        WelcomeStep.SETTINGS -> WelcomePage(R.string.welcome_settings_title, R.string.welcome_settings_text) {
                            PhoneFrame { HomePreview(highlightSettings = true) }
                        }
                        WelcomeStep.DESIGN -> WelcomePage(R.string.welcome_design_title, R.string.welcome_design_text, illustrationWeight = 0f) {
                            DesignChoice(designsViewModel, dark)
                        }
                        WelcomeStep.WALLPAPER -> WelcomePage(R.string.welcome_wallpaper_title, R.string.welcome_wallpaper_text, illustrationWeight = 0f) {
                            WallpaperChoice(packsViewModel, dark)
                        }
                        WelcomeStep.SHAPE -> WelcomePage(R.string.welcome_shape_title, R.string.welcome_shape_text, illustrationWeight = 0f) {
                            ShapeChoice(onboarding, designsViewModel)
                        }
                        WelcomeStep.PERMISSIONS -> WelcomePage(R.string.welcome_permissions_title, R.string.welcome_permissions_text, illustrationWeight = 0f) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PermissionGroups(viewModel = settingsViewModel, purposeVisible = true)
                            }
                        }
                        WelcomeStep.LOCK_SCREEN -> WelcomePage(R.string.welcome_lock_title, R.string.welcome_lock_text) {
                            PhoneFrame {
                                PictureThumb(lockPicture, Modifier.fillMaxSize())
                                Text(
                                    "12:30",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp)
                                )
                            }
                        }
                    }
                }
                if (step == WelcomeStep.LOCK_SCREEN && !lockAnswered) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { lockAnswered = true; onDone() }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.lock_wallpaper_offer_no))
                        }
                        Button(onClick = {
                            lockAnswered = true
                            lockPicture?.let { file ->
                                scope.launch {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        runCatching {
                                            file.inputStream().use {
                                                android.app.WallpaperManager.getInstance(context).setStream(it, null, true, android.app.WallpaperManager.FLAG_LOCK)
                                            }
                                        }
                                    }
                                    onDone()
                                }
                            } ?: onDone()
                        }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.lock_wallpaper_offer_yes))
                        }
                    }
                }
                if (step != WelcomeStep.HELLO) Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        steps.indices.forEach { i ->
                            val active = i == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(width = if (active) 20.dp else 8.dp, height = 8.dp)
                                    .clip(CircleShape)
                                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }
                    if (pagerState.currentPage < steps.lastIndex) {
                        Button(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                            Text(stringResource(R.string.welcome_next))
                        }
                    } else if (step != WelcomeStep.LOCK_SCREEN) {
                        Button(onClick = onDone) { Text(stringResource(R.string.welcome_done)) }
                    }
                }
            }
        }
    }
}

/** Logo, greeting, and the two ways in: everything recommended at once, or step by step. */
@Composable
private fun HelloPage(onQuick: () -> Unit, onOwn: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(112.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xFF0758F5)),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.welcome_hello_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.welcome_hello_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(36.dp))
        Button(onClick = onQuick, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.welcome_quick_setup), style = MaterialTheme.typography.titleMedium)
        }
        Text(
            stringResource(R.string.welcome_quick_setup_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        OutlinedButton(onClick = onOwn, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.welcome_own_setup))
        }
    }
}

/** The app language behind a flag; "automatic" follows the phone. */
@Composable
private fun LanguageButton() {
    val context = LocalContext.current
    var language by remember { mutableStateOf(com.bisc.flauncher.util.AppLanguage.current(context)) }
    var open by remember { mutableStateOf(false) }
    val languages = listOf("" to stringResource(R.string.settings_language_system), "en" to "English", "de" to "Deutsch")
    val shown = language.ifBlank { LocalConfiguration.current.locales.get(0).language }
    val flag = when (shown) { "de" -> "🇩🇪"; "en" -> "🇬🇧"; else -> "🌐" }
    TextButton(onClick = { open = true }) {
        Text(flag, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(6.dp))
        Text(languages.firstOrNull { it.first == language }?.second ?: languages.first().second)
    }
    if (open) {
        ChoiceDialog(
            title = stringResource(R.string.settings_language_label),
            options = languages,
            selected = language,
            onPick = { tag ->
                open = false
                language = tag
                (context as? android.app.Activity)?.let { com.bisc.flauncher.util.AppLanguage.set(it, tag) }
            },
            onDismiss = { open = false }
        )
    }
}

private const val RELEASES_URL = "https://github.com/BISCLab/freiraum-launcher/releases/latest"

/** Bundled designs to tap, the downloadable ones with a way to get them, and "none". */
@Composable
private fun DesignChoice(viewModel: DesignsViewModel, dark: Boolean) {
    val context = LocalContext.current
    val designs by viewModel.designs.collectAsState()
    val activeId by viewModel.activeId.collectAsState()
    val german = LocalConfiguration.current.locales.get(0).language == "de"
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri) { name ->
            if (name == null) android.widget.Toast.makeText(context, R.string.designs_import_failed, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    // Neon is a download; it shows up as a real tile once added.
    val missing = listOf("neon-light" to R.string.welcome_design_neon_light, "neon-dark" to R.string.welcome_design_neon_dark)
        .filter { (id, _) -> designs.none { it.id == id } }
    val outline = MaterialTheme.colorScheme.outline
    ChoiceGrid(buildList {
        designs.sortedBy { (it.theme == com.bisc.flauncher.ui.theme.ThemePreference.DARK) != dark }.forEach { design ->
            add(Tile(design.displayName(german), design.id == activeId, { viewModel.apply(design) }) { DesignPreview(design, Modifier.fillMaxWidth()) })
        }
        missing.forEach { (_, label) ->
            add(Tile(stringResource(label), false, { openUrl(context, RELEASES_URL) }) { DownloadPlaceholder() })
        }
        add(Tile(stringResource(R.string.welcome_design_none), activeId == null, { if (activeId != null) viewModel.restoreMine() }) {
            Box(Modifier.fillMaxWidth().aspectRatio(0.5f).clip(RoundedCornerShape(20.dp)).border(2.dp, outline, RoundedCornerShape(20.dp)))
        })
    })
    if (missing.isNotEmpty()) {
        TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.welcome_pick_file)) }
    }
}

/** Installed packs, the official ones to download, and plain backgrounds. */
@Composable
private fun WallpaperChoice(viewModel: WallpaperPacksViewModel, dark: Boolean) {
    val context = LocalContext.current
    val packs by viewModel.packs.collectAsState()
    val activeId by viewModel.activeId.collectAsState()
    val german = LocalConfiguration.current.locales.get(0).language == "de"
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri) { name ->
            if (name == null) android.widget.Toast.makeText(context, R.string.packs_import_failed, android.widget.Toast.LENGTH_LONG).show()
            else viewModel.packs.value.firstOrNull { it.name == name }?.let { viewModel.apply(it, lockScreen = false) {} }
        }
    }
    val missing = listOf("modern-light" to R.string.welcome_pack_modern_light, "modern-dark" to R.string.welcome_pack_modern_dark)
        .filter { (id, _) -> packs.none { it.id == id } }
        .sortedBy { (id, _) -> (id == "modern-dark") != dark }
    val outline = MaterialTheme.colorScheme.outline
    ChoiceGrid(buildList {
        packs.forEach { pack ->
            add(Tile(pack.displayName(german), pack.id == activeId, { viewModel.apply(pack, lockScreen = false) {} }) {
                PictureThumb(pack.preview, Modifier.fillMaxWidth().aspectRatio(0.5f).clip(RoundedCornerShape(20.dp)))
            })
        }
        missing.forEach { (_, label) ->
            add(Tile(stringResource(label), false, { openUrl(context, RELEASES_URL) }) { DownloadPlaceholder() })
        }
        add(Tile(stringResource(R.string.welcome_pack_plain), activeId == WallpaperPackRepository.PLAIN || activeId == null, { viewModel.applyPlain {} }) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(0.5f).clip(RoundedCornerShape(20.dp))
                    .background(Color(com.bisc.flauncher.ui.theme.backgroundColorFor(dark)))
                    .border(2.dp, outline, RoundedCornerShape(20.dp))
            )
        })
    })
    TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.welcome_pick_file)) }
}

/** "Like the design", or one of three shapes, optionally part by part. */
@Composable
private fun ShapeChoice(viewModel: OnboardingViewModel, designsViewModel: DesignsViewModel) {
    val designs by designsViewModel.designs.collectAsState()
    val activeId by designsViewModel.activeId.collectAsState()
    var chosen by remember { mutableStateOf<ShapePreset?>(null) }
    var parts by remember { mutableStateOf<Map<ShapePart, ShapePreset>>(emptyMap()) }
    var single by remember { mutableStateOf(false) }
    ChoiceGrid(columns = 2, tiles = buildList {
        add(Tile(stringResource(R.string.welcome_shape_design), chosen == null && parts.isEmpty(), {
            chosen = null
            parts = emptyMap()
            designs.firstOrNull { it.id == activeId }?.let(designsViewModel::apply)
        }) { ShapeSample(null) })
        ShapePreset.entries.forEach { preset ->
            add(Tile(stringResource(preset.label()), chosen == preset && parts.isEmpty(), {
                chosen = preset
                parts = emptyMap()
                viewModel.applyShape(ShapePart.entries.associateWith { preset })
            }) { ShapeSample(preset) })
        }
    })
    TextButton(onClick = { single = !single }) {
        Text(stringResource(R.string.welcome_shape_single))
        Icon(if (single) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
    }
    if (single) Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ShapePart.entries.forEach { part ->
            val current = parts[part] ?: chosen
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(part.label()), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShapePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = current == preset,
                            onClick = {
                                parts = parts + (part to preset)
                                viewModel.applyShape(mapOf(part to preset))
                            },
                            label = { Text(stringResource(preset.label())) }
                        )
                    }
                }
            }
        }
    }
}

private fun ShapePreset.label() = when (this) {
    ShapePreset.ROUND -> R.string.welcome_shape_round
    ShapePreset.SOFT -> R.string.welcome_shape_soft
    ShapePreset.SQUARE -> R.string.welcome_shape_square
}

private fun ShapePart.label() = when (this) {
    ShapePart.DOCK -> R.string.welcome_shape_part_dock
    ShapePart.WIDGETS -> R.string.welcome_shape_part_widgets
    ShapePart.SEARCH_BUTTON -> R.string.welcome_shape_part_search_button
    ShapePart.TEXT_SEARCH -> R.string.welcome_shape_part_text_search
    ShapePart.FOLDERS -> R.string.welcome_shape_part_folders
}

/** A search pill, a widget and a Dock with the preset's corners; null shows a question of taste. */
@Composable
private fun ShapeSample(preset: ShapePreset?) {
    val elements = preset?.elements ?: 100
    val widgets = preset?.widgets ?: 10
    val ink = MaterialTheme.colorScheme.primary.copy(alpha = if (preset == null) 0.35f else 0.6f)
    Column(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(percent = elements / 2)).background(ink))
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(percent = widgets / 2)).background(ink))
        Box(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(percent = elements / 2)).background(ink))
    }
}

private class Tile(val label: String, val selected: Boolean, val onClick: () -> Unit, val picture: @Composable () -> Unit)

/** Tiles in rows of [columns]; a short last row keeps the tile size. */
@Composable
private fun ChoiceGrid(tiles: List<Tile>, columns: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        tiles.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { tile -> ChoiceTile(tile) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ChoiceTile(tile: Tile) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .border(if (tile.selected) 3.dp else 0.dp, if (tile.selected) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .clickable(onClick = tile.onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        tile.picture()
        Text(
            tile.label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun DownloadPlaceholder() {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.5f).clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.welcome_download), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PictureThumb(file: java.io.File?, modifier: Modifier) {
    val bitmap by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, file) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            file?.takeIf { it.isFile }?.let {
                runCatching {
                    val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                    android.graphics.BitmapFactory.decodeFile(it.absolutePath, opts)?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
}

/** One step: the picture on top, a title and a short text below it. */
@Composable
private fun WelcomePage(titleRes: Int, textRes: Int, illustrationWeight: Float = 1f, illustration: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (illustrationWeight > 0f) {
            Box(modifier = Modifier.weight(illustrationWeight).fillMaxWidth(), contentAlignment = Alignment.Center) { illustration() }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (illustrationWeight > 0f) Modifier else Modifier.weight(1f).verticalScroll(rememberScrollState()))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                stringResource(textRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (illustrationWeight == 0f) {
                Spacer(Modifier.height(8.dp))
                illustration()
            }
        }
    }
}

/** A phone outline, the same as on the gestures screen, sized to the space it gets. */
@Composable
private fun PhoneFrame(content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxHeight()
            .aspectRatio(0.52f, matchHeightConstraintsFirst = true)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(3.dp, MaterialTheme.colorScheme.outline, shape),
        content = content
    )
}

/** The home screen in small: a soft backdrop and a Dock, optionally with the settings button
 *  picked out. */
@Composable
private fun BoxScope.HomePreview(highlightSettings: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFDCE6F5), Color(0xFFF4F3EF))))
    )
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(4) {
            Box(
                modifier = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF0758F5).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Apps, contentDescription = null, tint = Color(0xFF0758F5).copy(alpha = 0.6f), modifier = Modifier.size(14.dp)) }
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF6B7280))
                .then(if (highlightSettings) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
    }
}

/** The gestures screen's phone, read-only, with what each gesture does right now. */
@Composable
private fun GesturesPreview(viewModel: SettingsViewModel) {
    val gestureActions by viewModel.gestureActions.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val labeledApps by viewModel.labeledApps.collectAsState()
    val homePanelRow by viewModel.homePanelRow.collectAsState()
    val panelLabel = stringResource(R.string.welcome_gesture_panel)
    val markers = HomeGesture.entries.associateWith { gesture ->
        // Swiping down belongs to the Panel while it's on, like on the gestures screen.
        val summary = if (gesture == HomeGesture.SWIPE_DOWN && homePanelRow?.enabled == true) panelLabel
            else gestureActionSummary(gestureActions[gesture] ?: GestureAction.NONE, allPages, labeledApps)
        GestureMarker(summary = summary, enabled = true)
    }
    Box(modifier = Modifier.fillMaxHeight().aspectRatio(0.52f, matchHeightConstraintsFirst = true), contentAlignment = Alignment.Center) {
        GesturePhone(markers = markers, onEdit = {}, widthFraction = 1f)
    }
}
