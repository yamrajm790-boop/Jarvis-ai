package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    object Processing : SpeechState()
    data class Error(val message: String) : SpeechState()
    // alternatives = all candidate transcriptions the recognizer returned, best first
    // (text == alternatives[0]). Callers that need to be lenient about mishearing
    // (e.g. wake-word matching) should check all of them, not just `text`.
    data class Success(val text: String, val alternatives: List<String> = listOf(text)) : SpeechState()
}

class SpeechRecognizerManager(private val context: Context) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState

    private val _rmsBuffer = MutableStateFlow(0f)
    val rmsBuffer: StateFlow<Float> = _rmsBuffer

    private var onResultCallback: ((String) -> Unit)? = null

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@SpeechRecognizerManager)
            }
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        onResultCallback = onResult
        if (speechRecognizer == null) {
            _speechState.value = SpeechState.Error("Speech recognition is unavailable on this device.")
            return
        }

        // Cancel any stale session before starting a fresh one - starting on top of an
        // already-active recognizer is a common cause of it silently mishearing/dropping audio.
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            // Ignored - nothing was running.
        }

        val languageTag = Locale.getDefault().toLanguageTag()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // BUG FIX: this must be a String (BCP-47 language tag), not a Locale object.
            // Passing the raw Locale silently failed to apply, so the recognizer fell back
            // to whatever the underlying service defaulted to instead of the device's
            // actual language - this was the main cause of poor recognition accuracy.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            // Ask for several candidate transcriptions instead of just one - lets callers
            // (e.g. wake-word matching) check alternates when the top guess is slightly off.
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Give people a realistic amount of time to speak instead of being cut off
            // after a very short pause - this was the other big cause of "half heard" input.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
            // Prefer the higher-accuracy online/cloud model over the (usually worse)
            // offline model when a connection is available.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        _speechState.value = SpeechState.Listening
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _speechState.value = SpeechState.Error("Failed to start microphone: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _speechState.value = SpeechState.Processing
        } catch (e: Exception) {
            _speechState.value = SpeechState.Idle
        }
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
            _speechState.value = SpeechState.Idle
        } catch (e: Exception) {
            _speechState.value = SpeechState.Idle
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignored
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _speechState.value = SpeechState.Listening
    }

    override fun onBeginningOfSpeech() {
        _speechState.value = SpeechState.Listening
    }

    override fun onRmsChanged(rmsdB: Float) {
        _rmsBuffer.value = rmsdB.coerceIn(0f, 10f)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _speechState.value = SpeechState.Processing
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
            else -> "Speech recognition error ($error)"
        }
        _speechState.value = SpeechState.Error(errorMessage)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            _speechState.value = SpeechState.Success(text, matches.toList())
            onResultCallback?.invoke(text)
        } else {
            _speechState.value = SpeechState.Error("No command heard")
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
