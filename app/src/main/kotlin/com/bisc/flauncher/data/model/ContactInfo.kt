package com.bisc.flauncher.data.model

data class ContactInfo(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String? = null,
    val phoneNumbers: List<ContactPhoneNumber> = emptyList()
)

/** [type] is a `ContactsContract.CommonDataKinds.Phone.TYPE_*` constant; [label] is only set when
 *  [type] is `TYPE_CUSTOM`. */
data class ContactPhoneNumber(val number: String, val type: Int, val label: String?)
