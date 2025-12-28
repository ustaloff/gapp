package com.example.adshield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.adshield.ui.theme.NeonGreen
import com.example.adshield.ui.theme.AdShieldTheme
import com.example.adshield.data.VpnLogEntry
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun GridBackground(
    modifier: Modifier = Modifier,
    gridSize: Dp = 20.dp,
    gridColor: Color = com.example.adshield.ui.theme.NeonGreen.copy(alpha = 0.05f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val step = gridSize.toPx()

        // Draw vertical lines
        for (x in 0..classWithVal(canvasWidth / step).toInt()) {
            val xPos = x * step
            drawLine(
                color = gridColor,
                start = Offset(xPos, 0f),
                end = Offset(xPos, canvasHeight),
                strokeWidth = 1f
            )
        }

        // Draw horizontal lines
        for (y in 0..classWithVal(canvasHeight / step).toInt()) {
            val yPos = y * step
            drawLine(
                color = gridColor,
                start = Offset(0f, yPos),
                end = Offset(canvasWidth, yPos),
                strokeWidth = 1f
            )
        }
    }
}

// Helper for type conversion in Canvas loop
private fun classWithVal(v: Float) = v

@Composable
fun Scanline(
    modifier: Modifier = Modifier,
    color: Color = Color.Black.copy(alpha = 0.1f)
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val y = size.height * offsetY
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, color, Color.Transparent),
                startY = y - 50f,
                endY = y + 50f
            )
        )
    }
}


@Composable
fun CyberStatCard(
    label: String,
    value: String,
    progress: Float? = null,
    progressSegments: Int = 1,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    NeonCard(
        modifier = modifier,
        borderColor = MaterialTheme.colorScheme.primary,
        shape = AdShieldTheme.shapes.container
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 1.5.sp
                )
                if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            // Progress Bar
            if (progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                if (progressSegments == 1) {
                    // Smooth Continuous Bar for Data Saved
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                com.example.adshield.ui.theme.NeonGreen.copy(alpha = 0.1f),
                                AdShieldTheme.shapes.indicator
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    com.example.adshield.ui.theme.NeonGreen,
                                    AdShieldTheme.shapes.indicator
                                )
                        )
                    }
                } else {
                    // Segmented Bar for Time Saved
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val filledSegments =
                            (progress * progressSegments).roundToInt().coerceIn(0, progressSegments)
                        for (i in 0 until progressSegments) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        color = if (i < filledSegments) com.example.adshield.ui.theme.NeonGreen else com.example.adshield.ui.theme.NeonGreen.copy(
                                            alpha = 0.1f
                                        ),
                                        shape = AdShieldTheme.shapes.indicator
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// Extension function for drawing rounded rect with shadow/glow


@Composable
fun CyberFilterCard(
    ruleCount: Int,
    isUpdating: Boolean,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AdShieldTheme.shapes.container,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Protection Engine",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isUpdating) "UPDATING RULES..." else "$ruleCount rules online",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            TextButton(
                onClick = onReload,
                enabled = !isUpdating
            ) {
                Text(
                    text = if (isUpdating) "SYNCING..." else "RELOAD",
                    color = if (isUpdating) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

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
            // Inner Icon (Custom Power Symbol)
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


@Composable
fun CyberNavBar(
    isRunning: Boolean,
    onPowerClick: () -> Unit,
    currentScreen: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center // Center content in the box
    ) {
        // Floating Pill Container
        Surface(
            modifier = Modifier
                .height(72.dp) // Height for the bar
                .fillMaxWidth(),
            shape = AdShieldTheme.shapes.menu, // Fully rounded pill
            //shape = RoundedCornerShape(3.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            ),
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. HOME
                NavBarItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = currentScreen == "HOME",
                    onClick = { onNavigate("HOME") }
                )

                // 2. LOGS
                NavBarItem(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "Logs",
                    isSelected = currentScreen == "LOGS",
                    onClick = { onNavigate("LOGS") }
                )

                // 3. CENTRAL SPACER (Hidden/Transparent to hold space)
                Spacer(modifier = Modifier.width(64.dp))

                // 4. STATS
                NavBarItem(
                    icon = androidx.compose.material.icons.Icons.Default.Info,
                    label = "Stats",
                    isSelected = currentScreen == "STATS",
                    onClick = { onNavigate("STATS") }
                )

                // 5. SETTINGS
                NavBarItem(
                    icon = Icons.Default.Settings,
                    label = "Config",
                    isSelected = currentScreen == "SETTINGS",
                    onClick = { onNavigate("SETTINGS") }
                )
            }
        }

        // CENTER FLOATING POWER BUTTON (No Offset = Vertically Centered)
        CyberMiniPowerButton(
            isRunning = isRunning,
            onClick = onPowerClick,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun NavBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}


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
                if (isRunning && graphData.isNotEmpty() && graphData.any { it > 0 }) {
                    val path = Path()
                    // ... (path building logic simplified for brevity, assume unchanged or reconstructed)
                    // Let's assume the previous logic works, we just need to add the Brush fill below it.

                    // Construct path from points
                    val stepX = width / (graphData.size - 1)
                    path.moveTo(0f, height - (graphData.first().toFloat() / max) * height)

                    for (i in 1 until graphData.size) {
                        val pX = i * stepX
                        val pY = height - (graphData[i].toFloat() / max) * height
                        // Simple line to for now, or bezier if we had the logic
                        path.lineTo(pX, pY)
                    }

                    // Draw Gradient Fill
                    val fillPath = Path()
                    fillPath.addPath(path)
                    fillPath.lineTo(width, height)
                    fillPath.lineTo(0f, height)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw Stroke
                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3f,
                            cap = StrokeCap.Round
                        )
                    )
                } else if (!isRunning) {
                    // Draw Flat Line or Static Noise if offline?
                    // Let's just draw a flat line at the bottom
                    drawLine(
                        color = offlineColor.copy(alpha = 0.3f),
                        start = Offset(0f, height),
                        end = Offset(width, height),
                        strokeWidth = 2.dp.value * density
                    )
                }
            }
            // Scanline Overlay
            //if (isRunning) {
            Scanline(
                modifier = Modifier.fillMaxSize(),
                color = primaryColor.copy(alpha = 0.01f)
            )
            //}
        }

        // Time Labels
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "-60m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
            Text(
                "-45m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                fontSize = 10.sp
            )
            Text(
                "-30m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
            Text(
                "-15m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                fontSize = 10.sp
            )
            Text(
                "NOW",
                style = MaterialTheme.typography.labelSmall,
                color = if (isRunning) primaryColor else offlineColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        // Threat Indicator Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(threatColor.copy(alpha = 0.2f), AdShieldTheme.shapes.indicator)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(threatColor, AdShieldTheme.shapes.indicator)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Footer: Threat Level & BPM
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bottom Left: Threat Logic
            Text(
                text = if (isRunning) "THREAT: $level" else "SYSTEM: STANDBY",
                color = threatColor,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )

            // Bottom Right: BPM
            Text(
                text = if (isRunning) "ACT :: $bpm/MIN" else "ACT :: ---",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}


// ----------------------------------------------------
// CYBER TOAST
// ----------------------------------------------------

enum class CyberToastType {
    SUCCESS,
    ERROR,
    INFO
}

@Composable
fun CyberTerminal(
    logs: List<VpnLogEntry>,
    onLogClick: (String) -> Unit
) {
    // 1. Cursor Animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    // 2. Auto-scroll
    val listState = rememberScrollState()
    var autoScrollEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(logs.size, autoScrollEnabled) {
        if (autoScrollEnabled && logs.isNotEmpty()) {
            listState.animateScrollTo(listState.maxValue)
        }
    }

    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        borderColor = MaterialTheme.colorScheme.primary,
        shape = AdShieldTheme.shapes.container
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ... Header ...
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "[ TERMINAL SESSION ACTIVE ]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                // Auto-scroll toggle
                Text(
                    text = "AUTO-SCROLL: ${if (autoScrollEnabled) "ON" else "OFF"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = if (autoScrollEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { autoScrollEnabled = !autoScrollEnabled }
                )
            }

            Spacer(Modifier.height(8.dp))

            // SCREEN CONTAINER (Box for layering Scanline)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), AdShieldTheme.shapes.screen)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        AdShieldTheme.shapes.screen
                    )
                    .clip(AdShieldTheme.shapes.screen) // Clip content to shape
            ) {
                // USE COLUMN WITH VERTICAL SCROLL instead of LazyColumn for simple cursor appending at end
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(listState)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            "> Initializing system...",
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Text(
                            "> Waiting for traffic...",
                            color = Color.Gray,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    logs.reversed().forEach { log ->
                        // Determine Color and Label based on Status
                        val (color, prefix, isClickable) = when (log.status) {
                            com.example.adshield.filter.FilterEngine.FilterStatus.BLOCKED ->
                                Triple(com.example.adshield.ui.theme.ErrorRed, "BLK", true) // Red
                            com.example.adshield.filter.FilterEngine.FilterStatus.BLOCKED_USER ->
                                Triple(
                                    com.example.adshield.ui.theme.WarningOrange,
                                    "BAN",
                                    true
                                ) // Orange (Manual Ban)
                            com.example.adshield.filter.FilterEngine.FilterStatus.ALLOWED_USER ->
                                Triple(
                                    com.example.adshield.ui.theme.NeonGreen,
                                    "USR",
                                    true
                                ) // Green
                            com.example.adshield.filter.FilterEngine.FilterStatus.ALLOWED_SYSTEM ->
                                Triple(Color.Gray, "SYS", false) // Gray, Non-interactive
                            com.example.adshield.filter.FilterEngine.FilterStatus.SUSPICIOUS ->
                                Triple(
                                    com.example.adshield.ui.theme.WarningYellow,
                                    "WRN",
                                    true
                                ) // Yellow, Warning, Clickable
                            com.example.adshield.filter.FilterEngine.FilterStatus.ALLOWED_DEFAULT ->
                                Triple(Color.White, "ALW", true) // White, CAN BE BLOCKED
                        }

                        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

                        Row(
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .clickable(enabled = isClickable) { onLogClick(log.domain) }
                        ) {
                            Text(
                                text = "> ${timeFormat.format(Date(log.timestamp))} ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "[$prefix] ",
                                style = MaterialTheme.typography.bodySmall,
                                color = color,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = log.domain,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isClickable) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // BLINKING CURSOR
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(8.dp, 14.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha))
                    )
                }

                // Scanline Effect (Overlay only on screen)
                Scanline(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "Tap any entry to manage domain.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun CyberToast(
    message: String,
    type: CyberToastType,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val typeColor = when (type) {
        CyberToastType.SUCCESS -> com.example.adshield.ui.theme.NeonGreen // Green
        CyberToastType.ERROR -> com.example.adshield.ui.theme.ErrorRed // Red
        CyberToastType.INFO -> MaterialTheme.colorScheme.primary // Cyan
    }

    val icon = when (type) {
        CyberToastType.SUCCESS -> androidx.compose.material.icons.Icons.Filled.CheckCircle
        CyberToastType.ERROR -> androidx.compose.material.icons.Icons.Filled.Close
        CyberToastType.INFO -> androidx.compose.material.icons.Icons.Filled.Info
    }

    val title = when (type) {
        CyberToastType.SUCCESS -> "ACCESS GRANTED"
        CyberToastType.ERROR -> "ACCESS DENIED"
        CyberToastType.INFO -> "SYSTEM NOTICE"
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 }) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 }) + androidx.compose.animation.fadeOut(),
        modifier = modifier
            //.padding(bottom = 100.dp) // Removed for flexible positioning
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(80.dp) // Fixed height for consistent look
    ) {
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = typeColor.copy(alpha = 0.8f),
            shape = CutCornerShape(
                topStart = 0.dp,
                bottomEnd = 0.dp,
                topEnd = 12.dp,
                bottomStart = 12.dp
            )
        ) {
            // Inner Box for background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
