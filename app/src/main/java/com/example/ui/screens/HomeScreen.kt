package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.example.ai.AgentExecutionState
import com.example.ai.BackendStatus
import com.example.ui.AssistantState
import com.example.ui.JarvisViewModel
import com.example.ui.components.JarvisOrb
import com.example.ui.components.JarvisWaveform
import com.example.ui.theme.JarvisAccentGold
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkBackground
import com.example.ui.theme.JarvisSuccessGreen
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: JarvisViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateHistory: () -> Unit
) {
    val assistantState by viewModel.assistantState.collectAsState()
    val agentState by viewModel.agentState.collectAsState()
    val latestResponse by viewModel.latestResponse.collectAsState()
    val lastToolExecuted by viewModel.lastToolExecuted.collectAsState()
    val backendStatus by viewModel.backendStatus.collectAsState()
    val rmsLevel by viewModel.rmsBuffer.collectAsState()
    val conversations by viewModel.conversations.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val statusText = when (assistantState) {
        is AssistantState.Listening -> "Listening..."
        is AssistantState.Thinking -> "Processing..."
        is AssistantState.ExecutingTool -> "Performing Action..."
        is AssistantState.Speaking -> "Speaking..."
        else -> "Ready"
    }

    val statusColor = when (assistantState) {
        is AssistantState.Listening -> JarvisCyan
        is AssistantState.Thinking -> JarvisSuccessGreen
        is AssistantState.ExecutingTool -> JarvisAccentGold
        is AssistantState.Speaking -> Color(0xFF0072FF)
        else -> JarvisCyan.copy(alpha = 0.8f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisDarkBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top HUD Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Drawer",
                    tint = TextPrimary
                )
            }

            Text(
                text = "JÁRVIS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                letterSpacing = 2.sp
            )

            // Online Badge
            val (badgeColor, badgeText) = when (backendStatus) {
                is BackendStatus.Connected -> Pair(JarvisSuccessGreen, "ONLINE")
                is BackendStatus.Connecting -> Pair(Color.Yellow, "CONNECTING")
                else -> Pair(Color.Red, "OFFLINE")
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable { viewModel.retryBackendConnection() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(badgeColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Central Animated AI Orb
        JarvisOrb(
            assistantState = assistantState,
            rmsLevel = rmsLevel,
            size = 200.dp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // State Status Text
        Text(
            text = statusText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Animated Waveform
        JarvisWaveform(
            assistantState = assistantState,
            rmsLevel = rmsLevel,
            height = 32.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Response Quote Text
        Text(
            text = "\"$latestResponse\"",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mic Button
        val isListening = assistantState is AssistantState.Listening
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isListening) Color.Red else JarvisCyan,
                                if (isListening) Color(0xFF880000) else Color(0xFF0072FF)
                            )
                        )
                    )
                    .border(2.dp, Color.White, CircleShape)
                    .clickable {
                        if (isListening) viewModel.stopVoiceInput() else viewModel.startVoiceInput()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mic",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isListening) "Tap to stop" else "Tap to speak",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AGENT MULTI-STEP ACTION PLAN & STATUS CARD
        when (agentState) {
            is AgentExecutionState.ExecutingStep -> {
                val state = agentState as AgentExecutionState.ExecutingStep
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "AGENT ACTION PLAN • STEP ${state.currentStepIndex + 1}/${state.plan.steps.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        state.completedSteps.forEach { step ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = JarvisSuccessGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(step, fontSize = 12.sp, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.SmartButton, contentDescription = null, tint = JarvisAccentGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Current: ${state.stepDescription}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            is AgentExecutionState.AwaitingConfirmation -> {
                val state = agentState as AgentExecutionState.AwaitingConfirmation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("ACTION CONFIRMATION REQUIRED", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = JarvisAccentGold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.confirmationPrompt, fontSize = 13.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = { viewModel.cancelAgentStep() }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(onClick = { viewModel.confirmAgentStep() }, colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)) {
                                Text("Send / Proceed", color = JarvisDarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            is AgentExecutionState.DisambiguationRequired -> {
                val state = agentState as AgentExecutionState.DisambiguationRequired
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("SELECT RECIPIENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = JarvisCyan)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Multiple contacts found for '${state.contactName}'. Tap to select:", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        state.matchingContacts.forEach { (name, phone) ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectDisambiguatedContact(name, phone) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                                    Text(phone, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            else -> {}
        }

        // Action Result Card (when lastToolExecuted exists)
        if (lastToolExecuted != null) {
            val result = lastToolExecuted!!
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = JarvisSuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ACTION PERFORMED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisSuccessGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = result.resultMessage,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateHistory() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VIEW DETAILS",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Details",
                            tint = JarvisCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Text input fallback
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type command here...", fontSize = 12.sp, color = TextSecondary) },
            trailingIcon = {
                if (textInput.isNotBlank()) {
                    IconButton(onClick = {
                        viewModel.submitTextCommand(textInput)
                        textInput = ""
                        focusManager.clearFocus()
                    }) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = JarvisCyan)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                viewModel.submitTextCommand(textInput)
                textInput = ""
                focusManager.clearFocus()
            }),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                focusedContainerColor = JarvisSurfaceDark,
                unfocusedContainerColor = JarvisSurfaceDark
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Commands Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT COMMANDS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "See all",
                fontSize = 12.sp,
                color = JarvisCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateHistory() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Default / Past command list preview matching reference
        val sampleList = if (conversations.isNotEmpty()) conversations.take(4) else emptyList()

        if (sampleList.isEmpty()) {
            val defaults = listOf(
                Pair("Open YouTube", "9:30 PM"),
                Pair("Set alarm for 7 AM", "9:25 PM"),
                Pair("What's the weather?", "9:20 PM"),
                Pair("Play Arijit Singh songs", "9:15 PM")
            )
            defaults.forEach { (cmd, time) ->
                RecentCommandRow(title = cmd, timestamp = time, onClick = { viewModel.processUserVoiceText(cmd) })
                Spacer(modifier = Modifier.height(6.dp))
            }
        } else {
            sampleList.forEach { conv ->
                RecentCommandRow(title = conv.userMessage, timestamp = "Recent", onClick = { viewModel.processUserVoiceText(conv.userMessage) })
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun RecentCommandRow(title: String, timestamp: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(JarvisCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = timestamp,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }
    }
}
