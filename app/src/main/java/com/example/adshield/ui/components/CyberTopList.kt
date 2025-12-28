package com.example.adshield.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import com.example.adshield.ui.theme.AdShieldTheme

@Composable
fun CyberTopList(
    title: String,
    data: Map<String, Int>,
    onAllowClick: (String) -> Unit,
    isWhitelisted: (String) -> Boolean, // Logic to check status
    onSettingsClick: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val packageManager = remember(context) { context.packageManager }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                AdShieldTheme.shapes.container
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (onSettingsClick != null) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onSettingsClick)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (data.isEmpty()) {
            Text(
                "NO DATA COLLECTED",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontStyle = FontStyle.Italic
            )
        } else {
            // Sort by count descending and take top 5
            val sorted = data.toList().sortedByDescending { (_, value) -> value }.take(5)

            sorted.forEach { (packageName, count) ->
                // Resolve App Info
                var appName by remember(packageName) { mutableStateOf(packageName) }
                var appIcon by remember(packageName) {
                    mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(
                        null
                    )
                }

                LaunchedEffect(packageName) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val appInfo = packageManager.getApplicationInfo(packageName, 0)
                            val label = packageManager.getApplicationLabel(appInfo).toString()
                            val iconDrawable = packageManager.getApplicationIcon(appInfo)
                            val bitmap =
                                iconDrawable.toBitmap(width = 64, height = 64).asImageBitmap()

                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                appName = label
                                appIcon = bitmap
                            }
                        } catch (e: Exception) {
                            // Keep default
                        }
                    }
                }

                val isActive = isWhitelisted(packageName)
                val statusIcon = Icons.Filled.Lock
                val tint =
                    if (isActive) androidx.compose.ui.graphics.Color(0xFFFF5252) else com.example.adshield.ui.theme.NeonGreen
                val bgBorder =
                    if (isActive) androidx.compose.ui.graphics.Color(0xFFFF5252)
                        .copy(alpha = 0.5f) else com.example.adshield.ui.theme.NeonGreen.copy(alpha = 0.5f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Icon
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon!!,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(AdShieldTheme.shapes.icon)
                        )
                    } else {
                        // Fallback Icon
                        Icon(
                            imageVector = Icons.Default.CheckCircle, // Generic
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appName, // UPDATED: Shows Human Name
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isActive) com.example.adshield.ui.theme.NeonGreen else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        // Optional: Show package name below in tiny font
                        if (appName != packageName) {
                            Text(
                                text = packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(Modifier.width(8.dp))
                    // Tiny toggle button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onAllowClick(packageName) }
                            .border(1.dp, bgBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            statusIcon,
                            contentDescription = "Toggle",
                            tint = tint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}
