package com.bisc.flauncher.data.model

import android.graphics.Bitmap

/** One extra button the playing app offers besides previous/play/next (shuffle, repeat, like...). */
data class NowPlayingCustomAction(val id: String, val name: String, val icon: Bitmap?)

/** Another app with an active media session. */
data class NowPlayingPlayer(val packageName: String, val label: String)

/** Everything the music widget shows about the current media session. */
data class NowPlayingInfo(
    val title: String,
    val artist: String,
    val album: String,
    val art: Bitmap?,
    val isPlaying: Boolean,
    val packageName: String,
    val appLabel: String,
    val durationMs: Long,
    val positionMs: Long,
    val positionUpdatedAt: Long,
    val speed: Float,
    val actions: Long,
    val customActions: List<NowPlayingCustomAction>,
    val otherPlayers: List<NowPlayingPlayer>,
    val isLive: Boolean = true
)
