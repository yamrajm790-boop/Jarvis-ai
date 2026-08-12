package com.example.tools

import android.content.Context
import android.provider.ContactsContract

data class ContactInfo(
    val name: String,
    val phoneNumber: String
)

sealed class ContactResolution {
    data class Single(val contact: ContactInfo) : ContactResolution()
    data class Multiple(val query: String, val contacts: List<ContactInfo>) : ContactResolution()
    data class IsDirectNumber(val number: String) : ContactResolution()
    data class NotFound(val query: String) : ContactResolution()
}

object ContactResolver {

    fun resolveContact(context: Context, query: String): ContactResolution {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return ContactResolution.NotFound(query)
        }

        // 1. Check if the query is a raw phone number (digits, optionally prefixed with '+')
        val cleanDigits = trimmedQuery.replace(Regex("[^0-9+]"), "")
        if (cleanDigits.length >= 7 && (cleanDigits.all { it.isDigit() || it == '+' })) {
            return ContactResolution.IsDirectNumber(cleanDigits)
        }

        // 2. Query Android ContactsContract
        return try {
            val resolver = context.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$trimmedQuery%")

            val cursor = resolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            val results = mutableListOf<ContactInfo>()
            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (c.moveToNext()) {
                    val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "" else ""
                    val number = if (numberIdx >= 0) c.getString(numberIdx) ?: "" else ""
                    val cleanNumber = number.replace(Regex("[^0-9+]"), "")
                    if (name.isNotBlank() && cleanNumber.isNotBlank()) {
                        results.add(ContactInfo(name, cleanNumber))
                    }
                }
            }

            // Deduplicate by normalized name and phone number
            val distinctResults = results.distinctBy { Pair(it.name.lowercase(), it.phoneNumber) }

            when {
                distinctResults.isEmpty() -> ContactResolution.NotFound(query)
                distinctResults.size == 1 -> ContactResolution.Single(distinctResults.first())
                else -> ContactResolution.Multiple(query, distinctResults)
            }
        } catch (e: Exception) {
            // Fallback if contact permission not granted or error occurs
            ContactResolution.NotFound(query)
        }
    }
}
