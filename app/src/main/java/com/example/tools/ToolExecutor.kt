package com.example.tools

import android.content.Context
import com.example.accessibility.JarvisAccessibilityService

class ToolExecutor(private val context: Context) {
    private val appTools = AppTools(context)
    private val mediaTools = MediaTools(context)
    private val deviceTools = DeviceTools(context)
    private val communicationTools = CommunicationTools(context)

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
            "increase_volume" -> {
                val step = (safeArgs["step"] as? Number)?.toInt() ?: 15
                mediaTools.increaseVolume(step)
            }
            "decrease_volume" -> {
                val step = (safeArgs["step"] as? Number)?.toInt() ?: 15
                mediaTools.decreaseVolume(step)
            }
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
            "open_settings" -> deviceTools.openSettings()
            "open_wifi_settings" -> deviceTools.openWifiSettings()
            "open_bluetooth_settings" -> deviceTools.openBluetoothSettings()
            "make_call" -> {
                val target = safeArgs["phone_number"]?.toString() ?: safeArgs["contact"]?.toString() ?: ""
                communicationTools.makeCall(target)
            }
            "send_message" -> {
                val target = safeArgs["phone_number"]?.toString() ?: ""
                val msg = safeArgs["message"]?.toString() ?: ""
                communicationTools.sendMessage(target, msg)
            }
            "go_home" -> {
                val service = JarvisAccessibilityService.instance
                if (service != null) {
                    service.performGoHome()
                } else {
                    appTools.openApp("com.android.launcher")
                }
            }
            "go_back" -> {
                val service = JarvisAccessibilityService.instance
                service?.performGoBack() ?: ToolExecutionResult(false, "Accessibility service required for back gesture.")
            }
            else -> ToolExecutionResult(false, "Unknown tool: $toolName")
        }
    }
}
