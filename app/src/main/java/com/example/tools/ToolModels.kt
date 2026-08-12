package com.example.tools

data class ToolCallRequest(
    val type: String, // "tool_call" or "response"
    val tool: String? = null,
    val arguments: Map<String, Any?>? = null,
    val speak_message: String? = null,
    val message: String? = null
)

data class ToolExecutionResult(
    val success: Boolean,
    val resultMessage: String,
    val data: Map<String, Any?>? = null
)
