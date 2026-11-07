package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R

/** Corners of every part at once, the same screens and table as in the welcome tour. Nothing
 *  changes until Save; leaving with changes asks first. */
@Composable
fun SettingsCornersScreen(navController: NavController, onBack: () -> Unit, viewModel: PartCornersViewModel = hiltViewModel()) {
    val saved by viewModel.corners.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadCorners() }
    var draft by rememberDraft(saved)
    SettingsSubScreen(
        title = stringResource(R.string.settings_corners_all),
        navController = navController,
        onBack = onBack,
        onSave = { viewModel.saveCorners(draft) },
        dirty = draft != saved
    ) {
        Text(
            stringResource(R.string.settings_corners_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        ShapeEditor(
            corners = draft,
            onAll = { preset -> draft = ShapePart.entries.associateWith { it.percentOf(preset) } },
            onPart = { part, preset -> draft = draft + (part to part.percentOf(preset)) }
        )
    }
}
