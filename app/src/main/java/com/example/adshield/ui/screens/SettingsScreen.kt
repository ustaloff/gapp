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
import androidx.compose.material.icons.filled.Refresh
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
import com.example.adshield.data.AppPreferences
import com.example.adshield.data.FilterRepository
import com.example.adshield.data.UserRepository
import com.example.adshield.ui.components.GridBackground
import com.example.adshield.ui.theme.AdShieldTheme
import com.example.adshield.ui.theme.AppTheme
import com.example.adshield.ui.theme.NeonAmberPrimary
import com.example.adshield.ui.theme.NeonBluePrimary
import com.example.adshield.ui.theme.NeonGreenPrimary
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
// import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
// import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun SettingsView(
    onBackClick: () -> Unit,
    onWhitelistClick: () -> Unit,
    onDomainConfigClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onThemeChange: (AppTheme) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPreferences(context) }

    @Suppress("UNUSED_VALUE") // False positive: Variable is assigned but analyzer thinks it's unused
    var showUrlDialog by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(prefs.getFilterSourceUrl()) }

    @Suppress("UNUSED_VALUE") // False positive on state delegation
    var tempUrl by remember { mutableStateOf(currentUrl) }

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

        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("FILTER SOURCE URL")
                    // RESET BUTTON
                    TextButton(onClick = {
                        tempUrl = FilterRepository.DEFAULT_URL
                        isError = false
                    }) {
                        Text("RESET", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = {
                            tempUrl = it.trim()
                            isError = false
                        },
                        label = { Text("https://...") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        isError = isError,
                        supportingText = {
                            if (isError) Text(
                                "Invalid URL (Must start with http/https)",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isValidUrl) {
                            currentUrl = tempUrl
                            prefs.setFilterSourceUrl(tempUrl)
                            showUrlDialog = false
                        } else {
                            isError = true
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) { Text("SAVE") }
            },
            dismissButton = {
                Button(
                    onClick = { showUrlDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = MaterialTheme.shapes.small
                ) { Text("CANCEL") }
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
                                    FirebaseAuth.getInstance().signOut()
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
                            Text("GO PREMIUM", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                "Unlock full power & support devs",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color.Yellow
                        )
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
                    // Green Button
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

                    // Blue Button
                    Button(
                        onClick = { onThemeChange(AppTheme.CyberBlue) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            NeonBluePrimary
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("BLUE", color = NeonBluePrimary)
                    }

                    // Amber Button
                    Button(
                        onClick = { onThemeChange(AppTheme.CyberAmber) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            NeonAmberPrimary
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("AMBER", color = NeonAmberPrimary)
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

                // Item 2: Domain Manager (Unified)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.shapes.medium
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
                                "DOMAIN MANAGER",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Manage allowed & banned domains",
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

                // Item 2: Filter Source
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.shapes.medium
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
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.Refresh,
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
