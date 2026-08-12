package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri

class CommunicationTools(private val context: Context) {

    fun makeCall(phoneNumberOrQuery: String): ToolExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumberOrQuery)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "Dialing $phoneNumberOrQuery, sir.")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Could not initiate dialer: ${e.message}")
        }
    }

    fun sendMessage(phoneNumber: String, messageText: String): ToolExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(phoneNumber)}")
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "Drafting message to $phoneNumber, sir.")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Could not open messaging app: ${e.message}")
        }
    }
}
