package com.bisc.flauncher.ui.settings

import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
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

/** Top of the settings start page, as its heading: the logo in the middle, name and version below
 *  it, all centered. A tap opens the About screen. */
@Composable
fun AboutHeaderCard(onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SettingsCardShape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShapeShiftingLogo(76.dp)
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            stringResource(R.string.about_version, appVersion(context)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * The logo turning from one shape into the next - square, rounded, round, flower, star, cookie,
 * clover, blob and more - for how freely the launcher changes. Every shape is an outline of the
 * same number of points around the middle, so any two melt into each other point by point. Only
 * the outline moves (drawn by the graphics layer, nothing is laid out again), and only while this
 * page is open. With motion off, or its own switch off, it stays square.
 */
@Composable
private fun ShapeShiftingLogo(size: Dp) {
    val still = com.bisc.flauncher.ui.LocalMotion.current == com.bisc.flauncher.ui.MotionLevel.OFF || !com.bisc.flauncher.ui.LocalMotionPrefs.current.logoShape
    val shapes = remember { LOGO_SHAPES.map { outline(it) } }
    val progress = if (still) null else androidx.compose.animation.core.rememberInfiniteTransition(label = "logoShape").animateFloat(
        initialValue = 0f,
        targetValue = shapes.size.toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(shapes.size * LOGO_STEP_MS, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "logoMorph"
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                val value = progress?.value ?: 0f
                val index = value.toInt() % shapes.size
                // Each step: a short rest in the shape, then the change to the next one.
                val within = value - value.toInt()
                val rest = LOGO_REST_MS.toFloat() / LOGO_STEP_MS
                val t = if (within <= rest) 0f else com.bisc.flauncher.ui.EmphasizedEasing.transform((within - rest) / (1f - rest))
                shape = MorphShape(shapes[index], shapes[(index + 1) % shapes.size], t)
                clip = true
            }
            .background(colorResource(R.color.ic_launcher_background))
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}

private const val LOGO_STEP_MS = 1_500
private const val LOGO_REST_MS = 600
private const val OUTLINE_POINTS = 96

/** Distance from the middle at an angle (0 to 2 pi), for one shape. */
private val LOGO_SHAPES: List<(Double) -> Double> = listOf(
    { a -> 1.0 / maxOf(kotlin.math.abs(kotlin.math.cos(a)), kotlin.math.abs(kotlin.math.sin(a))) },          // square
    { a -> superellipse(a, 4.0) },                                                                             // rounded square
    { _ -> 1.0 },                                                                                              // circle
    { a -> 0.80 + 0.20 * kotlin.math.cos(6 * a) },                                                             // flower
    { a -> 0.55 + 0.45 * Math.pow((kotlin.math.cos(5 * (a + Math.PI / 2)) + 1) / 2, 1.6) },                    // star
    { a -> superellipse(a, 2.6) },                                                                             // squircle
    { a -> polygon(a, 6) },                                                                                    // hexagon
    { a -> 0.90 + 0.10 * kotlin.math.cos(12 * a) },                                                            // cookie
    { a -> 0.62 + 0.38 * kotlin.math.abs(kotlin.math.cos(2 * a)) },                                            // clover
    { a -> 0.84 + 0.09 * kotlin.math.sin(3 * a) + 0.07 * kotlin.math.cos(5 * a + 1) },                         // blob
    { a -> polygon(a + Math.PI / 2, 3) }                                                                       // triangle
)

private fun superellipse(a: Double, p: Double): Double =
    Math.pow(Math.pow(kotlin.math.abs(kotlin.math.cos(a)), p) + Math.pow(kotlin.math.abs(kotlin.math.sin(a)), p), -1.0 / p)

/** A regular polygon with [n] corners, one corner pointing right. */
private fun polygon(a: Double, n: Int): Double {
    val sector = 2 * Math.PI / n
    val local = ((a % sector) + sector) % sector - sector / 2
    return kotlin.math.cos(Math.PI / n) / kotlin.math.cos(local)
}

/** A shape's outline as distances at evenly spread angles, scaled so it just fills its box. */
private fun outline(r: (Double) -> Double): FloatArray {
    val raw = DoubleArray(OUTLINE_POINTS) { r(2 * Math.PI * it / OUTLINE_POINTS) }
    val xs = raw.indices.map { raw[it] * kotlin.math.cos(2 * Math.PI * it / OUTLINE_POINTS) }
    val ys = raw.indices.map { raw[it] * kotlin.math.sin(2 * Math.PI * it / OUTLINE_POINTS) }
    val reach = maxOf(xs.max(), -xs.min(), ys.max(), -ys.min())
    return FloatArray(OUTLINE_POINTS) { (raw[it] / reach).toFloat() }
}

/** Partway ([t]) from one outline to the next, point by point. */
private class MorphShape(private val from: FloatArray, private val to: FloatArray, private val t: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val path = androidx.compose.ui.graphics.Path()
        for (i in from.indices) {
            val r = from[i] + (to[i] - from[i]) * t
            val angle = 2.0 * Math.PI * i / from.size
            val x = cx + (r * cx * kotlin.math.cos(angle)).toFloat()
            val y = cy + (r * cy * kotlin.math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
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
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Each opens in the browser.
        SettingsTileGrid(
            title = stringResource(R.string.about_project_label),
            columns = 3,
            tiles = listOfNotNull(
                if (AboutInfo.SOURCE_URL.isNotBlank()) SettingsTile(Icons.Filled.Code, stringResource(R.string.about_source)) { openUrl(context, AboutInfo.SOURCE_URL) } else null,
                SettingsTile(Icons.Filled.NewReleases, stringResource(R.string.about_releases)) { openUrl(context, AboutInfo.RELEASES_URL) },
                if (AboutInfo.ISSUES_URL.isNotBlank()) SettingsTile(Icons.Filled.BugReport, stringResource(R.string.about_issues)) { openUrl(context, AboutInfo.ISSUES_URL) } else null
            )
        )

        SettingsTileGrid(
            title = stringResource(R.string.about_legal_label),
            tiles = listOf(
                SettingsTile(Icons.Filled.Gavel, stringResource(R.string.about_license), stringResource(R.string.about_license_summary)) { showLicense = true },
                SettingsTile(Icons.Filled.Description, stringResource(R.string.about_third_party), stringResource(R.string.about_third_party_summary)) { showThirdParty = true }
            )
        )
        SettingsPreferenceGroup {
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

