package com.example.adshield

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.adshield.data.VpnStats
import com.example.adshield.data.VpnLogEntry
import com.example.adshield.data.FilterRepository
import com.example.adshield.filter.FilterEngine
import com.example.adshield.service.LocalVpnService
import com.example.adshield.ui.theme.AdShieldTheme
import com.example.adshield.ui.components.* // Import CyberComponents
import androidx.compose.ui.graphics.Path
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        requestVpnPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.adshield.filter.FilterEngine.initialize(this)
        setContent {
            AdShieldTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("dashboard") }

                    if (currentScreen == "dashboard") {
                        DashboardScreen(
                            onStartClick = { checkPermissionsAndStart() },
                            onStopClick = { stopVpnService() },
                            onWhitelistClick = { currentScreen = "whitelist" }
                        )
                    } else if (currentScreen == "whitelist") {
                        com.example.adshield.ui.AppListScreen(
                            onBackClick = { currentScreen = "dashboard" }
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestVpnPermission()
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, LocalVpnService::class.java).apply {
            action = LocalVpnService.ACTION_START
        }
        startService(intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, LocalVpnService::class.java).apply {
            action = LocalVpnService.ACTION_STOP
        }
        startService(intent)
    }
}

@Composable
fun DashboardScreen(
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onWhitelistClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isRunning = VpnStats.isRunning.value
    val blockedCount = VpnStats.blockedCount.value
    val totalCount = VpnStats.totalCount.value
    val recentLogs = VpnStats.recentLogs
    val dataSaved = VpnStats.dataSavedBytes.value
    val bpm = VpnStats.blocksPerMinute.value
    
    var filterCount by remember { mutableStateOf(FilterEngine.getRuleCount()) }
    var isUpdatingFilters by remember { mutableStateOf(false) }
    var showWhitelistDialog by remember { mutableStateOf(false) }
    var domainToWhitelist by remember { mutableStateOf("") }
    
    var hasAcceptedDisclosure by remember { mutableStateOf(false) }
    var showDisclosureDialog by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

    // Load states on startup
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("adshield_prefs", android.content.Context.MODE_PRIVATE)
        hasAcceptedDisclosure = prefs.getBoolean("disclosure_accepted", false)
        
        // Initialize Persistent Stats
        VpnStats.initialize(context)
        
        if (filterCount < 100) {
            isUpdatingFilters = true
            delay(1000)
            withContext(Dispatchers.IO) {
                val filterData = FilterRepository.downloadAndParseFilters()
                if (filterData.blockRules.isNotEmpty()) {
                    FilterEngine.updateBlocklist(filterData)
                }
            }
            filterCount = FilterEngine.getRuleCount()
            isUpdatingFilters = false
        }
    }

    // Dialogs (kept standard for now, but could be stylized)
    if (showWhitelistDialog) {
        AlertDialog(
            onDismissRequest = { showWhitelistDialog = false },
            title = { Text("WHITELIST DOMAIN", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = { Text("Allow traffic for '$domainToWhitelist'?", color = MaterialTheme.colorScheme.onSurface) },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                TextButton(onClick = {
                    FilterEngine.addToAllowlist(context, domainToWhitelist)
                    showWhitelistDialog = false
                }) { Text("ALLOW", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showWhitelistDialog = false }) { Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    if (showDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showDisclosureDialog = false },
            title = { Text("SYSTEM DISCLOSURE") },
            text = { Text("Local VPN required for packet inspection.\nData remains local.\nProceed?") },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Button(
                    onClick = {
                        hasAcceptedDisclosure = true
                        context.getSharedPreferences("adshield_prefs", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("disclosure_accepted", true).apply()
                        showDisclosureDialog = false
                        onStartClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("INITIALIZE", color = MaterialTheme.colorScheme.onPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosureDialog = false }) { Text("ABORT", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    // MAIN CYBER UI CONTAINER
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GridBackground()
        // Optional: Scanline(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))

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
                        style = MaterialTheme.typography.titleSmall,
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

            // HERO SECTION (Unified Control)
            Spacer(modifier = Modifier.height(1.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CyberPowerButton(
                    isRunning = isRunning,
                    onClick = {
                         if (isRunning) onStopClick() 
                         else if (!hasAcceptedDisclosure) showDisclosureDialog = true 
                         else onStartClick()
                    }
                )
            }

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
                    value = formatBytes(dataSaved),
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
                    CyberTopList("TOP APPS", VpnStats.appBlockedStatsMap, onAllowClick = { /* No-op for now */ })
                }
                Box(Modifier.weight(1f)) {
                    CyberTopList("TOP DOMAINS", VpnStats.domainBlockedStatsMap, onAllowClick = { domainToWhitelist = it; showWhitelistDialog = true })
                }
            }

            // PRORITY 6: TERMINAL LOG (Deep Details)
            Spacer(modifier = Modifier.height(24.dp))
            CyberTerminal(logs = recentLogs, onLogClick = { domainToWhitelist = it; showWhitelistDialog = true })

            // PRIORITY 7: PROTECTION ENGINE (Technical Footer)
            Spacer(modifier = Modifier.height(24.dp))
            CyberFilterCard(
                ruleCount = filterCount,
                isUpdating = isUpdatingFilters,
                onReload = {
                     if (!isUpdatingFilters) {
                         isUpdatingFilters = true
                         scope.launch {
                             withContext(Dispatchers.IO) {
                                 val filterData = FilterRepository.downloadAndParseFilters()
                                 if (filterData.blockRules.isNotEmpty()) {
                                     FilterEngine.updateBlocklist(filterData)
                                 }
                             }
                             val newCount = FilterEngine.getRuleCount()
                             filterCount = newCount
                             isUpdatingFilters = false
                         }
                     }
                }
            )


            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun CyberGraphSection(data: List<Int>, bpm: Int, isRunning: Boolean) {
     val primaryColor = MaterialTheme.colorScheme.primary
     val offlineColor = MaterialTheme.colorScheme.error // Or Gray
     
     val infiniteTransition = rememberInfiniteTransition(label = "monitoring_pulse")
     val pulseAlpha by if (isRunning) {
         infiniteTransition.animateFloat(
             initialValue = 0.4f,
             targetValue = 1f,
             animationSpec = infiniteRepeatable(
                 animation = tween(1000, easing = LinearEasing),
                 repeatMode = RepeatMode.Reverse
             ),
             label = "pulse_alpha"
         )
     } else { remember { mutableFloatStateOf(1f) } } // Static when offline

     // Threat Logic (Force Low/Gray if offline)
     val (level, threatColor) = when {
        !isRunning -> "OFFLINE" to offlineColor.copy(alpha=0.5f)
        bpm > 20 -> "HIGH" to Color(0xFFFF5252) // Red
        bpm > 5 -> "MED" to Color(0xFFFFAB40) // Orange
        else -> "LOW" to primaryColor // Green/Primary
    }
    val progress = if (!isRunning) 0f else when {
        bpm > 30 -> 1f
        else -> (bpm / 30f).coerceIn(0.05f, 1f)
    }

     Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, (if (isRunning) primaryColor else offlineColor).copy(alpha = 0.2f), RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(16.dp)
     ) {
         // HUD Header
         Row(
             modifier = Modifier.fillMaxWidth(), 
             horizontalArrangement = Arrangement.SpaceBetween,
             verticalAlignment = Alignment.CenterVertically
         ) {
             // Left: Title
             Row(verticalAlignment = Alignment.CenterVertically) {
                 Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isRunning) threatColor.copy(alpha = pulseAlpha) else offlineColor, CircleShape)
                 )
                 Spacer(Modifier.width(8.dp))
                 Text(
                     text = if (isRunning) "TRAFFIC ANALYSIS // LIVE" else "TRAFFIC ANALYSIS // OFFLINE", 
                     style = MaterialTheme.typography.labelSmall, 
                     fontWeight = FontWeight.Bold, 
                     color = if (isRunning) primaryColor else offlineColor,
                     fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                     letterSpacing = 1.sp
                 )
             }
         }
         Spacer(Modifier.height(16.dp))
         
         // Canvas Graph
         Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp) // Taller graph
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                .border(1.dp, (if (isRunning) primaryColor else offlineColor).copy(alpha = 0.1f), RoundedCornerShape(5.dp))
         ) {
             Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp)) {
                 val width = size.width
                 val height = size.height
                 // Use all 60 points or whatever is available, but limit if needed
                 val graphData = if (data.isEmpty()) List(60) { 0 } else data
                 val max = (graphData.maxOrNull() ?: 5).coerceAtLeast(5).toFloat()
                 val graphColor = if (isRunning) primaryColor else offlineColor.copy(alpha=0.3f)
                 
                 // Draw Grid
                 val verticalLines = 6 // roughly every 10 mins
                 val horizontalLines = 4
                 
                 for (i in 1 until verticalLines) {
                     val x = (width / verticalLines) * i
                     drawLine(
                         color = graphColor.copy(alpha = 0.05f),
                         start = Offset(x, 0f),
                         end = Offset(x, height),
                         strokeWidth = 1f
                     )
                 }
                 
                 for (i in 1 until horizontalLines) {
                     val y = (height / horizontalLines) * i
                     drawLine(
                         color = graphColor.copy(alpha = 0.05f),
                         start = Offset(0f, y),
                         end = Offset(width, y),
                         strokeWidth = 1f
                     )
                 }

                 // Draw Path (Smooth Bezier) -- ONLY IF RUNNING OR DATA EXISTS
                 if (isRunning && graphData.isNotEmpty() && graphData.any { it > 0 }) {
                     val path = Path()
                     val stepX = width / (graphData.size - 1).coerceAtLeast(1)
                     
                     // Move to first point
                     val firstY = height - ((graphData[0] / max) * height)
                     path.moveTo(0f, firstY)

                     for (i in 0 until graphData.size - 1) {
                         val x1 = i * stepX
                         val y1 = height - ((graphData[i] / max) * height)
                         val x2 = (i + 1) * stepX
                         val y2 = height - ((graphData[i + 1] / max) * height)

                         // Control points for smooth curve
                         val cx1 = (x1 + x2) / 2
                         val cy1 = y1
                         val cx2 = (x1 + x2) / 2
                         val cy2 = y2

                         path.cubicTo(cx1, cy1, cx2, cy2, x2, y2)
                     }
                     
                     // Draw Line
                     drawPath(
                         path = path,
                         color = primaryColor,
                         style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                     )
                     
                     // Draw Gradient Fill
                     path.lineTo(width, height)
                     path.lineTo(0f, height)
                     path.close()
                     
                     drawPath(
                         path = path,
                         brush = Brush.verticalGradient(
                             colors = listOf(
                                 primaryColor.copy(alpha = 0.3f),
                                 primaryColor.copy(alpha = 0.0f)
                             ),
                             startY = 0f,
                             endY = height
                         )
                     )
                 } else if (!isRunning) {
                     // Draw Flat Line or Static Noise if offline?
                     // Let's just draw a flat line at the bottom
                     drawLine(
                         color = offlineColor.copy(alpha = 0.3f),
                         start = Offset(0f, height),
                         end = Offset(width, height),
                         strokeWidth = 2.dp.toPx()
                     )
                 }
             }
             // Scanline Overlay
             //if (isRunning) {
                 Scanline(
                     modifier = Modifier.fillMaxSize(),
                     color = primaryColor.copy(alpha = 0.01f)
                 )
             //}
         }
         
         // Time Labels
         Spacer(Modifier.height(8.dp))
         Row(
             modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
             horizontalArrangement = Arrangement.SpaceBetween
         ) {
              Text("-60m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 10.sp)
              Text("-45m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), fontSize = 10.sp)
              Text("-30m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 10.sp)
              Text("-15m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), fontSize = 10.sp)
              Text("NOW", style = MaterialTheme.typography.labelSmall, color = if (isRunning) primaryColor else offlineColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
         }

         Spacer(Modifier.height(12.dp))
         
         // Threat Indicator Bar
         Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(threatColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
         ) {
             Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(threatColor, RoundedCornerShape(2.dp))
             )
         }
         
         Spacer(Modifier.height(8.dp))

         // Footer: Threat Level & BPM
         Row(
             modifier = Modifier.fillMaxWidth(),
             horizontalArrangement = Arrangement.SpaceBetween,
             verticalAlignment = Alignment.CenterVertically
         ) {
             // Bottom Left: Threat Logic
             Text(
                 text = if (isRunning) "THREAT: $level" else "SYSTEM: STANDBY", 
                 color = threatColor, 
                 fontWeight = FontWeight.Bold, 
                 fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, 
                 fontSize = 12.sp,
                 letterSpacing = 1.sp
             )
             
             // Bottom Right: BPM
             Text(
                 text = if (isRunning) "ACT :: $bpm/MIN" else "ACT :: ---", 
                 color = MaterialTheme.colorScheme.onSurfaceVariant, 
                 fontWeight = FontWeight.Bold, 
                 fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, 
                 fontSize = 12.sp,
                 letterSpacing = 1.sp
             )
         }
     }
}

@Composable
fun CyberTerminal(logs: List<VpnLogEntry>, onLogClick: (String) -> Unit) {
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

    // Auto-scroll state
    var autoScrollEnabled by remember { mutableStateOf(true) }
    val listState = rememberScrollState()

    // Auto-scroll logic
    LaunchedEffect(logs.firstOrNull(), autoScrollEnabled) {
        if (autoScrollEnabled && logs.isNotEmpty()) {
            listState.animateScrollTo(listState.maxValue)
        }
    }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("EVENT LOG", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "AUTO-SCROLL: ${if(autoScrollEnabled) "ON" else "OFF"}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = if(autoScrollEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { autoScrollEnabled = !autoScrollEnabled }
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant) // Terminal BG
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                //.padding(8.dp)
        ) {
             // Basic list
             Column(modifier = Modifier.verticalScroll(listState).padding(8.dp)) {
                 if (logs.isEmpty()) {
                     Text("> Initializing system...", color = MaterialTheme.colorScheme.primary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                     Text("> Waiting for traffic...", color = Color.Gray, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                 }
                 logs.reversed().forEach { log ->
                     Row(modifier = Modifier.clickable { onLogClick(log.domain) }.padding(vertical = 2.dp)) {
                         Text("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(log.timestamp))}] ", color = Color.Gray, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                         Text(
                             text = if (log.isBlocked) "BLOCKED: ${log.domain}" else "ALLOWED: ${log.domain}",
                             color = if (log.isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                             fontSize = 12.sp,
                             fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                             maxLines = 1,
                             overflow = TextOverflow.Ellipsis
                         )
                     }
                 }
                 // Blinking Cursor
                 Box(Modifier.size(8.dp, 14.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha)))
             }
             Scanline(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f))
        }
    }
}



fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format(java.util.Locale.US, "%.1f %cB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}


