package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.ai.AgentExecutionState
import com.example.ai.AgentExecutor
import com.example.ai.AgentPlanner
import com.example.ai.JarvisClient
import com.example.data.JarvisDatabase
import com.example.data.JarvisRepository
import com.example.data.Preferences
import com.example.tools.ToolExecutor
import com.example.tools.WhatsAppTools
import com.example.voice.SpeechRecognizerManager
import com.example.voice.SpeechState
import com.example.voice.TTSManager
import com.example.voice.WakeWordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BackgroundAssistantController(
    private val appContext: Context,
    private val scope: CoroutineScope
) {

    private enum class Mode {
        STOPPED,
        WAKE_WORD,
        COMMAND,
        BUSY,
        PAUSED
    }

    private val preferences =
        Preferences(appContext)

    private val repository =
        JarvisRepository(
            JarvisDatabase
                .getDatabase(appContext)
                .jarvisDao()
        )

    private val jarvisClient =
        JarvisClient(
            appContext,
            repository,
            scope
        )

    private val toolExecutor =
        ToolExecutor(appContext)

    private val whatsAppTools =
        WhatsAppTools(appContext)

    private val agentPlanner =
        AgentPlanner(
            appContext,
            jarvisClient
        )

    private val agentExecutor =
        AgentExecutor(
            appContext,
            toolExecutor,
            whatsAppTools,
            jarvisClient
        )

    private val speechManager =
        SpeechRecognizerManager(appContext)

    private val wakeWordManager =
        WakeWordManager(appContext)

    private val ttsManager =
        TTSManager(appContext)

    private val handler =
        Handler(Looper.getMainLooper())

    private var mode =
        Mode.STOPPED

    private var restartRunnable: Runnable? = null

    private var speechJob: Job? = null
    private var agentJob: Job? = null

    private var commandGeneration = 0L

    private val _statusText =
        MutableStateFlow("Starting JARVIS…")

    val statusText: StateFlow<String> =
        _statusText

    fun start() {

        if (
            mode != Mode.STOPPED &&
            mode != Mode.PAUSED
        ) {
            return
        }

        if (speechJob == null) {
            observeSpeech()
        }

        if (agentJob == null) {
            observeAgent()
        }

        commandGeneration++

        startWakeWordLoop()
    }

    fun pause() {

        mode = Mode.PAUSED

        cancelRestart()

        speechManager.cancel()

        ttsManager.stop()

        _statusText.value =
            "JARVIS paused"
    }

    fun resume() {

        if (mode != Mode.PAUSED) {
            return
        }

        commandGeneration++

        startWakeWordLoop()
    }

    fun destroy() {

        mode = Mode.STOPPED

        cancelRestart()

        speechJob?.cancel()
        speechJob = null

        agentJob?.cancel()
        agentJob = null

        speechManager.cancel()
        speechManager.destroy()

        ttsManager.shutdown()
    }

    // ------------------------------------------------------------------
    // WAKE WORD
    // ------------------------------------------------------------------

    private fun startWakeWordLoop() {

        if (mode == Mode.STOPPED) {
            return
        }

        if (!preferences.isBackgroundAssistantEnabled) {

            mode = Mode.PAUSED

            _statusText.value =
                "Background assistant disabled"

            return
        }

        if (!hasMicrophonePermission()) {

            mode = Mode.PAUSED

            _statusText.value =
                "Microphone permission required"

            return
        }

        mode = Mode.WAKE_WORD

        wakeWordManager.setWakeWordActive(true)

        _statusText.value =
            "Listening for \"${preferences.wakeWordPhrase}\""

        startRecognizer()
    }

    private fun startRecognizer() {

        if (
            mode == Mode.STOPPED ||
            mode == Mode.PAUSED
        ) {
            return
        }

        try {

            speechManager.startListening {
                // Results are handled through StateFlow.
            }

        } catch (_: Exception) {

            scheduleRestart()
        }
    }

    private fun startCommandListening() {

        if (!hasMicrophonePermission()) {

            startWakeWordLoop()

            return
        }

        mode = Mode.COMMAND

        _statusText.value =
            "Listening for your command…"

        speechManager.cancel()

        handler.postDelayed(
            {
                if (mode == Mode.COMMAND) {
                    speechManager.startListening { }
                }
            },
            250L
        )
    }

    private fun hasMicrophonePermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun scheduleRestart(
        delayMs: Long = 500L
    ) {

        if (
            mode == Mode.STOPPED ||
            mode == Mode.PAUSED
        ) {
            return
        }

        cancelRestart()

        val runnable =
            Runnable {

                restartRunnable = null

                if (
                    mode != Mode.STOPPED &&
                    mode != Mode.PAUSED
                ) {
                    startWakeWordLoop()
                }
            }

        restartRunnable = runnable

        handler.postDelayed(
            runnable,
            delayMs
        )
    }

    private fun cancelRestart() {

        restartRunnable?.let {
            handler.removeCallbacks(it)
        }

        restartRunnable = null
    }

    // ------------------------------------------------------------------
    // SPEECH
    // ------------------------------------------------------------------

    private fun observeSpeech() {

        speechJob =
            scope.launch {

                speechManager.speechState.collect { state ->

                    when (state) {

                        is SpeechState.Success -> {

                            handleSpeech(
                                state.text
                            )
                        }

                        is SpeechState.Error -> {

                            handleSpeechError(
                                state.message
                            )
                        }

                        else -> Unit
                    }
                }
            }
    }

    private fun handleSpeech(
        spokenText: String
    ) {

        val text =
            spokenText
                .trim()
                .replace(
                    Regex("\\s+"),
                    " "
                )

        if (text.isBlank()) {
            scheduleRestart(200L)
            return
        }

        when (mode) {

            Mode.WAKE_WORD -> {

                if (
                    wakeWordManager
                        .isWakeWordTriggered(text)
                ) {

                    val command =
                        extractCommandAfterWakeWord(
                            text
                        )

                    if (command.length >= 2) {

                        executeCommand(
                            command
                        )

                    } else {

                        acknowledgeUser()
                    }

                } else {

                    scheduleRestart(150L)
                }
            }

            Mode.COMMAND -> {

                if (text.length >= 2) {

                    executeCommand(text)

                } else {

                    speakAndResume(
                        "Sorry ${preferences.userName}, I didn't catch that."
                    )
                }
            }

            else -> Unit
        }
    }

    private fun handleSpeechError(
        message: String
    ) {

        when (mode) {

            Mode.WAKE_WORD -> {

                if (
                    message.contains(
                        "permission",
                        ignoreCase = true
                    )
                ) {

                    mode = Mode.PAUSED

                    _statusText.value =
                        "Microphone permission required"

                    return
                }

                scheduleRestart(
                    when {
                        message.contains(
                            "busy",
                            true
                        ) -> 800L

                        message.contains(
                            "network",
                            true
                        ) -> 1200L

                        else -> 350L
                    }
                )
            }

            Mode.COMMAND -> {

                speakAndResume(
                    "Sorry ${preferences.userName}, I didn't catch that."
                )
            }

            else -> Unit
        }
    }

    private fun extractCommandAfterWakeWord(
        spokenText: String
    ): String {

        val trigger =
            preferences
                .wakeWordPhrase
                .trim()

        var text =
            spokenText.trim()

        val patterns =
            listOf(
                trigger,
                "hey jarvis",
                "hi jarvis",
                "ok jarvis",
                "okay jarvis",
                "jarvis"
            )

        for (pattern in patterns) {

            if (pattern.isNotBlank()) {

                text =
                    text.replace(
                        Regex(
                            Regex.escape(pattern),
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

    // ------------------------------------------------------------------
    // COMMAND
    // ------------------------------------------------------------------

    private fun acknowledgeUser() {

        mode = Mode.BUSY

        speechManager.cancel()

        _statusText.value =
            "Waiting for command…"

        ttsManager.speak(
            "Yes, ${preferences.userName}?"
        ) {

            handler.post {

                if (
                    mode != Mode.STOPPED &&
                    mode != Mode.PAUSED
                ) {
                    startCommandListening()
                }
            }
        }
    }

    private fun executeCommand(
        commandText: String
    ) {

        mode = Mode.BUSY

        speechManager.cancel()

        val generation =
            ++commandGeneration

        _statusText.value =
            "Processing command…"

        scope.launch {

            try {

                val plan =
                    agentPlanner.createPlan(
                        commandText
                    )

                val isAgentTask =
                    plan.steps.size > 1 ||
                    plan.steps.any {
                        it.tool in MULTI_STEP_TOOLS
                    }

                if (isAgentTask) {

                    agentExecutor.executePlan(
                        plan
                    )

                } else {

                    val history =
                        repository
                            .allConversations
                            .firstOrNull()
                            ?: emptyList()

                    val response =
                        jarvisClient.processCommand(
                            commandText,
                            history
                        )

                    if (
                        generation == commandGeneration &&
                        mode != Mode.STOPPED &&
                        mode != Mode.PAUSED
                    ) {

                        speakAndResume(
                            response.first
                        )
                    }
                }

            } catch (e: Exception) {

                if (
                    generation == commandGeneration
                ) {

                    speakAndResume(
                        "Sorry ${preferences.userName}, I couldn't complete that command."
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // AGENT
    // ------------------------------------------------------------------

    private fun observeAgent() {

        agentJob =
            scope.launch {

                agentExecutor.agentState.collect { state ->

                    when (state) {

                        is AgentExecutionState.AwaitingConfirmation -> {

                            speakAndResume(
                                "${state.confirmationPrompt} Please open JARVIS to confirm."
                            )
                        }

                        is AgentExecutionState.DisambiguationRequired -> {

                            speakAndResume(
                                "Sir, I found multiple contacts named ${state.contactName}. Please open JARVIS and choose one."
                            )
                        }

                        is AgentExecutionState.Success -> {

                            repository.saveConversation(
                                "Background Agent",
                                state.message
                            )

                            speakAndResume(
                                state.message
                            )
                        }

                        is AgentExecutionState.Failed -> {

                            speakAndResume(
                                "Sir, ${state.reason}"
                            )
                        }

                        else -> Unit
                    }
                }
            }
    }

    // ------------------------------------------------------------------
    // TTS -> LISTEN AGAIN
    // ------------------------------------------------------------------

    private fun speakAndResume(
        text: String
    ) {

        if (
            mode == Mode.STOPPED ||
            mode == Mode.PAUSED
        ) {
            return
        }

        mode = Mode.BUSY

        speechManager.cancel()

        _statusText.value =
            "JARVIS speaking…"

        ttsManager.stop()

        ttsManager.speak(text) {

            handler.post {

                if (
                    mode != Mode.STOPPED &&
                    mode != Mode.PAUSED
                ) {

                    scheduleRestart(
                        350L
                    )
                }
            }
        }
    }

    companion object {

        private val MULTI_STEP_TOOLS =
            setOf(
                "whatsapp_send_message",
                "generate_text",
                "youtube_search",
                "maps_search",
                "make_call",
                "read_notifications"
            )
    }
}
