package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.repository.ProfileApp
import com.bisc.flauncher.data.repository.ProfileKind
import com.bisc.flauncher.data.repository.ProfileSection
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** What the app list needs to show the work profile and the private space. */
class ProfileListHost(
    val sections: StateFlow<List<ProfileSection>>,
    val icon: (ProfileApp) -> ImageBitmap?,
    val launch: (ProfileApp) -> Unit,
    val setLocked: (android.os.UserHandle, Boolean) -> Unit
)

@Composable
fun rememberProfileListHost(viewModel: HomeViewModel): ProfileListHost = remember(viewModel) {
    ProfileListHost(viewModel.profileSections, viewModel::profileIcon, { app -> viewModel.launchProfileApp(app); viewModel.closeAppList() }, viewModel::setProfileLocked)
}

/** Each profile after the own apps: a header with its name and the switch to pause or lock it,
 *  then its apps, or a note while it is off. While searching, only matching apps. */
internal fun LazyListScope.profileSectionItems(
    sections: List<ProfileSection>,
    query: String,
    host: ProfileListHost,
    handedness: Handedness,
    edgeInset: Dp,
    iconSize: Dp
) {
    sections.forEach { section ->
        val apps = if (query.isBlank()) section.apps else section.apps.filter { com.bisc.flauncher.util.matchesAllWordStarts(it.label, query) }
        // While searching, a profile without a match stays out of the way.
        if (query.isNotBlank() && apps.isEmpty()) return@forEach
        item(key = "profile-${section.kind}-${section.user.hashCode()}") {
            ProfileHeader(section, host, edgeInset)
        }
        items(apps, key = { "profile-app-${section.user.hashCode()}-${it.componentName.flattenToShortString()}" }) { app ->
            ProfileAppRow(app, host, handedness, edgeInset, iconSize)
        }
    }
}

@Composable
private fun ProfileHeader(section: ProfileSection, host: ProfileListHost, edgeInset: Dp) {
    val private = section.kind == ProfileKind.PRIVATE
    Column(modifier = Modifier.fillMaxWidth().padding(start = edgeInset, end = edgeInset - 8.dp, top = 28.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (private) R.string.profile_private else R.string.profile_work),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { host.setLocked(section.user, !section.locked) }) {
                Icon(
                    when {
                        private && section.locked -> Icons.Filled.LockOpen
                        private -> Icons.Filled.Lock
                        section.locked -> Icons.Filled.PlayArrow
                        else -> Icons.Filled.Pause
                    },
                    contentDescription = null,
                    tint = LocalContentColor.current,
                    modifier = Modifier.size(18.dp).padding(end = 2.dp)
                )
                Text(
                    stringResource(
                        when {
                            private && section.locked -> R.string.profile_unlock
                            private -> R.string.profile_lock
                            section.locked -> R.string.profile_resume
                            else -> R.string.profile_pause
                        }
                    ),
                    color = LocalContentColor.current,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        if (section.locked) {
            Text(
                stringResource(if (private) R.string.profile_locked else R.string.profile_paused),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ProfileAppRow(app: ProfileApp, host: ProfileListHost, handedness: Handedness, edgeInset: Dp, iconSize: Dp) {
    val icon by produceState<ImageBitmap?>(null, app) { value = withContext(Dispatchers.Default) { host.icon(app) } }
    val isLeft = handedness == Handedness.LEFT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appClickable(onClick = { host.launch(app) }, onLongClick = null)
            .padding(start = edgeInset, end = edgeInset, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, if (isLeft) Alignment.End else Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val image = @Composable { Box(Modifier.size(iconSize)) { icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(iconSize)) } } }
        val label = @Composable { Text(app.label) }
        if (isLeft) { label(); image() } else { image(); label() }
    }
}
