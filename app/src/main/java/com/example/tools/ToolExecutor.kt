package com.example.tools

import android.content.Context
import com.example.accessibility.JarvisAccessibilityService
import com.example.data.Preferences
import com.example.service.JarvisNotificationListenerService

class ToolExecutor(private val context: Context) {
    private val appTools = AppTools(context)
    private val mediaTools = MediaTools(context)
    private val deviceTools = DeviceTools(context)
    private val communicationTools = CommunicationTools(context)
    private val prefs = Preferences(context)

    fun executeTool(toolName: String, args: Map<String, Any?>?): ToolExecutionResult {
        val safeArgs = args ?: emptyMap()

        return when (toolName) {
            "open_app" -> {
                val pkg = safeArgs["package"]?.toString() ?: safeArgs["app"]?.toString() ?: ""
                appTools.openApp(pkg)
            }
            "open_url" -> {
                val url = safeArgs["url"]?.toString() ?: ""
                appTools.openUrl(url)
            }
            "search_web" -> {
                val query = safeArgs["query"]?.toString() ?: ""
                appTools.searchWeb(query)
            }
            "set_volume" -> {
                val level = (safeArgs["level"] as? Number)?.toInt() ?: 50
                mediaTools.setVolume(level)
            }
            "increase_volume", "volume_up" -> {
                val step = (safeArgs["step"] as? Number)?.toInt() ?: 15
                mediaTools.increaseVolume(step)
            }
            "decrease_volume", "volume_down" -> {
                val step = (safeArgs["step"] as? Number)?.toInt() ?: 15
                mediaTools.decreaseVolume(step)
            }
            "mute" -> mediaTools.setVolume(0)
            "unmute" -> mediaTools.setVolume(50)
            "play_music", "resume_music" -> mediaTools.playMedia()
            "pause_music" -> mediaTools.pauseMedia()
            "next_track" -> mediaTools.nextTrack()
            "previous_track" -> mediaTools.previousTrack()
            "get_time" -> deviceTools.getTime()
            "get_date" -> deviceTools.getDate()
            "get_battery" -> deviceTools.getBatteryStatus()
            "get_device_info" -> deviceTools.getDeviceInfo()
            "set_alarm" -> {
                val hour = (safeArgs["hour"] as? Number)?.toInt() ?: 7
                val minute = (safeArgs["minute"] as? Number)?.toInt() ?: 0
                val label = safeArgs["label"]?.toString()
                deviceTools.setAlarm(hour, minute, label)
            }
            "set_timer" -> {
                val seconds = (safeArgs["seconds"] as? Number)?.toInt() ?: 60
                val label = safeArgs["label"]?.toString()
                deviceTools.setTimer(seconds, label)
            }
            "turn_flashlight_on", "flashlight_on", "torch_on" -> deviceTools.setTorch(true)
            "turn_flashlight_off", "flashlight_off", "torch_off" -> deviceTools.setTorch(false)
            "open_settings" -> deviceTools.openSettings()
            "open_wifi_settings" -> deviceTools.openWifiSettings()
            "open_bluetooth_settings" -> deviceTools.openBluetoothSettings()
            "open_display_settings" -> deviceTools.openDisplaySettings()
            "open_sound_settings" -> deviceTools.openSoundSettings()
            "open_camera" -> deviceTools.openCamera()
            "open_gallery" -> deviceTools.openGallery()

            "make_call" -> {
                val target = safeArgs["phone_number"]?.toString() ?: safeArgs["contact"]?.toString() ?: ""
                if (prefs.isConfirmationModeEnabled && !prefs.isAutoExecuteEnabled) {
                    ToolExecutionResult(false, "Call confirmation required for $target. Please confirm in settings or tap to call.")
                } else {
                    communicationTools.makeCall(target)
                }
            }
            "send_message" -> {
                val target = safeArgs["phone_number"]?.toString() ?: safeArgs["contact"]?.toString() ?: ""
                val msg = safeArgs["message"]?.toString() ?: ""
                if (prefs.isConfirmationModeEnabled && !prefs.isAutoExecuteEnabled) {
                    ToolExecutionResult(false, "Message confirmation required for $target. Draft ready: '$msg'")
                } else {
                    communicationTools.sendMessage(target, msg)
                }
            }

            // Accessibility UI Navigation & Automation Tools
            "go_home" -> {
                val service = JarvisAccessibilityService.instance
                service?.performGoHome() ?: appTools.openApp("com.android.launcher")
            }
            "go_back" -> {
                val service = JarvisAccessibilityService.instance
                service?.performGoBack() ?: ToolExecutionResult(false, "Accessibility Service is disabled, sir.")
            }
            "open_recent_apps" -> {
                val service = JarvisAccessibilityService.instance
                service?.performOpenRecents() ?: ToolExecutionResult(false, "Accessibility Service required to open recent apps.")
            }
            "take_screenshot" -> {
                val service = JarvisAccessibilityService.instance
                service?.performTakeScreenshot() ?: ToolExecutionResult(false, "Accessibility Service required for screenshots.")
            }
            "scroll", "scroll_down" -> {
                val service = JarvisAccessibilityService.instance
                service?.performScrollDown() ?: ToolExecutionResult(false, "Accessibility Service required for scrolling.")
            }
            "scroll_up" -> {
                val service = JarvisAccessibilityService.instance
                service?.performScrollUp() ?: ToolExecutionResult(false, "Accessibility Service required for scrolling.")
            }
            "click_known_element", "click_element" -> {
                val query = safeArgs["text"]?.toString() ?: safeArgs["element"]?.toString() ?: ""
                val service = JarvisAccessibilityService.instance
                service?.findAndClickElement(query) ?: ToolExecutionResult(false, "Accessibility Service required to click element.")
            }
            "long_click_known_element" -> {
                val query = safeArgs["text"]?.toString() ?: safeArgs["element"]?.toString() ?: ""
                val service = JarvisAccessibilityService.instance
                service?.findAndLongClickElement(query) ?: ToolExecutionResult(false, "Accessibility Service required to long click element.")
            }
            "type_text_into_supported_field", "type_text" -> {
                val field = safeArgs["field"]?.toString() ?: safeArgs["element"]?.toString() ?: ""
                val text = safeArgs["text"]?.toString() ?: ""
                val service = JarvisAccessibilityService.instance
                service?.typeTextIntoField(field, text) ?: ToolExecutionResult(false, "Accessibility Service required to type text.")
            }
            "read_visible_screen", "read_visible_ui_text" -> {
                val service = JarvisAccessibilityService.instance
                service?.readVisibleScreenText() ?: ToolExecutionResult(false, "Accessibility Service required to read screen text.")
            }

            // Notification Access Tool
            "read_notifications" -> {
                val service = JarvisNotificationListenerService.instance
                if (service != null) {
                    val summary = service.getNotificationSummary()
                    if (summary.totalCount == 0) {
                        ToolExecutionResult(true, "You have no new notifications, sir.")
                    } else {
                        val details = summary.recentTitles.joinToString("; ")
                        ToolExecutionResult(true, "Sir, you have ${summary.totalCount} active notifications. Details: $details")
                    }
                } else {
                    ToolExecutionResult(false, "Notification Access Service is not enabled, sir.")
                }
            }

            else -> ToolExecutionResult(false, "Unknown tool: $toolName")
        }
    }
}
