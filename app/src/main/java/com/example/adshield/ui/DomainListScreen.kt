package com.example.adshield.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.data.AppPreferences
import com.example.adshield.filter.FilterEngine
import com.example.adshield.ui.components.GridBackground
import com.example.adshield.ui.theme.AdShieldTheme

enum class DomainTab {
    ALLOWED, BLOCKED
}

@Composable
fun DomainListScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context) }

    // State
    var currentTab by remember { mutableStateOf(DomainTab.ALLOWED) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Fetch lists dynamically
    // We use a key to force refresh when tab changes or after an operation
    var refreshTrigger by remember { mutableStateOf(0) }

    val domains by produceState(
        initialValue = emptyList<String>(),
        key1 = currentTab,
        key2 = refreshTrigger,
        key3 = searchQuery
    ) {
        val list = if (currentTab == DomainTab.BLOCKED) {
            preferences.getUserBlocklist().toList()
        } else {
            preferences.getUserAllowlist().toList()
        }

        // Filter
        value = list.filter { it.contains(searchQuery, ignoreCase = true) }.sorted()
    }

    val emptyText = if (searchQuery.isNotEmpty()) "> NO MATCHES FOUND"
    else if (currentTab == DomainTab.BLOCKED) "> NO BANNED DOMAINS"
    else "> NO ALLOWED DOMAINS"

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
                Text(
                    text = "DOMAIN MANAGER",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            }

            // SEARCH
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace
                ),
                placeholder = {
                    Text(
                        "SEARCH_DOMAIN...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace
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
                shape = MaterialTheme.shapes.small,
                singleLine = true
            )

            // TABS (Filters)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ALLOWED TAB
                FilterChip(
                    selected = currentTab == DomainTab.ALLOWED,
                    onClick = { currentTab = DomainTab.ALLOWED },
                    label = { Text("ALLOWED") },
                    leadingIcon = {
                        if (currentTab == DomainTab.ALLOWED) Icon(
                            Icons.Default.CheckCircle,
                            null,
                            Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary, // Green
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = MaterialTheme.shapes.small,
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        enabled = true,
                        selected = currentTab == DomainTab.ALLOWED
                    ),
                    modifier = Modifier.weight(1f)
                )

                // BLOCKED TAB
                FilterChip(
                    selected = currentTab == DomainTab.BLOCKED,
                    onClick = { currentTab = DomainTab.BLOCKED },
                    label = { Text("BLOCKED") },
                    leadingIcon = {
                        if (currentTab == DomainTab.BLOCKED) Icon(
                            Icons.Default.Close,
                            null,
                            Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        // Use Error color for blocked tab when selected? Or keep uniform primary?
                        // Let's use Error color for distinctiveness
                        selectedContainerColor = MaterialTheme.colorScheme.error,
                        selectedLabelColor = MaterialTheme.colorScheme.onError,
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = MaterialTheme.shapes.small,
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (currentTab == DomainTab.BLOCKED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.5f
                        ),
                        enabled = true,
                        selected = currentTab == DomainTab.BLOCKED
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // LIST
            if (domains.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        emptyText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp), // Space for FAB
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(domains, key = { it }) { domain ->
                        DomainItem(
                            domain = domain,
                            isBlocked = currentTab == DomainTab.BLOCKED,
                            onDelete = {
                                if (currentTab == DomainTab.BLOCKED) {
                                    FilterEngine.removeFromBlocklist(context, domain)
                                } else {
                                    FilterEngine.removeFromAllowlist(context, domain)
                                }
                                refreshTrigger++ // Refresh list
                                // Force stats refresh
                                com.example.adshield.data.VpnStats.refreshLogStatuses()
                            }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = if (currentTab == DomainTab.BLOCKED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            contentColor = if (currentTab == DomainTab.BLOCKED) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Domain")
        }

        // ADD DIALOG
        if (showAddDialog) {
            AddDomainDialog(
                title = if (currentTab == DomainTab.BLOCKED) "BLOCK DOMAIN" else "ALLOW DOMAIN",
                onDismiss = { showAddDialog = false },
                onAdd = { newDomain ->
                    if (currentTab == DomainTab.BLOCKED) {
                        FilterEngine.addToBlocklist(context, newDomain)
                    } else {
                        FilterEngine.addToAllowlist(context, newDomain)
                    }
                    refreshTrigger++
                    showAddDialog = false
                    // Force stats refresh
                    com.example.adshield.data.VpnStats.refreshLogStatuses()
                }
            )
        }
    }
}

@Composable
fun DomainItem(
    domain: String,
    isBlocked: Boolean,
    onDelete: () -> Unit
) {
    val borderColor =
        if (isBlocked) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(
            alpha = 0.5f
        )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                borderColor,
                MaterialTheme.shapes.extraSmall
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Optional: Icon based on status
            Icon(
                if (isBlocked) Icons.Default.Close else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = domain,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun AddDomainDialog(
    title: String,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Domain (e.g. google.com)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (text.isNotBlank()) onAdd(text)
                }),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onAdd(text) },
                enabled = text.isNotBlank(),
                shape = MaterialTheme.shapes.small
            ) {
                Text("ADD")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant, // Neutral Dark
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Text("CANCEL")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium
    )
}
