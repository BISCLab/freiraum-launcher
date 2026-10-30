package com.bisc.flauncher.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.bisc.flauncher.data.db.PinnedContactDao
import com.bisc.flauncher.data.model.ContactInfo
import com.bisc.flauncher.data.model.ContactPhoneNumber
import com.bisc.flauncher.data.model.PinnedContact
import com.bisc.flauncher.util.matchesAllWordStarts
import com.bisc.flauncher.util.matchesWordStart
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

    /** Contacts whose name has a word starting with each word of [query] (like every other search
     *  category), whose phone number matches its digits whatever the formatting, or whose e-mail
     *  address has a part starting with it. */
    suspend fun search(query: String): List<ContactInfo> {
        if (!hasPermission() || query.isBlank()) return emptyList()
        val q = query.trim()
        return withContext(Dispatchers.IO) {
            val results = LinkedHashMap<Long, ContactInfo>()

            // Names are matched here rather than in SQL, which can't match word starts or ignore accents.
            val all = LinkedHashMap<Long, ContactInfo>()
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                CONTACT_PROJECTION,
                null,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor -> readContacts(cursor, all) }
            all.values.filter { matchesAllWordStarts(it.displayName, q) }.take(MAX_RESULTS).forEach { results[it.id] = it }

            // The filter URI compares normalized numbers: "0151 234…", "0151-234…" and "+49151234…" alike.
            val digits = q.filter { it.isDigit() }
            if (digits.length >= 3 && q.none { it.isLetter() }) {
                context.contentResolver.query(
                    Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(q)),
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                    ),
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                    val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                    val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                    while (cursor.moveToNext() && results.size < MAX_RESULTS) {
                        val id = cursor.getLong(idIndex)
                        if (id !in results) {
                            results[id] = ContactInfo(id, cursor.getString(lookupIndex), cursor.getString(nameIndex) ?: "", cursor.getString(photoIndex))
                        }
                    }
                }
            }

            // E-mail addresses: a part of the address (split at ".", "@", "-", ...) starting with the query.
            if (q.length >= 2) {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Email.LOOKUP_KEY,
                        ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY,
                        ContactsContract.CommonDataKinds.Email.PHOTO_URI,
                        ContactsContract.CommonDataKinds.Email.ADDRESS
                    ),
                    "${ContactsContract.CommonDataKinds.Email.ADDRESS} LIKE ?",
                    arrayOf("%$q%"),
                    "${ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY} ASC"
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                    val lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.LOOKUP_KEY)
                    val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY)
                    val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.PHOTO_URI)
                    val addressIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    while (cursor.moveToNext() && results.size < MAX_RESULTS) {
                        val address = cursor.getString(addressIndex) ?: continue
                        if (!matchesWordStart(address, q)) continue
                        val id = cursor.getLong(idIndex)
                        if (id !in results) {
                            results[id] = ContactInfo(
                                id, cursor.getString(lookupIndex), cursor.getString(nameIndex) ?: "", cursor.getString(photoIndex),
                                email = address, matchedByEmail = true
                            )
                        }
                    }
                }
            }

            attachPhoneNumbers(results)
            attachEmails(results)
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

    /** Fills in each result's first e-mail address (for the "Send e-mail" action), keeping one the
     *  search already matched. */
    private fun attachEmails(results: MutableMap<Long, ContactInfo>) {
        val missing = results.values.filter { it.email == null }.map { it.id }
        if (missing.isEmpty()) return
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.CONTACT_ID, ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} IN (${missing.joinToString(",") { "?" }})",
            missing.map { it.toString() }.toTypedArray(),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val addressIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val contact = results[id] ?: continue
                if (contact.email == null) results[id] = contact.copy(email = cursor.getString(addressIndex))
            }
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
        const val MAX_RESULTS = 20
        val CONTACT_PROJECTION = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI
        )
    }
}
