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
    val (primaryColor, statusText, subText) = when (userAccess.state) {
        UserAccessState.FREE -> StatusConfig(
            color = NeonGreenError, // Warm crimson/red
            status = "FREE",
            sub = "LOW PROTECTION"
        )
        UserAccessState.TRIAL -> {
            val daysLeft = getDaysLeft(userAccess.trialEndsAt)
            StatusConfig(
                color = NeonAmberPrimary,
                status = "TRIAL",
                sub = daysLeft.toString() + "d left"
            )
        }
        UserAccessState.PREMIUM -> {
            val daysLeft = getDaysLeft(userAccess.premiumExpiresAt)
            val sub = if (daysLeft <= 7 && daysLeft >= 0) "{$daysLeft}d left" else null
            StatusConfig(
                color = NeonGreenPrimary,
                status = "PRO",
                sub = sub // Null means hide second line
            )
        }
    }

    // Animation for pulsing glow (only if needed)
    val alphaAnim by rememberInfiniteTransition(label = "badgePulse").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (subText !== null) {
            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    lineHeight = 5.sp,
                    letterSpacing = 1.sp
                ),
                color = primaryColor.copy(alpha = alphaAnim)
            )

            Spacer(Modifier.width(4.dp))
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
                    shape = RoundedCornerShape(2.dp)
                )
                .background(
                    color = primaryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(2.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    lineHeight = 5.sp,
                    letterSpacing = 1.sp
                ),
                color = primaryColor.copy(alpha = alphaAnim)
            )
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
    val sub: String?
)
