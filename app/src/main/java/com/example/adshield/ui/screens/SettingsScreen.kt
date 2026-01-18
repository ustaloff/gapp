package com.example.adshield.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.R
import com.example.adshield.data.AppConfig
import com.example.adshield.data.AppPreferences
import com.example.adshield.data.FilterRepository
import com.example.adshield.data.UserRepository
import com.example.adshield.data.UserAccessState
import com.example.adshield.ui.components.GridBackground
import com.example.adshield.ui.theme.AdShieldTheme
import com.example.adshield.ui.theme.AppTheme
import com.example.adshield.ui.theme.NeonAmberPrimary
import com.example.adshield.ui.theme.NeonBluePrimary
import com.example.adshield.ui.theme.NeonGreenPrimary
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.example.adshield.data.BillingManager
import com.example.adshield.BuildConfig
import kotlinx.coroutines.launch
import com.example.adshield.ui.theme.ContentDescriptions
import com.example.adshield.ui.components.UiAccessState
import com.example.adshield.ui.components.applyAccessState

@Composable
fun SettingsView(
    onBackClick: () -> Unit,
    onWhitelistClick: () -> Unit,
    onDomainConfigClick: () -> Unit,
    onLogsClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onReloadFilters: () -> Unit, // New
    isUpdatingFilters: Boolean, // New
    whitelistCount: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPreferences(context) }
    val userAccessState by BillingManager.userAccessState.collectAsState(initial = UserAccessState.FREE)
    val isPremium = userAccessState == UserAccessState.PREMIUM ||
            userAccessState == UserAccessState.TRIAL
    val isFree = !isPremium

    // Removed local isUpdatingFilters state

    @Suppress("UNUSED_VALUE")
    var showUrlDialog by remember { mutableStateOf(false) }

    var currentUrl by remember { mutableStateOf(prefs.getFilterSourceUrl()) }

    @Suppress("UNUSED_VALUE") // False positive on state delegation
    var tempUrl by remember { mutableStateOf(currentUrl) }
// ... (start line 77 to 746 remains same, skipping to bottom) ...
    // Item 3: Filter Source - DIM for FREE (custom URL locked)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.01f),
                MaterialTheme.shapes.medium
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                MaterialTheme.shapes.medium
            )
            .clickable(onClick = {
                if (isFree) {
                    onPremiumClick()
                } else {
                    tempUrl = currentUrl
                    showUrlDialog = true
                }
            })
            .padding(16.dp)
            .applyAccessState(if (isFree) UiAccessState.DIM else UiAccessState.FULL)
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
                    if (isFree) "AdShield Recommended (Official) 🔒"
                    else if (currentUrl.trim() == AppConfig.DEFAULT_FILTER_URL.trim() ||
                        currentUrl.contains("ustaloff/adshield-lists") && currentUrl.endsWith(
                            "blocklist.txt"
                        )
                    ) "AdShield Recommended (Official)" else currentUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onReloadFilters,
                enabled = !isUpdatingFilters,
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                if (isUpdatingFilters) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("RELOAD", fontSize = 12.sp)
                }
            }
        }
    }
    // -- Google Sign In Setup --
    // We observe the user state to update UI immediately
    val currentUser by UserRepository.user.collectAsState()
    var isSigningIn by remember { mutableStateOf(false) }

    // Explicitly defining gso and client to assist compiler type inference
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount =
                task.getResult(com.google.android.gms.common.api.ApiException::class.java)
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
        } catch (_: Exception) {
            isSigningIn = false
            Toast.makeText(context, "Google Sign In Error", Toast.LENGTH_SHORT).show()
        }
    }

    if (showUrlDialog) {
        // Valid URL Regex (Simple check)
        val isValidUrl = remember(tempUrl) {
            tempUrl.isNotEmpty() && (tempUrl.startsWith("http://") || tempUrl.startsWith("https://")) && tempUrl.contains(
                "."
            )
        }

        @Suppress("UNUSED_VALUE")
        var isError by remember { mutableStateOf(false) }

        var isValidating by remember { mutableStateOf(false) }
        var validationResult by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = {
                if (!isValidating) showUrlDialog = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("FILTER SOURCE URL")
                    // RESET BUTTON
                    if (tempUrl.trim() != AppConfig.DEFAULT_FILTER_URL.trim() && !tempUrl.contains("ustaloff/adshield-lists")) {
                        TextButton(
                            onClick = {
                                tempUrl = AppConfig.DEFAULT_FILTER_URL
                                isError = false
                                validationResult = null
                            },
                            enabled = !isValidating
                        ) {
                            Text("RESET", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isDefault = tempUrl.trim() == AppConfig.DEFAULT_FILTER_URL.trim() ||
                            (tempUrl.contains("ustaloff/adshield-lists") && tempUrl.endsWith("blocklist.txt"))

                    if (isDefault) {
                        // MASKED VIEW
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    MaterialTheme.shapes.small
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    MaterialTheme.shapes.small
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✅", fontSize = 16.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "AdShield Official List (Recommended)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "This is the default, curated blocklist for maximum protection. Click 'Custom' below to use a third-party list.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                tempUrl = "" // Clear to let user type
                                isError = false
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("USE CUSTOM URL", color = MaterialTheme.colorScheme.primary)
                        }

                    } else {
                        // CUSTOM EDIT VIEW
                        OutlinedTextField(
                            value = tempUrl,
                            onValueChange = {
                                tempUrl = it.trim()
                                isError = false
                                validationResult = null
                            },
                            label = { Text("https://...") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            isError = isError || validationResult != null,
                            enabled = !isValidating,
                            supportingText = {
                                if (isError) {
                                    Text(
                                        "Invalid URL (Must start with http/https)",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else if (validationResult != null) {
                                    val isSuccess = validationResult!!.startsWith("✅")
                                    Text(
                                        validationResult!!,
                                        color = if (isSuccess) AdShieldTheme.colors.success else MaterialTheme.colorScheme.error,
                                        fontWeight = if (isSuccess) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                val isDefault = tempUrl.trim() == AppConfig.DEFAULT_FILTER_URL.trim() ||
                        (tempUrl.contains("ustaloff/adshield-lists") && tempUrl.endsWith("blocklist.txt"))

                if (!isDefault) {
                    Button(
                        onClick = {
                            if (isValidUrl) {
                                isValidating = true
                                validationResult = null
                                scope.launch {
                                    val result = FilterRepository.verifyUrl(tempUrl)
                                    if (result.isSuccess) {
                                        val count = result.getOrNull() ?: 0
                                        validationResult = "✅ Success! Found $count rules."
                                        // Save and Close after brief delay to show success
                                        prefs.setFilterSourceUrl(tempUrl)
                                        currentUrl = tempUrl
                                        Toast.makeText(
                                            context,
                                            "Filter Source Updated!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        kotlinx.coroutines.delay(1000)
                                        showUrlDialog = false
                                    } else {
                                        validationResult =
                                            "❌ Error: ${result.exceptionOrNull()?.message}"
                                    }
                                    isValidating = false
                                }
                            } else {
                                isError = true
                            }
                        },
                        enabled = !isValidating,
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("CHECKING...")
                        } else {
                            Text("VERIFY & SAVE")
                        }
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { showUrlDialog = false },
                    enabled = !isValidating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = MaterialTheme.shapes.small
                ) { Text("CLOSE") }
            },
            shape = MaterialTheme.shapes.medium
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
                            MaterialTheme.shapes.medium
                        )
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.shapes.medium
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
                    fontFamily = FontFamily.Monospace
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
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.shapes.medium
                        )
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
                                    UserRepository.signOut()
                                    try {
                                        googleSignInClient.signOut().addOnFailureListener {
                                            // Ignore GMS errors during sign-out (e.g. if service is unreachable)
                                        }
                                    } catch (_: Exception) {
                                        // Prevent crash if GMS process is dead
                                    }
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

                if (!isPremium) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // PREMIUM BANNER
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        AdShieldTheme.colors.premiumStart,
                                        AdShieldTheme.colors.premiumEnd
                                    )
                                ),
                                shape = MaterialTheme.shapes.large
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
                                Text(
                                    "GO PREMIUM",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Unlock full power & support devs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Premium Banner",
                                tint = Color.Yellow
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // THEME SELECTOR
                Text(
                    "INTERFACE THEME",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Green Button (Free)
                    Button(
                        onClick = { onThemeChange(AppTheme.CyberGreen) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            NeonGreenPrimary
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("GREEN", color = NeonGreenPrimary)
                    }

                    // Blue Button (Premium)
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = {
                                if (isPremium) onThemeChange(AppTheme.CyberBlue) else onPremiumClick()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isPremium) NeonBluePrimary else Color.Gray
                            ),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("BLUE", color = if (isPremium) NeonBluePrimary else Color.Gray)
                        }
                        if (!isPremium) {
                            Icon(
                                Icons.Default.Lock,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp)
                            )
                        }
                    }

                    // Amber Button (Premium)
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = {
                                if (isPremium) onThemeChange(AppTheme.CyberAmber) else onPremiumClick()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isPremium) NeonAmberPrimary else Color.Gray
                            ),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("AMBER", color = if (isPremium) NeonAmberPrimary else Color.Gray)
                        }
                        if (!isPremium) {
                            Icon(
                                Icons.Default.Lock,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ==========================
                // CONFIGURATION (MANAGERS)
                // ==========================
                Text(
                    "CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Item 1: APP MANAGER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.01f),
                            MaterialTheme.shapes.medium
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.shapes.medium
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
                                "APP MANAGER",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Excluded apps (Whitelist)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!isPremium) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "($whitelistCount/${AppConfig.FREE_WHITELIST_LIMIT})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = ContentDescriptions.lockIcon,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Item 2: DOMAIN MANAGER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.01f),
                            MaterialTheme.shapes.medium
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.shapes.medium
                        )
                        .clickable(onClick = {
                            if (isFree) onPremiumClick() else onDomainConfigClick()
                        })
                        .padding(16.dp)
                        .applyAccessState(if (isFree) UiAccessState.DIM else UiAccessState.FULL)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "DOMAIN MANAGER",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (isFree) "Blocked & Allowed domains 🔒" else "Blocked & Allowed domains",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = ContentDescriptions.domainIcon,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ==========================
                // MONITORING (LOGS)
                // ==========================
                Text(
                    "MONITORING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Item 3: SYSTEM LOGS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.01f),
                            MaterialTheme.shapes.medium
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.shapes.medium
                        )
                        .clickable(onClick = {
                            if (isFree) onPremiumClick() else onLogsClick()
                        })
                        .padding(16.dp)
                        .applyAccessState(if (isFree) UiAccessState.DIM else UiAccessState.FULL)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "SYSTEM LOGS",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (isFree) "Traffic history (Apps & Domains) 🔒" else "Traffic history (Apps & Domains)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (isFree) Icons.Default.Lock else Icons.AutoMirrored.Filled.List,
                            contentDescription = "Logs",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Item 3: Filter Source - DIM for FREE (custom URL locked)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.01f),
                            MaterialTheme.shapes.medium
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.shapes.medium
                        )
                        .clickable(onClick = {
                            if (isFree) {
                                onPremiumClick()
                            } else {
                                tempUrl = currentUrl
                                showUrlDialog = true
                            }
                        })
                        .padding(16.dp)
                        .applyAccessState(if (isFree) UiAccessState.DIM else UiAccessState.FULL)
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
                                if (isFree) "AdShield Recommended (Official) 🔒"
                                else if (currentUrl.trim() == AppConfig.DEFAULT_FILTER_URL.trim() ||
                                    currentUrl.contains("ustaloff/adshield-lists") && currentUrl.endsWith(
                                        "blocklist.txt"
                                    )
                                ) "AdShield Recommended (Official)" else currentUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onReloadFilters,
                            enabled = !isUpdatingFilters,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            if (isUpdatingFilters) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("RELOAD", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (BuildConfig.DEBUG) {
                    Spacer(Modifier.height(32.dp))
                    Text(
                        "QA / DEBUG ZONE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Red.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Current State: ${userAccessState.name}",
                            color = Color.White,
                            fontSize = 12.sp
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { BillingManager.resetToFree(context) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("FREE", fontSize = 10.sp) }

                            Button(
                                onClick = { BillingManager.activateTrial(context) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("TRIAL", fontSize = 10.sp) }

                            Button(
                                onClick = { BillingManager.activatePremium(context) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("PREMIUM", fontSize = 10.sp) }
                        }

                        Button(
                            onClick = {
                                prefs.resetOnboarding()
                                Toast.makeText(
                                    context,
                                    "Onboarding Reset. Restart App.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red.copy(
                                    alpha = 0.3f
                                )
                            )
                        ) {
                            Text("RESET ONBOARDING FLAG", color = Color.Red)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(150.dp))
            }
        }
    }
}
