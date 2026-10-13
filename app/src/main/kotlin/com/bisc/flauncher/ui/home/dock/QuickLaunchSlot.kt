package com.bisc.flauncher.ui.home.dock

import android.content.ComponentName
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.bisc.flauncher.data.model.LauncherPageKind

/**
 * One rendered dock slot — resolved from [com.bisc.flauncher.data.model.DockSlot] (DB row: type +
 * componentName/linkedPageId + optional color overrides) plus live app/page info. Every slot renders
 * as the exact same uniform squircle button regardless of kind (see [DockDimens]/[SearchBar]) — there's
 * no special "Hub orb" or fixed power-button action anymore; a [PageLink] slot just shows whatever
 * page it's linked to (see [PageIconGlyph]) and opens it on tap — pages are autonomous and look/work
 * the same whether or not any dock slot links to them.
 */
sealed interface DockSlotUi {
    val index: Int
    val backgroundColor: Color
    val iconColor: Color

    /** [componentName] null means "not set yet" — shows a neutral add-placeholder, same invitation the old dock's "Not set" slots gave. */
    data class App(
        override val index: Int,
        val componentName: ComponentName?,
        val label: String?,
        override val backgroundColor: Color,
        override val iconColor: Color = Color.White
    ) : DockSlotUi

    /** [pageId] null means the slot's linked page no longer exists (deleted) — rendered the same as an unset App slot. */
    data class PageLink(
        override val index: Int,
        val pageId: Long?,
        val pageKind: LauncherPageKind,
        val glyph: PageIconGlyph,
        override val backgroundColor: Color,
        override val iconColor: Color = Color.White
    ) : DockSlotUi
}

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
