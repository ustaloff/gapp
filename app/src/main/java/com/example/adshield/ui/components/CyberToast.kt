package com.example.adshield.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import com.example.adshield.ui.theme.AdShieldTheme

enum class CyberToastType {
    SUCCESS,
    ERROR,
    INFO
}

@Composable
fun CyberToast(
    message: String,
    type: CyberToastType,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val typeColor = when (type) {
        CyberToastType.SUCCESS -> AdShieldTheme.colors.success // Green
        CyberToastType.ERROR -> MaterialTheme.colorScheme.error // Error (Red) - Sticking to standard Error
        CyberToastType.INFO -> AdShieldTheme.colors.info // Info (Yellow/Gold)
    }

    val icon = when (type) {
        CyberToastType.SUCCESS -> Icons.Filled.CheckCircle
        CyberToastType.ERROR -> Icons.Filled.Close
        CyberToastType.INFO -> Icons.Filled.Info
    }

    val title = when (type) {
        CyberToastType.SUCCESS -> "ACCESS GRANTED"
        CyberToastType.ERROR -> "ACCESS DENIED"
        CyberToastType.INFO -> "SYSTEM NOTICE"
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
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
