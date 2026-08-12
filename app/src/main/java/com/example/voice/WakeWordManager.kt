package com.example.voice

import android.content.Context
import com.example.data.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class WakeWordManager(
    context: Context
) {

    private val preferences =
        Preferences(context)

    private val _isWakeWordListening =
        MutableStateFlow(false)

    val isWakeWordListening:
            StateFlow<Boolean> =
        _isWakeWordListening

    fun isWakeWordTriggered(
        spokenText: String
    ): Boolean {

        if (!preferences.isWakeWordEnabled) {
            return false
        }

        val text =
            normalize(spokenText)

        val configured =
            normalize(
                preferences.wakeWordPhrase
            )

        if (configured.isBlank()) {
            return text.contains("jarvis")
        }

        if (text.contains(configured)) {
            return true
        }

        // Common speech-recognition variations
        if (
            configured.contains("hey jarvis") &&
            (
                text.contains("hey jarvis") ||
                text.contains("hi jarvis") ||
                text.contains("okay jarvis") ||
                text.contains("ok jarvis")
            )
        ) {
            return true
        }

        if (
            configured.contains("jarvis") &&
            text.contains("jarvis")
        ) {
            return true
        }

        return false
    }

    fun stripWakeWord(
        spokenText: String
    ): String {

        var text =
            spokenText.trim()

        val words =
            listOf(
                preferences.wakeWordPhrase,
                "hey jarvis",
                "hi jarvis",
                "okay jarvis",
                "ok jarvis",
                "jarvis"
            )

        words.forEach { word ->

            if (word.isNotBlank()) {

                text =
                    text.replace(
                        Regex(
                            Regex.escape(word),
                            RegexOption.IGNORE_CASE
                        ),
                        ""
                    )
            }
        }

        return text
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    fun setWakeWordActive(
        active: Boolean
    ) {
        _isWakeWordListening.value =
            active
    }

    private fun normalize(
        value: String
    ): String {

        return value
            .lowercase(Locale.getDefault())
            .replace(
                Regex("[^a-z0-9 ]"),
                " "
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }
}
