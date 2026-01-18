package com.example.adshield.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.adshield.data.AppInfo
import com.example.adshield.data.AppPreferences
import com.example.adshield.data.AppsRepository
import com.example.adshield.ui.components.CyberChip
import com.example.adshield.ui.components.GridBackground
import com.example.adshield.ui.theme.ContentDescriptions

enum class AppListTab {
    ALL, BLOCKED, ALLOWED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    onBackClick: () -> Unit,
    onAppToggle: (String, Boolean) -> Unit
) {

    val context = LocalContext.current
    val repository = remember { AppsRepository(context) }
    val preferences = remember { AppPreferences(context) }

    // State
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var excludedApps by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(AppListTab.ALL) }
    
    // UI State for list scrolling
    val listState = rememberLazyListState()

    // Observe Entitlements for Limit Logic
    val userAccess by com.example.adshield.data.BillingManager.currentUserAccess.collectAsState()
    val entitlements = com.example.adshield.data.AccessControl.entitlementsFor(userAccess.state)

    // Calculate EFFECTIVE Whitelist (Who actually bypasses VPN)
    // Matches logic in LocalVpnService.kt (Sorted by Label/Name)
    val effectiveExcludedApps = remember(excludedApps, entitlements, apps) { // Added apps dependency
        if (entitlements.unlimitedWhitelist) {
            excludedApps
        } else {
            // We need to sort by NAME to match the Service logic and UI sorting
            // We can look up names from the 'apps' list which is already loaded
            excludedApps.sortedBy { pkg ->
                 apps.find { it.packageName == pkg }?.name?.lowercase() ?: pkg
            }.take(com.example.adshield.data.AppConfig.FREE_WHITELIST_LIMIT).toSet()
        }
    }

    // Load data
    LaunchedEffect(Unit) {
        excludedApps = preferences.getExcludedApps()
        apps = repository.getInstalledApps()
        isLoading = false
    }

    // Reset scroll to top when Tab or Search changes
    LaunchedEffect(currentTab, searchQuery) {
        listState.scrollToItem(0)
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
            // HEADER
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
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "WHITELIST CONFIG",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    if (!entitlements.unlimitedWhitelist) {
                        Text(
                            text = "Free Limit: ${excludedApps.size}/${com.example.adshield.data.AppConfig.FREE_WHITELIST_LIMIT}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            // SEARCH
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                placeholder = {
                    Text(
                        "SEARCH_MODULE...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = ContentDescriptions.searchButton,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                ),
                shape = MaterialTheme.shapes.small,
                singleLine = true
            )

            // UNIFIED FILTER CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CyberChip(
                    text = "ALL",
                    selected = currentTab == AppListTab.ALL,
                    onClick = { currentTab = AppListTab.ALL },
                    modifier = Modifier.weight(1f)
                )
                CyberChip(
                    text = "BLOCKED",
                    selected = currentTab == AppListTab.BLOCKED,
                    onClick = { currentTab = AppListTab.BLOCKED },
                    modifier = Modifier.weight(1f)
                )
                CyberChip(
                    text = "ALLOWED",
                    selected = currentTab == AppListTab.ALLOWED,
                    onClick = { currentTab = AppListTab.ALLOWED },
                    modifier = Modifier.weight(1f)
                )
            }

            // LIST
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val filteredApps = apps.filter { app ->
                    // 1. Search Query
                    val matchesSearch = app.name.contains(searchQuery, ignoreCase = true) ||
                            app.packageName.contains(searchQuery, ignoreCase = true)

                    // 2. Tab Filter
                    val isExcluded = excludedApps.contains(app.packageName)
                    val matchesTab = when (currentTab) {
                        AppListTab.ALL -> true
                        AppListTab.BLOCKED -> !isExcluded // NOT in whitelist = Blocked/Protected
                        AppListTab.ALLOWED -> isExcluded  // IN whitelist = Allowed/Bypass
                    }

                    matchesSearch && matchesTab
                }.sortedBy { it.name.lowercase() } // Sort by Name for UI usability (Case Insensitive)

                if (filteredApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "> NO MODULES FOUND",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            AppListItem(
                                app = app,
                                isExcluded = excludedApps.contains(app.packageName),
                                isEffective = effectiveExcludedApps.contains(app.packageName),
                                onToggle = { isChecked ->
                                    if (isChecked) {
                                        onAppToggle(app.packageName, true)
                                        excludedApps = excludedApps + app.packageName
                                    } else {
                                        onAppToggle(app.packageName, false)
                                        excludedApps = excludedApps - app.packageName
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    isExcluded: Boolean,
    isEffective: Boolean,
    onToggle: (Boolean) -> Unit
) {
    // Logic for Colors:
    // Excluded & Effective -> Primary (Green)
    // Excluded & NOT Effective (Overflow) -> Grey/Dim
    // Not Excluded -> Very dim / default
    
    val isActive = isExcluded && isEffective
    val isOverflow = isExcluded && !isEffective

    val borderColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isOverflow -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) // Grey for Overflow
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    }
    
    val alpha = when {
        isActive -> 1f
        isOverflow -> 0.6f // Dimmed for overflow
        else -> 0.7f
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon
        val iconBitmap = remember(app.icon) {
            app.icon.toBitmap(width = 48, height = 48).asImageBitmap()
        }

        Image(
            bitmap = iconBitmap,
            contentDescription = ContentDescriptions.appIcon,
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            alpha = alpha
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f * alpha),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 1
            )
        }

        Switch(
            checked = isExcluded,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isOverflow) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.background,
                checkedTrackColor = if (isOverflow) MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f) else MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}
