package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TTSManager(
    private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val preferences = Preferences(context)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private var pendingCompletion: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            isInitialized = false
            return
        }

        val engine = tts ?: return

        val result = engine.setLanguage(Locale.US)

        if (
            result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            isInitialized = false
            return
        }

        isInitialized = true
        applyPreferences()

        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {

                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false

                    val callback = pendingCompletion
                    pendingCompletion = null

                    callback?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false

                    val callback = pendingCompletion
                    pendingCompletion = null

                    callback?.invoke()
                }
            }
        )
    }

    fun applyPreferences() {
        if (!isInitialized) return

        tts?.setPitch(preferences.ttsPitch)
        tts?.setSpeechRate(preferences.ttsRate)
    }

    @Synchronized
    fun speak(
        text: String,
        onComplete: (() -> Unit)? = null
    ) {
        if (text.isBlank()) {
            onComplete?.invoke()
            return
        }

        if (!isInitialized || tts == null) {
            onComplete?.invoke()
            return
        }

        applyPreferences()

        pendingCompletion = onComplete

        val utteranceId = "JARVIS_${System.currentTimeMillis()}"

        _isSpeaking.value = true

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )
    }

    fun stop() {
        pendingCompletion = null

        if (isInitialized) {
            tts?.stop()
        }

        _isSpeaking.value = false
    }

    fun shutdown() {
        pendingCompletion = null

        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }

        tts = null
        isInitialized = false
        _isSpeaking.value = false
    }
}
