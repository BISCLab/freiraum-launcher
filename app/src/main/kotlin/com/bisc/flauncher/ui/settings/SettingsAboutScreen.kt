package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.lerp
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisc.flauncher.R

/** Project facts shown on the About screen. A row whose value is blank is left out. */
private object AboutInfo {
    const val DEVELOPER = "BISC Lab"
    const val SOURCE_URL = "https://github.com/BISCLab/freiraum-launcher"
    const val ISSUES_URL = "https://github.com/BISCLab/freiraum-launcher/issues"
    const val RELEASES_URL = "https://github.com/BISCLab/freiraum-launcher/releases"
    const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
}

/** BISC Lab's brand red, darkened for text. */
private val BISC_RED = androidx.compose.ui.graphics.Color(0xFFB3202A)

/** The app's own icon, drawn from the adaptive icon's layers (Compose can't paint the adaptive icon itself). */
@Composable
fun AppLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(30))
            .background(colorResource(R.color.ic_launcher_background))
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}

private fun appVersion(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

/** Top of the settings start page: logo, name and version, opening the About screen. */
@Composable
fun AboutHeaderCard(onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SettingsCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppLogo(48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.about_version, appVersion(context)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** About: what the app is, its source and its license. */
@Composable
fun SettingsAboutScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var showLicense by remember { mutableStateOf(false) }
    var showThirdParty by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.about_title), navController = navController, onBack = onBack) {
        // The launcher itself first: its logo, name, version and what it is, on its own color.
        val brand = colorResource(R.color.ic_launcher_background)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(brand, lerp(brand, androidx.compose.ui.graphics.Color.Black, 0.22f))))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(120.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = androidx.compose.ui.graphics.Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                stringResource(R.string.about_version, appVersion(context)),
                style = MaterialTheme.typography.labelLarge,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
            Text(
                stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // What it stands for, at a glance.
        Row(modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                Icons.Filled.WifiOff to R.string.about_promise_offline,
                Icons.Filled.VisibilityOff to R.string.about_promise_tracking,
                Icons.Filled.Block to R.string.about_promise_ads
            ).forEach { (icon, label) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(SettingsCardShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        if (AboutInfo.SOURCE_URL.isNotBlank() || AboutInfo.ISSUES_URL.isNotBlank()) {
            SettingsPreferenceGroup(title = stringResource(R.string.about_project_label)) {
                if (AboutInfo.SOURCE_URL.isNotBlank()) {
                    SettingsPreference(
                        icon = Icons.Filled.Code,
                        title = stringResource(R.string.about_source),
                        summary = AboutInfo.SOURCE_URL.removePrefix("https://"),
                        onClick = { openUrl(context, AboutInfo.SOURCE_URL) },
                        trailing = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
                    )
                }
                SettingsPreference(
                    icon = Icons.Filled.NewReleases,
                    title = stringResource(R.string.about_releases),
                    summary = stringResource(R.string.about_releases_summary),
                    onClick = { openUrl(context, AboutInfo.RELEASES_URL) },
                    trailing = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
                )
                if (AboutInfo.ISSUES_URL.isNotBlank()) {
                    SettingsPreference(
                        icon = Icons.Filled.BugReport,
                        title = stringResource(R.string.about_issues),
                        summary = stringResource(R.string.about_issues_summary),
                        onClick = { openUrl(context, AboutInfo.ISSUES_URL) },
                        trailing = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
                    )
                }
            }
        }

        SettingsPreferenceGroup(title = stringResource(R.string.about_legal_label)) {
            SettingsPreference(
                icon = Icons.Filled.Gavel,
                title = stringResource(R.string.about_license),
                summary = stringResource(R.string.about_license_summary),
                onClick = { showLicense = true },
                trailing = null
            )
            SettingsPreference(
                icon = Icons.Filled.Description,
                title = stringResource(R.string.about_third_party),
                summary = stringResource(R.string.about_third_party_summary),
                onClick = { showThirdParty = true },
                trailing = null
            )
            SettingsPreference(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.about_privacy),
                summary = stringResource(R.string.about_privacy_summary),
                onClick = null,
                trailing = null
            )
        }

        // Who makes it, apart from what it is.
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(stringResource(R.string.about_made_by), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Image(painterResource(R.drawable.bisc_logo), contentDescription = null, modifier = Modifier.size(width = 24.dp, height = 48.dp))
            Text(AboutInfo.DEVELOPER, style = MaterialTheme.typography.titleSmall, color = BISC_RED)
        }
    }

    if (showLicense) {
        AlertDialog(
            onDismissRequest = { showLicense = false },
            title = { Text(stringResource(R.string.about_license)) },
            text = { Text(stringResource(R.string.about_license_text)) },
            confirmButton = {
                TextButton(onClick = { openUrl(context, AboutInfo.LICENSE_URL) }) { Text(stringResource(R.string.about_license_full_text)) }
            },
            dismissButton = { TextButton(onClick = { showLicense = false }) { Text(stringResource(R.string.about_close)) } }
        )
    }
    if (showThirdParty) {
        AlertDialog(
            onDismissRequest = { showThirdParty = false },
            title = { Text(stringResource(R.string.about_third_party)) },
            text = { Text(stringResource(R.string.about_third_party_text)) },
            confirmButton = { TextButton(onClick = { showThirdParty = false }) { Text(stringResource(R.string.about_close)) } }
        )
    }
}

