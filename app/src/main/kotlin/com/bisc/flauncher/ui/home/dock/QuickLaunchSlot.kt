package com.bisc.flauncher.ui.home.dock

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val UNSET_APP_SLOT_COLOR = Color(0xFF6B7280)

/** Default accent color per glyph — distinct, legible colors for each of the 4 built-in page looks. */
fun PageIconGlyph.defaultColor(): Color = when (this) {
    PageIconGlyph.SEARCH -> Color(0xFF1AF0FF)
    PageIconGlyph.APP_DRAWER -> Color(0xFF6366F1)
    PageIconGlyph.APP_LIST -> Color(0xFFF43F5E)
    PageIconGlyph.WIDGET_PAGE -> Color(0xFFA14AFF)
}

/** Placeholder glyph for an unset App slot — invites the user to tap and assign one. */
val unsetAppSlotIcon: ImageVector = Icons.Filled.Add
