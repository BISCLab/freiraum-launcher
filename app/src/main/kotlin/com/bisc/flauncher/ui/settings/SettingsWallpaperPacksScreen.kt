package com.bisc.flauncher.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.data.wallpaper.WallpaperPack
import com.bisc.flauncher.data.wallpaper.WallpaperPackRepository
import com.bisc.flauncher.ui.theme.MATTE_DARK_COLOR
import com.bisc.flauncher.ui.theme.MATTE_LIGHT_COLOR
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
class WallpaperPacksViewModel @Inject constructor(
    private val repository: WallpaperPackRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {
    val packs: StateFlow<List<WallpaperPack>> = repository.packs
    val activeId: StateFlow<String?> = settingsRepository.activeWallpaperPackId.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init { viewModelScope.launch { repository.refresh() } }

    fun import(uri: Uri, onDone: (String?) -> Unit) = viewModelScope.launch { onDone(repository.import(uri)) }
    fun apply(pack: WallpaperPack, lockScreen: Boolean, onDone: () -> Unit) = viewModelScope.launch { repository.apply(pack, lockScreen); onDone() }
    fun applyPlain(onDone: () -> Unit) = viewModelScope.launch { repository.applyPlain(); onDone() }
    fun delete(pack: WallpaperPack) = viewModelScope.launch { repository.delete(pack) }
}

/** Wallpaper packs: install from a file, preview, apply to the pages, or go without pictures. */
@Composable
fun SettingsWallpaperPacksScreen(navController: NavController, onBack: () -> Unit, viewModel: WallpaperPacksViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val packs by viewModel.packs.collectAsState()
    val activeId by viewModel.activeId.collectAsState()
    var previewing by remember { mutableStateOf<WallpaperPack?>(null) }
    var removing by remember { mutableStateOf<WallpaperPack?>(null) }
    var confirmingPlain by remember { mutableStateOf(false) }
    val german = LocalConfiguration.current.locales.get(0).language == "de"
    val applied = { Toast.makeText(context, R.string.packs_applied, Toast.LENGTH_SHORT).show() }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri) { name ->
            Toast.makeText(
                context,
                if (name != null) context.getString(R.string.packs_import_done, name) else context.getString(R.string.packs_import_failed),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    SettingsSubScreen(title = stringResource(R.string.packs_title), navController = navController, onBack = onBack) {
        Text(
            stringResource(R.string.packs_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.packs_find),
                summary = stringResource(R.string.packs_find_summary),
                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PACKS_URL))) } },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.packs_add),
                summary = stringResource(R.string.packs_add_summary),
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                trailing = null
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.packs_installed)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.packs_plain),
                summary = listOfNotNull(
                    stringResource(R.string.packs_plain_summary),
                    if (activeId == WallpaperPackRepository.PLAIN) stringResource(R.string.designs_active) else null
                ).joinToString(" · "),
                onClick = { confirmingPlain = true },
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(end = 12.dp)) {
                        listOf(MATTE_LIGHT_COLOR, MATTE_DARK_COLOR).forEach {
                            Box(Modifier.size(width = 14.dp, height = 24.dp).clip(RoundedCornerShape(4.dp)).background(Color(it)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)))
                        }
                    }
                }
            )
            packs.forEach { pack ->
                SettingsPreference(
                    icon = null,
                    title = pack.displayName(german),
                    summary = listOfNotNull(
                        pack.dark?.let { stringResource(if (it) R.string.settings_theme_dark else R.string.settings_theme_light) },
                        stringResource(R.string.packs_picture_count, pack.images.size - (if (pack.lock != null) 1 else 0) + pack.extras.size),
                        if (pack.id == activeId) stringResource(R.string.designs_active) else null
                    ).joinToString(" · "),
                    onClick = { previewing = pack },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PackThumb(pack.preview, 24.dp)
                            IconButton(onClick = { removing = pack }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.designs_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                )
            }
        }
    }

    if (confirmingPlain) {
        AlertDialog(
            onDismissRequest = { confirmingPlain = false },
            title = { Text(stringResource(R.string.packs_plain)) },
            text = { Text(stringResource(R.string.packs_plain_text)) },
            confirmButton = {
                TextButton(onClick = { confirmingPlain = false; viewModel.applyPlain(applied) }) { Text(stringResource(R.string.designs_apply)) }
            },
            dismissButton = { TextButton(onClick = { confirmingPlain = false }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }

    removing?.let { pack ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.designs_remove_title, pack.displayName(german))) },
            text = { Text(stringResource(R.string.packs_remove_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(pack); removing = null }) {
                    Text(stringResource(R.string.designs_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }

    previewing?.let { pack ->
        var lockScreen by remember(pack.id) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { previewing = null },
            title = { Text(pack.displayName(german)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOfNotNull(pack.images["home"], pack.images["appList"], pack.images["search"], pack.images["appGrid"]) + pack.extras + listOfNotNull(pack.lock))
                            .forEach { PackThumb(it, 72.dp) }
                    }
                    Text(stringResource(R.string.packs_apply_text), style = MaterialTheme.typography.bodyMedium)
                    if (pack.lock != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = lockScreen, onCheckedChange = { lockScreen = it })
                            Text(stringResource(R.string.designs_lock_screen_too), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.apply(pack, lockScreen, applied); previewing = null }) {
                    Text(stringResource(R.string.designs_apply))
                }
            },
            dismissButton = { TextButton(onClick = { previewing = null }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }
}

private const val PACKS_URL = "https://github.com/BISCLab/freiraum-launcher/tree/master/wallpaper-packs"

/** A small portrait thumbnail of one picture, decoded at a fraction of its size. */
@Composable
private fun PackThumb(file: File?, height: Dp) {
    val shape = RoundedCornerShape(if (height > 40.dp) 10.dp else 4.dp)
    val bitmap by produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) {
            file?.takeIf(File::isFile)?.let {
                runCatching {
                    val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = if (height > 40.dp) 8 else 16 }
                    android.graphics.BitmapFactory.decodeFile(it.absolutePath, opts)?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    Box(
        modifier = Modifier
            .height(height * 2)
            .aspectRatio(0.5f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
    ) {
        bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}
