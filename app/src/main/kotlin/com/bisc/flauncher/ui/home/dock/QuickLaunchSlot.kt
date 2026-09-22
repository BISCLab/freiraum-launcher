package com.bisc.flauncher.ui.home.dock

import android.content.ComponentName
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sms
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** One of the dock's 4 quick-launch slots. */
data class QuickLaunchSlot(
    val index: Int,
    val componentName: ComponentName? = null,
    val label: String? = null,
    val placeholderIcon: ImageVector,
    val placeholderTint: Color
)

val defaultQuickLaunchSlots = listOf(
    QuickLaunchSlot(index = 0, placeholderIcon = Icons.Filled.Call, placeholderTint = Color(0xFF3B82F6)),
    QuickLaunchSlot(index = 1, placeholderIcon = Icons.Filled.Sms, placeholderTint = Color(0xFF22C55E)),
    QuickLaunchSlot(index = 2, placeholderIcon = Icons.Filled.CameraAlt, placeholderTint = Color(0xFFF59E0B)),
    QuickLaunchSlot(index = 3, placeholderIcon = Icons.Filled.MusicNote, placeholderTint = Color(0xFFEC4899))
)
