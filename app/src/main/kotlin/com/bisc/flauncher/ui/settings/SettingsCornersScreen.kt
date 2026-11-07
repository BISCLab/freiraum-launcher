package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.home.OnboardingViewModel
import com.bisc.flauncher.ui.home.ShapeChoice

/** Corners of every part at once, the same screens and table as in the welcome tour. Each part's own
 *  settings can still fine-tune it afterwards. */
@Composable
fun SettingsCornersScreen(navController: NavController, onBack: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    SettingsSubScreen(title = stringResource(R.string.settings_corners_all), navController = navController, onBack = onBack) {
        Text(
            stringResource(R.string.settings_corners_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        ShapeChoice(viewModel)
    }
}
