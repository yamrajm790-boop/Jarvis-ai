package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceTools(private val context: Context) {

    fun getTime(): ToolExecutionResult {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val currentTime = sdf.format(Date())
        return ToolExecutionResult(true, "Sir, it's $currentTime.")
    }

    fun getDate(): ToolExecutionResult {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())
        return ToolExecutionResult(true, "Today is $currentDate, sir.")
    }

    fun getBatteryStatus(): ToolExecutionResult {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
        
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val stateText = if (isCharging) "and currently charging" else "remaining"
        return ToolExecutionResult(true, "Sir, your battery is at $batteryPct percent $stateText.")
    }

    fun getDeviceInfo(): ToolExecutionResult {
        val model = Build.MODEL
        val brand = Build.BRAND.capitalize(Locale.getDefault())
        val osVersion = Build.VERSION.RELEASE
        return ToolExecutionResult(
            true,
            "Device: $brand $model running Android $osVersion, sir."
        )
    }

    fun setAlarm(hour: Int, minute: Int, label: String? = null): ToolExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label ?: "JARVIS Alarm")
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            ToolExecutionResult(true, "Alarm set for $timeStr, sir.")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Could not set alarm: ${e.message}")
        }
    }

    fun setTimer(seconds: Int, label: String? = null): ToolExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label ?: "JARVIS Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "Timer set for $seconds seconds, sir.")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Could not set timer: ${e.message}")
        }
    }

    fun openSettings(): ToolExecutionResult {
        return openSettingsIntent(Settings.ACTION_SETTINGS, "Device Settings")
    }

    fun openWifiSettings(): ToolExecutionResult {
        return openSettingsIntent(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings")
    }

    fun openBluetoothSettings(): ToolExecutionResult {
        return openSettingsIntent(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings")
    }

    private fun openSettingsIntent(action: String, name: String): ToolExecutionResult {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "Opening $name, sir.")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Could not open $name.")
        }
    }
}
