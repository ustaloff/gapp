package com.example.adshield.ui.screens

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.adshield.data.VpnStats
import com.example.adshield.ui.components.CyberGraphSection
import com.example.adshield.ui.components.CyberStatCard
import com.example.adshield.ui.components.GridBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    isRunning: Boolean
) {
    val context = LocalContext.current
    var topApps by remember { mutableStateOf<List<AppStatItem>>(emptyList()) }

    // Convert bytes to readable string
    val dataSavedBytes = VpnStats.dataSavedBytes.value
    val dataSavedStr = remember(dataSavedBytes) {
        if (dataSavedBytes > 1024 * 1024) String.format("%.1f MB", dataSavedBytes / (1024f * 1024f))
        else String.format("%.1f KB", dataSavedBytes / 1024f)
    }

    // Convert ms to readable time (Seconds/Minutes)
    val timeSavedMs = VpnStats.timeSavedMs.value
    val timeSavedStr = remember(timeSavedMs) {
        if (timeSavedMs > 60000) String.format("%d MIN", timeSavedMs / 60000)
        else String.format("%d SEC", timeSavedMs / 1000)
    }

    // Load Top Offenders Async
    val updateTrigger = VpnStats.blockedCount.value // Subscribe to updates
    LaunchedEffect(updateTrigger) { // Trigger when stats change
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val rawMap = VpnStats.appBlockedStatsMap.toMap() // Copy
            val sorted = rawMap.entries.sortedByDescending { it.value }.take(10)

            val result = sorted.map { entry ->
                var name = entry.key
                var icon: Drawable? = null
                try {
                    val appInfo = pm.getApplicationInfo(entry.key, 0)
                    name = pm.getApplicationLabel(appInfo).toString()
                    icon = pm.getApplicationIcon(appInfo)
                } catch (e: Exception) {
                    // Packet name fallback
                }
                AppStatItem(entry.key, name, entry.value, icon)
            }
            topApps = result
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
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
                    contentDescription = null,
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
            CyberGraphSection(data, bpm, isRunning)

            Spacer(Modifier.height(24.dp))

            // 2. Traffic Saver Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CyberStatCard(
                    label = "DATA SAVED",
                    value = dataSavedStr,
                    progress = 1.0f,
                    progressSegments = 1,
                    modifier = Modifier.weight(1f)
                )
                CyberStatCard(
                    label = "TIME SAVED",
                    value = timeSavedStr,
                    progress = 0.8f, // Decorative
                    progressSegments = 5,
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
                        OffenderItem(app)
                    }
                }
            }

            Spacer(modifier = Modifier.height(150.dp))
        }
    }
}

@Composable
fun OffenderItem(app: AppStatItem) {
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
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        setImageDrawable(app.icon)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                modifier = Modifier.size(40.dp)
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
                color = MaterialTheme.colorScheme.onSurface,
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

        // Badge
        Box(
            modifier = Modifier
                .background(Color(0xFFFF2A6D).copy(alpha = 0.2f), MaterialTheme.shapes.small)
                .border(1.dp, Color(0xFFFF2A6D).copy(alpha = 0.5f), MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${app.blockCount}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFF2A6D), // Cyber Pink
                fontWeight = FontWeight.Bold
            )
        }
    }
}
