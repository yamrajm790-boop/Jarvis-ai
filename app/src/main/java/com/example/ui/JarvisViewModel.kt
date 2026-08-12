package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.JarvisApplication
import com.example.ai.BackendStatus
import com.example.ai.JarvisClient
import com.example.data.ConversationEntity
import com.example.data.CustomCommandEntity
import com.example.data.MemoryEntity
import com.example.data.Preferences
import com.example.tools.ToolExecutionResult
import com.example.voice.SpeechRecognizerManager
import com.example.voice.SpeechState
import com.example.voice.TTSManager
import com.example.voice.WakeWordManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AssistantState {
    object Idle : AssistantState()
    object Listening : AssistantState()
    object Thinking : AssistantState()
    data class Speaking(val text: String) : AssistantState()
    data class ExecutingTool(val toolName: String) : AssistantState()
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JarvisApplication
    val repository = app.database.jarvisDao().let { com.example.data.JarvisRepository(it) }
    val preferences = Preferences(application)

    val jarvisClient = JarvisClient(application, repository, viewModelScope)
    val speechManager = SpeechRecognizerManager(application)
    val ttsManager = TTSManager(application)
    val wakeWordManager = WakeWordManager(application)

    private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val assistantState: StateFlow<AssistantState> = _assistantState

    private val _latestResponse = MutableStateFlow("JARVIS online. Standing by for voice input, sir.")
    val latestResponse: StateFlow<String> = _latestResponse

    private val _lastToolExecuted = MutableStateFlow<ToolExecutionResult?>(null)
    val lastToolExecuted: StateFlow<ToolExecutionResult?> = _lastToolExecuted

    val conversations: StateFlow<List<ConversationEntity>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCommands: StateFlow<List<CustomCommandEntity>> = repository.allCustomCommands
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val speechState: StateFlow<SpeechState> = speechManager.speechState
    val rmsBuffer: StateFlow<Float> = speechManager.rmsBuffer
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val backendStatus: StateFlow<BackendStatus> = jarvisClient.backendStatus

    init {
        viewModelScope.launch {
            speechManager.speechState.collect { state ->
                when (state) {
                    is SpeechState.Listening -> _assistantState.value = AssistantState.Listening
                    is SpeechState.Processing -> _assistantState.value = AssistantState.Thinking
                    is SpeechState.Error -> {
                        _assistantState.value = AssistantState.Idle
                    }
                    is SpeechState.Success -> {
                        processUserVoiceText(state.text)
                    }
                    else -> {}
                }
            }
        }
    }

    fun startVoiceInput() {
        ttsManager.stop()
        speechManager.startListening { text ->
            processUserVoiceText(text)
        }
    }

    fun stopVoiceInput() {
        speechManager.stopListening()
    }

    fun processUserVoiceText(userText: String) {
        viewModelScope.launch {
            _assistantState.value = AssistantState.Thinking

            val (speakResponse, toolResult) = jarvisClient.processCommand(userText, conversations.value)

            _latestResponse.value = speakResponse
            _lastToolExecuted.value = toolResult

            if (toolResult != null) {
                _assistantState.value = AssistantState.ExecutingTool(toolResult.resultMessage)
            } else {
                _assistantState.value = AssistantState.Speaking(speakResponse)
            }

            ttsManager.speak(speakResponse) {
                _assistantState.value = AssistantState.Idle
            }
        }
    }

    fun submitTextCommand(commandText: String) {
        if (commandText.isBlank()) return
        processUserVoiceText(commandText)
    }

    fun addCustomCommand(trigger: String, description: String, toolName: String, packageOrArg: String) {
        viewModelScope.launch {
            val actionsJson = """[{"tool":"$toolName","arguments":{"package":"$packageOrArg"}}]"""
            repository.saveCustomCommand(trigger, description, actionsJson)
        }
    }

    fun deleteCustomCommand(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomCommand(id)
        }
    }

    fun addMemory(key: String, value: String) {
        viewModelScope.launch {
            repository.saveMemory(key, value)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun clearAllMemoryAndHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            repository.clearAllMemory()
        }
    }

    fun retryBackendConnection() {
        jarvisClient.checkBackendHealth()
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
    }
}
