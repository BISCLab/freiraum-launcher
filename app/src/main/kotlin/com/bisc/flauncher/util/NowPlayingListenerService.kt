package com.bisc.flauncher.util

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.bisc.flauncher.data.repository.NotificationDotsRepository
import com.bisc.flauncher.data.repository.NowPlayingRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Notification access unlocks the MediaSession API for the music widget, and tells which apps have
 *  a notification for the dots on their icons. The content of notifications is never read. The
 *  system unbinds it on every app update and rebinds it some time later; reading media sessions
 *  fails in between, so each (re)connect re-syncs the music widget. */
@AndroidEntryPoint
class NowPlayingListenerService : NotificationListenerService() {
    @Inject lateinit var nowPlayingRepository: NowPlayingRepository
    @Inject lateinit var notificationDots: NotificationDotsRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        nowPlayingRepository.refresh()
        publishDots()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        notificationDots.set(emptySet())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = publishDots()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publishDots()

    /** Only what a person would clear: no ongoing ones such as music or a running timer. */
    private fun publishDots() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        notificationDots.set(
            active.asSequence()
                .filter { it.isClearable && !it.isOngoing && it.packageName != packageName }
                .map { it.packageName }
                .toSet()
        )
    }
}
