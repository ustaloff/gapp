package com.example.adshield.ui.components

import androidx.compose.animation.animateColor
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun CyberMiniPowerButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    // 1. Alive Color Pulse (Cyan <-> Neon Blue)
    val baseColor = MaterialTheme.colorScheme.primary
    val activeColor by infiniteTransition.animateColor(
        initialValue = baseColor,
        targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), // Or use secondary
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color"
    )

    val primaryColor =
        if (isRunning) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    // 2. Rotation Animations (Double Ring)
    val rotationSlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "slow"
    )
    val rotationFast by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing)
        ),
        label = "fast"
    )

    // 3. Pulse Scale (Heartbeat)
    val pulseScale by if (isRunning) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing), // Faster, heartbeat-like
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    // 4. Glow Alpha
    val glowAlpha by if (isRunning) {
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // PRE-CALCULATE PIXEL VALUES to avoid scope issues
    val density = LocalDensity.current
    val outerStrokeWidth = with(density) { 2.dp.toPx() }
    val innerStrokeWidth = with(density) { 1.5.dp.toPx() }
    val iconStrokeWidth = with(density) { 3.dp.toPx() }

    Box(
        modifier = modifier
            .size(80.dp) // Increased touch target and visual space
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // --- LAYER 0: REACTOR GLOW (Behind everything) ---
        if (isRunning) {
            Box(
                modifier = Modifier
                    .size(100.dp) // Large spill-over glow
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = glowAlpha),
                                primaryColor.copy(alpha = 0f)
                            )
                        )
                    )
            )
        }

        // --- LAYER 1: OUTER RING (Slow Rotate) ---
        Canvas(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer { rotationZ = rotationSlow }
        ) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = size.minDimension / 2,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = outerStrokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), 0f)
                )
            )
        }

        // --- LAYER 2: INNER RING (Fast Rotate) ---
        Canvas(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer { rotationZ = rotationFast }
        ) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.5f),
                radius = size.minDimension / 2,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = innerStrokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f)
                )
            )
        }

        // --- LAYER 3: CORE (Pulsing Heart) ---
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(pulseScale) // The core literally physically expands
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (isRunning) 0.9f else 0.2f),
                            primaryColor.copy(alpha = if (isRunning) 0.4f else 0.1f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = primaryColor.copy(alpha = 0.8f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Icon
            if (isRunning) {
                CyberLogo(
                    size = 28.dp,
                    color = Color.White // White icon looks better on neon background
                )
            } else {
                // OFF State Icon (Simple Power Symbol)
                Canvas(modifier = Modifier.size(20.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawArc(
                        color = primaryColor,
                        startAngle = -60f,
                        sweepAngle = 300f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = iconStrokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(center.x, 0f + iconStrokeWidth),
                        end = Offset(center.x, center.y),
                        strokeWidth = iconStrokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
