package com.example.adshield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition()
    
    // Rotating outer ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        )
    )

    // Pulsing inner ring
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Outer decorative ring (rotating)
        Canvas(modifier = Modifier.size(220.dp)) {
            rotate(rotation) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.1f),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
                )
            }
        }
        
        // Static ring
        Box(
            modifier = Modifier
                .size(160.dp)
                .border(1.dp, primaryColor.copy(alpha = 0.3f), CircleShape)
        )
        
        // Pulsing echo
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(pulseScale)
                .border(2.dp, primaryColor.copy(alpha = pulseAlpha), CircleShape)
        )

        // Center visual
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(1.dp, primaryColor.copy(alpha = 0.5f), CircleShape)
                .shadow(
                    elevation = 15.dp,
                    shape = CircleShape,
                    spotColor = primaryColor
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Close, 
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(48.dp)
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
        shape = RoundedCornerShape(8.dp),
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
