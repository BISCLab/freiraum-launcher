package com.bisc.flauncher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.sp
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
    val tourPrefs = remember { context.getSharedPreferences("welcome_tour", android.content.Context.MODE_PRIVATE) }
    val pagerState = rememberPagerState(initialPage = tourPrefs.getInt("step", 0).coerceIn(0, steps.lastIndex)) { steps.size }
    LaunchedEffect(pagerState.currentPage) { tourPrefs.edit().putInt("step", pagerState.currentPage).apply() }
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
                            DesignChoice(designsViewModel, onboarding, dark)
                        }
                        WelcomeStep.WALLPAPER -> WelcomePage(R.string.welcome_wallpaper_title, R.string.welcome_wallpaper_text, illustrationWeight = 0f) {
                            WallpaperChoice(packsViewModel, dark)
                        }
                        WelcomeStep.SHAPE -> WelcomePage(R.string.welcome_shape_title, R.string.welcome_shape_text, illustrationWeight = 0f) {
                            ShapeChoice(onboarding)
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

/** Every design side by side to swipe through: the bundled ones, the downloadable ones with a way
 *  to get them. One is always chosen - Modern in the tour's mode to begin with. */
@Composable
private fun DesignChoice(viewModel: DesignsViewModel, onboarding: OnboardingViewModel, dark: Boolean) {
    val context = LocalContext.current
    val designs by viewModel.designs.collectAsState()
    val activeId by viewModel.activeId.collectAsState()
    val german = LocalConfiguration.current.locales.get(0).language == "de"
    LaunchedEffect(Unit) { onboarding.ensureDesign(dark) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri) { name ->
            if (name == null) android.widget.Toast.makeText(context, R.string.designs_import_failed, android.widget.Toast.LENGTH_LONG).show()
            else viewModel.designs.value.firstOrNull { it.name == name }?.let(viewModel::apply)
        }
    }
    // Neon is a download; it shows up as a real tile once added.
    val missing = listOf("neon-light" to R.string.welcome_design_neon_light, "neon-dark" to R.string.welcome_design_neon_dark)
        .filter { (id, _) -> designs.none { it.id == id } }
    TileRow(buildList {
        designs.sortedBy { (it.theme == com.bisc.flauncher.ui.theme.ThemePreference.DARK) != dark }.forEach { design ->
            add(Tile(design.displayName(german), design.id == activeId, { viewModel.apply(design) }) { DesignPreview(design, Modifier.fillMaxWidth()) })
        }
        missing.forEach { (_, label) ->
            add(Tile(stringResource(label), false, { openUrl(context, RELEASES_URL) }) { DownloadPlaceholder() })
        }
    })
    if (missing.isNotEmpty()) {
        TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.welcome_pick_file)) }
    }
}

/** Installed packs, the official ones to download, and plain backgrounds, side by side. */
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
    TileRow(buildList {
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
    Text(
        stringResource(R.string.welcome_wallpaper_own_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

/** The screens the shape previews show, each with the parts that live on it. */
private enum class ShapeScreen(val label: Int) {
    HOME(R.string.welcome_screen_home),
    PANEL(R.string.welcome_screen_panel),
    APP_LIST(R.string.welcome_screen_app_list),
    SEARCH(R.string.welcome_screen_search),
    FOLDER(R.string.welcome_screen_folder)
}

/** Small screens in a row showing every part with its corners, live; above the table one shape
 *  for all, below it each part on its own. Starts from what the chosen design brought. */
@Composable
private fun ShapeChoice(viewModel: OnboardingViewModel) {
    val corners by viewModel.corners.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadCorners() }
    ShapeEditor(corners, onAll = viewModel::applyAll, onPart = viewModel::applyPart)
}

/** The shape previews, the choice for all and the table; [onAll] and [onPart] decide whether a
 *  change applies at once (welcome tour) or waits for Save (settings). */
@Composable
internal fun ShapeEditor(corners: Map<ShapePart, Int>, onAll: (ShapePreset) -> Unit, onPart: (ShapePart, ShapePreset) -> Unit) {
    var focus by remember { mutableStateOf<ShapePart?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShapeScreen.entries.forEach { screen ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MockScreen(screen, corners, focus)
                    Text(stringResource(screen.label), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        val all = ShapePreset.entries.firstOrNull { preset ->
            corners.isNotEmpty() && ShapePart.entries.all { corners[it] == it.percentOf(preset) }
        }
        Segmented(
            options = ShapePreset.entries,
            label = { stringResource(it.label()) },
            selected = all,
            onSelect = { focus = null; onAll(it) },
            modifier = Modifier.fillMaxWidth(),
            large = true
        )
        Text(
            stringResource(R.string.welcome_shape_single),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            ShapePart.entries.forEachIndexed { i, part ->
                val focused = focus == part
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { focus = part }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(part.label()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Segmented(
                        options = ShapePreset.entries,
                        label = { stringResource(it.label()) },
                        selected = corners[part]?.let(part::presetOf),
                        onSelect = { focus = part; onPart(part, it) }
                    )
                }
                if (i < ShapePart.entries.lastIndex) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                }
            }
        }
    }
}

/** A pill of choices, the chosen one filled with the accent. */
@Composable
private fun <T> Segmented(
    options: List<T>,
    label: @Composable (T) -> String,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(3.dp)
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                modifier = Modifier
                    .then(if (large) Modifier.weight(1f) else Modifier)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = if (large) 8.dp else 10.dp, vertical = if (large) 10.dp else 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option),
                    style = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    ShapePart.PANEL -> R.string.welcome_shape_part_panel
    ShapePart.WIDGETS -> R.string.welcome_shape_part_widgets
    ShapePart.ELEMENT_BOXES -> R.string.welcome_shape_part_element_boxes
    ShapePart.FOLDERS -> R.string.welcome_shape_part_folders
    ShapePart.SEARCH_BUTTON -> R.string.welcome_shape_part_search_button
    ShapePart.ALPHABET_RAIL -> R.string.welcome_shape_part_alphabet_rail
    ShapePart.SEARCH_BAR -> R.string.welcome_shape_part_search_bar
    ShapePart.SEARCH_RESULTS -> R.string.welcome_shape_part_search_results
}

/** One screen in small with its parts drawn at their corners; the part picked in the table glows
 *  in the accent so it's clear which one it is. */
@Composable
private fun MockScreen(screen: ShapeScreen, corners: Map<ShapePart, Int>, focus: ShapePart?) {
    val frame = RoundedCornerShape(18.dp)
    val neutral = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val accent = MaterialTheme.colorScheme.primary
    @Composable
    fun Part(part: ShapePart, modifier: Modifier, text: String? = null, content: @Composable BoxScope.() -> Unit = {}) {
        val shape = RoundedCornerShape(percent = ((corners[part] ?: 50) / 2).coerceIn(0, 50))
        Box(
            modifier = modifier
                .clip(shape)
                .background(if (focus == part) accent.copy(alpha = 0.55f) else neutral)
                .then(if (focus == part) Modifier.border(1.5.dp, accent, shape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            content()
            if (text != null) Text(text, fontSize = 8.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        }
    }
    @Composable
    fun Dots(count: Int, size: androidx.compose.ui.unit.Dp = 9.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(count) { Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))) }
        }
    }
    Box(
        modifier = Modifier
            .width(120.dp)
            .aspectRatio(0.5f)
            .clip(frame)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(2.dp, MaterialTheme.colorScheme.outline, frame)
            .padding(8.dp)
    ) {
        when (screen) {
            ShapeScreen.HOME -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(Modifier.height(10.dp))
                Part(ShapePart.WIDGETS, Modifier.fillMaxWidth().height(52.dp), stringResource(R.string.welcome_shape_part_widgets))
                Part(ShapePart.ELEMENT_BOXES, Modifier.fillMaxWidth().height(34.dp)) { Dots(4) }
                Spacer(Modifier.weight(1f))
                Part(ShapePart.DOCK, Modifier.fillMaxWidth().height(28.dp), stringResource(R.string.welcome_shape_part_dock))
            }
            ShapeScreen.PANEL -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Part(ShapePart.PANEL, Modifier.fillMaxWidth().height(96.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.welcome_shape_part_panel), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                        Dots(4)
                        Dots(4)
                    }
                }
                Spacer(Modifier.weight(1f))
                Part(ShapePart.DOCK, Modifier.fillMaxWidth().height(28.dp), stringResource(R.string.welcome_shape_part_dock))
            }
            ShapeScreen.APP_LIST -> {
                Column(Modifier.fillMaxSize().padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Part(ShapePart.SEARCH_BUTTON, Modifier.fillMaxWidth().height(22.dp), stringResource(R.string.welcome_mock_search))
                    repeat(7) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)))
                            Box(Modifier.width(44.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)))
                        }
                    }
                }
                Part(ShapePart.ALPHABET_RAIL, Modifier.align(Alignment.CenterEnd).width(12.dp).fillMaxHeight(0.8f)) {
                    Text("A\nZ", fontSize = 7.sp, lineHeight = 8.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            ShapeScreen.SEARCH -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(Modifier.height(10.dp))
                Part(ShapePart.SEARCH_RESULTS, Modifier.fillMaxWidth().height(110.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.welcome_shape_part_search_results), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                        Dots(4)
                        Dots(4)
                    }
                }
                Spacer(Modifier.weight(1f))
                Part(ShapePart.SEARCH_BAR, Modifier.fillMaxWidth().height(26.dp), stringResource(R.string.welcome_mock_search))
            }
            ShapeScreen.FOLDER -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(3) { Part(ShapePart.FOLDERS, Modifier.weight(1f).aspectRatio(1f)) }
                        }
                    }
                    Part(ShapePart.WIDGETS, Modifier.fillMaxWidth().height(40.dp), stringResource(R.string.welcome_shape_part_widgets))
                }
            }
        }
    }
}

/** Tiles side by side, to swipe through when there are more than fit. */
@Composable
private fun TileRow(tiles: List<Tile>) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tiles.forEach { tile -> ChoiceTile(tile, Modifier.width(112.dp)) }
    }
}

private class Tile(val label: String, val selected: Boolean, val onClick: () -> Unit, val picture: @Composable () -> Unit)

@Composable
private fun ChoiceTile(tile: Tile, modifier: Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
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
