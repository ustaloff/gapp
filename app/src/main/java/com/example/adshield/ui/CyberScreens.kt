package com.example.adshield.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import com.example.adshield.data.VpnStats
import com.example.adshield.data.VpnLogEntry
import com.example.adshield.ui.components.*

@Composable
fun HomeView(
    isRunning: Boolean,
    blockedCount: Int,
    bpm: Int,
    filterCount: Int,
    dataSaved: Long,
    recentLogs: List<VpnLogEntry>,
    isUpdatingFilters: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onWhitelistClick: () -> Unit,
    onReloadFilters: () -> Unit,
    onLogClick: (String) -> Unit,
    onAppClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP BAR
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.adshield.R.drawable.ic_app_logo_final),
                contentDescription = "AdShield Logo",
                modifier = Modifier.size(48.dp)
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isRunning) "ADSHIELD ACTIVE" else "ADSHIELD PAUSED",
                    style = MaterialTheme.typography.titleSmall, // Requires Material3
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = if (isRunning) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), 
                                CircleShape
                            )
                    )
                    Text(
                        text = if (isRunning) "TUNNELING ACTIVE // IP MASKED" else "PROTECTION DISABLED",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp, 
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, 
                        color = if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            IconButton(onClick = onWhitelistClick) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PRIORITY 2: DATA & TIME SAVED (Rewards)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max), // Force equal height
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CyberStatCard(
                label = "DATA SAVED",
                value = formatBytes(dataSaved), // Need formatBytes available or import it
                progress = (dataSaved / (100 * 1024 * 1024f)).coerceIn(0.01f, 1f), // Goal: 100 MB
                iconVector = null,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            
            val timeMs = VpnStats.timeSavedMs.value
            val timeString = when {
                timeMs < 1000 -> "${timeMs}ms"
                timeMs < 60000 -> String.format("%.1fs", timeMs / 1000f)
                else -> "${timeMs / 60000}m"
            }

            CyberStatCard(
                label = "FASTER LOAD",
                value = timeString,
                progress = (timeMs / (5 * 60 * 1000f)).coerceIn(0.01f, 1f), // Goal: 5 Minutes
                progressSegments = 3,
                iconVector = androidx.compose.material.icons.Icons.Default.Refresh, 
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // PRIORITY 3: LIVE TRAFFIC GRAPH (Trust)
        Spacer(modifier = Modifier.height(24.dp))
        CyberGraphSection(VpnStats.blockedHistory, bpm, isRunning)

        Spacer(modifier = Modifier.height(32.dp))
        
        // PRIORITY 4: BLOCKED STATS (History) 
        // BLOCKED REQUESTS HEADER
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "BLOCKED REQUESTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // BLOCKED STATS GRID (Total, Today, 7 Days)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CyberStatCard(
                label = "TOTAL",
                value = blockedCount.toString(),
                modifier = Modifier.weight(1f)
            )
            CyberStatCard(
                label = "TODAY",
                value = VpnStats.blockedToday.value.toString(),
                modifier = Modifier.weight(1f)
            )
            CyberStatCard(
                label = "7 DAYS",
                value = VpnStats.blockedWeekly.value.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        
        // PRIORITY 5: TOP LISTS (Details)
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                CyberTopList("TOP APPS", VpnStats.appBlockedStatsMap, onAllowClick = { onAppClick(it) })
            }
            Box(Modifier.weight(1f)) {
                CyberTopList("TOP DOMAINS", VpnStats.domainBlockedStatsMap, onAllowClick = { onLogClick(it) })
            }
        }

        // PRORITY 6: TERMINAL LOG (Deep Details)
        Spacer(modifier = Modifier.height(24.dp))
        CyberTerminal(logs = recentLogs, onLogClick = { onLogClick(it) })

        // PRIORITY 7: PROTECTION ENGINE (Technical Footer)
        Spacer(modifier = Modifier.height(24.dp))
        CyberFilterCard(
            ruleCount = filterCount,
            isUpdating = isUpdatingFilters,
            onReload = onReloadFilters
        )
        
        Spacer(modifier = Modifier.height(150.dp))
    }
}

// Utils (moved helper here or make it public in MainActivity/Utils)
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format(java.util.Locale.US, "%.1f %cB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}

@Composable
fun LogsView(
    logs: List<VpnLogEntry>,
    onLogClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 150.dp) // Top padding + Bottom specific for logs
    ) {
         // Header
         Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "SYSTEM LOGS // LIVE FEED",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
        }
        Spacer(Modifier.height(16.dp))
        
        // Full Screen Terminal
        // We reuse CyberTerminal logic but expanded
        CyberTerminal(logs = logs, onLogClick = onLogClick)
        
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tap any entry to whitelist domain.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatsView(
    data: List<Int>,
    bpm: Int,
    isRunning: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
         Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "NETWORK ANALYTICS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
        }
        Spacer(Modifier.height(24.dp))
        
        CyberGraphSection(data, bpm, isRunning)
        
        Spacer(Modifier.height(24.dp))
        Text("> Detailed analysis modules loading...", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(150.dp))
    }
}

@Composable
fun SettingsView(
    onWhitelistClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
    ) {
         Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "SYSTEM CONFIGURATION",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
        }
        Spacer(Modifier.height(32.dp))
        
        // Config Item 1
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable(onClick = onWhitelistClick)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("WHITELIST MANAGEMENT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Manage allowed domains and apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        
         Spacer(modifier = Modifier.height(150.dp))
    }
}
