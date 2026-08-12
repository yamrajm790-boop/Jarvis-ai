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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Runs JARVIS's real "always listening" pipeline independently of any Activity/ViewModel.
 *
 * This is what makes background listening real instead of a notification stub:
 * it owns its own SpeechRecognizer + wake-word check + TTS + command pipeline, and keeps
 * re-arming the microphone on a loop for as long as the foreground service is alive -
 * so JARVIS reacts to the wake word even when the app UI is closed.
 *
 * State machine:
 *  WAKE_WORD -> continuously restarts the recognizer. Only text containing the wake
 *               phrase is ever forwarded anywhere (privacy + battery: everything else
 *               is discarded locally and the mic is immediately re-armed).
 *  COMMAND   -> one listening pass for the actual instruction, right after the wake
 *               word was detected on its own (e.g. "Hey JARVIS" ... pause ... command).
 *  BUSY      -> a command is being processed / spoken back; mic is not active.
 *  PAUSED    -> mic released on purpose (user paused JARVIS, or the app's own mic UI
 *               is active and needs exclusive access).
 *  STOPPED   -> controller torn down.
 *
 * Note: this uses Android's on-device/cloud SpeechRecognizer in a loop, not a dedicated
 * offline wake-word engine (e.g. Porcupine), so it does consume some battery/network while
 * armed. That's a reasonable tradeoff for a from-scratch implementation with no extra SDKs.
 */
class BackgroundAssistantController(
    private val appContext: Context,
    private val scope: CoroutineScope
) {
    private enum class Mode { WAKE_WORD, COMMAND, BUSY, PAUSED, STOPPED }

    private val preferences = Preferences(appContext)
    private val repository = JarvisRepository(JarvisDatabase.getDatabase(appContext).jarvisDao())
    private val jarvisClient = JarvisClient(appContext, repository, scope)
    private val toolExecutor = ToolExecutor(appContext)
    private val whatsAppTools = WhatsAppTools(appContext)
    private val agentPlanner = AgentPlanner(appContext, jarvisClient)
    private val agentExecutor = AgentExecutor(appContext, toolExecutor, whatsAppTools, jarvisClient)

    private val speechManager = SpeechRecognizerManager(appContext)
    private val wakeWordManager = WakeWordManager(appContext)
    private val ttsManager = TTSManager(appContext)

    private val mainHandler = Handler(Looper.getMainLooper())
    private var mode = Mode.STOPPED
    private var pendingRestart: Runnable? = null
    private var consecutiveErrors = 0

    private val _statusText = MutableStateFlow("Starting…")
    val statusText: StateFlow<String> = _statusText

    private var speechCollectorJob: Job? = null
    private var agentCollectorJob: Job? = null

    fun start() {
        if (mode == Mode.WAKE_WORD || mode == Mode.COMMAND || mode == Mode.BUSY) return
        if (speechCollectorJob == null) observeSpeechState()
        if (agentCollectorJob == null) observeAgentState()
        consecutiveErrors = 0
        beginWakeWordListening()
    }

    /** Mic released on purpose - e.g. the app's own mic button is being used right now. */
    fun pause() {
        mode = Mode.PAUSED
        cancelPendingRestart()
        speechManager.cancel()
        ttsManager.stop()
        _statusText.value = "Paused"
    }

    fun resume() {
        if (mode != Mode.PAUSED) return
        consecutiveErrors = 0
        beginWakeWordListening()
    }

    fun destroy() {
        mode = Mode.STOPPED
        cancelPendingRestart()
        speechCollectorJob?.cancel()
        agentCollectorJob?.cancel()
        speechManager.cancel()
        speechManager.destroy()
        ttsManager.shutdown()
    }

    // ---------------------------------------------------------------------
    // Wake-word loop
    // ---------------------------------------------------------------------

    private fun beginWakeWordListening() {
        if (!preferences.isBackgroundAssistantEnabled) {
            mode = Mode.PAUSED
            _statusText.value = "Background assistant disabled"
            return
        }
        if (!hasMicPermission()) {
            mode = Mode.PAUSED
            _statusText.value = "Microphone permission required"
            return
        }
        mode = Mode.WAKE_WORD
        wakeWordManager.setWakeWordActive(true)
        _statusText.value = "Listening for \"${preferences.wakeWordPhrase}\""
        try {
            speechManager.startListening { /* handled via speechState collector below */ }
        } catch (e: Exception) {
            scheduleWakeWordRestart()
        }
    }

    private fun beginCommandListening() {
        if (!hasMicPermission()) {
            beginWakeWordListening()
            return
        }
        mode = Mode.COMMAND
        _statusText.value = "Listening for command…"
        try {
            speechManager.startListening { }
        } catch (e: Exception) {
            scheduleWakeWordRestart()
        }
    }

    private fun hasMicPermission(): Boolean = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    private fun scheduleWakeWordRestart(immediate: Boolean = false) {
        if (mode == Mode.PAUSED || mode == Mode.STOPPED) return
        cancelPendingRestart()
        consecutiveErrors = (consecutiveErrors + 1).coerceAtMost(8)
        val delay = if (immediate) 200L else (350L * consecutiveErrors).coerceAtMost(5000L)
        val runnable = Runnable {
            pendingRestart = null
            if (mode != Mode.PAUSED && mode != Mode.STOPPED) {
                beginWakeWordListening()
            }
        }
        pendingRestart = runnable
        mainHandler.postDelayed(runnable, delay)
    }

    private fun cancelPendingRestart() {
        pendingRestart?.let { mainHandler.removeCallbacks(it) }
        pendingRestart = null
    }

    // ---------------------------------------------------------------------
    // Speech recognition events
    // ---------------------------------------------------------------------

    private fun observeSpeechState() {
        speechCollectorJob = scope.launch {
            speechManager.speechState.collect { state ->
                when (state) {
                    is SpeechState.Success -> {
                        consecutiveErrors = 0
                        handleSpeechResult(state.text)
                    }
                    is SpeechState.Error -> handleSpeechError(state.message)
                    else -> {}
                }
            }
        }
    }

    private fun handleSpeechResult(text: String) {
        when (mode) {
            Mode.WAKE_WORD -> {
                if (wakeWordManager.isWakeWordTriggered(text)) {
                    val command = extractCommandAfterWakeWord(text)
                    if (command.length >= 3) {
                        runCommand(command)
                    } else {
                        acknowledgeAndListenForCommand()
                    }
                } else {
                    scheduleWakeWordRestart(immediate = true)
                }
            }
            Mode.COMMAND -> runCommand(text)
            else -> {}
        }
    }

    private fun handleSpeechError(message: String) {
        when (mode) {
            Mode.WAKE_WORD -> {
                if (message.contains("permission", ignoreCase = true)) {
                    mode = Mode.PAUSED
                    _statusText.value = message
                    return
                }
                // ERROR_NO_MATCH / ERROR_SPEECH_TIMEOUT / busy etc. - just keep listening.
                scheduleWakeWordRestart()
            }
            Mode.COMMAND -> {
                _statusText.value = "Didn't catch that"
                speakThenResumeWakeWord("Sorry ${preferences.userName}, I didn't catch that.")
            }
            else -> {}
        }
    }

    /** Mirrors WakeWordManager.stripWakeWord but returns "" when nothing but the wake word was said. */
    private fun extractCommandAfterWakeWord(spokenText: String): String {
        val trigger = preferences.wakeWordPhrase.lowercase().trim()
        var text = spokenText.lowercase().trim()
        text = text.replace(trigger, "")
            .replace("hey jarvis", "")
            .replace("hi jarvis", "")
            .replace("jarvis", "")
            .trim()
        return text
    }

    private fun acknowledgeAndListenForCommand() {
        mode = Mode.BUSY
        _statusText.value = "Yes, ${preferences.userName}?"
        mainHandler.post {
            ttsManager.speak("Yes, ${preferences.userName}?") {
                mainHandler.post { beginCommandListening() }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Command execution
    // ---------------------------------------------------------------------

    private fun runCommand(commandText: String) {
        mode = Mode.BUSY
        _statusText.value = "Processing: \"$commandText\""
        scope.launch {
            try {
                val plan = agentPlanner.createPlan(commandText)
                val isAgentTask = plan.steps.size > 1 || plan.steps.any { it.tool in MULTI_STEP_TOOLS }
                if (isAgentTask) {
                    // Response is delivered via observeAgentState() below.
                    agentExecutor.executePlan(plan)
                } else {
                    val recentHistory = repository.allConversations.firstOrNull() ?: emptyList()
                    val (speakResponse, _) = jarvisClient.processCommand(commandText, recentHistory)
                    speakThenResumeWakeWord(speakResponse)
                }
            } catch (e: Exception) {
                speakThenResumeWakeWord("Sorry ${preferences.userName}, something went wrong: ${e.message}")
            }
        }
    }

    private fun observeAgentState() {
        agentCollectorJob = scope.launch {
            agentExecutor.agentState.collect { state ->
                when (state) {
                    is AgentExecutionState.AwaitingConfirmation -> {
                        _statusText.value = "Awaiting confirmation - open app"
                        speakThenResumeWakeWord("${state.confirmationPrompt} Please open the app to confirm.")
                    }
                    is AgentExecutionState.DisambiguationRequired -> {
                        _statusText.value = "Multiple contacts found - open app"
                        speakThenResumeWakeWord(
                            "Sir, I found multiple contacts named ${state.contactName}. Please open the app to choose one."
                        )
                    }
                    is AgentExecutionState.Success -> {
                        repository.saveConversation("Background Agent Task", state.message)
                        speakThenResumeWakeWord(state.message)
                    }
                    is AgentExecutionState.Failed -> {
                        speakThenResumeWakeWord("Sir, ${state.reason}")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun speakThenResumeWakeWord(text: String) {
        if (mode == Mode.PAUSED || mode == Mode.STOPPED) return
        mode = Mode.BUSY
        _statusText.value = "Speaking…"
        mainHandler.post {
            ttsManager.speak(text) {
                mainHandler.post { scheduleWakeWordRestart(immediate = true) }
            }
        }
    }

    companion object {
        // Tools that need the multi-step AgentPlanner/AgentExecutor pipeline instead of
        // the single-shot JarvisClient.processCommand path - mirrors JarvisViewModel.
        private val MULTI_STEP_TOOLS = setOf(
            "whatsapp_send_message", "generate_text", "youtube_search",
            "maps_search", "make_call", "read_notifications"
        )
    }
}
