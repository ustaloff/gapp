package com.example.adshield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.data.UserAccess
import com.example.adshield.data.UserAccessState
import com.example.adshield.ui.theme.*
import java.util.concurrent.TimeUnit

@Composable
fun UserStatusBadge(
    userAccess: UserAccess,
    modifier: Modifier = Modifier
) {
    // Determine colors and text based on state
    val (primaryColor, statusText, subText, shouldPulse) = when (userAccess.state) {
        UserAccessState.FREE -> StatusConfig(
            color = NeonGreenError, // Warm crimson/red
            status = "FREE MODE",
            sub = "Limited protection",
            pulse = false
        )
        UserAccessState.TRIAL -> {
            val daysLeft = getDaysLeft(userAccess.trialEndsAt)
            StatusConfig(
                color = NeonAmberPrimary,
                status = "TRIAL ACTIVE",
                sub = "Ends in $daysLeft days",
                pulse = true
            )
        }
        UserAccessState.PREMIUM -> {
            val daysLeft = getDaysLeft(userAccess.premiumExpiresAt)
            val sub = if (daysLeft <= 7 && daysLeft >= 0) "Renews in $daysLeft days" else null
            StatusConfig(
                color = NeonGreenPrimary,
                status = "PREMIUM ACTIVE",
                sub = sub, // Null means hide second line
                pulse = false
            )
        }
    }

    // Animation for pulsing glow (only if needed)
    val alphaAnim by if (shouldPulse) {
        rememberInfiniteTransition(label = "badgePulse").animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500),
                repeatMode = RepeatMode.Reverse
            ), label = "alpha"
        )
    } else {
        rememberInfiniteTransition(label = "static").animateFloat(
            initialValue = 0.8f, targetValue = 0.8f, animationSpec = infiniteRepeatable(tween(1000))
        )
    }

    // MAIN BADGE CONTAINER
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.2f),
                        primaryColor.copy(alpha = 0.6f * alphaAnim),
                        primaryColor.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = primaryColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator Dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(primaryColor.copy(alpha = if (shouldPulse) alphaAnim else 1f), MaterialTheme.shapes.small)
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            Column {
                // Top Line: STATUS
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = primaryColor,
                    fontSize = 10.sp
                )
                
                // Bottom Line: Secondary Info (if visible)
                if (subText != null) {
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp
                        ),
                        color = primaryColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun getDaysLeft(timestamp: Long?): Long {
    if (timestamp == null) return 99
    val diff = timestamp - System.currentTimeMillis()
    if (diff < 0) return 0
    return TimeUnit.MILLISECONDS.toDays(diff)
}

private data class StatusConfig(
    val color: Color,
    val status: String,
    val sub: String?,
    val pulse: Boolean
)
