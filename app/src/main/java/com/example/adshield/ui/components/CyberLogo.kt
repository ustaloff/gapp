package com.example.adshield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.adshield.R

@Composable
fun CyberLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")

    // 1. Breathing Glow Effect
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // 2. Scanner Line Effect
    val scannerProgress by infiniteTransition.animateFloat(
        initialValue = -0.2f, // Start above
        targetValue = 1.2f,  // End below
        animationSpec = infiniteRepeatable(
            animation = tween(2500, delayMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanner"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Base Logo Layer
        Image(
            painter = painterResource(id = R.drawable.ic_cyber_logo),
            contentDescription = "Cyber Shield Logo",
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.colorMatrix(
                androidx.compose.ui.graphics.ColorMatrix(
                    floatArrayOf(
                        0f, 0f, 0f, 0f, color.red * 255,
                        0f, 0f, 0f, 0f, color.green * 255,
                        0f, 0f, 0f, 0f, color.blue * 255,
                        0.33f, 0.33f, 0.33f, 0f, 0f
                    )
                )
            )
        )

        // Overlay Scanner Layer
        /*Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.toPx()
            val canvasHeight = size.toPx()
            val scanY = scannerProgress * canvasHeight

            // Draw scanning line only if within bounds (visually)
            if (scannerProgress in 0.0f..1.0f) {
                drawLine(
                    color = color.copy(alpha = 0.8f),
                    start = Offset(0f, scanY),
                    end = Offset(canvasWidth, scanY),
                    strokeWidth = 2.dp.toPx()
                )
                // Draw gradient trail behind the line
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0f), color.copy(alpha = 0.3f)),
                        startY = scanY - 40f,
                        endY = scanY
                    ),
                    topLeft = Offset(0f, scanY - 40f),
                    size = Size(canvasWidth, 40f)
                )
            }
        }*/
    }
}
