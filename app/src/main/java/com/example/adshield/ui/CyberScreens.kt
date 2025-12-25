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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import com.example.adshield.data.VpnStats
import com.example.adshield.data.VpnLogEntry
import com.example.adshield.data.AppPreferences
import androidx.compose.ui.platform.LocalContext
import com.example.adshield.ui.components.*
import com.example.adshield.data.UserRepository
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseUser
import androidx.compose.material.icons.filled.CheckCircle

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
        // CENTERED HEADER (Logo + Title)
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.adshield.R.drawable.ic_app_logo_final),
                contentDescription = "AdShield Logo",
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            GlitchText(
                text = "ADSHIELD",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // STATUS SECTION (Redsigned)
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column {
                // Header (Status Indicator)
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
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
                        progress = (timeMs / (5 * 60 * 1000f)).coerceIn(0.01f, 1f),
                        progressSegments = 3,
                        iconVector = androidx.compose.material.icons.Icons.Filled.Speed,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
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
    onWhitelistClick: () -> Unit,
    onPremiumClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPreferences(context) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(prefs.getFilterSourceUrl()) }
    var tempUrl by remember { mutableStateOf(currentUrl) }

    // -- Google Sign In Setup --
    // We observe the user state to update UI immediately
    var currentUser by remember { mutableStateOf(UserRepository.getCurrentUser()) }
    
    // Refresh user on composition (in case it changed)
    LaunchedEffect(Unit) {
        currentUser = UserRepository.getCurrentUser()
    }

    val webClientId = androidx.compose.ui.res.stringResource(com.example.adshield.R.string.default_web_client_id)
    val gso = remember {
         com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
             .requestIdToken(webClientId)
             .requestEmail()
             .build()
    }
    val googleSignInClient = remember { com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso) }

    val signInLauncher = rememberLauncherForActivityResult(
         contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
         val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
         try {
             val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
             val idToken = account.idToken
             if (idToken != null) {
                 scope.launch {
                      val authResult = UserRepository.signInWithGoogle(idToken)
                      if (authResult.isSuccess) {
                          currentUser = UserRepository.getCurrentUser()
                          android.widget.Toast.makeText(context, "Identity Linked!", android.widget.Toast.LENGTH_SHORT).show()
                      } else {
                          android.widget.Toast.makeText(context, "Link Failed", android.widget.Toast.LENGTH_SHORT).show()
                      }
                 }
             }
         } catch (e: com.google.android.gms.common.api.ApiException) {
              android.util.Log.w("Auth", "Google sign in failed", e)
              android.widget.Toast.makeText(context, "Sign In Error: ${e.statusCode}", android.widget.Toast.LENGTH_SHORT).show()
         }
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("FILTER SOURCE URL") },
            text = {
                Column {
                    Text("Enter raw text URL for block list:", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.setFilterSourceUrl(tempUrl)
                    currentUrl = tempUrl
                    showUrlDialog = false
                }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text("CANCEL") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
            .verticalScroll(rememberScrollState())
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

        // SECTION: ACCOUNT (CYBER IDENTITY)
        Text(
            text = "IDENTITY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
             Column {
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier.fillMaxWidth()
                 ) {
                     Icon(
                         imageVector = if (currentUser != null) Icons.Default.CheckCircle else Icons.Default.Info,
                         contentDescription = null,
                         tint = if (currentUser != null) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
                     )
                     Spacer(modifier = Modifier.width(12.dp))
                     Column {
                         Text(
                             text = if (currentUser != null) "LINKED" else "NOT LINKED",
                             fontWeight = FontWeight.Bold,
                             color = if (currentUser != null) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
                         )
                         if (currentUser != null) {
                             Text(
                                 text = currentUser?.email ?: "Unknown ID",
                                 style = MaterialTheme.typography.labelSmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                             )
                         }
                     }
                 }
                 
                 Spacer(modifier = Modifier.height(16.dp))
                 
                 if (currentUser == null) {
                     Button(
                         onClick = { signInLauncher.launch(googleSignInClient.signInIntent) },
                         modifier = Modifier.fillMaxWidth(),
                         colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                         shape = RoundedCornerShape(4.dp)
                     ) {
                         Text("LINK GOOGLE IDENTITY")
                     }
                 } else {
                      OutlinedButton(
                         onClick = { 
                             UserRepository.signOut()
                             currentUser = null
                             googleSignInClient.signOut()
                         },
                         modifier = Modifier.fillMaxWidth(),
                         shape = RoundedCornerShape(4.dp)
                     ) {
                         Text("UNLINK IDENTITY")
                     }
                 }
             }
        }

        Spacer(Modifier.height(32.dp))
        
        // PREMIUM BANNER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6200EE), Color(0xFFBB86FC))
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onPremiumClick)
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.SpaceBetween, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("GO PREMIUM", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Unlock full power & support devs", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha=0.8f))
                }
                Icon(androidx.compose.material.icons.Icons.Filled.Star, contentDescription = null, tint = Color.Yellow)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Item 1: Whitelist
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

        Spacer(Modifier.height(16.dp))

        // Item 2: Filter Source
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable(onClick = { tempUrl = currentUrl; showUrlDialog = true })
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("FILTER SOURCE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(currentUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                Icon(androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        
         Spacer(modifier = Modifier.height(150.dp))
    }
}
