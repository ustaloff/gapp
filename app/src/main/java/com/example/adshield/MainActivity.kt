package com.example.adshield

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.adshield.data.UserRepository
import com.example.adshield.ui.* // Import Screens
import com.example.adshield.ui.screens.* // Import Refactored Screens
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
        com.example.adshield.data.BillingManager.initialize(this)
        com.example.adshield.data.BillingManager.initialize(this)
        setContent {
            val context = LocalContext.current
            val prefs = remember { AppPreferences(context) }
            // Load initial theme from prefs
            var appTheme by remember { mutableStateOf(prefs.getAppTheme()) }

            AdShieldTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by rememberSaveable { mutableStateOf("splash") }
                    val user = com.example.adshield.data.UserRepository.getCurrentUser()

                    // -- TOAST STATE (Lifted to onCreate for access in callbacks) --
                    var toastVisible by remember { mutableStateOf(false) }
                    var toastMessage by remember { mutableStateOf("") }
                    var toastType by remember { mutableStateOf(CyberToastType.INFO) }

                    LaunchedEffect(Unit) {
                        // Always go to dashboard
                        if (currentScreen == "splash") {
                            currentScreen = "dashboard"
                        }
                    }

                    if (currentScreen == "dashboard") {
                        DashboardScreen(
                            onStartClick = { checkPermissionsAndStart() },
                            onStopClick = { stopVpnService() },
                            onWhitelistApp = { packageName ->
                                val prefs = AppPreferences(this@MainActivity)
                                val currentExcluded = prefs.getExcludedApps()
                                if (currentExcluded.contains(packageName)) {
                                    // WAS EXCLUDED -> NOW INCLUDE (PROTECT)
                                    prefs.removeExcludedApp(packageName)
                                    toastMessage = "PROTECTED: $packageName"
                                    toastType = CyberToastType.SUCCESS
                                    toastVisible = true
                                } else {
                                    // WAS INCLUDED -> NOW EXCLUDE (WHITELIST)
                                    prefs.addExcludedApp(packageName)
                                    toastMessage = "whitelisted: $packageName"
                                    toastType = CyberToastType.INFO // Info/Warning color
                                    toastVisible = true
                                }

                                // Restart VPN if running to apply changes
                                if (VpnStats.isRunning.value) {
                                    stopVpnService()
                                    lifecycleScope.launch {
                                        kotlinx.coroutines.delay(500)
                                        startVpnService()
                                    }
                                }
                            },
                            // Pass State
                            toastVisible = toastVisible,
                            toastMessage = toastMessage,
                            toastType = toastType,
                            onToastChange = { v, m, t ->
                                toastVisible = v
                                if (m != null) toastMessage = m
                                if (t != null) toastType = t
                            },
                            onThemeChange = {
                                appTheme = it
                                prefs.setAppTheme(it) // Save to Prefs
                            }
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
    onWhitelistApp: (String) -> Unit,
    // Toast Props
    toastVisible: Boolean,
    toastMessage: String,
    toastType: CyberToastType,
    onToastChange: (Boolean, String?, CyberToastType?) -> Unit,
    onThemeChange: (com.example.adshield.ui.theme.AppTheme) -> Unit
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

    val preferences = remember { AppPreferences(context) }

    var hasAcceptedDisclosure by remember { mutableStateOf(false) }
    var showDisclosureDialog by remember { mutableStateOf(false) }

    // State for Navigation
    var currentScreen by rememberSaveable { mutableStateOf("HOME") }
    // Back navigation stack
    var backStack by rememberSaveable { mutableStateOf(listOf<String>()) }

    // State for Whitelist (for UI Refresh)
    var excludedApps by remember { mutableStateOf(preferences.getExcludedApps()) }

    // State for Domain Filter Refresh
    var filterUpdateTrigger by remember { mutableLongStateOf(0L) }

    // Helper: Navigate forward (Push to stack)
    val navigateTo: (String) -> Unit = { target ->
        if (currentScreen != target) {
            backStack = backStack + currentScreen
            currentScreen = target
        }
    }

    // Helper: Navigate back (Pop from stack)
    val navigateBack: () -> Unit = {
        if (backStack.isNotEmpty()) {
            currentScreen = backStack.last()
            backStack = backStack.dropLast(1)
        } else {
            if (currentScreen != "HOME") {
                currentScreen = "HOME"
            }
        }
    }

    val scrollState = rememberScrollState()

    // Load states on startup
    LaunchedEffect(Unit) {
        val prefs =
            context.getSharedPreferences("adshield_prefs", android.content.Context.MODE_PRIVATE)
        hasAcceptedDisclosure = prefs.getBoolean("disclosure_accepted", false)

        // Initialize Persistent Stats
        VpnStats.initialize(context)

        if (filterCount < 100) {
            isUpdatingFilters = true
            delay(1000)
            withContext(Dispatchers.IO) {
                val filterData = FilterRepository.downloadAndParseFilters(context)
                if (filterData.blockRules.isNotEmpty()) {
                    FilterEngine.updateBlocklist(filterData)
                }
            }
            filterCount = FilterEngine.getRuleCount()
            isUpdatingFilters = false
        }
    }

    // Dialogs (kept standard for now, but could be stylized)
    // Auto-dismiss toast
    LaunchedEffect(toastVisible) {
        if (toastVisible) {
            delay(2500)
            onToastChange(false, null, null)
        }
    }

    val onDomainToggle: (String) -> Unit = { domain ->
        val result = FilterEngine.toggleDomainStatus(context, domain)
        onToastChange(true, result.message, result.toastType)
        // Force UI update
        VpnStats.refreshLogStatuses()
        filterUpdateTrigger++ // Increment to trigger UI refresh
    }



    if (showDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showDisclosureDialog = false },
            title = { Text("SYSTEM DISCLOSURE") },
            text = { Text("Local VPN required for packet inspection.\nData remains local.\nProceed?") },

            confirmButton = {
                Button(
                    onClick = {
                        hasAcceptedDisclosure = true
                        context.getSharedPreferences(
                            "adshield_prefs",
                            android.content.Context.MODE_PRIVATE
                        )
                            .edit().putBoolean("disclosure_accepted", true).apply()
                        showDisclosureDialog = false
                        onStartClick()
                    },
                    shape = MaterialTheme.shapes.small
                ) { Text("INITIALIZE", color = MaterialTheme.colorScheme.onPrimary) }
            },

            dismissButton = {
                Button(
                    onClick = { showDisclosureDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("ABORT")
                }
            },

            shape = MaterialTheme.shapes.medium
        )
    }

    // MAIN CYBER UI CONTAINER
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Dark Background
    ) {
        GridBackground()


        // Content Switcher
        when (currentScreen) {
            "HOME" -> HomeView(
                isRunning = isRunning,
                blockedCount = blockedCount,
                bpm = bpm,
                filterCount = filterCount,
                dataSaved = dataSaved,
                recentLogs = recentLogs,
                excludedApps = excludedApps, // Pass state
                filterUpdateTrigger = filterUpdateTrigger, // Pass trigger
                isUpdatingFilters = isUpdatingFilters,
                onStartClick = onStartClick,
                onStopClick = onStopClick,
                onWhitelistClick = { navigateTo("APP_LIST") },
                onReloadFilters = {
                    // Reload logic (moved from inline)
                    if (!isUpdatingFilters) {
                        isUpdatingFilters = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val filterData = FilterRepository.downloadAndParseFilters(context)
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
                onLogClick = { onDomainToggle(it) },
                onDomainManagerClick = { navigateTo("DOMAIN_LIST") }, // Added callback
                onAppClick = { packageName ->
                    onWhitelistApp(packageName)
                    excludedApps =
                        preferences.getExcludedApps() // Update state to trigger UI refresh
                }
            )

            "LOGS" -> {
                androidx.activity.compose.BackHandler { navigateBack() }
                LogsView(
                    logs = recentLogs,
                    onLogClick = { onDomainToggle(it) }
                )
            }

            "STATS" -> {
                androidx.activity.compose.BackHandler { navigateBack() }
                StatsView(
                    data = VpnStats.blockedHistory,
                    bpm = bpm,
                    isRunning = isRunning
                )
            }

            "SETTINGS" -> {
                androidx.activity.compose.BackHandler { navigateBack() }
                SettingsView(
                    onBackClick = { navigateBack() },
                    onWhitelistClick = { navigateTo("APP_LIST") },
                    onDomainConfigClick = { navigateTo("DOMAIN_LIST") },
                    onPremiumClick = { navigateTo("PREMIUM") },
                    onThemeChange = onThemeChange
                )
            }

            "APP_LIST" -> {
                androidx.activity.compose.BackHandler { navigateBack() }
                com.example.adshield.ui.AppListScreen(
                    onBackClick = { navigateBack() },
                    onAppToggle = { pkg, isExcluded ->
                        // If isExcluded == true, we want to EXCLUDE it (add to list).
                        // MainActivity's onWhitelistApp toggles state smartly based on current state.
                        // Let's check MainActivity logic again.
                        // onWhitelistApp checks: if in list -> remove (protect); else -> add (whitelist).
                        // so we can just call onWhitelistApp(pkg) directly as it acts as a toggle.
                        // However, to be safe with the 'isExcluded' boolean from UI, we should ensure we match intent.
                        onWhitelistApp(pkg)
                        excludedApps = preferences.getExcludedApps() // Update Dashboard state
                    }
                )
            }

            "DOMAIN_LIST" -> {
                androidx.activity.compose.BackHandler { navigateBack() }
                DomainListScreen(
                    onBackClick = { navigateBack() }
                )
            }

            "PREMIUM" -> {
                androidx.activity.compose.BackHandler { navigateBack() }
                PremiumScreen(
                    onBackClick = { navigateBack() }
                )
            }

            else -> HomeView( // Fallback
                isRunning = isRunning,
                blockedCount = blockedCount,
                bpm = bpm,
                filterCount = filterCount,
                dataSaved = dataSaved,
                recentLogs = recentLogs,
                excludedApps = excludedApps, // Pass state
                filterUpdateTrigger = filterUpdateTrigger, // Pass trigger
                isUpdatingFilters = isUpdatingFilters,
                onStartClick = onStartClick,
                onStopClick = onStopClick,
                onWhitelistClick = { navigateTo("APP_LIST") },
                onReloadFilters = {},
                onLogClick = { onDomainToggle(it) },
                onDomainManagerClick = { navigateTo("DOMAIN_LIST") }, // Added callback
                onAppClick = { packageName ->
                    onWhitelistApp(packageName)
                    excludedApps = preferences.getExcludedApps()
                }
            )
        }

        // FLOATING NAV BAR OVERYLAY
        if (currentScreen in listOf("HOME", "LOGS", "STATS")) {
            CyberNavBar(
                isRunning = isRunning,
                onPowerClick = {
                    if (isRunning) onStopClick()
                    else if (!hasAcceptedDisclosure) showDisclosureDialog = true
                    else onStartClick()
                },
                currentScreen = currentScreen,
                onNavigate = { navigateTo(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // CYBER TOAST OVERLAY
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberToast(
                message = toastMessage,
                type = toastType,
                visible = toastVisible
            )
        }
    }
}
