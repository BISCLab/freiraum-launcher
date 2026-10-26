package com.bisc.flauncher.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.bisc.flauncher.data.db.PinnedContactDao
import com.bisc.flauncher.data.model.ContactInfo
import com.bisc.flauncher.data.model.ContactPhoneNumber
import com.bisc.flauncher.data.model.PinnedContact
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Contact search/listing degrades gracefully to an empty result list without READ_CONTACTS. */
class ContactsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pinnedContactDao: PinnedContactDao
) {
    val pinnedContacts: Flow<List<PinnedContact>> = pinnedContactDao.observeAll()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun search(query: String): List<ContactInfo> {
        if (!hasPermission() || query.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            val results = LinkedHashMap<Long, ContactInfo>()

            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                CONTACT_PROJECTION,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?",
                arrayOf("%$query%"),
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC LIMIT 20"
            )?.use { cursor -> readContacts(cursor, results) }

            val digits = query.filter { it.isDigit() }
            if (digits.length >= 3) {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                    ),
                    "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?",
                    arrayOf("%$query%"),
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC LIMIT 20"
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                    val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                    val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        if (id !in results) {
                            results[id] = ContactInfo(id, cursor.getString(lookupIndex), cursor.getString(nameIndex) ?: "", cursor.getString(photoIndex))
                        }
                    }
                }
            }

            attachPhoneNumbers(results)
            results.values.toList()
        }
    }

    suspend fun getAll(): List<ContactInfo> {
        if (!hasPermission()) return emptyList()
        return withContext(Dispatchers.IO) {
            val results = LinkedHashMap<Long, ContactInfo>()
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                CONTACT_PROJECTION,
                null,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC LIMIT $MAX_LISTED"
            )?.use { cursor -> readContacts(cursor, results) }
            results.values.toList()
        }
    }

    /** Fills in [ContactInfo.phoneNumbers] for an already-built result set, one batched query
     *  instead of one per contact — result sets here are search-sized (<=20), so this stays cheap. */
    private fun attachPhoneNumbers(results: MutableMap<Long, ContactInfo>) {
        if (results.isEmpty()) return
        val placeholders = results.keys.joinToString(",") { "?" }
        val numbersByContact = mutableMapOf<Long, MutableList<ContactPhoneNumber>>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN ($placeholders)",
            results.keys.map { it.toString() }.toTypedArray(),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)
            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idIndex)
                val number = cursor.getString(numberIndex) ?: continue
                numbersByContact.getOrPut(contactId) { mutableListOf() }
                    .add(ContactPhoneNumber(number, cursor.getInt(typeIndex), cursor.getString(labelIndex)))
            }
        }
        numbersByContact.forEach { (id, numbers) ->
            results[id]?.let { results[id] = it.copy(phoneNumbers = numbers) }
        }
    }

    private fun readContacts(cursor: android.database.Cursor, into: MutableMap<Long, ContactInfo>) {
        val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
        val lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
        val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
        val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            into[id] = ContactInfo(id, cursor.getString(lookupIndex), cursor.getString(nameIndex) ?: "", cursor.getString(photoIndex))
        }
    }

    suspend fun pin(contact: ContactInfo) {
        if (pinnedContactDao.countByContactId(contact.id) == 0) {
            pinnedContactDao.insert(
                PinnedContact(contactId = contact.id, lookupKey = contact.lookupKey, displayName = contact.displayName, photoUri = contact.photoUri)
            )
        }
    }

    private companion object {
        const val MAX_LISTED = 500
        val CONTACT_PROJECTION = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI
        )
    }
}
