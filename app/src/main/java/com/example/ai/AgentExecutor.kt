package com.example.ai

import android.content.Context
import com.example.data.Preferences
import com.example.tools.ContactResolution
import com.example.tools.ContactResolver
import com.example.tools.ToolExecutionResult
import com.example.tools.ToolExecutor
import com.example.tools.WhatsAppTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AgentExecutor(
    private val context: Context,
    private val toolExecutor: ToolExecutor,
    private val whatsAppTools: WhatsAppTools,
    private val jarvisClient: JarvisClient
) {
    private val prefs = Preferences(context)
    private val _agentState = MutableStateFlow<AgentExecutionState>(AgentExecutionState.Idle)
    val agentState: StateFlow<AgentExecutionState> = _agentState

    private var activePlan: ActionPlan? = null
    private var currentStepIndex: Int = 0
    private var completedStepLogs = mutableListOf<String>()
    private var lastOutputText: String? = null

    suspend fun executePlan(plan: ActionPlan) {
        activePlan = plan
        currentStepIndex = 0
        completedStepLogs.clear()
        lastOutputText = null

        processSteps()
    }

    private suspend fun processSteps() = withContext(Dispatchers.IO) {
        val plan = activePlan ?: return@withContext

        while (currentStepIndex < plan.steps.size) {
            val step = plan.steps[currentStepIndex]
            val stepDesc = step.description

            _agentState.value = AgentExecutionState.ExecutingStep(
                plan = plan,
                currentStepIndex = currentStepIndex,
                stepDescription = stepDesc,
                completedSteps = completedStepLogs.toList()
            )

            // 1. Resolve arguments & dynamic inputs
            val resolvedArgs = step.arguments.toMutableMap()
            if (resolvedArgs["message"] == "USE_PREVIOUS_OUTPUT" || resolvedArgs["text_from_previous_step"] == true) {
                resolvedArgs["message"] = lastOutputText ?: "Here is the requested message."
            }

            // 2. Handle WhatsApp / Communication Contact Resolution & Ambiguity Check
            val recipient = resolvedArgs["recipient"]?.toString() ?: resolvedArgs["contact"]?.toString() ?: resolvedArgs["phone_number"]?.toString()
            if (step.tool in listOf("whatsapp_send_message", "make_call", "send_message", "find_contact") && !recipient.isNullOrBlank()) {
                val resolution = ContactResolver.resolveContact(context, recipient)
                when (resolution) {
                    is ContactResolution.Multiple -> {
                        _agentState.value = AgentExecutionState.DisambiguationRequired(
                            plan = plan,
                            currentStepIndex = currentStepIndex,
                            contactName = recipient,
                            matchingContacts = resolution.contacts.map { Pair(it.name, it.phoneNumber) }
                        )
                        return@withContext // Pause execution for user selection
                    }
                    is ContactResolution.NotFound -> {
                        if (step.tool != "whatsapp_send_message" && !whatsAppTools.isWhatsAppInstalled()) {
                            _agentState.value = AgentExecutionState.Failed("Contact '$recipient' was not found.", stepDesc)
                            return@withContext
                        }
                    }
                    is ContactResolution.Single -> {
                        resolvedArgs["recipient"] = resolution.contact.name
                        resolvedArgs["phone_number"] = resolution.contact.phoneNumber
                    }
                    is ContactResolution.IsDirectNumber -> {
                        resolvedArgs["recipient"] = resolution.number
                        resolvedArgs["phone_number"] = resolution.number
                    }
                }
            }

            // 3. Confirmation Check
            if (step.requiresConfirmation && prefs.isConfirmationModeEnabled && !prefs.isAutoExecuteEnabled) {
                val promptMsg = "Sir, ${resolvedArgs["recipient"] ?: "target"} ko message/call execution execute kar du?"
                _agentState.value = AgentExecutionState.AwaitingConfirmation(
                    plan = plan,
                    currentStepIndex = currentStepIndex,
                    pendingStep = step.copy(arguments = resolvedArgs),
                    confirmationPrompt = promptMsg
                )
                return@withContext // Pause execution for user confirmation
            }

            // 4. Step Execution
            val result = executeSingleTool(step.tool, resolvedArgs)

            // NO FAKE SUCCESS: Halt if step failed
            if (!result.success) {
                _agentState.value = AgentExecutionState.Failed(
                    reason = result.resultMessage,
                    failedStep = stepDesc
                )
                return@withContext
            }

            // Record success and step output
            if (!result.data?.get("text")?.toString().isNullOrBlank()) {
                lastOutputText = result.data?.get("text")?.toString()
            } else {
                lastOutputText = result.resultMessage
            }

            completedStepLogs.add(stepDesc)
            currentStepIndex++
        }

        // All steps executed successfully
        val finalMessage = "Done sir, ${plan.summary}"
        _agentState.value = AgentExecutionState.Success(
            message = finalMessage,
            summary = plan.summary
        )
    }

    private suspend fun executeSingleTool(
        toolName: String,
        args: Map<String, Any?>
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        return@withContext when (toolName) {
            "whatsapp_send_message" -> {
                val recipient = args["recipient"]?.toString() ?: args["contact"]?.toString() ?: ""
                val msg = args["message"]?.toString() ?: ""
                whatsAppTools.sendMessageToContactOrNumber(recipient, msg)
            }
            "generate_text" -> {
                val purpose = args["purpose"]?.toString() ?: "write a short leave letter"
                val text = jarvisClient.processTextCommandDirect("Write the requested text clearly: $purpose")
                    ?: "Dear Sir, I will be unable to attend tomorrow due to personal work. Regards."
                ToolExecutionResult(true, "Generated message text.", mapOf("text" to text))
            }
            "youtube_search" -> {
                val query = args["query"]?.toString() ?: ""
                toolExecutor.executeTool("search_web", mapOf("query" to "https://www.youtube.com/results?search_query=$query"))
            }
            "maps_search" -> {
                val query = args["query"]?.toString() ?: ""
                toolExecutor.executeTool("open_url", mapOf("url" to "https://www.google.com/maps/search/$query"))
            }
            else -> {
                toolExecutor.executeTool(toolName, args)
            }
        }
    }

    fun confirmPendingStep() {
        val currentState = _agentState.value
        if (currentState is AgentExecutionState.AwaitingConfirmation) {
            CoroutineScope(Dispatchers.IO).launch {
                val step = currentState.pendingStep
                val result = executeSingleTool(step.tool, step.arguments)
                if (!result.success) {
                    _agentState.value = AgentExecutionState.Failed(result.resultMessage, step.description)
                } else {
                    completedStepLogs.add(step.description)
                    currentStepIndex++
                    processSteps()
                }
            }
        }
    }

    fun selectDisambiguatedContact(contactName: String, phoneNumber: String) {
        val currentState = _agentState.value
        if (currentState is AgentExecutionState.DisambiguationRequired) {
            val plan = currentState.plan
            val step = plan.steps[currentStepIndex]
            val updatedArgs = step.arguments.toMutableMap().apply {
                put("recipient", contactName)
                put("phone_number", phoneNumber)
            }
            val updatedStep = step.copy(arguments = updatedArgs)

            CoroutineScope(Dispatchers.IO).launch {
                val result = executeSingleTool(updatedStep.tool, updatedArgs)
                if (!result.success) {
                    _agentState.value = AgentExecutionState.Failed(result.resultMessage, updatedStep.description)
                } else {
                    completedStepLogs.add(updatedStep.description)
                    currentStepIndex++
                    processSteps()
                }
            }
        }
    }

    fun cancelPendingStep() {
        _agentState.value = AgentExecutionState.Failed("Task cancelled by user.", "Cancelled")
    }

    fun resetState() {
        _agentState.value = AgentExecutionState.Idle
    }
}
