package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.example.accessibility.JarvisAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class WhatsAppTools(
    private val context: Context
) {

    private val appContext =
        context.applicationContext

    fun isWhatsAppInstalled(): Boolean {

        val pm =
            appContext.packageManager

        return try {

            pm.getPackageInfo(
                "com.whatsapp",
                0
            )

            true

        } catch (_: PackageManager.NameNotFoundException) {

            try {

                pm.getPackageInfo(
                    "com.whatsapp.w4b",
                    0
                )

                true

            } catch (_: PackageManager.NameNotFoundException) {

                false
            }
        }
    }

    suspend fun sendMessageToContactOrNumber(
        recipientQuery: String,
        messageText: String
    ): ToolExecutionResult =
        withContext(Dispatchers.Main) {

            if (!isWhatsAppInstalled()) {

                return@withContext ToolExecutionResult(
                    false,
                    "Sir, WhatsApp is not installed."
                )
            }

            if (recipientQuery.isBlank()) {

                return@withContext ToolExecutionResult(
                    false,
                    "I need a WhatsApp contact or phone number."
                )
            }

            if (messageText.isBlank()) {

                return@withContext ToolExecutionResult(
                    false,
                    "The message is empty."
                )
            }

            val resolution =
                ContactResolver.resolveContact(
                    appContext,
                    recipientQuery
                )

            when (resolution) {

                is ContactResolution.Multiple -> {

                    val names =
                        resolution.contacts
                            .take(5)
                            .joinToString(", ") {
                                it.name
                            }

                    ToolExecutionResult(
                        false,
                        "I found multiple contacts: $names. Please specify the contact."
                    )
                }

                is ContactResolution.Single -> {

                    automateWhatsApp(
                        resolution.contact.name,
                        resolution.contact.phoneNumber,
                        messageText
                    )
                }

                is ContactResolution.IsDirectNumber -> {

                    automateWhatsApp(
                        resolution.number,
                        resolution.number,
                        messageText
                    )
                }

                is ContactResolution.NotFound -> {

                    // Try WhatsApp UI search even if the
                    // contact is not present in Android Contacts.
                    automateWhatsApp(
                        recipientQuery,
                        null,
                        messageText
                    )
                }
            }
        }

    private suspend fun automateWhatsApp(
        displayName: String,
        phoneNumber: String?,
        messageText: String
    ): ToolExecutionResult {

        val service =
            JarvisAccessibilityService.instance

        if (service == null) {

            return ToolExecutionResult(
                false,
                "Sir, please enable JARVIS Accessibility Service first."
            )
        }

        val packageName =
            resolveWhatsAppPackage()

        if (packageName == null) {

            return ToolExecutionResult(
                false,
                "WhatsApp could not be opened."
            )
        }

        // ---------------------------------------------------------
        // CASE 1: PHONE NUMBER
        // ---------------------------------------------------------

        if (
            phoneNumber != null &&
            phoneNumber.isNotBlank()
        ) {

            val normalized =
                normalizePhoneNumber(
                    phoneNumber
                )

            val uri =
                Uri.parse(
                    "https://wa.me/$normalized?text=${
                        Uri.encode(messageText)
                    }"
                )

            try {

                val intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        uri
                    ).apply {

                        setPackage(packageName)

                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                        )
                    }

                appContext.startActivity(
                    intent
                )

            } catch (_: Exception) {

                return ToolExecutionResult(
                    false,
                    "Could not open WhatsApp chat."
                )
            }

            // Wait for WhatsApp
            waitForWhatsApp(
                packageName
            )

            // wa.me normally puts message into the
            // input field. We still verify and send it.
            for (attempt in 0 until 8) {

                delay(350L)

                if (
                    service.clickWhatsAppSend()
                ) {

                    return ToolExecutionResult(
                        true,
                        "Message sent to $displayName.",
                        mapOf(
                            "recipient" to displayName,
                            "phone" to normalized,
                            "message" to messageText
                        )
                    )
                }
            }

            // If message wasn't prefilled, find input
            val input =
                service.findWhatsAppMessageInput()

            if (input != null) {

                service.setTextOnNode(
                    input,
                    messageText
                )

                delay(300L)

                if (
                    service.clickWhatsAppSend()
                ) {

                    return ToolExecutionResult(
                        true,
                        "Message sent to $displayName.",
                        mapOf(
                            "recipient" to displayName,
                            "phone" to normalized,
                            "message" to messageText
                        )
                    )
                }
            }

            return ToolExecutionResult(
                false,
                "WhatsApp opened, but the Send button could not be activated."
            )
        }

        // ---------------------------------------------------------
        // CASE 2: CONTACT NAME
        // ---------------------------------------------------------

        try {

            val launchIntent =
                appContext.packageManager
                    .getLaunchIntentForPackage(
                        packageName
                    )

            if (launchIntent == null) {

                return ToolExecutionResult(
                    false,
                    "Could not launch WhatsApp."
                )
            }

            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )

            appContext.startActivity(
                launchIntent
            )

        } catch (_: Exception) {

            return ToolExecutionResult(
                false,
                "Could not launch WhatsApp."
            )
        }

        waitForWhatsApp(
            packageName
        )

        // Open WhatsApp Search
        var searchOpened = false

        for (attempt in 0 until 6) {

            delay(350L)

            if (
                service.findWhatsAppSearchButton()
            ) {

                searchOpened = true
                break
            }
        }

        if (!searchOpened) {

            return ToolExecutionResult(
                false,
                "Could not find WhatsApp Search."
            )
        }

        delay(450L)

        // Search contact
        val searchInput =
            service.findWhatsAppMessageInput()

        if (searchInput == null) {

            return ToolExecutionResult(
                false,
                "WhatsApp search field was not found."
            )
        }

        if (
            !service.setTextOnNode(
                searchInput,
                displayName
            )
        ) {

            return ToolExecutionResult(
                false,
                "Could not type the contact name."
            )
        }

        delay(900L)

        // Open matching chat
        var chatOpened = false

        for (attempt in 0 until 8) {

            if (
                service.findWhatsAppChat(
                    displayName
                )
            ) {

                chatOpened = true
                break
            }

            delay(350L)
        }

        if (!chatOpened) {

            return ToolExecutionResult(
                false,
                "Could not find WhatsApp chat for $displayName."
            )
        }

        delay(700L)

        // Find message field
        val messageInput =
            service.findWhatsAppMessageInput()

        if (messageInput == null) {

            return ToolExecutionResult(
                false,
                "WhatsApp message field was not found."
            )
        }

        if (
            !service.setTextOnNode(
                messageInput,
                messageText
            )
        ) {

            return ToolExecutionResult(
                false,
                "Could not type the WhatsApp message."
            )
        }

        delay(350L)

        // Send
        for (attempt in 0 until 6) {

            if (
                service.clickWhatsAppSend()
            ) {

                return ToolExecutionResult(
                    true,
                    "Message sent to $displayName.",
                    mapOf(
                        "recipient" to displayName,
                        "message" to messageText
                    )
                )
            }

            delay(300L)
        }

        return ToolExecutionResult(
            false,
            "Message was typed, but WhatsApp Send could not be activated."
        )
    }

    private suspend fun waitForWhatsApp(
        packageName: String
    ) {

        repeat(10) {

            delay(300L)

            val service =
                JarvisAccessibilityService.instance
                    ?: return@repeat

            val root =
                service.rootInActiveWindow

            if (
                root?.packageName
                    ?.toString() == packageName
            ) {
                return
            }
        }
    }

    private fun resolveWhatsAppPackage():
            String? {

        val pm =
            appContext.packageManager

        return try {

            pm.getPackageInfo(
                "com.whatsapp",
                0
            )

            "com.whatsapp"

        } catch (_: Exception) {

            try {

                pm.getPackageInfo(
                    "com.whatsapp.w4b",
                    0
                )

                "com.whatsapp.w4b"

            } catch (_: Exception) {

                null
            }
        }
    }

    private fun normalizePhoneNumber(
        number: String
    ): String {

        var digits =
            number.replace(
                Regex("[^0-9]"),
                ""
            )

        // India fallback
        if (digits.length == 10) {
            digits = "91$digits"
        }

        return digits
    }
}
