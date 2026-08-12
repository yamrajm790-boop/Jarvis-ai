package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.AssistantState
import com.example.ui.theme.JarvisAccentGold
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkBackground
import com.example.ui.theme.JarvisSuccessGreen
import com.example.ui.theme.JarvisWarningRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisOrb(
    assistantState: AssistantState,
    rmsLevel: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransition")

    // Slow & fast rotation angles
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (assistantState is AssistantState.Thinking) 3000 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverseRotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Dynamic color based on state
    val targetColor = when (assistantState) {
        is AssistantState.Listening -> JarvisCyan
        is AssistantState.Thinking -> JarvisSuccessGreen
        is AssistantState.ExecutingTool -> JarvisAccentGold
        is AssistantState.Speaking -> Color(0xFF0072FF)
        is AssistantState.Idle -> JarvisCyan.copy(alpha = 0.85f)
    }

    val orbColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(600),
        label = "orbColor"
    )

    val micScale = if (assistantState is AssistantState.Listening || assistantState is AssistantState.Speaking) {
        (1.0f + (rmsLevel * 0.08f)).coerceIn(1.0f, 1.35f)
    } else {
        pulseScale
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glowing Blur Background
        Box(
            modifier = Modifier
                .size(size * 0.82f)
                .scale(micScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            orbColor.copy(alpha = 0.5f),
                            orbColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .blur(32.dp)
        )

        // Concentric HUD Ring Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = (this.size.minDimension / 2f) - 10f

            // Ring 1: Outer dashed ring
            rotate(rotationAngle, center) {
                drawCircle(
                    color = orbColor.copy(alpha = 0.35f),
                    radius = maxRadius,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f))
                    )
                )
            }

            // Ring 2: Concentric segmented arcs
            rotate(reverseRotation, center) {
                drawArc(
                    color = orbColor.copy(alpha = 0.7f),
                    startAngle = 15f,
                    sweepAngle = 100f,
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawArc(
                    color = orbColor.copy(alpha = 0.5f),
                    startAngle = 145f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawArc(
                    color = orbColor.copy(alpha = 0.8f),
                    startAngle = 240f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Ring 3: Inner delicate ring
            rotate(rotationAngle * 0.6f, center) {
                drawCircle(
                    color = orbColor.copy(alpha = 0.25f),
                    radius = maxRadius * 0.72f,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f))
                    )
                )
            }

            // Orbiting Particles on outer ring
            val particleAngleRad = Math.toRadians(rotationAngle.toDouble())
            val particleX = center.x + maxRadius * cos(particleAngleRad).toFloat()
            val particleY = center.y + maxRadius * sin(particleAngleRad).toFloat()
            drawCircle(
                color = orbColor,
                radius = 4.dp.toPx(),
                center = Offset(particleX, particleY)
            )

            val particleAngleRad2 = Math.toRadians((rotationAngle + 180).toDouble())
            val particleX2 = center.x + maxRadius * cos(particleAngleRad2).toFloat()
            val particleY2 = center.y + maxRadius * sin(particleAngleRad2).toFloat()
            drawCircle(
                color = orbColor.copy(alpha = 0.7f),
                radius = 3.dp.toPx(),
                center = Offset(particleX2, particleY2)
            )
        }

        // Inner Core Sphere
        Box(
            modifier = Modifier
                .size(size * 0.52f)
                .scale(micScale)
                .clip(CircleShape)
                .border(1.5.dp, orbColor.copy(alpha = 0.8f), CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f),
                            orbColor,
                            orbColor.copy(alpha = 0.6f),
                            JarvisDarkBackground
                        )
                    )
                )
        )
    }
}
