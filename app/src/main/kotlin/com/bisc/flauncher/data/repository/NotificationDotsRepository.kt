package com.bisc.flauncher.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The apps that have a notification right now, by package; only which apps, never what they say.
 *  Filled by [com.bisc.flauncher.util.NowPlayingListenerService] while notification access is on. */
@Singleton
class NotificationDotsRepository @Inject constructor() {
    private val _packages = MutableStateFlow<Set<String>>(emptySet())
    val packages: StateFlow<Set<String>> = _packages.asStateFlow()

    fun set(packages: Set<String>) {
        if (_packages.value != packages) _packages.value = packages
    }
}
