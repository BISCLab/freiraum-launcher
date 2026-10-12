package com.bisc.flauncher.data.repository

import com.bisc.flauncher.data.db.DockSlotDao
import com.bisc.flauncher.data.model.DockPowerButtonAction
import com.bisc.flauncher.data.model.DockSlot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Thin repository over [DockSlotDao]. The slot list is flexible-length; add/remove/reorder all go through [setSlots]. */
class DockRepository @Inject constructor(private val dao: DockSlotDao) {
    val slots: Flow<List<DockSlot>> = dao.observeAll()

    /** Fresh-install default: both power buttons (immediately useful) plus 4 empty app slots to invite
     *  the user to fill in — mirrors the old fixed-4-slots dock's own "Not set" placeholder invitation. */
    suspend fun ensureSeeded() {
        if (dao.count() > 0) return
        val defaults = listOf(
            DockSlot(slotIndex = 0, type = "APP"),
            DockSlot(slotIndex = 1, type = "APP"),
            DockSlot(slotIndex = 2, type = "APP"),
            DockSlot(slotIndex = 3, type = "APP"),
            DockSlot(slotIndex = 4, type = "POWER_BUTTON", powerButtonAction = DockPowerButtonAction.TEXT_SEARCH.name),
            DockSlot(slotIndex = 5, type = "POWER_BUTTON", powerButtonAction = DockPowerButtonAction.CATEGORY_SEARCH.name)
        )
        defaults.forEach { dao.upsert(it) }
    }

    /** Replaces the whole list, re-indexed 0..size-1 in the given order. */
    suspend fun setSlots(slots: List<DockSlot>) {
        dao.replaceAll(slots.mapIndexed { index, slot -> slot.copy(slotIndex = index) })
    }

    suspend fun addAppSlot(componentName: String, currentSlots: List<DockSlot>) {
        setSlots(currentSlots + DockSlot(slotIndex = currentSlots.size, type = "APP", componentName = componentName))
    }

    suspend fun addPowerButtonSlot(action: DockPowerButtonAction, currentSlots: List<DockSlot>) {
        setSlots(currentSlots + DockSlot(slotIndex = currentSlots.size, type = "POWER_BUTTON", powerButtonAction = action.name))
    }
}
