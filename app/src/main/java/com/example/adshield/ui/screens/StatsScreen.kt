package com.example.adshield.ui.screens

import java.util.Locale
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import com.example.adshield.data.VpnStats
import com.example.adshield.ui.components.CyberGraphSection
import com.example.adshield.ui.components.CyberStatCard
import com.example.adshield.ui.components.GridBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.adshield.ui.theme.ContentDescriptions

// Helper Data Class for Top Offenders
data class AppStatItem(
    val packageName: String,
    val appName: String,
    val blockCount: Int,
    val icon: Drawable?
)

@Composable
fun StatsView(
    data: List<Int>,
    bpm: Int,
    isRunning: Boolean,
    excludedApps: Set<String>, // Hoisted State
    effectiveWhitelist: Set<String>, // Hoisted State
    onAllowClick: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    // We still need repository ONLY for resolving names for Top Offenders list if we want, or do it here. 
    // Ideally repository handles data fetching.
    // The previous implementation instantiated repository just for effective whitelist AND name resolution?
    // Looking at previous code... yes it fetched names in LaunchedEffect.
    // So we keep repository for name resolution but NOT for whitelist logic.

    val appsRepository = remember { com.example.adshield.data.AppsRepository(context) }
    // val preferences = remember { com.example.adshield.data.AppPreferences(context) } // No longer needed for internal state

    // Top Offenders State
    var topApps by remember { mutableStateOf<List<AppStatItem>>(emptyList()) }

    // Removed internal UserAccess observation, as effective whitelist is passed in.

    // ... (rest of code)

    // Ensure we trigger name resolution when blocked stats change
    val updateTrigger = VpnStats.blockedCount.value
    LaunchedEffect(updateTrigger) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val rawMap = VpnStats.appBlockedStatsMap.toMap()
            val sorted = rawMap.entries.sortedByDescending { it.value }.take(10)

            val result = sorted.map { entry ->
                var name = entry.key
                var icon: Drawable? = null
                try {
                    val appInfo = pm.getApplicationInfo(entry.key, 0)
                    name = pm.getApplicationLabel(appInfo).toString()
                    icon = pm.getApplicationIcon(appInfo)
                } catch (_: Exception) {
                    // Packet name fallback
                }
                AppStatItem(entry.key, name, entry.value, icon)
            }
            topApps = result
        }
    }

    // START RESTORE VARIABLES
    // Convert bytes to readable string
    val dataSavedBytes = VpnStats.dataSavedBytes.value
    val dataSavedStr = remember(dataSavedBytes) {
        if (dataSavedBytes > 1024 * 1024) String.format(
            Locale.US,
            "%.1f MB",
            dataSavedBytes / (1024f * 1024f)
        )
        else String.format(Locale.US, "%.1f KB", dataSavedBytes / 1024f)
    }

    // Convert ms to readable time (Seconds/Minutes)
    val timeSavedMs = VpnStats.timeSavedMs.value
    val timeSavedStr = remember(timeSavedMs) {
        if (timeSavedMs > 60000) String.format(Locale.US, "%d MIN", timeSavedMs / 60000)
        else String.format(Locale.US, "%d SEC", timeSavedMs / 1000)
    }
    // END RESTORE VARIABLES

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
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Network Analytics",
                    tint = MaterialTheme.colorScheme.primary
                )
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

            // 1. Live Graph
            CyberGraphSection(data, VpnStats.totalHistory, bpm, isRunning)

            Spacer(Modifier.height(24.dp))

            // 2. Traffic Saver Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CyberStatCard(
                    label = "DATA SAVED",
                    value = dataSavedStr,
                    progress = (dataSavedBytes / (100 * 1024 * 1024f)).coerceIn(0.01f, 1f),
                    progressSegments = 1,
                    iconVector = Icons.Default.ThumbUp,
                    modifier = Modifier.weight(1f)
                )
                CyberStatCard(
                    label = "TIME SAVED",
                    value = timeSavedStr,
                    progress = (timeSavedMs / (5 * 60 * 1000f)).coerceIn(0.01f, 1f),
                    progressSegments = 3, // Match HomeScreen's 3 segments (was 5)
                    iconVector = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(32.dp))

            // 3. Top Offenders
            Text(
                text = "TOP OFFENDERS // MOST BLOCKED",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(16.dp))

            if (topApps.isEmpty()) {
                Text(
                    text = "> WAITING FOR TRAFFIC...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topApps.forEach { app ->
                        OffenderItem(
                            app = app,
                            isExcluded = excludedApps.contains(app.packageName),
                            isEffective = effectiveWhitelist.contains(app.packageName),
                            onToggle = {
                                val isCurrentlyExcluded = excludedApps.contains(app.packageName)
                                if (isCurrentlyExcluded) {
                                    // Remove
                                    onAllowClick(app.packageName, false)
                                } else {
                                    // Add
                                    onAllowClick(app.packageName, true)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(130.dp))
        }
    }
}

@Composable
fun OffenderItem(
    app: AppStatItem,
    isExcluded: Boolean,
    isEffective: Boolean,
    onToggle: () -> Unit
) {
    val isActive = isExcluded && isEffective
    val isOverflow = isExcluded && !isEffective

    val statusIcon = if (isExcluded) Icons.Filled.CheckCircle else Icons.Filled.Lock

    val tint = when {
        isActive -> MaterialTheme.colorScheme.primary // Green
        isOverflow -> MaterialTheme.colorScheme.onSurfaceVariant // Grey
        else -> MaterialTheme.colorScheme.error // Red
    }

    val bgBorder = tint.copy(alpha = 0.5f)

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
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon
        if (app.icon != null) {
            val imageBitmap = remember(app.icon) {
                app.icon.toBitmap().asImageBitmap()
            }
            Image(
                bitmap = imageBitmap,
                contentDescription = ContentDescriptions.appIcon,
                modifier = Modifier
                    .size(40.dp)
                    .alpha(if (isOverflow) 0.5f else 1f), // Dim if overflow
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), MaterialTheme.shapes.small)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (isOverflow) 0.5f else 1f
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // Toggle Button (Lock/Unlock)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clickable { onToggle() }
                .border(1.dp, bgBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                statusIcon,
                contentDescription = "Toggle",
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


