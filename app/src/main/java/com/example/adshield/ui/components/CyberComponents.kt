package com.example.adshield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.adshield.ui.theme.NeonGreen

@Composable
fun GridBackground(
    modifier: Modifier = Modifier,
    gridSize: Dp = 20.dp,
    gridColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
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
fun PulsatingShield(
    modifier: Modifier = Modifier,
    isSecure: Boolean = true
) {
    val primaryColor = if (isSecure) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f)
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animations (Active only if secure)
    val rotation by if (isSecure) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing)
            )
        )
    } else { remember { mutableFloatStateOf(0f) } }

    val pulseAlpha by if (isSecure) {
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = EaseOutCubic),
                repeatMode = RepeatMode.Restart
            )
        )
    } else { remember { mutableFloatStateOf(0f) } }
    
    val pulseScale by if (isSecure) {
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = EaseOutCubic),
                repeatMode = RepeatMode.Restart
            )
        )
    } else { remember { mutableFloatStateOf(1f) } }

    val floatOffset by if (isSecure) {
        infiniteTransition.animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            )
        )
    } else { remember { mutableFloatStateOf(0f) } }
    
    val shieldShape = CircleShape

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 1. External Pulse Ring (Ethereal ripple)
        if (isSecure) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .border(1.dp, primaryColor.copy(alpha = pulseAlpha), CircleShape)
            )
        }

        // 2. Rotating Dash Rings (Circuit Lines)
        if (isSecure) {
            Canvas(modifier = Modifier.size(160.dp).graphicsLayer { rotationZ = rotation }) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.3f),
                    radius = size.minDimension / 2,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 40f), 0f)
                    )
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.15f),
                    radius = (size.minDimension / 2) - 30f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 20f), 10f)
                    )
                )
            }
        }
        
        // 3. Main Container (Glassmorphism base)
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (isSecure) 0.15f else 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .border(1.dp, primaryColor.copy(alpha = if (isSecure) 0.5f else 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
             // Inner Gradient Glow
             Box(
                 modifier = Modifier
                     .fillMaxSize()
                     .background(
                         brush = Brush.linearGradient(
                             colors = listOf(
                                 primaryColor.copy(alpha = if (isSecure) 0.2f else 0f),
                                 Color.Transparent
                             )
                         ),
                         shape = CircleShape
                     )
             )
        }

        // 4. Floating Icon
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = floatOffset }
                .size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            // Custom Power Icon "Circle with Stick"
            Canvas(modifier = Modifier.size(42.dp)) {
                val strokeWidth = 8f
                val radius = size.minDimension / 2 - strokeWidth
                val center = Offset(size.width / 2, size.height / 2)
                
                // 1. The Arc (Circle with top gap)
                // Start angle -90 is top. We want a gap at top.
                // Let's start at -60 and sweep 300 degrees.
                drawArc(
                    color = primaryColor,
                    startAngle = -60f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                
                // 2. The Stick (Vertical line at top)
                drawLine(
                    color = primaryColor,
                    start = Offset(center.x, center.y - radius - strokeWidth/2), // Slightly above center logic
                    end = Offset(center.x, center.y - radius * 0.2f),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}


// Workaround for scale modifier missing import or needing graphicsLayer
fun Modifier.scale(scale: Float) = this.then(Modifier.graphicsLayer {
    scaleX = scale
    scaleY = scale
})

@Composable
fun CyberStatCard(
    label: String,
    value: String,
    subValue: String? = null,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
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
            if (subValue != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// Extension function for drawing rounded rect with shadow/glow
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlow(
    color: Color,
    radius: Float,
    center: Offset,
    size: Float
) {
    // Simplified glow implementation
}

@Composable
fun CyberUnifiedControl(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isRunning) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White
    val infiniteTransition = rememberInfiniteTransition(label = "unified_pulse")
    
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, primaryColor.copy(alpha = borderAlpha), RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(5.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT COLUMN: Status Information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Status Header
                Text(
                    text = if (isRunning) "SYSTEM SECURE" else "SYSTEM VULNERABLE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Status Subtitle (Boxed)
                Box(
                    modifier = Modifier
                        .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                        .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                     Text(
                        text = if (isRunning) "TUNNELING ACTIVE // IP MASKED" else "PROTECTION DISABLED // EXPOSED",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
                

            }
            
            // RIGHT COLUMN: Animated Indicator (Mini Shield)
            Box(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(150.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                // Native size (approx 160dp internal, scaled slightly to fit 150dp comfortably or let it overflow slightly)
                PulsatingShield(
                   modifier = Modifier.scale(0.9f),
                   isSecure = isRunning
                )
            }
        }
    }
}

@Composable
fun CyberFilterCard(
    ruleCount: Int,
    isUpdating: Boolean,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
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
