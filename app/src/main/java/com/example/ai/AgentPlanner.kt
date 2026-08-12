package com.example.ai

import android.content.Context
import org.json.JSONObject

class AgentPlanner(
    private val context: Context,
    private val jarvisClient: JarvisClient
) {

    suspend fun createPlan(userPrompt: String): ActionPlan {
        val cleanInput = userPrompt.trim()

        // 1. Check local pattern heuristics first for instant multi-step plan creation
        val localPlan = tryLocalPatternPlan(cleanInput)
        if (localPlan != null) {
            return localPlan
        }

        // 2. Fallback to Groq AI planner via JarvisClient
        val groqPlan = tryGroqAiPlanner(cleanInput)
        if (groqPlan != null) {
            return groqPlan
        }

        // 3. Fallback single-step general response plan
        return ActionPlan(
            userIntent = "general_command",
            summary = "Execute command",
            steps = listOf(
                ActionStep(
                    id = 1,
                    description = "Process command with Jarvis",
                    tool = "general_ai_query",
                    arguments = mapOf("query" to cleanInput)
                )
            )
        )
    }

    private fun tryLocalPatternPlan(prompt: String): ActionPlan? {
        val lower = prompt.lowercase()

        // Pattern 1: ChatGPT explicit open + prompt + WhatsApp send
        if (lower.contains("chatgpt") && (lower.contains("whatsapp") || lower.contains("message"))) {
            val recipient = extractRecipient(prompt) ?: "Rahul"
            return ActionPlan(
                userIntent = "cross_app_chatgpt_whatsapp",
                summary = "Generate via ChatGPT & send via WhatsApp to $recipient",
                steps = listOf(
                    ActionStep(1, "Open ChatGPT", "open_app", mapOf("package" to "com.openai.chatgpt")),
                    ActionStep(2, "Type leave prompt in ChatGPT", "type_text", mapOf("field" to "Ask ChatGPT", "text" to "Write a professional short leave letter")),
                    ActionStep(3, "Read generated ChatGPT text", "read_visible_ui_text", emptyMap()),
                    ActionStep(4, "Send message via WhatsApp to $recipient", "whatsapp_send_message", mapOf("recipient" to recipient, "message" to "USE_PREVIOUS_OUTPUT"), requiresConfirmation = true)
                )
            )
        }

        // Pattern 2: Generate text (leave letter, message) then send to WhatsApp
        if ((lower.contains("leave letter") || lower.contains("message likh") || lower.contains("write")) && lower.contains("whatsapp") && lower.contains("bhej")) {
            val recipient = extractRecipient(prompt) ?: "Rahul"
            return ActionPlan(
                userIntent = "generate_and_send_whatsapp",
                summary = "Generate content and send via WhatsApp to $recipient",
                steps = listOf(
                    ActionStep(1, "Generate message content", "generate_text", mapOf("purpose" to prompt)),
                    ActionStep(2, "Send message via WhatsApp to $recipient", "whatsapp_send_message", mapOf("recipient" to recipient, "message" to "USE_PREVIOUS_OUTPUT"), requiresConfirmation = true)
                )
            )
        }

        // Pattern 3: Direct WhatsApp message to contact or phone number
        if (lower.contains("whatsapp") || lower.contains("message kar")) {
            val recipient = extractRecipient(prompt)
            val msg = extractMessageText(prompt)
            if (recipient != null && msg != null) {
                return ActionPlan(
                    userIntent = "send_whatsapp_message",
                    summary = "Send WhatsApp message to $recipient",
                    steps = listOf(
                        ActionStep(1, "Send WhatsApp message to $recipient", "whatsapp_send_message", mapOf("recipient" to recipient, "message" to msg), requiresConfirmation = true)
                    )
                )
            }
        }

        // Pattern 4: YouTube search
        if (lower.contains("youtube") && (lower.contains("search") || lower.contains("play"))) {
            val query = prompt.substringAfter("search", prompt.substringAfter("play", "")).trim()
            val cleanQuery = if (query.isNotBlank()) query else "music"
            return ActionPlan(
                userIntent = "youtube_search",
                summary = "Open YouTube and search $cleanQuery",
                steps = listOf(
                    ActionStep(1, "Open YouTube", "open_app", mapOf("package" to "com.google.android.youtube")),
                    ActionStep(2, "Search for $cleanQuery on YouTube", "youtube_search", mapOf("query" to cleanQuery))
                )
            )
        }

        // Pattern 5: Google Maps search
        if ((lower.contains("maps") || lower.contains("map")) && lower.contains("search")) {
            val query = prompt.substringAfter("for", prompt.substringAfter("search", "")).trim()
            val cleanQuery = if (query.isNotBlank()) query else "nearby"
            return ActionPlan(
                userIntent = "maps_search",
                summary = "Open Google Maps and search for $cleanQuery",
                steps = listOf(
                    ActionStep(1, "Open Google Maps", "open_app", mapOf("package" to "com.google.android.apps.maps")),
                    ActionStep(2, "Search for $cleanQuery on Google Maps", "maps_search", mapOf("query" to cleanQuery))
                )
            )
        }

        // Pattern 6: Phone call
        if (lower.startsWith("call ") || lower.contains("ko call kar")) {
            val target = prompt.replace(Regex("(?i)^(call|phone|ko call kar|dial)"), "").trim()
            return ActionPlan(
                userIntent = "make_call",
                summary = "Call $target",
                steps = listOf(
                    ActionStep(1, "Dial $target", "make_call", mapOf("contact" to target), requiresConfirmation = true)
                )
            )
        }

        // Pattern 7: Read notifications
        if (lower.contains("notification") || lower.contains("read my message")) {
            return ActionPlan(
                userIntent = "read_notifications",
                summary = "Read active notifications",
                steps = listOf(
                    ActionStep(1, "Read notifications", "read_notifications", emptyMap())
                )
            )
        }

        return null
    }

    private suspend fun tryGroqAiPlanner(prompt: String): ActionPlan? {
        return try {
            val systemPrompt = """
                You are JARVIS Agent Planner. Convert the user command into a JSON ActionPlan.
                Output ONLY valid JSON with keys:
                {
                  "userIntent": "...",
                  "summary": "...",
                  "steps": [
                    {
                      "id": 1,
                      "description": "...",
                      "tool": "whatsapp_send_message|open_app|generate_text|make_call|read_notifications|type_text|read_visible_ui_text",
                      "arguments": { ... }
                    }
                  ]
                }
            """.trimIndent()

            val responseText = jarvisClient.processTextCommandDirect(prompt, systemPrompt) ?: return null
            val jsonStart = responseText.indexOf("{")
            val jsonEnd = responseText.lastIndexOf("}")
            if (jsonStart < 0 || jsonEnd <= jsonStart) return null

            val jsonStr = responseText.substring(jsonStart, jsonEnd + 1)
            val jsonObj = JSONObject(jsonStr)

            val intent = jsonObj.optString("userIntent", "ai_plan")
            val summary = jsonObj.optString("summary", "Execute task")
            val stepsArr = jsonObj.optJSONArray("steps") ?: return null

            val stepsList = mutableListOf<ActionStep>()
            for (i in 0 until stepsArr.length()) {
                val stepObj = stepsArr.getJSONObject(i)
                val argsObj = stepObj.optJSONObject("arguments")
                val argsMap = mutableMapOf<String, Any?>()
                if (argsObj != null) {
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.get(key)
                    }
                }
                val toolName = stepObj.optString("tool", "general_ai_query")
                val requiresConfirmation = toolName in listOf("whatsapp_send_message", "make_call", "send_message", "send_sms")

                stepsList.add(
                    ActionStep(
                        id = stepObj.optInt("id", i + 1),
                        description = stepObj.optString("description", "Step ${i + 1}"),
                        tool = toolName,
                        arguments = argsMap,
                        requiresConfirmation = requiresConfirmation
                    )
                )
            }

            if (stepsList.isNotEmpty()) {
                ActionPlan(intent, summary, stepsList)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractRecipient(prompt: String): String? {
        // e.g., "Rahul ko", "WhatsApp pe Rahul", "9876543210 ko"
        val regexContact = Regex("(?i)(?:whatsapp pe|message|ko)?\\s*([a-zA-Z0-9_+]{3,15})\\s*(?:ko|par)?")
        val numberRegex = Regex("([0-9+]{8,15})")

        val numMatch = numberRegex.find(prompt)
        if (numMatch != null) return numMatch.groupValues[1]

        val match = regexContact.find(prompt)
        if (match != null) {
            val candidate = match.groupValues[1].trim()
            if (!candidate.equals("whatsapp", ignoreCase = true) &&
                !candidate.equals("message", ignoreCase = true) &&
                !candidate.equals("jarvis", ignoreCase = true)) {
                return candidate
            }
        }
        return null
    }

    private fun extractMessageText(prompt: String): String? {
        val lower = prompt.lowercase()
        if (lower.contains(" ki ")) {
            return prompt.substringAfter(" ki ").trim()
        }
        if (lower.contains("saying ")) {
            return prompt.substringAfter("saying ").trim()
        }
        if (lower.contains("that ")) {
            return prompt.substringAfter("that ").trim()
        }
        return null
    }
}
