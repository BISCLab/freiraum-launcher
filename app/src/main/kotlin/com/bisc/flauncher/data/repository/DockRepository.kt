package com.bisc.flauncher.data.repository

import com.bisc.flauncher.data.db.DockSlotDao
import com.bisc.flauncher.data.model.DockSlot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Thin repository over [DockSlotDao]. The slot list is flexible-length but capped at
 *  [MAX_SLOTS]; add/remove/reorder all go through [setSlots]. */
class DockRepository @Inject constructor(private val dao: DockSlotDao) {
    val slots: Flow<List<DockSlot>> = dao.observeAll()

    /** Fresh-install default: 3 empty app slots plus the two auto-seeded singleton pages (search,
     *  app overview) linked — exactly [MAX_SLOTS] slots total, so the default dock arrives already
     *  "full" at its default fixed width instead of looking sparse inside it. Called once
     *  [com.bisc.flauncher.data.repository.PageContentRepository.ensureSeeded] has run, so the two
     *  page IDs already exist. */
    suspend fun ensureSeeded(searchPageId: Long, appOverviewPageId: Long) {
        if (dao.count() > 0) return
        val defaults = listOf(
            DockSlot(slotIndex = 0, type = "APP"),
            DockSlot(slotIndex = 1, type = "APP"),
            DockSlot(slotIndex = 2, type = "APP"),
            DockSlot(slotIndex = 3, type = "PAGE", linkedPageId = searchPageId),
            DockSlot(slotIndex = 4, type = "PAGE", linkedPageId = appOverviewPageId)
        )
        defaults.forEach { dao.upsert(it) }
    }

    /** Replaces the whole list, re-indexed 0..size-1 in the given order, capped to [MAX_SLOTS]. */
    suspend fun setSlots(slots: List<DockSlot>) {
        dao.replaceAll(slots.take(MAX_SLOTS).mapIndexed { index, slot -> slot.copy(slotIndex = index) })
    }

    suspend fun addAppSlot(componentName: String, currentSlots: List<DockSlot>) {
        if (currentSlots.size >= MAX_SLOTS) return
        setSlots(currentSlots + DockSlot(slotIndex = currentSlots.size, type = "APP", componentName = componentName))
    }

    suspend fun addPageSlot(pageId: Long, currentSlots: List<DockSlot>) {
        if (currentSlots.size >= MAX_SLOTS) return
        setSlots(currentSlots + DockSlot(slotIndex = currentSlots.size, type = "PAGE", linkedPageId = pageId))
    }

    companion object {
        const val MAX_SLOTS = 5
    }
}
