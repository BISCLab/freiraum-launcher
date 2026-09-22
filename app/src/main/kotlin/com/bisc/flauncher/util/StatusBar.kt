package com.bisc.flauncher.util

import android.view.View
import android.view.Window
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Shows or hides the status bar. */
fun Window.setStatusBarHidden(view: View, hidden: Boolean) {
    val controller = WindowInsetsControllerCompat(this, view)
    // Sticky: a swipe reveals the bar temporarily instead of it staying shown, so hidden state holds reliably.
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (hidden) controller.hide(WindowInsetsCompat.Type.statusBars()) else controller.show(WindowInsetsCompat.Type.statusBars())
}
