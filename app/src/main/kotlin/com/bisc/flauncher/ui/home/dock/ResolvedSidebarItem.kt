package com.bisc.flauncher.ui.home.dock

import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.PinnedShortcut
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.model.SidebarItem
import com.bisc.flauncher.data.model.SidebarItemType

/** A sidebar item with its payload resolved to the app, shortcut, place or category it refers to. */
sealed interface ResolvedSidebarItem {
    val raw: SidebarItem

    data class Category(override val raw: SidebarItem, val entryType: SidebarEntryType) : ResolvedSidebarItem
    data class App(override val raw: SidebarItem, val app: AppInfo) : ResolvedSidebarItem
    data class Shortcut(override val raw: SidebarItem, val shortcut: PinnedShortcut) : ResolvedSidebarItem
    data class MapTarget(override val raw: SidebarItem, val target: SavedMapTarget) : ResolvedSidebarItem
}

/** Resolves sidebar items against the known apps, shortcuts and places; unresolvable items are dropped. */
fun resolveSidebarItems(
    items: List<SidebarItem>,
    apps: List<AppInfo>,
    shortcuts: List<PinnedShortcut>,
    mapTargets: List<SavedMapTarget>
): List<ResolvedSidebarItem> {
    val appsByComponent = apps.associateBy { it.componentName.flattenToString() }
    val shortcutsById = shortcuts.associateBy { it.id }
    val mapTargetsById = mapTargets.associateBy { it.id }
    return items.mapNotNull { item ->
        when (item.type) {
            SidebarItemType.CATEGORY ->
                runCatching { SidebarEntryType.valueOf(item.payload) }.getOrNull()
                    ?.let { ResolvedSidebarItem.Category(item, it) }
            SidebarItemType.APP ->
                appsByComponent[item.payload]?.let { ResolvedSidebarItem.App(item, it) }
            SidebarItemType.SHORTCUT ->
                item.payload.toLongOrNull()?.let { shortcutsById[it] }?.let { ResolvedSidebarItem.Shortcut(item, it) }
            SidebarItemType.MAP_TARGET ->
                item.payload.toLongOrNull()?.let { mapTargetsById[it] }?.let { ResolvedSidebarItem.MapTarget(item, it) }
        }
    }
}
