package com.example.adshield.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.ui.theme.AdShieldTheme
import kotlin.math.roundToInt

@Composable
fun CyberStatCard(
    label: String,
    value: String,
    progress: Float? = null,
    progressSegments: Int = 1,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    NeonCard(
        modifier = modifier,
        borderColor = MaterialTheme.colorScheme.primary,
        shape = AdShieldTheme.shapes.container
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
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

            // Progress Bar
            if (progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                if (progressSegments == 1) {
                    // Smooth Continuous Bar for Data Saved
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                com.example.adshield.ui.theme.NeonGreen.copy(alpha = 0.1f),
                                AdShieldTheme.shapes.indicator
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    com.example.adshield.ui.theme.NeonGreen,
                                    AdShieldTheme.shapes.indicator
                                )
                        )
                    }
                } else {
                    // Segmented Bar for Time Saved
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val filledSegments =
                            (progress * progressSegments).roundToInt().coerceIn(0, progressSegments)
                        for (i in 0 until progressSegments) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        color = if (i < filledSegments) com.example.adshield.ui.theme.NeonGreen else com.example.adshield.ui.theme.NeonGreen.copy(
                                            alpha = 0.1f
                                        ),
                                        shape = AdShieldTheme.shapes.indicator
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
