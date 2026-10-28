package com.bisc.flauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.view.KeyEvent
import java.io.File
import java.util.concurrent.Executors
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.bisc.flauncher.data.model.NowPlayingCustomAction
import com.bisc.flauncher.data.model.NowPlayingInfo
import com.bisc.flauncher.data.model.NowPlayingPlayer
import com.bisc.flauncher.util.NowPlayingListenerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Media playback state from the MediaSession API, including the remembered last track. Requires notification access. */
@Singleton
class NowPlayingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mediaSessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, NowPlayingListenerService::class.java)
    private var activeController: MediaController? = null
    private var controllers: List<MediaController> = emptyList()
    private var preferredPackage: String? = null
    // Apps allowed to show up in the widget (package names); empty = every app.
    private var allowedSources: Set<String> = emptySet()
    private var customActionsById: Map<String, PlaybackState.CustomAction> = emptyMap()
    private val customIconCache = HashMap<String, Bitmap?>()
    private var listening = false

    private val cachePrefs = context.getSharedPreferences("now_playing_cache", Context.MODE_PRIVATE)
    private val cacheArtFile = File(context.filesDir, "now_playing_art.jpg")
    private val cacheWriter = Executors.newSingleThreadExecutor()
    private var lastCachedKey: String? = null
    private var remembered: NowPlayingInfo? = loadCached()

    private val _nowPlaying = MutableStateFlow<NowPlayingInfo?>(remembered)
    val nowPlaying: StateFlow<NowPlayingInfo?> = _nowPlaying.asStateFlow()

    private fun isAllowed(packageName: String) = allowedSources.isEmpty() || packageName in allowedSources

    /** Limits the widget to [packages]' playback (empty = every app); a remembered last track from
     *  any other app is dropped. */
    fun setAllowedSources(packages: Set<String>) {
        if (packages == allowedSources) return
        allowedSources = packages
        remembered?.let { if (!isAllowed(it.packageName)) forgetRemembered() }
        refresh()
        // Without access refresh() publishes nothing new; still drop a no-longer-allowed track.
        if (!hasPermission()) _nowPlaying.value = remembered?.let(::asIdle)
    }

    private fun forgetRemembered() {
        remembered = null
        lastCachedKey = null
        cacheWriter.execute {
            runCatching {
                cachePrefs.edit().clear().apply()
                cacheArtFile.delete()
            }
        }
    }

    fun hasPermission(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** (Re)connects to the media sessions and re-reads them. Cheap, so it runs whenever the widget
     *  shows, the home screen resumes, the listener (re)connects, and before every button press —
     *  a one-off connect went stale after app updates (the system unbinds the listener, reading
     *  sessions fails until it's rebound) and when a player replaced its session. */
    fun refresh() {
        val granted = hasPermission()
        if (granted) {
            try {
                if (!listening) {
                    mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, listenerComponent)
                    listening = true
                }
                onSessionsChanged(mediaSessionManager?.getActiveSessions(listenerComponent))
            } catch (e: SecurityException) {
                // Access is granted but the listener isn't bound (yet) — ask the system to rebind it;
                // its onListenerConnected calls back into here once it is.
                runCatching { mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener) }
                listening = false
                runCatching { NotificationListenerService.requestRebind(listenerComponent) }
            }
        } else if (listening) {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
            controllers = emptyList()
            attach(null)
            listening = false
        }
    }

    private fun sendMediaKey(code: Int) {
        // A system-wide media key goes to whichever app played last — possibly one that isn't allowed.
        if (allowedSources.isNotEmpty()) return
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        runCatching {
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        }
    }

    fun togglePlayPause() {
        refresh()
        val controller = activeController ?: return sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        val state = controller.playbackState
        val actions = state?.actions ?: 0L
        val playing = state?.state == PlaybackState.STATE_PLAYING
        // Use play()/pause() only when the session says it supports them; otherwise (some players,
        // e.g. Media3-based ones, only advertise play/pause as a toggle) send that session itself a
        // play/pause media button, which every player handles.
        when {
            playing && actions and PlaybackState.ACTION_PAUSE != 0L -> controller.transportControls.pause()
            !playing && actions and PlaybackState.ACTION_PLAY != 0L -> controller.transportControls.play()
            else -> sendMediaButtonTo(controller, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }
    }

    private fun sendMediaButtonTo(controller: MediaController, code: Int) {
        runCatching {
            controller.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            controller.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        }
    }

    fun skipNext() {
        refresh()
        activeController?.transportControls?.skipToNext() ?: sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun skipPrevious() {
        refresh()
        activeController?.transportControls?.skipToPrevious() ?: sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun seekTo(positionMs: Long) {
        activeController?.transportControls?.seekTo(positionMs)
    }

    fun sendCustomAction(id: String) {
        val action = customActionsById[id] ?: return
        activeController?.transportControls?.sendCustomAction(action, action.extras)
    }

    fun selectPlayer(packageName: String) {
        preferredPackage = packageName
        onSessionsChanged(controllers)
    }

    /** Opens the player — its own "now playing" screen (the session's activity) when it has one,
     *  otherwise the app itself. */
    fun openPlayer() {
        refresh()
        val controller = activeController
        val sessionActivity = controller?.sessionActivity
        val opened = sessionActivity != null && runCatching {
            // Since Android 14 the sender has to explicitly allow the other app's activity to start
            // from its PendingIntent; without this the start is silently blocked (and still looks
            // like a successful send, so no fallback ever ran).
            val options = android.app.ActivityOptions.makeBasic().apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                }
            }
            sessionActivity.send(context, 0, null, null, null, null, options.toBundle())
        }.isSuccess
        if (!opened) {
            val pkg = controller?.packageName ?: remembered?.packageName ?: return
            context.packageManager.getLaunchIntentForPackage(pkg)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?.let { runCatching { context.startActivity(it) } }
        }
    }

    private val sessionListener =
        MediaSessionManager.OnActiveSessionsChangedListener { onSessionsChanged(it) }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publish(activeController)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publish(activeController)
        }

        override fun onSessionDestroyed() {
            attach(null)
        }
    }

    private fun onSessionsChanged(list: List<MediaController>?) {
        controllers = list.orEmpty().filter { isAllowed(it.packageName) }
        // The player the user picked, else whichever is playing right now, else the one already shown
        // (so a re-sync before a button press can't silently hand the buttons to another paused app),
        // else the most recently active.
        val currentPackage = activeController?.packageName
        val next = preferredPackage?.let { pkg -> controllers.firstOrNull { it.packageName == pkg } }
            ?: controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: currentPackage?.let { pkg -> controllers.firstOrNull { it.packageName == pkg } }
            ?: controllers.firstOrNull()
        attach(next)
    }

    private fun attach(controller: MediaController?) {
        activeController?.unregisterCallback(controllerCallback)
        activeController = controller
        controller?.registerCallback(controllerCallback)
        publish(controller)
    }

    private fun publish(controller: MediaController?) {
        val live = buildInfo(controller)
        if (live != null) remember(live)
        _nowPlaying.value = live ?: remembered?.let(::asIdle)
    }

    private fun asIdle(info: NowPlayingInfo) = info.copy(
        isPlaying = false, actions = 0L, customActions = emptyList(), otherPlayers = emptyList(), isLive = false
    )

    private fun remember(info: NowPlayingInfo) {
        remembered = info
        val key = "${info.packageName}|${info.title}|${info.artist}|${info.album}|${info.art != null}"
        if (key == lastCachedKey) return
        lastCachedKey = key
        cacheWriter.execute {
            runCatching {
                cachePrefs.edit()
                    .putString("title", info.title).putString("artist", info.artist).putString("album", info.album)
                    .putString("package", info.packageName).putString("label", info.appLabel)
                    .putLong("duration", info.durationMs).putLong("position", info.positionMs)
                    .apply()
                val art = info.art
                if (art != null) {
                    val scaled = if (maxOf(art.width, art.height) > 512) {
                        val f = 512f / maxOf(art.width, art.height)
                        Bitmap.createScaledBitmap(art, (art.width * f).toInt().coerceAtLeast(1), (art.height * f).toInt().coerceAtLeast(1), true)
                    } else art
                    cacheArtFile.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                } else cacheArtFile.delete()
            }
        }
    }

    private fun loadCached(): NowPlayingInfo? {
        val title = cachePrefs.getString("title", null) ?: return null
        val pkg = cachePrefs.getString("package", "").orEmpty()
        return NowPlayingInfo(
            title = title,
            artist = cachePrefs.getString("artist", "").orEmpty(),
            album = cachePrefs.getString("album", "").orEmpty(),
            art = runCatching { if (cacheArtFile.exists()) BitmapFactory.decodeFile(cacheArtFile.path) else null }.getOrNull(),
            isPlaying = false,
            packageName = pkg,
            appLabel = cachePrefs.getString("label", "").orEmpty(),
            durationMs = cachePrefs.getLong("duration", 0L),
            positionMs = cachePrefs.getLong("position", PlaybackState.PLAYBACK_POSITION_UNKNOWN),
            positionUpdatedAt = 0L,
            speed = 1f,
            actions = 0L,
            customActions = emptyList(),
            otherPlayers = emptyList(),
            isLive = false
        )
    }

    private fun appLabel(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault("")

    private fun customIcon(packageName: String, action: PlaybackState.CustomAction): Bitmap? =
        customIconCache.getOrPut("$packageName/${action.action}/${action.icon}") {
            runCatching {
                val resources = context.packageManager.getResourcesForApplication(packageName)
                ResourcesCompat.getDrawable(resources, action.icon, null)?.toBitmap(96, 96)
            }.getOrNull()
        }

    private fun buildInfo(controller: MediaController?): NowPlayingInfo? {
        val metadata = controller?.metadata ?: return null
        val state = controller.playbackState
        val pkg = controller.packageName
        val custom = state?.customActions.orEmpty()
        customActionsById = custom.associateBy { it.action }
        return NowPlayingInfo(
            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).orEmpty(),
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART),
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            packageName = pkg,
            appLabel = appLabel(pkg),
            durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L),
            positionMs = state?.position ?: PlaybackState.PLAYBACK_POSITION_UNKNOWN,
            positionUpdatedAt = state?.lastPositionUpdateTime ?: 0L,
            speed = state?.playbackSpeed ?: 1f,
            actions = state?.actions ?: 0L,
            customActions = custom.map { NowPlayingCustomAction(it.action, it.name.toString(), customIcon(pkg, it)) },
            otherPlayers = controllers.filter { it.packageName != pkg }.map { NowPlayingPlayer(it.packageName, appLabel(it.packageName)) }
        )
    }
}
