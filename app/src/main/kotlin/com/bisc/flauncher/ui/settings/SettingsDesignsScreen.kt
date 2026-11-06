package com.bisc.flauncher.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.design.Design
import com.bisc.flauncher.data.design.DesignRepository
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.DesignBackgroundEffects
import com.bisc.flauncher.ui.theme.LocalDesignEffects
import com.bisc.flauncher.ui.theme.designGlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DesignsViewModel @Inject constructor(
    private val repository: DesignRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val designs: StateFlow<List<Design>> = repository.designs
    val activeId: StateFlow<String?> = settingsRepository.activeDesignId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val effects: StateFlow<com.bisc.flauncher.data.design.DesignEffects?> =
        settingsRepository.designEffects.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setEffectMotion(motion: com.bisc.flauncher.data.design.EffectMotion) = viewModelScope.launch {
        effects.value?.let { settingsRepository.setDesignEffects(it.copy(motion = motion)) }
    }

    init { viewModelScope.launch { repository.refresh() } }

    fun import(uri: Uri, onDone: (String?) -> Unit) = viewModelScope.launch { onDone(repository.import(uri)) }
    fun apply(design: Design, lockScreen: Boolean, context: android.content.Context) = viewModelScope.launch {
        repository.apply(design)
        if (lockScreen) repository.lockWallpaperOf(design)?.let { file ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    android.app.WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, android.app.WallpaperManager.FLAG_LOCK)
                }
            }
        }
    }
    fun restoreMine() = viewModelScope.launch { repository.restoreMine() }
    fun delete(design: Design) = viewModelScope.launch { repository.delete(design) }
    fun export(uri: Uri, name: String, onDone: () -> Unit) = viewModelScope.launch { repository.exportCurrent(uri, name); onDone() }
}

/** Designs: install them from a file, preview, apply, go back to your own look, or save your look
 *  as a design. Freiraum has no internet access; designs are downloaded in the browser. */
@Composable
fun SettingsDesignsScreen(navController: NavController, onBack: () -> Unit, viewModel: DesignsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val designs by viewModel.designs.collectAsState()
    val activeId by viewModel.activeId.collectAsState()
    val effects by viewModel.effects.collectAsState()
    var previewing by remember { mutableStateOf<Design?>(null) }
    var removing by remember { mutableStateOf<Design?>(null) }
    val german = LocalConfiguration.current.locales.get(0).language == "de"

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri) { name ->
            Toast.makeText(
                context,
                if (name != null) context.getString(R.string.designs_import_done, name) else context.getString(R.string.designs_import_failed),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val exportName = stringResource(R.string.designs_export_default_name)
    var confirmingDesignExport by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.export(uri, exportName) {
            Toast.makeText(context, R.string.designs_export_done, Toast.LENGTH_LONG).show()
        }
    }

    SettingsSubScreen(title = stringResource(R.string.designs_title), navController = navController, onBack = onBack) {
        Text(
            stringResource(R.string.designs_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.designs_find),
                summary = stringResource(R.string.designs_find_summary),
                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DESIGNS_URL))) } },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.designs_add),
                summary = stringResource(R.string.designs_add_summary),
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                trailing = null
            )
        }
        if (designs.isNotEmpty()) {
            SettingsPreferenceGroup(title = stringResource(R.string.designs_installed)) {
                designs.forEach { design ->
                    SettingsPreference(
                        icon = null,
                        title = design.displayName(german),
                        summary = listOfNotNull(
                            design.author.takeIf { it.isNotBlank() },
                            design.version.takeIf { it.isNotBlank() }?.let { "v$it" },
                            if (design.id == activeId) stringResource(R.string.designs_active) else null
                        ).joinToString(" · "),
                        onClick = { previewing = design },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DesignSwatch(design)
                                androidx.compose.material3.IconButton(onClick = { removing = design }) {
                                    androidx.compose.material3.Icon(
                                        androidx.compose.material.icons.Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.designs_remove),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
        val activeEffects = effects
        if (activeEffects != null && activeEffects.glowBorder) {
            SettingsPreferenceGroup(title = stringResource(R.string.designs_effect_motion)) {
                RadioPreference(stringResource(R.string.designs_effect_still), com.bisc.flauncher.data.design.EffectMotion.STILL, activeEffects.motion, viewModel::setEffectMotion)
                RadioPreference(stringResource(R.string.designs_effect_once), com.bisc.flauncher.data.design.EffectMotion.ONCE, activeEffects.motion, viewModel::setEffectMotion)
                RadioPreference(
                    stringResource(R.string.designs_effect_slow), com.bisc.flauncher.data.design.EffectMotion.SLOW, activeEffects.motion, viewModel::setEffectMotion,
                    summary = stringResource(R.string.designs_effect_slow_summary)
                )
            }
        }
        SettingsPreferenceGroup {
            if (activeId != null) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.designs_restore),
                    summary = stringResource(R.string.designs_restore_summary),
                    onClick = viewModel::restoreMine,
                    trailing = null
                )
            }
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.designs_export),
                summary = stringResource(R.string.designs_export_summary),
                onClick = { confirmingDesignExport = true },
                trailing = null
            )
        }
    }

    if (confirmingDesignExport) {
        AlertDialog(
            onDismissRequest = { confirmingDesignExport = false },
            title = { Text(stringResource(R.string.designs_export)) },
            text = { Text(stringResource(R.string.designs_export_info)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDesignExport = false
                    exportLauncher.launch("freiraum-design.freiraum-design")
                }) { Text(stringResource(R.string.settings_backup_export_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmingDesignExport = false }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }

    removing?.let { design ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.designs_remove_title, design.displayName(german))) },
            text = {
                Text(stringResource(if (design.id == activeId) R.string.designs_remove_active_text else R.string.designs_remove_text))
            },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(design); removing = null }) {
                    Text(stringResource(R.string.designs_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }

    previewing?.let { design ->
        var lockScreen by remember(design.id) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { previewing = null },
            title = { Text(design.displayName(german)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    DesignPreview(design, Modifier.fillMaxWidth(0.6f))
                    if (design.lockWallpaper != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = lockScreen, onCheckedChange = { lockScreen = it })
                            Text(stringResource(R.string.designs_lock_screen_too), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.apply(design, lockScreen, context); previewing = null }) {
                    Text(stringResource(R.string.designs_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.delete(design); previewing = null }) {
                    Text(stringResource(R.string.designs_remove), color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

private const val DESIGNS_URL = "https://github.com/BISCLab/freiraum-launcher/tree/master/designs"

/** The design's accent and dock color as two small dots. */
@Composable
private fun DesignSwatch(design: Design) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOfNotNull(design.accent, design.dock?.fill?.color, design.backgrounds["global"]?.color).take(3).forEach {
            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(it)).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
}

/** The home screen in small with the design's background, effects, a search field, a widget card
 *  and the Dock - drawn with the same code as the real ones. */
@Composable
private fun DesignPreview(design: Design, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    val home = design.backgrounds["home"] ?: design.backgrounds["global"]
    val bitmap by produceState<ImageBitmap?>(null, home?.image) {
        value = withContext(Dispatchers.IO) {
            home?.image?.takeIf(File::isFile)?.let { file ->
                runCatching {
                    val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    CompositionLocalProvider(LocalDesignEffects provides design.effects) {
        Box(
            modifier = modifier
                .aspectRatio(0.5f)
                .clip(shape)
                .background(home?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceContainer)
                .border(2.dp, MaterialTheme.colorScheme.outline, shape)
        ) {
            bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            DesignBackgroundEffects(Modifier.fillMaxSize())
            Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                MiniSurface(design.elements["search_field"] ?: design.box, RoundedCornerShape(50), "search", Modifier.fillMaxWidth().height(18.dp))
                MiniSurface(design.box, RoundedCornerShape(12.dp), "widgets", Modifier.fillMaxWidth().height(60.dp))
                MiniSurface(design.dock ?: design.box, RoundedCornerShape(14.dp), "dock", Modifier.fillMaxWidth().height(34.dp)) {
                    Row(modifier = Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(4) { Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(Color(design.accent ?: 0xFF0758F5.toInt()).copy(alpha = 0.7f))) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSurface(
    appearance: ElementAppearance?,
    shape: androidx.compose.ui.graphics.Shape,
    target: String,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val look = appearance ?: ElementAppearance(borderEnabled = true)
    val colors = resolveElementColors(look)
    Box(
        modifier = modifier
            .designGlow(shape, target)
            .clip(shape)
            .background(colors.background, alpha = colors.backgroundAlpha)
            .then(if (look.borderEnabled) Modifier.border(1.dp, colors.borderColor, shape) else Modifier),
        content = content
    )
}
