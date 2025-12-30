package com.example.adshield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun CyberMiniPowerButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor =
        if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.5f
        ) // Gray when off
    val infiniteTransition = rememberInfiniteTransition()

    // Animations
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        )
    )

    val pulseScale by if (isRunning) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseOutCubic),
                repeatMode = RepeatMode.Restart
            )
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val pulseAlpha by if (isRunning) {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseOutCubic),
                repeatMode = RepeatMode.Restart
            )
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier
            .size(64.dp) // Base size
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. External Pulse Ring (Mini)
        if (isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .border(1.dp, primaryColor.copy(alpha = pulseAlpha), CircleShape)
            )
        }

        // 2. Rotating Dash Rings (Mini)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation }) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = size.minDimension / 2,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }

        // 3. Main Container (Glassmorphism base)
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (isRunning) 0.3f else 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    1.dp,
                    primaryColor.copy(alpha = if (isRunning) 0.8f else 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner Icon
            if (isRunning) {
                CyberLogo(
                    size = 28.dp,
                    color = primaryColor
                )
            } else {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val strokeWidth = 3.dp.toPx()
                    val center = Offset(size.width / 2, size.height / 2)

                    // Arc
                    drawArc(
                        color = primaryColor,
                        startAngle = -60f,
                        sweepAngle = 300f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )

                    // Stick
                    drawLine(
                        color = primaryColor,
                        start = Offset(center.x, 0f + strokeWidth),
                        end = Offset(center.x, center.y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
