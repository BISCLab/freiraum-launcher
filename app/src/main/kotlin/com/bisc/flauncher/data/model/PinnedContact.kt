package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A pinned contact, for contact entries placed in the Dock, the Panel or an element box. */
@Entity(tableName = "pinned_contacts")
data class PinnedContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String? = null
)
