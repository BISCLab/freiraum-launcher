package com.bisc.flauncher.data.model

data class ContactInfo(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String? = null
)
