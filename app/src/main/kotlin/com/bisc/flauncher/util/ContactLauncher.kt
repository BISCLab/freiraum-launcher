package com.bisc.flauncher.util

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import com.bisc.flauncher.data.model.ContactInfo

fun openContact(context: Context, contact: ContactInfo) {
    val uri = ContactsContract.Contacts.getLookupUri(contact.id, contact.lookupKey)
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
