package com.bisc.flauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisc.flauncher.data.design.DesignRepository
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.theme.SettingsThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
/** What the welcome tour does besides the shared settings pieces: its light/dark switch, the quick
 *  setup and the design it always has. */
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val designRepository: DesignRepository
) : ViewModel() {

    /** The tour's light/dark switch: the settings and the tour follow it. */
    fun setTourDark(dark: Boolean) = viewModelScope.launch {
        settingsRepository.setSettingsThemeMode(if (dark) SettingsThemeMode.DARK else SettingsThemeMode.LIGHT)
    }

    /** Everything recommended in one go: the bundled Modern design in the chosen mode. */
    fun quickSetup(dark: Boolean, onDone: () -> Unit) = viewModelScope.launch {
        applyModern(dark)
        onDone()
    }

    /** A design is always chosen: Modern in the tour's mode until another one is picked. */
    fun ensureDesign(dark: Boolean) = viewModelScope.launch {
        if (settingsRepository.activeDesignId.first() == null) applyModern(dark)
    }

    private suspend fun applyModern(dark: Boolean) {
        designRepository.refresh()
        designRepository.designs.value.firstOrNull { it.id == if (dark) "modern-dark" else "modern-light" }?.let { designRepository.apply(it) }
    }

}
