package com.example.adshield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.ui.theme.AdShieldTheme

@Composable
fun CyberGraphSection(data: List<Int>, bpm: Int, isRunning: Boolean) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val offlineColor = MaterialTheme.colorScheme.error // Or Gray

    // Animation state for pulse
    val infiniteTransition = rememberInfiniteTransition(label = "monitoring_pulse")
    val pulseAlpha by if (isRunning) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    } // Static when offline

    // Threat Logic
    val (level, threatColor) = when {
        !isRunning -> "OFFLINE" to offlineColor.copy(alpha = 0.5f)
        bpm > 20 -> "HIGH" to com.example.adshield.ui.theme.ErrorRed // Red
        bpm > 5 -> "MED" to com.example.adshield.ui.theme.WarningOrange // Orange
        else -> "LOW" to primaryColor // Green/Primary
    }
    val progress = if (!isRunning) 0f else when {
        bpm > 30 -> 1f
        else -> (bpm / 30f).coerceIn(0.05f, 1f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                (if (isRunning) primaryColor else offlineColor).copy(alpha = 0.2f),
                AdShieldTheme.shapes.container
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        // HUD Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isRunning) "TRAFFIC ANALYSIS // LIVE" else "TRAFFIC ANALYSIS // OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) primaryColor else offlineColor,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isRunning) threatColor.copy(alpha = pulseAlpha) else offlineColor,
                            CircleShape
                        )
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // Canvas Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp) // Taller graph
                .background(Color.Black.copy(alpha = 0.3f), AdShieldTheme.shapes.screen)
                .border(
                    1.dp,
                    (if (isRunning) primaryColor else offlineColor).copy(alpha = 0.1f),
                    AdShieldTheme.shapes.screen
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                val width = size.width
                val height = size.height
                // Use all 60 points or whatever is available, but limit if needed
                val graphData = if (data.isEmpty()) List(60) { 0 } else data
                val max = (graphData.maxOrNull() ?: 5).coerceAtLeast(5).toFloat()
                val graphColor = if (isRunning) primaryColor else offlineColor.copy(alpha = 0.3f)

                // Draw Grid
                val verticalLines = 6 // roughly every 10 mins
                val horizontalLines = 4

                for (i in 1 until verticalLines) {
                    val x = (width / verticalLines) * i
                    drawLine(
                        color = graphColor.copy(alpha = 0.05f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                }

                for (i in 1 until horizontalLines) {
                    val y = (height / horizontalLines) * i
                    drawLine(
                        color = graphColor.copy(alpha = 0.05f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // Draw Path (Smooth Bezier) -- ONLY IF RUNNING OR DATA EXISTS
                if (isRunning && graphData.isNotEmpty()) {
                    val path = androidx.compose.ui.graphics.Path()
                    val stepX = width / (graphData.size - 1).coerceAtLeast(1)

                    graphData.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - ((value / max) * height)

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            val prevX = (index - 1) * stepX
                            val prevY = height - ((graphData[index - 1] / max) * height)
                            val controlX1 = prevX + (x - prevX) / 2
                            path.cubicTo(controlX1, prevY, controlX1, y, x, y)
                        }
                    }

                    // Stroke
                    drawPath(
                        path = path,
                        color = graphColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )

                    // Fill Gradient (Optional, requires closing path)
                    path.lineTo(width, height)
                    path.lineTo(0f, height)
                    path.close()
                    drawPath(
                        path = path,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                graphColor.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Footer: Analysis Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Load Level Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    //text = "SYSTEM LOAD: $level",
                    text = if (isRunning) "THREAT: $level" else "SYSTEM: STANDBY",
                    style = MaterialTheme.typography.labelSmall,
                    color = threatColor,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 10.sp
                )
                Spacer(Modifier.width(8.dp))
                // Simple stacked bar for load
                Row(modifier = Modifier
                    .height(4.dp)
                    .width(40.dp)) {
                    val segments = 4
                    val filled = (progress * segments).toInt().coerceIn(0, segments)
                    for (i in 0 until segments) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 1.dp)
                                .background(
                                    if (i < filled) threatColor else threatColor.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        )
                    }
                }
            }

            // BPM / Rate
            Text(
                text = if (isRunning) "ACT :: $bpm/MIN" else "ACT :: ---",
                style = MaterialTheme.typography.labelSmall,
                color = if (isRunning) primaryColor else offlineColor,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}
