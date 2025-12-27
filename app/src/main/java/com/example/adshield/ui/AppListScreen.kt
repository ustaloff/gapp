package com.example.adshield.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.adshield.data.AppInfo
import com.example.adshield.data.AppPreferences
import com.example.adshield.data.AppsRepository
import com.example.adshield.ui.components.GridBackground
import kotlinx.coroutines.launch
import com.example.adshield.ui.theme.AdShieldTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AppsRepository(context) }
    val preferences = remember { AppPreferences(context) }

    // State
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var excludedApps by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Load data
    LaunchedEffect(Unit) {
        excludedApps = preferences.getExcludedApps()
        apps = repository.getInstalledApps()
        isLoading = false
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
                            AdShieldTheme.shapes.back
                        )
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            AdShieldTheme.shapes.back
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "WHITELIST CONFIG",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
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
                        contentDescription = null,
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
                shape = AdShieldTheme.shapes.input,
                singleLine = true
            )

            // FILTERS (2 Toggles)
            var showWhitelistedOnly by remember { mutableStateOf(false) }
            var showSystemApps by remember { mutableStateOf(false) } // Default hidden

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Whitelist Filter
                FilterChip(
                    selected = showWhitelistedOnly,
                    onClick = { showWhitelistedOnly = !showWhitelistedOnly },
                    label = { Text(if (showWhitelistedOnly) "WHITELISTED ONLY" else "SHOW ALL") },
                    leadingIcon = {
                        if (showWhitelistedOnly) Icon(
                            Icons.Default.CheckCircle,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                // Right: System Filter
                FilterChip(
                    selected = showSystemApps,
                    onClick = { showSystemApps = !showSystemApps },
                    label = { Text("SYSTEM APPS") },
                    leadingIcon = {
                        if (showSystemApps) Icon(
                            Icons.Default.Settings,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
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

                    // 2. Whitelist Filter
                    val isExcluded = excludedApps.contains(app.packageName)
                    val matchesWhitelist = if (showWhitelistedOnly) isExcluded else true

                    // 3. System Filter
                    // If showSystemApps is FALSE, we hide system apps (isSystem=true)
                    // If showSystemApps is TRUE, we show everything
                    val matchesSystem = if (showSystemApps) true else !app.isSystem

                    matchesSearch && matchesWhitelist && matchesSystem
                }

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
                                onToggle = { isChecked ->
                                    if (isChecked) {
                                        preferences.addExcludedApp(app.packageName)
                                        excludedApps = excludedApps + app.packageName
                                    } else {
                                        preferences.removeExcludedApp(app.packageName)
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
    onToggle: (Boolean) -> Unit
) {
    val borderColor =
        if (isExcluded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = 0.2f
        )
    val alpha = if (isExcluded) 1f else 0.7f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, AdShieldTheme.shapes.entity)
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
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(AdShieldTheme.shapes.icon),
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
                checkedThumbColor = MaterialTheme.colorScheme.background,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}
