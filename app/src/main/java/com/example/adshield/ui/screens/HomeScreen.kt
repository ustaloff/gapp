package com.example.adshield.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.data.VpnLogEntry
import com.example.adshield.data.VpnStats
import com.example.adshield.filter.FilterEngine
import com.example.adshield.ui.components.*
import com.example.adshield.ui.theme.AdShieldTheme

@Composable
fun HomeView(
    isRunning: Boolean,
    blockedCount: Int,
    bpm: Int,
    filterCount: Int,
    dataSaved: Long,
    recentLogs: List<VpnLogEntry>,
    excludedApps: Set<String>, // Added state
    filterUpdateTrigger: Long, // Added trigger for Domain/Filter UI refresh
    isUpdatingFilters: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onWhitelistClick: () -> Unit,
    onReloadFilters: () -> Unit,
    onLogClick: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onDomainManagerClick: () -> Unit // Added callback
) {
    val scrollState = rememberScrollState()

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
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // CENTERED HEADER (Logo + Title)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                CyberLogo(
                    modifier = Modifier.size(48.dp),
                    size = 48.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                GlitchText(
                    text = "ADSHIELD",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .border(
                        1.dp,
                        if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "TUNNELING ACTIVE // IP MASKED" else "PROTECTION DISABLED // EXPOSED",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STATUS SECTION (Redsigned)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column {
                    // Stats Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CyberStatCard(
                            label = "DATA SAVED",
                            value = formatBytes(dataSaved),
                            progress = (dataSaved / (100 * 1024 * 1024f)).coerceIn(0.01f, 1f),
                            iconVector = androidx.compose.material.icons.Icons.Default.ThumbUp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
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
                            progress = (timeMs / (5 * 60 * 1000f)).coerceIn(0.01f, 1f),
                            progressSegments = 3,
                            iconVector = androidx.compose.material.icons.Icons.Default.Speed,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            // PRIORITY 3: LIVE TRAFFIC GRAPH (Trust)
            Spacer(modifier = Modifier.height(24.dp))
            CyberGraphSection(VpnStats.blockedHistory, bpm, isRunning)

            Spacer(modifier = Modifier.height(24.dp))

            // PRIORITY 4: BLOCKED STATS (History)
            // BLOCKED REQUESTS HEADER
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.fillMaxWidth()) {
                    CyberTopList(
                        title = "TOP APPS",
                        data = VpnStats.appBlockedStatsMap,
                        onAllowClick = { onAppClick(it) },
                        isWhitelisted = { pkg -> excludedApps.contains(pkg) }, // Use State
                        onSettingsClick = onWhitelistClick // Go to App Whitelist
                    )
                }
                Box(Modifier.fillMaxWidth()) {
                    CyberTopList(
                        title = "TOP DOMAINS",
                        data = VpnStats.domainBlockedStatsMap,
                        onAllowClick = { onLogClick(it) },
                        isWhitelisted = { domain ->
                            val tick = filterUpdateTrigger // Force Recomp reading
                            FilterEngine.checkDomain(domain) == FilterEngine.FilterStatus.ALLOWED_USER
                        },
                        onSettingsClick = onDomainManagerClick // Go to Domain Manager
                    )
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
}

// Utils (moved helper here or make it public in MainActivity/Utils)
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format(
        java.util.Locale.US,
        "%.1f %cB",
        bytes / Math.pow(1024.0, exp.toDouble()),
        pre
    )
}
