package com.example.adshield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.data.UserAccess
import com.example.adshield.data.UserAccessState
import com.example.adshield.ui.theme.ContentDescriptions

@Composable
fun CyberStatusCapsule(
    isRunning: Boolean,
    userAccess: UserAccess,
    modifier: Modifier = Modifier
) {
    // Determine State Config
    val isPremium = userAccess.state == UserAccessState.PREMIUM || userAccess.state == UserAccessState.TRIAL
    val isProtected = isRunning && isPremium
    val isAdBlockOnly = isRunning && !isPremium

    // Colors
    val themeColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    
    val (primaryColor, text, icon) = when {
        isProtected -> Triple(themeColor, "TUNNELING ACTIVE // IP MASKED", Icons.Default.Lock)
        isAdBlockOnly -> Triple(errorColor, "ADBLOCK ACTIVE // IP VISIBLE", Icons.Default.Warning)
        else -> Triple(errorColor, "PROTECTION DISABLED // EXPOSED", Icons.Default.Warning)
    }

    // Shimmer Animation for Protected State
    val transition = rememberInfiniteTransition(label = "capsule")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer"
    )

    val backgroundBrush = if (isProtected) {
        Brush.linearGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.2f),
                primaryColor.copy(alpha = 0.5f),
                primaryColor.copy(alpha = 0.2f)
            ),
            start = Offset(translateAnim, translateAnim),
            end = Offset(translateAnim + 100f, translateAnim + 100f)
        )
    } else {
        if (isAdBlockOnly) {
            // Solid dim red for adblock only
            SolidColor(primaryColor.copy(alpha = 0.15f))
        } else {
            // Transparent for disabled
            SolidColor(Color.Transparent)
        }
    }

    val borderBrush = if (isProtected) {
        Brush.linearGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.5f),
                primaryColor,
                primaryColor.copy(alpha = 0.5f)
            )
        )
    } else {
        SolidColor(primaryColor.copy(alpha = 0.5f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = ContentDescriptions.vpnStatusBadge,
                tint = primaryColor,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))

            // Text
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = primaryColor
            )
        }
    }
}
