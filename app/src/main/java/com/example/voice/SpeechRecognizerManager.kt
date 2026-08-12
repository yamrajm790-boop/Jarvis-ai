package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    data class Error(
        val message: String
    ) : SpeechState()

    data class Success(
        val text: String
    ) : SpeechState()
}

class SpeechRecognizerManager(
    private val context: Context
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null

    private val handler = Handler(Looper.getMainLooper())

    private val _speechState =
        MutableStateFlow<SpeechState>(SpeechState.Idle)

    val speechState: StateFlow<SpeechState> = _speechState

    private val _rmsBuffer =
        MutableStateFlow(0f)

    val rmsBuffer: StateFlow<Float> = _rmsBuffer

    private var currentCallback: ((String) -> Unit)? = null

    private var isListening = false

    init {
        createRecognizer()
    }

    private fun createRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _speechState.value =
                SpeechState.Error(
                    "Speech recognition is unavailable on this device."
                )
            return
        }

        try {
            speechRecognizer =
                SpeechRecognizer
                    .createSpeechRecognizer(context.applicationContext)
                    .also {
                        it.setRecognitionListener(this)
                    }
        } catch (e: Exception) {
            speechRecognizer = null

            _speechState.value =
                SpeechState.Error(
                    "Unable to initialize speech recognition."
                )
        }
    }

    fun startListening(
        onResult: (String) -> Unit
    ) {
        handler.post {

            currentCallback = onResult

            val recognizer = speechRecognizer

            if (recognizer == null) {
                createRecognizer()
            }

            val activeRecognizer = speechRecognizer

            if (activeRecognizer == null) {
                _speechState.value =
                    SpeechState.Error(
                        "Speech recognition unavailable."
                    )
                return@post
            }

            if (isListening) {
                try {
                    activeRecognizer.cancel()
                } catch (_: Exception) {
                }

                isListening = false
            }

            val intent =
                Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                ).apply {

                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE,
                        Locale.getDefault()
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                        Locale.getDefault()
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_MAX_RESULTS,
                        3
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                        true
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_CALLING_PACKAGE,
                        context.packageName
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_PREFER_OFFLINE,
                        false
                    )
                }

            try {
                isListening = true
                _speechState.value = SpeechState.Listening

                activeRecognizer.startListening(intent)

            } catch (e: Exception) {

                isListening = false

                _speechState.value =
                    SpeechState.Error(
                        "Failed to start microphone."
                    )
            }
        }
    }

    fun stopListening() {
        handler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }

            isListening = false
            _speechState.value = SpeechState.Processing
        }
    }

    fun cancel() {
        handler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {
            }

            isListening = false
            currentCallback = null
            _speechState.value = SpeechState.Idle
        }
    }

    fun destroy() {
        handler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {
            }

            speechRecognizer = null
            currentCallback = null
            isListening = false

            _speechState.value = SpeechState.Idle
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        isListening = true
        _speechState.value = SpeechState.Listening
    }

    override fun onBeginningOfSpeech() {
        _speechState.value = SpeechState.Listening
    }

    override fun onRmsChanged(rmsdB: Float) {
        _rmsBuffer.value =
            rmsdB.coerceIn(0f, 10f)
    }

    override fun onBufferReceived(buffer: ByteArray?) {
    }

    override fun onEndOfSpeech() {
        isListening = false
        _speechState.value = SpeechState.Processing
    }

    override fun onError(error: Int) {

        isListening = false

        val message =
            when (error) {

                SpeechRecognizer.ERROR_AUDIO ->
                    "Audio recording error"

                SpeechRecognizer.ERROR_CLIENT ->
                    "Speech client error"

                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    "Microphone permission required"

                SpeechRecognizer.ERROR_NETWORK ->
                    "Network connection error"

                SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    "Network timeout"

                SpeechRecognizer.ERROR_NO_MATCH ->
                    "No speech recognized"

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    "Speech recognizer busy"

                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    "No speech detected"

                SpeechRecognizer.ERROR_SERVER ->
                    "Speech server error"

                else ->
                    "Speech recognition error"
            }

        _speechState.value =
            SpeechState.Error(message)
    }

    override fun onResults(results: Bundle?) {

        isListening = false

        val matches =
            results?.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION
            )

        val best =
            matches
                ?.firstOrNull()
                ?.trim()

        if (!best.isNullOrBlank()) {

            _speechState.value =
                SpeechState.Success(best)

            currentCallback?.invoke(best)

        } else {

            _speechState.value =
                SpeechState.Error(
                    "No command heard"
                )
        }
    }

    override fun onPartialResults(
        partialResults: Bundle?
    ) {
        // Deliberately ignored.
        // We only execute final recognition results.
    }

    override fun onEvent(
        eventType: Int,
        params: Bundle?
    ) {
    }
}
