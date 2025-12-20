package com.example.adshield

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
    val isRunning by VpnStats.isRunning.collectAsState()
    val blockedCount by VpnStats.blockedCount.collectAsState()
    val totalCount by VpnStats.totalRequests.collectAsState()
    
    // Coroutine Scope for UI actions (downloading)
    val scope = rememberCoroutineScope()
    
    // Filter State
    var filterCount by remember { mutableStateOf(com.example.adshield.filter.FilterEngine.getRuleCount()) }
    var isUpdatingFilters by remember { mutableStateOf(false) }

    // Auto-update on first launch if list is small
    LaunchedEffect(Unit) {
        if (filterCount < 100) {
            isUpdatingFilters = true
            kotlinx.coroutines.delay(1000) // Small delay for UX
            // Perform download in IO context inside the LaunchedEffect scope
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val newRules = com.example.adshield.data.FilterRepository.downloadAndParseFilters()
                 // Switch back to Main is implicit when updating state in Compose but let's be safe/explicit if needed, 
                 // actually LaunchedEffect runs on main, so we just need to offload the heavy work.
                 if (newRules.isNotEmpty()) {
                     com.example.adshield.filter.FilterEngine.updateBlocklist(newRules)
                     // Update UI state on Main thread (implied by return from withContext)
                 }
            }
            // Update UI state after suspension
            filterCount = com.example.adshield.filter.FilterEngine.getRuleCount()
            isUpdatingFilters = false
        }
    }

    // Scroll state for smaller screens
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "AdShield",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Protecting your privacy",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        StatusIndicator(isRunning)

        Spacer(modifier = Modifier.height(30.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Blocked Ads",
                value = blockedCount.toString(),
                color = Color(0xFFEF5350), // Red
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Requests",
                value = totalCount.toString(),
                color = Color(0xFF42A5F5), // Blue
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Settings / Whitelist Button
        OutlinedButton(
            onClick = onWhitelistClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("⛔ APP WHITELIST (SPLIT TUNNELING)")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filter Database Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Filter Rules", fontWeight = FontWeight.Bold)
                    Text("$filterCount active", style = MaterialTheme.typography.bodySmall)
                }
                
                if (isUpdatingFilters) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = {
                        isUpdatingFilters = true
                        // Use rememberCoroutineScope's launch
                        scope.launch {
                             kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val newRules = com.example.adshield.data.FilterRepository.downloadAndParseFilters()
                                if (newRules.isNotEmpty()) {
                                    com.example.adshield.filter.FilterEngine.updateBlocklist(newRules)
                                }
                             }
                             // Back on Main Logic
                             filterCount = com.example.adshield.filter.FilterEngine.getRuleCount()
                             isUpdatingFilters = false
                        }
                    }) {
                        Text("UPDATE")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isRunning) onStopClick() else onStartClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (isRunning) "STOP PROTECTION" else "ACTIVATE SHIELD",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
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
