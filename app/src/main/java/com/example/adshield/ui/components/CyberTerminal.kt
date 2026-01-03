package com.example.adshield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.example.adshield.data.VpnLogEntry
import com.example.adshield.ui.theme.AdShieldTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CyberTerminal(
    logs: List<VpnLogEntry>,
    onLogClick: (String) -> Unit
) {
    // 1. Cursor Animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    // 2. Auto-scroll
    val listState = rememberScrollState()
    var autoScrollEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(logs.size, autoScrollEnabled) {
        if (autoScrollEnabled && logs.isNotEmpty()) {
            listState.animateScrollTo(listState.maxValue)
        }
    }

    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        borderColor = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ... Header ...
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "[ TERMINAL SESSION ACTIVE ]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                // Auto-scroll toggle
                Text(
                    text = "AUTO-SCROLL: ${if (autoScrollEnabled) "ON" else "OFF"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = if (autoScrollEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { autoScrollEnabled = !autoScrollEnabled }
                )
            }

            Spacer(Modifier.height(8.dp))

            // SCREEN CONTAINER (Box for layering Scanline)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.extraSmall)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.shapes.extraSmall
                    )
                    .clip(MaterialTheme.shapes.extraSmall) // Clip content to shape
            ) {
                // USE COLUMN WITH VERTICAL SCROLL instead of LazyColumn for simple cursor appending at end
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(listState)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            "> Initializing system...",
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Text(
                            "> Waiting for traffic...",
                            color = Color.Gray,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    logs.reversed().forEach { log ->
                        // Determine Color and Label based on Status
                        val (color, prefix, isClickable) = when (log.status) {
                            com.example.adshield.filter.FilterEngine.FilterStatus.BLOCKED ->
                                Triple(MaterialTheme.colorScheme.error, "BLK", true) // Blocked
                            com.example.adshield.filter.FilterEngine.FilterStatus.BLOCKED_USER ->
                                Triple(
                                    AdShieldTheme.colors.warning,
                                    "BAN",
                                    true
                                ) // Orange (Manual Ban)
                            com.example.adshield.filter.FilterEngine.FilterStatus.ALLOWED_USER ->
                                Triple(
                                    MaterialTheme.colorScheme.primary,
                                    "USR",
                                    true
                                ) // User Allowed
                            com.example.adshield.filter.FilterEngine.FilterStatus.ALLOWED_SYSTEM ->
                                Triple(Color.Gray, "SYS", false) // Gray, Non-interactive
                            com.example.adshield.filter.FilterEngine.FilterStatus.SUSPICIOUS ->
                                Triple(
                                    AdShieldTheme.colors.warning,
                                    "WRN",
                                    true
                                ) // Yellow, Warning, Clickable
                            com.example.adshield.filter.FilterEngine.FilterStatus.ALLOWED_DEFAULT ->
                                Triple(Color.White, "ALW", true) // White, CAN BE BLOCKED
                        }

                        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

                        Row(
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .clickable(enabled = isClickable) { onLogClick(log.domain) }
                        ) {
                            Text(
                                text = "> ${timeFormat.format(Date(log.timestamp))} ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "[$prefix] ",
                                style = MaterialTheme.typography.bodySmall,
                                color = color,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = log.domain,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isClickable) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // BLINKING CURSOR
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(8.dp, 14.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha))
                    )
                }

                // Scanline Effect (Overlay only on screen)
                Scanline(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "[Tap any entry]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        }
    }
}
