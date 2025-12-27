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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Bolt // Replaced Speed
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import com.example.adshield.data.VpnStats
import com.example.adshield.data.VpnLogEntry
import androidx.compose.ui.platform.LocalContext
import com.example.adshield.data.AppPreferences
import com.example.adshield.filter.FilterEngine
import com.example.adshield.ui.components.*
import com.example.adshield.data.UserRepository
import com.example.adshield.ui.theme.AdShieldTheme
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import android.widget.Toast

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
    onAppClick: (String) -> Unit
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
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.adshield.R.drawable.ic_app_logo_final),
                    contentDescription = "AdShield Logo",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
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
                    .border(1.dp, if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, AdShieldTheme.shapes.container)
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
                            iconVector = androidx.compose.material.icons.Icons.Default.Speed, // Changed from Speed to Bolt
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
            val context = LocalContext.current
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    CyberTopList(
                        title = "TOP APPS",
                        data = VpnStats.appBlockedStatsMap,
                        onAllowClick = { onAppClick(it) },
                        isWhitelisted = { pkg -> excludedApps.contains(pkg) } // Use State
                    )
                }
                Box(Modifier.weight(1f)) {
                    CyberTopList(
                        title = "TOP DOMAINS",
                        data = VpnStats.domainBlockedStatsMap,
                        onAllowClick = { onLogClick(it) },
                        isWhitelisted = { domain ->
                            val tick = filterUpdateTrigger // Force Recomp reading
                            FilterEngine.checkDomain(domain) == FilterEngine.FilterStatus.ALLOWED_USER
                        }
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
            Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
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
            Icon(
                androidx.compose.material.icons.Icons.Default.Info,
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

        CyberGraphSection(data, bpm, isRunning)

        Spacer(Modifier.height(24.dp))
        Text(
            "> Detailed analysis modules loading...",
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(150.dp))
    }
}


@Composable
fun SettingsView(
    onBackClick: () -> Unit,
    onWhitelistClick: () -> Unit,
    onDomainConfigClick: () -> Unit,
    onBlockedConfigClick: () -> Unit,
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
    val currentUser by UserRepository.user.collectAsState()
    var isSigningIn by remember { mutableStateOf(false) }

    // Explicitly defining gso and client to assist compiler type inference
    val gso = remember<GoogleSignInOptions> {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.adshield.R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember<GoogleSignInClient> { GoogleSignIn.getClient(context, gso) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                scope.launch {
                    val authResult = UserRepository.signInWithGoogle(idToken)
                    isSigningIn = false
                    if (authResult.isSuccess) {
                        Toast.makeText(context, "Identity Linked!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Sign In Failed: ${authResult.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                isSigningIn = false
                Toast.makeText(context, "Error: Google ID Token is missing", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            isSigningIn = false
            Toast.makeText(context, "Google Sign In Error", Toast.LENGTH_SHORT).show()
        }
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("FILTER SOURCE URL") },
            text = {
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = { tempUrl = it },
                    label = { Text("https://...") },
                    singleLine = true,
                    shape = AdShieldTheme.shapes.input
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentUrl = tempUrl
                        prefs.setFilterSourceUrl(tempUrl)
                        showUrlDialog = false
                    },
                    shape = AdShieldTheme.shapes.button
                ) { Text("SAVE") }
            },
            dismissButton = {
                Button(
                    onClick = { showUrlDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = AdShieldTheme.shapes.button
                ) { Text("CANCEL") }
            },

            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            shape = AdShieldTheme.shapes.dialog
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GridBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Custom Header with Back Button (STATIC)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            AdShieldTheme.shapes.back
                        )
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            AdShieldTheme.shapes.back
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "SYSTEM CONFIG",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Account Status Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.secondary, AdShieldTheme.shapes.setting)
                        .padding(16.dp)
                ) {
                    if (currentUser != null) {
                        Column {
                            Text(
                                "OPERATOR IDENTIFIED",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                currentUser?.email ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                    googleSignInClient.signOut()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("TERMINATE SESSION")
                            }
                        }
                    } else {
                        Column {
                            Text(
                                "NO IDENTITY",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    isSigningIn = true
                                    signInLauncher.launch(googleSignInClient.signInIntent)
                                },
                                enabled = !isSigningIn,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("LINK IDENTITY (GOOGLE)")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // PREMIUM BANNER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(com.example.adshield.ui.theme.PremiumStart, com.example.adshield.ui.theme.PremiumEnd)
                            ),
                            shape = AdShieldTheme.shapes.banner
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
                            Text(
                                "Unlock full power & support devs",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            androidx.compose.material.icons.Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color.Yellow
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Item 1: Whitelist (Renamed to APP WHITELIST)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            AdShieldTheme.shapes.setting
                        )
                        .clickable(onClick = onWhitelistClick)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "APP WHITELIST",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Exclude apps from VPN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Item 2: Domain Config
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            AdShieldTheme.shapes.setting
                        )
                        .clickable(onClick = onDomainConfigClick)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "DOMAIN CONFIG",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Manage allowed domains",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Item 3: Blocked Config (User Banned)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            AdShieldTheme.shapes.setting
                        )
                        .clickable(onClick = onBlockedConfigClick)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "BLOCKED CONFIG",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Manage banned domains",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Item 2: Filter Source
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            AdShieldTheme.shapes.setting
                        )
                        .clickable(onClick = { tempUrl = currentUrl; showUrlDialog = true })
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "FILTER SOURCE",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                currentUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            androidx.compose.material.icons.Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(150.dp))
            }
        }
    }
}
