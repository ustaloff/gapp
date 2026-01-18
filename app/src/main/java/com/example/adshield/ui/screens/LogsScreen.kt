package com.example.adshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.data.VpnLogEntry
import com.example.adshield.ui.components.CyberChip
import com.example.adshield.ui.components.CyberTextField
import com.example.adshield.ui.components.GridBackground
import java.text.SimpleDateFormat
import java.util.Locale

import com.example.adshield.filter.FilterEngine
import com.example.adshield.ui.components.getLogStyle
import com.example.adshield.data.UserAccessState
import com.example.adshield.ui.components.UiAccessState
import com.example.adshield.ui.components.applyAccessState

import com.example.adshield.ui.components.LockedContainer

enum class LogTab {
    DOMAINS, APPS
}

@Composable
fun LogsView(
    logs: List<VpnLogEntry>,
    onLogClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onPremiumClick: () -> Unit,
    userAccessState: UserAccessState = UserAccessState.FREE
) {
    val isFree = userAccessState.isFree()
    var searchQuery by remember { mutableStateOf("") }
    // Tab state removed -> Universal List

    val filteredLogs by remember(logs, searchQuery) {
        derivedStateOf {
            logs.filter { entry ->
                // Universal Search
                if (searchQuery.isNotEmpty()) {
                    entry.domain.contains(searchQuery, ignoreCase = true) ||
                            (entry.appName?.contains(searchQuery, ignoreCase = true) == true)
                } else true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(32.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.shapes.small
                        )
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.shapes.small
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))

                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = "System logs",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SYSTEM LOGS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "TRAFFIC MONITORING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Search Bar - STANDARD for everyone (Input allowed, but results locked via List)
            CyberTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search query...",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // Log List
            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (logs.isEmpty()) "NO TRAFFIC DETECTED" else "NO MATCHES FOUND",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            } else {
                // Log List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredLogs) { entry ->
                        LogItemCard(entry, onLogClick)
                    }
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LogItemCard(entry: VpnLogEntry, onClick: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timestamp = timeFormat.format(java.util.Date(entry.timestamp))

    val style = getLogStyle(entry.status)

    // Resolve App Icon
    val appIcon = remember(entry.appName) {
        entry.appName?.let { pkg ->
            try {
                context.packageManager.getApplicationIcon(pkg)
            } catch (e: Exception) {
                null
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                MaterialTheme.shapes.small
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                MaterialTheme.shapes.small
            )
            .clickable(enabled = style.isClickable) { onClick(entry.domain) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(30.dp)
                .background(style.color, MaterialTheme.shapes.extraSmall)
        )
        Spacer(Modifier.width(12.dp))

        // App Icon (if available)
        if (appIcon != null) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                    }
                },
                update = { view ->
                    view.setImageDrawable(appIcon)
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Spacer(Modifier.width(8.dp))

                // Prefix [SYS], [BLK], etc.
                Text(
                    text = "[${style.prefix}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = style.color,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                if (entry.appName != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = entry.appName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.domain,
                style = MaterialTheme.typography.bodyMedium,
                color = if (style.isClickable) MaterialTheme.colorScheme.onSurface else Color.Gray,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
