package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AssistantState
import com.example.ui.theme.JarvisAccentGold
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisDarkBackground
import com.example.ui.theme.JarvisSuccessGreen
import com.example.ui.theme.JarvisWarningRed

@Composable
fun OrbVisualizer(
    assistantState: AssistantState,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbRotation")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = when (assistantState) {
        is AssistantState.Listening -> JarvisCyan
        is AssistantState.Thinking -> JarvisAccentGold
        is AssistantState.Speaking -> JarvisSuccessGreen
        is AssistantState.ExecutingTool -> JarvisCyan
        else -> JarvisCyan.copy(alpha = 0.6f)
    }

    val statusText = when (assistantState) {
        is AssistantState.Listening -> "LISTENING"
        is AssistantState.Thinking -> "PROCESSING"
        is AssistantState.Speaking -> "SPEAKING"
        is AssistantState.ExecutingTool -> "EXECUTING"
        else -> "STANDBY"
    }

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glow Blur
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(pulseScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .blur(30.dp)
        )

        // Outer Rotating Ring Arc
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            rotate(rotationAngle) {
                drawArc(
                    color = primaryColor.copy(alpha = 0.4f),
                    startAngle = 0f,
                    sweepAngle = 100f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = primaryColor.copy(alpha = 0.3f),
                    startAngle = 180f,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
            }
            rotate(-rotationAngle * 0.7f) {
                drawArc(
                    color = primaryColor.copy(alpha = 0.25f),
                    startAngle = 45f,
                    sweepAngle = 60f,
                    useCenter = false,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                    )
                )
            }
        }

        // Inner Core Orb Container
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .border(1.dp, primaryColor.copy(alpha = 0.5f), CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.25f), JarvisDarkBackground)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Wave Animation Bars
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barHeights = listOf(14.dp, 28.dp, 18.dp, 32.dp, 22.dp, 12.dp)
                    barHeights.forEachIndexed { index, defaultHeight ->
                        val dynamicHeight = if (assistantState is AssistantState.Listening || assistantState is AssistantState.Speaking) {
                            (defaultHeight.value + (rmsLevel * (index + 1) * 1.5f)).coerceIn(10f, 48f).dp
                        } else {
                            defaultHeight * 0.5f
                        }

                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(dynamicHeight)
                                .background(primaryColor, shape = CircleShape)
                        )
                        if (index < barHeights.size - 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = statusText,
                    color = primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
