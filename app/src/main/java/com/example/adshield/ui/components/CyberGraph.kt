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
fun CyberGraphSection(
    data: List<Int>, // Blocked
    totalData: List<Int> = emptyList(), // Total Traffic (Optional for compat, but we'll pass it)
    bpm: Int, 
    isRunning: Boolean,
    showBpm: Boolean = true,           // FALSE for FREE users
    showThreatLevel: Boolean = true,   // FALSE for FREE users (shows "---")
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary // Or a distinct color for Total
    val offlineColor = MaterialTheme.colorScheme.error // Or Gray

    // ... (rest of val level, threatColor etc)
    
    // Pulse and Threat Logic ...
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
        bpm > 20 -> "HIGH" to MaterialTheme.colorScheme.error // Red
        bpm > 5 -> "MED" to AdShieldTheme.colors.warning // Orange
        else -> "LOW" to primaryColor // Green/Primary
    }
    val progress = if (!isRunning) 0f else when {
        bpm > 30 -> 1f
        else -> (bpm / 30f).coerceIn(0.05f, 1f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                (if (isRunning) primaryColor else offlineColor).copy(alpha = 0.2f),
                MaterialTheme.shapes.small
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        // ... (Header same)
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

            // Right: Legend
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Total Legend
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(secondaryColor.copy(alpha = 0.5f), CircleShape)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "ALL",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = secondaryColor.copy(alpha = 0.7f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                Spacer(Modifier.width(8.dp))
                
                // Blocked Legend
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(threatColor, CircleShape)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "BLK",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = threatColor.copy(alpha = 0.8f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // Canvas Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp) // Taller graph
                .background(Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.extraSmall)
                .border(
                    1.dp,
                    (if (isRunning) primaryColor else offlineColor).copy(alpha = 0.1f),
                    MaterialTheme.shapes.extraSmall
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                val width = size.width
                val height = size.height
                
                // DATA PREP
                val blockedData = data.ifEmpty { List(60) { 0 } }
                val trafficData = totalData.ifEmpty { List(60) { 0 } }
                
                // Determine Max Scale based on TOTAL traffic (so blocked is relative to it)
                val maxTotal = (trafficData.maxOrNull() ?: 5).coerceAtLeast(5).toFloat()
                
                // Colors
                val trafficColor = if (isRunning) threatColor.copy(alpha = 0.5f) else offlineColor.copy(alpha = 0.1f)
                val blockedColor = if (isRunning) threatColor else offlineColor.copy(alpha = 0.3f)

                // Draw Grid (Same)
                val verticalLines = 6 // roughly every 10 mins
                val horizontalLines = 4

                for (i in 1 until verticalLines) {
                    val x = (width / verticalLines) * i
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                }

                for (i in 1 until horizontalLines) {
                    val y = (height / horizontalLines) * i
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // DRAW PATHS -- ONLY IF RUNNING
                if (isRunning) {
                    
                    // 1. BLOCKED TRAFFIC LINE (Background/Underlay)
                    if (blockedData.isNotEmpty()) {
                        val path = androidx.compose.ui.graphics.Path()
                        val stepX = width / (blockedData.size - 1).coerceAtLeast(1)

                        blockedData.forEachIndexed { index, value ->
                            val x = index * stepX
                            // Use SAME maxTotal for scale to show proportion
                            val y = height - ((value / maxTotal) * height)

                            if (index == 0) path.moveTo(x, y)
                            else {
                                val prevX = (index - 1) * stepX
                                val prevY = height - ((blockedData[index - 1] / maxTotal) * height)
                                val cx = prevX + (x - prevX) / 2
                                path.cubicTo(cx, prevY, cx, y, x, y)
                            }
                        }

                        // Fill Gradient
                        val fillPath = androidx.compose.ui.graphics.Path()
                        fillPath.addPath(path)
                        fillPath.lineTo(width, height)
                        fillPath.lineTo(0f, height)
                        fillPath.close()
                        
                        drawPath(
                            path = fillPath,
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    blockedColor.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Stroke
                        drawPath(
                            path = path,
                            color = blockedColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }

                    // 2. TOTAL TRAFFIC LINE (Foreground/Overlay)
                    if (trafficData.isNotEmpty()) {
                        val path = androidx.compose.ui.graphics.Path()
                        val stepX = width / (trafficData.size - 1).coerceAtLeast(1)

                        trafficData.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = height - ((value / maxTotal) * height)

                            if (index == 0) path.moveTo(x, y)
                            else {
                                val prevX = (index - 1) * stepX
                                val prevY = height - ((trafficData[index - 1] / maxTotal) * height)
                                val cx = prevX + (x - prevX) / 2
                                path.cubicTo(cx, prevY, cx, y, x, y)
                            }
                        }
                        
                        drawPath(
                            path = path,
                            color = trafficColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.5.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                }
            }
        }

        // Time Axis Labels
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labels = listOf("-60m", "-45m", "-30m", "-15m", "NOW")
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (isRunning) primaryColor else offlineColor).copy(alpha = 0.5f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))

        // Threat Line (Progress Bar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    color = (if (isRunning) threatColor else offlineColor).copy(alpha = 0.2f), // Track color
                    shape = MaterialTheme.shapes.extraSmall
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress) // Fill based on BPM progress
                    .fillMaxHeight()
                    .background(
                        color = if (isRunning) threatColor else offlineColor.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.extraSmall
                    )
            )
        }

        Spacer(Modifier.height(8.dp))

        // Footer: Analysis Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Load Level Indicator Text (respect showThreatLevel)
            val threatText = when {
                !isRunning -> "SYSTEM: STANDBY"
                !showThreatLevel -> "THREAT: ---"
                else -> "THREAT: $level"
            }
            Text(
                text = threatText,
                style = MaterialTheme.typography.labelSmall,
                color = if (showThreatLevel || !isRunning) threatColor else threatColor.copy(alpha = 0.5f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // BPM / Rate (respect showBpm)
            val bpmText = when {
                !isRunning -> "ACT :: ---"
                !showBpm -> "ACT :: ---"
                else -> "ACT :: $bpm/MIN"
            }
            Text(
                text = bpmText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isRunning && showBpm) primaryColor else offlineColor.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}
