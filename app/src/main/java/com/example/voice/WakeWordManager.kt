package com.example.voice

import android.content.Context
import com.example.data.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lightweight Wake Word Manager.
 * Checks spoken phrase for the configured wake phrase (e.g. "Hey Jarvis" or "Jarvis").
 */
class WakeWordManager(private val context: Context) {

    private val preferences = Preferences(context)
    private val _isWakeWordListening = MutableStateFlow(false)
    val isWakeWordListening: StateFlow<Boolean> = _isWakeWordListening

    fun isWakeWordTriggered(spokenText: String): Boolean {
        if (!preferences.isWakeWordEnabled) return false

        val trigger = preferences.wakeWordPhrase.lowercase().trim()
        val text = spokenText.lowercase().trim()

        return text.contains(trigger) ||
                (trigger.contains("jarvis") && text.contains("jarvis")) ||
                (trigger.contains("hey jarvis") && (text.contains("hey jarvis") || text.contains("hi jarvis")))
    }

    fun stripWakeWord(spokenText: String): String {
        val trigger = preferences.wakeWordPhrase.lowercase().trim()
        var text = spokenText.lowercase().trim()

        text = text.replace(trigger, "")
            .replace("hey jarvis", "")
            .replace("hi jarvis", "")
            .replace("jarvis", "")
            .trim()

        return text.ifEmpty { spokenText }
    }

    fun setWakeWordActive(active: Boolean) {
        _isWakeWordListening.value = active
    }
}
