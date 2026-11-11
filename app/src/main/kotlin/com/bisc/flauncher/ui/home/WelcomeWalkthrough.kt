package com.bisc.flauncher.ui.home

import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.bisc.flauncher.data.wallpaper.WallpaperPackRepository
import com.bisc.flauncher.ui.settings.ChoiceDialog
import com.bisc.flauncher.ui.settings.DesignPreview
import com.bisc.flauncher.ui.settings.DesignsViewModel
import com.bisc.flauncher.ui.settings.WallpaperPacksViewModel
import com.bisc.flauncher.ui.settings.PartCornersViewModel
import com.bisc.flauncher.ui.settings.ShapeEditor
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
import com.bisc.flauncher.ui.MaxContentWidth
import com.bisc.flauncher.ui.settings.PermissionGroups
import com.bisc.flauncher.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

private enum class WelcomeStep { HELLO, DESIGN, WALLPAPER, SHAPE, GESTURES, WIDGETS, SETTINGS, PERMISSIONS, LOCK_SCREEN }

/** The first-start tour: welcome with a quick setup, the gestures, where the settings are, then
 *  design, wallpaper pack and shape to choose, the permissions, and the lock screen when the pack
 *  has a picture for it. Skippable at any point; shown once. */
@Composable
fun WelcomeWalkthrough(
    onDone: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onboarding: OnboardingViewModel = hiltViewModel(),
    designsViewModel: DesignsViewModel = hiltViewModel(),
    packsViewModel: WallpaperPacksViewModel = hiltViewModel(),
    cornersViewModel: PartCornersViewModel = hiltViewModel()
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

    // Everything the look steps show in their phone: the design, the wallpaper and the corners in use.
    val designs by designsViewModel.designs.collectAsState()
    val activeDesignId by designsViewModel.activeId.collectAsState()
    val activeDesign = designs.firstOrNull { it.id == activeDesignId }
    val activePack = packs.firstOrNull { it.id == activePackId }
    val corners by cornersViewModel.corners.collectAsState()
    LaunchedEffect(activeDesignId) { cornersViewModel.loadCorners() }
    val phone: @Composable () -> Unit = {
        LivePhone(activeDesign, activePack?.images?.get("home") ?: activePack?.preview, dark, corners)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val heroShown = (1f - (pagerState.currentPage + pagerState.currentPageOffsetFraction)).coerceIn(0f, 1f)
        if (heroShown > 0f) HeroBackground(Modifier.fillMaxSize().graphicsLayer { alpha = heroShown })
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
                val step = steps.getOrElse(pagerState.currentPage) { steps.last() }
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step == WelcomeStep.HELLO) {
                        // White on the hero's blue.
                        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Color.White, onSurface = Color.White)) {
                        TextButton(onClick = { onboarding.setTourDark(!dark) }) {
                            Icon(if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(if (dark) R.string.settings_theme_light else R.string.settings_theme_dark))
                        }
                        Spacer(Modifier.weight(1f))
                        LanguageButton()
                        }
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
                        WelcomeStep.DESIGN -> LookPage(R.string.welcome_design_title, R.string.welcome_design_text, phone) {
                            DesignChoice(designsViewModel, onboarding, dark)
                        }
                        WelcomeStep.WALLPAPER -> LookPage(R.string.welcome_wallpaper_title, R.string.welcome_wallpaper_text, phone) {
                            WallpaperChoice(packsViewModel, dark)
                        }
                        WelcomeStep.SHAPE -> LookPage(R.string.welcome_shape_title, R.string.welcome_shape_text, phone) {
                            ShapeChoice(cornersViewModel)
                        }
                        WelcomeStep.WIDGETS -> WelcomePage(R.string.welcome_widgets_title, R.string.welcome_widgets_text, illustrationWeight = 0f) {
                            WidgetsOverview()
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
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f).padding(end = 20.dp)) {
                        // The welcome itself is not a step to count.
                        (1 until steps.size).forEach { i ->
                            val reached by androidx.compose.animation.animateColorAsState(
                                if (i <= pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                label = "step"
                            )
                            Box(modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape).background(reached))
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
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, androidx.compose.animation.core.tween(700, easing = com.bisc.flauncher.ui.EmphasizedEasing)) }
    fun Modifier.rise(delay: Float) = graphicsLayer {
        val p = ((appear.value - delay) / (1f - delay)).coerceIn(0f, 1f)
        alpha = p
        translationY = (1f - p) * 40.dp.toPx()
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val s = 0.7f + 0.3f * appear.value
                    scaleX = s; scaleY = s; alpha = appear.value
                }
                .size(132.dp)
                .clip(RoundedCornerShape(38.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(38.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.welcome_hello_title),
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.rise(0.15f)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.welcome_hello_text),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.88f),
            textAlign = TextAlign.Center,
            modifier = Modifier.rise(0.25f)
        )
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.rise(0.35f)) {
            listOf(
                Icons.Filled.WifiOff to R.string.about_promise_offline,
                Icons.Filled.VisibilityOff to R.string.about_promise_tracking,
                Icons.Filled.Block to R.string.about_promise_ads
            ).forEach { (icon, label) ->
                Row(
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        Column(modifier = Modifier.rise(0.45f), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onQuick,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0758F5)),
                modifier = Modifier.fillMaxWidth().height(58.dp)
            ) {
                Text(stringResource(R.string.welcome_quick_setup), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                stringResource(R.string.welcome_quick_setup_summary),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
            )
            OutlinedButton(
                onClick = onOwn,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text(stringResource(R.string.welcome_own_setup), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** The launcher's blue, deepening towards the bottom, with soft light drifting slowly over it. */
@Composable
private fun HeroBackground(modifier: Modifier) {
    val drift = androidx.compose.animation.core.rememberInfiniteTransition(label = "hero")
    val t by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(14000, easing = androidx.compose.animation.core.LinearEasing),
            androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "drift"
    )
    androidx.compose.foundation.Canvas(modifier) {
        drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF1A6BFF), Color(0xFF0758F5), Color(0xFF0236A8))))
        listOf(
            Triple(Offset(0.15f + 0.25f * t, 0.12f), 0.75f, Color(0xFF7FB2FF)),
            Triple(Offset(0.95f - 0.2f * t, 0.45f + 0.1f * t), 0.65f, Color(0xFF9B7BFF)),
            Triple(Offset(0.35f + 0.15f * t, 1.0f - 0.08f * t), 0.9f, Color(0xFF00C2E0))
        ).forEach { (spot, reach, color) ->
            val center = Offset(size.width * spot.x, size.height * spot.y)
            val radius = size.width * reach
            drawCircle(
                androidx.compose.ui.graphics.Brush.radialGradient(0f to color.copy(alpha = 0.35f), 1f to Color.Transparent, center = center, radius = radius),
                radius = radius,
                center = center
            )
        }
    }
}

/** A look step: the phone with what is chosen on top, live, and the choices below it. */
@Composable
private fun LookPage(titleRes: Int, textRes: Int, phone: @Composable () -> Unit, choices: @Composable () -> Unit) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.weight(0.9f).fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), contentAlignment = Alignment.Center) { phone() }
        Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            stringResource(textRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().fadeScrollEdges(scroll).verticalScroll(scroll).padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) { choices() }
    }
}

/** A phone showing the home screen with the chosen design, wallpaper and corners, as they are now. */
@Composable
private fun LivePhone(
    design: com.bisc.flauncher.data.design.Design?,
    picture: java.io.File?,
    dark: Boolean,
    corners: Map<com.bisc.flauncher.ui.settings.ShapePart, Int>
) {
    val designDark = design?.theme?.let { it == com.bisc.flauncher.ui.theme.ThemePreference.DARK } ?: dark
    val frame = RoundedCornerShape(30.dp)
    val box = design?.box ?: com.bisc.flauncher.data.model.ElementAppearance()
    val accent = Color(design?.accent ?: 0xFF0758F5.toInt())
    fun shape(part: com.bisc.flauncher.ui.settings.ShapePart, fallback: Int) =
        com.bisc.flauncher.ui.theme.PartCornerShape(corners[part] ?: fallback, full = 16.dp)
    androidx.compose.animation.Crossfade(targetState = Triple(design, picture, corners), animationSpec = androidx.compose.animation.core.tween(350), label = "phone") { _ ->
        CompositionLocalProvider(
            com.bisc.flauncher.ui.theme.LocalDesignEffects provides design?.effects,
            com.bisc.flauncher.ui.home.LocalSurfaceIsDark provides designDark
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(0.5f, matchHeightConstraintsFirst = true)
                    .shadow(20.dp, frame)
                    .clip(frame)
                    .background(Color(com.bisc.flauncher.ui.theme.backgroundColorFor(designDark)))
                    .border(3.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), frame)
            ) {
                if (picture != null) PictureThumb(picture, Modifier.fillMaxSize())
                com.bisc.flauncher.ui.theme.DesignBackgroundEffects(Modifier.fillMaxSize())
                Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val clockColor = if (designDark || picture != null) Color.White else Color(0xFF16171B)
                    Text("9:41", fontSize = 30.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Light, color = clockColor, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                    com.bisc.flauncher.ui.home.dock.DockPanelSurface(design?.widget ?: box, shape(com.bisc.flauncher.ui.settings.ShapePart.WIDGETS, 80), Modifier.fillMaxWidth().height(58.dp), effectTarget = "widgets") {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.fillMaxWidth(0.7f).height(6.dp).clip(CircleShape).background(androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.55f)))
                            Box(Modifier.fillMaxWidth(0.45f).height(6.dp).clip(CircleShape).background(androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.3f)))
                        }
                    }
                    com.bisc.flauncher.ui.home.dock.DockPanelSurface(box, shape(com.bisc.flauncher.ui.settings.ShapePart.ELEMENT_BOXES, 50), Modifier.fillMaxWidth().height(44.dp), effectTarget = "widgets") {
                        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(4) { i -> Box(Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(accent.copy(alpha = 0.35f + 0.15f * i))) }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    com.bisc.flauncher.ui.home.dock.DockPanelSurface(
                        design?.elements?.get("search_field") ?: box,
                        shape(com.bisc.flauncher.ui.settings.ShapePart.SEARCH_BAR, 100),
                        Modifier.fillMaxWidth().height(26.dp),
                        effectTarget = "search"
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(13.dp))
                    }
                    com.bisc.flauncher.ui.home.dock.DockPanelSurface(design?.dock ?: box, shape(com.bisc.flauncher.ui.settings.ShapePart.DOCK, 100), Modifier.fillMaxWidth().height(46.dp), effectTarget = "dock") {
                        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            repeat(4) { Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(accent.copy(alpha = 0.85f))) }
                        }
                    }
                }
            }
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
    val missing = listOf(
        "neon-light" to R.string.welcome_design_neon_light, "neon-dark" to R.string.welcome_design_neon_dark,
        "3d-light" to R.string.welcome_design_3d_light, "3d-dark" to R.string.welcome_design_3d_dark
    )
        .filter { (id, _) -> designs.none { it.id == id } }
        // The tour's own mode first.
        .sortedBy { (id, _) -> id.endsWith("-dark") != dark }
    TileRow(buildList {
        designs.sortedBy { (it.theme == com.bisc.flauncher.ui.theme.ThemePreference.DARK) != dark }.forEach { design ->
            add(Tile(design.displayName(german), design.id == activeId, { viewModel.apply(design) }) { DesignPreview(design, Modifier.fillMaxWidth()) })
        }
        missing.forEach { (_, label) ->
            add(Tile(stringResource(label), false, { com.bisc.flauncher.ui.settings.openExtensionsPage(context, "#designs") }) { DownloadPlaceholder() })
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
            add(Tile(stringResource(label), false, { com.bisc.flauncher.ui.settings.openExtensionsPage(context, "#wallpaper") }) { DownloadPlaceholder() })
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

/** Small screens in a row showing every part with its corners, live; above the table one shape
 *  for all, below it each part on its own. Starts from what the chosen design brought. */
@Composable
private fun ShapeChoice(viewModel: PartCornersViewModel) {
    val corners by viewModel.corners.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadCorners() }
    // The phone above shows the result, so no small screens of their own here.
    ShapeEditor(corners, onAll = viewModel::applyAll, onPart = viewModel::applyPart, showScreens = false)
}

/** Tiles side by side; exactly three fit, like the page tiles, the rest are a swipe away. */
/** Always three side by side; more go on in the next line. */
@Composable
private fun TileRow(tiles: List<Tile>) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TILE_GAP * 2)) {
        tiles.chunked(3).forEach { line ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
                line.forEach { tile -> ChoiceTile(tile, Modifier.weight(1f)) }
                repeat(3 - line.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private val TILE_GAP = 10.dp

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

/** The widgets there are: three that come along, and Weather with the way to an app that brings its data. */
@Composable
private fun WidgetsOverview() {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        com.bisc.flauncher.ui.settings.SettingsPreferenceGroup {
            listOf(
                Triple(Icons.Filled.Schedule, R.string.picker_widget_clock, R.string.welcome_widgets_clock),
                Triple(Icons.Filled.EditNote, R.string.picker_widget_notes, R.string.welcome_widgets_notes),
                Triple(Icons.Filled.MusicNote, R.string.picker_widget_music, R.string.welcome_widgets_music),
                Triple(Icons.Filled.Cloud, R.string.picker_widget_weather, R.string.welcome_widgets_weather)
            ).forEach { (icon, title, summary) ->
                com.bisc.flauncher.ui.settings.SettingsPreference(icon = icon, title = stringResource(title), summary = stringResource(summary), onClick = null, trailing = null)
            }
        }
        Text(
            stringResource(R.string.welcome_widgets_weather_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )
        TextButton(
            onClick = { com.bisc.flauncher.ui.settings.openExtensionsPage(context, "#widgets") },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text(stringResource(R.string.extensions_weather_find)) }
    }
}

/** One step: the picture on top, a title and a short text below it. */
@Composable
private fun WelcomePage(titleRes: Int, textRes: Int, illustrationWeight: Float = 1f, illustration: @Composable () -> Unit) {
    val scroll = rememberScrollState()
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
                .then(if (illustrationWeight > 0f) Modifier else Modifier.weight(1f).fadeScrollEdges(scroll).verticalScroll(scroll))
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

/** The gestures phone; tapping a gesture sets what it does, like in the settings. */
@Composable
private fun GesturesPreview(viewModel: SettingsViewModel) {
    Box(modifier = Modifier.fillMaxHeight().aspectRatio(0.52f, matchHeightConstraintsFirst = true), contentAlignment = Alignment.Center) {
        com.bisc.flauncher.ui.settings.EditableGesturePhone(viewModel, widthFraction = 1f)
    }
}

/** Content scrolled out of view fades out at the top and the bottom edge instead of being cut off. */
private fun Modifier.fadeScrollEdges(scroll: androidx.compose.foundation.ScrollState): Modifier = this
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val edge = 40.dp.toPx()
        // Only where there is something to scroll to.
        val top = (scroll.value / edge).coerceIn(0f, 1f)
        val bottom = ((scroll.maxValue - scroll.value) / edge).coerceIn(0f, 1f)
        if (top > 0f) drawRect(
            androidx.compose.ui.graphics.Brush.verticalGradient(0f to Color.Black.copy(alpha = 1f - top), 1f to Color.Black, startY = 0f, endY = edge),
            size = androidx.compose.ui.geometry.Size(size.width, edge),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
        )
        if (bottom > 0f) drawRect(
            androidx.compose.ui.graphics.Brush.verticalGradient(0f to Color.Black, 1f to Color.Black.copy(alpha = 1f - bottom), startY = size.height - edge, endY = size.height),
            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - edge),
            size = androidx.compose.ui.geometry.Size(size.width, edge),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
        )
    }
