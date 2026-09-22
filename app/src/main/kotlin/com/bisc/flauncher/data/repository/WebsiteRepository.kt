package com.bisc.flauncher.data.repository

import com.bisc.flauncher.data.db.PinnedWebsiteDao
import com.bisc.flauncher.data.model.PinnedWebsite
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WebsiteRepository @Inject constructor(
    private val dao: PinnedWebsiteDao
) {
    val websites: Flow<List<PinnedWebsite>> = dao.observeAll()

    suspend fun add(label: String, url: String): Long =
        dao.insert(PinnedWebsite(label = label, url = url))

    suspend fun remove(website: PinnedWebsite) {
        dao.delete(website)
    }
}
