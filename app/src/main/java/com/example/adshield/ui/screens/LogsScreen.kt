package com.example.adshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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

@Composable
fun LogsView(
    logs: List<VpnLogEntry>,
    onLogClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, BLOCKED, ALLOWED

    val filteredLogs by remember(logs, searchQuery, filterType) {
        derivedStateOf {
            logs.filter { entry ->
                val isBlocked = entry.status == FilterEngine.FilterStatus.BLOCKED ||
                        entry.status == FilterEngine.FilterStatus.BLOCKED_USER

                val matchesSearch = entry.domain.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (filterType) {
                    "BLOCKED" -> isBlocked
                    "ALLOWED" -> !isBlocked
                    else -> true
                }
                matchesSearch && matchesFilter
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
                .padding(top = 16.dp, bottom = 100.dp) // Bottom padding for nav bar + spacing
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SYSTEM LOGS // DEEP DIVE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
            }
            Spacer(Modifier.height(16.dp))

            // Search Bar
            CyberTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search domain...",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CyberChip(
                    text = "ALL",
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    modifier = Modifier.weight(1f)
                )
                CyberChip(
                    text = "BLOCKED",
                    selected = filterType == "BLOCKED",
                    onClick = { filterType = "BLOCKED" },
                    modifier = Modifier.weight(1f)
                )
                CyberChip(
                    text = "ALLOWED",
                    selected = filterType == "ALLOWED",
                    onClick = { filterType = "ALLOWED" },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(16.dp))

            // Log List
            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (logs.isEmpty()) "NO TRAFFIC DETECTED" else "NO MATCHES FOUND",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Show newest first if list is chronological (assuming logs are appended)
                    // If logs is already reversed, remove .reversed()
                    // Usually logs are appended, so last is newest.
                    items(filteredLogs.reversed()) { entry ->
                        LogItemCard(entry, onLogClick)
                    }
                }
            }
        }
    }
}

@Composable
fun LogItemCard(entry: VpnLogEntry, onClick: (String) -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timestamp = timeFormat.format(java.util.Date(entry.timestamp))

    val style = getLogStyle(entry.status)

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

                Spacer(Modifier.width(8.dp))

                // Human Readable Status
                Text(
                    text = if (style.prefix == "BLK" || style.prefix == "BAN") "BLOCKED" else "ALLOWED",
                    style = MaterialTheme.typography.labelSmall,
                    color = style.color.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
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
