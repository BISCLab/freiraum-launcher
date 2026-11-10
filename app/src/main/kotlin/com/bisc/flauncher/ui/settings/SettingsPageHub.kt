package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisc.flauncher.R

/** The start of a page with two sides (Text search, App list, App grid): what the page is, then two
 *  large cards - Appearance (how it looks) and Behavior (what it does) - each saying what's inside. */
@Composable
internal fun AppearanceBehaviorHub(
    title: String,
    intro: String,
    appearanceContents: String,
    behaviorContents: String,
    navController: NavController,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    SettingsSubScreen(title = title, navController = navController, onBack = onBack) {
        Text(
            intro,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HubCard(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.settings_home_appearance_title),
                what = stringResource(R.string.settings_hub_appearance_what),
                contents = appearanceContents,
                onClick = { onNavigate("appearance") }
            )
            HubCard(
                icon = Icons.Filled.Tune,
                title = stringResource(R.string.settings_search_behavior_label),
                what = stringResource(R.string.settings_hub_behavior_what),
                contents = behaviorContents,
                onClick = { onNavigate("behavior") }
            )
        }
    }
}

@Composable
internal fun HubCard(icon: ImageVector, title: String, what: String, contents: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(what, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(contents, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}
