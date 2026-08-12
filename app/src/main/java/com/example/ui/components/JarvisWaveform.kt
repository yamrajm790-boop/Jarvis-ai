package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.AssistantState
import com.example.ui.theme.JarvisAccentGold
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisSuccessGreen
import kotlin.math.sin

@Composable
fun JarvisWaveform(
    assistantState: AssistantState,
    rmsLevel: Float = 0f,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    barCount: Int = 32
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformPhase")

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val targetColor = when (assistantState) {
        is AssistantState.Listening -> JarvisCyan
        is AssistantState.Thinking -> JarvisSuccessGreen
        is AssistantState.ExecutingTool -> JarvisAccentGold
        is AssistantState.Speaking -> Color(0xFF0072FF)
        is AssistantState.Idle -> JarvisCyan.copy(alpha = 0.5f)
    }

    val waveColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(500),
        label = "waveColor"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val maxHeight = size.height
        val centerY = maxHeight / 2f
        val barSpacing = width / (barCount * 1.5f)
        val barWidth = barSpacing * 0.6f
        val startX = (width - (barCount * barSpacing)) / 2f

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            // Envelope curve (higher in middle, tapers to ends)
            val envelope = sin(progress * Math.PI).toFloat()

            val baseAmplitude = when (assistantState) {
                is AssistantState.Listening -> {
                    val dynamicMic = (rmsLevel * 2.5f).coerceIn(0.2f, 1.2f)
                    val sinVal = sin((progress * Math.PI * 4) + wavePhase).toFloat()
                    (0.3f + 0.7f * Math.abs(sinVal)) * dynamicMic
                }
                is AssistantState.Speaking -> {
                    val sinVal = sin((progress * Math.PI * 6) + (wavePhase * 2)).toFloat()
                    0.4f + 0.6f * Math.abs(sinVal)
                }
                is AssistantState.Thinking -> {
                    val sinVal = sin((progress * Math.PI * 8) + (wavePhase * 3)).toFloat()
                    0.25f + 0.35f * Math.abs(sinVal)
                }
                is AssistantState.ExecutingTool -> {
                    val sinVal = sin((progress * Math.PI * 5) + wavePhase).toFloat()
                    0.3f + 0.5f * Math.abs(sinVal)
                }
                else -> 0.15f
            }

            val barHeight = (maxHeight * envelope * baseAmplitude).coerceAtLeast(3.dp.toPx())
            val x = startX + (i * barSpacing)
            val y = centerY - (barHeight / 2f)

            val alpha = (0.3f + envelope * 0.7f).coerceIn(0.2f, 1.0f)

            drawRoundRect(
                color = waveColor.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
