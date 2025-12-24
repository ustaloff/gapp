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
import com.example.adshield.data.AppPreferences
import com.example.adshield.ui.* // Import Screens
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
                            onWhitelistClick = { currentScreen = "whitelist" },
                            onWhitelistApp = { packageName ->
                                val prefs = AppPreferences(this@MainActivity)
                                if (!prefs.getExcludedApps().contains(packageName)) {
                                    prefs.addExcludedApp(packageName)
                                    android.widget.Toast.makeText(this@MainActivity, "Whitelisted: $packageName", android.widget.Toast.LENGTH_SHORT).show()
                                    if (VpnStats.isRunning.value) { // Use State directly or track locally? VpnStats is global object
                                         stopVpnService()
                                         // Restart with delay
                                         kotlinx.coroutines.GlobalScope.launch { // Or use lifecycle scope if available
                                             kotlinx.coroutines.delay(500)
                                             startVpnService()
                                         }
                                    }
                                }
                            }
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
    onWhitelistClick: () -> Unit,
    onWhitelistApp: (String) -> Unit
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
    
    // State for Navigation
    var currentScreen by remember { mutableStateOf("HOME") }
    
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

        // Content Switcher
        when (currentScreen) {
            "HOME" -> HomeView(
                isRunning = isRunning,
                blockedCount = blockedCount,
                bpm = bpm,
                filterCount = filterCount,
                dataSaved = dataSaved,
                recentLogs = recentLogs, // Takes top 5 anyway? HomeView logic handles display
                isUpdatingFilters = isUpdatingFilters,
                onStartClick = onStartClick,
                onStopClick = onStopClick,
                onWhitelistClick = onWhitelistClick,
                onReloadFilters = { 
                     // Reload logic (moved from inline)
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
                },
                onLogClick = { domainToWhitelist = it; showWhitelistDialog = true },
                onAppClick = { packageName -> onWhitelistApp(packageName) }
            )
            "LOGS" -> LogsView(
                 logs = recentLogs, // Or full logs if available? reusing recentLogs for now
                 onLogClick = { domainToWhitelist = it; showWhitelistDialog = true }
            )
            "STATS" -> StatsView(
                 data = VpnStats.blockedHistory,
                 bpm = bpm,
                 isRunning = isRunning
            )
            "SETTINGS" -> SettingsView(
                 onWhitelistClick = onWhitelistClick
            )
            else -> HomeView(
                isRunning = isRunning,
                blockedCount = blockedCount,
                bpm = bpm,
                filterCount = filterCount,
                dataSaved = dataSaved,
                recentLogs = recentLogs,
                isUpdatingFilters = isUpdatingFilters,
                onStartClick = onStartClick,
                onStopClick = onStopClick,
                onWhitelistClick = onWhitelistClick,
                onReloadFilters = {},
                onLogClick = { domainToWhitelist = it; showWhitelistDialog = true },
                onAppClick = { packageName -> onWhitelistApp(packageName) }
            )
        }

        // FLOATING NAV BAR OVERYLAY
        CyberNavBar(
            isRunning = isRunning,
            onPowerClick = {
                 if (isRunning) onStopClick() 
                 else if (!hasAcceptedDisclosure) showDisclosureDialog = true 
                 else onStartClick()
            },
            currentScreen = currentScreen,
            onNavigate = { currentScreen = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
