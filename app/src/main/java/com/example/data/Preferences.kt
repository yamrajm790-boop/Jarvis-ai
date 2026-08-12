package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

import com.example.ai.BackendConfig

class Preferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_user_preferences", Context.MODE_PRIVATE)

    var backendUrl: String
        get() = prefs.getString("backend_url", BackendConfig.baseUrl) ?: BackendConfig.baseUrl
        set(value) = prefs.edit().putString("backend_url", value.trim()).apply()

    var deviceToken: String
        get() = prefs.getString("device_token", BackendConfig.deviceToken) ?: BackendConfig.deviceToken
        set(value) = prefs.edit().putString("device_token", value.trim()).apply()

    var isWakeWordEnabled: Boolean
        get() = prefs.getBoolean("wake_word_enabled", true)
        set(value) = prefs.edit().putBoolean("wake_word_enabled", value).apply()

    var wakeWordPhrase: String
        get() = prefs.getString("wake_word_phrase", "Hey Jarvis") ?: "Hey Jarvis"
        set(value) = prefs.edit().putString("wake_word_phrase", value.trim()).apply()

    var isOfflineModeOnly: Boolean
        get() = prefs.getBoolean("offline_mode_only", false)
        set(value) = prefs.edit().putBoolean("offline_mode_only", value).apply()

    var isAutoStartEnabled: Boolean
        get() = prefs.getBoolean("auto_start_enabled", true)
        set(value) = prefs.edit().putBoolean("auto_start_enabled", value).apply()

    var isBackgroundAssistantEnabled: Boolean
        get() = prefs.getBoolean("background_assistant_enabled", true)
        set(value) = prefs.edit().putBoolean("background_assistant_enabled", value).apply()

    var isAutoExecuteEnabled: Boolean
        get() = prefs.getBoolean("auto_execute_enabled", false)
        set(value) = prefs.edit().putBoolean("auto_execute_enabled", value).apply()

    var isConfirmationModeEnabled: Boolean
        get() = prefs.getBoolean("confirmation_mode_enabled", true)
        set(value) = prefs.edit().putBoolean("confirmation_mode_enabled", value).apply()

    var assistantName: String
        get() = prefs.getString("assistant_name", "JARVIS") ?: "JARVIS"
        set(value) = prefs.edit().putString("assistant_name", value.trim()).apply()

    var userName: String
        get() = prefs.getString("user_name", "Sir") ?: "Sir"
        set(value) = prefs.edit().putString("user_name", value.trim()).apply()

    var ttsPitch: Float
        get() = prefs.getFloat("tts_pitch", 0.95f)
        set(value) = prefs.edit().putFloat("tts_pitch", value).apply()

    var ttsRate: Float
        get() = prefs.getFloat("tts_rate", 1.05f)
        set(value) = prefs.edit().putFloat("tts_rate", value).apply()
}
