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
    data class Success(val text: String) : SpeechState()
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

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
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
            _speechState.value = SpeechState.Success(text)
            onResultCallback?.invoke(text)
        } else {
            _speechState.value = SpeechState.Error("No command heard")
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
