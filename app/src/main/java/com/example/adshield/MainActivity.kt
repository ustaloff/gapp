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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.adshield.data.VpnStats
import com.example.adshield.service.LocalVpnService
import com.example.adshield.ui.theme.AdShieldTheme

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
    val isRunning = VpnStats.isRunning.value
    val blockedCount = VpnStats.blockedCount.value
    val totalCount = VpnStats.totalCount.value
    val recentLogs = VpnStats.recentLogs
    
    val scope = rememberCoroutineScope()
    var filterCount by remember { mutableStateOf(com.example.adshield.filter.FilterEngine.getRuleCount()) }
    var isUpdatingFilters by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (filterCount < 100) {
            isUpdatingFilters = true
            kotlinx.coroutines.delay(1000)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val newRules = com.example.adshield.data.FilterRepository.downloadAndParseFilters()
                 if (newRules.isNotEmpty()) {
                     com.example.adshield.filter.FilterEngine.updateBlocklist(newRules)
                 }
            }
            filterCount = com.example.adshield.filter.FilterEngine.getRuleCount()
            isUpdatingFilters = false
        }
    }

    val scrollState = rememberScrollState()

    var showWhitelistDialog by remember { mutableStateOf(false) }
    var domainToWhitelist by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showWhitelistDialog) {
        AlertDialog(
            onDismissRequest = { showWhitelistDialog = false },
            title = { Text("Whitelist Domain?") },
            text = { Text("Do you want to always allow traffic for $domainToWhitelist?") },
            confirmButton = {
                TextButton(onClick = {
                    com.example.adshield.filter.FilterEngine.addToAllowlist(context, domainToWhitelist)
                    showWhitelistDialog = false
                }) {
                    Text("ALLOW")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhitelistDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Header
        Text(
            text = "AdShield",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Premium Protection Active",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Large Status Shield
        StatusIndicator(isRunning)

        Spacer(modifier = Modifier.height(32.dp))
        
        // Dynamic Graph
        Text(
            text = "BLOCKS PER MINUTE",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BlockedGraph(VpnStats.blockedHistory)

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Blocked",
                value = blockedCount.toString(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Analysed",
                value = totalCount.toString(),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Real-time Activity Logs
        Text(
            text = "RECENT ACTIVITY (TAP TO ALLOW)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Card(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
        ) {
            if (recentLogs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No activity yet", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                    recentLogs.forEach { log ->
                        LogItem(log) { domain ->
                            domainToWhitelist = domain
                            showWhitelistDialog = true
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Actions
        OutlinedButton(
            onClick = onWhitelistClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Icon(androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_agenda), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("MANAGE APP WHITELIST")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Update Filters Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.4f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Protection Engine", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("$filterCount rules online", style = MaterialTheme.typography.bodySmall)
                }
                
                if (isUpdatingFilters) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = {
                        isUpdatingFilters = true
                        scope.launch {
                             kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val newRules = com.example.adshield.data.FilterRepository.downloadAndParseFilters()
                                if (newRules.isNotEmpty()) {
                                    com.example.adshield.filter.FilterEngine.updateBlocklist(newRules)
                                }
                             }
                             filterCount = com.example.adshield.filter.FilterEngine.getRuleCount()
                             isUpdatingFilters = false
                        }
                    }) {
                        Text("RELOAD", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

    var hasAcceptedDisclosure by remember { mutableStateOf(false) }
    var showDisclosureDialog by remember { mutableStateOf(false) }
    
    // Check if disclosure was previously accepted
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("adshield_prefs", android.content.Context.MODE_PRIVATE)
        hasAcceptedDisclosure = prefs.getBoolean("disclosure_accepted", false)
    }

    if (showDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showDisclosureDialog = false },
            title = { Text("Privacy & Security Disclosure") },
            text = { 
                Text("AdShield uses a local VPN service to provide DNS-level protection. \n\n" +
                     "• We ONLY analyze DNS queries to block ads and trackers.\n" +
                     "• NO personal data is collected, stored, or shared.\n" +
                     "• Internal traffic is never sent to remote servers except for DNS resolution.\n" +
                     "• You can deactivate protection at any time.") 
            },
            confirmButton = {
                Button(onClick = {
                    hasAcceptedDisclosure = true
                    context.getSharedPreferences("adshield_prefs", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("disclosure_accepted", true).apply()
                    showDisclosureDialog = false
                    onStartClick()
                }) {
                    Text("ACCEPT & CONTINUE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosureDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Update ACTIVATE button logic
    Button(
        onClick = { 
            if (isRunning) {
                onStopClick()
            } else {
                if (!hasAcceptedDisclosure) {
                    showDisclosureDialog = true
                } else {
                    onStartClick()
                }
            }
        },
        modifier = Modifier.fillMaxWidth().height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Text(
            text = if (isRunning) "DEACTIVATE SHIELD" else "ACTIVATE ADSHIELD",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun LogItem(log: com.example.adshield.data.VpnLogEntry, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onSelect(log.domain) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (log.isBlocked) Color(0xFFEF5350) else Color(0xFF66BB6A), RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.domain,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = if (log.isBlocked) "Blocked by Shield" else "Allowed by Engine",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (log.appName != null) {
            Text(
                text = log.appName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha=0.7f),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}


@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun StatusIndicator(isRunning: Boolean) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isRunning)
                        listOf(Color(0xFF66BB6A).copy(alpha = 0.2f), Color.Transparent)
                    else
                        listOf(Color(0xFFBDBDBD).copy(alpha = 0.2f), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            // Using placeholder icons for now
            painter = androidx.compose.ui.res.painterResource(id = if (isRunning) android.R.drawable.ic_secure else android.R.drawable.ic_lock_idle_lock),
            contentDescription = null,
            tint = if (isRunning) Color(0xFF43A047) else Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = if (isRunning) "ACTIVE" else "OFFLINE",
            color = if (isRunning) Color(0xFF43A047) else Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 90.dp)
        )
    }
}

@Composable
fun BlockedGraph(history: List<Int>) {
    val maxVal = (history.maxOrNull() ?: 1).coerceAtLeast(5)
    
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            history.forEach { value ->
                val heightFactor = value.toFloat() / maxVal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(heightFactor.coerceIn(0.05f, 1f))
                        .background(
                            color = if (value > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
            }
        }
    }
}

