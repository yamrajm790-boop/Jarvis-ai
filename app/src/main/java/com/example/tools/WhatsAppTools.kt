package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.example.accessibility.JarvisAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WhatsAppTools(private val context: Context) {

    fun isWhatsAppInstalled(): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                pm.getPackageInfo("com.whatsapp.w4b", 0) // WhatsApp Business
                true
            } catch (ex: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun sendMessageToContactOrNumber(
        recipientQuery: String,
        messageText: String
    ): ToolExecutionResult {
        if (!isWhatsAppInstalled()) {
            return ToolExecutionResult(
                success = false,
                resultMessage = "Sir, WhatsApp is not installed on this device."
            )
        }

        val resolution = ContactResolver.resolveContact(context, recipientQuery)

        return when (resolution) {
            is ContactResolution.IsDirectNumber -> {
                sendDirectToNumber(resolution.number, messageText)
            }
            is ContactResolution.Single -> {
                sendDirectToNumber(resolution.contact.phoneNumber, messageText)
            }
            is ContactResolution.Multiple -> {
                val names = resolution.contacts.joinToString(", ") { "${it.name} (${it.phoneNumber})" }
                ToolExecutionResult(
                    success = false,
                    resultMessage = "Multiple contacts found for '$recipientQuery': $names. Please specify which contact to message.",
                    data = mapOf(
                        "ambiguity" to true,
                        "query" to recipientQuery,
                        "contacts" to resolution.contacts.map { mapOf("name" to it.name, "phone" to it.phoneNumber) }
                    )
                )
            }
            is ContactResolution.NotFound -> {
                // Attempt Accessibility-assisted search inside WhatsApp UI if service is running
                if (JarvisAccessibilityService.isServiceAvailable()) {
                    performAccessibilityWhatsAppSend(recipientQuery, messageText)
                } else {
                    ToolExecutionResult(
                        success = false,
                        resultMessage = "Contact '$recipientQuery' was not found in your phonebook."
                    )
                }
            }
        }
    }

    private fun sendDirectToNumber(phoneNumber: String, messageText: String): ToolExecutionResult {
        return try {
            // Standardize number (ensure digits only, adding default country code if missing 10-digit number)
            var cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
            if (cleanPhone.length == 10) {
                cleanPhone = "91$cleanPhone" // Default to India prefix +91 if 10-digit, or send directly
            }

            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageText)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            // Trigger accessibility auto-click send button after UI renders
            if (JarvisAccessibilityService.isServiceAvailable()) {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(1200)
                    JarvisAccessibilityService.instance?.findAndClickElement("Send")
                        ?: JarvisAccessibilityService.instance?.findAndClickElement("send")
                }
            }

            ToolExecutionResult(
                success = true,
                resultMessage = "WhatsApp opened with message to $phoneNumber, sir."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                resultMessage = "Could not open WhatsApp: ${e.message}"
            )
        }
    }

    private fun performAccessibilityWhatsAppSend(contactName: String, messageText: String): ToolExecutionResult {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.whatsapp")
                ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
                ?: return ToolExecutionResult(false, "WhatsApp is not installed.")

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            CoroutineScope(Dispatchers.IO).launch {
                val service = JarvisAccessibilityService.instance ?: return@launch
                delay(1500)

                // 1. Search contact
                service.findAndClickElement("Search")
                delay(800)
                service.typeTextIntoField("Search…", contactName)
                delay(1200)

                // 2. Open chat
                service.findAndClickElement(contactName)
                delay(1200)

                // 3. Type message
                service.typeTextIntoField("Type a message", messageText)
                delay(800)

                // 4. Click send
                service.findAndClickElement("Send")
            }

            ToolExecutionResult(
                success = true,
                resultMessage = "Initiated WhatsApp message delivery to $contactName."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                resultMessage = "Failed to send message via WhatsApp: ${e.message}"
            )
        }
    }
}
