package com.bisc.flauncher.ui.home.widgets

import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.NowPlayingInfo
import com.bisc.flauncher.ui.home.HomeViewModel
import kotlinx.coroutines.delay

private val FULL_MIN_HEIGHT = 150.dp

private const val MAX_INLINE_CUSTOM_ACTIONS = 2

/** Now-playing widget with cover, progress, transport controls and player actions. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicWidget(
    modifier: Modifier = Modifier,
    appearance: ElementAppearance = ElementAppearance(),
    cornerRadiusPercent: Int = com.bisc.flauncher.ui.theme.DEFAULT_CORNER_RADIUS_PERCENT,
    horizontalMargin: Dp = 0.dp,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // Re-syncs with the media sessions every time the home screen comes back, not just once —
    // a player started or switched while the launcher was in the background shows up right away.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshWidgetData()
        onPauseOrDispose { }
    }
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val notificationAccessGranted by viewModel.notificationAccessGranted.collectAsState()
    val context = LocalContext.current

    WidgetCard(
        modifier = modifier,
        appearance = appearance,
        cornerRadiusPercent = cornerRadiusPercent,
        horizontalMargin = horizontalMargin,
        contentPadding = 10.dp
    ) {
        val info = nowPlaying
        if (info == null && !notificationAccessGranted) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { com.bisc.flauncher.util.openNotificationAccessSettings(context) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.6f))
                Text(
                    stringResource(R.string.widget_music_enable_access),
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        } else {
            val shown = info ?: NowPlayingInfo(
                title = stringResource(R.string.widget_music_placeholder_title),
                artist = "", album = "", art = null, isPlaying = false, packageName = "", appLabel = "",
                durationMs = 0L, positionMs = PlaybackState.PLAYBACK_POSITION_UNKNOWN, positionUpdatedAt = 0L, speed = 1f,
                actions = 0L, customActions = emptyList(), otherPlayers = emptyList(), isLive = false
            )
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (maxHeight >= FULL_MIN_HEIGHT) FullPlayer(shown, viewModel) else CompactPlayer(shown, viewModel)
            }
        }
    }
}

@Composable
private fun rememberLivePosition(info: NowPlayingInfo): Long {
    var position by remember(info) { mutableLongStateOf(currentPosition(info)) }
    LaunchedEffect(info) {
        position = currentPosition(info)
        while (info.isPlaying && info.durationMs > 0) {
            delay(500)
            position = currentPosition(info)
        }
    }
    return position
}

private fun currentPosition(info: NowPlayingInfo): Long {
    if (info.positionMs == PlaybackState.PLAYBACK_POSITION_UNKNOWN) return 0L
    val advanced = if (info.isPlaying) ((SystemClock.elapsedRealtime() - info.positionUpdatedAt) * info.speed).toLong() else 0L
    val position = info.positionMs + advanced.coerceAtLeast(0L)
    return if (info.durationMs > 0) position.coerceIn(0L, info.durationMs) else position.coerceAtLeast(0L)
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private fun NowPlayingInfo.supports(action: Long) = actions and action != 0L

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumArt(info: NowPlayingInfo, size: Dp, shape: androidx.compose.ui.graphics.Shape, viewModel: HomeViewModel) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        val bitmap = info.art?.asImageBitmap()
        val clickModifier = Modifier
            .size(size)
            .clip(shape)
            .combinedClickable(
                onClick = viewModel::openNowPlayingPlayer,
                onLongClick = { if (info.otherPlayers.isNotEmpty()) menuOpen = true }
            )
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = stringResource(R.string.widget_music_open_player), contentScale = ContentScale.Crop, modifier = clickModifier)
        } else {
            Box(
                modifier = clickModifier.background(LocalContentColor.current.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = stringResource(R.string.widget_music_open_player), modifier = Modifier.size(size / 2))
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            info.otherPlayers.forEach { player ->
                DropdownMenuItem(
                    text = { Text(player.label.ifBlank { player.packageName }) },
                    onClick = {
                        menuOpen = false
                        viewModel.selectNowPlayingPlayer(player.packageName)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressRow(info: NowPlayingInfo, position: Long, viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    if (info.durationMs <= 0) return
    var dragging by remember { mutableStateOf<Float?>(null) }
    val fraction = (position.toFloat() / info.durationMs).coerceIn(0f, 1f)
    val shownMillis = dragging?.let { (it * info.durationMs).toLong() } ?: position
    Column(modifier = modifier) {
        if (info.supports(PlaybackState.ACTION_SEEK_TO)) {
            Slider(
                value = dragging ?: fraction,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { viewModel.seekNowPlaying((it * info.durationMs).toLong()) }
                    dragging = null
                },
                modifier = Modifier.fillMaxWidth().height(24.dp)
            )
        } else {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                color = LocalContentColor.current,
                trackColor = LocalContentColor.current.copy(alpha = 0.2f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(shownMillis), style = MaterialTheme.typography.labelSmall, color = LocalContentColor.current.copy(alpha = 0.6f))
            Text(formatTime(info.durationMs), style = MaterialTheme.typography.labelSmall, color = LocalContentColor.current.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun PlayPauseButton(info: NowPlayingInfo, viewModel: HomeViewModel, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = viewModel::toggleNowPlaying),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (info.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(if (info.isPlaying) R.string.widget_music_pause else R.string.widget_music_play),
            tint = Color.White,
            modifier = Modifier.size(size / 2)
        )
    }
}

@Composable
private fun CustomActionIcon(action: com.bisc.flauncher.data.model.NowPlayingCustomAction, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        val bitmap = action.icon?.asImageBitmap()
        if (bitmap != null) Icon(bitmap, contentDescription = action.name, modifier = Modifier.size(22.dp))
        else Text(action.name.take(2), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullPlayer(info: NowPlayingInfo, viewModel: HomeViewModel) {
    val position = rememberLivePosition(info)
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                if (info.appLabel.isNotBlank()) {
                    Text(
                        info.appLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    info.title,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (info.artist.isNotBlank()) {
                    Text(
                        info.artist,
                        color = LocalContentColor.current.copy(alpha = 0.7f),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (info.album.isNotBlank()) {
                    Text(
                        info.album,
                        color = LocalContentColor.current.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                ProgressRow(info, position, viewModel, modifier = Modifier.padding(top = 6.dp))
            }
            AlbumArt(info, 96.dp, RoundedCornerShape(16.dp), viewModel)
        }

        val inline = info.customActions.take(MAX_INLINE_CUSTOM_ACTIONS)
        val overflow = info.customActions.drop(MAX_INLINE_CUSTOM_ACTIONS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            inline.forEach { action -> CustomActionIcon(action) { viewModel.sendNowPlayingCustomAction(action.id) } }
            if (info.supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS) || info.actions == 0L) {
                IconButton(onClick = viewModel::skipPrevious, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.widget_music_previous))
                }
            }
            PlayPauseButton(info, viewModel, 44.dp)
            if (info.supports(PlaybackState.ACTION_SKIP_TO_NEXT) || info.actions == 0L) {
                IconButton(onClick = viewModel::skipNext, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.widget_music_next))
                }
            }
            if (overflow.isNotEmpty()) {
                var open by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { open = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.widget_music_more_actions))
                    }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        overflow.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.name) },
                                onClick = {
                                    open = false
                                    viewModel.sendNowPlayingCustomAction(action.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPlayer(info: NowPlayingInfo, viewModel: HomeViewModel) {
    val position = rememberLivePosition(info)
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(info, 40.dp, CircleShape, viewModel)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    info.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    info.artist,
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = viewModel::skipPrevious, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.widget_music_previous))
            }
            Box(modifier = Modifier.padding(horizontal = 8.dp)) { PlayPauseButton(info, viewModel, 36.dp) }
            IconButton(onClick = viewModel::skipNext, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.widget_music_next))
            }
        }
        if (info.durationMs > 0) {
            LinearProgressIndicator(
                progress = { (position.toFloat() / info.durationMs).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(2.dp),
                color = LocalContentColor.current,
                trackColor = LocalContentColor.current.copy(alpha = 0.15f)
            )
        }
    }
}
