package com.example.ai

data class ActionStep(
    val id: Int,
    val description: String,
    val tool: String,
    val arguments: Map<String, Any?> = emptyMap(),
    val requiresConfirmation: Boolean = false
)

data class ActionPlan(
    val userIntent: String,
    val summary: String,
    val steps: List<ActionStep>
)

sealed class AgentExecutionState {
    object Idle : AgentExecutionState()

    data class Planning(val userPrompt: String) : AgentExecutionState()

    data class ExecutingStep(
        val plan: ActionPlan,
        val currentStepIndex: Int,
        val stepDescription: String,
        val completedSteps: List<String>
    ) : AgentExecutionState()

    data class AwaitingConfirmation(
        val plan: ActionPlan,
        val currentStepIndex: Int,
        val pendingStep: ActionStep,
        val confirmationPrompt: String
    ) : AgentExecutionState()

    data class DisambiguationRequired(
        val plan: ActionPlan,
        val currentStepIndex: Int,
        val contactName: String,
        val matchingContacts: List<Pair<String, String>> // Name, Phone
    ) : AgentExecutionState()

    data class Success(
        val message: String,
        val summary: String
    ) : AgentExecutionState()

    data class Failed(
        val reason: String,
        val failedStep: String? = null
    ) : AgentExecutionState()
}
