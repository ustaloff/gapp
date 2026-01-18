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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.ui.theme.AdShieldTheme

@Composable
fun CyberBarGraphSection(
    data: List<Int>, // Blocked
    totalData: List<Int> = emptyList(), // Total Traffic
    bpm: Int,
    isRunning: Boolean,
    showBpm: Boolean = true,           // FALSE for FREE users
    showThreatLevel: Boolean = true,   // FALSE for FREE users
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val offlineColor = MaterialTheme.colorScheme.error
    val errorColor = MaterialTheme.colorScheme.error

    // Pulse Animation
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
    }

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
            .drawCyberCorners(
                color = (if (isRunning) primaryColor else offlineColor).copy(alpha = 0.4f),
                strokeWidth = 2.dp,
                cornerLength = 12.dp,
                cornerShape = MaterialTheme.shapes.small as? androidx.compose.foundation.shape.CornerBasedShape
            )
            .border(
                1.dp,
                (if (isRunning) primaryColor else offlineColor).copy(alpha = 0.2f),
                MaterialTheme.shapes.small
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

            // Right: Legend
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Total Legend
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(primaryColor.copy(alpha = 0.5f), CircleShape) // Primary for Total
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "ALL",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = primaryColor.copy(alpha = 0.7f),
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

        // Canvas Graph (Bar Chart)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
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
                    .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 0.dp)
            ) {
                val width = size.width
                val height = size.height

                // DATA PREP
                val blockedData = data.ifEmpty { List(60) { 0 } }
                val trafficData = totalData.ifEmpty { List(60) { 0 } }

                // Determine Max Scale based on TOTAL traffic (so blocked is relative to it)
                val maxTotal = (trafficData.maxOrNull() ?: 5).coerceAtLeast(5).toFloat()

                // BAR GRAPH LOGIC
                val barCount = 60
                val barSpacing = 2.dp.toPx()
                // Calculate total available width for bars (subtracting spacing)
                // We have (barCount - 1) gaps
                val totalGapWidth = (barCount - 1) * barSpacing
                val availableBarWidth = width - totalGapWidth
                val barWidth = (availableBarWidth / barCount).coerceAtLeast(1f)
                
                // Effective spacing (re-calculated to fit exactly)
                val stepX = (width - barWidth) / (barCount - 1).coerceAtLeast(1)

                // Colors
                val totalBarColor = if (isRunning) primaryColor.copy(alpha = 0.3f) else offlineColor.copy(alpha = 0.1f)
                val blockedBarColor = if (isRunning) errorColor else offlineColor.copy(alpha = 0.3f)

                // DRAW BARS
                // We iterate 0..59.
                for (i in 0 until barCount) {
                    val x = i * stepX
                    
                    // Safe access in case list size differs (shouldn't if passed correctly, but safe is better)
                    val totalVal = trafficData.getOrElse(i) { 0 }
                    val blockedVal = blockedData.getOrElse(i) { 0 }

                    // HEIGHTS
                    val totalBarHeight = (totalVal / maxTotal) * height
                    val blockedBarHeight = (blockedVal / maxTotal) * height

                    // 1. Draw TOTAL Bar (Background)
                    if (totalBarHeight > 0) {
                        drawRoundRect(
                            color = totalBarColor,
                            topLeft = Offset(x, height - totalBarHeight),
                            size = Size(barWidth, totalBarHeight),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }

                    // 2. Draw BLOCKED Bar (Foreground/Overlay)
                    if (blockedBarHeight > 0) {
                         drawRoundRect(
                            color = blockedBarColor,
                            topLeft = Offset(x, height - blockedBarHeight),
                            size = Size(barWidth, blockedBarHeight),
                            cornerRadius = CornerRadius(2.dp.toPx())
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
                    color = (if (isRunning) threatColor else offlineColor).copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.extraSmall
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
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
