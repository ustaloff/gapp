package com.example.adshield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        // Ghost text (Cyan offset)
        Text(
            text = text,
            style = style,
            color = com.example.adshield.ui.theme.NeonCyan.copy(alpha = 0.5f),
            modifier = Modifier.offset(x = (offset * 1.5).dp, y = 0.dp),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        // Ghost text (Purple offset)
        Text(
            text = text,
            style = style,
            color = com.example.adshield.ui.theme.NeonPurple.copy(alpha = 0.5f),
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
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(5.dp),
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
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.8f),
                        borderColor.copy(alpha = 0.1f),
                        borderColor.copy(alpha = 0.8f)
                    )
                ),
                shape = shape
            )
            .padding(1.dp) // Inner padding for border
    ) {
        content()
    }
}
