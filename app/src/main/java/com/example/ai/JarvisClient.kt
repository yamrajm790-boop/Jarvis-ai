package com.example.ai

import android.content.Context
import com.example.data.ConversationEntity
import com.example.data.JarvisRepository
import com.example.data.Preferences
import com.example.tools.LocalCommandParser
import com.example.tools.ToolCallRequest
import com.example.tools.ToolExecutionResult
import com.example.tools.ToolExecutor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class BackendStatus {
    object Disconnected : BackendStatus()
    object Connecting : BackendStatus()
    object Connected : BackendStatus()
    data class Error(val message: String) : BackendStatus()
}

class JarvisClient(
    private val context: Context,
    private val repository: JarvisRepository,
    private val coroutineScope: CoroutineScope
) {
    private val preferences = Preferences(context)
    val toolExecutor = ToolExecutor(context)
    private val localCommandParser = LocalCommandParser(context, toolExecutor)

    private val _backendStatus = MutableStateFlow<BackendStatus>(BackendStatus.Disconnected)
    val backendStatus: StateFlow<BackendStatus> = _backendStatus

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    init {
        checkBackendHealth()
    }

    fun checkBackendHealth() {
        coroutineScope.launch(Dispatchers.IO) {
            _backendStatus.value = BackendStatus.Connecting
            try {
                val baseUrl = preferences.backendUrl.removeSuffix("/")
                val request = Request.Builder()
                    .url("$baseUrl/health")
                    .addHeader("X-Device-Token", preferences.deviceToken)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        _backendStatus.value = BackendStatus.Connected
                    } else {
                        _backendStatus.value = BackendStatus.Error("HTTP ${response.code}: Backend unreachable")
                    }
                }
            } catch (e: Exception) {
                _backendStatus.value = BackendStatus.Error("Failed to connect to backend: ${e.message}")
            }
        }
    }

    /**
     * Connect persistent WebSocket stream
     */
    fun connectWebSocket(onMessageReceived: (ToolCallRequest) -> Unit) {
        val baseUrl = preferences.backendUrl.removeSuffix("/")
        val wsUrl = if (baseUrl.startsWith("https://")) {
            baseUrl.replace("https://", "wss://") + "/ws?token=${preferences.deviceToken}"
        } else {
            baseUrl.replace("http://", "ws://") + "/ws?token=${preferences.deviceToken}"
        }

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("X-Device-Token", preferences.deviceToken)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _backendStatus.value = BackendStatus.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val jsonObj = JSONObject(text)
                    val type = jsonObj.optString("type", "response")
                    val tool = jsonObj.optString("tool", null)
                    val speakMessage = jsonObj.optString("speak_message", null)
                    val message = jsonObj.optString("message", null)

                    val argsMap = mutableMapOf<String, Any?>()
                    val argsObj = jsonObj.optJSONObject("arguments")
                    argsObj?.keys()?.forEach { key ->
                        argsMap[key] = argsObj.get(key)
                    }

                    val request = ToolCallRequest(
                        type = type,
                        tool = tool,
                        arguments = if (argsMap.isNotEmpty()) argsMap else null,
                        speak_message = speakMessage,
                        message = message
                    )
                    onMessageReceived(request)
                } catch (e: Exception) {
                    // Fallback JSON parse
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _backendStatus.value = BackendStatus.Error("WS Error: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _backendStatus.value = BackendStatus.Disconnected
            }
        })
    }

    suspend fun processCommand(
        userText: String,
        recentHistory: List<ConversationEntity> = emptyList()
    ): Pair<String, ToolExecutionResult?> = withContext(Dispatchers.IO) {
        val cleanInput = userText.trim()
        if (cleanInput.isEmpty()) {
            return@withContext Pair("I didn't catch that, sir.", null)
        }

        // 1. Custom Command system check
        val customCmds = repository.getActiveCustomCommands()
        val matchingCustom = customCmds.find { it.triggerPhrase.equals(cleanInput.lowercase(), ignoreCase = true) }
        if (matchingCustom != null) {
            val result = executeCustomCommand(matchingCustom.actionsJson)
            val speak = "Executing custom routine ${matchingCustom.triggerPhrase}, sir."
            repository.saveConversation(cleanInput, speak, toolCalled = "custom_command", isOfflineMode = true)
            return@withContext Pair(speak, result)
        }

        // 2. Offline Mode / Local Command Parser check
        if (preferences.isOfflineModeOnly) {
            val localResult = localCommandParser.parseAndExecute(cleanInput)
            if (localResult != null) {
                repository.saveConversation(cleanInput, localResult.resultMessage, isOfflineMode = true)
                return@withContext Pair(localResult.resultMessage, localResult)
            }
            val fallbackMsg = "Sir, offline mode is enabled and I couldn't match a local command."
            return@withContext Pair(fallbackMsg, null)
        }

        // Try local parse first for immediate response
        val localQuickResult = localCommandParser.parseAndExecute(cleanInput)
        if (localQuickResult != null) {
            repository.saveConversation(cleanInput, localQuickResult.resultMessage, isOfflineMode = true)
            return@withContext Pair(localQuickResult.resultMessage, localQuickResult)
        }

        // 3. Forward to Render Node.js + Groq Backend via REST
        try {
            val baseUrl = preferences.backendUrl.removeSuffix("/")
            val historyList = recentHistory.take(6).map {
                mapOf("role" to "user", "content" to it.userMessage) to mapOf("role" to "assistant", "content" to it.assistantResponse)
            }.flatMap { listOf(it.first, it.second) }

            val jsonBody = JSONObject().apply {
                put("message", cleanInput)
                put("history", historyList)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/api/chat")
                .addHeader("X-Device-Token", preferences.deviceToken)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errMsg = "Sorry sir, AI service is temporarily unavailable."
                    repository.saveConversation(cleanInput, errMsg)
                    return@withContext Pair(errMsg, null)
                }

                val responseStr = response.body?.string() ?: "{}"
                val jsonObj = JSONObject(responseStr)

                val type = jsonObj.optString("type", "response")
                val toolName = jsonObj.optString("tool", null)
                val speakMsg = jsonObj.optString("speak_message", null)
                val responseMsg = jsonObj.optString("message", "Command processed, sir.")

                if (type == "tool_call" && !toolName.isNullOrEmpty()) {
                    val argsMap = mutableMapOf<String, Any?>()
                    val argsObj = jsonObj.optJSONObject("arguments")
                    argsObj?.keys()?.forEach { key ->
                        argsMap[key] = argsObj.get(key)
                    }

                    // Execute tool safely on Android device
                    val toolResult = toolExecutor.executeTool(toolName, argsMap)
                    val finalSpeak = speakMsg ?: toolResult.resultMessage
                    repository.saveConversation(cleanInput, finalSpeak, toolCalled = toolName, toolArguments = argsMap.toString())
                    return@withContext Pair(finalSpeak, toolResult)
                } else {
                    repository.saveConversation(cleanInput, responseMsg)
                    return@withContext Pair(responseMsg, null)
                }
            }
        } catch (e: Exception) {
            val errMsg = "Connecting to Jarvis server..."
            repository.saveConversation(cleanInput, errMsg)
            return@withContext Pair(errMsg, null)
        }
    }

    private fun executeCustomCommand(actionsJson: String): ToolExecutionResult {
        var lastResult = ToolExecutionResult(true, "Routine complete.")
        try {
            val arr = org.json.JSONArray(actionsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val tool = obj.optString("tool")
                val argsObj = obj.optJSONObject("arguments")
                val argsMap = mutableMapOf<String, Any?>()
                argsObj?.keys()?.forEach { k -> argsMap[k] = argsObj.get(k) }
                lastResult = toolExecutor.executeTool(tool, argsMap)
            }
        } catch (e: Exception) {
            return ToolExecutionResult(false, "Failed custom routine: ${e.message}")
        }
        return lastResult
    }
}
