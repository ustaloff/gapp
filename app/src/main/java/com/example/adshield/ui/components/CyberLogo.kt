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
    }
}
