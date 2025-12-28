package com.example.adshield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.adshield.ui.theme.AdShieldTheme

// Helper for type conversion in Canvas loop
fun classWithVal(v: Float) = v

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
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition()
    // Random jitter offset
    val offset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    // Flicker opacity
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0.8f at 0
                0.2f at 100 // Quick glitch
                1f at 200
                1f at 2500
                0.5f at 2600
                1f at 3000
            },
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = modifier) {
        // Ghost text (Green offset)
        Text(
            text = text,
            style = style,
            color = com.example.adshield.ui.theme.NeonGreen.copy(alpha = 0.5f),
            modifier = Modifier.offset(x = (offset * 1.5).dp, y = 0.dp),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        // Ghost text (White/Noise offset)
        Text(
            text = text,
            style = style,
            color = Color.White.copy(alpha = 0.3f), // White noise instead of purple
            modifier = Modifier.offset(x = (-offset).dp, y = (offset * 0.5).dp),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        // Main Text
        Text(
            text = text,
            style = style,
            color = color.copy(alpha = alpha),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = AdShieldTheme.shapes.container,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    )
                ),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = borderColor.copy(alpha = 0.2f), // Solid border, slight alpha
                shape = shape
            )
            .padding(1.dp) // Inner padding for border
    ) {
        content()
    }
}
