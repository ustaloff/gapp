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
    val primaryColor = if (isSecure) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f)
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animations (Active only if secure)
    val rotation by if (isSecure) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(10000, easing = LinearEasing)
            )
        )
    } else { remember { mutableFloatStateOf(0f) } }

    val pulseAlpha by if (isSecure) {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Restart
            )
        )
    } else { remember { mutableFloatStateOf(0f) } }
    
    val pulseScale by if (isSecure) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Restart
            )
        )
    } else { remember { mutableFloatStateOf(1f) } }
    
    val shieldShape = CutCornerShape(16.dp)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Outer decorative ring (rotating)
        if (isSecure) {
            Box(
                 modifier = Modifier
                     .size(160.dp) // Adjusted size
                     .graphicsLayer { rotationZ = rotation }
                     .border(1.dp, primaryColor.copy(alpha = 0.1f), shieldShape)
            )
        }
        
        // Static ring
        Box(
            modifier = Modifier
                .size(120.dp) // Adjusted size
                .border(1.dp, primaryColor.copy(alpha = if (isSecure) 0.3f else 0.1f), shieldShape)
        )
        
        // Pulsing echo (Only if secure)
        if (isSecure) {
            Box(
                modifier = Modifier
                    .size(90.dp) // Adjusted size
                    .scale(pulseScale)
                    .border(2.dp, primaryColor.copy(alpha = pulseAlpha), shieldShape)
            )
        }

        // Center visual
        Box(
            modifier = Modifier
                .size(90.dp) // Adjusted size
                .border(1.dp, primaryColor.copy(alpha = 0.5f), shieldShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Close, 
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(32.dp)
            )
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
    val primaryColor = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Action Prompt
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Lock else Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isRunning) "TAP TO DEACTIVATE" else "TAP TO ACTIVATE",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
