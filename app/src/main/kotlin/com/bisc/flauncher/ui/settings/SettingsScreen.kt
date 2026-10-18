package com.bisc.flauncher.ui.settings

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.theme.noRippleClickable
import com.bisc.flauncher.util.DefaultLauncherUtil

/** Top-level settings list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsListScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val isDefault = DefaultLauncherUtil.isDefaultLauncher(context)

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = settingsTopBarInsets(),
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = { SettingsBreadcrumbBar(navController) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Masterplan's final nav (Phase 9): Globale Einstellungen / Elemente / Seiten /
            // Berechtigungen. Apps/Folder live only under Elements now; Gestures/App overview only
            // under Pages — see SettingsElementsScreen/SettingsPagesScreen for those sub-links.
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.settings_section_appearance),
                    summary = stringResource(R.string.settings_card_appearance_subtitle),
                    onClick = { onNavigate("appearance") }
                )
                SettingsPreference(
                    icon = Icons.Filled.Apps,
                    title = stringResource(R.string.settings_section_elements),
                    summary = stringResource(R.string.settings_card_elements_subtitle),
                    onClick = { onNavigate("elements") }
                )
                SettingsPreference(
                    icon = Icons.Filled.ViewCarousel,
                    title = stringResource(R.string.settings_section_pages),
                    summary = stringResource(R.string.settings_card_pages_subtitle),
                    onClick = { onNavigate("pages") }
                )
                SettingsPreference(
                    icon = Icons.Filled.PhoneAndroid,
                    title = stringResource(R.string.settings_section_permissions),
                    summary = if (isDefault) {
                        stringResource(R.string.settings_default_launcher_already_set)
                    } else {
                        stringResource(R.string.settings_default_launcher_description)
                    },
                    onClick = { onNavigate("permissions") }
                )
            }
        }
    }
}

/** Rounded group of preference rows with an optional title and an optional explanation that covers
 *  the whole group. When [info] is given, a neutral-gray "?" button next to the title expands it
 *  inline — this is the shared spot for a group's explanatory text, so a group's rows generally
 *  shouldn't also carry their own [SettingsPreference.info] (see that param's doc). */
@Composable
fun SettingsPreferenceGroup(title: String? = null, info: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            SettingsGroupLabel(title, info = info)
        }
        Column(
            modifier = Modifier.clip(MaterialTheme.shapes.medium),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content
        )
    }
}

/** The small secondary-colored label [SettingsPreferenceGroup] renders above its own card, with an
 *  optional expandable [info] text next to it. */
@Composable
fun SettingsGroupLabel(title: String, info: String? = null) {
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 4.dp)
            )
            if (info != null) {
                IconButton(onClick = { infoOpen = !infoOpen }, modifier = Modifier.size(24.dp).padding(start = 2.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = stringResource(R.string.settings_info_button),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        if (info != null) {
            AnimatedVisibility(visible = infoOpen) {
                Text(
                    info,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }
    }
}

/** One row of a [SettingsPreferenceGroup]. */
@Composable
fun SettingsPreference(
    icon: ImageVector?,
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
    titleFontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    leading: (@Composable () -> Unit)? = icon?.let {
        {
            Icon(
                it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    },
    trailing: (@Composable () -> Unit)? = { ChevronIndicator() },
    info: String? = null,
    infoAction: Pair<String, () -> Unit>? = null,
    enabled: Boolean = true
) {
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .then(if (enabled) Modifier.noRippleClickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading?.invoke()
            Column(modifier = Modifier.padding(start = if (leading != null) 16.dp else 0.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontFamily = titleFontFamily ?: MaterialTheme.typography.titleMedium.fontFamily))
                if (summary != null) {
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (info != null) {
                IconButton(onClick = { infoOpen = !infoOpen }, modifier = Modifier.padding(start = 4.dp).size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = stringResource(R.string.settings_info_button),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (trailing != null) {
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    trailing()
                }
            }
        }
        if (info != null) {
            AnimatedVisibility(visible = infoOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(info, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (infoAction != null) {
                        TextButton(onClick = infoAction.second) { Text(infoAction.first) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChevronIndicator() {
    Icon(
        Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Shared scaffold for every sub-screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFixedScreen(title: String, navController: NavController, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = settingsTopBarInsets(),
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = { SettingsBreadcrumbBar(navController) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 8.dp), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSubScreen(title: String, navController: NavController, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = settingsTopBarInsets(),
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = { SettingsBreadcrumbBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            content = content
        )
    }
}
