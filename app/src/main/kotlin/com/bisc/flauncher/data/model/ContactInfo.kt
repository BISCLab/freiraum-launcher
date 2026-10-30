package com.bisc.flauncher.data.model

data class ContactInfo(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String? = null,
    val phoneNumbers: List<ContactPhoneNumber> = emptyList(),
    /** The contact's e-mail address — the one the search matched, if it matched one. */
    val email: String? = null,
    /** Whether the search found this contact by [email] (then shown under the name). */
    val matchedByEmail: Boolean = false
)

/** [type] is a `ContactsContract.CommonDataKinds.Phone.TYPE_*` constant; [label] is only set when
 *  [type] is `TYPE_CUSTOM`. */
data class ContactPhoneNumber(val number: String, val type: Int, val label: String?)
