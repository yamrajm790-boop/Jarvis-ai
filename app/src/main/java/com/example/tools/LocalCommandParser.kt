package com.example.tools

import android.content.Context
import com.example.accessibility.JarvisAccessibilityService
import com.example.service.JarvisNotificationListenerService

/**
 * Fast offline pattern matching engine for common device controls.
 * Executes instantly on-device without network latency or Groq API calls.
 */
class LocalCommandParser(
    private val context: Context,
    private val toolExecutor: ToolExecutor
) {
    fun parseAndExecute(input: String): ToolExecutionResult? {
        val cmd = input.trim().lowercase()

        // 1. Volume commands
        if (cmd.contains("volume up") || cmd == "increase volume" || cmd == "louder") {
            return toolExecutor.executeTool("increase_volume", mapOf("step" to 15))
        }
        if (cmd.contains("volume down") || cmd == "decrease volume" || cmd == "quieter") {
            return toolExecutor.executeTool("decrease_volume", mapOf("step" to 15))
        }
        if (cmd.startsWith("set volume to ") || cmd.startsWith("volume ")) {
            val digits = cmd.filter { it.isDigit() }
            if (digits.isNotEmpty()) {
                val level = digits.toIntOrNull() ?: 50
                return toolExecutor.executeTool("set_volume", mapOf("level" to level))
            }
        }
        if (cmd == "mute" || cmd == "silence") {
            return toolExecutor.executeTool("mute", emptyMap())
        }
        if (cmd == "unmute") {
            return toolExecutor.executeTool("unmute", emptyMap())
        }

        // 2. Media control
        if (cmd == "play" || cmd == "play music" || cmd == "resume music" || cmd == "resume") {
            return toolExecutor.executeTool("play_music", emptyMap())
        }
        if (cmd == "pause" || cmd == "pause music" || cmd == "stop music" || cmd == "stop") {
            return toolExecutor.executeTool("pause_music", emptyMap())
        }
        if (cmd == "next" || cmd == "next song" || cmd == "next track") {
            return toolExecutor.executeTool("next_track", emptyMap())
        }
        if (cmd == "previous" || cmd == "previous song" || cmd == "previous track") {
            return toolExecutor.executeTool("previous_track", emptyMap())
        }

        // 3. Navigation / Accessibility UI Automation
        if (cmd == "go home" || cmd == "home" || cmd == "open home") {
            val service = JarvisAccessibilityService.instance
            return service?.performGoHome() ?: toolExecutor.executeTool("go_home", emptyMap())
        }
        if (cmd == "go back" || cmd == "back") {
            val service = JarvisAccessibilityService.instance
            return service?.performGoBack() ?: toolExecutor.executeTool("go_back", emptyMap())
        }
        if (cmd.contains("recent apps") || cmd == "recents" || cmd == "open recents") {
            return toolExecutor.executeTool("open_recent_apps", emptyMap())
        }
        if (cmd.contains("screenshot") || cmd == "take screenshot") {
            return toolExecutor.executeTool("take_screenshot", emptyMap())
        }
        if (cmd == "scroll down" || cmd == "page down") {
            return toolExecutor.executeTool("scroll_down", emptyMap())
        }
        if (cmd == "scroll up" || cmd == "page up") {
            return toolExecutor.executeTool("scroll_up", emptyMap())
        }
        if (cmd.contains("read screen") || cmd.contains("what is on my screen") || cmd.contains("read visible text")) {
            return toolExecutor.executeTool("read_visible_screen", emptyMap())
        }

        // 4. Notifications
        if (cmd.contains("notification") || cmd.contains("check notifications") || cmd == "read notifications") {
            return toolExecutor.executeTool("read_notifications", emptyMap())
        }

        // 5. Device info / Time / Battery
        if (cmd.contains("time") && (cmd.contains("what") || cmd.contains("get") || cmd == "time")) {
            return toolExecutor.executeTool("get_time", emptyMap())
        }
        if (cmd.contains("date") && (cmd.contains("what") || cmd.contains("get") || cmd == "date")) {
            return toolExecutor.executeTool("get_date", emptyMap())
        }
        if (cmd.contains("battery") || cmd.contains("charge")) {
            return toolExecutor.executeTool("get_battery", emptyMap())
        }
        if (cmd.contains("device info") || cmd.contains("phone info")) {
            return toolExecutor.executeTool("get_device_info", emptyMap())
        }

        // 6. Settings & Camera/Gallery intents
        if (cmd == "open settings" || cmd == "settings") {
            return toolExecutor.executeTool("open_settings", emptyMap())
        }
        if (cmd == "open wifi" || cmd == "wifi settings") {
            return toolExecutor.executeTool("open_wifi_settings", emptyMap())
        }
        if (cmd == "open bluetooth" || cmd == "bluetooth settings") {
            return toolExecutor.executeTool("open_bluetooth_settings", emptyMap())
        }
        if (cmd == "open display" || cmd == "display settings") {
            return toolExecutor.executeTool("open_display_settings", emptyMap())
        }
        if (cmd == "open sound" || cmd == "sound settings") {
            return toolExecutor.executeTool("open_sound_settings", emptyMap())
        }
        if (cmd == "open camera" || cmd == "take photo" || cmd == "camera") {
            return toolExecutor.executeTool("open_camera", emptyMap())
        }
        if (cmd == "open gallery" || cmd == "open photos" || cmd == "photos") {
            return toolExecutor.executeTool("open_gallery", emptyMap())
        }

        // 7. Direct app launcher short circuit
        if (cmd.startsWith("open ") || cmd.startsWith("launch ")) {
            val appName = cmd.removePrefix("open ").removePrefix("launch ").trim()
            if (appName.isNotEmpty() && !appName.contains(" ")) {
                return toolExecutor.executeTool("open_app", mapOf("package" to appName))
            }
        }

        // Not a local command -> proceed to Groq AI
        return null
    }
}
