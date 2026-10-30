package com.bisc.flauncher.util

import android.service.notification.NotificationListenerService
import com.bisc.flauncher.data.repository.NowPlayingRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Exists only so notification access unlocks the MediaSession API - we don't read notification
 *  content. The system unbinds it on every app update and rebinds it some time later; reading media
 *  sessions fails in between, so each (re)connect re-syncs the music widget. */
@AndroidEntryPoint
class NowPlayingListenerService : NotificationListenerService() {
    @Inject lateinit var nowPlayingRepository: NowPlayingRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        nowPlayingRepository.refresh()
    }
}
