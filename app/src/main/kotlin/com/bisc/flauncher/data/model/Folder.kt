package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconMode: FolderIconMode = FolderIconMode.AUTO_STACK,
    val iconColor: String? = null,
    val iconPackPackage: String? = null,
    val iconPackDrawable: String? = null
)
