package com.bisc.flauncher.ui.home

import androidx.compose.foundation.Image
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

private enum class WelcomeStep { HELLO, GESTURES, SETTINGS, PERMISSIONS, LOCK_SCREEN }

/** The first-start walkthrough: what Freiraum is, its gestures, where the settings are, the
 *  permissions and the lock screen offer. Skippable at any point; shown once. */
@Composable
fun WelcomeWalkthrough(
    onLockScreenAnswer: (Boolean) -> Unit,
    onDone: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val steps = WelcomeStep.entries
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    var lockAnswered by remember { mutableStateOf<Boolean?>(null) }
    val finish = {
        if (lockAnswered == null) onLockScreenAnswer(false)
        onDone()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = finish) { Text(stringResource(R.string.welcome_skip)) }
                }
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
                    when (steps[index]) {
                        WelcomeStep.HELLO -> WelcomePage(R.string.welcome_hello_title, R.string.welcome_hello_text) {
                            PhoneFrame { HomePreview(highlightSettings = false) }
                        }
                        WelcomeStep.GESTURES -> WelcomePage(R.string.welcome_gestures_title, R.string.welcome_gestures_text) {
                            GesturesPreview(settingsViewModel)
                        }
                        WelcomeStep.SETTINGS -> WelcomePage(R.string.welcome_settings_title, R.string.welcome_settings_text) {
                            PhoneFrame { HomePreview(highlightSettings = true) }
                        }
                        WelcomeStep.PERMISSIONS -> WelcomePage(R.string.welcome_permissions_title, R.string.welcome_permissions_text, illustrationWeight = 0f) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PermissionGroups(viewModel = settingsViewModel, purposeVisible = true)
                            }
                        }
                        WelcomeStep.LOCK_SCREEN -> WelcomePage(R.string.welcome_lock_title, R.string.welcome_lock_text) {
                            PhoneFrame {
                                Image(
                                    painter = painterResource(R.drawable.wallpaper_2),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
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
                val step = steps[pagerState.currentPage]
                if (step == WelcomeStep.LOCK_SCREEN && lockAnswered == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { lockAnswered = false; onLockScreenAnswer(false); onDone() }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.lock_wallpaper_offer_no))
                        }
                        Button(onClick = { lockAnswered = true; onLockScreenAnswer(true); onDone() }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.lock_wallpaper_offer_yes))
                        }
                    }
                }
                Row(
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
                    if (step != WelcomeStep.LOCK_SCREEN) {
                        Button(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                            Text(stringResource(R.string.welcome_next))
                        }
                    } else if (lockAnswered != null) {
                        Button(onClick = onDone) { Text(stringResource(R.string.welcome_done)) }
                    }
                }
            }
        }
    }
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

/** The home screen in small: the bundled wallpaper and a Dock, optionally with the settings button
 *  picked out. */
@Composable
private fun BoxScope.HomePreview(highlightSettings: Boolean) {
    Image(
        painter = painterResource(R.drawable.wallpaper_3),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
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
